package com.project.pawn.authentication.payload;

import lombok.Data;

@Data
public class AuthRequest {
    private String username;
    private String password;
}