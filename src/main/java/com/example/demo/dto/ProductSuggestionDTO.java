package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for product suggestions from AI
 * Contains essential product information for chat display
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSuggestionDTO {
    
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private String productUrl; // Link to product detail page
    private Integer stockQuantity;
    private String catalogName;
    private Boolean available;

    /**
     * Create from product entity fields
     */
    public ProductSuggestionDTO(Long id, String name, BigDecimal price, String imageUrl) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.productUrl = "/products/" + id;
    }

    /**
     * Format price as Vietnamese currency
     */
    public String getFormattedPrice() {
        if (price == null) {
            return "0đ";
        }
        return String.format("%,.0fđ", price);
    }

    /**
     * Get full image URL
     */
    public String getFullImageUrl() {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return "/images/placeholder-product.jpg";
        }
        if (imageUrl.startsWith("http")) {
            return imageUrl;
        }
        return imageUrl;
    }
}

