package com.project.pawn.customeronboarding.service;

import com.project.pawn.customeronboarding.dto.CustomerResponse;
import com.project.pawn.customeronboarding.model.CustomerInfo;
import com.project.pawn.customeronboarding.repository.CustomerRepository;
import com.project.pawn.customeronboarding.validation.CustomerValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final CustomerValidator customerValidator;

    @Transactional
    public CustomerResponse onboardCustomer(CustomerInfo request) {
        log.info("Starting customer onboarding process for customer: {}", request.getName());

        try {
            customerValidator.validateCustomer(request);

            request.setStatus("ACTIVE");
            request.setCreatedBy("SYSTEM");

            // Save customer and related entities
            CustomerInfo savedCustomer = customerRepository.save(request);

            log.info("Successfully onboarded customer with ID: {}", savedCustomer.getCustId());

            return CustomerResponse.builder()
                    .customerId(savedCustomer.getCustId())
                    .status("SUCCESS")
                    .message("Customer onboarded successfully")
                    .build();

        } catch (Exception e) {
            log.error("Error during customer onboarding: {}", e.getMessage(), e);

            return CustomerResponse.builder()
                    .status("FAILED")
                    .message("Customer onboarding failed: " + e.getMessage())
                    .build();
        }
    }
}
