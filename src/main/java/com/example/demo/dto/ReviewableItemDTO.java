package com.example.demo.dto;

import com.example.demo.entity.OrderItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for order items that can be reviewed
 */
public class ReviewableItemDTO {
    
    private Long orderItemId;
    private Long productId;
    private String productName;
    private String productImage;
    private String orderId;
    private LocalDateTime orderDate;
    private Integer quantity;
    private BigDecimal price;
    private boolean alreadyReviewed;
    
    // Constructors
    public ReviewableItemDTO() {}
    
    public ReviewableItemDTO(OrderItem orderItem, boolean alreadyReviewed) {
        this.orderItemId = orderItem.getId();
        this.productId = orderItem.getProduct().getId();
        this.productName = orderItem.getProduct().getName();
        this.productImage = orderItem.getProduct().getImage();
        this.orderId = orderItem.getOrder().getId();
        this.orderDate = orderItem.getOrder().getOrderDate();
        this.quantity = orderItem.getQuantity();
        this.price = orderItem.getPrice();
        this.alreadyReviewed = alreadyReviewed;
    }
    
    // Getters and Setters
    public Long getOrderItemId() {
        return orderItemId;
    }
    
    public void setOrderItemId(Long orderItemId) {
        this.orderItemId = orderItemId;
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
    
    public String getProductImage() {
        return productImage;
    }
    
    public void setProductImage(String productImage) {
        this.productImage = productImage;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public LocalDateTime getOrderDate() {
        return orderDate;
    }
    
    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    public boolean isAlreadyReviewed() {
        return alreadyReviewed;
    }
    
    public void setAlreadyReviewed(boolean alreadyReviewed) {
        this.alreadyReviewed = alreadyReviewed;
    }
}
