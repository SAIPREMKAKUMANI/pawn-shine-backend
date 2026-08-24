package com.project.pawn.authentication.payload;

import lombok.Data;

import java.util.Map;

@Data
public class WebAuthnVerifyRequest {
    private String username;
    private String id;
    private String rawId;
    private String type;
    private Map<String, String> response;
}

