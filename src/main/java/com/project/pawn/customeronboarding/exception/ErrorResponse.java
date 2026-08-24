package com.project.pawn.customeronboarding.exception;

import lombok.Data;
import java.time.LocalDateTime;
@Data
public class ErrorResponse {
    private LocalDateTime timestamp;
    private String message;
    private String details;

    public ErrorResponse(String message, String details) {
        this.timestamp = LocalDateTime.now();
        this.message = message;
        this.details = details;
    }
}