package com.project.pawn.billing.exception;

import com.project.pawn.common.exception.ErrorDetail;
import com.project.pawn.common.exception.PawnBrokingException;

import java.util.List;

public class BillValidationException extends PawnBrokingException {
    public BillValidationException(String message) {
        super(message);
    }
    public BillValidationException(String message, List<ErrorDetail> errorDetails) {
        super(message, errorDetails);
    }
}
