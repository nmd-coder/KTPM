package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for Server-Sent Events (SSE) to provide real-time updates
 */
@Service
public class SseService {
    
    private static final Logger logger = LoggerFactory.getLogger(SseService.class);
    
    // Store SSE emitters by orderId
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    
    /**
     * Create SSE connection for an order
     */
    public SseEmitter subscribe(String orderId) {
        logger.info("Creating SSE subscription for order: {}", orderId);
        
        // Create emitter with 5 minute timeout
        SseEmitter emitter = new SseEmitter(300_000L);
        
        // Store emitter
        emitters.put(orderId, emitter);
        
        // Handle completion and timeout
        emitter.onCompletion(() -> {
            logger.info("SSE completed for order: {}", orderId);
            emitters.remove(orderId);
        });
        
        emitter.onTimeout(() -> {
            logger.info("SSE timeout for order: {}", orderId);
            emitters.remove(orderId);
        });
        
        emitter.onError((ex) -> {
            logger.error("SSE error for order {}: {}", orderId, ex.getMessage());
            emitters.remove(orderId);
        });
        
        // Send initial connection message
        try {
            emitter.send(SseEmitter.event()
                .name("connected")
                .data("{\"message\":\"Connected to order updates\",\"orderId\":\"" + orderId + "\"}"));
        } catch (IOException e) {
            logger.error("Failed to send initial SSE message for order {}: {}", orderId, e.getMessage());
            emitters.remove(orderId);
        }
        
        return emitter;
    }
    
    /**
     * Push payment status update to subscribers
     */
    public void pushPaymentUpdate(String orderId, String status, String message, String transactionId) {
        logger.info("Pushing payment update for order {}: status={}, message={}", orderId, status, message);
        
        SseEmitter emitter = emitters.get(orderId);
        if (emitter == null) {
            logger.warn("No SSE subscriber found for order: {}", orderId);
            return;
        }
        
        try {
            String eventData = String.format(
                "{\"orderId\":\"%s\",\"status\":\"%s\",\"message\":\"%s\",\"transactionId\":\"%s\",\"timestamp\":%d}",
                orderId, status, message, transactionId != null ? transactionId : "", System.currentTimeMillis()
            );
            
            emitter.send(SseEmitter.event()
                .name("payment")
                .data(eventData));
                
            logger.info("Successfully sent SSE payment update for order: {}", orderId);
            
        } catch (IOException e) {
            logger.error("Failed to send SSE payment update for order {}: {}", orderId, e.getMessage());
            emitters.remove(orderId);
        }
    }
    
    /**
     * Get active subscriber count
     */
    public int getActiveSubscribers() {
        return emitters.size();
    }
    
    /**
     * Check if order has active subscriber
     */
    public boolean hasSubscriber(String orderId) {
        return emitters.containsKey(orderId);
    }
}
