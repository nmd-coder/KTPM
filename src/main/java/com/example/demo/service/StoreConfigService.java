package com.example.demo.service;

import com.example.demo.entity.AiChatConfig;
import com.example.demo.repository.AiChatConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing store configuration
 * Provides store information for AI chat system
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StoreConfigService {

    private final AiChatConfigRepository configRepository;

    /**
     * Get configuration value by key
     */
    @Transactional(readOnly = true)
    public String getConfig(String key) {
        return configRepository.getValueByKey(key).orElse(null);
    }

    /**
     * Get configuration as integer
     */
    @Transactional(readOnly = true)
    public Integer getConfigAsInt(String key, Integer defaultValue) {
        Optional<AiChatConfig> config = configRepository.findByConfigKey(key);
        if (config.isPresent()) {
            Integer value = config.get().getAsInteger();
            return value != null ? value : defaultValue;
        }
        return defaultValue;
    }

    /**
     * Get configuration as double
     */
    @Transactional(readOnly = true)
    public Double getConfigAsDouble(String key, Double defaultValue) {
        Optional<AiChatConfig> config = configRepository.findByConfigKey(key);
        if (config.isPresent()) {
            Double value = config.get().getAsDouble();
            return value != null ? value : defaultValue;
        }
        return defaultValue;
    }

    /**
     * Get configuration as boolean
     */
    @Transactional(readOnly = true)
    public Boolean getConfigAsBoolean(String key, Boolean defaultValue) {
        Optional<AiChatConfig> config = configRepository.findByConfigKey(key);
        if (config.isPresent()) {
            return config.get().getAsBoolean();
        }
        return defaultValue;
    }

    /**
     * Get all configurations by prefix
     */
    @Transactional(readOnly = true)
    public Map<String, String> getConfigsByPrefix(String prefix) {
        List<AiChatConfig> configs = configRepository.findByKeyPrefix(prefix);
        Map<String, String> result = new HashMap<>();
        for (AiChatConfig config : configs) {
            result.put(config.getConfigKey(), config.getConfigValue());
        }
        return result;
    }

    /**
     * Get store information
     */
    @Transactional(readOnly = true)
    public Map<String, String> getStoreInfo() {
        return getConfigsByPrefix("store.");
    }

    /**
     * Get AI configuration
     */
    @Transactional(readOnly = true)
    public Map<String, String> getAiConfig() {
        return getConfigsByPrefix("ai.");
    }

    /**
     * Get policy information
     */
    @Transactional(readOnly = true)
    public Map<String, String> getPolicies() {
        return getConfigsByPrefix("policy.");
    }

    /**
     * Get store information as formatted text for AI
     */
    @Transactional(readOnly = true)
    public String getStoreInfoText() {
        Map<String, String> storeInfo = getStoreInfo();
        
        StringBuilder text = new StringBuilder();
        text.append("THÔNG TIN CỬA HÀNG:\n");
        text.append("- Tên: ").append(storeInfo.getOrDefault("store.name", "StarShop")).append("\n");
        text.append("- Địa chỉ: ").append(storeInfo.getOrDefault("store.address", "01 Võ Văn Ngân, TP. Thủ Đức, TP.HCM")).append("\n");
        text.append("- Hotline: ").append(storeInfo.getOrDefault("store.hotline", "1900 xxxx")).append("\n");
        text.append("- Email: ").append(storeInfo.getOrDefault("store.email", "starshop.a.6868@gmail.com")).append("\n");
        text.append("- Giờ mở cửa: ").append(storeInfo.getOrDefault("store.hours", "8:00 - 22:00 hàng ngày")).append("\n");
        
        return text.toString();
    }

    /**
     * Get policies as formatted text for AI
     */
    @Transactional(readOnly = true)
    public String getPoliciesText() {
        Map<String, String> policies = getPolicies();
        
        StringBuilder text = new StringBuilder();
        text.append("CHÍNH SÁCH:\n");
        text.append("- Vận chuyển: ").append(policies.getOrDefault("policy.shipping", "Giao hàng toàn TP.HCM")).append("\n");
        text.append("- Đổi trả: ").append(policies.getOrDefault("policy.return", "Đổi trả trong 24h")).append("\n");
        text.append("- Thanh toán: ").append(policies.getOrDefault("policy.payment", "COD, MoMo, chuyển khoản")).append("\n");
        
        return text.toString();
    }

    /**
     * Get AI confidence thresholds
     */
    @Transactional(readOnly = true)
    public Map<String, Double> getConfidenceThresholds() {
        Map<String, Double> thresholds = new HashMap<>();
        thresholds.put("auto", getConfigAsDouble("ai.confidence_threshold_auto", 0.80));
        thresholds.put("suggest", getConfigAsDouble("ai.confidence_threshold_suggest", 0.65));
        thresholds.put("handoff", getConfigAsDouble("ai.confidence_threshold_handoff", 0.65));
        return thresholds;
    }

    /**
     * Update configuration
     */
    @Transactional
    public void updateConfig(String key, String value) {
        Optional<AiChatConfig> configOpt = configRepository.findByConfigKey(key);
        if (configOpt.isPresent()) {
            AiChatConfig config = configOpt.get();
            config.setConfigValue(value);
            configRepository.save(config);
            log.info("Updated config: {} = {}", key, value);
        } else {
            log.warn("Config key not found: {}", key);
        }
    }

    /**
     * Create or update configuration
     */
    @Transactional
    public void setConfig(String key, String value, String type, String description) {
        Optional<AiChatConfig> configOpt = configRepository.findByConfigKey(key);
        if (configOpt.isPresent()) {
            AiChatConfig config = configOpt.get();
            config.setConfigValue(value);
            configRepository.save(config);
        } else {
            AiChatConfig config = new AiChatConfig(key, value, type);
            config.setDescription(description);
            configRepository.save(config);
        }
        log.info("Set config: {} = {}", key, value);
    }
}

