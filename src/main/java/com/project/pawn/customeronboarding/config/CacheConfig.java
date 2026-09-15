package com.project.pawn.customeronboarding.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    //I don't want to cache images in client cache.
    @Bean
    public CaffeineCacheManager cacheManager(CustomerCacheLoader customerCacheLoader) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("customers");
        cacheManager.setCacheLoader(customerCacheLoader);
        cacheManager.setCaffeine(
                Caffeine.newBuilder()
                        .refreshAfterWrite(Duration.ofMinutes(15))
                        .expireAfterWrite(Duration.ofHours(24))
                        .maximumSize(10_000)
        );
        return cacheManager;
    }
}
