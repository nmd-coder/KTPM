package com.example.demo.controller;

import com.example.demo.dto.address.AddressDto;
import com.example.demo.dto.address.AddressUpsertDto;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AddressService;
import com.example.demo.dto.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Tag(name = "🏠 Addresses", description = "User address management APIs")
@RestController
@RequestMapping("/api/addresses")
public class AddressController {
    
    private static final Logger logger = LoggerFactory.getLogger(AddressController.class);
    
    private final AddressService addressService;
    private final UserRepository userRepository;
    
    public AddressController(AddressService addressService, UserRepository userRepository) {
        this.addressService = addressService;
        this.userRepository = userRepository;
    }
    
    /**
     * Create or update address
     */
    @Operation(
        summary = "Create or update address",
        description = "Create a new address or update existing address for the authenticated user"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Address created/updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid address data"),
        @ApiResponse(responseCode = "401", description = "User not authenticated"),
        @ApiResponse(responseCode = "500", description = "Error saving address")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<ResponseWrapper<AddressDto>> createOrUpdateAddress(
            @Valid @RequestBody AddressUpsertDto dto,
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
            
            // Create or update address
            AddressDto result = addressService.createOrUpdateAddress(user.getId(), dto);
            
            String action = dto.id() != null ? "cập nhật" : "tạo";
            logger.info("User {} {} address {} in {} mode", user.getId(), action, result.id(), dto.addressMode());
            
            return ResponseEntity.ok(ResponseWrapper.success(result));
            
        } catch (IllegalArgumentException e) {
            logger.warn("Validation error: {}", e.getMessage());
            return ResponseEntity.ok(ResponseWrapper.error(e.getMessage()));
            
        } catch (Exception e) {
            logger.error("Error creating/updating address", e);
            return ResponseEntity.ok(ResponseWrapper.error("Lỗi hệ thống khi lưu địa chỉ"));
        }
    }
    
    /**
     * Get all addresses for current user
     */
    @Operation(
        summary = "Get user addresses",
        description = "Retrieve all addresses for the authenticated user"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Addresses retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "User not authenticated"),
        @ApiResponse(responseCode = "500", description = "Error retrieving addresses")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<ResponseWrapper<List<AddressDto>>> getUserAddresses(Authentication authentication) {
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
            
            List<AddressDto> addresses = addressService.getUserAddresses(user.getId());
            return ResponseEntity.ok(ResponseWrapper.success(addresses));
            
        } catch (Exception e) {
            logger.error("Error getting user addresses", e);
            return ResponseEntity.ok(ResponseWrapper.error("Lỗi hệ thống khi tải danh sách địa chỉ"));
        }
    }
    
    /**
     * Get user's default address
     */
    @Operation(
        summary = "Get default address",
        description = "Retrieve the default address for the authenticated user"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Default address retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "User not authenticated"),
        @ApiResponse(responseCode = "404", description = "No default address found"),
        @ApiResponse(responseCode = "500", description = "Error retrieving default address")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/default")
    public ResponseEntity<ResponseWrapper<AddressDto>> getUserDefaultAddress(Authentication authentication) {
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
            
            Optional<AddressDto> defaultAddress = addressService.getUserDefaultAddress(user.getId());
            
            if (defaultAddress.isEmpty()) {
                return ResponseEntity.ok(ResponseWrapper.error("Chưa có địa chỉ mặc định"));
            }
            
            return ResponseEntity.ok(ResponseWrapper.success(defaultAddress.get()));
            
        } catch (Exception e) {
            logger.error("Error getting user default address", e);
            return ResponseEntity.ok(ResponseWrapper.error("Lỗi hệ thống khi tải địa chỉ mặc định"));
        }
    }
    
    /**
     * Get GHN-compatible addresses for current user
     */
    @Operation(
        summary = "Get GHN-compatible addresses",
        description = "Retrieve addresses that are compatible with GHN shipping service for the authenticated user"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "GHN-compatible addresses retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "User not authenticated"),
        @ApiResponse(responseCode = "500", description = "Error retrieving addresses")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/ghn-compatible")
    public ResponseEntity<ResponseWrapper<List<AddressDto>>> getGhnCompatibleAddresses(Authentication authentication) {
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
            
            List<AddressDto> addresses = addressService.getGhnCompatibleAddresses(user.getId());
            return ResponseEntity.ok(ResponseWrapper.success(addresses));
            
        } catch (Exception e) {
            logger.error("Error getting GHN-compatible addresses", e);
            return ResponseEntity.ok(ResponseWrapper.error("Lỗi hệ thống khi tải địa chỉ tương thích GHN"));
        }
    }
    
    /**
     * Delete address
     */
    @Operation(
        summary = "Delete address",
        description = "Delete a specific address for the authenticated user"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Address deleted successfully"),
        @ApiResponse(responseCode = "401", description = "User not authenticated"),
        @ApiResponse(responseCode = "404", description = "Address not found"),
        @ApiResponse(responseCode = "500", description = "Error deleting address")
    })
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseWrapper<Void>> deleteAddress(
            @Parameter(description = "Address ID", required = true) @PathVariable Long id,
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
            
            addressService.deleteAddress(user.getId(), id);
            
            logger.info("User {} deleted address {}", user.getId(), id);
            return ResponseEntity.ok(ResponseWrapper.success(null));
            
        } catch (RuntimeException e) {
            logger.warn("Error deleting address: {}", e.getMessage());
            return ResponseEntity.ok(ResponseWrapper.error(e.getMessage()));
            
        } catch (Exception e) {
            logger.error("Error deleting address", e);
            return ResponseEntity.ok(ResponseWrapper.error("Lỗi hệ thống khi xóa địa chỉ"));
        }
    }
}
