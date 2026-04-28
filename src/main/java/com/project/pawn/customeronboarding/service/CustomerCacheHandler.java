package com.project.pawn.customeronboarding.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.project.pawn.customeronboarding.dto.CustomerDto;
import com.project.pawn.customeronboarding.exception.GenericCustomerOnboardingException;
import com.project.pawn.customeronboarding.mapper.DtoToModel;
import com.project.pawn.customeronboarding.mapper.ModelToDto;
import com.project.pawn.customeronboarding.model.CustomerInfo;
import com.project.pawn.customeronboarding.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static com.project.pawn.customeronboarding.constants.Constant.ACTIVE;
import static com.project.pawn.customeronboarding.constants.Constant.USERNAME;

@Service
@RequiredArgsConstructor
@Slf4j
@CacheConfig(cacheNames = "customers")
public class CustomerCacheHandler {

    private final CustomerRepository customerRepository;
    private final CacheManager cacheManager;
    private final ModelToDto modelToDto;
    private final DtoToModel dtoToModel;

    /**
     * Saves a new customer to DB and returns the auto-generated ID.
     * Caches the DTO using the generated ID.
     */
    public Long saveCustomerAndGetId(CustomerDto customer) {
        try {
            CustomerInfo customerInfo = dtoToModel.toModel(customer);
            log.info("Converted Customer DTO to Entity for customer: {}", customer.getName());

            setAuditFields(customerInfo);
            customerInfo.setStatus(ACTIVE);

            CustomerInfo saved = customerRepository.save(customerInfo);
            Long generatedId = saved.getCustId();
            customer.setCustId(generatedId);

            putCustomerInCache(generatedId, customer);

            log.info("Saved customer with auto-generated ID: {}", generatedId);
            return generatedId;
        } catch (DataAccessException ex) {
            log.error("Database error while saving customer: {}", customer.getName(), ex);
            throw new GenericCustomerOnboardingException("Error accessing the database");
        }
    }

    /**
     * Updates an existing customer in DB and cache.
     */
    @CachePut(key = "#custId")
    public CustomerDto updateCustomer(Long custId, CustomerDto customer) {
        try {
            CustomerInfo customerInfo = dtoToModel.toModel(customer);
            log.info("Converted Customer DTO to Entity for update, ID: {}", custId);

            setAuditFields(customerInfo);
            customerInfo.setCustId(custId);

            customerRepository.save(customerInfo);
            log.info("Updated customer ID: {}", custId);
        } catch (DataAccessException ex) {
            log.error("Database error while updating customer ID: {}", custId, ex);
            throw new GenericCustomerOnboardingException("Error accessing the database");
        }
        return customer;
    }

    @Cacheable
    public Optional<CustomerDto> getCustomer(Long customerId) {
        try {
            return customerRepository.findByCustId(customerId)
                    .map(customerInfo -> {
                        log.info("Customer {} found in database", customerId);
                        return modelToDto.mapToFullCustomerDTO(customerInfo);
                    });
        } catch (DataAccessException ex) {
            log.error("Database error while retrieving customer ID: {}", customerId, ex);
            throw new GenericCustomerOnboardingException("Error accessing the database");
        }
    }

    /**
     * Cache initialization — stores a DTO in cache without DB interaction.
     */
    @CachePut(key = "#custId")
    public CustomerDto putCustomerInCache(Long custId, CustomerDto customer) {
        return customer;
    }

    public List<Long> getAllKeys() {
        CaffeineCacheManager caffeineCacheManager = (CaffeineCacheManager) cacheManager;
        CaffeineCache cache = (CaffeineCache) caffeineCacheManager.getCache("customers");
        Cache<Object, Object> caffeine = cache.getNativeCache();

        return caffeine.asMap().keySet().stream()
                .filter(key -> key instanceof Long)
                .map(key -> (Long) key)
                .toList();
    }

    private void setAuditFields(CustomerInfo entity) {
        String currentUser = MDC.get(USERNAME);
        entity.setCreatedBy(currentUser);
        entity.setUpdatedBy(currentUser);

        if (entity.getContacts() != null) {
            entity.getContacts().forEach(c -> {
                c.setCreatedBy(currentUser);
                c.setUpdatedBy(currentUser);
            });
        }
        if (entity.getAddresses() != null) {
            entity.getAddresses().forEach(a -> {
                a.setCreatedBy(currentUser);
                a.setUpdatedBy(currentUser);
            });
        }
        if (entity.getRelatives() != null) {
            entity.getRelatives().forEach(r -> {
                r.setCreatedBy(currentUser);
                r.setUpdatedBy(currentUser);
            });
        }
        if (entity.getIdProofs() != null) {
            entity.getIdProofs().forEach(ip -> {
                ip.setCreatedBy(currentUser);
                ip.setUpdatedBy(currentUser);
            });
        }
    }
}
