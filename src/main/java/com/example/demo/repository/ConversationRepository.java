package com.example.demo.repository;

import com.example.demo.entity.Conversation;
import com.example.demo.entity.enums.ConversationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Conversation entity
 * Following rules.mdc specifications for data access layer
 */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    
    /**
     * Find conversations by status, ordered by last message timestamp
     */
    List<Conversation> findByStatusOrderByLastMessageAtDesc(ConversationStatus status);
    
    /**
     * Find conversations by status with pagination
     */
    @RestResource(path = "findByStatusOrderByLastMessageAtDescPaged", rel = "findByStatusOrderByLastMessageAtDescPaged")
    Page<Conversation> findByStatusOrderByLastMessageAtDesc(ConversationStatus status, Pageable pageable);
    
    /**
     * Find conversations assigned to a staff member with specific statuses
     */
    List<Conversation> findByAssignedStaffIdAndStatusIn(Long staffId, List<ConversationStatus> statuses);
    
    /**
     * Find conversations assigned to a staff member with pagination
     */
    Page<Conversation> findByAssignedStaffIdAndStatusInOrderByLastMessageAtDesc(
        Long staffId, List<ConversationStatus> statuses, Pageable pageable);
    
    /**
     * Find active conversation for a customer (OPEN or ASSIGNED status)
     */
    @Query("SELECT c FROM Conversation c WHERE c.customer.id = :customerId " +
           "AND c.status IN :statuses ORDER BY c.lastMessageAt DESC")
    Optional<Conversation> findByCustomerIdAndStatusIn(
        @Param("customerId") Long customerId, 
        @Param("statuses") List<ConversationStatus> statuses);
    
    /**
     * Find the most recent conversation for a customer
     */
    Optional<Conversation> findFirstByCustomerIdOrderByCreatedAtDesc(Long customerId);
    
    /**
     * Count conversations by status and assigned staff
     */
    Long countByStatusAndAssignedStaffId(ConversationStatus status, Long staffId);
    
    /**
     * Count all conversations for a staff member
     */
    Long countByAssignedStaffId(Long staffId);
    
    /**
     * Count unassigned conversations (OPEN status)
     */
    Long countByStatus(ConversationStatus status);
    
    /**
     * Find all conversations for a customer
     */
    Page<Conversation> findByCustomerIdOrderByLastMessageAtDesc(Long customerId, Pageable pageable);
    
    /**
     * Get staff workload - count of active conversations
     */
    @Query("SELECT COUNT(c) FROM Conversation c WHERE c.assignedStaff.id = :staffId " +
           "AND c.status IN ('OPEN', 'ASSIGNED')")
    Long getStaffActiveConversationCount(@Param("staffId") Long staffId);
    
    /**
     * Find unassigned conversations ordered by creation time
     */
    @Query("SELECT c FROM Conversation c WHERE c.status = 'OPEN' " +
           "ORDER BY c.createdAt ASC")
    List<Conversation> findUnassignedConversations();
    
    /**
     * Find unassigned conversations with pagination
     */
    @Query("SELECT c FROM Conversation c WHERE c.status = 'OPEN' " +
           "ORDER BY c.createdAt ASC")
    @RestResource(path = "findUnassignedConversationsPaged", rel = "findUnassignedConversationsPaged")
    Page<Conversation> findUnassignedConversations(Pageable pageable);
    
    /**
     * Search conversations by customer name or email
     */
    @Query("SELECT c FROM Conversation c WHERE c.assignedStaff.id = :staffId " +
           "AND (LOWER(c.customer.firstname) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(c.customer.lastname) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(c.customer.email) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND c.status IN :statuses " +
           "ORDER BY c.lastMessageAt DESC")
    Page<Conversation> searchStaffConversations(
        @Param("staffId") Long staffId,
        @Param("search") String search,
        @Param("statuses") List<ConversationStatus> statuses,
        Pageable pageable);
    
    /**
     * Find idle conversations (for auto-close)
     * Find conversations with specific status and last message before threshold
     */
    @Query("SELECT c FROM Conversation c WHERE c.status = :status " +
           "AND c.lastMessageAt < :before " +
           "ORDER BY c.lastMessageAt ASC")
    List<Conversation> findIdleConversations(
        @Param("status") ConversationStatus status,
        @Param("before") LocalDateTime before);
}

