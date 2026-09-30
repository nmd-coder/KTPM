package com.example.demo.service.cache;

import com.example.demo.dto.ProductSuggestionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache for product recommendations
 * Provides fast access to frequently searched products with TTL
 */
@Component
@Slf4j
public class ProductRecommendationCache {

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final int defaultTtlMinutes = 5; // Default TTL: 5 minutes

    /**
     * Cache entry with TTL
     */
    private static class CacheEntry {
        private final List<ProductSuggestionDTO> products;
        private final LocalDateTime expiry;

        public CacheEntry(List<ProductSuggestionDTO> products, int ttlMinutes) {
            this.products = new ArrayList<>(products);
            this.expiry = LocalDateTime.now().plusMinutes(ttlMinutes);
        }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiry);
        }

        public List<ProductSuggestionDTO> getProducts() {
            return new ArrayList<>(products); // Return copy to prevent modification
        }
    }

    /**
     * Get cached products for search query
     */
    public List<ProductSuggestionDTO> get(String query, BigDecimal maxPrice) {
        String key = generateKey(query, maxPrice);
        CacheEntry entry = cache.get(key);
        
        if (entry == null) {
            log.debug("Cache MISS for key: {}", key);
            return null;
        }
        
        if (entry.isExpired()) {
            cache.remove(key);
            log.debug("Cache EXPIRED for key: {}", key);
            return null;
        }
        
        log.debug("Cache HIT for key: {} ({} products)", key, entry.getProducts().size());
        return entry.getProducts();
    }

    /**
     * Put products in cache
     */
    public void put(String query, BigDecimal maxPrice, List<ProductSuggestionDTO> products) {
        put(query, maxPrice, products, defaultTtlMinutes);
    }

    /**
     * Put products in cache with custom TTL
     */
    public void put(String query, BigDecimal maxPrice, List<ProductSuggestionDTO> products, int ttlMinutes) {
        if (products == null || products.isEmpty()) {
            return; // Don't cache empty results
        }
        
        String key = generateKey(query, maxPrice);
        CacheEntry entry = new CacheEntry(products, ttlMinutes);
        cache.put(key, entry);
        
        log.debug("Cache PUT for key: {} ({} products, TTL: {}min)", key, products.size(), ttlMinutes);
    }

    /**
     * Get popular/top products (special cache key)
     */
    public List<ProductSuggestionDTO> getPopularProducts() {
        return get("__POPULAR__", null);
    }

    /**
     * Cache popular/top products
     */
    public void putPopularProducts(List<ProductSuggestionDTO> products) {
        put("__POPULAR__", null, products, 10); // Longer TTL for popular products
    }

    /**
     * Invalidate cache entry
     */
    public void invalidate(String query, BigDecimal maxPrice) {
        String key = generateKey(query, maxPrice);
        cache.remove(key);
        log.debug("Cache INVALIDATED for key: {}", key);
    }

    /**
     * Invalidate all cache entries (for flash sales, promotions, etc.)
     */
    public void invalidateAll() {
        int size = cache.size();
        cache.clear();
        log.info("Cache CLEARED all {} entries", size);
    }

    /**
     * Invalidate entries matching pattern
     */
    public void invalidateByPattern(String pattern) {
        List<String> keysToRemove = new ArrayList<>();
        for (String key : cache.keySet()) {
            if (key.toLowerCase().contains(pattern.toLowerCase())) {
                keysToRemove.add(key);
            }
        }
        
        for (String key : keysToRemove) {
            cache.remove(key);
        }
        
        log.info("Cache INVALIDATED {} entries matching pattern: {}", keysToRemove.size(), pattern);
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
            log.debug("Cache CLEANUP removed {} expired entries", expiredKeys.size());
        }
    }

    /**
     * Generate cache key from query and price
     */
    private String generateKey(String query, BigDecimal maxPrice) {
        String normalizedQuery = query == null ? "" : query.toLowerCase().trim();
        String priceKey = maxPrice == null ? "NO_LIMIT" : maxPrice.toString();
        return normalizedQuery + "|" + priceKey;
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
            return String.format("CacheStats{active=%d, expired=%d, total=%d}", 
                activeEntries, expiredEntries, totalEntries);
        }
    }
}
