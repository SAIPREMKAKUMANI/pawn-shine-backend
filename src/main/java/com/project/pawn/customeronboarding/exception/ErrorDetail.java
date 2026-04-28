package com.project.pawn.customeronboarding.exception;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ErrorDetail {
    private String field;
    private String reason;
}
