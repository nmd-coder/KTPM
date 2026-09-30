package com.example.demo.client;

import com.example.demo.config.GeminiProperties;
import com.example.demo.dto.AiAnalysisResult;
import com.example.demo.dto.gemini.GeminiRequest;
import com.example.demo.dto.gemini.GeminiResponse;
import com.example.demo.service.AiGenerationProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Client for interacting with Google Gemini API
 * Handles API calls, retries, and error handling
 */
@Component
@Slf4j
public class GeminiClient {

    private final GeminiProperties geminiProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeminiClient(GeminiProperties geminiProperties, ObjectMapper objectMapper) {
        this.geminiProperties = geminiProperties;
        this.objectMapper = objectMapper;
        
        // Create RestTemplate with timeout
        this.restTemplate = new RestTemplate();
        this.restTemplate.setRequestFactory(
            new org.springframework.http.client.SimpleClientHttpRequestFactory() {{
                setConnectTimeout(Duration.ofSeconds(geminiProperties.getTimeoutSeconds()));
                setReadTimeout(Duration.ofSeconds(geminiProperties.getTimeoutSeconds()));
            }}
        );
    }

    /**
     * Generate content using Gemini API
     * 
     * @param prompt The prompt to send to Gemini
     * @return GeminiResponse containing the generated content
     */
    public GeminiResponse generateContent(String prompt) {
        if (!geminiProperties.isConfigured()) {
            log.error("Gemini API is not properly configured");
            return null;
        }

        try {
            String url = geminiProperties.getGenerateContentUrl();
            
            GeminiRequest request = GeminiRequest.create(
                prompt,
                geminiProperties.getTemperature(),
                geminiProperties.getMaxTokens()
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<GeminiRequest> entity = new HttpEntity<>(request, headers);

            log.debug("Calling Gemini API: {}", url);
            log.debug("Request: {}", objectMapper.writeValueAsString(request));

            ResponseEntity<GeminiResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                GeminiResponse.class
            );

            GeminiResponse responseBody = response.getBody();
            
            if (responseBody != null && responseBody.isSuccessful()) {
                String textResponse = responseBody.getTextResponse();
                if (textResponse != null && !textResponse.trim().isEmpty()) {
                    log.debug("Gemini API response: {}", textResponse);
                    return responseBody;
                } else {
                    log.warn("Gemini API returned empty text response");
                    return null;
                }
            } else {
                log.warn("Gemini API returned empty or unsuccessful response");
                // Debug logging
                if (responseBody != null) {
                    log.warn("Response body candidates: {}", responseBody.getCandidates());
                    log.warn("Response body usage: {}", responseBody.getUsageMetadata());
                } else {
                    log.warn("Response body is null");
                }
                return null;
            }

        } catch (RestClientException e) {
            log.error("Error calling Gemini API", e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error with Gemini API", e);
            return null;
        }
    }

    /**
     * Generate content with system prompt
     * 
     * @param systemPrompt System instructions for the AI
     * @param userMessage User's message
     * @return GeminiResponse containing the generated content
     */
    public GeminiResponse generateContentWithSystemPrompt(String systemPrompt, String userMessage) {
        if (!geminiProperties.isConfigured()) {
            log.error("Gemini API is not properly configured");
            return null;
        }

        try {
            String url = geminiProperties.getGenerateContentUrl();
            
            GeminiRequest request = GeminiRequest.createWithSystemPrompt(
                systemPrompt,
                userMessage,
                geminiProperties.getTemperature(),
                geminiProperties.getMaxTokens()
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<GeminiRequest> entity = new HttpEntity<>(request, headers);

            log.debug("Calling Gemini API with system prompt");

            ResponseEntity<GeminiResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                GeminiResponse.class
            );

            GeminiResponse responseBody = response.getBody();
            
            if (responseBody != null && responseBody.isSuccessful()) {
                String textResponse = responseBody.getTextResponse();
                if (textResponse != null && !textResponse.trim().isEmpty()) {
                    log.debug("Gemini API response received successfully");
                    return responseBody;
                } else {
                    log.warn("Gemini API returned empty text response");
                    return null;
                }
            } else {
                log.warn("Gemini API returned empty or unsuccessful response");
                return null;
            }

        } catch (RestClientException e) {
            log.error("Error calling Gemini API", e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error with Gemini API", e);
            return null;
        }
    }

    /**
     * Analyze message and get structured AI response
     * 
     * @param systemPrompt System instructions
     * @param userMessage User's message
     * @return AiAnalysisResult parsed from JSON response
     */
    public AiAnalysisResult analyzeMessage(String systemPrompt, String userMessage) {
        GeminiResponse response = generateContentWithSystemPrompt(systemPrompt, userMessage);
        
        if (response == null || !response.isSuccessful()) {
            log.error("Failed to get response from Gemini");
            return null;
        }

        try {
            String jsonResponse = response.getTextResponse();
            log.debug("Parsing AI analysis from JSON: {}", jsonResponse);
            
            // Parse JSON response to AiAnalysisResult
            AiAnalysisResult result = objectMapper.readValue(jsonResponse, AiAnalysisResult.class);
            
            log.info("AI Analysis - Intent: {}, Confidence: {}, Handoff: {}", 
                result.getIntent(), result.getConfidence(), result.getNeedHandoff());
            
            return result;
            
        } catch (Exception e) {
            log.error("Error parsing AI analysis result", e);
            return null;
        }
    }

    /**
     * Generate content with retry logic
     * 
     * @param prompt The prompt
     * @param maxRetries Maximum number of retries
     * @return GeminiResponse or null if all retries fail
     */
    public GeminiResponse generateContentWithRetry(String prompt, int maxRetries) {
        int attempts = 0;
        GeminiResponse response = null;

        while (attempts < maxRetries && response == null) {
            attempts++;
            log.debug("Gemini API call attempt {} of {}", attempts, maxRetries);
            
            response = generateContent(prompt);
            
            if (response == null && attempts < maxRetries) {
                try {
                    // Wait before retry (exponential backoff)
                    Thread.sleep(1000L * attempts);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        if (response == null) {
            log.error("Failed to get response from Gemini after {} attempts", maxRetries);
        }

        return response;
    }

    /**
     * Generate content with generation profile
     * 
     * @param systemPrompt System instructions
     * @param userMessage User's message
     * @param profile Generation profile with parameters
     * @return GeminiResponse containing the generated content
     */
    public GeminiResponse generateContentWithProfile(String systemPrompt, String userMessage, AiGenerationProfileService.GenerationProfile profile) {
        if (!geminiProperties.isConfigured()) {
            log.error("Gemini API is not properly configured");
            return null;
        }

        try {
            String url = geminiProperties.getGenerateContentUrl();
            
            GeminiRequest request = GeminiRequest.createWithSystemPrompt(
                systemPrompt,
                userMessage,
                profile.getTemperature(),
                profile.getTopP(),
                profile.getMaxTokens()
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create RestTemplate with profile-specific timeout
            RestTemplate profileRestTemplate = createRestTemplateWithTimeout(profile.getTimeoutSeconds());

            HttpEntity<GeminiRequest> entity = new HttpEntity<>(request, headers);

            log.debug("Calling Gemini API with profile - Temp: {}, TopP: {}, MaxTokens: {}, Timeout: {}s", 
                profile.getTemperature(), profile.getTopP(), profile.getMaxTokens(), profile.getTimeoutSeconds());

            ResponseEntity<GeminiResponse> response = profileRestTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                GeminiResponse.class
            );

            GeminiResponse responseBody = response.getBody();
            
            if (responseBody != null && responseBody.isSuccessful()) {
                String textResponse = responseBody.getTextResponse();
                if (textResponse != null && !textResponse.trim().isEmpty()) {
                    log.debug("Gemini API response received successfully with profile");
                    return responseBody;
                } else {
                    log.warn("Gemini API returned empty text response with profile");
                    return null;
                }
            } else {
                log.warn("Gemini API returned empty or unsuccessful response");
                return null;
            }

        } catch (RestClientException e) {
            log.error("Error calling Gemini API with profile", e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error with Gemini API", e);
            return null;
        }
    }

    /**
     * Generate content with generation profile and retry logic
     * 
     * @param systemPrompt System instructions
     * @param userMessage User's message
     * @param profile Generation profile with parameters
     * @param maxRetries Maximum number of retries (default 3)
     * @return GeminiResponse or null if all retries fail
     */
    public GeminiResponse generateContentWithProfileAndRetry(String systemPrompt, String userMessage, 
                                                             AiGenerationProfileService.GenerationProfile profile, 
                                                             int maxRetries) {
        int attempts = 0;
        GeminiResponse response = null;
        long startTime = System.currentTimeMillis();

        while (attempts < maxRetries && response == null) {
            attempts++;
            log.debug("Gemini API call attempt {} of {} with profile", attempts, maxRetries);
            
            response = generateContentWithProfile(systemPrompt, userMessage, profile);
            
            if (response == null && attempts < maxRetries) {
                try {
                    // Exponential backoff: 400ms, 800ms, 1600ms with jitter
                    long baseBackoff = (long) (400 * Math.pow(2, attempts - 1));
                    long jitter = (long) (Math.random() * 150);
                    long backoffMs = baseBackoff + jitter;
                    log.warn("Retrying Gemini after {}ms backoff (attempt {}/{})", backoffMs, attempts + 1, maxRetries);
                    Thread.sleep(backoffMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;
        if (response == null) {
            log.error("Failed to get response from Gemini after {} attempts in {}ms", maxRetries, totalTime);
        } else {
            log.debug("Gemini API succeeded on attempt {} in {}ms", attempts, totalTime);
        }

        return response;
    }

    /**
     * Analyze message with generation profile
     * 
     * @param systemPrompt System instructions
     * @param userMessage User's message
     * @param profile Generation profile for analysis phase
     * @return AiAnalysisResult parsed from JSON response
     */
    public AiAnalysisResult analyzeMessageWithProfile(String systemPrompt, String userMessage, 
                                                      AiGenerationProfileService.GenerationProfile profile) {
        GeminiResponse response = generateContentWithProfileAndRetry(systemPrompt, userMessage, profile, 3);
        
        if (response == null || !response.isSuccessful()) {
            log.error("Failed to get response from Gemini for analysis");
            return null;
        }

        try {
            String jsonResponse = response.getTextResponse();
            log.debug("Parsing AI analysis from JSON: {}", jsonResponse);
            
            // Parse JSON response to AiAnalysisResult
            AiAnalysisResult result = objectMapper.readValue(jsonResponse, AiAnalysisResult.class);
            
            log.info("AI Analysis - Intent: {}, Confidence: {}, Handoff: {}", 
                result.getIntent(), result.getConfidence(), result.getNeedHandoff());
            
            return result;
            
        } catch (Exception e) {
            log.error("Error parsing AI analysis result", e);
            return null;
        }
    }

    /**
     * Generate content for final response with generation profile
     * 
     * @param systemPrompt System instructions
     * @param userMessage User's message
     * @param profile Generation profile for final response
     * @return GeminiResponse containing the generated content
     */
    public GeminiResponse generateFinalResponseWithProfile(String systemPrompt, String userMessage, 
                                                           AiGenerationProfileService.GenerationProfile profile) {
        if (!geminiProperties.isConfigured()) {
            log.error("Gemini API is not properly configured");
            return null;
        }

        try {
            String url = geminiProperties.getGenerateContentUrl();
            
            // For final response, use text/plain mime type instead of JSON
            GeminiRequest request = GeminiRequest.createWithSystemPrompt(
                systemPrompt,
                userMessage,
                profile.getTemperature(),
                profile.getTopP(),
                profile.getMaxTokens(),
                "text/plain"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create RestTemplate with profile-specific timeout
            RestTemplate profileRestTemplate = createRestTemplateWithTimeout(profile.getTimeoutSeconds());

            HttpEntity<GeminiRequest> entity = new HttpEntity<>(request, headers);

            log.debug("Calling Gemini API for final response with profile");

            ResponseEntity<GeminiResponse> response = profileRestTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                GeminiResponse.class
            );

            GeminiResponse responseBody = response.getBody();
            
            if (responseBody != null && responseBody.isSuccessful()) {
                log.debug("Gemini API final response received successfully");
                return responseBody;
            } else {
                log.warn("Gemini API returned empty or unsuccessful response for final response");
                return null;
            }

        } catch (RestClientException e) {
            log.error("Error calling Gemini API for final response", e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error with Gemini API final response", e);
            return null;
        }
    }

    /**
     * Generate final response with retry/backoff
     */
    public GeminiResponse generateFinalResponseWithProfileAndRetry(String systemPrompt, String userMessage,
                                                                   AiGenerationProfileService.GenerationProfile profile,
                                                                   int maxRetries) {
        int attempts = 0;
        GeminiResponse response = null;
        long startTime = System.currentTimeMillis();

        while (attempts < maxRetries && response == null) {
            attempts++;
            log.debug("Gemini FINAL call attempt {} of {} with profile", attempts, maxRetries);

            response = generateFinalResponseWithProfile(systemPrompt, userMessage, profile);

            if (response == null && attempts < maxRetries) {
                try {
                    long baseBackoff = (long) (400 * Math.pow(2, attempts - 1));
                    long jitter = (long) (Math.random() * 150);
                    long backoffMs = baseBackoff + jitter;
                    log.warn("Retrying Gemini FINAL after {}ms backoff (attempt {}/{})", backoffMs, attempts + 1, maxRetries);
                    Thread.sleep(backoffMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;
        if (response == null) {
            log.error("Failed to get FINAL response from Gemini after {} attempts in {}ms", maxRetries, totalTime);
        } else {
            log.debug("Gemini FINAL succeeded on attempt {} in {}ms", attempts, totalTime);
        }

        return response;
    }

    /**
     * Generate content with streaming support
     * 
     * @param systemPrompt System instructions
     * @param userMessage User's message
     * @param profile Generation profile
     * @param streamCallback Callback for streaming updates (null for non-streaming)
     * @return GeminiResponse containing the final content
     */
    public GeminiResponse generateContentWithStreaming(String systemPrompt, String userMessage, 
                                                       AiGenerationProfileService.GenerationProfile profile,
                                                       StreamingCallback streamCallback) {
        if (!geminiProperties.isConfigured()) {
            log.error("Gemini API is not properly configured");
            return null;
        }

        // Check if streaming is supported and requested
        boolean useStreaming = streamCallback != null && profile.isStreamingEnabled();
        
        if (useStreaming) {
            log.debug("🌊 Starting STREAMING generation with profile");
            return generateWithStreaming(systemPrompt, userMessage, profile, streamCallback);
        } else {
            log.debug("📝 Using NON-STREAMING generation (fallback)");
            if (streamCallback != null) {
                streamCallback.onStreamingUnavailable();
            }
            return generateFinalResponseWithProfile(systemPrompt, userMessage, profile);
        }
    }

    /**
     * Generate content with server-side streaming
     */
    private GeminiResponse generateWithStreaming(String systemPrompt, String userMessage,
                                                 AiGenerationProfileService.GenerationProfile profile,
                                                 StreamingCallback streamCallback) {
        try {
            // Streaming parameters are intentionally not used in the current simulation

            // Use streaming-compatible HTTP client
            StringBuilder finalResponse = new StringBuilder();
            
            // Simulate streaming with chunked processing (real implementation would use SSE)
            GeminiResponse response = generateFinalResponseWithProfile(systemPrompt, userMessage, profile);
            
            if (response != null && response.isSuccessful()) {
                String fullText = response.getTextResponse();
                simulateStreaming(fullText, streamCallback, finalResponse);
                
                // Return final response
                return response;
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("Error in streaming generation", e);
            streamCallback.onError("Streaming error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Simulate streaming by sending chunks (placeholder for real SSE implementation)
     */
    private void simulateStreaming(String fullText, StreamingCallback callback, StringBuilder finalResponse) {
        try {
            String[] words = fullText.split(" ");
            StringBuilder currentChunk = new StringBuilder();
            
            for (int i = 0; i < words.length; i++) {
                currentChunk.append(words[i]).append(" ");
                finalResponse.append(words[i]).append(" ");
                
                // Send chunk every 5-10 words or at the end
                if ((i + 1) % 7 == 0 || i == words.length - 1) {
                    callback.onChunk(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                    
                    // Small delay to simulate streaming
                    Thread.sleep(50);
                }
            }
            
            callback.onComplete();
            
        } catch (Exception e) {
            log.error("Error in streaming simulation", e);
            callback.onError("Streaming simulation error");
        }
    }

    /**
     * Streaming callback interface
     */
    public interface StreamingCallback {
        void onChunk(String chunk);
        void onComplete();
        void onError(String error);
        void onStreamingUnavailable();
    }

    /**
     * Create RestTemplate with specific timeout
     */
    private RestTemplate createRestTemplateWithTimeout(int timeoutSeconds) {
        RestTemplate customRestTemplate = new RestTemplate();
        customRestTemplate.setRequestFactory(
            new org.springframework.http.client.SimpleClientHttpRequestFactory() {{
                setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
                setReadTimeout(Duration.ofSeconds(timeoutSeconds));
            }}
        );
        return customRestTemplate;
    }

    /**
     * Check if Gemini API is available
     */
    public boolean isAvailable() {
        return geminiProperties.isConfigured();
    }
}
