package com.example.demo.service;

import com.example.demo.dto.StaffCheckInDTO;
import com.example.demo.dto.StaffDashboardDTO;
import com.example.demo.entity.TimeSheet;
import com.example.demo.entity.User;
import com.example.demo.entity.enums.ConversationStatus;
import com.example.demo.entity.enums.UserRole;
import com.example.demo.repository.ConversationRepository;
import com.example.demo.repository.MessageRepository;
import com.example.demo.repository.TimeSheetRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for staff-specific operations
 * Handles check-in/check-out, workload, and dashboard data
 * Following rules.mdc specifications for business logic tier
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StaffService {

    private final TimeSheetRepository timeSheetRepository;
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ChatService chatService;

    /**
     * Check in a staff member
     */
    public StaffCheckInDTO checkIn(Long staffId) {
        log.info("Staff {} checking in", staffId);
        
        User staff = userRepository.findById(staffId)
            .orElseThrow(() -> new RuntimeException("Staff not found"));
        
        if (staff.getRole() != UserRole.STAFF && staff.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("User is not a staff member");
        }
        
        LocalDate today = LocalDate.now();
        
        // Check if already checked in today (and not checked out yet)
        Optional<TimeSheet> existingSheet = timeSheetRepository.findByStaffIdAndDate(staffId, today);
        if (existingSheet.isPresent()) {
            TimeSheet sheet = existingSheet.get();
            if (sheet.getCheckOut() == null) {
                // Already checked in and not checked out - return existing
                log.warn("Staff {} already checked in today at {}", staffId, sheet.getCheckIn());
                return convertToCheckInDTO(sheet);
            } else {
                // Already checked out today - prevent multiple check-ins in same day
                log.warn("Staff {} already completed shift today (checked out at {})", staffId, sheet.getCheckOut());
                throw new RuntimeException("Bạn đã hoàn thành ca làm việc hôm nay. Không thể check-in lại trong cùng ngày.");
            }
        }
        
        // Create new timesheet entry
        TimeSheet timeSheet = new TimeSheet(staff, LocalDateTime.now(), today);
        timeSheet = timeSheetRepository.save(timeSheet);
        
        log.info("Staff {} checked in successfully at {}", staffId, timeSheet.getCheckIn());
        return convertToCheckInDTO(timeSheet);
    }

    /**
     * Check out a staff member
     */
    public StaffCheckInDTO checkOut(Long staffId) {
        log.info("Staff {} checking out", staffId);
        
        LocalDate today = LocalDate.now();
        TimeSheet timeSheet = timeSheetRepository.findByStaffIdAndDate(staffId, today)
            .orElseThrow(() -> new RuntimeException("No check-in found for today"));
        
        if (timeSheet.getCheckOut() != null) {
            log.warn("Staff {} already checked out", staffId);
            return convertToCheckInDTO(timeSheet);
        }
        
        timeSheet.setCheckOut(LocalDateTime.now());
        timeSheet.calculateHoursWorked();
        timeSheet = timeSheetRepository.save(timeSheet);
        
        log.info("Staff {} checked out successfully at {}, worked {} hours", 
            staffId, timeSheet.getCheckOut(), timeSheet.getHoursWorked());
        return convertToCheckInDTO(timeSheet);
    }

    /**
     * Get current shift for a staff member
     */
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Optional<StaffCheckInDTO> getCurrentShift(Long staffId) {
        LocalDate today = LocalDate.now();
        return timeSheetRepository.findByStaffIdAndDate(staffId, today)
            .map(this::convertToCheckInDTO);
    }

    /**
     * Get staff workload (active conversation count)
     */
    @Transactional(readOnly = true)
    public Long getStaffWorkload(Long staffId) {
        return conversationRepository.getStaffActiveConversationCount(staffId);
    }

    /**
     * Get available staff (active and with low workload)
     */
    @Transactional(readOnly = true)
    public List<User> getAvailableStaff() {
        List<User> staffList = userRepository.findByRole(UserRole.STAFF);
        
        return staffList.stream()
            .filter(User::getIsActive)
            .filter(staff -> {
                Long workload = conversationRepository.getStaffActiveConversationCount(staff.getId());
                return workload < 5; // Staff with less than 5 active conversations
            })
            .collect(Collectors.toList());
    }

    /**
     * Get staff dashboard data
     */
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public StaffDashboardDTO getStaffDashboard(Long staffId) {
        log.info("Getting dashboard data for staff {}", staffId);
        
        User staff = userRepository.findById(staffId)
            .orElseThrow(() -> new RuntimeException("Staff not found"));
        
        StaffDashboardDTO dashboard = new StaffDashboardDTO();
        
        // Staff info
        dashboard.setStaffId(staff.getId());
        dashboard.setStaffName(staff.getFullName());
        dashboard.setStaffCode(staff.getEmployeeCode());
        dashboard.setPosition(staff.getPosition());
        
        // Current shift
        getCurrentShift(staffId).ifPresent(dashboard::setCurrentShift);
        
        // Conversation statistics
        Long activeConvs = conversationRepository.countByStatusAndAssignedStaffId(
            ConversationStatus.ASSIGNED, staffId);
        dashboard.setActiveConversations(activeConvs);
        dashboard.setAssignedConversations(conversationRepository.countByAssignedStaffId(staffId));
        dashboard.setUnreadMessages(messageRepository.countUnreadMessages(staffId));
        
        // Today's messages count
        // Note: This is a simplified count, you may want to add a more specific query
        dashboard.setTodayMessages(messageRepository.countUnreadMessages(staffId));
        
        // Work hours
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
        YearMonth currentMonth = YearMonth.from(today);
        LocalDate monthStart = currentMonth.atDay(1);
        
        // Today's hours
        timeSheetRepository.findByStaffIdAndDate(staffId, today)
            .ifPresent(ts -> dashboard.setTodayHoursWorked(ts.getHoursWorked()));
        
        // Week's hours
        BigDecimal weekHours = timeSheetRepository.getTotalHoursWorked(staffId, weekStart, today);
        dashboard.setWeekHoursWorked(weekHours != null ? weekHours : BigDecimal.ZERO);
        
        // Month's hours
        BigDecimal monthHours = timeSheetRepository.getTotalHoursWorked(
            staffId, monthStart, today);
        dashboard.setMonthHoursWorked(monthHours != null ? monthHours : BigDecimal.ZERO);
        
        // Recent conversations
        dashboard.setRecentConversations(
            chatService.getStaffConversations(staffId, 0, 5));
        
        // Unassigned queue (for all staff to see)
        dashboard.setUnassignedQueue(
            chatService.getUnassignedConversations(0, 5));
        
        // Performance metrics (placeholder - implement based on requirements)
        dashboard.setTotalResolvedToday(0L);
        dashboard.setAvgResponseTimeMinutes(0.0);
        
        log.info("Dashboard data retrieved successfully for staff {}", staffId);
        return dashboard;
    }

    /**
     * Get timesheet history for a staff member
     */
    @Transactional(readOnly = true)
    public List<StaffCheckInDTO> getTimesheetHistory(Long staffId, LocalDate startDate, LocalDate endDate) {
        log.info("Getting timesheet history for staff {} from {} to {}", staffId, startDate, endDate);
        
        List<TimeSheet> timeSheets = timeSheetRepository
            .findByStaffIdAndDateBetween(staffId, startDate, endDate);
        
        return timeSheets.stream()
            .map(this::convertToCheckInDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get monthly timesheet summary
     */
    @Transactional(readOnly = true)
    public List<StaffCheckInDTO> getMonthlyTimesheet(Long staffId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        
        return getTimesheetHistory(staffId, startDate, endDate);
    }

    /**
     * Update timesheet notes
     */
    public StaffCheckInDTO updateTimesheetNotes(Long timesheetId, String notes) {
        log.info("Updating notes for timesheet {}", timesheetId);
        
        TimeSheet timeSheet = timeSheetRepository.findById(timesheetId)
            .orElseThrow(() -> new RuntimeException("Timesheet not found"));
        
        timeSheet.setNotes(notes);
        timeSheet = timeSheetRepository.save(timeSheet);
        
        return convertToCheckInDTO(timeSheet);
    }

    // Helper methods

    private StaffCheckInDTO convertToCheckInDTO(TimeSheet timeSheet) {
        StaffCheckInDTO dto = new StaffCheckInDTO();
        dto.setTimesheetId(timeSheet.getId());
        dto.setStaffId(timeSheet.getStaff().getId());
        dto.setStaffName(timeSheet.getStaff().getFullName());
        dto.setCheckIn(timeSheet.getCheckIn());
        dto.setCheckOut(timeSheet.getCheckOut());
        dto.setDate(timeSheet.getDate());
        dto.setHoursWorked(timeSheet.getHoursWorked());
        dto.setNotes(timeSheet.getNotes());
        // Set boolean flags - use the correct setter names
        dto.setCheckedIn(timeSheet.getCheckIn() != null);
        dto.setCheckedOut(timeSheet.getCheckOut() != null);
        return dto;
    }
}

