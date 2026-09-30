package com.example.demo.repository;

import com.example.demo.entity.RoutingDecision;
import com.example.demo.entity.enums.IntentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for RoutingDecision entity
 * Handles AI routing decisions and analytics
 */
@Repository
public interface RoutingDecisionRepository extends JpaRepository<RoutingDecision, Long> {

    /**
     * Find routing decisions by conversation ID
     */
    List<RoutingDecision> findByConversationIdOrderByCreatedAtDesc(Long conversationId);

    /**
     * Find routing decisions by intent type
     */
    List<RoutingDecision> findByIntent(IntentType intent);

    /**
     * Find routing decisions that needed handoff
     */
    List<RoutingDecision> findByNeedHandoffTrue();

    /**
     * Count routing decisions within date range
     */
    @Query("SELECT COUNT(rd) FROM RoutingDecision rd WHERE rd.createdAt BETWEEN :startDate AND :endDate")
    Long countByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Count AI-handled messages (no handoff needed)
     */
    @Query("SELECT COUNT(rd) FROM RoutingDecision rd WHERE rd.needHandoff = false AND rd.createdAt BETWEEN :startDate AND :endDate")
    Long countAiHandledByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Count handoff messages
     */
    @Query("SELECT COUNT(rd) FROM RoutingDecision rd WHERE rd.needHandoff = true AND rd.createdAt BETWEEN :startDate AND :endDate")
    Long countHandoffByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Get average confidence score
     */
    @Query("SELECT AVG(rd.confidence) FROM RoutingDecision rd WHERE rd.createdAt BETWEEN :startDate AND :endDate")
    Double getAverageConfidence(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Get average processing time
     */
    @Query("SELECT AVG(rd.processingTimeMs) FROM RoutingDecision rd WHERE rd.createdAt BETWEEN :startDate AND :endDate")
    Double getAverageProcessingTime(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Get intent distribution
     */
    @Query("SELECT rd.intent, COUNT(rd) FROM RoutingDecision rd WHERE rd.createdAt BETWEEN :startDate AND :endDate GROUP BY rd.intent ORDER BY COUNT(rd) DESC")
    List<Object[]> getIntentDistribution(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Get handoff reason distribution
     */
    @Query("SELECT rd.handoffReason, COUNT(rd) FROM RoutingDecision rd WHERE rd.needHandoff = true AND rd.createdAt BETWEEN :startDate AND :endDate GROUP BY rd.handoffReason ORDER BY COUNT(rd) DESC")
    List<Object[]> getHandoffReasonDistribution(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Get recent routing decisions
     */
    @Query("SELECT rd FROM RoutingDecision rd ORDER BY rd.createdAt DESC")
    List<RoutingDecision> findRecentDecisions(org.springframework.data.domain.Pageable pageable);
}

