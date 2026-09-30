package com.example.demo.dto;

import com.example.demo.entity.enums.IntentType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DTO representing AI analysis result from Gemini
 * Contains intent, confidence, response, and tool requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiAnalysisResult {

    private String intent; // Will be converted to IntentType
    private Double confidence;
    private String reply;
    
    @JsonProperty("suggest_handoff")
    private Boolean suggestHandoff = false;
    
    @JsonProperty("need_handoff")
    private Boolean needHandoff = false;
    
    @JsonProperty("tool_requests")
    private List<ToolRequest> toolRequests = new ArrayList<>();
    
    @JsonProperty("product_suggestions")
    private List<ProductSuggestion> productSuggestions = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ToolRequest {
        private String name; // product_search, shipping_fee, promotion_lookup, store_info
        private Map<String, Object> args;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProductSuggestion {
        private String name;
        private BigDecimal price;
        
        @JsonProperty("image_url")
        private String imageUrl;
        
        private String description;
        
        @JsonProperty("product_id")
        private Long productId;
    }

    /**
     * Get intent as enum
     */
    public IntentType getIntentType() {
        return IntentType.fromString(intent);
    }

    /**
     * Get confidence as BigDecimal
     */
    public BigDecimal getConfidenceAsBigDecimal() {
        if (confidence == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(confidence);
    }

    /**
     * Check if AI is confident enough to respond
     */
    public boolean isConfidentEnough(double threshold) {
        return confidence != null && confidence >= threshold;
    }

    /**
     * Check if handoff is required
     */
    public boolean requiresHandoff() {
        return needHandoff != null && needHandoff;
    }

    /**
     * Check if handoff is suggested
     */
    public boolean suggestsHandoff() {
        return suggestHandoff != null && suggestHandoff;
    }

    /**
     * Check if any tools were requested
     */
    public boolean hasToolRequests() {
        return toolRequests != null && !toolRequests.isEmpty();
    }

    /**
     * Check if product suggestions were provided
     */
    public boolean hasProductSuggestions() {
        return productSuggestions != null && !productSuggestions.isEmpty();
    }
}

