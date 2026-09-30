package com.example.demo.entity;

import jakarta.persistence.*;

/**
 * Entity for AI chat system configuration
 * Stores dynamic configuration that can be updated without code changes
 */
@Entity
@Table(name = "ai_chat_config")
public class AiChatConfig extends BaseEntity {

    @Column(name = "config_key", nullable = false, unique = true, length = 100)
    private String configKey;

    @Column(name = "config_value", nullable = false, columnDefinition = "TEXT")
    private String configValue;

    @Column(name = "config_type", nullable = false, length = 20)
    private String configType = "STRING";

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // Constructors
    public AiChatConfig() {
    }

    public AiChatConfig(String configKey, String configValue, String configType) {
        this.configKey = configKey;
        this.configValue = configValue;
        this.configType = configType;
    }

    // Helper methods
    public String getAsString() {
        return configValue;
    }

    public Integer getAsInteger() {
        try {
            return Integer.parseInt(configValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Double getAsDouble() {
        try {
            return Double.parseDouble(configValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Boolean getAsBoolean() {
        return Boolean.parseBoolean(configValue);
    }

    // Getters and Setters
    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public String getConfigType() {
        return configType;
    }

    public void setConfigType(String configType) {
        this.configType = configType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}

