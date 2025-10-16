package com.project.pawn.customeronboarding.repository.cache;

import com.project.pawn.customeronboarding.dto.IdProofDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerCacheRepository {
    private final CaffeineCacheManager cacheManager;

    public boolean existsByIdProofs_IdNumber(String idNumber) {
        Cache idProofsCache = cacheManager.getCache("idProofs");

        com.github.benmanes.caffeine.cache.Cache<Object, Object> cache =
                (com.github.benmanes.caffeine.cache.Cache<Object, Object>) idProofsCache.getNativeCache();

        IdProofDto dto = (IdProofDto) cache.getIfPresent(idNumber);
        return dto != null;
    }
}
