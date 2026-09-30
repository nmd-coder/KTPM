package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.service.VoucherService;
import com.example.demo.service.AdminAiInsightsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "🎫 Admin Vouchers", description = "Admin voucher management APIs")
@RestController
@RequestMapping("/admin/api/vouchers")
@RequiredArgsConstructor
@Slf4j
public class AdminVoucherController {
    
    private final VoucherService voucherService;
    private final com.example.demo.service.ExcelExportService excelExportService;
    private final AdminAiInsightsService adminAiInsightsService;
    
    /**
     * Get all vouchers with filters
     */
    @Operation(
        summary = "Get all vouchers with filters",
        description = "Retrieve paginated list of vouchers with optional filters (type, status, date, search)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Vouchers retrieved successfully"),
        @ApiResponse(responseCode = "500", description = "Error retrieving vouchers")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllVouchers(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "desc") String sortDir,
            @Parameter(description = "Filter by discount type") @RequestParam(required = false) String type,
            @Parameter(description = "Filter by status") @RequestParam(required = false) String status,
            @Parameter(description = "Filter from date") @RequestParam(required = false) String fromDate,
            @Parameter(description = "Search by code or name") @RequestParam(required = false) String search
    ) {
        try {
            Sort sort = sortDir.equalsIgnoreCase("asc") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
            
            Pageable pageable = PageRequest.of(page, size, sort);
            
            // Apply filters
            Page<VoucherDTO> voucherPage;
            if (search != null && !search.trim().isEmpty()) {
                // Search by keyword (code, name)
                voucherPage = voucherService.searchVouchers(search.trim(), pageable);
            } else {
                // Get all vouchers with optional filters
                voucherPage = voucherService.getVouchersWithFilters(
                    pageable, type, status, fromDate
                );
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("vouchers", voucherPage.getContent());
            response.put("currentPage", voucherPage.getNumber());
            response.put("totalItems", voucherPage.getTotalElements());
            response.put("totalPages", voucherPage.getTotalPages());
            response.put("success", true);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting vouchers", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi khi lấy danh sách voucher: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Export vouchers to Excel
     */
    @Operation(
        summary = "Export vouchers to Excel",
        description = "Export filtered vouchers to Excel file for download"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Excel file generated successfully"),
        @ApiResponse(responseCode = "500", description = "Error generating Excel file")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportVouchers(
            @Parameter(description = "Filter by discount type") @RequestParam(required = false) String type,
            @Parameter(description = "Filter by status") @RequestParam(required = false) String status,
            @Parameter(description = "Filter from date") @RequestParam(required = false) String fromDate,
            @Parameter(description = "Search by code or name") @RequestParam(required = false) String search
    ) {
        try {
            var page = voucherService.getVouchersWithFilters(org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE), type, status, fromDate);
            java.util.List<com.example.demo.dto.VoucherDTO> list = page.getContent();
            byte[] bytes = excelExportService.exportVouchers(list);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=vouchers.xlsx");
            return ResponseEntity.ok().headers(headers).body(bytes);
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body(new byte[0]);
        }
    }
    
    /**
     * Get voucher by ID
     */
    @Operation(
        summary = "Get voucher by ID",
        description = "Retrieve a specific voucher by its ID"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Voucher retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Voucher not found"),
        @ApiResponse(responseCode = "500", description = "Error retrieving voucher")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getVoucherById(
            @Parameter(description = "Voucher ID", required = true) @PathVariable Long id) {
        try {
            VoucherDTO voucher = voucherService.getVoucherById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("voucher", voucher);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting voucher with ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }
    
    /**
     * Get valid vouchers
     */
    @Operation(
        summary = "Get valid vouchers",
        description = "Retrieve all active and valid vouchers"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Valid vouchers retrieved successfully"),
        @ApiResponse(responseCode = "500", description = "Error retrieving valid vouchers")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/valid")
    public ResponseEntity<Map<String, Object>> getValidVouchers() {
        try {
            List<VoucherDTO> vouchers = voucherService.getValidVouchers();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("vouchers", vouchers);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting valid vouchers", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi khi lấy danh sách voucher hợp lệ: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Create new voucher
     */
    @Operation(
        summary = "Create new voucher",
        description = "Create a new voucher with specified discount type and value"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Voucher created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid voucher data"),
        @ApiResponse(responseCode = "500", description = "Error creating voucher")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<Map<String, Object>> createVoucher(@Valid @RequestBody CreateVoucherRequest request) {
        try {
            VoucherDTO voucher = voucherService.createVoucher(request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tạo voucher thành công");
            response.put("voucher", voucher);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Validation error creating voucher", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            log.error("Error creating voucher", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi khi tạo voucher: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Update voucher
     */
    @Operation(
        summary = "Update voucher",
        description = "Update an existing voucher with new data"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Voucher updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid voucher data"),
        @ApiResponse(responseCode = "404", description = "Voucher not found"),
        @ApiResponse(responseCode = "500", description = "Error updating voucher")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateVoucher(
            @Parameter(description = "Voucher ID", required = true) @PathVariable Long id,
            @Valid @RequestBody UpdateVoucherRequest request
    ) {
        try {
            VoucherDTO voucher = voucherService.updateVoucher(id, request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cập nhật voucher thành công");
            response.put("voucher", voucher);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Validation error updating voucher", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            log.error("Error updating voucher with ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi khi cập nhật voucher: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Delete voucher
     */
    @Operation(
        summary = "Delete voucher",
        description = "Delete a voucher by its ID"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Voucher deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Voucher not found"),
        @ApiResponse(responseCode = "500", description = "Error deleting voucher")
    })
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteVoucher(
            @Parameter(description = "Voucher ID", required = true) @PathVariable Long id) {
        try {
            voucherService.deleteVoucher(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Xóa voucher thành công");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting voucher with ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Toggle voucher status
     */
    @Operation(
        summary = "Toggle voucher status",
        description = "Toggle the active/inactive status of a voucher"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Voucher status toggled successfully"),
        @ApiResponse(responseCode = "404", description = "Voucher not found"),
        @ApiResponse(responseCode = "500", description = "Error toggling voucher status")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<Map<String, Object>> toggleVoucherStatus(
            @Parameter(description = "Voucher ID", required = true) @PathVariable Long id) {
        try {
            VoucherDTO voucher = voucherService.toggleVoucherStatus(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cập nhật trạng thái voucher thành công");
            response.put("voucher", voucher);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error toggling voucher status with ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Validate voucher
     */
    @Operation(
        summary = "Validate voucher",
        description = "Validate a voucher code against an order amount"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Voucher validation completed"),
        @ApiResponse(responseCode = "500", description = "Error validating voucher")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateVoucher(
            @Parameter(description = "Voucher code", required = true) @RequestParam String code,
            @Parameter(description = "Order amount", required = true) @RequestParam java.math.BigDecimal orderAmount
    ) {
        try {
            boolean isValid = voucherService.validateVoucher(code, orderAmount);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("valid", isValid);
            if (isValid) {
                VoucherDTO voucher = voucherService.getVoucherByCode(code);
                response.put("voucher", voucher);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error validating voucher", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("valid", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.ok(errorResponse);
        }
    }
    
    /**
     * Get AI suggestion for voucher configuration
     */
    @Operation(
        summary = "Get AI voucher suggestion",
        description = "Get AI-powered suggestions for voucher configuration based on objective and target product"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Voucher suggestion generated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "500", description = "Error generating suggestion")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/suggest")
    public ResponseWrapper<VoucherSuggestionResponse> suggestVoucher(
            @Valid @RequestBody VoucherSuggestionRequest request
    ) {
        try {
            log.info("AI voucher suggestion requested for objective: {}, targetProduct: {}", 
                request.getObjective(), request.getTargetProduct());
            
            VoucherSuggestionResponse suggestion = adminAiInsightsService.suggestVoucher(
                request.getObjective(), 
                request.getTargetProduct()
            );
            
            return ResponseWrapper.success(suggestion, "Đã tạo gợi ý voucher thành công");
            
        } catch (Exception e) {
            log.error("Error generating voucher suggestion", e);
            return ResponseWrapper.error("Lỗi khi tạo gợi ý voucher: " + e.getMessage());
        }
    }
}

