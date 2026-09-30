package com.example.demo.dto;

import com.example.demo.entity.OrderItem;
import java.math.BigDecimal;

/**
 * DTO for OrderItem entity
 * Represents an item in customer order
 */
public class OrderItemDTO {
    
    private Long id;
    private String orderId;
    private Long productId;
    private String productName;
    private String productDescription;
    private String productImage;
    private BigDecimal productPrice; // Current product price
    private Integer productStockQuantity;
    private String productStatus;
    private Integer quantity;
    private BigDecimal price; // Price at time of order
    private BigDecimal subtotal;
    private Boolean hasReview; // Indicates if this item has been reviewed
    private Boolean canReview; // Indicates if this item can be reviewed
    private String reviewReason; // Reason why item cannot be reviewed
    private Boolean reviewed; // Indicates if this item has been reviewed (alias for hasReview)
    
    // Constructors
    public OrderItemDTO() {
    }
    
    public OrderItemDTO(Long id, String orderId, Long productId, String productName,
                        String productDescription, String productImage, BigDecimal productPrice,
                        Integer productStockQuantity, String productStatus, Integer quantity,
                        BigDecimal price, BigDecimal subtotal) {
        this.id = id;
        this.orderId = orderId;
        this.productId = productId;
        this.productName = productName;
        this.productDescription = productDescription;
        this.productImage = productImage;
        this.productPrice = productPrice;
        this.productStockQuantity = productStockQuantity;
        this.productStatus = productStatus;
        this.quantity = quantity;
        this.price = price;
        this.subtotal = subtotal;
    }
    
    // Static factory method to create from OrderItem entity
    public static OrderItemDTO fromOrderItem(OrderItem orderItem) {
        if (orderItem == null) {
            return null;
        }
        
        BigDecimal subtotal = orderItem.getPrice() != null ?
            orderItem.getPrice().multiply(new BigDecimal(orderItem.getQuantity())) :
            BigDecimal.ZERO;
        
        // Safe extraction of product information
        Long productId = null;
        String productName = "Unknown Product";
        String productDescription = null;
        String productImage = null;
        BigDecimal productPrice = BigDecimal.ZERO;
        Integer productStockQuantity = 0;
        String productStatus = null;
        
        try {
            if (orderItem.getProduct() != null) {
                productId = orderItem.getProduct().getId();
                productName = orderItem.getProduct().getName();
                productDescription = orderItem.getProduct().getDescription();
                productImage = orderItem.getProduct().getImage();
                productPrice = orderItem.getProduct().getPrice();
                productStockQuantity = orderItem.getProduct().getStockQuantity();
                productStatus = orderItem.getProduct().getStatus() != null ? 
                    orderItem.getProduct().getStatus().name() : null;
            }
        } catch (Exception e) {
            // Ignore lazy loading exceptions - use default values
        }
        
        return new OrderItemDTO(
            orderItem.getId(),
            orderItem.getOrder() != null ? orderItem.getOrder().getId() : null,
            productId,
            productName,
            productDescription,
            productImage,
            productPrice,
            productStockQuantity,
            productStatus,
            orderItem.getQuantity(),
            orderItem.getPrice(), // Price at time of order
            subtotal
        );
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
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
    
    public String getProductImage() {
        return productImage;
    }
    
    public void setProductImage(String productImage) {
        this.productImage = productImage;
    }
    
    public BigDecimal getProductPrice() {
        return productPrice;
    }
    
    public void setProductPrice(BigDecimal productPrice) {
        this.productPrice = productPrice;
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
    
    public BigDecimal getSubtotal() {
        return subtotal;
    }
    
    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
    
    public Boolean getHasReview() {
        return hasReview;
    }
    
    public void setHasReview(Boolean hasReview) {
        this.hasReview = hasReview;
    }
    
    public Boolean getCanReview() {
        return canReview;
    }
    
    public void setCanReview(Boolean canReview) {
        this.canReview = canReview;
    }
    
    public String getReviewReason() {
        return reviewReason;
    }
    
    public void setReviewReason(String reviewReason) {
        this.reviewReason = reviewReason;
    }
    
    public Boolean getReviewed() {
        return reviewed;
    }
    
    public void setReviewed(Boolean reviewed) {
        this.reviewed = reviewed;
    }
}
