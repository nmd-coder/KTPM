package com.example.demo.service;

import com.example.demo.entity.enums.IntentType;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for monitoring AI chat performance and safety
 * Tracks metrics, errors, and performance indicators
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiMonitoringService {

    // Performance metrics
    private final Map<String, AtomicLong> performanceMetrics = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> counterMetrics = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> latencyHistory = new ConcurrentHashMap<>();
    
    // Error tracking
    private final Map<String, AtomicInteger> errorCounts = new ConcurrentHashMap<>();
    private final List<ErrorEvent> recentErrors = Collections.synchronizedList(new ArrayList<>());
    
    // Safety monitoring
    private final Map<String, AtomicInteger> safetyEvents = new ConcurrentHashMap<>();
    private final List<SafetyAlert> safetyAlerts = Collections.synchronizedList(new ArrayList<>());
    
    // Cache metrics
    private final Map<String, CacheMetrics> cacheMetrics = new ConcurrentHashMap<>();
    
    /**
     * Record AI generation latency
     */
    public void recordGenerationLatency(IntentType intent, long latencyMs) {
        String key = "generation_latency_" + intent.name().toLowerCase();
        performanceMetrics.computeIfAbsent(key, k -> new AtomicLong(0)).addAndGet(latencyMs);
        
        // Keep latency history for percentile calculations
        latencyHistory.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>())).add(latencyMs);
        
        // Keep only last 1000 entries
        List<Long> history = latencyHistory.get(key);
        if (history.size() > 1000) {
            history.remove(0);
        }
        
        // Increment request count
        counterMetrics.computeIfAbsent("requests_" + intent.name().toLowerCase(), k -> new AtomicInteger(0)).incrementAndGet();
        
        log.debug("📊 Generation latency recorded: {} = {}ms", key, latencyMs);
    }

    /**
     * Record tool execution latency
     */
    public void recordToolLatency(String toolName, long latencyMs, boolean isParallel) {
        String key = "tool_latency_" + toolName + (isParallel ? "_parallel" : "_sequential");
        performanceMetrics.computeIfAbsent(key, k -> new AtomicLong(0)).addAndGet(latencyMs);
        
        latencyHistory.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>())).add(latencyMs);
        
        counterMetrics.computeIfAbsent("tool_calls_" + toolName, k -> new AtomicInteger(0)).incrementAndGet();
        
        if (isParallel) {
            counterMetrics.computeIfAbsent("parallel_executions", k -> new AtomicInteger(0)).incrementAndGet();
        }
        
        log.debug("🔧 Tool latency recorded: {} = {}ms", key, latencyMs);
    }

    /**
     * Record cache hit/miss
     */
    public void recordCacheEvent(String cacheType, boolean hit) {
        CacheMetrics metrics = cacheMetrics.computeIfAbsent(cacheType, k -> new CacheMetrics());
        if (hit) {
            metrics.hits.incrementAndGet();
        } else {
            metrics.misses.incrementAndGet();
        }
        
        log.debug("💾 Cache event: {} = {}", cacheType, hit ? "HIT" : "MISS");
    }

    /**
     * Record retry attempt
     */
    public void recordRetryAttempt(String operation, int attemptNumber, boolean successful) {
        String key = "retries_" + operation;
        counterMetrics.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
        
        if (successful) {
            counterMetrics.computeIfAbsent(key + "_successful", k -> new AtomicInteger(0)).incrementAndGet();
        }
        
        if (attemptNumber > 1) {
            counterMetrics.computeIfAbsent("retry_recoveries", k -> new AtomicInteger(0)).incrementAndGet();
        }
        
        log.debug("🔄 Retry recorded: {} attempt {} = {}", operation, attemptNumber, successful ? "SUCCESS" : "FAIL");
    }

    /**
     * Record error event
     */
    public void recordError(String operation, String errorType, String errorMessage, Exception exception) {
        String key = "errors_" + operation + "_" + errorType;
        errorCounts.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
        
        ErrorEvent errorEvent = new ErrorEvent();
        errorEvent.timestamp = LocalDateTime.now();
        errorEvent.operation = operation;
        errorEvent.errorType = errorType;
        errorEvent.errorMessage = errorMessage;
        errorEvent.exceptionClass = exception != null ? exception.getClass().getSimpleName() : null;
        
        recentErrors.add(errorEvent);
        
        // Keep only last 100 errors
        if (recentErrors.size() > 100) {
            recentErrors.remove(0);
        }
        
        log.error("❌ Error recorded: {} - {} - {}", operation, errorType, errorMessage, exception);
    }

    /**
     * Record safety event (PII detection, inappropriate content, etc.)
     */
    public void recordSafetyEvent(String eventType, String details, String conversationId) {
        String key = "safety_" + eventType;
        safetyEvents.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
        
        SafetyAlert alert = new SafetyAlert();
        alert.timestamp = LocalDateTime.now();
        alert.eventType = eventType;
        alert.details = details;
        alert.conversationId = conversationId;
        
        safetyAlerts.add(alert);
        
        // Keep only last 50 safety alerts
        if (safetyAlerts.size() > 50) {
            safetyAlerts.remove(0);
        }
        
        log.warn("🛡️ Safety event: {} - {} - Conversation: {}", eventType, details, conversationId);
    }

    /**
     * Record token usage
     */
    public void recordTokenUsage(IntentType intent, int inputTokens, int outputTokens) {
        String intentKey = intent.name().toLowerCase();
        counterMetrics.computeIfAbsent("input_tokens_" + intentKey, k -> new AtomicInteger(0)).addAndGet(inputTokens);
        counterMetrics.computeIfAbsent("output_tokens_" + intentKey, k -> new AtomicInteger(0)).addAndGet(outputTokens);
        counterMetrics.computeIfAbsent("total_tokens", k -> new AtomicInteger(0)).addAndGet(inputTokens + outputTokens);
        
        log.debug("🪙 Token usage: {} - Input: {}, Output: {}", intent, inputTokens, outputTokens);
    }

    /**
     * Record handoff event
     */
    public void recordHandoff(IntentType intent, String reason, double confidence) {
        counterMetrics.computeIfAbsent("handoffs_total", k -> new AtomicInteger(0)).incrementAndGet();
        counterMetrics.computeIfAbsent("handoffs_" + intent.name().toLowerCase(), k -> new AtomicInteger(0)).incrementAndGet();
        counterMetrics.computeIfAbsent("handoffs_reason_" + reason.toLowerCase(), k -> new AtomicInteger(0)).incrementAndGet();
        
        log.info("👥 Handoff recorded: {} - {} - Confidence: {}", intent, reason, confidence);
    }

    /**
     * Get comprehensive metrics summary
     */
    public MetricsSummary getMetricsSummary() {
        MetricsSummary summary = new MetricsSummary();
        
        // Performance metrics
        summary.avgGenerationLatency = calculateAverageLatency("generation_latency");
        summary.avgToolLatency = calculateAverageLatency("tool_latency");
        summary.p95GenerationLatency = calculatePercentileLatency("generation_latency", 95);
        summary.p95ToolLatency = calculatePercentileLatency("tool_latency", 95);
        
        // Request counts
        summary.totalRequests = counterMetrics.values().stream()
            .filter(Objects::nonNull)
            .mapToInt(AtomicInteger::get)
            .sum();
        
        summary.parallelExecutions = counterMetrics.getOrDefault("parallel_executions", new AtomicInteger(0)).get();
        summary.retryRecoveries = counterMetrics.getOrDefault("retry_recoveries", new AtomicInteger(0)).get();
        
        // Cache metrics
        summary.cacheHitRate = calculateOverallCacheHitRate();
        summary.cacheMetrics = new HashMap<>(cacheMetrics);
        
        // Error rates
        summary.totalErrors = errorCounts.values().stream()
            .filter(Objects::nonNull)
            .mapToInt(AtomicInteger::get)
            .sum();
        
        summary.errorRate = summary.totalRequests > 0 ? 
            (double) summary.totalErrors / summary.totalRequests * 100 : 0.0;
        
        // Safety metrics
        summary.safetyEvents = safetyEvents.entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().get()
            ));
        
        // Recent events
        summary.recentErrors = new ArrayList<>(recentErrors);
        summary.recentSafetyAlerts = new ArrayList<>(safetyAlerts);
        
        return summary;
    }

    /**
     * Calculate average latency for a metric prefix
     */
    private double calculateAverageLatency(String metricPrefix) {
        return performanceMetrics.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith(metricPrefix))
            .mapToLong(entry -> entry.getValue().get())
            .average()
            .orElse(0.0);
    }

    /**
     * Calculate percentile latency
     */
    private double calculatePercentileLatency(String metricPrefix, int percentile) {
        List<Long> allLatencies = new ArrayList<>();
        
        latencyHistory.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith(metricPrefix))
            .forEach(entry -> allLatencies.addAll(entry.getValue()));
        
        if (allLatencies.isEmpty()) {
            return 0.0;
        }
        
        Collections.sort(allLatencies);
        int index = (int) Math.ceil(percentile / 100.0 * allLatencies.size()) - 1;
        return allLatencies.get(Math.max(0, index));
    }

    /**
     * Calculate overall cache hit rate
     */
    private double calculateOverallCacheHitRate() {
        int totalHits = cacheMetrics.values().stream()
            .mapToInt(metrics -> metrics.hits.get())
            .sum();
        
        int totalRequests = cacheMetrics.values().stream()
            .mapToInt(metrics -> metrics.hits.get() + metrics.misses.get())
            .sum();
        
        return totalRequests > 0 ? (double) totalHits / totalRequests * 100 : 0.0;
    }

    /**
     * Check if system health is degraded
     */
    public boolean isSystemHealthy() {
        MetricsSummary summary = getMetricsSummary();
        
        // Check error rate threshold (> 5%)
        if (summary.errorRate > 5.0) {
            log.warn("🚨 High error rate detected: {}%", summary.errorRate);
            return false;
        }
        
        // Check average latency threshold (> 10 seconds)
        if (summary.avgGenerationLatency > 10000) {
            log.warn("🚨 High generation latency detected: {}ms", summary.avgGenerationLatency);
            return false;
        }
        
        // Check recent safety events
        long recentSafetyCount = safetyAlerts.stream()
            .filter(alert -> alert.timestamp.isAfter(LocalDateTime.now().minusHours(1)))
            .count();
        
        if (recentSafetyCount > 10) {
            log.warn("🚨 High safety event rate detected: {} events in last hour", recentSafetyCount);
            return false;
        }
        
        return true;
    }

    /**
     * Reset all metrics (for testing/maintenance)
     */
    public void resetMetrics() {
        performanceMetrics.clear();
        counterMetrics.clear();
        latencyHistory.clear();
        errorCounts.clear();
        recentErrors.clear();
        safetyEvents.clear();
        safetyAlerts.clear();
        cacheMetrics.clear();
        
        log.info("🧹 All monitoring metrics reset");
    }

    // Data classes
    @Data
    public static class MetricsSummary {
        private double avgGenerationLatency;
        private double avgToolLatency;
        private double p95GenerationLatency;
        private double p95ToolLatency;
        private int totalRequests;
        private int parallelExecutions;
        private int retryRecoveries;
        private double cacheHitRate;
        private Map<String, CacheMetrics> cacheMetrics;
        private int totalErrors;
        private double errorRate;
        private Map<String, Integer> safetyEvents;
        private List<ErrorEvent> recentErrors;
        private List<SafetyAlert> recentSafetyAlerts;
    }

    @Data
    public static class CacheMetrics {
        private final AtomicInteger hits = new AtomicInteger(0);
        private final AtomicInteger misses = new AtomicInteger(0);
        
        public double getHitRate() {
            int total = hits.get() + misses.get();
            return total > 0 ? (double) hits.get() / total * 100 : 0.0;
        }
    }

    @Data
    public static class ErrorEvent {
        private LocalDateTime timestamp;
        private String operation;
        private String errorType;
        private String errorMessage;
        private String exceptionClass;
    }

    @Data
    public static class SafetyAlert {
        private LocalDateTime timestamp;
        private String eventType;
        private String details;
        private String conversationId;
    }
}
