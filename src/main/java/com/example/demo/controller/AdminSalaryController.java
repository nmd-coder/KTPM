package com.example.demo.controller;

import com.example.demo.dto.SalaryResponse;
import com.example.demo.entity.Salary;
import com.example.demo.entity.enums.SalaryStatus;
import com.example.demo.repository.SalaryRepository;
import com.example.demo.scheduler.SalaryScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/admin/api/salaries")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Salary Management", description = "APIs for managing employee salaries")
public class AdminSalaryController {

    private final SalaryScheduler salaryScheduler;
    private final SalaryRepository salaryRepository;

    /**
     * Manually trigger salary generation for a specific month
     */
    @Operation(summary = "Generate salaries for specific month", 
               description = "Manually trigger salary calculation for all employees for a specific month")
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateSalaries(
            @RequestParam Integer year,
            @RequestParam Integer month) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            YearMonth yearMonth = YearMonth.of(year, month);
            
            log.info("Admin manually triggering salary generation for: {}", yearMonth);
            
            salaryScheduler.generateSalariesForMonth(yearMonth);
            
            response.put("success", true);
            response.put("message", String.format("Đã tính lương thành công cho tháng %d/%d", month, year));
            response.put("yearMonth", yearMonth.toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error generating salaries", e);
            response.put("success", false);
            response.put("message", "Lỗi khi tính lương: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Generate salaries for current month
     */
    @Operation(summary = "Generate salaries for current month", 
               description = "Manually trigger salary calculation for current month")
    @PostMapping("/generate/current")
    public ResponseEntity<Map<String, Object>> generateCurrentMonthSalaries() {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            YearMonth currentMonth = YearMonth.now();
            
            log.info("Admin manually triggering salary generation for current month: {}", currentMonth);
            
            salaryScheduler.generateSalariesForMonth(currentMonth);
            
            response.put("success", true);
            response.put("message", "Đã tính lương thành công cho tháng hiện tại");
            response.put("yearMonth", currentMonth.toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error generating current month salaries", e);
            response.put("success", false);
            response.put("message", "Lỗi khi tính lương: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Get all salaries for a specific month
     */
    @Operation(summary = "Get all salaries", description = "Get salary records for all employees for a specific month")
    @GetMapping("")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getAllSalaries(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Use current month if not specified
            final int finalYear = (year == null) ? LocalDate.now().getYear() : year;
            final int finalMonth = (month == null) ? LocalDate.now().getMonthValue() : month;
            
            log.info("Getting salaries for month: {}/{}", finalMonth, finalYear);
            
            // Get all salaries and filter by month/year
            List<SalaryResponse> salaries = salaryRepository.findAll().stream()
                .filter(s -> s.getMonthYear() != null 
                    && s.getMonthYear().getYear() == finalYear 
                    && s.getMonthYear().getMonthValue() == finalMonth)
                .map(SalaryResponse::fromEntity)
                .collect(Collectors.toList());
            
            log.info("Found {} salaries for month {}/{}", salaries.size(), finalMonth, finalYear);
            
            response.put("success", true);
            response.put("salaries", salaries);
            response.put("month", finalMonth);
            response.put("year", finalYear);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting salaries for month {}/{}", month, year, e);
            response.put("success", false);
            response.put("message", "Lỗi khi lấy dữ liệu lương: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            response.put("stackTrace", e.getStackTrace()[0].toString());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Mark salary as paid
     */
    @Operation(summary = "Mark salary as paid", description = "Update salary status to PAID")
    @PostMapping("/{id}/mark-paid")
    public ResponseEntity<Map<String, Object>> markAsPaid(@PathVariable Long id) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Salary salary = salaryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bản ghi lương"));
            
            salary.setStatus(SalaryStatus.PAID);
            salaryRepository.save(salary);
            
            response.put("success", true);
            response.put("message", "Đã cập nhật trạng thái thành công");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error marking salary as paid", e);
            response.put("success", false);
            response.put("message", "Lỗi khi cập nhật: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
