package com.example.demo.repository;

import com.example.demo.entity.AiChatConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for AiChatConfig entity
 * Manages AI chat system configuration
 */
@Repository
public interface AiChatConfigRepository extends JpaRepository<AiChatConfig, Long> {

    /**
     * Find configuration by key
     */
    Optional<AiChatConfig> findByConfigKey(String configKey);

    /**
     * Find all active configurations
     */
    List<AiChatConfig> findByIsActiveTrue();

    /**
     * Find configurations by type
     */
    List<AiChatConfig> findByConfigType(String configType);

    /**
     * Find configurations by key prefix
     */
    @Query("SELECT ac FROM AiChatConfig ac WHERE ac.configKey LIKE :prefix% AND ac.isActive = true")
    List<AiChatConfig> findByKeyPrefix(@Param("prefix") String prefix);

    /**
     * Check if configuration exists
     */
    boolean existsByConfigKey(String configKey);

    /**
     * Get configuration value by key
     */
    @Query("SELECT ac.configValue FROM AiChatConfig ac WHERE ac.configKey = :key AND ac.isActive = true")
    Optional<String> getValueByKey(@Param("key") String key);
}

