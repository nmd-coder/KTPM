package com.example.demo.dto;

import com.example.demo.entity.Product;

/**
 * DTO for optimized product detail queries
 * Contains product information along with rating statistics
 */
public class ProductDetailDTO {
    private final Product product;
    private final Double averageRating;
    private final Long reviewCount;

    public ProductDetailDTO(Product product, Double averageRating, Long reviewCount) {
        this.product = product;
        this.averageRating = averageRating != null ? averageRating : 0.0;
        this.reviewCount = reviewCount != null ? reviewCount : 0L;
    }

    public Product getProduct() {
        return product;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public Long getReviewCount() {
        return reviewCount;
    }
}
