package com.project.pawn.customeronboarding.controller;

import com.project.pawn.common.service.RequestSanitizationService;
import com.project.pawn.customeronboarding.dto.CustomerDto;
import com.project.pawn.customeronboarding.dto.CustomerResponse;
import com.project.pawn.customeronboarding.dto.response.GetCustomerResponse;
import com.project.pawn.customeronboarding.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final RequestSanitizationService sanitizationService;

    @PostMapping(value = "/onboard", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CustomerResponse> createCustomer(@ModelAttribute CustomerDto request) {
        sanitizationService.sanitize(request);
        CustomerResponse response = customerService.onboardCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping(value = "/update/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable("id") Long id,
            @ModelAttribute CustomerDto request) {
        sanitizationService.sanitize(request);
        CustomerResponse response = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/get")
    public ResponseEntity<GetCustomerResponse> getCustomers() {
        GetCustomerResponse response = customerService.getCustomers();
        log.info("GetCustomers returned {} customers", response.getCustomers().size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/page")
    public ResponseEntity<Page<CustomerDto>> getCustomersPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String search) {
        search = sanitizationService.sanitize(search);
        Page<CustomerDto> response = customerService.getCustomersPage(page, size, search);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerDto> getCustomerById(@PathVariable("id") Long custId) {
        CustomerDto response = customerService.getCustomerById(custId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getCountTotalCustomers() {
        long count = customerService.getCountTotalCustomers();
        return ResponseEntity.ok(count);
    }
}
