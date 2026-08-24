package com.project.pawn.authentication.service;

import com.project.pawn.authentication.payload.UsernameRequest;
import com.project.pawn.authentication.payload.WebAuthnVerifyRequest;

import java.util.Map;

public interface WebAuthnService {
    Map<String, Object> registerOptions(UsernameRequest req, String authorization);
    Map<String, Object> registerVerify(WebAuthnVerifyRequest req, String authorization);
    Map<String, Object> loginOptions(UsernameRequest req);
    Map<String, Object> loginVerify(WebAuthnVerifyRequest req);
}
