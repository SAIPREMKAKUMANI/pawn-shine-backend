package com.project.pawn.customeronboarding.config;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.project.pawn.customeronboarding.mapper.ModelToDto;
import com.project.pawn.customeronboarding.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerCacheLoader implements CacheLoader<Object, Object> {

    private final CustomerRepository customerRepository;
    private final ModelToDto modelToDto;

    @Override
    public @Nullable Object load(@NonNull Object key) throws Exception {
        if (key instanceof Long custId) {
            log.info("CacheLoader: Reloading customer ID {} from database", custId);
            return customerRepository.findByCustId(custId)
                    .map(modelToDto::mapToFullCustomerDTO)
                    .orElse(null);
        }
        
        log.warn("CacheLoader: Unrecognized cache key type: {}", key.getClass());
        return null;
    }
}
