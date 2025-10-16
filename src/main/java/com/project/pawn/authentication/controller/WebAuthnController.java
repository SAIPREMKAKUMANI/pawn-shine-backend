// ...existing code...
package com.project.pawn.controller;

import com.project.pawn.model.Credential;
import com.project.pawn.payload.UsernameRequest;
import com.project.pawn.payload.WebAuthnVerifyRequest;
import com.project.pawn.repository.CredentialRepository;
import com.project.pawn.security.JwtUtil;
import com.project.pawn.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth/webauthn")
@RequiredArgsConstructor
public class WebAuthnController {
    private final CredentialRepository credentialRepository;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    // In-memory challenge store (map username -> challenge). For production, persist with expiry.
    private final Map<String, String> challenges = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    @PostMapping("/register/options")
    public Map<String, Object> registerOptions(@RequestBody UsernameRequest req, @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing Authorization header");
        }
        String token = authorization.substring("Bearer ".length());
        String subject;
        try {
            subject = jwtUtil.extractUsername(token);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid token");
        }
        if (!Objects.equals(subject, req.getUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Token does not match username");
        }

        byte[] challengeBytes = new byte[32];
        random.nextBytes(challengeBytes);
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes);
        challenges.put(req.getUsername(), challenge);

        Map<String, Object> user = new HashMap<>();
        user.put("id", Base64.getUrlEncoder().withoutPadding().encodeToString(req.getUsername().getBytes(StandardCharsets.UTF_8)));
        user.put("name", req.getUsername());
        user.put("displayName", req.getUsername());

        Map<String, Object> options = new HashMap<>();
        options.put("challenge", challenge);
        options.put("user", user);
        options.put("rp", Map.of("name", "PawnApp"));

        // Keep allowCredentials empty during registration
        options.put("pubKeyCredParams", List.of(Map.of("type", "public-key", "alg", -7))); // ES256 as example

        return options;
    }

    @PostMapping("/register/verify")
    public Map<String, Object> registerVerify(@RequestBody WebAuthnVerifyRequest req, @RequestHeader(value = "Authorization", required = false) String authorization) {
        // Basic verification: ensure a challenge exists and store the credential metadata.
        String challenge = challenges.get(req.getUsername());
        if (challenge == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No registration in progress for user");
        }

        // NOTE: This implementation does NOT perform full attestation verification.
        // For production, verify the attestationObject and clientDataJSON cryptographically (use WebAuthn4J or similar).

        Credential credential = Credential.builder()
                .username(req.getUsername())
                .credentialId(req.getId())
                .publicKey(req.getResponse() != null ? req.getResponse().getOrDefault("attestationObject", "") : "")
                .signCount(0L)
                .build();

        credentialRepository.save(credential);

        // registration complete; remove challenge
        challenges.remove(req.getUsername());

        return Map.of("success", true);
    }

    @PostMapping("/login/options")
    public Map<String, Object> loginOptions(@RequestBody(required = false) UsernameRequest req) {
        byte[] challengeBytes = new byte[32];
        random.nextBytes(challengeBytes);
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes);

        // If no username provided, return options suitable for discoverable (resident) credentials.
        if (req == null || req.getUsername() == null || req.getUsername().isBlank()) {
            Map<String, Object> options = new HashMap<>();
            options.put("challenge", challenge);
            // No allowed credentials: let the authenticator select resident credentials
            options.put("allowCredentials", List.of());
            // Require user verification (biometric) on the authenticator
            options.put("userVerification", "required");
            options.put("timeout", 60000);
            // Do not store challenge keyed by username since username is unknown; in production persist challenge and correlate on verification.
            return options;
        }

        // Username provided: return options limited to user's registered credentials
        challenges.put(req.getUsername(), challenge);

        List<Credential> creds = credentialRepository.findAllByUsername(req.getUsername());
        List<Map<String, Object>> allowed = creds.stream()
                .map(c -> Map.<String, Object>of(
                        "type", "public-key",
                        "id", c.getCredentialId()
                ))
                .collect(Collectors.toList());

        Map<String, Object> options = new HashMap<>();
        options.put("challenge", challenge);
        options.put("allowCredentials", allowed);
        options.put("userVerification", "preferred");
        options.put("timeout", 60000);

        return options;
    }

    @PostMapping("/login/verify")
    public Map<String, Object> loginVerify(@RequestBody WebAuthnVerifyRequest req) {
        // Basic verification: check credential exists. Support cases where username is not provided (discoverable credentials).

        // If credential id is present, find by id
        Credential credential = null;
        if (req.getId() != null && !req.getId().isBlank()) {
            credential = credentialRepository.findByCredentialId(req.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown credential"));
        } else if (req.getResponse() != null && req.getResponse().get("userHandle") != null) {
            // If userHandle is provided (base64), decode to username and find credentials
            try {
                String userHandleB64 = req.getResponse().get("userHandle");
                byte[] decoded = Base64.getDecoder().decode(userHandleB64);
                String username = new String(decoded, StandardCharsets.UTF_8);
                List<Credential> creds = credentialRepository.findAllByUsername(username);
                if (creds.isEmpty()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown credential");
                credential = creds.get(0);
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid userHandle");
            }
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing credential id and userHandle");
        }

        // NOTE: This implementation does NOT verify assertion signatures. Use a WebAuthn library to perform proper checks.
        if (req.getUsername() != null && !Objects.equals(credential.getUsername(), req.getUsername())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credential does not belong to user");
        }

        // load user details and issue JWT
        org.springframework.security.core.userdetails.UserDetails userDetails = userService.loadUserByUsername(credential.getUsername());
        String token = jwtUtil.generateToken(userDetails);

        return Map.of("token", token);
    }
}
// ...existing code...
