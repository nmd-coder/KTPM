package com.example.demo.controller;

import com.example.demo.service.SseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Controller for Server-Sent Events (SSE) real-time updates
 */
@RestController
@RequestMapping("/sse")
@Tag(name = "📡 SSE", description = "Server-Sent Events for real-time updates")
public class SseController {
    
    private static final Logger logger = LoggerFactory.getLogger(SseController.class);
    
    @Autowired
    private SseService sseService;
    
    /**
     * Subscribe to order updates via SSE
     */
    @Operation(
        summary = "Subscribe to order updates",
        description = "Create SSE connection to receive real-time payment status updates for an order"
    )
    @GetMapping(value = "/orders/{orderId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToOrderUpdates(@PathVariable String orderId) {
        logger.info("SSE subscription request for order: {}", orderId);
        return sseService.subscribe(orderId);
    }
    
    /**
     * Get SSE service status
     */
    @Operation(
        summary = "Get SSE service status",
        description = "Get information about active SSE connections"
    )
    @GetMapping("/status")
    public java.util.Map<String, Object> getSseStatus() {
        return java.util.Map.of(
            "activeSubscribers", sseService.getActiveSubscribers(),
            "status", "running",
            "timestamp", System.currentTimeMillis()
        );
    }
}
