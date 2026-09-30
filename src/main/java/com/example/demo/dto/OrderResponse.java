package com.example.demo.dto;

/**
 * Response DTO for order operations
 */
public class OrderResponse {
    
    private boolean success;
    private String message;
    private OrderDTO order;
    private String orderNumber; // For tracking
    
    // Constructors
    public OrderResponse() {
    }
    
    public OrderResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public OrderResponse(boolean success, String message, OrderDTO order) {
        this.success = success;
        this.message = message;
        this.order = order;
    }
    
    public OrderResponse(boolean success, String message, OrderDTO order, String orderNumber) {
        this.success = success;
        this.message = message;
        this.order = order;
        this.orderNumber = orderNumber;
    }
    
    // Static factory methods
    public static OrderResponse success(String message) {
        return new OrderResponse(true, message);
    }
    
    public static OrderResponse success(String message, OrderDTO order) {
        return new OrderResponse(true, message, order);
    }
    
    public static OrderResponse success(String message, OrderDTO order, String orderNumber) {
        return new OrderResponse(true, message, order, orderNumber);
    }
    
    public static OrderResponse error(String message) {
        return new OrderResponse(false, message);
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
    
    public OrderDTO getOrder() {
        return order;
    }
    
    public void setOrder(OrderDTO order) {
        this.order = order;
    }
    
    public String getOrderNumber() {
        return orderNumber;
    }
    
    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }
}
