package com.example.demo.service.cache;

import com.example.demo.dto.ShippingFeeEstimate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache for shipping fee estimates
 * Provides fast access to previously calculated shipping fees with TTL
 */
@Component
@Slf4j
public class ShippingFeeCache {

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final int defaultTtlMinutes = 30; // Default TTL: 30 minutes (shipping fees change less frequently)

    /**
     * Cache entry with TTL
     */
    private static class CacheEntry {
        private final ShippingFeeEstimate estimate;
        private final LocalDateTime expiry;

        public CacheEntry(ShippingFeeEstimate estimate, int ttlMinutes) {
            this.estimate = estimate;
            this.expiry = LocalDateTime.now().plusMinutes(ttlMinutes);
        }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiry);
        }

        public ShippingFeeEstimate getEstimate() {
            return estimate;
        }
    }

    /**
     * Get cached shipping fee estimate for location
     */
    public ShippingFeeEstimate get(String location) {
        String key = generateKey(location);
        CacheEntry entry = cache.get(key);
        
        if (entry == null) {
            log.debug("Shipping cache MISS for location: {}", location);
            return null;
        }
        
        if (entry.isExpired()) {
            cache.remove(key);
            log.debug("Shipping cache EXPIRED for location: {}", location);
            return null;
        }
        
        log.debug("Shipping cache HIT for location: {}", location);
        return entry.getEstimate();
    }

    /**
     * Put shipping fee estimate in cache
     */
    public void put(String location, ShippingFeeEstimate estimate) {
        put(location, estimate, defaultTtlMinutes);
    }

    /**
     * Put shipping fee estimate in cache with custom TTL
     */
    public void put(String location, ShippingFeeEstimate estimate, int ttlMinutes) {
        if (estimate == null || !estimate.getSuccess()) {
            return; // Don't cache failed estimates
        }
        
        String key = generateKey(location);
        CacheEntry entry = new CacheEntry(estimate, ttlMinutes);
        cache.put(key, entry);
        
        log.debug("Shipping cache PUT for location: {} (TTL: {}min)", location, ttlMinutes);
    }

    /**
     * Get shipping fee by city/province (bracketed lookup)
     */
    public ShippingFeeEstimate getByProvince(String province) {
        String key = generateProvinceKey(province);
        CacheEntry entry = cache.get(key);
        
        if (entry == null) {
            log.debug("Shipping cache MISS for province: {}", province);
            return null;
        }
        
        if (entry.isExpired()) {
            cache.remove(key);
            log.debug("Shipping cache EXPIRED for province: {}", province);
            return null;
        }
        
        log.debug("Shipping cache HIT for province: {}", province);
        return entry.getEstimate();
    }

    /**
     * Cache shipping fee by province (for bracketed fee lookups)
     */
    public void putByProvince(String province, ShippingFeeEstimate estimate) {
        putByProvince(province, estimate, defaultTtlMinutes);
    }

    /**
     * Cache shipping fee by province with custom TTL
     */
    public void putByProvince(String province, ShippingFeeEstimate estimate, int ttlMinutes) {
        if (estimate == null || !estimate.getSuccess()) {
            return;
        }
        
        String key = generateProvinceKey(province);
        CacheEntry entry = new CacheEntry(estimate, ttlMinutes);
        cache.put(key, entry);
        
        log.debug("Shipping cache PUT for province: {} (TTL: {}min)", province, ttlMinutes);
    }

    /**
     * Invalidate cache entry for specific location
     */
    public void invalidate(String location) {
        String key = generateKey(location);
        cache.remove(key);
        log.debug("Shipping cache INVALIDATED for location: {}", location);
    }

    /**
     * Invalidate all shipping cache entries (for rate changes, system updates)
     */
    public void invalidateAll() {
        int size = cache.size();
        cache.clear();
        log.info("Shipping cache CLEARED all {} entries", size);
    }

    /**
     * Invalidate entries for specific province/region
     */
    public void invalidateByRegion(String region) {
        String regionLower = region.toLowerCase().trim();
        List<String> keysToRemove = new ArrayList<>();
        
        for (String key : cache.keySet()) {
            if (key.toLowerCase().contains(regionLower)) {
                keysToRemove.add(key);
            }
        }
        
        for (String key : keysToRemove) {
            cache.remove(key);
        }
        
        log.info("Shipping cache INVALIDATED {} entries for region: {}", keysToRemove.size(), region);
    }

    /**
     * Get cache statistics
     */
    public CacheStats getStats() {
        int activeEntries = 0;
        int expiredEntries = 0;
        
        for (CacheEntry entry : cache.values()) {
            if (entry.isExpired()) {
                expiredEntries++;
            } else {
                activeEntries++;
            }
        }
        
        return new CacheStats(activeEntries, expiredEntries, cache.size());
    }

    /**
     * Clean up expired entries
     */
    public void cleanup() {
        List<String> expiredKeys = new ArrayList<>();
        
        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            if (entry.getValue().isExpired()) {
                expiredKeys.add(entry.getKey());
            }
        }
        
        for (String key : expiredKeys) {
            cache.remove(key);
        }
        
        if (!expiredKeys.isEmpty()) {
            log.debug("Shipping cache CLEANUP removed {} expired entries", expiredKeys.size());
        }
    }

    /**
     * Pre-warm cache with common locations
     */
    public void preWarmCommonLocations(Map<String, ShippingFeeEstimate> commonEstimates) {
        for (Map.Entry<String, ShippingFeeEstimate> entry : commonEstimates.entrySet()) {
            put(entry.getKey(), entry.getValue(), 60); // Longer TTL for pre-warmed data
        }
        log.info("Shipping cache PRE-WARMED with {} common locations", commonEstimates.size());
    }

    /**
     * Generate cache key from location
     */
    private String generateKey(String location) {
        if (location == null || location.trim().isEmpty()) {
            return "EMPTY_LOCATION";
        }
        
        // Normalize location: lowercase, trim, remove extra spaces
        return location.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    /**
     * Generate province-specific cache key
     */
    private String generateProvinceKey(String province) {
        return "PROVINCE:" + generateKey(province);
    }

    /**
     * Cache statistics
     */
    public static class CacheStats {
        public final int activeEntries;
        public final int expiredEntries;
        public final int totalEntries;

        public CacheStats(int activeEntries, int expiredEntries, int totalEntries) {
            this.activeEntries = activeEntries;
            this.expiredEntries = expiredEntries;
            this.totalEntries = totalEntries;
        }

        @Override
        public String toString() {
            return String.format("ShippingCacheStats{active=%d, expired=%d, total=%d}", 
                activeEntries, expiredEntries, totalEntries);
        }
    }
}
