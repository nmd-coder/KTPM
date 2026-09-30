package com.example.demo.repository;

import com.example.demo.entity.HandoffQueue;
import com.example.demo.entity.enums.HandoffReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for HandoffQueue entity
 * Manages conversation handoff queue
 */
@Repository
public interface HandoffQueueRepository extends JpaRepository<HandoffQueue, Long> {

    /**
     * Find handoff entry by conversation ID
     */
    Optional<HandoffQueue> findByConversationId(Long conversationId);

    /**
     * Check if conversation is in queue
     */
    boolean existsByConversationId(Long conversationId);

    /**
     * Get waiting queue (not yet assigned)
     */
    @Query("SELECT hq FROM HandoffQueue hq WHERE hq.assignedAt IS NULL AND hq.resolvedAt IS NULL ORDER BY hq.priority DESC, hq.enqueuedAt ASC")
    List<HandoffQueue> findWaitingQueue();

    /**
     * Get assigned queue (assigned but not resolved)
     */
    @Query("SELECT hq FROM HandoffQueue hq WHERE hq.assignedAt IS NOT NULL AND hq.resolvedAt IS NULL ORDER BY hq.assignedAt DESC")
    List<HandoffQueue> findAssignedQueue();

    /**
     * Get queue assigned to specific staff
     */
    @Query("SELECT hq FROM HandoffQueue hq WHERE hq.assignedToStaff.id = :staffId AND hq.resolvedAt IS NULL ORDER BY hq.assignedAt DESC")
    List<HandoffQueue> findByAssignedToStaffId(@Param("staffId") Long staffId);

    /**
     * Get queue by handoff reason
     */
    List<HandoffQueue> findByHandoffReason(HandoffReason reason);

    /**
     * Count waiting in queue
     */
    @Query("SELECT COUNT(hq) FROM HandoffQueue hq WHERE hq.assignedAt IS NULL AND hq.resolvedAt IS NULL")
    Long countWaiting();

    /**
     * Count assigned
     */
    @Query("SELECT COUNT(hq) FROM HandoffQueue hq WHERE hq.assignedAt IS NOT NULL AND hq.resolvedAt IS NULL")
    Long countAssigned();

    /**
     * Get average wait time
     */
    @Query("SELECT AVG(hq.waitTimeSeconds) FROM HandoffQueue hq WHERE hq.waitTimeSeconds IS NOT NULL")
    Double getAverageWaitTime();

    /**
     * Delete resolved entries older than specified days
     */
    @Query("DELETE FROM HandoffQueue hq WHERE hq.resolvedAt IS NOT NULL AND hq.resolvedAt < :cutoffDate")
    void deleteResolvedOlderThan(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);
}

