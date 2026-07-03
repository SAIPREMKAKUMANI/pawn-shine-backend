package com.project.pawn.common.exception;

import com.project.pawn.billing.exception.BillValidationException;
import com.project.pawn.customeronboarding.exception.CustomerValidationException;
import com.project.pawn.customeronboarding.exception.ErrorResponse;
import com.project.pawn.customeronboarding.exception.GenericCustomerOnboardingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BillValidationException.class)
    public ResponseEntity<Map<String, Object>> handleBillValidationException(BillValidationException ex, WebRequest request) {
        log.info("Bill validation error: {}", ex.getMessage());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", ex.getMessage());
        body.put("errors", ex.getErrorDetail());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CustomerValidationException.class)
    public ResponseEntity<ErrorResponse> handleCustomerValidationException(CustomerValidationException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse("Validation Error", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(GenericCustomerOnboardingException.class)
    public ResponseEntity<ErrorResponse> handleGenericOnboardingException(GenericCustomerOnboardingException ex, WebRequest request) {
        log.error("Customer onboarding error: ", ex);
        ErrorResponse errorResponse = new ErrorResponse("Onboarding Error", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse("Validation Error", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
        log.error("Unhandled exception: ", ex);
        ErrorResponse errorResponse = new ErrorResponse("Internal Server Error", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
