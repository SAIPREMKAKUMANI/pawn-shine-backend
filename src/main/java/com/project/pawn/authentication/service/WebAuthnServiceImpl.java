package com.project.pawn.authentication.service;

import com.project.pawn.authentication.model.Credential;
import com.project.pawn.authentication.model.Users;
import com.project.pawn.authentication.payload.UsernameRequest;
import com.project.pawn.authentication.payload.WebAuthnVerifyRequest;
import com.project.pawn.authentication.repository.CredentialRepository;
import com.project.pawn.authentication.repository.UserRepository;
import com.project.pawn.authentication.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebAuthnServiceImpl implements WebAuthnService {

    private final CredentialRepository credentialRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    // In-memory challenge store (map username -> challenge). For production, persist with expiry.
    private final Map<String, String> challenges = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    @Override
    public Map<String, Object> registerOptions(UsernameRequest req, String authorization) {
        log.info("Generating registration options for user: {}", req.getUsername());
        String tokenSubject = extractAndValidateToken(authorization, req.getUsername());

        byte[] challengeBytes = new byte[32];
        random.nextBytes(challengeBytes);
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes);
        challenges.put(req.getUsername(), challenge);
        log.debug("Stored challenge for user: {}", req.getUsername());

        Map<String, Object> user = new HashMap<>();
        user.put("id", Base64.getUrlEncoder().withoutPadding().encodeToString(
                req.getUsername().getBytes(StandardCharsets.UTF_8)));
        user.put("name", req.getUsername());
        user.put("displayName", req.getUsername());

        Map<String, Object> options = new HashMap<>();
        options.put("challenge", challenge);
        options.put("user", user);
        options.put("rp", Map.of("name", "PawnApp"));
        options.put("pubKeyCredParams", List.of(Map.of("type", "public-key", "alg", -7)));

        log.info("Successfully generated registration options for user: {}", req.getUsername());
        return options;
    }

    @Override
    public Map<String, Object> registerVerify(WebAuthnVerifyRequest req, String authorization) {
        log.info("Verifying registration for user: {}", req.getUsername());
        String challenge = challenges.get(req.getUsername());
        if (challenge == null) {
            log.warn("No registration in progress for user: {}", req.getUsername());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No registration in progress for user");
        }

        Users user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Credential credential = Credential.builder()
                .user(user)
                .credentialId(req.getId())
                .publicKey(req.getResponse() != null ? req.getResponse().getOrDefault("attestationObject", "") : "")
                .signCount(0L)
                .build();

        credentialRepository.save(credential);
        log.info("Saved new credential for user: {}", req.getUsername());

        challenges.remove(req.getUsername());
        return Map.of("success", true);
    }

    @Override
    public Map<String, Object> loginOptions(UsernameRequest req) {
        log.info("Generating login options.");
        byte[] challengeBytes = new byte[32];
        random.nextBytes(challengeBytes);
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes);

        if (req == null || req.getUsername() == null || req.getUsername().isBlank()) {
            log.info("Username not provided, preparing for discoverable credentials.");
            Map<String, Object> options = new HashMap<>();
            options.put("challenge", challenge);
            options.put("allowCredentials", List.of());
            options.put("userVerification", "required");
            options.put("timeout", 60000);
            return options;
        }

        log.info("Generating login options for user: {}", req.getUsername());
        challenges.put(req.getUsername(), challenge);

        List<Credential> creds = credentialRepository.findAllByUser_Username(req.getUsername());
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

        log.info("Successfully generated login options for user: {}", req.getUsername());
        return options;
    }

    @Override
    public Map<String, Object> loginVerify(WebAuthnVerifyRequest req) {
        log.info("Verifying login.");
        Credential credential = resolveCredential(req);

        if (req.getUsername() != null && !Objects.equals(credential.getUser().getUsername(), req.getUsername())) {
            log.warn("Credential does not belong to user: {}. Expected: {}", req.getUsername(), credential.getUser().getUsername());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credential does not belong to user");
        }

        UserDetails userDetails = userService.loadUserByUsername(credential.getUser().getUsername());
        String token = jwtUtil.generateToken(userDetails);
        log.info("Successfully verified login for user: {}", credential.getUser().getUsername());

        return Map.of("token", token);
    }

    // --- Private helpers ---

    private String extractAndValidateToken(String authorization, String expectedUsername) {
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
        if (!Objects.equals(subject, expectedUsername)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Token does not match username");
        }
        return subject;
    }

    private Credential resolveCredential(WebAuthnVerifyRequest req) {
        if (req.getId() != null && !req.getId().isBlank()) {
            log.info("Resolving credential by ID: {}", req.getId());
            return credentialRepository.findByCredentialId(req.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown credential"));
        }

        if (req.getResponse() != null && req.getResponse().get("userHandle") != null) {
            try {
                String userHandleB64 = req.getResponse().get("userHandle");
                byte[] decoded = Base64.getDecoder().decode(userHandleB64);
                String username = new String(decoded, StandardCharsets.UTF_8);
                log.info("User handle decoded to username: {}", username);

                List<Credential> creds = credentialRepository.findAllByUser_Username(username);
                if (creds.isEmpty()) {
                    log.warn("No credentials found for username: {}", username);
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown credential");
                }
                return creds.get(0);
            } catch (IllegalArgumentException e) {
                log.error("Invalid userHandle provided.", e);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid userHandle");
            }
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing credential id and userHandle");
    }
}
