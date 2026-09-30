package com.example.demo.dto;

import com.example.demo.entity.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request DTO for creating orders
 */
public class OrderRequest {
    
    // Address ID is optional - will use default address if not provided
    private Long addressId;
    
    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
    
    private Long deliveryUnitId;
    
    private String voucherCode;
    
    // GHN service type ID for shipping calculation
    private Integer serviceTypeId;
    
    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;
    
    // Shipping information (for checkout form)
    private String shippingAddress;
    private String shippingPhone;
    
    // For direct order creation (bypass cart)
    private List<OrderItemRequest> items;
    
    // Selected product IDs from cart (for partial checkout)
    private List<Long> selectedProductIds;
    
    // Constructors
    public OrderRequest() {
    }
    
    public OrderRequest(Long addressId, PaymentMethod paymentMethod, Long deliveryUnitId, 
                       String voucherCode, String notes, List<OrderItemRequest> items) {
        this.addressId = addressId;
        this.paymentMethod = paymentMethod;
        this.deliveryUnitId = deliveryUnitId;
        this.voucherCode = voucherCode;
        this.notes = notes;
        this.items = items;
    }
    
    // Getters and Setters
    public Long getAddressId() {
        return addressId;
    }
    
    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }
    
    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public Long getDeliveryUnitId() {
        return deliveryUnitId;
    }
    
    public void setDeliveryUnitId(Long deliveryUnitId) {
        this.deliveryUnitId = deliveryUnitId;
    }
    
    public String getVoucherCode() {
        return voucherCode;
    }
    
    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }
    
    public Integer getServiceTypeId() {
        return serviceTypeId;
    }
    
    public void setServiceTypeId(Integer serviceTypeId) {
        this.serviceTypeId = serviceTypeId;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public String getShippingAddress() {
        return shippingAddress;
    }
    
    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }
    
    public String getShippingPhone() {
        return shippingPhone;
    }
    
    public void setShippingPhone(String shippingPhone) {
        this.shippingPhone = shippingPhone;
    }
    
    public List<OrderItemRequest> getItems() {
        return items;
    }
    
    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
    }
    
    public List<Long> getSelectedProductIds() {
        return selectedProductIds;
    }
    
    public void setSelectedProductIds(List<Long> selectedProductIds) {
        this.selectedProductIds = selectedProductIds;
    }
    
    /**
     * Inner class for order item requests
     */
    public static class OrderItemRequest {
        @NotNull(message = "Product ID is required")
        private Long productId;
        
        @NotNull(message = "Quantity is required")
        private Integer quantity;
        
        public OrderItemRequest() {
        }
        
        public OrderItemRequest(Long productId, Integer quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }
        
        public Long getProductId() {
            return productId;
        }
        
        public void setProductId(Long productId) {
            this.productId = productId;
        }
        
        public Integer getQuantity() {
            return quantity;
        }
        
        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}
