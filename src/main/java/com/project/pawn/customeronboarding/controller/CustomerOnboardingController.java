package com.project.pawn.customeronboarding.controller;

import com.project.pawn.customeronboarding.model.CustomerInfo;
import com.project.pawn.customeronboarding.dto.CustomerResponse;
import com.project.pawn.customeronboarding.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerOnboardingController {

    private final CustomerService customerService;

    @PostMapping("/onboard")
    public ResponseEntity<CustomerResponse> createCustomer(@RequestBody CustomerInfo request) {
        customerService.onboardCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable("id") Long id) {
        // Placeholder: real implementation should fetch from DB or map cached CustomerInfo -> CustomerResponse.
        return ResponseEntity.notFound().build();
    }
}
