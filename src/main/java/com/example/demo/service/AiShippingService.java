package com.example.demo.service;

import com.example.demo.client.GhnClient;
import com.example.demo.config.GhnProperties;
import com.example.demo.dto.ShippingFeeEstimate;
import com.example.demo.dto.ghn.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Service for AI shipping fee calculations
 * Enhanced to use GHN API for accurate nationwide shipping estimates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiShippingService {

    private final StoreConfigService storeConfigService;
    private final GhnClient ghnClient;
    private final GhnProperties ghnProperties;

    /**
     * Calculate shipping fee for AI using GHN API for all locations
     * Enhanced to support nationwide shipping instead of just HCM inner city
     */
    public ShippingFeeEstimate calculateShippingFeeForAi(String toLocation) {
        log.info("AI shipping fee calculation for: {}", toLocation);
        
        if (toLocation == null || toLocation.trim().isEmpty()) {
            return ShippingFeeEstimate.error("Vui lòng cung cấp địa chỉ giao hàng");
        }

        String location = toLocation.toLowerCase().trim();
        
        try {
            // First check if GHN is properly configured
            if (!ghnClient.isConfigurationValid() || !ghnClient.isFromLocationConfigured()) {
                log.warn("GHN not properly configured, falling back to estimated rates");
                return calculateFallbackEstimate(toLocation);
            }

            // Try to parse location and get accurate GHN calculation
            ShippingFeeEstimate ghnEstimate = calculateWithGhnApi(location);
            if (ghnEstimate.getSuccess()) {
                log.info("✅ GHN API success: {} for {}", ghnEstimate.getFormattedFee(), toLocation);
                return ghnEstimate;
            }
            
            // If GHN fails, provide intelligent fallback with better coverage
            log.info("GHN API failed, using intelligent fallback for: {}", toLocation);
            return calculateIntelligentFallback(toLocation);
            
        } catch (Exception e) {
            log.error("Error calculating shipping fee for AI", e);
            return calculateIntelligentFallback(toLocation);
        }
    }

    /**
     * Calculate shipping fee using GHN API
     */
    private ShippingFeeEstimate calculateWithGhnApi(String location) {
        try {
            // Simple location parsing - could be enhanced with more sophisticated parsing
            GhnLocation parsedLocation = parseLocation(location);
            if (parsedLocation == null) {
                return ShippingFeeEstimate.error("Không thể xác định địa chỉ");
            }

            // Create GHN fee request using correct parameter order/types per GhnFeeRequest record
            Integer serviceTypeId = ghnProperties.getServiceTypeIdDefault();
            Integer serviceId = null; // Let GHN auto select service based on serviceTypeId when null
            Integer fromDistrictId = Integer.parseInt(ghnProperties.getFrom().getDistrictId());
            String fromWardCode = ghnProperties.getFrom().getWardCode();
            Integer toDistrictId = parsedLocation.getDistrictId();
            String toWardCode = parsedLocation.getWardCode().isEmpty() ? null : parsedLocation.getWardCode();

            GhnFeeRequest feeRequest = new GhnFeeRequest(
                serviceTypeId,       // service_type_id
                serviceId,           // service_id
                fromDistrictId,      // from_district_id
                fromWardCode,        // from_ward_code
                toDistrictId,        // to_district_id
                toWardCode,          // to_ward_code
                10,                  // weight (gram)
                20,                  // length (cm)
                15,                  // width (cm)
                500,                 // height (cm)
                0,                   // insurance_value
                0,                   // cod_value
                null                 // coupon
            );

            GhnFeeResponse response = ghnClient.calculateShippingFee(feeRequest);
            
            if (response != null && "200".equals(response.code()) && response.data() != null) {
                GhnFeeData data = response.data();
                BigDecimal fee = BigDecimal.valueOf(data.serviceFee());
                String estimatedTime = "2-3 ngày"; // Default time since GhnFeeData doesn't have leadTime
                
                ShippingFeeEstimate estimate = ShippingFeeEstimate.success(fee, estimatedTime);
                estimate.setToLocation(location);
                estimate.setFromLocation("TP. Thủ Đức, TP.HCM");
                
                return estimate;
            } else {
                log.warn("GHN API returned error: {}", response != null ? response.message() : "null response");
                return ShippingFeeEstimate.error("Không thể tính phí ship qua GHN");
            }
            
        } catch (Exception e) {
            log.error("Error calling GHN API", e);
            return ShippingFeeEstimate.error("Lỗi khi gọi GHN API");
        }
    }

    /**
     * Parse location string to GHN location data
     * This is a simplified version - could be enhanced with location matching service
     */
    private GhnLocation parseLocation(String location) {
        try {
            // For demo purposes, we'll use some common location mappings
            // In production, this should use a proper address parsing service
            
            if (location.contains("hcm") || location.contains("hồ chí minh") || 
                location.contains("sài gòn") || location.contains("saigon")) {
                // Ho Chi Minh City - use a common district
                return new GhnLocation(3695, ""); // District 1, HCMC
            } else if (location.contains("hà nội") || location.contains("ha noi") || 
                       location.contains("hanoi")) {
                // Hanoi
                return new GhnLocation(1454, ""); // Ba Dinh District, Hanoi  
            } else if (location.contains("đà nẵng") || location.contains("da nang") ||
                       location.contains("danang")) {
                // Da Nang
                return new GhnLocation(1442, ""); // Hai Chau District, Da Nang
            } else if (location.contains("cần thơ") || location.contains("can tho")) {
                // Can Tho
                return new GhnLocation(1581, ""); // Ninh Kieu District, Can Tho
            } else if (location.contains("biên hòa") || location.contains("bien hoa") ||
                       location.contains("đồng nai") || location.contains("dong nai")) {
                // Dong Nai
                return new GhnLocation(1820, ""); // Bien Hoa City
            } else if (location.contains("bình dương") || location.contains("binh duong")) {
                // Binh Duong
                return new GhnLocation(1533, ""); // Thu Dau Mot City
            }
            
            // For other locations, we can't parse accurately, return null to use fallback
            return null;
            
        } catch (Exception e) {
            log.error("Error parsing location: {}", location, e);
            return null;
        }
    }

    /**
     * Helper class for GHN location data
     */
    private static class GhnLocation {
        private final int districtId;
        private final String wardCode;
        
        public GhnLocation(int districtId, String wardCode) {
            this.districtId = districtId;
            this.wardCode = wardCode;
        }
        
        public int getDistrictId() { return districtId; }
        public String getWardCode() { return wardCode; }
    }

    // removed unused formatEstimatedTime helper

    /**
     * Intelligent fallback calculation with broader coverage
     * Enhanced to support all Vietnam locations instead of just HCM
     */
    private ShippingFeeEstimate calculateIntelligentFallback(String toLocation) {
        String location = toLocation.toLowerCase().trim();
        
        try {
            BigDecimal fee;
            String estimatedTime;
            
            // Enhanced location detection for all major cities and provinces
            if (location.contains("hcm") || location.contains("hồ chí minh") || 
                location.contains("sài gòn") || location.contains("saigon") ||
                location.contains("thủ đức") || location.contains("thu duc")) {
                
                // Ho Chi Minh City area
                if (location.contains("quận") || location.contains("quan") ||
                    location.contains("thủ đức") || location.contains("thu duc")) {
                    fee = new BigDecimal("25000"); // Inner city
                    estimatedTime = "2-4 giờ";
                } else {
                    fee = new BigDecimal("30000"); // Outer areas
                    estimatedTime = "4-6 giờ";
                }
                
            } else if (location.contains("hà nội") || location.contains("ha noi") || 
                       location.contains("hanoi")) {
                
                // Hanoi
                fee = new BigDecimal("45000");
                estimatedTime = "1-2 ngày";
                
            } else if (location.contains("đà nẵng") || location.contains("da nang") ||
                       location.contains("danang")) {
                
                // Da Nang
                fee = new BigDecimal("40000");
                estimatedTime = "1-2 ngày";
                
            } else if (location.contains("cần thơ") || location.contains("can tho")) {
                
                // Can Tho
                fee = new BigDecimal("35000");
                estimatedTime = "1 ngày";
                
            } else if (location.contains("biên hòa") || location.contains("bien hoa") ||
                       location.contains("đồng nai") || location.contains("dong nai") ||
                       location.contains("bình dương") || location.contains("binh duong") ||
                       location.contains("long an") || location.contains("tây ninh") ||
                       location.contains("tay ninh")) {
                
                // Nearby provinces (Southeast region)
                fee = new BigDecimal("30000");
                estimatedTime = "4-8 giờ";
                
            } else if (location.contains("vũng tàu") || location.contains("vung tau") ||
                       location.contains("bà rịa") || location.contains("ba ria")) {
                
                // Ba Ria - Vung Tau
                fee = new BigDecimal("35000");
                estimatedTime = "6-8 giờ";
                
            } else if (location.contains("bến tre") || location.contains("ben tre") ||
                       location.contains("tiền giang") || location.contains("tien giang") ||
                       location.contains("vĩnh long") || location.contains("vinh long") ||
                       location.contains("an giang") || location.contains("đồng tháp") ||
                       location.contains("dong thap")) {
                
                // Mekong Delta provinces
                fee = new BigDecimal("40000");
                estimatedTime = "1-2 ngày";
                
            } else if (location.contains("nha trang") || location.contains("khánh hòa") ||
                       location.contains("khanh hoa") || location.contains("đà lạt") ||
                       location.contains("da lat") || location.contains("lâm đồng") ||
                       location.contains("lam dong")) {
                
                // Central coastal and highland
                fee = new BigDecimal("50000");
                estimatedTime = "2-3 ngày";
                
            } else if (location.contains("huế") || location.contains("hue") ||
                       location.contains("quảng nam") || location.contains("quang nam") ||
                       location.contains("hội an") || location.contains("hoi an")) {
                
                // Central Vietnam
                fee = new BigDecimal("45000");
                estimatedTime = "2 ngày";
                
            } else if (location.contains("hải phòng") || location.contains("hai phong") ||
                       location.contains("quảng ninh") || location.contains("quang ninh") ||
                       location.contains("hạ long") || location.contains("ha long")) {
                
                // Northern coastal
                fee = new BigDecimal("50000");
                estimatedTime = "2-3 ngày";
                
            } else {
                
                // Other locations - general Vietnam estimate
                fee = new BigDecimal("55000");
                estimatedTime = "2-4 ngày";
            }
            
            ShippingFeeEstimate estimate = ShippingFeeEstimate.success(fee, estimatedTime);
            estimate.setToLocation(toLocation);
            estimate.setFromLocation("TP. Thủ Đức, TP.HCM");
            
            log.info("Intelligent fallback estimate: {} for location: {}", fee, toLocation);
            
            return estimate;
            
        } catch (Exception e) {
            log.error("Error in intelligent fallback calculation", e);
            return ShippingFeeEstimate.error("Không thể tính phí ship lúc này");
        }
    }

    /**
     * Fallback for when GHN is not configured
     */
    private ShippingFeeEstimate calculateFallbackEstimate(String toLocation) {
        log.info("Using basic fallback estimate for: {}", toLocation);
        
        // Basic estimate when GHN is not available
        BigDecimal fee = new BigDecimal("45000");
        String estimatedTime = "2-3 ngày";
        
        ShippingFeeEstimate estimate = ShippingFeeEstimate.success(fee, estimatedTime);
        estimate.setToLocation(toLocation);
        estimate.setFromLocation("TP. Thủ Đức, TP.HCM");
        
        return estimate;
    }

    /**
     * Get shipping policy text
     */
    public String getShippingPolicy() {
        String policy = storeConfigService.getConfig("policy.shipping");
        if (policy != null) {
            return policy;
        }
        return "🚚 Chúng mình giao hàng TOÀN QUỐC! Miễn phí ship đơn từ 500k trong TP.HCM. " +
               "Giao hàng nhanh 2-4 giờ trong thành phố, 1-3 ngày toàn quốc.";
    }

    /**
     * Format shipping fee estimate as text for customer
     */
    public String formatEstimateAsText(ShippingFeeEstimate estimate) {
        if (!estimate.getSuccess()) {
            return estimate.getErrorMessage();
        }
        
        StringBuilder result = new StringBuilder();
        result.append(String.format("📦 **Phí ship đến %s: %s**", 
            estimate.getToLocation(),
            estimate.getFormattedFee()));
        
        result.append(String.format("\n⏰ Thời gian dự kiến: %s", estimate.getEstimatedTime()));
        
        // Add shipping policy note for all locations
        result.append("\n💡 Mình ship toàn quốc! Phí này là ước tính, sẽ xác nhận chính xác khi đặt hàng.");
        
        return result.toString();
    }
}
