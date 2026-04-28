package com.project.pawn.customeronboarding.exception;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class GenericCustomerOnboardingException extends RuntimeException {

    private final String currentTime;
    private final List<ErrorDetail> errorDetail;

    public GenericCustomerOnboardingException(String message, List<ErrorDetail> errorDetail) {
        super(message);
        this.currentTime = LocalDateTime.now().toString();
        this.errorDetail = errorDetail;
    }

    public GenericCustomerOnboardingException(String message) {
        super(message);
        this.currentTime = LocalDateTime.now().toString();
        this.errorDetail = null;
    }
}
