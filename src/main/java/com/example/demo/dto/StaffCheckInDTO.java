package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for staff check-in/check-out operations
 * Used for timesheet management
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffCheckInDTO {
    
    private Long timesheetId;
    private Long staffId;
    private String staffName;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime checkIn;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime checkOut;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
    
    private BigDecimal hoursWorked;
    private String notes;
    
    // Status flags
    private boolean isCheckedIn;
    private boolean isCheckedOut;
    
    // Helper methods
    public boolean isActiveShift() {
        return checkIn != null && checkOut == null;
    }
    
    public String getShiftStatus() {
        if (checkOut != null) {
            return "Đã check-out";
        } else if (checkIn != null) {
            return "Đang làm việc";
        } else {
            return "Chưa check-in";
        }
    }
}

