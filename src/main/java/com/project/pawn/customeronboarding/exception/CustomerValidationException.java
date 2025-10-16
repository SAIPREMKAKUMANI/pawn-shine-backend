package com.project.pawn.customeronboarding.exception;

public class CustomerValidationException extends RuntimeException {
    public CustomerValidationException(String message) {
        super(message);
    }
}