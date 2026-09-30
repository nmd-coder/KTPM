package com.example.demo.entity;

import com.example.demo.entity.enums.StaffStatus;
import com.example.demo.entity.enums.StaffStatusConverter;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity tracking staff presence and workload
 * Used for auto-assignment and load balancing
 */
@Entity
@Table(name = "staff_presence")
public class StaffPresence {

    @Id
    @Column(name = "staff_id")
    private Long staffId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "staff_id")
    private User staff;

    @Column(nullable = false)
    private Boolean online = false;

    @Column(nullable = false)
    private Integer workload = 0;

    @Column(name = "max_workload", nullable = false)
    private Integer maxWorkload = 5;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt = LocalDateTime.now();

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    @Column(length = 20)
    @Convert(converter = StaffStatusConverter.class)
    private StaffStatus status = StaffStatus.OFFLINE;

    @Column(name = "status_message", columnDefinition = "TEXT")
    private String statusMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public StaffPresence() {
    }

    public StaffPresence(User staff) {
        this.staff = staff;
        this.staffId = staff.getId();
    }

    // Helper methods
    public boolean isAvailable() {
        return online && status == StaffStatus.AVAILABLE && workload < maxWorkload;
    }

    public double getAvailabilityScore() {
        if (!isAvailable()) {
            return 0.0;
        }
        return (1.0 - ((double) workload / maxWorkload)) * 100;
    }

    public void incrementWorkload() {
        this.workload = Math.min(workload + 1, maxWorkload);
        this.lastActivityAt = LocalDateTime.now();
    }

    public void decrementWorkload() {
        this.workload = Math.max(workload - 1, 0);
        this.lastActivityAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getStaffId() {
        return staffId;
    }

    public void setStaffId(Long staffId) {
        this.staffId = staffId;
    }

    public User getStaff() {
        return staff;
    }

    public void setStaff(User staff) {
        this.staff = staff;
        if (staff != null) {
            this.staffId = staff.getId();
        }
    }

    public Boolean getOnline() {
        return online;
    }

    public void setOnline(Boolean online) {
        this.online = online;
        if (online) {
            this.lastSeenAt = LocalDateTime.now();
        }
    }

    public Integer getWorkload() {
        return workload;
    }

    public void setWorkload(Integer workload) {
        this.workload = workload;
    }

    public Integer getMaxWorkload() {
        return maxWorkload;
    }

    public void setMaxWorkload(Integer maxWorkload) {
        this.maxWorkload = maxWorkload;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public LocalDateTime getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(LocalDateTime lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public StaffStatus getStatus() {
        return status;
    }

    public void setStatus(StaffStatus status) {
        this.status = status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

