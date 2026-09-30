package com.example.demo.service;

import com.example.demo.dto.AiAnalysisResult;
import com.example.demo.dto.ChatMessageDTO;
import com.example.demo.entity.enums.HandoffReason;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for routing messages between AI and staff
 * Determines whether AI can handle message or needs staff intervention
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoutingService {

    private final AiChatService aiChatService;
    private final PiiDetectionService piiDetectionService;
    private final StoreConfigService storeConfigService;

    /**
     * Route message - decide if AI handles or handoff to staff
     */
    public RoutingDecision routeMessage(Long conversationId, String messageContent) {
        log.info("Routing message for conversation {}", conversationId);
        
        // Quick PII check first
        if (piiDetectionService.containsPII(messageContent)) {
            log.info("PII detected, routing to staff");
            return RoutingDecision.handoffToStaff(HandoffReason.PII_DETECTED, 
                "Vui lòng chờ, mình đang kết nối tới nhân viên cửa hàng để hỗ trợ bạn nhé! 💬");
        }

        // Check if AI is enabled
        Boolean aiEnabled = storeConfigService.getConfigAsBoolean("ai.enable_auto_handoff", true);
        if (!aiEnabled) {
            log.info("AI disabled, routing to staff");
            return RoutingDecision.handoffToStaff(HandoffReason.EXPLICIT_REQUEST,
                "AI tạm thời không khả dụng");
        }

        // Analyze with AI
        AiAnalysisResult analysis = aiChatService.analyzeMessage(conversationId, messageContent);
        
        if (analysis == null) {
            log.error("AI analysis failed, routing to staff");
            return RoutingDecision.handoffToStaff(HandoffReason.AI_ERROR,
                "Lỗi phân tích AI");
        }

        // Check if AI requires handoff
        if (analysis.requiresHandoff()) {
            log.info("AI requires handoff - Intent: {}, Confidence: {}", 
                analysis.getIntent(), analysis.getConfidence());
            
            HandoffReason reason = determineHandoffReason(analysis);
            return RoutingDecision.handoffToStaff(reason, analysis.getReply());
        }

        // Check if AI can handle with confidence
        if (aiChatService.shouldAiHandle(analysis)) {
            log.info("AI will handle - Intent: {}, Confidence: {}", 
                analysis.getIntent(), analysis.getConfidence());
            return RoutingDecision.handleByAi(analysis);
        }

        // Medium confidence - suggest handoff but let AI try
        if (aiChatService.shouldSuggestHandoff(analysis)) {
            log.info("AI will handle with handoff suggestion - Confidence: {}", 
                analysis.getConfidence());
            return RoutingDecision.handleByAiWithSuggestion(analysis);
        }

        // Low confidence - handoff to staff
        log.info("Low confidence, routing to staff - Confidence: {}", 
            analysis.getConfidence());
        return RoutingDecision.handoffToStaff(HandoffReason.LOW_CONFIDENCE,
            analysis.getReply());
    }

    /**
     * Determine handoff reason from AI analysis
     */
    private HandoffReason determineHandoffReason(AiAnalysisResult analysis) {
        String intent = analysis.getIntent();
        
        if (intent == null) {
            return HandoffReason.COMPLEX_QUERY;
        }

        switch (intent.toUpperCase()) {
            case "ORDER_SUPPORT":
                return HandoffReason.ORDER_INQUIRY;
            case "PAYMENT":
                return HandoffReason.PAYMENT_ISSUE;
            default:
                if (analysis.getConfidence() != null && analysis.getConfidence() < 0.65) {
                    return HandoffReason.LOW_CONFIDENCE;
                }
                return HandoffReason.COMPLEX_QUERY;
        }
    }

    /**
     * Inner class representing routing decision
     */
    public static class RoutingDecision {
        private final boolean handleByAi;
        private final boolean suggestHandoff;
        private final HandoffReason handoffReason;
        private final String context;
        private final AiAnalysisResult aiAnalysis;

        private RoutingDecision(boolean handleByAi, boolean suggestHandoff, 
                               HandoffReason handoffReason, String context, 
                               AiAnalysisResult aiAnalysis) {
            this.handleByAi = handleByAi;
            this.suggestHandoff = suggestHandoff;
            this.handoffReason = handoffReason;
            this.context = context;
            this.aiAnalysis = aiAnalysis;
        }

        public static RoutingDecision handleByAi(AiAnalysisResult analysis) {
            return new RoutingDecision(true, false, null, null, analysis);
        }

        public static RoutingDecision handleByAiWithSuggestion(AiAnalysisResult analysis) {
            return new RoutingDecision(true, true, null, null, analysis);
        }

        public static RoutingDecision handoffToStaff(HandoffReason reason, String context) {
            return new RoutingDecision(false, false, reason, context, null);
        }

        // Getters
        public boolean isHandleByAi() {
            return handleByAi;
        }

        public boolean isSuggestHandoff() {
            return suggestHandoff;
        }

        public boolean isHandoffToStaff() {
            return !handleByAi;
        }

        public HandoffReason getHandoffReason() {
            return handoffReason;
        }

        public String getContext() {
            return context;
        }

        public AiAnalysisResult getAiAnalysis() {
            return aiAnalysis;
        }
    }
}

