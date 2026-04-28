package com.project.pawn.customeronboarding.validation;

import com.project.pawn.customeronboarding.enums.SearchFields;
import com.project.pawn.customeronboarding.exception.CustomerValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Slf4j
public class GetCustomersValidator {
    public void validate(String field, String value) {
        if (Objects.nonNull(field)) {
            validateSearchField(field);
        }
    }
    private void validateSearchField(String field) {
        try {
            SearchFields.valueOf(field.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomerValidationException("Invalid Search Field");
        }
    }
}
