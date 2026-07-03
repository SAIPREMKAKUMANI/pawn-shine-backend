package com.project.pawn.common.exception;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Getter
@Setter
public class PawnBrokingException extends RuntimeException {
    private String submitTimeStampUtc;
    private String message;
    private List<ErrorDetail> errorDetail;

    public PawnBrokingException(String message, List<ErrorDetail> errorDetail) {
        super(message);
        this.submitTimeStampUtc = DateTimeFormatter.ISO_INSTANT.format(LocalDateTime.now());
        this.message = message;
        this.errorDetail = errorDetail;
    }

    public PawnBrokingException(String message) {
        super(message);
        this.submitTimeStampUtc = DateTimeFormatter.ISO_INSTANT.format(LocalDateTime.now());
        this.message = message;
    }
}
