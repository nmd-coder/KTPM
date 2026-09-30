package com.example.demo.dto;

/**
 * Response DTO for cart operations
 */
public class CartResponse {
    
    private boolean success;
    private String message;
    private CartDTO cart;
    private Long totalItems; // Total items count for header badge
    
    // Constructors
    public CartResponse() {
    }
    
    public CartResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public CartResponse(boolean success, String message, CartDTO cart) {
        this.success = success;
        this.message = message;
        this.cart = cart;
    }
    
    public CartResponse(boolean success, String message, CartDTO cart, Long totalItems) {
        this.success = success;
        this.message = message;
        this.cart = cart;
        this.totalItems = totalItems;
    }
    
    // Static factory methods
    public static CartResponse success(String message) {
        return new CartResponse(true, message);
    }
    
    public static CartResponse success(String message, CartDTO cart) {
        return new CartResponse(true, message, cart);
    }
    
    public static CartResponse success(String message, CartDTO cart, Long totalItems) {
        return new CartResponse(true, message, cart, totalItems);
    }
    
    public static CartResponse error(String message) {
        return new CartResponse(false, message);
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public CartDTO getCart() {
        return cart;
    }
    
    public void setCart(CartDTO cart) {
        this.cart = cart;
    }
    
    public Long getTotalItems() {
        return totalItems;
    }
    
    public void setTotalItems(Long totalItems) {
        this.totalItems = totalItems;
    }
}
