package com.project.pawn.customeronboarding.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.project.pawn.customeronboarding.dto.*;
import com.project.pawn.customeronboarding.exception.GenericCustomerOnboardingException;
import com.project.pawn.customeronboarding.mapper.DtoToModel;
import com.project.pawn.customeronboarding.mapper.ModelToDto;
import com.project.pawn.customeronboarding.model.*;
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
import java.util.Map;
import java.util.stream.Collectors;

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
    public CustomerDto updateCustomer(Long custId, CustomerDto customerDto) {
        try {
            CustomerInfo existingCustomer = customerRepository.findById(custId)
                    .orElseThrow(() -> new GenericCustomerOnboardingException("Customer not found"));

            // Map basic fields
            existingCustomer.setName(customerDto.getName());
            existingCustomer.setDateOfBirth(customerDto.getDateOfBirth());
            if (customerDto.getGender() != null) existingCustomer.setGender(customerDto.getGender().name());
            if (customerDto.getMaritalStatus() != null) existingCustomer.setMaritalStatus(customerDto.getMaritalStatus().name());
            existingCustomer.setOccupation(customerDto.getOccupation());

            if (customerDto.getImageUrl() != null) {
                existingCustomer.setImageUrl(customerDto.getImageUrl());
            }

            setAuditFields(existingCustomer);

            // Update collections
            updateContacts(existingCustomer, customerDto.getContacts());
            updateAddresses(existingCustomer, customerDto.getAddresses());
            updateIdProofs(existingCustomer, customerDto.getIdProofs());
            updateRelatives(existingCustomer, customerDto.getRelatives());

            CustomerInfo saved = customerRepository.save(existingCustomer);
            log.info("Updated customer ID: {}", custId);
            return modelToDto.mapToFullCustomerDTO(saved);
        } catch (DataAccessException ex) {
            log.error("Database error while updating customer ID: {}", custId, ex);
            throw new GenericCustomerOnboardingException("Error accessing the database");
        }
    }

    /**
     * Updates only image URLs for an existing customer in DB and cache.
     */
    @CachePut(key = "#custId")
    public CustomerDto updateImageUrls(Long custId, CustomerDto customerDto) {
        try {
            CustomerInfo existingCustomer = customerRepository.findById(custId)
                    .orElseThrow(() -> new GenericCustomerOnboardingException("Customer not found"));

            if (customerDto.getImageUrl() != null) {
                existingCustomer.setImageUrl(customerDto.getImageUrl());
            }

            // Update ID proof image URLs
            if (customerDto.getIdProofs() != null && existingCustomer.getIdProofs() != null) {
                for (int i = 0; i < customerDto.getIdProofs().size() && i < existingCustomer.getIdProofs().size(); i++) {
                    String url = customerDto.getIdProofs().get(i).getImageUrl();
                    if (url != null) {
                        existingCustomer.getIdProofs().get(i).setImageUrl(url);
                    }
                }
            }

            // Update relative image URLs
            if (customerDto.getRelatives() != null && existingCustomer.getRelatives() != null) {
                for (int i = 0; i < customerDto.getRelatives().size() && i < existingCustomer.getRelatives().size(); i++) {
                    String url = customerDto.getRelatives().get(i).getImageUrl();
                    if (url != null) {
                        existingCustomer.getRelatives().get(i).setImageUrl(url);
                    }
                }
            }

            CustomerInfo saved = customerRepository.save(existingCustomer);
            log.info("Persisted image URLs for customer ID: {}", custId);
            return modelToDto.mapToFullCustomerDTO(saved);
        } catch (DataAccessException ex) {
            log.error("Database error while updating image URLs for customer ID: {}", custId, ex);
            throw new GenericCustomerOnboardingException("Error accessing the database");
        }
    }

    private void updateContacts(CustomerInfo customer, List<ContactDto> contactDtos) {
        Map<Long, ContactInfo> existingContacts = customer.getContacts().stream()
                .collect(Collectors.toMap(ContactInfo::getContactId, c -> c));

        customer.getContacts().clear();

        if (contactDtos != null) {
            for (ContactDto dto : contactDtos) {
                if (dto.getContactId() != null && existingContacts.containsKey(dto.getContactId())) {
                    ContactInfo existing = existingContacts.get(dto.getContactId());
                    existing.setPhone(dto.getPhone());
                    existing.setSecondaryPhone(dto.getSecondaryPhone());
                    existing.setWhatsappPhone(dto.getWhatsappPhone());
                    existing.setEmail(dto.getEmail());
                    customer.getContacts().add(existing);
                } else {
                    ContactInfo newContact = dtoToModel.toModel(dto);
                    newContact.setCustomer(customer);
                    customer.getContacts().add(newContact);
                }
            }
        }
    }

    private void updateAddresses(CustomerInfo customer, List<AddressDto> addressDtos) {
        Map<Long, AddressInfo> existingAddresses = customer.getAddresses().stream()
                .collect(Collectors.toMap(AddressInfo::getAddressId, a -> a));

        customer.getAddresses().clear();

        if (addressDtos != null) {
            for (AddressDto dto : addressDtos) {
                if (dto.getAddressId() != null && existingAddresses.containsKey(dto.getAddressId())) {
                    AddressInfo existing = existingAddresses.get(dto.getAddressId());
                    existing.setStreet(dto.getStreet());
                    existing.setCity(dto.getCity());
                    existing.setState(dto.getState());
                    existing.setZipCode(dto.getPostalCode());
                    existing.setCountry(dto.getCountry());
                    customer.getAddresses().add(existing);
                } else {
                    AddressInfo newAddress = dtoToModel.toModel(dto);
                    newAddress.setCustomer(customer);
                    customer.getAddresses().add(newAddress);
                }
            }
        }
    }

    private void updateIdProofs(CustomerInfo customer, List<IdProofDto> idProofDtos) {
        Map<Long, IdProofInfo> existingProofs = customer.getIdProofs().stream()
                .collect(Collectors.toMap(IdProofInfo::getIdProofId, p -> p));

        customer.getIdProofs().clear();

        if (idProofDtos != null) {
            for (IdProofDto dto : idProofDtos) {
                if (dto.getIdProofId() != null && existingProofs.containsKey(dto.getIdProofId())) {
                    IdProofInfo existing = existingProofs.get(dto.getIdProofId());
                    existing.setIdType(dto.getIdType() != null ? dto.getIdType().name() : null);
                    existing.setIdNumber(dto.getIdNumber());
                    if (dto.getImageUrl() != null) {
                        existing.setImageUrl(dto.getImageUrl());
                    }
                    customer.getIdProofs().add(existing);
                } else {
                    IdProofInfo newProof = dtoToModel.toModel(dto);
                    newProof.setCustomer(customer);
                    customer.getIdProofs().add(newProof);
                }
            }
        }
    }

    private void updateRelatives(CustomerInfo customer, List<RelativeDto> relativeDtos) {
        Map<Long, RelativeInfo> existingRelatives = customer.getRelatives().stream()
                .collect(Collectors.toMap(RelativeInfo::getRelativeId, r -> r));

        customer.getRelatives().clear();

        if (relativeDtos != null) {
            for (RelativeDto dto : relativeDtos) {
                if (dto.getRelativeId() != null && existingRelatives.containsKey(dto.getRelativeId())) {
                    RelativeInfo existing = existingRelatives.get(dto.getRelativeId());
                    existing.setName(dto.getName());
                    existing.setRelationship(dto.getRelationship());
                    existing.setContactNumber(dto.getContactNumber());
                    if (dto.getImageUrl() != null) {
                        existing.setImageUrl(dto.getImageUrl());
                    }
                    customer.getRelatives().add(existing);
                } else {
                    RelativeInfo newRelative = dtoToModel.toModel(dto);
                    newRelative.setCustomer(customer);
                    customer.getRelatives().add(newRelative);
                }
            }
        }
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
