package com.example.demo.service;

import com.example.demo.client.GhnClient;
import com.example.demo.config.GhnProperties;
import com.example.demo.dto.ghn.GhnFeeRequest;
import com.example.demo.dto.ghn.GhnFeeResponse;
import com.example.demo.dto.shipping.ShippingFeeResponse;
import com.example.demo.entity.Address;
import com.example.demo.entity.CartItem;
import com.example.demo.entity.Product;
import com.example.demo.repository.AddressRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ShippingService {
    
    private static final Logger logger = LoggerFactory.getLogger(ShippingService.class);
    
    private final GhnClient ghnClient;
    private final GhnProperties ghnProperties;
    private final AddressRepository addressRepository;
    
    public ShippingService(GhnClient ghnClient, GhnProperties ghnProperties, AddressRepository addressRepository) {
        this.ghnClient = ghnClient;
        this.ghnProperties = ghnProperties;
        this.addressRepository = addressRepository;
    }
    
    /**
     * Calculate shipping fee for cart items to specific address
     */
    public ShippingFeeResponse calculateShippingFee(Long userId, Long addressId, List<CartItem> items, Integer serviceTypeIdOpt) {
        try {
            // Validate GHN configuration
            if (!ghnClient.isConfigurationValid()) {
                logger.warn("GHN configuration is invalid");
                return ShippingFeeResponse.error("Dịch vụ tính phí ship chưa được cấu hình");
            }
            
            // Get destination address
            Address address = addressRepository.findById(addressId)
                    .orElse(null);
            
            if (address == null || !address.getUser().getId().equals(userId)) {
                return ShippingFeeResponse.error("Địa chỉ không tồn tại hoặc không thuộc về người dùng");
            }
            
            if (!address.isGhnCompatible()) {
                return ShippingFeeResponse.error("Địa chỉ không tương thích với hệ thống GHN. Vui lòng cập nhật địa chỉ.");
            }
            
            // Calculate total weight and dimensions
            ShippingDimensions dimensions = calculateShippingDimensions(items);
            
            // Use provided service type or default
            Integer serviceTypeId = serviceTypeIdOpt != null ? serviceTypeIdOpt : ghnProperties.getServiceTypeIdDefault();
            
            // Build GHN fee request
            GhnFeeRequest feeRequest = buildGhnFeeRequest(address, dimensions, serviceTypeId);
            
            if (feeRequest == null) {
                return ShippingFeeResponse.error("Không thể tạo yêu cầu tính phí. Vui lòng kiểm tra cấu hình điểm gửi.");
            }
            
            // Call GHN API
            GhnFeeResponse ghnResponse = ghnClient.calculateShippingFee(feeRequest);
            
            if (ghnResponse == null || !"200".equals(ghnResponse.code())) {
                String errorMsg = ghnResponse != null ? ghnResponse.message() : "Không có phản hồi từ GHN";
                logger.error("GHN fee calculation failed: {}", errorMsg);
                return ShippingFeeResponse.error("Lỗi tính phí ship: " + errorMsg);
            }
            
            if (ghnResponse.data() == null || ghnResponse.data().total() == null) {
                return ShippingFeeResponse.error("Dữ liệu phí ship không hợp lệ");
            }
            
            // Convert VND to BigDecimal
            BigDecimal shippingFee = new BigDecimal(ghnResponse.data().total());
            
            logger.info("Calculated shipping fee: {} VND for address {} with {} items", 
                shippingFee, addressId, items.size());
            
            return ShippingFeeResponse.success(shippingFee, serviceTypeId);
            
        } catch (Exception e) {
            logger.error("Error calculating shipping fee", e);
            return ShippingFeeResponse.error("Lỗi hệ thống khi tính phí ship: " + e.getMessage());
        }
    }
    
    /**
     * Calculate shipping dimensions from cart items
     */
    private ShippingDimensions calculateShippingDimensions(List<CartItem> items) {
        int totalWeight = 0;
        int maxLength = 0;
        int maxWidth = 0;
        int maxHeight = 0;
        
        for (CartItem item : items) {
            Product product = item.getProduct();
            int quantity = item.getQuantity();
            
            // Calculate total weight
            totalWeight += product.getTotalWeightForQuantity(quantity);
            
            // Get max dimensions (assuming items can be packed together)
            maxLength = Math.max(maxLength, product.getMaxLength());
            maxWidth = Math.max(maxWidth, product.getMaxWidth());
            maxHeight = Math.max(maxHeight, product.getMaxHeight());
        }
        
        return new ShippingDimensions(totalWeight, maxLength, maxWidth, maxHeight);
    }
    
    /**
     * Build GHN fee request
     */
    private GhnFeeRequest buildGhnFeeRequest(Address toAddress, ShippingDimensions dimensions, Integer serviceTypeId) {
        // Check if FROM location is configured
        if (!ghnClient.isFromLocationConfigured()) {
            logger.warn("GHN FROM location is not configured. Using ShopID default.");
            // When FROM location is not configured, GHN will use shop's default address
        }
        
        try {
            Integer fromDistrictId = null;
            String fromWardCode = null;
            
            // Parse FROM location if configured
            if (ghnProperties.getFrom().getDistrictId() != null && !ghnProperties.getFrom().getDistrictId().isEmpty()) {
                fromDistrictId = Integer.parseInt(ghnProperties.getFrom().getDistrictId());
            }
            if (ghnProperties.getFrom().getWardCode() != null && !ghnProperties.getFrom().getWardCode().isEmpty()) {
                fromWardCode = ghnProperties.getFrom().getWardCode();
            }
            
            return new GhnFeeRequest(
                serviceTypeId,
                null, // service_id (use service_type_id instead)
                fromDistrictId,
                fromWardCode,
                toAddress.getDistrictId(),
                toAddress.getWardCode(),
                dimensions.weight(),
                dimensions.length(),
                dimensions.width(),
                dimensions.height(),
                null, // insurance_value
                null, // cod_value
                null  // coupon
            );
            
        } catch (NumberFormatException e) {
            logger.error("Error parsing GHN FROM location configuration", e);
            return null;
        }
    }
    
    /**
     * Check if shipping service is available
     */
    public boolean isShippingServiceAvailable() {
        return ghnClient.isConfigurationValid();
    }
    
    /**
     * Get default service type ID
     */
    public Integer getDefaultServiceTypeId() {
        return ghnProperties.getServiceTypeIdDefault();
    }
    
    // Helper record for shipping dimensions
    private record ShippingDimensions(int weight, int length, int width, int height) {}
}
