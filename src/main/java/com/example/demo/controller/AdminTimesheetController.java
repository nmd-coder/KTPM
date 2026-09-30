package com.example.demo.controller;

import com.example.demo.entity.TimeSheet;
import com.example.demo.entity.User;
import com.example.demo.entity.enums.UserRole;
import com.example.demo.repository.TimeSheetRepository;
import com.example.demo.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin/api/timesheets")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Timesheet Management", description = "APIs for managing employee timesheets")
public class AdminTimesheetController {

    private final TimeSheetRepository timeSheetRepository;
    private final UserRepository userRepository;

    /**
     * Get timesheet summary for all staff for a specific month
     */
    @Operation(summary = "Get timesheet summary", description = "Get aggregated timesheet data for all staff")
    @GetMapping("/summary")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getTimesheetSummary(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Use current month if not specified
            if (year == null) {
                year = LocalDate.now().getYear();
            }
            if (month == null) {
                month = LocalDate.now().getMonthValue();
            }
            
            log.info("Getting timesheet summary for month: {}/{}", month, year);
            
            // Get all active staff
            List<User> staffList = userRepository.findAll().stream()
                .filter(u -> UserRole.STAFF.equals(u.getRole()) && Boolean.TRUE.equals(u.getIsActive()))
                .toList();
            
            log.info("Found {} active staff members", staffList.size());
            
            List<Map<String, Object>> summary = new ArrayList<>();
            BigDecimal totalHoursAll = BigDecimal.ZERO;
            
            final int finalYear = year;
            final int finalMonth = month;
            
            for (User staff : staffList) {
                List<TimeSheet> timesheets = timeSheetRepository.findByStaffIdAndMonthAndYear(
                    staff.getId(), finalMonth, finalYear
                );
                
                if (!timesheets.isEmpty()) {
                    BigDecimal totalHours = timesheets.stream()
                        .map(TimeSheet::getHoursWorked)
                        .filter(java.util.Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    BigDecimal avgHours = totalHours.divide(
                        BigDecimal.valueOf(timesheets.size()), 
                        2, 
                        RoundingMode.HALF_UP
                    );
                    
                    Map<String, Object> staffData = new HashMap<>();
                    staffData.put("staffId", staff.getId());
                    staffData.put("staffName", staff.getFullName());
                    staffData.put("employeeCode", staff.getEmployeeCode());
                    staffData.put("totalDays", timesheets.size());
                    staffData.put("totalHours", totalHours);
                    staffData.put("avgHours", avgHours);
                    
                    summary.add(staffData);
                    totalHoursAll = totalHoursAll.add(totalHours);
                }
            }
            
            // Calculate stats
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalStaff", summary.size());
            stats.put("totalHours", totalHoursAll);
            stats.put("avgHours", summary.isEmpty() ? 0 : 
                totalHoursAll.divide(BigDecimal.valueOf(summary.size()), 2, RoundingMode.HALF_UP));
            
            response.put("success", true);
            response.put("summary", summary);
            response.put("stats", stats);
            response.put("month", month);
            response.put("year", year);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting timesheet summary", e);
            response.put("success", false);
            response.put("message", "Lỗi khi lấy dữ liệu chấm công: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
