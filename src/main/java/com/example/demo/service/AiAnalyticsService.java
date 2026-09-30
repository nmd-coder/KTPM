package com.example.demo.service;

import com.example.demo.entity.RoutingDecision;
import com.example.demo.entity.enums.IntentType;
import com.example.demo.entity.enums.HandoffReason;
import com.example.demo.repository.RoutingDecisionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for AI chat analytics and monitoring
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AiAnalyticsService {

    private final RoutingDecisionRepository routingDecisionRepository;

    /**
     * Get AI performance metrics
     */
    public AiPerformanceMetrics getPerformanceMetrics(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Calculating AI performance metrics from {} to {}", startDate, endDate);
        
        Long totalDecisions = routingDecisionRepository.countByDateRange(startDate, endDate);
        
        if (totalDecisions == 0) {
            return AiPerformanceMetrics.empty();
        }
        
        AiPerformanceMetrics metrics = new AiPerformanceMetrics();
        
        // Total decisions
        metrics.setTotalDecisions(totalDecisions.intValue());
        
        // AI handled vs handoff
        Long aiHandled = routingDecisionRepository.countAiHandledByDateRange(startDate, endDate);
        Long handoffCount = routingDecisionRepository.countHandoffByDateRange(startDate, endDate);
        
        metrics.setAiHandledCount(aiHandled);
        metrics.setHandoffCount(handoffCount);
        
        // Containment rate (% handled by AI)
        BigDecimal containmentRate = BigDecimal.valueOf(aiHandled)
            .divide(BigDecimal.valueOf(totalDecisions), 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
        metrics.setContainmentRate(containmentRate);
        
        // Average confidence
        Double avgConfidence = routingDecisionRepository.getAverageConfidence(startDate, endDate);
        metrics.setAverageConfidence(avgConfidence != null ? 
            BigDecimal.valueOf(avgConfidence).setScale(3, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        
        // Intent distribution
        List<Object[]> intentData = routingDecisionRepository.getIntentDistribution(startDate, endDate);
        Map<IntentType, Long> intentCounts = new HashMap<>();
        for (Object[] row : intentData) {
            intentCounts.put((IntentType) row[0], (Long) row[1]);
        }
        metrics.setIntentDistribution(intentCounts);
        
        // Handoff reasons
        List<Object[]> handoffData = routingDecisionRepository.getHandoffReasonDistribution(startDate, endDate);
        Map<HandoffReason, Long> handoffReasons = new HashMap<>();
        for (Object[] row : handoffData) {
            if (row[0] != null) {
                handoffReasons.put((HandoffReason) row[0], (Long) row[1]);
            }
        }
        metrics.setHandoffReasons(handoffReasons);
        
        return metrics;
    }

    /**
     * Get recent routing decisions
     */
    public List<RoutingDecisionSummary> getRecentDecisions(int limit) {
        log.info("Getting {} recent routing decisions", limit);
        
        org.springframework.data.domain.Pageable pageable = 
            org.springframework.data.domain.PageRequest.of(0, limit);
        List<RoutingDecision> decisions = routingDecisionRepository.findRecentDecisions(pageable);
        
        return decisions.stream()
            .map(this::convertToSummary)
            .collect(Collectors.toList());
    }

    /**
     * Get intent distribution for chart
     */
    public Map<String, Long> getIntentDistribution(LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> intentData = routingDecisionRepository.getIntentDistribution(startDate, endDate);
        
        Map<String, Long> distribution = new LinkedHashMap<>();
        for (Object[] row : intentData) {
            IntentType intent = (IntentType) row[0];
            Long count = (Long) row[1];
            distribution.put(intent.name(), count);
        }
        
        return distribution;
    }

    /**
     * Get handoff rate trend (by day)
     * This method is kept simple for now - can be optimized with native queries later
     */
    public Map<String, BigDecimal> getHandoffRateTrend(LocalDateTime startDate, LocalDateTime endDate) {
        // For simplicity, return empty map
        // In production, this would use a more efficient query
        return new LinkedHashMap<>();
    }

    /**
     * Convert RoutingDecision to summary
     */
    private RoutingDecisionSummary convertToSummary(RoutingDecision decision) {
        RoutingDecisionSummary summary = new RoutingDecisionSummary();
        summary.setId(decision.getId());
        summary.setConversationId(decision.getConversation().getId());
        summary.setIntent(decision.getIntent());
        summary.setConfidence(decision.getConfidence());
        summary.setNeedHandoff(decision.getNeedHandoff());
        // Note: reasons field might not exist in entity, using handoffReason instead
        if (decision.getHandoffReason() != null) {
            summary.setReasons(decision.getHandoffReason().name());
        }
        summary.setCreatedAt(decision.getCreatedAt());
        return summary;
    }

    /**
     * AI Performance Metrics DTO
     */
    public static class AiPerformanceMetrics {
        private Integer totalDecisions;
        private Long aiHandledCount;
        private Long handoffCount;
        private BigDecimal containmentRate;
        private BigDecimal averageConfidence;
        private Map<IntentType, Long> intentDistribution;
        private Map<HandoffReason, Long> handoffReasons;

        public static AiPerformanceMetrics empty() {
            AiPerformanceMetrics metrics = new AiPerformanceMetrics();
            metrics.setTotalDecisions(0);
            metrics.setAiHandledCount(0L);
            metrics.setHandoffCount(0L);
            metrics.setContainmentRate(BigDecimal.ZERO);
            metrics.setAverageConfidence(BigDecimal.ZERO);
            metrics.setIntentDistribution(new HashMap<>());
            metrics.setHandoffReasons(new HashMap<>());
            return metrics;
        }

        // Getters and setters
        public Integer getTotalDecisions() { return totalDecisions; }
        public void setTotalDecisions(Integer totalDecisions) { this.totalDecisions = totalDecisions; }

        public Long getAiHandledCount() { return aiHandledCount; }
        public void setAiHandledCount(Long aiHandledCount) { this.aiHandledCount = aiHandledCount; }

        public Long getHandoffCount() { return handoffCount; }
        public void setHandoffCount(Long handoffCount) { this.handoffCount = handoffCount; }

        public BigDecimal getContainmentRate() { return containmentRate; }
        public void setContainmentRate(BigDecimal containmentRate) { this.containmentRate = containmentRate; }

        public BigDecimal getAverageConfidence() { return averageConfidence; }
        public void setAverageConfidence(BigDecimal averageConfidence) { this.averageConfidence = averageConfidence; }

        public Map<IntentType, Long> getIntentDistribution() { return intentDistribution; }
        public void setIntentDistribution(Map<IntentType, Long> intentDistribution) { this.intentDistribution = intentDistribution; }

        public Map<HandoffReason, Long> getHandoffReasons() { return handoffReasons; }
        public void setHandoffReasons(Map<HandoffReason, Long> handoffReasons) { this.handoffReasons = handoffReasons; }
    }

    /**
     * Routing Decision Summary DTO
     */
    public static class RoutingDecisionSummary {
        private Long id;
        private Long conversationId;
        private IntentType intent;
        private BigDecimal confidence;
        private Boolean needHandoff;
        private String reasons;
        private LocalDateTime createdAt;

        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public Long getConversationId() { return conversationId; }
        public void setConversationId(Long conversationId) { this.conversationId = conversationId; }

        public IntentType getIntent() { return intent; }
        public void setIntent(IntentType intent) { this.intent = intent; }

        public BigDecimal getConfidence() { return confidence; }
        public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }

        public Boolean getNeedHandoff() { return needHandoff; }
        public void setNeedHandoff(Boolean needHandoff) { this.needHandoff = needHandoff; }

        public String getReasons() { return reasons; }
        public void setReasons(String reasons) { this.reasons = reasons; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}

