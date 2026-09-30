package com.example.demo.entity;

import com.example.demo.entity.enums.ConversationPriority;
import com.example.demo.entity.enums.ConversationPriorityConverter;
import com.example.demo.entity.enums.ConversationStatus;
import com.example.demo.entity.enums.ConversationStatusConverter;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Conversation Entity
 * Represents a chat conversation between a customer and staff member
 * Following rules.mdc specifications for MVC pattern
 */
@Entity
@Table(name = "Conversations")
public class Conversation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_staff_id")
    private User assignedStaff;

    @Column(nullable = false, length = 20)
    @Convert(converter = ConversationStatusConverter.class)
    private ConversationStatus status = ConversationStatus.OPEN;

    @Column(length = 20)
    @Convert(converter = ConversationPriorityConverter.class)
    private ConversationPriority priority = ConversationPriority.NORMAL;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Constructors
    public Conversation() {
    }

    public Conversation(User customer) {
        this.customer = customer;
        this.status = ConversationStatus.OPEN;
        this.priority = ConversationPriority.NORMAL;
    }

    public Conversation(User customer, ConversationPriority priority) {
        this.customer = customer;
        this.status = ConversationStatus.OPEN;
        this.priority = priority;
    }

    // Business methods
    public void assignToStaff(User staff) {
        this.assignedStaff = staff;
        this.status = ConversationStatus.ASSIGNED;
    }

    public void close() {
        this.status = ConversationStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
    }

    public void reopen() {
        this.status = ConversationStatus.ASSIGNED;
        this.closedAt = null;
    }

    public boolean isOpen() {
        return this.status == ConversationStatus.OPEN;
    }

    public boolean isAssigned() {
        return this.status == ConversationStatus.ASSIGNED;
    }

    public boolean isClosed() {
        return this.status == ConversationStatus.CLOSED;
    }

    // Getters and Setters

    public User getCustomer() {
        return customer;
    }

    public void setCustomer(User customer) {
        this.customer = customer;
    }

    public User getAssignedStaff() {
        return assignedStaff;
    }

    public void setAssignedStaff(User assignedStaff) {
        this.assignedStaff = assignedStaff;
    }

    public ConversationStatus getStatus() {
        return status;
    }

    public void setStatus(ConversationStatus status) {
        this.status = status;
    }

    public ConversationPriority getPriority() {
        return priority;
    }

    public void setPriority(ConversationPriority priority) {
        this.priority = priority;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

