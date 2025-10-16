package com.project.pawn.customeronboarding.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomerValidationException.class)
    public ResponseEntity<?> handleCustomerValidationException(CustomerValidationException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
            "Validation Error",
            ex.getMessage()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGlobalException(Exception ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
            "Internal Server Error",
            ex.getMessage()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}




