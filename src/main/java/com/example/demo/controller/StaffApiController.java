package com.example.demo.controller;

import com.example.demo.dto.ConversationDTO;
import com.example.demo.dto.ResponseWrapper;
import com.example.demo.dto.StaffCheckInDTO;
import com.example.demo.dto.StaffDashboardDTO;
import com.example.demo.service.ChatService;
import com.example.demo.service.ConversationSupervisorService;
import com.example.demo.service.StaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * REST API Controller for Staff Operations
 * Handles check-in/check-out and conversation management
 * Following rules.mdc specifications for REST API
 */
@RestController
@RequestMapping("/api/staff")
@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Staff API", description = "Staff management and operations endpoints")
public class StaffApiController extends BaseController {

    private final StaffService staffService;
    private final ChatService chatService;
    private final ConversationSupervisorService conversationSupervisorService;

    /**
     * Check in staff member
     */
    @Operation(summary = "Check in", description = "Staff member checks in for work")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully checked in"),
        @ApiResponse(responseCode = "400", description = "Already checked in")
    })
    @PostMapping("/check-in")
    public ResponseEntity<ResponseWrapper<StaffCheckInDTO>> checkIn(Authentication authentication) {
        try {
            Long staffId = getUserIdFromAuthentication(authentication);
            if (staffId == null) {
                return ResponseEntity.status(401).body(new ResponseWrapper<>(null, "Unauthorized"));
            }
            log.info("Staff {} checking in", staffId);
            
            StaffCheckInDTO checkInDTO = staffService.checkIn(staffId);
            return ResponseEntity.ok(new ResponseWrapper<>(checkInDTO, "Check-in thành công"));
            
        } catch (Exception e) {
            log.error("Error checking in staff", e);
            return ResponseEntity.badRequest()
                .body(new ResponseWrapper<>(null, "Check-in thất bại: " + e.getMessage()));
        }
    }

    /**
     * Check out staff member
     */
    @Operation(summary = "Check out", description = "Staff member checks out from work")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully checked out"),
        @ApiResponse(responseCode = "400", description = "Not checked in")
    })
    @PostMapping("/check-out")
    public ResponseEntity<ResponseWrapper<StaffCheckInDTO>> checkOut(Authentication authentication) {
        try {
            Long staffId = getUserIdFromAuthentication(authentication);
            if (staffId == null) {
                return ResponseEntity.status(401).body(new ResponseWrapper<>(null, "Unauthorized"));
            }
            log.info("Staff {} checking out", staffId);
            
            StaffCheckInDTO checkOutDTO = staffService.checkOut(staffId);
            return ResponseEntity.ok(new ResponseWrapper<>(checkOutDTO, "Check-out thành công"));
            
        } catch (Exception e) {
            log.error("Error checking out staff", e);
            return ResponseEntity.badRequest()
                .body(new ResponseWrapper<>(null, "Check-out thất bại: " + e.getMessage()));
        }
    }

    /**
     * Get current shift status
     */
    @Operation(summary = "Get current shift", description = "Get current shift information")
    @GetMapping("/shift/current")
    public ResponseEntity<ResponseWrapper<StaffCheckInDTO>> getCurrentShift(Authentication authentication) {
        try {
            Long staffId = getUserIdFromAuthentication(authentication);
            if (staffId == null) {
                return ResponseEntity.status(401).body(new ResponseWrapper<>(null, "Unauthorized"));
            }
            
            return staffService.getCurrentShift(staffId)
                .map(shift -> ResponseEntity.ok(new ResponseWrapper<>(shift, null)))
                .orElse(ResponseEntity.ok(new ResponseWrapper<>(null, "Chưa check-in hôm nay")));
            
        } catch (Exception e) {
            log.error("Error getting current shift", e);
            return ResponseEntity.badRequest()
                .body(new ResponseWrapper<>(null, e.getMessage()));
        }
    }

    /**
     * Get staff dashboard data
     */
    @Operation(summary = "Get dashboard data", description = "Get staff dashboard statistics")
    @GetMapping("/dashboard")
    public ResponseEntity<ResponseWrapper<StaffDashboardDTO>> getDashboard(Authentication authentication) {
        try {
            Long staffId = getUserIdFromAuthentication(authentication);
            if (staffId == null) {
                return ResponseEntity.status(401).body(new ResponseWrapper<>(null, "Unauthorized"));
            }
            
            StaffDashboardDTO dashboard = staffService.getStaffDashboard(staffId);
            return ResponseEntity.ok(new ResponseWrapper<>(dashboard, null));
            
        } catch (Exception e) {
            log.error("Error getting dashboard data", e);
            return ResponseEntity.badRequest()
                .body(new ResponseWrapper<>(null, e.getMessage()));
        }
    }

    /**
     * Get staff's assigned conversations and unassigned conversations (queue)
     */
    @Operation(summary = "Get conversations", description = "Get all conversations for staff including assigned and unassigned")
    @GetMapping("/conversations")
    public ResponseEntity<ResponseWrapper<List<ConversationDTO>>> getConversations(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Long staffId = getUserIdFromAuthentication(authentication);
            if (staffId == null) {
                log.warn("Unauthorized access to conversations - no staff ID found");
                return ResponseEntity.status(401).body(new ResponseWrapper<>(null, "Unauthorized"));
            }
            
            log.info("Loading conversations for staff ID: {}, page: {}, size: {}", staffId, page, size);
            
            // Get assigned conversations
            List<ConversationDTO> assignedConversations = chatService.getStaffConversations(staffId, page, size);
            log.info("Found {} assigned conversations for staff {}", assignedConversations.size(), staffId);
            
            // Get unassigned conversations (queue) - only for first page
            List<ConversationDTO> unassignedConversations = new ArrayList<>();
            if (page == 0) {
                unassignedConversations = chatService.getUnassignedConversations(0, 10);
                log.info("Found {} unassigned conversations", unassignedConversations.size());
            }
            
            // Combine and return
            List<ConversationDTO> allConversations = new ArrayList<>();
            allConversations.addAll(assignedConversations);
            allConversations.addAll(unassignedConversations);
            
            log.info("Returning total {} conversations to staff {}", allConversations.size(), staffId);
            return ResponseEntity.ok(new ResponseWrapper<>(allConversations, null));
            
        } catch (Exception e) {
            log.error("Error getting staff conversations", e);
            return ResponseEntity.badRequest()
                .body(new ResponseWrapper<>(null, e.getMessage()));
        }
    }

    /**
     * Assign conversation to self
     */
    @Operation(summary = "Assign conversation", description = "Assign an unassigned conversation to this staff member")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully assigned"),
        @ApiResponse(responseCode = "400", description = "Assignment failed")
    })
    @PostMapping("/conversations/{id}/assign")
    public ResponseEntity<ResponseWrapper<ConversationDTO>> assignConversation(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            Long staffId = getUserIdFromAuthentication(authentication);
            if (staffId == null) {
                return ResponseEntity.status(401).body(new ResponseWrapper<>(null, "Unauthorized"));
            }
            log.info("Staff {} assigning conversation {}", staffId, id);
            
            ConversationDTO conversation = chatService.assignConversation(id, staffId);
            return ResponseEntity.ok(new ResponseWrapper<>(conversation, "Đã nhận cuộc hội thoại"));
            
        } catch (Exception e) {
            log.error("Error assigning conversation", e);
            return ResponseEntity.badRequest()
                .body(new ResponseWrapper<>(null, "Không thể nhận cuộc hội thoại: " + e.getMessage()));
        }
    }

    /**
     * Close a conversation
     */
    @Operation(summary = "Close conversation", description = "Mark a conversation as closed")
    @PostMapping("/conversations/{id}/close")
    public ResponseEntity<ResponseWrapper<ConversationDTO>> closeConversation(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            Long staffId = getUserIdFromAuthentication(authentication);
            if (staffId == null) {
                return ResponseEntity.status(401).body(new ResponseWrapper<>(null, "Unauthorized"));
            }
            log.info("Staff {} closing conversation {}", staffId, id);
            
            ConversationDTO conversation = chatService.closeConversation(id);
            return ResponseEntity.ok(new ResponseWrapper<>(conversation, "Đã đóng cuộc hội thoại"));
            
        } catch (Exception e) {
            log.error("Error closing conversation", e);
            return ResponseEntity.badRequest()
                .body(new ResponseWrapper<>(null, "Không thể đóng cuộc hội thoại: " + e.getMessage()));
        }
    }

    /**
     * Reopen a conversation
     */
    @Operation(summary = "Reopen conversation", description = "Reopen a closed conversation")
    @PostMapping("/conversations/{id}/reopen")
    public ResponseEntity<ResponseWrapper<ConversationDTO>> reopenConversation(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            Long staffId = getUserIdFromAuthentication(authentication);
            if (staffId == null) {
                return ResponseEntity.status(401).body(new ResponseWrapper<>(null, "Unauthorized"));
            }
            log.info("Staff {} reopening conversation {}", staffId, id);
            
            ConversationDTO conversation = chatService.reopenConversation(id);
            return ResponseEntity.ok(new ResponseWrapper<>(conversation, "Đã mở lại cuộc hội thoại"));
            
        } catch (Exception e) {
            log.error("Error reopening conversation", e);
            return ResponseEntity.badRequest()
                .body(new ResponseWrapper<>(null, "Không thể mở lại cuộc hội thoại: " + e.getMessage()));
        }
    }

    /**
     * Return conversation to AI
     * Staff can hand off back to AI assistant after 30 seconds if customer doesn't respond
     */
    @Operation(summary = "Return to AI", description = "Queue conversation to return to AI after 30 seconds if customer doesn't message")
    @PostMapping("/conversations/{id}/return-to-ai")
    public ResponseEntity<ResponseWrapper<String>> returnToAi(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            Long staffId = getUserIdFromAuthentication(authentication);
            if (staffId == null) {
                return ResponseEntity.status(401).body(new ResponseWrapper<>(null, "Unauthorized"));
            }
            log.info("Staff {} returning conversation {} to AI", staffId, id);
            
            conversationSupervisorService.queueReturnToAi(id);
            return ResponseEntity.ok(new ResponseWrapper<>("success", "Sẽ trao lại cho Hoa AI sau 30 giây nếu khách không nhắn tin"));
            
        } catch (Exception e) {
            log.error("Error returning conversation to AI", e);
            return ResponseEntity.badRequest()
                .body(new ResponseWrapper<>(null, "Không thể trao lại cho AI: " + e.getMessage()));
        }
    }

    /**
     * Search conversations
     */
    @Operation(summary = "Search conversations", description = "Search staff's conversations by customer name or email")
    @GetMapping("/conversations/search")
    public ResponseEntity<ResponseWrapper<List<ConversationDTO>>> searchConversations(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        try {
            Long staffId = getUserIdFromAuthentication(authentication);
            if (staffId == null) {
                return ResponseEntity.status(401).body(new ResponseWrapper<>(null, "Unauthorized"));
            }
            
            List<ConversationDTO> conversations = chatService.searchStaffConversations(staffId, q, page, size);
            return ResponseEntity.ok(new ResponseWrapper<>(conversations, null));
            
        } catch (Exception e) {
            log.error("Error searching conversations", e);
            return ResponseEntity.badRequest()
                .body(new ResponseWrapper<>(null, e.getMessage()));
        }
    }

    /**
     * Get timesheet history
     */
    @Operation(summary = "Get timesheet history", description = "Get timesheet records for a date range")
    @GetMapping("/timesheet")
    public ResponseEntity<ResponseWrapper<List<StaffCheckInDTO>>> getTimesheet(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Authentication authentication) {
        try {
            Long staffId = getUserIdFromAuthentication(authentication);
            if (staffId == null) {
                return ResponseEntity.status(401).body(new ResponseWrapper<>(null, "Unauthorized"));
            }
            
            LocalDate start = startDate != null ? LocalDate.parse(startDate) : 
                LocalDate.now().withDayOfMonth(1);
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
            
            List<StaffCheckInDTO> timesheet = staffService.getTimesheetHistory(staffId, start, end);
            return ResponseEntity.ok(new ResponseWrapper<>(timesheet, null));
            
        } catch (Exception e) {
            log.error("Error getting timesheet", e);
            return ResponseEntity.badRequest()
                .body(new ResponseWrapper<>(null, e.getMessage()));
        }
    }

    /**
     * Get monthly timesheet
     */
    @Operation(summary = "Get monthly timesheet", description = "Get timesheet for a specific month")
    @GetMapping("/timesheet/month")
    public ResponseEntity<ResponseWrapper<List<StaffCheckInDTO>>> getMonthlyTimesheet(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            Authentication authentication) {
        try {
            Long staffId = getUserIdFromAuthentication(authentication);
            if (staffId == null) {
                return ResponseEntity.status(401).body(new ResponseWrapper<>(null, "Unauthorized"));
            }
            
            YearMonth yearMonth = (year != null && month != null) ? 
                YearMonth.of(year, month) : YearMonth.now();
            
            List<StaffCheckInDTO> timesheet = staffService.getMonthlyTimesheet(
                staffId, yearMonth.getYear(), yearMonth.getMonthValue());
            return ResponseEntity.ok(new ResponseWrapper<>(timesheet, null));
            
        } catch (Exception e) {
            log.error("Error getting monthly timesheet", e);
            return ResponseEntity.badRequest()
                .body(new ResponseWrapper<>(null, e.getMessage()));
        }
    }

    /**
     * Update timesheet notes
     */
    @Operation(summary = "Update timesheet notes", description = "Add or update notes for a timesheet entry")
    @PutMapping("/timesheet/{id}/notes")
    public ResponseEntity<ResponseWrapper<StaffCheckInDTO>> updateTimesheetNotes(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String notes = request.get("notes");
            
            StaffCheckInDTO timesheet = staffService.updateTimesheetNotes(id, notes);
            return ResponseEntity.ok(new ResponseWrapper<>(timesheet, "Đã cập nhật ghi chú"));
            
        } catch (Exception e) {
            log.error("Error updating timesheet notes", e);
            return ResponseEntity.badRequest()
                .body(new ResponseWrapper<>(null, e.getMessage()));
        }
    }
}

