package com.example.demo.service;

import com.example.demo.entity.StaffPresence;
import com.example.demo.entity.User;
import com.example.demo.entity.enums.StaffStatus;
import com.example.demo.entity.enums.UserRole;
import com.example.demo.repository.StaffPresenceRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing staff presence and availability
 * Tracks online status, workload, and availability for chat assignment
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StaffPresenceService {

    private final StaffPresenceRepository presenceRepository;
    private final UserRepository userRepository;

    /**
     * Initialize or get staff presence
     */
    @Transactional
    public StaffPresence initializePresence(Long staffId) {
        Optional<StaffPresence> existingPresence = presenceRepository.findByStaffId(staffId);
        
        if (existingPresence.isPresent()) {
            return existingPresence.get();
        }

        User staff = userRepository.findById(staffId)
            .orElseThrow(() -> new RuntimeException("Staff not found"));

        if (staff.getRole() != UserRole.STAFF && staff.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("User is not a staff member");
        }

        StaffPresence presence = new StaffPresence(staff);
        presence.setOnline(false);
        presence.setStatus(StaffStatus.OFFLINE);
        presence.setWorkload(0);
        presence.setMaxWorkload(5);
        
        return presenceRepository.save(presence);
    }

    /**
     * Update staff online status
     */
    @Transactional
    public void setOnline(Long staffId, boolean online) {
        log.info("Setting staff {} online status to: {}", staffId, online);
        
        StaffPresence presence = presenceRepository.findByStaffId(staffId)
            .orElseGet(() -> initializePresence(staffId));

        presence.setOnline(online);
        presence.setLastSeenAt(LocalDateTime.now());
        
        if (online) {
            presence.setStatus(StaffStatus.AVAILABLE);
        } else {
            presence.setStatus(StaffStatus.OFFLINE);
        }
        
        presenceRepository.save(presence);
        
        log.info("Staff {} is now {}", staffId, online ? "online" : "offline");
    }

    /**
     * Update staff status
     */
    @Transactional
    public void updateStatus(Long staffId, StaffStatus status) {
        log.info("Updating staff {} status to: {}", staffId, status);
        
        StaffPresence presence = presenceRepository.findByStaffId(staffId)
            .orElseGet(() -> initializePresence(staffId));

        presence.setStatus(status);
        presence.setLastActivityAt(LocalDateTime.now());
        
        presenceRepository.save(presence);
    }

    /**
     * Update staff status with message
     */
    @Transactional
    public void updateStatus(Long staffId, StaffStatus status, String statusMessage) {
        StaffPresence presence = presenceRepository.findByStaffId(staffId)
            .orElseGet(() -> initializePresence(staffId));

        presence.setStatus(status);
        presence.setStatusMessage(statusMessage);
        presence.setLastActivityAt(LocalDateTime.now());
        
        presenceRepository.save(presence);
    }

    /**
     * Increment staff workload
     */
    @Transactional
    public void incrementWorkload(Long staffId) {
        log.debug("Incrementing workload for staff {}", staffId);
        
        StaffPresence presence = presenceRepository.findByStaffId(staffId)
            .orElseGet(() -> initializePresence(staffId));

        presence.incrementWorkload();
        
        // Auto-update status if at max capacity
        if (presence.getWorkload() >= presence.getMaxWorkload()) {
            presence.setStatus(StaffStatus.BUSY);
        }
        
        presenceRepository.save(presence);
    }

    /**
     * Decrement staff workload
     */
    @Transactional
    public void decrementWorkload(Long staffId) {
        log.debug("Decrementing workload for staff {}", staffId);
        
        StaffPresence presence = presenceRepository.findByStaffId(staffId)
            .orElse(null);

        if (presence != null) {
            presence.decrementWorkload();
            
            // Auto-update status if no longer at max capacity
            if (presence.getWorkload() < presence.getMaxWorkload() && 
                presence.getStatus() == StaffStatus.BUSY && 
                presence.getOnline()) {
                presence.setStatus(StaffStatus.AVAILABLE);
            }
            
            presenceRepository.save(presence);
        }
    }

    /**
     * Get available staff (online and not at max workload)
     */
    @Transactional(readOnly = true)
    public List<User> getAvailableStaff() {
        List<StaffPresence> availablePresences = presenceRepository.findAvailableStaff();
        
        return availablePresences.stream()
            .map(StaffPresence::getStaff)
            .collect(Collectors.toList());
    }

    /**
     * Get staff with lowest workload
     */
    @Transactional(readOnly = true)
    public Optional<User> getStaffWithLowestWorkload() {
        List<StaffPresence> staffList = presenceRepository.findStaffWithLowestWorkload(
            PageRequest.of(0, 1)
        );
        
        if (staffList.isEmpty()) {
            return Optional.empty();
        }
        
        return Optional.of(staffList.get(0).getStaff());
    }

    /**
     * Get all online staff
     */
    @Transactional(readOnly = true)
    public List<User> getOnlineStaff() {
        List<StaffPresence> onlinePresences = presenceRepository.findByOnlineTrue();
        
        return onlinePresences.stream()
            .map(StaffPresence::getStaff)
            .collect(Collectors.toList());
    }

    /**
     * Get staff presence
     */
    @Transactional(readOnly = true)
    public Optional<StaffPresence> getPresence(Long staffId) {
        return presenceRepository.findByStaffId(staffId);
    }

    /**
     * Check if staff is available
     */
    @Transactional(readOnly = true)
    public boolean isAvailable(Long staffId) {
        Optional<StaffPresence> presence = presenceRepository.findByStaffId(staffId);
        return presence.map(StaffPresence::isAvailable).orElse(false);
    }

    /**
     * Get availability score for staff
     */
    @Transactional(readOnly = true)
    public double getAvailabilityScore(Long staffId) {
        Optional<StaffPresence> presence = presenceRepository.findByStaffId(staffId);
        return presence.map(StaffPresence::getAvailabilityScore).orElse(0.0);
    }

    /**
     * Count available staff
     */
    @Transactional(readOnly = true)
    public Long countAvailable() {
        return presenceRepository.countAvailableStaff();
    }

    /**
     * Count online staff
     */
    @Transactional(readOnly = true)
    public Long countOnline() {
        return presenceRepository.countByOnlineTrue();
    }

    /**
     * Update max workload for staff
     */
    @Transactional
    public void updateMaxWorkload(Long staffId, Integer maxWorkload) {
        StaffPresence presence = presenceRepository.findByStaffId(staffId)
            .orElseGet(() -> initializePresence(staffId));

        presence.setMaxWorkload(maxWorkload);
        presenceRepository.save(presence);
        
        log.info("Updated max workload for staff {} to {}", staffId, maxWorkload);
    }

    /**
     * Set all staff offline (for system restart/maintenance)
     */
    @Transactional
    public void setAllOffline() {
        log.info("Setting all staff offline");
        presenceRepository.setAllOffline();
    }

    /**
     * Heartbeat - update last seen time
     */
    @Transactional
    public void heartbeat(Long staffId) {
        StaffPresence presence = presenceRepository.findByStaffId(staffId)
            .orElse(null);

        if (presence != null && presence.getOnline()) {
            presence.setLastSeenAt(LocalDateTime.now());
            presenceRepository.save(presence);
        }
    }

    /**
     * Check for inactive staff and mark them offline
     * Should be called periodically (e.g., every 5 minutes)
     */
    @Transactional
    public void checkInactiveStaff() {
        LocalDateTime inactiveThreshold = LocalDateTime.now().minusMinutes(10);
        
        List<StaffPresence> onlineStaff = presenceRepository.findByOnlineTrue();
        
        for (StaffPresence presence : onlineStaff) {
            if (presence.getLastSeenAt().isBefore(inactiveThreshold)) {
                log.info("Marking staff {} as offline due to inactivity", presence.getStaffId());
                presence.setOnline(false);
                presence.setStatus(StaffStatus.OFFLINE);
                presenceRepository.save(presence);
            }
        }
    }
}

