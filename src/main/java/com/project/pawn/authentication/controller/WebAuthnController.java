package com.project.pawn.authentication.controller;

import com.project.pawn.authentication.payload.UsernameRequest;
import com.project.pawn.authentication.payload.WebAuthnVerifyRequest;
import com.project.pawn.authentication.service.WebAuthnService;
import com.project.pawn.common.service.RequestSanitizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/webauthn")
@RequiredArgsConstructor
public class WebAuthnController {

    private final WebAuthnService webAuthnService;
    private final RequestSanitizationService sanitizationService;

    @PostMapping("/register/options")
    public ResponseEntity<Map<String, Object>> registerOptions(@RequestBody UsernameRequest req, @RequestHeader(value = "Authorization", required = false) String authorization) {
        sanitizationService.sanitize(req);
        return ResponseEntity.ok(webAuthnService.registerOptions(req, authorization));
    }

    @PostMapping("/register/verify")
    public Map<String, Object> registerVerify(@RequestBody WebAuthnVerifyRequest req, @RequestHeader(value = "Authorization", required = false) String authorization) {
        sanitizationService.sanitize(req);
        return webAuthnService.registerVerify(req, authorization);
    }

    @PostMapping("/login/options")
    public Map<String, Object> loginOptions(@RequestBody(required = false) UsernameRequest req) {
        sanitizationService.sanitize(req);
        return webAuthnService.loginOptions(req);
    }

    @PostMapping("/login/verify")
    public Map<String, Object> loginVerify(@RequestBody WebAuthnVerifyRequest req) {
        sanitizationService.sanitize(req);
        return webAuthnService.loginVerify(req);
    }
}