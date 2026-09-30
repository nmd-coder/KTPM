package com.example.demo.repository;

import com.example.demo.entity.StaffPresence;
import com.example.demo.entity.enums.StaffStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for StaffPresence entity
 * Manages staff online status and workload
 */
@Repository
public interface StaffPresenceRepository extends JpaRepository<StaffPresence, Long> {

    /**
     * Find presence by staff ID
     */
    Optional<StaffPresence> findByStaffId(Long staffId);

    /**
     * Find all online staff
     */
    List<StaffPresence> findByOnlineTrue();

    /**
     * Find available staff (online and not at max workload)
     */
    @Query("SELECT sp FROM StaffPresence sp WHERE sp.online = true AND sp.status = 'AVAILABLE' AND sp.workload < sp.maxWorkload ORDER BY sp.workload ASC")
    List<StaffPresence> findAvailableStaff();

    /**
     * Find staff by status
     */
    List<StaffPresence> findByStatus(StaffStatus status);

    /**
     * Get staff with lowest workload
     */
    @Query("SELECT sp FROM StaffPresence sp WHERE sp.online = true AND sp.status = 'AVAILABLE' AND sp.workload < sp.maxWorkload ORDER BY sp.workload ASC, sp.lastActivityAt ASC")
    List<StaffPresence> findStaffWithLowestWorkload(org.springframework.data.domain.Pageable pageable);

    /**
     * Count online staff
     */
    Long countByOnlineTrue();

    /**
     * Count available staff
     */
    @Query("SELECT COUNT(sp) FROM StaffPresence sp WHERE sp.online = true AND sp.status = 'AVAILABLE' AND sp.workload < sp.maxWorkload")
    Long countAvailableStaff();

    /**
     * Update online status
     */
    @Modifying
    @Query("UPDATE StaffPresence sp SET sp.online = :online, sp.lastSeenAt = :lastSeen WHERE sp.staffId = :staffId")
    void updateOnlineStatus(@Param("staffId") Long staffId, @Param("online") Boolean online, @Param("lastSeen") LocalDateTime lastSeen);

    /**
     * Update status
     */
    @Modifying
    @Query("UPDATE StaffPresence sp SET sp.status = :status, sp.lastActivityAt = :lastActivity WHERE sp.staffId = :staffId")
    void updateStatus(@Param("staffId") Long staffId, @Param("status") StaffStatus status, @Param("lastActivity") LocalDateTime lastActivity);

    /**
     * Increment workload
     */
    @Modifying
    @Query("UPDATE StaffPresence sp SET sp.workload = sp.workload + 1, sp.lastActivityAt = :lastActivity WHERE sp.staffId = :staffId")
    void incrementWorkload(@Param("staffId") Long staffId, @Param("lastActivity") LocalDateTime lastActivity);

    /**
     * Decrement workload
     */
    @Modifying
    @Query("UPDATE StaffPresence sp SET sp.workload = CASE WHEN sp.workload > 0 THEN sp.workload - 1 ELSE 0 END, sp.lastActivityAt = :lastActivity WHERE sp.staffId = :staffId")
    void decrementWorkload(@Param("staffId") Long staffId, @Param("lastActivity") LocalDateTime lastActivity);

    /**
     * Set all staff offline (for system restart/maintenance)
     */
    @Modifying
    @Query("UPDATE StaffPresence sp SET sp.online = false, sp.status = 'OFFLINE'")
    void setAllOffline();
}

