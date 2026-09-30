package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for adding/removing wishlist items
 */
public class WishlistRequest {
    
    @NotNull(message = "Product ID is required")
    private Long productId;
    
    // Constructors
    public WishlistRequest() {
    }
    
    public WishlistRequest(Long productId) {
        this.productId = productId;
    }
    
    // Getters and Setters
    public Long getProductId() {
        return productId;
    }
    
    public void setProductId(Long productId) {
        this.productId = productId;
    }
}
