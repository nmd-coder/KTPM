package com.example.demo.entity;

import com.example.demo.entity.enums.HandoffReason;
import com.example.demo.entity.enums.HandoffReasonConverter;
import com.example.demo.entity.enums.IntentType;
import com.example.demo.entity.enums.IntentTypeConverter;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing AI routing decisions for chat messages
 * Stores intent analysis, confidence scores, and handoff decisions
 */
@Entity
@Table(name = "routing_decisions")
public class RoutingDecision extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private Message message;

    @Column(nullable = false, length = 50)
    @Convert(converter = IntentTypeConverter.class)
    private IntentType intent;

    @Column(nullable = false, precision = 4, scale = 3)
    private BigDecimal confidence;

    @Column(name = "need_handoff", nullable = false)
    private Boolean needHandoff = false;

    @Column(name = "suggest_handoff", nullable = false)
    private Boolean suggestHandoff = false;

    @Column(name = "handoff_reason", length = 50)
    @Convert(converter = HandoffReasonConverter.class)
    private HandoffReason handoffReason;

    @Column(name = "ai_response", columnDefinition = "TEXT")
    private String aiResponse;

    @Column(name = "tools_used", columnDefinition = "TEXT[]")
    private String[] toolsUsed; // Array of tool names

    @Column(name = "processing_time_ms")
    private Integer processingTimeMs;

    // Constructors
    public RoutingDecision() {
    }

    public RoutingDecision(Conversation conversation, IntentType intent, BigDecimal confidence) {
        this.conversation = conversation;
        this.intent = intent;
        this.confidence = confidence;
    }

    // Getters and Setters
    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public IntentType getIntent() {
        return intent;
    }

    public void setIntent(IntentType intent) {
        this.intent = intent;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public Boolean getNeedHandoff() {
        return needHandoff;
    }

    public void setNeedHandoff(Boolean needHandoff) {
        this.needHandoff = needHandoff;
    }

    public Boolean getSuggestHandoff() {
        return suggestHandoff;
    }

    public void setSuggestHandoff(Boolean suggestHandoff) {
        this.suggestHandoff = suggestHandoff;
    }

    public HandoffReason getHandoffReason() {
        return handoffReason;
    }

    public void setHandoffReason(HandoffReason handoffReason) {
        this.handoffReason = handoffReason;
    }

    public String getAiResponse() {
        return aiResponse;
    }

    public void setAiResponse(String aiResponse) {
        this.aiResponse = aiResponse;
    }

    public String[] getToolsUsed() {
        return toolsUsed;
    }

    public void setToolsUsed(String[] toolsUsed) {
        this.toolsUsed = toolsUsed;
    }

    public Integer getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(Integer processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
}

