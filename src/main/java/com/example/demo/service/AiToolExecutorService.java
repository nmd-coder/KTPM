package com.example.demo.service;

import com.example.demo.dto.AiAnalysisResult;
import com.example.demo.dto.ProductSuggestionDTO;
import com.example.demo.dto.PromotionSummaryDTO;
import com.example.demo.dto.ShippingFeeEstimate;
import com.example.demo.service.cache.ProductRecommendationCache;
import com.example.demo.service.cache.ShippingFeeCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Service for executing AI tool requests
 * Handles product search, shipping fee, promotions, etc.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiToolExecutorService {

    private final ProductService productService;
    private final VoucherService voucherService;
    private final StoreConfigService storeConfigService;
    private final AiShippingService aiShippingService;
    private final ProductRecommendationCache productCache;
    private final ShippingFeeCache shippingCache;
    
    // Timeboxing configuration (700-900ms per tool)
    private static final int TOOL_TIMEOUT_MS = 800;
    private static final int PARALLEL_TIMEOUT_MS = 900;

    /**
     * Execute all tool requests from AI analysis - ENHANCED with parallel execution
     */
    public String executeTools(AiAnalysisResult analysis) {
        if (!analysis.hasToolRequests()) {
            return "";
        }

        long startTime = System.currentTimeMillis();
        
        // Check if we can run product_search and shipping_fee in parallel
        List<AiAnalysisResult.ToolRequest> toolRequests = analysis.getToolRequests();
        boolean hasProductSearch = toolRequests.stream().anyMatch(t -> "product_search".equals(t.getName()));
        boolean hasShippingFee = toolRequests.stream().anyMatch(t -> "shipping_fee".equals(t.getName()));
        
        if (hasProductSearch && hasShippingFee) {
            log.debug("🚀 PARALLEL execution: product_search + shipping_fee");
            return executeToolsInParallel(analysis);
        } else {
            log.debug("📋 SEQUENTIAL execution: {} tools", toolRequests.size());
            return executeToolsSequentially(analysis);
        }
    }

    /**
     * Execute tools in parallel with timeboxing (product_search + shipping_fee)
     */
    private String executeToolsInParallel(AiAnalysisResult analysis) {
        long startTime = System.currentTimeMillis();
        StringBuilder results = new StringBuilder();
        
        // Find product_search and shipping_fee requests
        AiAnalysisResult.ToolRequest productSearchRequest = null;
        AiAnalysisResult.ToolRequest shippingFeeRequest = null;
        
        for (AiAnalysisResult.ToolRequest request : analysis.getToolRequests()) {
            if ("product_search".equals(request.getName())) {
                productSearchRequest = request;
            } else if ("shipping_fee".equals(request.getName())) {
                shippingFeeRequest = request;
            }
        }
        
        // Launch parallel executions
        CompletableFuture<String> productFuture = null;
        CompletableFuture<String> shippingFuture = null;
        
        if (productSearchRequest != null) {
            final AiAnalysisResult.ToolRequest finalProductRequest = productSearchRequest;
            productFuture = CompletableFuture.supplyAsync(() -> {
                return executeProductSearchWithCache(finalProductRequest.getArgs());
            });
        }
        
        if (shippingFeeRequest != null) {
            final AiAnalysisResult.ToolRequest finalShippingRequest = shippingFeeRequest;
            shippingFuture = CompletableFuture.supplyAsync(() -> {
                return executeShippingFeeWithCache(finalShippingRequest.getArgs());
            });
        }
        
        // Wait for completion with timeout
        try {
            // Product search result
            if (productFuture != null) {
                try {
                    String productResult = productFuture.get(PARALLEL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                    if (productResult != null && !productResult.isEmpty()) {
                        results.append(productResult).append("\n\n");
                    }
                } catch (TimeoutException e) {
                    log.warn("⏰ Product search timed out after {}ms", PARALLEL_TIMEOUT_MS);
                    productFuture.cancel(true);
                    results.append("TOOL_RESULT: Tìm kiếm sản phẩm đang xử lý, mình sẽ gửi thông tin sớm nhất.\n\n");
                }
            }
            
            // Shipping fee result
            if (shippingFuture != null) {
                try {
                    String shippingResult = shippingFuture.get(PARALLEL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                    if (shippingResult != null && !shippingResult.isEmpty()) {
                        results.append(shippingResult).append("\n\n");
                    }
                } catch (TimeoutException e) {
                    log.warn("⏰ Shipping fee calculation timed out after {}ms", PARALLEL_TIMEOUT_MS);
                    shippingFuture.cancel(true);
                    results.append("Phí ship dự kiến 25.000-50.000₫ (xác nhận lại ngay khi có số cụ thể).\n\n");
                }
            }
            
        } catch (Exception e) {
            log.error("Error in parallel tool execution", e);
        }
        
        // Execute remaining tools sequentially
        for (AiAnalysisResult.ToolRequest request : analysis.getToolRequests()) {
            if (!"product_search".equals(request.getName()) && !"shipping_fee".equals(request.getName())) {
                try {
                    String result = executeTool(request.getName(), request.getArgs());
                    if (result != null && !result.isEmpty()) {
                        results.append(result).append("\n\n");
                    }
                } catch (Exception e) {
                    log.error("Error executing tool: {}", request.getName(), e);
                }
            }
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        log.info("🎯 PARALLEL execution completed in {}ms", totalTime);
        
        return results.toString();
    }

    /**
     * Execute tools sequentially (fallback)
     */
    private String executeToolsSequentially(AiAnalysisResult analysis) {
        StringBuilder results = new StringBuilder();
        
        for (AiAnalysisResult.ToolRequest toolRequest : analysis.getToolRequests()) {
            String toolName = toolRequest.getName();
            Map<String, Object> args = toolRequest.getArgs();
            
            log.debug("Executing tool: {} with args: {}", toolName, args);
            
            try {
                String result = executeTool(toolName, args);
                if (result != null && !result.isEmpty()) {
                    results.append(result).append("\n\n");
                }
            } catch (Exception e) {
                log.error("Error executing tool: {}", toolName, e);
            }
        }
        
        return results.toString();
    }

    /**
     * Execute single tool
     */
    private String executeTool(String toolName, Map<String, Object> args) {
        switch (toolName.toLowerCase()) {
            case "product_search":
                return executeProductSearch(args);
            case "shipping_fee":
                return executeShippingFee(args);
            case "promotion_lookup":
                return executePromotionLookup();
            case "store_info":
                return executeStoreInfo();
            default:
                log.warn("Unknown tool: {}", toolName);
                return null;
        }
    }

    /**
     * Execute product search tool
     */
    private String executeProductSearch(Map<String, Object> args) {
        try {
            String query = (String) args.get("query");
            Object priceMaxObj = args.get("price_max");
            BigDecimal priceMax = null;
            
            if (priceMaxObj != null) {
                if (priceMaxObj instanceof Number) {
                    priceMax = BigDecimal.valueOf(((Number) priceMaxObj).doubleValue());
                } else if (priceMaxObj instanceof String) {
                    priceMax = new BigDecimal((String) priceMaxObj);
                }
            }
            
            log.info("Searching products: query={}, priceMax={}", query, priceMax);
            
            // Search products - get more results for AI to analyze
            List<ProductSuggestionDTO> products = searchProductsForAi(query, priceMax, 5);
            
            if (products.isEmpty()) {
                // Try broader search without the specific query
                log.debug("No exact match, trying broader search");
                products = searchProductsForAi("", priceMax, 5);
            }
            
            if (products.isEmpty()) {
                return "TOOL_RESULT: Không tìm thấy sản phẩm nào trong hệ thống.";
            }
            
            // Return clean, consultation-focused data for AI
            StringBuilder result = new StringBuilder();
            result.append("SẢN PHẨM GỢI Ý CHO TƯ VẤN:\n\n");
            
            for (ProductSuggestionDTO product : products) {
                result.append("• **").append(product.getName()).append("**\n");
                result.append("  Giá: ").append(product.getFormattedPrice()).append("\n");
                
                // Only include meaningful description (not technical specs)
                if (product.getDescription() != null && !product.getDescription().isEmpty()) {
                    String cleanDesc = cleanDescription(product.getDescription());
                    if (!cleanDesc.isEmpty()) {
                        result.append("  Đặc điểm: ").append(cleanDesc).append("\n");
                    }
                }
                
                if (product.getImageUrl() != null) {
                    // Include product metadata for add-to-cart/wishlist functionality
                    result.append("  ![").append(product.getName())
                          .append("](").append(product.getFullImageUrl())
                          .append(")<!--product:")
                          .append(product.getId()).append(",")
                          .append(product.getPrice()).append("-->\n");
                }
                
                result.append("\n");
            }
            
            // Remove the consultation instruction - this should not be visible to customers
            // The AI will handle product consultation internally
            
            return result.toString();
            
        } catch (Exception e) {
            log.error("Error in product search tool", e);
            return "Lỗi khi tìm kiếm sản phẩm.";
        }
    }

    /**
     * Clean product description by removing technical specifications
     */
    private String cleanDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            return "";
        }
        
        String cleaned = description.trim();
        
        // Remove technical specifications that aren't useful for consultation
        cleaned = cleaned.replaceAll("(?i)\\b(SKU|ID|Mã)\\s*[:\\-]?\\s*[A-Z0-9]+\\b", "");
        cleaned = cleaned.replaceAll("(?i)\\b(Số lượng|Stock|Quantity)\\s*[:\\-]?\\s*\\d+\\b", "");
        cleaned = cleaned.replaceAll("(?i)\\b(Danh mục|Category)\\s*[:\\-]?\\s*[^.]*\\.", "");
        cleaned = cleaned.replaceAll("(?i)\\b(Kích thước|Size)\\s*[:\\-]?\\s*\\d+x\\d+x?\\d*\\s*(cm|mm)\\b", "");
        
        // Clean up extra spaces and punctuation
        cleaned = cleaned.replaceAll("\\s+", " ");
        cleaned = cleaned.replaceAll("\\s*[,;]\\s*$", "");
        
        return cleaned.trim();
    }

    /**
     * Search products for AI
     */
    private List<ProductSuggestionDTO> searchProductsForAi(String query, BigDecimal maxPrice, int limit) {
        return productService.searchForAi(query, maxPrice, limit);
    }

    /**
     * Execute shipping fee tool
     */
    private String executeShippingFee(Map<String, Object> args) {
        try {
            String toLocation = (String) args.get("to_location");
            
            if (toLocation == null || toLocation.trim().isEmpty()) {
                return "Bạn cho mình biết địa chỉ giao hàng để tính phí ship nhé!";
            }
            
            log.info("Calculating shipping fee to: {}", toLocation);
            
            ShippingFeeEstimate estimate = aiShippingService.calculateShippingFeeForAi(toLocation);
            
            if (!estimate.getSuccess()) {
                return estimate.getErrorMessage();
            }
            
            return aiShippingService.formatEstimateAsText(estimate);
            
        } catch (Exception e) {
            log.error("Error in shipping fee tool", e);
            return "Không thể tính phí ship lúc này.";
        }
    }

    /**
     * Execute promotion lookup tool
     */
    private String executePromotionLookup() {
        try {
            log.info("Looking up active promotions");
            
            List<PromotionSummaryDTO> promotions = voucherService.getActivePromotionsForAi();
            
            if (promotions.isEmpty()) {
                return "Hiện tại chưa có chương trình khuyến mãi nào. Bạn theo dõi fanpage để cập nhật ưu đãi mới nhất nhé!";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("**Khuyến mãi hiện tại:**\n\n");
            
            for (PromotionSummaryDTO promo : promotions) {
                result.append(String.format("🎁 **%s** - Mã: `%s`\n", 
                    promo.getName(), promo.getCode()));
                result.append(String.format("   %s\n", promo.getFormattedDiscount()));
                result.append(String.format("   %s\n", promo.getFormattedMinOrder()));
                result.append(String.format("   %s\n\n", promo.getExpiryText()));
            }
            
            return result.toString();
            
        } catch (Exception e) {
            log.error("Error in promotion lookup tool", e);
            return "Không thể tra cứu khuyến mãi lúc này.";
        }
    }

    /**
     * Execute store info tool
     */
    private String executeStoreInfo() {
        try {
            log.info("Getting store information");
            
            String storeInfo = storeConfigService.getStoreInfoText();
            String policies = storeConfigService.getPoliciesText();
            
            return storeInfo + "\n" + policies;
            
        } catch (Exception e) {
            log.error("Error in store info tool", e);
            return "Không thể lấy thông tin cửa hàng lúc này.";
        }
    }

    /**
     * Execute product search with cache integration
     */
    private String executeProductSearchWithCache(Map<String, Object> args) {
        try {
            String query = (String) args.get("query");
            Object priceMaxObj = args.get("price_max");
            BigDecimal priceMax = null;
            
            if (priceMaxObj != null) {
                if (priceMaxObj instanceof Number) {
                    priceMax = BigDecimal.valueOf(((Number) priceMaxObj).doubleValue());
                } else if (priceMaxObj instanceof String) {
                    priceMax = new BigDecimal((String) priceMaxObj);
                }
            }
            
            log.debug("🔍 Product search with cache: query={}, priceMax={}", query, priceMax);
            
            // Check cache first
            List<ProductSuggestionDTO> cachedProducts = productCache.get(query, priceMax);
            List<ProductSuggestionDTO> products;
            
            if (cachedProducts != null) {
                log.debug("📦 Cache HIT for products: {}", query);
                products = cachedProducts;
            } else {
                log.debug("🔄 Cache MISS, searching database: {}", query);
                // Search products - get more results for AI to analyze
                products = searchProductsForAi(query, priceMax, 5);
                
                if (products.isEmpty()) {
                    // Try broader search without the specific query
                    log.debug("No exact match, trying broader search");
                    products = searchProductsForAi("", priceMax, 5);
                }
                
                // Cache the results
                if (!products.isEmpty()) {
                    productCache.put(query, priceMax, products);
                }
            }
            
            if (products.isEmpty()) {
                return "SẢN PHẨM GỢI Ý CHO TƯ VẤN: Không tìm thấy sản phẩm nào trong hệ thống.";
            }
            
            // Return clean, consultation-focused data for AI (same as non-cached version)
            StringBuilder result = new StringBuilder();
            result.append("SẢN PHẨM GỢI Ý CHO TƯ VẤN:\n\n");
            
            for (ProductSuggestionDTO product : products) {
                result.append("• **").append(product.getName()).append("**\n");
                result.append("  Giá: ").append(product.getFormattedPrice()).append("\n");
                
                // Only include meaningful description (not technical specs)
                if (product.getDescription() != null && !product.getDescription().isEmpty()) {
                    String cleanDesc = cleanDescription(product.getDescription());
                    if (!cleanDesc.isEmpty()) {
                        result.append("  Đặc điểm: ").append(cleanDesc).append("\n");
                    }
                }
                
                if (product.getImageUrl() != null) {
                    // Include product metadata for add-to-cart/wishlist functionality
                    result.append("  ![").append(product.getName())
                          .append("](").append(product.getFullImageUrl())
                          .append(")<!--product:")
                          .append(product.getId()).append(",")
                          .append(product.getPrice()).append("-->\n");
                }
                
                result.append("\n");
            }
            
            // Remove the consultation instruction - this should not be visible to customers
            // The AI will handle product consultation internally
            
            return result.toString();
            
        } catch (Exception e) {
            log.error("Error in cached product search", e);
            return "Lỗi khi tìm kiếm sản phẩm.";
        }
    }

    /**
     * Execute shipping fee calculation with cache integration
     */
    private String executeShippingFeeWithCache(Map<String, Object> args) {
        try {
            String toLocation = (String) args.get("to_location");
            
            if (toLocation == null || toLocation.trim().isEmpty()) {
                return "Bạn cho mình biết địa chỉ giao hàng để tính phí ship nhé!";
            }
            
            log.debug("🚚 Shipping fee calculation with cache: {}", toLocation);
            
            // Check cache first
            ShippingFeeEstimate cachedEstimate = shippingCache.get(toLocation);
            ShippingFeeEstimate estimate;
            
            if (cachedEstimate != null) {
                log.debug("📦 Cache HIT for shipping: {}", toLocation);
                estimate = cachedEstimate;
            } else {
                log.debug("🔄 Cache MISS, calculating shipping fee: {}", toLocation);
                estimate = aiShippingService.calculateShippingFeeForAi(toLocation);
                
                // Cache successful results
                if (estimate.getSuccess()) {
                    shippingCache.put(toLocation, estimate);
                }
            }
            
            if (!estimate.getSuccess()) {
                return estimate.getErrorMessage();
            }
            
            return aiShippingService.formatEstimateAsText(estimate);
            
        } catch (Exception e) {
            log.error("Error in cached shipping fee calculation", e);
            return "Không thể tính phí ship lúc này.";
        }
    }

    /**
     * Invalidate product cache (for promotions, flash sales, etc.)
     */
    public void invalidateProductCache() {
        productCache.invalidateAll();
        log.info("🧹 Product cache invalidated for promotion/flash sale");
    }

    /**
     * Invalidate shipping cache (for rate changes)
     */
    public void invalidateShippingCache() {
        shippingCache.invalidateAll();
        log.info("🧹 Shipping cache invalidated for rate changes");
    }

    /**
     * Invalidate product cache by pattern (for specific categories)
     */
    public void invalidateProductCacheByPattern(String pattern) {
        productCache.invalidateByPattern(pattern);
        log.info("🧹 Product cache invalidated for pattern: {}", pattern);
    }

    /**
     * Get cache statistics for monitoring
     */
    public String getCacheStats() {
        ProductRecommendationCache.CacheStats productStats = productCache.getStats();
        ShippingFeeCache.CacheStats shippingStats = shippingCache.getStats();
        
        return String.format("Cache Stats - Products: %s, Shipping: %s", 
            productStats.toString(), shippingStats.toString());
    }

    /**
     * Cleanup expired cache entries
     */
    public void cleanupCaches() {
        productCache.cleanup();
        shippingCache.cleanup();
        log.debug("🧹 Cache cleanup completed");
    }

    /**
     * Enhance AI reply with tool execution results
     */
    public String enhanceReplyWithToolResults(AiAnalysisResult analysis) {
        String baseReply = analysis.getReply();
        String toolResults = executeTools(analysis);
        
        if (toolResults.isEmpty()) {
            return baseReply;
        }
        
        // Combine AI reply with tool results
        return baseReply + "\n\n" + toolResults;
    }
}
