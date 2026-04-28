package com.project.pawn.customeronboarding.repository.cache;

import com.project.pawn.customeronboarding.dto.CustomerDto;
import com.project.pawn.customeronboarding.dto.GetCustomerResponse;
import com.project.pawn.customeronboarding.mapper.ModelToDto;
import com.project.pawn.customeronboarding.repository.CustomerRepository;
import com.project.pawn.customeronboarding.repository.IdProofRepository;
import com.project.pawn.customeronboarding.service.CustomerCacheHandler;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerCacheRepository {

    private final ModelToDto modelToDto;
    private final CustomerRepository customerRepository;
    private final IdProofRepository idProofRepository;
    private final CustomerCacheHandler customerCacheHandler;

    /**
     * Warms up all related caches with data from the database on application startup.
     * This method is triggered after the Spring application context is initialized.
     */
    @EventListener(ContextRefreshedEvent.class)
    @Transactional
    public void warmUpCaches() {
        log.info("Warming up caches on application startup...");

        customerRepository.findAll().forEach(customer -> {
            Long custId = customer.getCustId();
            customerCacheHandler.putCustomerInCache(custId, modelToDto.mapToFullCustomerDTO(customer));
        });

        log.info("Cache warming complete.");
    }

    public Optional<CustomerDto> getCustomer(Long custId) {
        return customerCacheHandler.getCustomer(custId);
    }

    public boolean existsByCustId(Long custId) {
        return customerCacheHandler.getCustomer(custId).isPresent();
    }

    public boolean existsByIdProofNumber(String idNumber) {
        if (idNumber == null) {
            return false;
        }
        return idProofRepository.existsByIdNumber(idNumber);
    }

    public GetCustomerResponse getCustomersFromCache() {
        log.info("Fetching all customers from cache");

        List<Long> custIds = customerCacheHandler.getAllKeys();
        List<CustomerDto> customers = custIds.stream()
                .map(customerCacheHandler::getCustomer)
                .flatMap(Optional::stream)
                .toList();

        return GetCustomerResponse.builder()
                .customers(customers)
                .build();
    }
}
