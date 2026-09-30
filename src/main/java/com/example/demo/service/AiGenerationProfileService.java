package com.example.demo.service;

import com.example.demo.entity.enums.IntentType;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service for managing AI generation profiles based on intent and context
 * Maps different intents to optimal generation parameters
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiGenerationProfileService {

    /**
     * Generation profile configuration
     */
    @Data
    public static class GenerationProfile {
        private double temperature;
        private double topP;
        private int maxTokens;
        private int timeoutSeconds;
        private boolean enableStreaming;
        
        public GenerationProfile(double temperature, double topP, int maxTokens, int timeoutSeconds, boolean enableStreaming) {
            this.temperature = temperature;
            this.topP = topP;
            this.maxTokens = maxTokens;
            this.timeoutSeconds = timeoutSeconds;
            this.enableStreaming = enableStreaming;
        }
        
        public boolean isStreamingEnabled() {
            return enableStreaming;
        }
    }

    /**
     * Get generation profile for specific intent
     */
    public GenerationProfile getProfileForIntent(IntentType intent) {
        switch (intent) {
            case STORE_INFO:
                // Store policy/info often needs longer, complete answers
                // Increase maxTokens and timeout slightly to avoid truncation
                return new GenerationProfile(0.35, 0.85, 768, 20, true);
                
            case CHITCHAT:
                // More creative for casual conversation
                return new GenerationProfile(0.5, 0.9, 384, 15, true);
                
            case SALES:
                // Balanced creativity for product recommendations
                return new GenerationProfile(0.65, 0.9, 768, 20, true);
                
            case SHIPPING:
                // Medium creativity for shipping-related responses
                return new GenerationProfile(0.45, 0.85, 384, 15, true);
                
            case PROMOTION:
                // Medium creativity for promotion information
                return new GenerationProfile(0.4, 0.85, 512, 15, true);
                
            case ORDER_SUPPORT:
            case PAYMENT:
                // Very conservative for sensitive operations
                return new GenerationProfile(0.2, 0.7, 128, 10, false);
                
            case OTHER:
            default:
                // Default balanced profile
                return new GenerationProfile(0.5, 0.85, 384, 15, true);
        }
    }

    /**
     * Get profile for analysis phase (always consistent)
     */
    public GenerationProfile getAnalysisProfile() {
        // Analysis phase should be fast and consistent
        return new GenerationProfile(0.1, 0.7, 512, 10, false);
    }

    /**
     * Get profile with custom overrides
     */
    public GenerationProfile getProfileWithOverrides(IntentType intent, Map<String, Object> overrides) {
        GenerationProfile baseProfile = getProfileForIntent(intent);
        
        if (overrides != null) {
            if (overrides.containsKey("temperature")) {
                baseProfile.setTemperature(((Number) overrides.get("temperature")).doubleValue());
            }
            if (overrides.containsKey("topP")) {
                baseProfile.setTopP(((Number) overrides.get("topP")).doubleValue());
            }
            if (overrides.containsKey("maxTokens")) {
                baseProfile.setMaxTokens(((Number) overrides.get("maxTokens")).intValue());
            }
            if (overrides.containsKey("timeoutSeconds")) {
                baseProfile.setTimeoutSeconds(((Number) overrides.get("timeoutSeconds")).intValue());
            }
            if (overrides.containsKey("enableStreaming")) {
                baseProfile.setEnableStreaming((Boolean) overrides.get("enableStreaming"));
            }
        }
        
        return baseProfile;
    }

    /**
     * Log profile usage for monitoring
     */
    public void logProfileUsage(IntentType intent, GenerationProfile profile, long executionTimeMs) {
        log.info("Generation Profile Used - Intent: {}, Temp: {}, TopP: {}, MaxTokens: {}, ExecutionTime: {}ms", 
            intent, profile.getTemperature(), profile.getTopP(), profile.getMaxTokens(), executionTimeMs);
    }

    /**
     * Check if profile supports streaming for given intent
     */
    public boolean supportsStreaming(IntentType intent) {
        return getProfileForIntent(intent).isEnableStreaming();
    }

    /**
     * Get recommended timeout for intent
     */
    public int getTimeoutForIntent(IntentType intent) {
        return getProfileForIntent(intent).getTimeoutSeconds();
    }
}
