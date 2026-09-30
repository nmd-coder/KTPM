package com.example.demo.dto;

import com.example.demo.entity.Cart;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO for Cart entity
 * Represents user's shopping cart
 */
public class CartDTO {
    
    private Long id;
    private Long userId;
    private BigDecimal totalAmount;
    private Integer totalQuantity;
    private List<CartItemDTO> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public CartDTO() {
    }
    
    public CartDTO(Long id, Long userId, BigDecimal totalAmount, Integer totalQuantity, 
                   List<CartItemDTO> items, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.totalQuantity = totalQuantity;
        this.items = items;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    // Static factory method to create from Cart entity
    public static CartDTO fromCart(Cart cart) {
        if (cart == null) {
            return null;
        }
        
        List<CartItemDTO> itemDTOs = cart.getCartItems() != null ? 
            cart.getCartItems().stream()
                .map(CartItemDTO::fromCartItem)
                .collect(Collectors.toList()) : 
            new java.util.ArrayList<>();
            
        return new CartDTO(
            cart.getId(),
            cart.getUser() != null ? cart.getUser().getId() : null,
            cart.getTotalAmount(),
            cart.getTotalQuantity(),
            itemDTOs,
            cart.getCreatedAt(),
            cart.getUpdatedAt()
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
    
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    public Integer getTotalQuantity() {
        return totalQuantity;
    }
    
    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }
    
    public List<CartItemDTO> getItems() {
        return items;
    }
    
    public void setItems(List<CartItemDTO> items) {
        this.items = items;
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
