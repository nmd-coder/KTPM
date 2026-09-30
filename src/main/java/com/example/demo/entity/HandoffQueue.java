package com.example.demo.entity;

import com.example.demo.entity.enums.HandoffReason;
import com.example.demo.entity.enums.HandoffReasonConverter;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing conversations in the handoff queue
 * Tracks conversations waiting for or assigned to staff
 */
@Entity
@Table(name = "handoff_queue")
public class HandoffQueue extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false, unique = true)
    private Conversation conversation;

    @Column(nullable = false)
    private Integer priority = 0;

    @Column(name = "handoff_reason", nullable = false, length = 50)
    @Convert(converter = HandoffReasonConverter.class)
    private HandoffReason handoffReason;

    @Column(columnDefinition = "TEXT[]")
    private String[] tags; // Array of tag strings

    @Column(name = "customer_message", columnDefinition = "TEXT")
    private String customerMessage;

    @Column(name = "ai_context", columnDefinition = "TEXT")
    private String aiContext;

    @Column(name = "enqueued_at", nullable = false)
    private LocalDateTime enqueuedAt = LocalDateTime.now();

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_staff_id")
    private User assignedToStaff;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "wait_time_seconds")
    private Integer waitTimeSeconds;

    // Constructors
    public HandoffQueue() {
    }

    public HandoffQueue(Conversation conversation, HandoffReason handoffReason, Integer priority) {
        this.conversation = conversation;
        this.handoffReason = handoffReason;
        this.priority = priority;
    }

    // Helper methods
    public boolean isWaiting() {
        return assignedAt == null && resolvedAt == null;
    }

    public boolean isAssigned() {
        return assignedAt != null && resolvedAt == null;
    }

    public boolean isResolved() {
        return resolvedAt != null;
    }

    // Getters and Setters
    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public HandoffReason getHandoffReason() {
        return handoffReason;
    }

    public void setHandoffReason(HandoffReason handoffReason) {
        this.handoffReason = handoffReason;
    }

    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }

    public String getCustomerMessage() {
        return customerMessage;
    }

    public void setCustomerMessage(String customerMessage) {
        this.customerMessage = customerMessage;
    }

    public String getAiContext() {
        return aiContext;
    }

    public void setAiContext(String aiContext) {
        this.aiContext = aiContext;
    }

    public LocalDateTime getEnqueuedAt() {
        return enqueuedAt;
    }

    public void setEnqueuedAt(LocalDateTime enqueuedAt) {
        this.enqueuedAt = enqueuedAt;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public User getAssignedToStaff() {
        return assignedToStaff;
    }

    public void setAssignedToStaff(User assignedToStaff) {
        this.assignedToStaff = assignedToStaff;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public Integer getWaitTimeSeconds() {
        return waitTimeSeconds;
    }

    public void setWaitTimeSeconds(Integer waitTimeSeconds) {
        this.waitTimeSeconds = waitTimeSeconds;
    }
}

