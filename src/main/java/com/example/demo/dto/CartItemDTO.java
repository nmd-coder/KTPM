package com.example.demo.dto;

import com.example.demo.entity.CartItem;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for CartItem entity
 * Represents an item in user's shopping cart
 */
public class CartItemDTO {
    
    private Long id;
    private Long cartId;
    private Long productId;
    private String productName;
    private String productDescription;
    private BigDecimal productPrice;
    private String productImage;
    private Integer productStockQuantity;
    private String productStatus;
    private Integer quantity;
    private BigDecimal subtotal;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public CartItemDTO() {
    }
    
    public CartItemDTO(Long id, Long cartId, Long productId, String productName, 
                       String productDescription, BigDecimal productPrice, String productImage,
                       Integer productStockQuantity, String productStatus, Integer quantity,
                       BigDecimal subtotal, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.cartId = cartId;
        this.productId = productId;
        this.productName = productName;
        this.productDescription = productDescription;
        this.productPrice = productPrice;
        this.productImage = productImage;
        this.productStockQuantity = productStockQuantity;
        this.productStatus = productStatus;
        this.quantity = quantity;
        this.subtotal = subtotal;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    // Static factory method to create from CartItem entity
    public static CartItemDTO fromCartItem(CartItem cartItem) {
        if (cartItem == null || cartItem.getProduct() == null) {
            return null;
        }
        
        BigDecimal subtotal = cartItem.getProduct().getPrice() != null ?
            cartItem.getProduct().getPrice().multiply(new BigDecimal(cartItem.getQuantity())) :
            BigDecimal.ZERO;
            
        return new CartItemDTO(
            cartItem.getId(),
            cartItem.getCart() != null ? cartItem.getCart().getId() : null,
            cartItem.getProduct().getId(),
            cartItem.getProduct().getName(),
            cartItem.getProduct().getDescription(),
            cartItem.getProduct().getPrice(),
            cartItem.getProduct().getImage(),
            cartItem.getProduct().getStockQuantity(),
            cartItem.getProduct().getStatus() != null ? cartItem.getProduct().getStatus().name() : null,
            cartItem.getQuantity(),
            subtotal,
            cartItem.getCreatedAt(),
            cartItem.getUpdatedAt()
        );
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getCartId() {
        return cartId;
    }
    
    public void setCartId(Long cartId) {
        this.cartId = cartId;
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
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    
    public BigDecimal getSubtotal() {
        return subtotal;
    }
    
    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
