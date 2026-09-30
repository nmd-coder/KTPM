package com.example.demo.service;

import com.example.demo.client.GeminiClient;
import com.example.demo.dto.AiAnalysisResult;
import com.example.demo.entity.Conversation;
import com.example.demo.entity.Message;
import com.example.demo.entity.RoutingDecision;
import com.example.demo.entity.enums.HandoffReason;
import com.example.demo.entity.enums.IntentType;
import com.example.demo.repository.ConversationRepository;
import com.example.demo.repository.MessageRepository;
import com.example.demo.repository.RoutingDecisionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for AI chat functionality
 * Handles message analysis, response generation, and tool execution
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatService {

    private final GeminiClient geminiClient;
    private final AiPromptService promptService;
    private final AiGenerationProfileService profileService;
    private final StoreConfigService storeConfigService;
    private final PiiDetectionService piiDetectionService;
    private final RoutingDecisionRepository routingDecisionRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final WebSocketService webSocketService;
    private final HandoffService handoffService;

    /**
     * Analyze customer message using AI
     */
    @Transactional
    public AiAnalysisResult analyzeMessage(Long conversationId, String customerMessage) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.debug("Analyzing message for conversation {}", conversationId);
            
            // Check for PII first
            if (piiDetectionService.containsPII(customerMessage)) {
                log.warn("⚠️ PII DETECTED in conversation {}: {}", conversationId, 
                    piiDetectionService.getPIITypes(customerMessage));
                
                // Return handoff result - HandoffService will send all notifications
                return createHandoffResult(IntentType.OTHER, 0.5, 
                    "Vui lòng chờ, mình đang kết nối tới nhân viên cửa hàng để hỗ trợ bạn nhé! 💬",
                    HandoffReason.PII_DETECTED);
            }

            // Get conversation history
            String conversationHistory = getConversationHistory(conversationId);
            
            // Generate prompts
            String systemPrompt = promptService.generateSystemPrompt();
            String userMessage = promptService.generateUserMessage(customerMessage, conversationHistory);
            
            // Use analysis profile for consistent, fast analysis
            AiGenerationProfileService.GenerationProfile analysisProfile = profileService.getAnalysisProfile();
            
            // Call Gemini API with profile
            AiAnalysisResult result = geminiClient.analyzeMessageWithProfile(systemPrompt, userMessage, analysisProfile);
            
            if (result == null) {
                log.error("Failed to get AI analysis, forcing handoff");
                AiAnalysisResult handoffResult = createHandoffResult(IntentType.OTHER, 0.0,
                    "Xin lỗi bạn, mình đang gặp chút vấn đề kỹ thuật. Để mình chuyển cho nhân viên hỗ trợ bạn nhé 💬",
                    HandoffReason.AI_ERROR);
                
                // CRITICAL FIX: Immediately notify staff about technical issues
                try {
                    handoffService.addToQueue(
                        conversationId,
                        HandoffReason.AI_ERROR,
                        customerMessage,
                        "🚨 Lỗi hệ thống AI - cần xử lý ngay",
                        8 // High priority for technical errors
                    );
                    log.info("Staff notified about AI technical error for conversation {}", conversationId);
                } catch (Exception e) {
                    log.error("CRITICAL: Failed to notify staff about AI error", e);
                    // Send emergency broadcast notification
                    try {
                        webSocketService.sendBroadcast(
                            "🚨 LỖI NGHIÊM TRỌNG: AI system error - conversation " + conversationId + " cần xử lý ngay!",
                            "critical_error",
                            conversationId
                        );
                    } catch (Exception fallbackError) {
                        log.error("EMERGENCY: All notification systems failed!", fallbackError);
                    }
                }
                
                return handoffResult;
            }

            // Calculate processing time
            long processingTime = System.currentTimeMillis() - startTime;
            
            // Save routing decision
            saveRoutingDecision(conversationId, result, processingTime);
            
            log.info("AI analysis completed - Intent: {}, Confidence: {}, Handoff: {}", 
                result.getIntent(), result.getConfidence(), result.getNeedHandoff());
            
            return result;
            
        } catch (Exception e) {
            log.error("Error analyzing message", e);
            return createHandoffResult(IntentType.OTHER, 0.0,
                "Xin lỗi bạn, hệ thống đang bận. Để mình chuyển cho nhân viên hỗ trợ bạn nhé 💬",
                HandoffReason.AI_ERROR);
        }
    }
    
    /**
     * Generate final AI response after tool execution
     * ENHANCED: Uses generation profiles and updated prompts with conditional CTA
     */
    public String generateFinalResponse(Long conversationId, String customerMessage, 
                                       String toolResults, AiAnalysisResult initialAnalysis) {
        return generateFinalResponse(conversationId, customerMessage, toolResults, initialAnalysis, null);
    }

    /**
     * Generate final AI response with optional streaming support
     * ENHANCED: Supports streaming for real-time user experience
     */
    public String generateFinalResponse(Long conversationId, String customerMessage, 
                                       String toolResults, AiAnalysisResult initialAnalysis,
                                       GeminiClient.StreamingCallback streamCallback) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.debug("Generating final AI response with tool results for conversation {}", conversationId);
            
            // Get intent type for profile selection
            IntentType intent = initialAnalysis.getIntentType();
            
            // Get appropriate generation profile for the intent
            AiGenerationProfileService.GenerationProfile profile = profileService.getProfileForIntent(intent);
            
            // Get conversation history
            String conversationHistory = getConversationHistory(conversationId);
            
            // Use the new prompt with intent-aware rules
            String finalPrompt = promptService.generateFinalResponsePrompt(
                customerMessage, 
                toolResults, 
                conversationHistory, 
                initialAnalysis.getReply(),
                intent.name()
            );
            
            // VERY STRICT system prompt to ensure plain text output with images
            String systemPrompt = "You are a Vietnamese flower consultant. " +
                "CRITICAL: Output ONLY conversational text in Vietnamese. " +
                "DO NOT use JSON format like {\"content\":\"...\"}. " +
                "DO NOT use any brackets, braces, or structured data. " +
                "Write naturally like you're chatting with a customer. " +
                "\n\nIMPORTANT PRODUCT DISPLAY RULES:" +
                "\n- If customer requests X products, you MUST show ALL X products (never show less)" +
                "\n- Display ALL products from tool results, don't skip any" +
                "\n- Each product MUST have: name, price, description, and image" +
                "\n\nIMAGE MARKDOWN RULES:" +
                "\n- When tool results contain markdown images like ![name](url)<!--product:123,456789-->, you MUST preserve them EXACTLY" +
                "\n- DO NOT remove the <!--product:...--> part after images - this is required metadata" +
                "\n- Copy the entire image markdown including any HTML comments exactly as provided" +
                "\n- Format: **Product Name**\\nDescription\\n![Product Name](image_url)<!--product:id,price-->" +
                "\n- Always include product images with metadata when recommending products";
            
            // Generate with streaming support if callback provided
            com.example.demo.dto.gemini.GeminiResponse response;
            if (streamCallback != null) {
                log.debug("🌊 Using STREAMING generation for conversation {}", conversationId);
                response = geminiClient.generateContentWithStreaming(
                    systemPrompt, 
                    finalPrompt,
                    profile,
                    streamCallback
                );
            } else {
                log.debug("📝 Using NON-STREAMING generation for conversation {}", conversationId);
                // Use retry/backoff to improve resilience on transient errors
                response = geminiClient.generateFinalResponseWithProfileAndRetry(
                    systemPrompt,
                    finalPrompt,
                    profile,
                    3
                );
            }
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            if (response != null && response.isSuccessful()) {
                String finalResponse = response.getTextResponse();
                log.info("✅ Generated final response: {} chars in {}ms", finalResponse.length(), processingTime);
                
                // Clean up any accidental JSON or metadata
                finalResponse = cleanResponseText(finalResponse);
                
                // QUALITY VALIDATION: Check if response is complete and adequate
                if (!isResponseComplete(finalResponse, customerMessage, toolResults)) {
                    log.warn("⚠️ Response quality check failed, using enhanced fallback");
                    finalResponse = createEnhancedFallbackResponse(toolResults, initialAnalysis.getReply(), customerMessage);
                    
                    // Stream fallback response if callback provided to avoid SSE timeout
                    if (streamCallback != null) {
                        log.info("Streaming fallback response to prevent SSE timeout");
                        streamCallback.onChunk(finalResponse);
                        streamCallback.onComplete();
                    }
                }
                
                // Log profile usage for monitoring
                profileService.logProfileUsage(intent, profile, processingTime);
                
                return finalResponse.trim();
            } else {
                log.warn("Failed to generate final response, using fallback");
                return createEnhancedFallbackResponse(toolResults, initialAnalysis.getReply(), customerMessage);
            }
            
        } catch (Exception e) {
            log.error("Error generating final response", e);
            return createFallbackResponse(toolResults, initialAnalysis.getReply());
        }
    }
    
    /**
     * Clean up response text - remove any JSON, metadata, or instructions
     * OPTIMIZED: Faster, simpler cleanup + validation
     */
    private String cleanResponseText(String response) {
        if (response == null || response.isEmpty()) {
            return response;
        }
        
        String cleaned = response.trim();
        
        // Remove common metadata prefixes
        cleaned = cleaned.replaceAll("(?i)^TOOL_RESULT:\\s*", "");
        cleaned = cleaned.replaceAll("(?i)^KẾT QUẢ:\\s*", "");
        cleaned = cleaned.replaceAll("(?i)HƯỚNG DẪN:.*$", "");
        
        // If response starts with JSON, try to extract text
        if (cleaned.startsWith("{") || cleaned.startsWith("[")) {
            log.warn("⚠️ AI returned JSON, extracting text...");
            cleaned = extractTextFromJson(cleaned);
        }
        
        // Validate: If response has product names but no images, log warning
        if (cleaned.contains("**") && !cleaned.contains("![")) {
            log.warn("⚠️ AI response contains product names but NO IMAGES!");
        }
        
        return cleaned.trim();
    }
    
    /**
     * Extract human-readable text from JSON response
     * ENHANCED: Better extraction with logging
     */
    private String extractTextFromJson(String json) {
        try {
            log.info("🔍 Attempting to extract text from JSON: {}", json.substring(0, Math.min(200, json.length())));
            
            // Try common patterns first
            String content = extractJsonField(json, "content");
            if (content != null && content.length() > 20) {
                log.info("✅ Extracted from 'content' field: {} chars", content.length());
                return content;
            }
            
            String reply = extractJsonField(json, "reply");
            if (reply != null && reply.length() > 20) {
                log.info("✅ Extracted from 'reply' field: {} chars", reply.length());
                return reply;
            }
            
            // Try to extract any long text value from JSON
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"([^\"]{30,})\"");
            java.util.regex.Matcher matcher = pattern.matcher(json);
            
            while (matcher.find()) {
                String text = matcher.group(1);
                // Skip technical fields
                if (!text.contains("product_search") && !text.contains("tool_requests") && 
                    !text.contains("http://") && text.length() > 30) {
                    log.info("✅ Extracted long text value: {} chars", text.length());
                    return text.replace("\\n", "\n");
                }
            }
            
            // If still JSON, show error message
            log.error("❌ Could not extract clean text from JSON");
            return "Xin lỗi bạn, mình gặp lỗi khi tạo câu trả lời. Bạn có thể mô tả lại yêu cầu không ạ? 🙏";
            
        } catch (Exception e) {
            log.error("Failed to extract from JSON", e);
            return "Xin lỗi bạn, mình gặp lỗi khi tạo câu trả lời. Bạn có thể mô tả lại yêu cầu không ạ? 🙏";
        }
    }
    
    /**
     * Check if AI response is complete and adequate
     * ENHANCED: Quality validation to prevent incomplete responses
     */
    private boolean isResponseComplete(String response, String customerMessage, String toolResults) {
        if (response == null || response.trim().isEmpty()) {
            log.warn("❌ Response is null or empty");
            return false;
        }
        
        String trimmedResponse = response.trim();
        
        // Check minimum length (loosen for STORE_INFO/policy-like requests)
        boolean looksLikePolicyOrStoreInfo =
            customerMessage != null && customerMessage.toLowerCase().matches(".*(chính sách|policy|cửa hàng|địa chỉ|giờ mở|hotline).*");
        int minLen = looksLikePolicyOrStoreInfo ? 20 : 30;
        if (trimmedResponse.length() < minLen) {
            log.warn("❌ Response too short for context (min {}): {} chars", minLen, trimmedResponse.length());
            return false;
        }
        
        // Check if response ends abruptly (common signs of incomplete generation)
        if (trimmedResponse.endsWith("...") || 
            trimmedResponse.endsWith(",") ||
            trimmedResponse.endsWith("và") ||
            trimmedResponse.endsWith("hoặc") ||
            trimmedResponse.endsWith("nhưng")) {
            log.warn("❌ Response appears to end abruptly");
            return false;
        }
        
        // Check if response contains product recommendations when tool results have products
        if (toolResults != null && toolResults.contains("sản phẩm") && 
            !trimmedResponse.toLowerCase().contains("sản phẩm") &&
            !trimmedResponse.contains("**")) {
            log.warn("❌ Tool results contain products but response doesn't mention them");
            return false;
        }
        
        // Check for generic error responses that indicate AI confusion
        String lowerResponse = trimmedResponse.toLowerCase();
        if (lowerResponse.contains("không hiểu") || 
            lowerResponse.contains("không thể trả lời") ||
            lowerResponse.contains("xin lỗi, mình không") ||
            lowerResponse.startsWith("tôi")) { // AI shouldn't use "tôi"
            log.warn("❌ Response indicates AI confusion or inappropriate language");
            return false;
        }
        
        log.info("✅ Response passed quality validation");
        return true;
    }
    
    /**
     * Create enhanced fallback response with better customer experience
     * ENHANCED: More intelligent fallback based on context
     */
    private String createEnhancedFallbackResponse(String toolResults, String initialReply, String customerMessage) {
        StringBuilder response = new StringBuilder();
        
        // Start with acknowledgment
        response.append(initialReply != null && !initialReply.trim().isEmpty() ? 
            initialReply : "Mình hiểu bạn đang tìm kiếm thông tin về hoa.");
        
        if (!response.toString().endsWith(".") && !response.toString().endsWith("!")) {
            response.append(".");
        }
        response.append("\n\n");
        
        // Add tool results if available
        if (toolResults != null && !toolResults.trim().isEmpty()) {
            String cleanToolResults = toolResults
                .replaceAll("(?i)TOOL_RESULT:\\s*Tìm thấy \\d+ sản phẩm:\\s*", "")
                .replaceAll("(?i)HƯỚNG DẪN:.*", "")
                .trim();
            
            if (!cleanToolResults.isEmpty()) {
                // Check if this is store info or product info
                String lowerToolResults = cleanToolResults.toLowerCase();
                if (lowerToolResults.contains("thông tin cửa hàng") || 
                    lowerToolResults.contains("địa chỉ") || 
                    lowerToolResults.contains("hotline") ||
                    lowerToolResults.contains("giờ mở cửa") ||
                    lowerToolResults.contains("chính sách")) {
                    // This is store information, don't add product prefix
                    response.append(cleanToolResults).append("\n\n");
                } else if (lowerToolResults.contains("sản phẩm") || 
                          lowerToolResults.contains("hoa") ||
                          lowerToolResults.contains("**")) {
                    // This is product information
                    response.append("Mình tìm thấy một số sản phẩm phù hợp cho bạn:\n\n");
                    response.append(cleanToolResults).append("\n\n");
                } else {
                    // Generic tool results
                    response.append(cleanToolResults).append("\n\n");
                }
            }
        }
        
        // Add helpful next steps based on customer message content
        String lowerMessage = customerMessage.toLowerCase();
        
        // Check if this is a store info request
        boolean isStoreInfoRequest = lowerMessage.contains("thông tin") || 
                                   lowerMessage.contains("địa chỉ") || 
                                   lowerMessage.contains("cửa hàng") ||
                                   lowerMessage.contains("shop") ||
                                   lowerMessage.contains("hotline") ||
                                   lowerMessage.contains("giờ mở") ||
                                   lowerMessage.contains("chính sách");
        
        if (isStoreInfoRequest) {
            // For store info requests, don't add unnecessary consultation suggestions
            response.append("🌸 Nếu bạn cần thêm thông tin gì khác, đừng ngại hỏi mình nhé!");
        } else if (lowerMessage.contains("sinh nhật") || lowerMessage.contains("birthday")) {
            response.append("💝 Để tư vấn chính xác hơn về hoa sinh nhật, ");
            response.append("bạn có thể mô tả rõ hơn nhu cầu hoặc để mình chuyển cho nhân viên hỗ trợ bạn nhé!");
        } else if (lowerMessage.contains("cưới") || lowerMessage.contains("wedding")) {
            response.append("💒 Để tư vấn về hoa cưới phù hợp, ");
            response.append("bạn có thể mô tả rõ hơn nhu cầu hoặc để mình chuyển cho nhân viên hỗ trợ bạn nhé!");
        } else if (lowerMessage.contains("tang") || lowerMessage.contains("chia buồn")) {
            response.append("🕯️ Để tư vấn về hoa tang lễ trang trọng, ");
            response.append("bạn có thể mô tả rõ hơn nhu cầu hoặc để mình chuyển cho nhân viên hỗ trợ bạn nhé!");
        } else if (lowerMessage.contains("giá") || lowerMessage.contains("bao nhiêu")) {
            response.append("💰 Để biết thông tin giá cả chính xác, ");
            response.append("bạn có thể mô tả rõ hơn nhu cầu hoặc để mình chuyển cho nhân viên hỗ trợ bạn nhé!");
        } else {
            response.append("🌸 Để được tư vấn chi tiết hơn, ");
            response.append("bạn có thể mô tả rõ hơn nhu cầu hoặc để mình chuyển cho nhân viên hỗ trợ bạn nhé!");
        }
        
        return response.toString();
    }
    
    /**
     * Create fallback response when AI generation fails
     */
    private String createFallbackResponse(String toolResults, String initialReply) {
        String cleanToolResults = toolResults
            .replaceAll("(?i)TOOL_RESULT:\\s*Tìm thấy \\d+ sản phẩm:\\s*", "")
            .replaceAll("(?i)HƯỚNG DẪN:.*", "")
            .trim();
        
        // Check if this is store info or product info
        String lowerToolResults = cleanToolResults.toLowerCase();
        if (lowerToolResults.contains("thông tin cửa hàng") || 
            lowerToolResults.contains("địa chỉ") || 
            lowerToolResults.contains("hotline") ||
            lowerToolResults.contains("giờ mở cửa") ||
            lowerToolResults.contains("chính sách")) {
            // This is store information
            return initialReply + "\n\n" + cleanToolResults;
        } else {
            // This is product information
            return initialReply + "\n\n" + 
                "Mình tìm thấy một số sản phẩm cho bạn nè:\n\n" + cleanToolResults;
        }
    }
    
    /**
     * Extract a field value from JSON string
     */
    private String extractJsonField(String json, String fieldName) {
        try {
            String pattern = "\"" + fieldName + "\"\\s*:\\s*\"";
            int start = json.indexOf(pattern);
            if (start < 0) {
                // Try without quotes (for nested objects)
                pattern = "\"" + fieldName + "\"\\s*:\\s*\\{";
                start = json.indexOf(pattern);
                if (start < 0) return null;
                
                start += pattern.length() - 1;
                int depth = 1;
                StringBuilder nested = new StringBuilder("{");
                for (int i = start + 1; i < json.length() && depth > 0; i++) {
                    char c = json.charAt(i);
                    nested.append(c);
                    if (c == '{') depth++;
                    else if (c == '}') depth--;
                }
                return nested.toString();
            }
            
            start += pattern.length();
            int end = start;
            boolean escaped = false;
            
            while (end < json.length()) {
                char c = json.charAt(end);
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    break;
                }
                end++;
            }
            
            if (end >= json.length()) return null;
            
            String value = json.substring(start, end);
            return value.replace("\\n", "\n")
                       .replace("\\\"", "\"")
                       .replace("\\\\", "\\")
                       .replace("\\/", "/");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get conversation history for context
     */
    private String getConversationHistory(Long conversationId) {
        try {
            List<Message> messages = messageRepository.findByConversationIdOrderBySentAtAsc(conversationId);
            
            if (messages.isEmpty()) {
                return "";
            }

            // Get last N messages
            int maxMessages = storeConfigService.getConfigAsInt("ai.max_conversation_history", 10);
            int start = Math.max(0, messages.size() - maxMessages);
            List<Message> recentMessages = messages.subList(start, messages.size());
            
            List<String> history = new ArrayList<>();
            for (Message msg : recentMessages) {
                String sender = msg.getSender().getFullName();
                String content = msg.getContent();
                history.add(sender + ": " + content);
            }
            
            return String.join("\n", history);
            
        } catch (Exception e) {
            log.error("Error getting conversation history", e);
            return "";
        }
    }

    /**
     * Save routing decision to database
     */
    @Transactional
    public void saveRoutingDecision(Long conversationId, AiAnalysisResult result, long processingTimeMs) {
        try {
            Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

            RoutingDecision decision = new RoutingDecision();
            decision.setConversation(conversation);
            decision.setIntent(result.getIntentType());
            decision.setConfidence(result.getConfidenceAsBigDecimal());
            decision.setNeedHandoff(result.requiresHandoff());
            decision.setSuggestHandoff(result.suggestsHandoff());
            decision.setAiResponse(result.getReply());
            decision.setProcessingTimeMs((int) processingTimeMs);
            
            // Set handoff reason if needed
            if (result.requiresHandoff()) {
                decision.setHandoffReason(determineHandoffReason(result));
            }
            
            // Set tools used
            if (result.hasToolRequests()) {
                List<String> toolNames = new ArrayList<>();
                for (AiAnalysisResult.ToolRequest tool : result.getToolRequests()) {
                    toolNames.add(tool.getName());
                }
                decision.setToolsUsed(toolNames.toArray(new String[0]));
            }
            
            routingDecisionRepository.save(decision);
            log.debug("Routing decision saved for conversation {}", conversationId);
            
        } catch (Exception e) {
            log.error("Error saving routing decision", e);
        }
    }

    /**
     * Determine handoff reason from AI result
     */
    private HandoffReason determineHandoffReason(AiAnalysisResult result) {
        IntentType intent = result.getIntentType();
        
        if (result.getConfidence() != null && result.getConfidence() < 0.65) {
            return HandoffReason.LOW_CONFIDENCE;
        }
        
        switch (intent) {
            case ORDER_SUPPORT:
                return HandoffReason.ORDER_INQUIRY;
            case PAYMENT:
                return HandoffReason.PAYMENT_ISSUE;
            default:
                return HandoffReason.COMPLEX_QUERY;
        }
    }

    /**
     * Create handoff result when AI cannot handle
     */
    private AiAnalysisResult createHandoffResult(IntentType intent, double confidence, 
                                                  String reply, HandoffReason reason) {
        AiAnalysisResult result = new AiAnalysisResult();
        result.setIntent(intent.name());
        result.setConfidence(confidence);
        result.setReply(reply);
        result.setNeedHandoff(true);
        result.setSuggestHandoff(false);
        result.setToolRequests(new ArrayList<>());
        result.setProductSuggestions(new ArrayList<>());
        return result;
    }

    /**
     * Check if AI should handle the message based on confidence
     */
    public boolean shouldAiHandle(AiAnalysisResult result) {
        if (result == null) {
            return false;
        }

        // Check if handoff is required
        if (result.requiresHandoff()) {
            return false;
        }

        // Check confidence threshold
        Map<String, Double> thresholds = storeConfigService.getConfidenceThresholds();
        double autoThreshold = thresholds.getOrDefault("auto", 0.80);
        
        return result.isConfidentEnough(autoThreshold);
    }

    /**
     * Check if AI suggests handoff
     */
    public boolean shouldSuggestHandoff(AiAnalysisResult result) {
        if (result == null || result.requiresHandoff()) {
            return false;
        }

        Map<String, Double> thresholds = storeConfigService.getConfidenceThresholds();
        double suggestThreshold = thresholds.getOrDefault("suggest", 0.65);
        double autoThreshold = thresholds.getOrDefault("auto", 0.80);
        
        return result.getConfidence() >= suggestThreshold && 
               result.getConfidence() < autoThreshold;
    }

    /**
     * Format AI reply with handoff suggestion if needed
     */
    public String formatReplyWithHandoffSuggestion(AiAnalysisResult result) {
        String reply = result.getReply();
        
        if (shouldSuggestHandoff(result)) {
            reply += "\n\n_Bạn có muốn mình chuyển cho nhân viên để được hỗ trợ tốt hơn không?_ 💬";
        }
        
        return reply;
    }
}
