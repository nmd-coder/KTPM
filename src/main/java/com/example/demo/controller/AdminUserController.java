package com.example.demo.controller;

import com.example.demo.dto.CreateCustomerRequest;
import com.example.demo.dto.CustomerDTO;
import com.example.demo.dto.UpdateCustomerRequest;
import com.example.demo.dto.address.AddressDto;
import com.example.demo.dto.address.AddressUpsertDto;
import com.example.demo.service.CustomerService;
import com.example.demo.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

@Tag(name = "👥 Admin Users", description = "Admin user/customer management APIs")
@RestController
@RequestMapping("/admin/api/users")
@RequiredArgsConstructor
@Slf4j
public class AdminUserController {
    
    private final CustomerService customerService;
    private final com.example.demo.service.ExcelExportService excelExportService;
    private final AddressService addressService;
    
    /**
     * Get all customers (excludes employees) with filters
     */
    @Operation(summary = "Get all customers", description = "Retrieve paginated list of customers with optional filters")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String segment,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String search
    ) {
        try {
            Sort sort = sortDir.equalsIgnoreCase("asc") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
            
            Pageable pageable = PageRequest.of(page, size, sort);
            
            // Apply filters
            Page<CustomerDTO> customerPage;
            if (search != null && !search.trim().isEmpty()) {
                // Search by keyword (name, email, phone)
                customerPage = customerService.searchCustomers(search.trim(), pageable);
            } else {
                // Get all customers with optional filters
                customerPage = customerService.getCustomersWithFilters(
                    pageable, status, type, segment, fromDate, toDate
                );
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("customers", customerPage.getContent());
            response.put("currentPage", customerPage.getNumber());
            response.put("totalItems", customerPage.getTotalElements());
            response.put("totalPages", customerPage.getTotalPages());
            response.put("success", true);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting customers", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi khi lấy danh sách khách hàng: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get customer by ID
     */
    @Operation(summary = "Get customer by ID", description = "Retrieve customer details by ID")
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getCustomerById(
            @Parameter(description = "Customer ID") @PathVariable Long id) {
        try {
            CustomerDTO customer = customerService.getCustomerById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("customer", customer);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting customer with ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }
    
    /**
     * Search customers
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchCustomers(@RequestParam String keyword) {
        try {
            List<CustomerDTO> customers = customerService.searchCustomers(keyword);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("customers", customers);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error searching customers", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi khi tìm kiếm khách hàng: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Create new customer
     */
    @Operation(summary = "Create customer", description = "Create a new customer account")
    @PostMapping
    public ResponseEntity<Map<String, Object>> createCustomer(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Customer data") 
            @Valid @RequestBody CreateCustomerRequest request) {
        try {
            CustomerDTO customer = customerService.createCustomer(request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Thêm khách hàng thành công");
            response.put("customer", customer);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Validation error creating customer", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            log.error("Error creating customer", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi khi tạo khách hàng: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Update customer
     */
    @Operation(summary = "Update customer", description = "Update customer information")
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateCustomer(
            @Parameter(description = "Customer ID") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated customer data") 
            @Valid @RequestBody UpdateCustomerRequest request
    ) {
        try {
            CustomerDTO customer = customerService.updateCustomer(id, request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cập nhật khách hàng thành công");
            response.put("customer", customer);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Validation error updating customer", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            log.error("Error updating customer with ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi khi cập nhật khách hàng: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Delete customer
     */
    @Operation(summary = "Delete customer", description = "Delete customer account")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteCustomer(
            @Parameter(description = "Customer ID") @PathVariable Long id) {
        try {
            customerService.deleteCustomer(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Xóa khách hàng thành công");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting customer with ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Toggle customer status
     */
    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<Map<String, Object>> toggleCustomerStatus(@PathVariable Long id) {
        try {
            CustomerDTO customer = customerService.toggleCustomerStatus(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cập nhật trạng thái khách hàng thành công");
            response.put("customer", customer);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error toggling customer status with ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get customer statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCustomerStats() {
        try {
            Map<String, Object> stats = customerService.getCustomerStats();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("stats", stats);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting customer stats", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi khi lấy thống kê khách hàng: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get AI customer segment statistics
     */
    @Operation(summary = "Get segment stats", description = "Get customer counts by AI-generated segments (VIP, NEW, AT_RISK)")
    @GetMapping("/segment-stats")
    public ResponseEntity<Map<String, Object>> getSegmentStats() {
        try {
            Map<String, Object> segmentStats = customerService.getSegmentStats();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("stats", segmentStats);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting segment stats", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi khi lấy thống kê phân khúc: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    /**
     * Add address for a customer (Admin only)
     */
    @Operation(summary = "Add address for customer", description = "Admin adds a new address for a specific customer")
    @PostMapping("/{userId}/addresses")
    public ResponseEntity<Map<String, Object>> addAddressForCustomer(
            @Parameter(description = "Customer ID") @PathVariable Long userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Address data") 
            @Valid @RequestBody AddressUpsertDto addressDto) {
        try {
            // Verify customer exists
            CustomerDTO customer = customerService.getCustomerById(userId);
            
            // Create address for the customer
            AddressDto createdAddress = addressService.createOrUpdateAddress(userId, addressDto);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Thêm địa chỉ cho khách hàng thành công");
            response.put("address", createdAddress);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Validation error adding address for customer {}", userId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            
        } catch (Exception e) {
            log.error("Error adding address for customer {}", userId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi khi thêm địa chỉ cho khách hàng: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get addresses for a customer (Admin only)
     */
    @Operation(summary = "Get customer addresses", description = "Admin retrieves all addresses for a specific customer")
    @GetMapping("/{userId}/addresses")
    public ResponseEntity<Map<String, Object>> getCustomerAddresses(
            @Parameter(description = "Customer ID") @PathVariable Long userId) {
        try {
            // Verify customer exists
            CustomerDTO customer = customerService.getCustomerById(userId);
            
            // Get customer addresses
            List<AddressDto> addresses = addressService.getUserAddresses(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("addresses", addresses);
            response.put("customer", customer);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting addresses for customer {}", userId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi khi lấy danh sách địa chỉ: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Export users (customers) to Excel
     */
    @Operation(summary = "Export customers to Excel", description = "Export filtered customer list to Excel file")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportUsers(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String segment,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String search
    ) {
        try {
            java.util.List<com.example.demo.dto.CustomerDTO> list;
            if (search != null && !search.isBlank()) {
                list = customerService.searchCustomers(search.trim());
            } else {
                // get with filters then aggregate content
                var page = customerService.getCustomersWithFilters(org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE), status, type, segment, fromDate, toDate);
                list = page.getContent();
            }
            byte[] bytes = excelExportService.exportUsers(list);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users.xlsx");
            return ResponseEntity.ok().headers(headers).body(bytes);
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body(new byte[0]);
        }
    }
}
