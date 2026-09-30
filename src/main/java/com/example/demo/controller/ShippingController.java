package com.example.demo.controller;

import com.example.demo.dto.shipping.ShippingFeeRequest;
import com.example.demo.dto.shipping.ShippingFeeResponse;
import com.example.demo.entity.Cart;
import com.example.demo.entity.CartItem;
import com.example.demo.entity.User;
import com.example.demo.repository.CartRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.ShippingService;
import com.example.demo.dto.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "🚚 Shipping", description = "Shipping fee calculation APIs")
@RestController
@RequestMapping("/api/shipping")
public class ShippingController {
    
    private static final Logger logger = LoggerFactory.getLogger(ShippingController.class);
    
    private final ShippingService shippingService;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    
    public ShippingController(ShippingService shippingService, UserRepository userRepository, CartRepository cartRepository) {
        this.shippingService = shippingService;
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
    }
    
    /**
     * Calculate shipping fee for current cart to specific address
     */
    @Operation(
        summary = "Calculate shipping fee",
        description = "Calculate shipping fee for current user's cart to specified address using GHN API"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Shipping fee calculated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or cart is empty"),
        @ApiResponse(responseCode = "401", description = "User not authenticated"),
        @ApiResponse(responseCode = "500", description = "Error calculating shipping fee")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/fee")
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseWrapper<ShippingFeeResponse>> calculateShippingFee(
            @RequestBody ShippingFeeRequest request,
            Authentication authentication) {
        
        try {
            if (authentication == null || authentication.getName() == null) {
                return ResponseEntity.ok(ResponseWrapper.error("Vui lòng đăng nhập để sử dụng tính năng này"));
            }
            
            // Get current user
            User user = userRepository.findByEmail(authentication.getName())
                    .orElse(null);
            
            if (user == null) {
                return ResponseEntity.ok(ResponseWrapper.error("Không tìm thấy thông tin người dùng"));
            }
            
            // Validate request
            if (request.addressId() == null) {
                return ResponseEntity.ok(ResponseWrapper.error("Vui lòng chọn địa chỉ giao hàng"));
            }
            
            // Get user's cart with items
            Cart cart = cartRepository.findByUserIdWithItems(user.getId()).orElse(null);
            if (cart == null || cart.getCartItems().isEmpty()) {
                return ResponseEntity.ok(ResponseWrapper.error("Giỏ hàng trống"));
            }
            
            List<CartItem> cartItems = cart.getCartItems();
            
            // Calculate shipping fee
            ShippingFeeResponse feeResponse = shippingService.calculateShippingFee(
                user.getId(), 
                request.addressId(), 
                cartItems, 
                request.serviceTypeId()
            );
            
            if (!feeResponse.success()) {
                return ResponseEntity.ok(ResponseWrapper.error(feeResponse.message()));
            }
            
            logger.info("Calculated shipping fee {} for user {} to address {}", 
                feeResponse.shippingFee(), user.getId(), request.addressId());
            
            return ResponseEntity.ok(ResponseWrapper.success(feeResponse));
            
        } catch (Exception e) {
            logger.error("Error calculating shipping fee", e);
            return ResponseEntity.ok(ResponseWrapper.error("Lỗi hệ thống khi tính phí ship"));
        }
    }
    
    /**
     * Check if shipping service is available
     */
    @Operation(
        summary = "Check shipping service status",
        description = "Check if GHN shipping service is available and accessible"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Service status retrieved"),
        @ApiResponse(responseCode = "500", description = "Error checking service status")
    })
    @GetMapping("/status")
    public ResponseEntity<ResponseWrapper<Boolean>> getShippingServiceStatus() {
        try {
            boolean isAvailable = shippingService.isShippingServiceAvailable();
            return ResponseEntity.ok(ResponseWrapper.success(isAvailable));
            
        } catch (Exception e) {
            logger.error("Error checking shipping service status", e);
            return ResponseEntity.ok(ResponseWrapper.error("Lỗi hệ thống khi kiểm tra trạng thái dịch vụ ship"));
        }
    }
    
    /**
     * Get default service type ID
     */
    @Operation(
        summary = "Get default service type",
        description = "Get the default GHN service type ID for shipping calculations"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Default service type retrieved"),
        @ApiResponse(responseCode = "500", description = "Error retrieving service type")
    })
    @GetMapping("/service-types/default")
    public ResponseEntity<ResponseWrapper<Integer>> getDefaultServiceType() {
        try {
            Integer defaultServiceType = shippingService.getDefaultServiceTypeId();
            return ResponseEntity.ok(ResponseWrapper.success(defaultServiceType));
            
        } catch (Exception e) {
            logger.error("Error getting default service type", e);
            return ResponseEntity.ok(ResponseWrapper.error("Lỗi hệ thống khi lấy loại dịch vụ mặc định"));
        }
    }
}
