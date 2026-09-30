package com.example.demo.dto;

import com.example.demo.entity.Product;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * DTO for Wishlist/Follow entity
 * Represents a user's wishlist product
 */
public class WishlistDTO {
    
    private Long id;
    private Long userId;
    private Long productId;
    private String productName;
    private String productDescription;
    private BigDecimal productPrice;
    private String productImage;
    private Integer productStockQuantity;
    private String productStatus;
    private Double productAverageRating;
    private LocalDateTime followedAt;
    
    // Constructors
    public WishlistDTO() {
    }
    
    public WishlistDTO(Long id, Long userId, Long productId, String productName, 
                      String productDescription, BigDecimal productPrice, String productImage,
                      Integer productStockQuantity, String productStatus, 
                      Double productAverageRating, LocalDateTime followedAt) {
        this.id = id;
        this.userId = userId;
        this.productId = productId;
        this.productName = productName;
        this.productDescription = productDescription;
        this.productPrice = productPrice;
        this.productImage = productImage;
        this.productStockQuantity = productStockQuantity;
        this.productStatus = productStatus;
        this.productAverageRating = productAverageRating;
        this.followedAt = followedAt;
    }
    
    // Static factory method to create from Product
    public static WishlistDTO fromProduct(Product product, Long userId, Long followId, LocalDateTime followedAt) {
        return new WishlistDTO(
            followId,
            userId,
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getImage(),
            product.getStockQuantity(),
            product.getStatus() != null ? product.getStatus().name() : null,
            product.getAverageRating(),
            followedAt
        );
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getProductId() {
        return productId;
    }
    
    public void setProductId(Long productId) {
        this.productId = productId;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public String getProductDescription() {
        return productDescription;
    }
    
    public void setProductDescription(String productDescription) {
        this.productDescription = productDescription;
    }
    
    public BigDecimal getProductPrice() {
        return productPrice;
    }
    
    public void setProductPrice(BigDecimal productPrice) {
        this.productPrice = productPrice;
    }
    
    public String getProductImage() {
        return productImage;
    }
    
    public void setProductImage(String productImage) {
        this.productImage = productImage;
    }
    
    public Integer getProductStockQuantity() {
        return productStockQuantity;
    }
    
    public void setProductStockQuantity(Integer productStockQuantity) {
        this.productStockQuantity = productStockQuantity;
    }
    
    public String getProductStatus() {
        return productStatus;
    }
    
    public void setProductStatus(String productStatus) {
        this.productStatus = productStatus;
    }
    
    public Double getProductAverageRating() {
        return productAverageRating;
    }
    
    public void setProductAverageRating(Double productAverageRating) {
        this.productAverageRating = productAverageRating;
    }
    
    public LocalDateTime getFollowedAt() {
        return followedAt;
    }
    
    public void setFollowedAt(LocalDateTime followedAt) {
        this.followedAt = followedAt;
    }
}
