package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for grouping reviews by order
 * Used in admin panel to display reviews grouped by order instead of individual products
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderReviewGroupDTO {
    
    /**
     * Order information
     */
    private String orderId;
    private LocalDateTime orderDate;
    private String customerName;
    private String customerEmail;
    private Long customerId;
    
    /**
     * Review aggregation data
     */
    private Double averageRating;
    private String commonComment; // The comment from user (same for all products in order)
    private List<ReviewResponse> reviews; // Individual reviews for each product
    private int totalProducts;
    
    /**
     * Admin response data (applied to all reviews in the order)
     */
    private String adminResponse;
    private LocalDateTime adminResponseAt;
    private String adminResponseBy;
    private boolean hasAdminResponse;
    
    /**
     * Additional metadata
     */
    private LocalDateTime createdAt; // When the first review was created
    private String sentiment; // Overall sentiment (derived from individual reviews)
    private boolean verified; // If all reviews are verified (have orderItem)
    private boolean hasMedia; // If any review has media files
    
    /**
     * Constructor for easy creation from review data
     */
    public OrderReviewGroupDTO(String orderId, LocalDateTime orderDate, String customerName, 
                              String customerEmail, Long customerId, String commonComment,
                              List<ReviewResponse> reviews) {
        this.orderId = orderId;
        this.orderDate = orderDate;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.customerId = customerId;
        this.commonComment = commonComment;
        this.reviews = reviews;
        this.totalProducts = reviews != null ? reviews.size() : 0;
        
        // Calculate average rating
        if (reviews != null && !reviews.isEmpty()) {
            this.averageRating = reviews.stream()
                .mapToInt(ReviewResponse::getRating)
                .average()
                .orElse(0.0);
            
            // Set admin response data from first review (they should all be the same)
            ReviewResponse firstReview = reviews.get(0);
            this.adminResponse = firstReview.getAdminResponse();
            this.adminResponseAt = firstReview.getAdminResponseAt();
            this.adminResponseBy = firstReview.getAdminResponseByName();
            this.hasAdminResponse = firstReview.getAdminResponse() != null;
            this.createdAt = firstReview.getCreatedAt();
            this.sentiment = "POSITIVE"; // Default sentiment, will be calculated later
            this.verified = reviews.stream().allMatch(ReviewResponse::isVerified);
            this.hasMedia = reviews.stream().anyMatch(review -> 
                review.getMediaUrls() != null && !review.getMediaUrls().trim().isEmpty());
        } else {
            this.averageRating = 0.0;
            this.hasAdminResponse = false;
            this.verified = false;
            this.hasMedia = false;
        }
    }
    
    /**
     * Get formatted customer name
     */
    public String getFormattedCustomerName() {
        return customerName != null ? customerName : "Khách hàng";
    }
    
    /**
     * Get formatted average rating
     */
    public String getFormattedAverageRating() {
        return averageRating != null ? String.format("%.1f", averageRating) : "0.0";
    }
    
    /**
     * Get product names as comma-separated string
     */
    public String getProductNames() {
        if (reviews == null || reviews.isEmpty()) {
            return "";
        }
        
        return reviews.stream()
            .map(ReviewResponse::getProductName)
            .reduce((a, b) -> a + ", " + b)
            .orElse("");
    }
    
    /**
     * Check if this order group has any reviews
     */
    public boolean hasReviews() {
        return reviews != null && !reviews.isEmpty();
    }
    
    /**
     * Get the number of products with specific rating
     */
    public long getProductCountByRating(int rating) {
        if (reviews == null) {
            return 0;
        }
        
        return reviews.stream()
            .mapToInt(ReviewResponse::getRating)
            .filter(r -> r == rating)
            .count();
    }
}
