package com.example.demo.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Cấu hình Spring Cache với Caffeine
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        // Sử dụng ConcurrentMapCacheManager đơn giản
        // Trong production có thể thay bằng Redis hoặc Caffeine
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(java.util.Arrays.asList(
            "adminAiInsights",
            "provinces",
            "districts", 
            "wards",
            "wardsByProvince"
        ));
        return cacheManager;
    }
}
