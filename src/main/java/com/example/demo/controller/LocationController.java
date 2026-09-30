package com.example.demo.controller;

import com.example.demo.dto.location.DistrictDto;
import com.example.demo.dto.location.ProvinceDto;
import com.example.demo.dto.location.WardDto;
import com.example.demo.service.LocationService;
import com.example.demo.dto.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "📍 Locations", description = "GHN location APIs - provinces, districts, wards")
@RestController
@RequestMapping("/api/locations")
public class LocationController {
    
    private static final Logger logger = LoggerFactory.getLogger(LocationController.class);
    
    private final LocationService locationService;
    
    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }
    
    /**
     * Get all provinces
     */
    @Operation(
        summary = "Get all provinces",
        description = "Retrieve all provinces from GHN shipping API"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved provinces"),
        @ApiResponse(responseCode = "500", description = "GHN service unavailable")
    })
    @GetMapping("/provinces")
    public ResponseEntity<ResponseWrapper<List<ProvinceDto>>> getProvinces() {
        try {
            logger.info("=== Getting provinces from LocationService ===");
            
            // Check configuration first
            boolean isValid = locationService.isGhnConfigurationValid();
            logger.info("GHN Configuration valid: {}", isValid);
            
            if (!isValid) {
                logger.error("GHN configuration is invalid!");
                return ResponseEntity.ok(ResponseWrapper.error("Cấu hình GHN không hợp lệ"));
            }
            
            List<ProvinceDto> provinces = locationService.getProvinces();
            logger.info("Retrieved {} provinces from GHN", provinces.size());
            
            if (provinces.isEmpty()) {
                logger.warn("Empty provinces list returned from GHN");
                return ResponseEntity.ok(ResponseWrapper.error("Không thể tải danh sách tỉnh/thành phố từ GHN"));
            }
            
            return ResponseEntity.ok(ResponseWrapper.success(provinces));
            
        } catch (Exception e) {
            logger.error("Error getting provinces", e);
            return ResponseEntity.ok(ResponseWrapper.error("Lỗi hệ thống: " + e.getMessage()));
        }
    }
    
    /**
     * Debug endpoint to check GHN configuration
     */
    @Operation(
        summary = "Debug GHN configuration",
        description = "Check GHN service configuration and connectivity status"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Configuration status retrieved"),
        @ApiResponse(responseCode = "500", description = "Error checking configuration")
    })
    @GetMapping("/debug")
    public ResponseEntity<ResponseWrapper<String>> debug() {
        try {
            boolean isValid = locationService.isGhnConfigurationValid();
            
            StringBuilder info = new StringBuilder();
            info.append("GHN Configuration Status: ").append(isValid ? "VALID" : "INVALID").append("\n");
            info.append("Base URL: ").append(System.getProperty("ghn.base-url", "Not set")).append("\n");
            info.append("Token configured: ").append(isValid ? "Yes" : "No").append("\n");
            info.append("Shop ID configured: ").append(isValid ? "Yes" : "No").append("\n");
            
            // Try to get provinces
            try {
                List<ProvinceDto> provinces = locationService.getProvinces();
                info.append("Provinces loaded: ").append(provinces.size()).append("\n");
            } catch (Exception e) {
                info.append("Error loading provinces: ").append(e.getMessage()).append("\n");
            }
            
            return ResponseEntity.ok(ResponseWrapper.success(info.toString()));
        } catch (Exception e) {
            logger.error("Error checking GHN configuration", e);
            return ResponseEntity.ok(ResponseWrapper.error("Error: " + e.getMessage()));
        }
    }
    
    /**
     * Get districts by province ID
     */
    @Operation(
        summary = "Get districts by province",
        description = "Retrieve all districts for a specific province from GHN API"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved districts"),
        @ApiResponse(responseCode = "500", description = "Error retrieving districts")
    })
    @GetMapping("/districts")
    public ResponseEntity<ResponseWrapper<List<DistrictDto>>> getDistricts(
            @Parameter(description = "Province ID", required = true) @RequestParam int province_id) {
        try {
            List<DistrictDto> districts = locationService.getDistricts(province_id);
            
            if (districts.isEmpty()) {
                return ResponseEntity.ok(ResponseWrapper.error("Không thể tải danh sách quận/huyện cho tỉnh/thành phố này"));
            }
            
            return ResponseEntity.ok(ResponseWrapper.success(districts));
            
        } catch (Exception e) {
            logger.error("Error getting districts for province {}", province_id, e);
            return ResponseEntity.ok(ResponseWrapper.error("Lỗi hệ thống khi tải danh sách quận/huyện"));
        }
    }
    
    /**
     * Get wards by district ID
     */
    @Operation(
        summary = "Get wards by district",
        description = "Retrieve all wards for a specific district from GHN API"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved wards"),
        @ApiResponse(responseCode = "500", description = "Error retrieving wards")
    })
    @GetMapping("/wards")
    public ResponseEntity<ResponseWrapper<List<WardDto>>> getWards(
            @Parameter(description = "District ID", required = true) @RequestParam int district_id) {
        try {
            List<WardDto> wards = locationService.getWards(district_id);
            
            if (wards.isEmpty()) {
                return ResponseEntity.ok(ResponseWrapper.error("Không thể tải danh sách phường/xã cho quận/huyện này"));
            }
            
            return ResponseEntity.ok(ResponseWrapper.success(wards));
            
        } catch (Exception e) {
            logger.error("Error getting wards for district {}", district_id, e);
            return ResponseEntity.ok(ResponseWrapper.error("Lỗi hệ thống khi tải danh sách phường/xã"));
        }
    }
    
    /**
     * Get all wards by province ID (for 2-level address mode)
     */
    @Operation(
        summary = "Get wards by province (2-level mode)",
        description = "Retrieve all wards for a province in 2-level address mode from GHN API"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved wards"),
        @ApiResponse(responseCode = "500", description = "Error retrieving wards")
    })
    @GetMapping("/wards-by-province")
    public ResponseEntity<ResponseWrapper<List<WardDto>>> getWardsByProvince(
            @Parameter(description = "Province ID", required = true) @RequestParam int province_id) {
        try {
            logger.info("Getting wards for province: {}", province_id);
            List<WardDto> wards = locationService.getWardsByProvince(province_id);
            
            if (wards.isEmpty()) {
                return ResponseEntity.ok(ResponseWrapper.error("Không thể tải danh sách phường/xã cho tỉnh/thành phố này"));
            }
            
            logger.info("Retrieved {} wards for province {}", wards.size(), province_id);
            return ResponseEntity.ok(ResponseWrapper.success(wards));
            
        } catch (Exception e) {
            logger.error("Error getting wards for province {}", province_id, e);
            return ResponseEntity.ok(ResponseWrapper.error("Lỗi hệ thống khi tải danh sách phường/xã"));
        }
    }
    
    /**
     * Check if GHN service is available
     */
    @Operation(
        summary = "Check GHN service status",
        description = "Check if GHN shipping service is available and accessible"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Service status retrieved"),
        @ApiResponse(responseCode = "500", description = "Error checking service status")
    })
    @GetMapping("/status")
    public ResponseEntity<ResponseWrapper<Boolean>> getServiceStatus() {
        try {
            boolean isAvailable = locationService.isGhnServiceAvailable();
            return ResponseEntity.ok(ResponseWrapper.success(isAvailable));
            
        } catch (Exception e) {
            logger.error("Error checking GHN service status", e);
            return ResponseEntity.ok(ResponseWrapper.error("Lỗi hệ thống khi kiểm tra trạng thái dịch vụ"));
        }
    }
}
