package com.project.pawn.customeronboarding.service;

import com.project.pawn.customeronboarding.dto.CustomerDto;
import com.project.pawn.customeronboarding.dto.CustomerResponse;
import com.project.pawn.customeronboarding.dto.GetCustomerResponse;
import com.project.pawn.customeronboarding.exception.CustomerValidationException;
import com.project.pawn.customeronboarding.model.CustomerInfo;
import com.project.pawn.customeronboarding.repository.CustomerRepository;
import com.project.pawn.customeronboarding.repository.cache.CustomerCacheRepository;
import com.project.pawn.customeronboarding.util.ImageHandlingService;
import com.project.pawn.customeronboarding.validation.CustomerValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.project.pawn.customeronboarding.mapper.ModelToDto;

import static com.project.pawn.customeronboarding.constants.Constant.SUCCESS;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerCacheRepository customerCacheRepository;
    private final CustomerRepository customerRepository;
    private final CustomerValidator customerValidator;
    private final CustomerCacheHandler customerCacheHandler;
    private final ImageHandlingService imageService;
    private final ModelToDto modelToDto;

    @Transactional
    public CustomerResponse onboardCustomer(CustomerDto request) {
        log.info("Starting customer onboarding process for customer: {}", request.getName());

        customerValidator.validateCustomer(request);

        Long custId = customerCacheHandler.saveCustomerAndGetId(request);
        imageService.uploadAllImagesToDisk(request, custId);

        log.info("Successfully onboarded customer: {}, id: {}", request.getName(), custId);

        return CustomerResponse.builder()
                .customerId(custId)
                .status(SUCCESS)
                .message("Customer onboarded successfully")
                .build();
    }

    @Transactional
    public CustomerResponse updateCustomer(Long id, CustomerDto request) {
        log.info("Starting customer update process for customer ID: {}", id);

        customerValidator.validateCustomer(request);

        if (!customerCacheRepository.existsByCustId(id)) {
            throw new CustomerValidationException("Customer with ID " + id + " not found");
        }

        imageService.uploadAllImagesToDisk(request, id);
        customerCacheHandler.updateCustomer(id, request);

        log.info("Successfully updated customer with ID: {}", id);

        return CustomerResponse.builder()
                .customerId(id)
                .status(SUCCESS)
                .message("Customer updated successfully")
                .build();
    }

    public GetCustomerResponse getCustomers() {
        return customerCacheRepository.getCustomersFromCache();
    }

    public CustomerDto getCustomerById(Long custId) {
        log.info("Fetching details of the customer: {}", custId);
        return customerCacheRepository.getCustomer(custId)
                .orElseThrow(() -> new CustomerValidationException("Customer: " + custId + " not found"));
    }

    public long getCountTotalCustomers() {
        return customerRepository.count();
    }

    public Page<CustomerDto> getCustomersPage(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CustomerInfo> customerPage;
        if (search != null && !search.isBlank()) {
            customerPage = customerRepository.findByNameContainingIgnoreCase(search, pageable);
        } else {
            customerPage = customerRepository.findAll(pageable);
        }
        return customerPage.map(modelToDto::mapToFullCustomerDTO);
    }
}
