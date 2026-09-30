package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.service.EmployeeService;
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

@Tag(name = "👨‍💼 Admin Employees", description = "Admin employee/staff management APIs")
@RestController
@RequestMapping("/admin/api/employees")
@RequiredArgsConstructor
@Slf4j
public class AdminEmployeeController {
    
    private final EmployeeService employeeService;
    private final com.example.demo.service.ExcelExportService excelExportService;
    private final com.example.demo.repository.TimeSheetRepository timeSheetRepository;
    private final com.example.demo.repository.SalaryRepository salaryRepository;
    
    /**
     * Get all employees with filters
     */
    @Operation(summary = "Get all employees", description = "Retrieve paginated list of employees/staff with optional filters")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search
    ) {
        try {
            Sort sort = sortDir.equalsIgnoreCase("asc") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
            
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<EmployeeDTO> employeePage;
            
            // Apply filters with combination support
            if (search != null && !search.trim().isEmpty()) {
                // Search has highest priority
                employeePage = employeeService.searchEmployees(search, pageable);
            } else if ((role != null && !role.trim().isEmpty()) || (status != null && !status.trim().isEmpty())) {
                // Combine role and status filters
                Boolean isActive = null;
                if (status != null && !status.trim().isEmpty()) {
                    isActive = status.equalsIgnoreCase("active");
                }
                employeePage = employeeService.getEmployeesFiltered(role, isActive, pageable);
            } else {
                // No filters
                employeePage = employeeService.getEmployees(pageable);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("employees", employeePage.getContent());
            response.put("currentPage", employeePage.getNumber());
            response.put("totalItems", employeePage.getTotalElements());
            response.put("totalPages", employeePage.getTotalPages());
            response.put("success", true);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting employees", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi khi lấy danh sách nhân viên: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get employee by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getEmployeeById(@PathVariable Long id) {
        try {
            EmployeeDTO employee = employeeService.getEmployeeById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("employee", employee);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting employee with ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }
    
    /**
     * Create new employee
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createEmployee(@Valid @RequestBody CreateEmployeeRequest request) {
        try {
            EmployeeDTO employee = employeeService.createEmployee(request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Thêm nhân viên thành công");
            response.put("employee", employee);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Validation error creating employee", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            log.error("Error creating employee", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi khi tạo nhân viên: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Update employee
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeRequest request
    ) {
        try {
            EmployeeDTO employee = employeeService.updateEmployee(id, request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cập nhật nhân viên thành công");
            response.put("employee", employee);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Validation error updating employee", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            log.error("Error updating employee with ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi khi cập nhật nhân viên: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Delete employee
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteEmployee(@PathVariable Long id) {
        try {
            employeeService.deleteEmployee(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Xóa nhân viên thành công");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting employee with ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Toggle employee status
     */
    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<Map<String, Object>> toggleEmployeeStatus(@PathVariable Long id) {
        try {
            EmployeeDTO employee = employeeService.toggleEmployeeStatus(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cập nhật trạng thái nhân viên thành công");
            response.put("employee", employee);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error toggling employee status with ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Search employees
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchEmployees(@RequestParam String keyword) {
        try {
            List<EmployeeDTO> employees = employeeService.searchEmployees(keyword);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("employees", employees);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error searching employees", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi khi tìm kiếm nhân viên: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get employee statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getEmployeeStats() {
        try {
            Map<String, Object> stats = employeeService.getEmployeeStats();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("stats", stats);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting employee stats", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi khi lấy thống kê nhân viên: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Export employees to Excel
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportEmployees(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String search
    ) {
        try {
            java.util.List<com.example.demo.dto.EmployeeDTO> list;
            if (search != null && !search.isBlank()) {
                list = employeeService.searchEmployees(search.trim());
            } else if (role != null || isActive != null) {
                var page = employeeService.getEmployeesFiltered(role, isActive, org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE));
                list = page.getContent();
            } else {
                list = employeeService.getAllEmployees();
            }
            byte[] bytes = excelExportService.exportEmployees(list);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=employees.xlsx");
            return ResponseEntity.ok().headers(headers).body(bytes);
        } catch (Exception e) {
            log.error("Export employees failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new byte[0]);
        }
    }
    
    /**
     * Get employee's timesheets for specified month/year
     */
    @Operation(summary = "Get employee timesheets", description = "Get employee's timesheets for specified month and year")
    @GetMapping("/{id}/timesheets")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getEmployeeTimesheets(
            @PathVariable Long id,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        try {
            // Use current month/year if not specified
            if (month == null) {
                month = java.time.LocalDate.now().getMonthValue();
            }
            if (year == null) {
                year = java.time.LocalDate.now().getYear();
            }
            
            List<com.example.demo.entity.TimeSheet> timesheets = timeSheetRepository.findByStaffIdAndMonthAndYear(id, month, year);
            
            // Convert to simple map to avoid lazy loading issues
            List<Map<String, Object>> timesheetData = timesheets.stream()
                .map(t -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", t.getId());
                    data.put("date", t.getDate());
                    data.put("checkIn", t.getCheckIn());
                    data.put("checkOut", t.getCheckOut());
                    data.put("hoursWorked", t.getHoursWorked());
                    data.put("notes", t.getNotes());
                    return data;
                })
                .collect(java.util.stream.Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("timesheets", timesheetData);
            response.put("totalHours", timesheets.stream()
                .map(com.example.demo.entity.TimeSheet::getHoursWorked)
                .filter(java.util.Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting timesheets for employee ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi khi lấy bảng chấm công: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get employee's salary for specified month/year
     */
    @Operation(summary = "Get employee salary", description = "Get employee's salary for specified month and year")
    @GetMapping("/{id}/salary")
    public ResponseEntity<Map<String, Object>> getEmployeeSalary(
            @PathVariable Long id,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        try {
            // Use current month/year if not specified
            if (month == null) {
                month = java.time.LocalDate.now().getMonthValue();
            }
            if (year == null) {
                year = java.time.LocalDate.now().getYear();
            }
            
            java.util.Optional<com.example.demo.entity.Salary> salaryOpt = salaryRepository.findByUserIdAndMonthAndYear(id, month, year);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            
            if (salaryOpt.isPresent()) {
                response.put("salary", salaryOpt.get());
            } else {
                response.put("salary", null);
                response.put("message", "Chưa có bản ghi lương tháng này");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting salary for employee ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi khi lấy thông tin lương: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}