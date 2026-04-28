package com.project.pawn.customeronboarding.validation;

import com.project.pawn.customeronboarding.dto.ContactDto;
import com.project.pawn.customeronboarding.dto.CustomerDto;
import com.project.pawn.customeronboarding.exception.CustomerValidationException;
import com.project.pawn.customeronboarding.repository.cache.CustomerCacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class CustomerValidator {
    private final CustomerCacheRepository customerRepository;

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?\\d{1,4}[- ]?\\d{6,14}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public void validateCustomer(CustomerDto customer) {
        validateBasicInfo(customer);
        validateAge(customer.getDateOfBirth());
        validateContacts(customer);
        validateIdProofs(customer);
    }

    private void validateBasicInfo(CustomerDto customer) {
        if (customer.getName() == null || customer.getName().trim().isEmpty()) {
            throw new CustomerValidationException("Customer name is required");
        }
        if (customer.getDateOfBirth() == null) {
            throw new CustomerValidationException("Date of birth is required");
        }
        if (customer.getGender() == null) {
            throw new CustomerValidationException("Gender is required");
        }
    }

    private void validateAge(LocalDate dateOfBirth) {
        int age = Period.between(dateOfBirth, LocalDate.now()).getYears();
        if (age < 18) {
            throw new CustomerValidationException("Customer must be at least 18 years old");
        }
    }

    private void validateContacts(CustomerDto customer) {
        if (customer.getContacts() == null || customer.getContacts().isEmpty()) {
            throw new CustomerValidationException("Contact information is required");
        }

        customer.getContacts().forEach(this::validateContact);
    }

    private void validateContact(ContactDto contact) {
        boolean hasAny = hasValue(contact.getPhone()) || hasValue(contact.getSecondaryPhone()) || hasValue(contact.getWhatsappPhone());
        if (!hasAny) {
            throw new CustomerValidationException("Each contact entry must have at least one contact field (phone, secondaryPhone, whatsappPhone or email)");
        }

        // Validate primary phone
        if (hasValue(contact.getPhone())) {
            String phone = contact.getPhone().trim();
            if (!PHONE_PATTERN.matcher(phone).matches()) {
                throw new CustomerValidationException("Invalid phone format: " + phone);
            }
        }

        // Validate secondary phone
        if (hasValue(contact.getSecondaryPhone())) {
            String sec = contact.getSecondaryPhone().trim();
            if (!PHONE_PATTERN.matcher(sec).matches()) {
                throw new CustomerValidationException("Invalid secondary phone format: " + sec);
            }
            if (contact.getSecondaryPhone().isEmpty()) {
                throw new CustomerValidationException("Duplicate phone in request: " + sec);
            }
        }

        // Validate whatsapp phone
        if (hasValue(contact.getWhatsappPhone())) {
            String wa = contact.getWhatsappPhone().trim();
            if (!PHONE_PATTERN.matcher(wa).matches()) {
                throw new CustomerValidationException("Invalid whatsapp phone format: " + wa);
            }
        }

        // Validate email
        if (hasValue(contact.getEmail())) {
            String email = contact.getEmail().trim();
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                throw new CustomerValidationException("Invalid email format: " + email);
            }
        }
    }

    private boolean hasValue(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private void validateIdProofs(CustomerDto customer) {
        if (customer.getIdProofs() == null || customer.getIdProofs().isEmpty()) {
            throw new CustomerValidationException("At least one ID proof is required");
        }

        customer.getIdProofs().forEach(idProof -> {
            if(idProof.getIdNumber() == null || idProof.getIdNumber().trim().isEmpty()) {
                throw new CustomerValidationException("ID number is required for ID type: " + idProof.getIdType());
            }

            if (customerRepository.existsByIdProofNumber(idProof.getIdNumber())) {
                throw new CustomerValidationException("ID number " + idProof.getIdNumber() + " already exists");
            }

            if(idProof.getIdType() == null) {
                throw new CustomerValidationException("ID type is required for ID number: " + idProof.getIdNumber());
            }
        });
    }
}
