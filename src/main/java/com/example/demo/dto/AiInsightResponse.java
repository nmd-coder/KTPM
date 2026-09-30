package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO cho AI response JSON structure
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiInsightResponse {
    
    @JsonProperty("insights")
    private List<InsightItem> insights;
    
    /**
     * Nested class cho từng insight item
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InsightItem {
        
        @JsonProperty("type")
        private String type; // "revenue" | "order" | "inventory" | "review"
        
        @JsonProperty("icon")
        private String icon; // emoji (📈📉⚠️⭐💰📦🚨)
        
        @JsonProperty("title")
        private String title; // tiêu đề ngắn (max 50 chars)
        
        @JsonProperty("message")
        private String message; // nội dung chi tiết với số liệu
        
        @JsonProperty("severity")
        private String severity; // "success" | "warning" | "danger" | "info"
        
        @JsonProperty("actionLink")
        private String actionLink; // (optional) link như "/admin/products"
        
        @JsonProperty("actionText")
        private String actionText; // (optional) text CTA như "Xem chi tiết"
    }
}
