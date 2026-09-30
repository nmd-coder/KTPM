package com.example.demo.dto.gemini;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Request DTO for Gemini API
 * Represents the structure of requests sent to Gemini
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeminiRequest {

    private List<Content> contents = new ArrayList<>();
    
    @JsonProperty("generationConfig")
    private GenerationConfig generationConfig;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        private List<Part> parts = new ArrayList<>();
        private String role; // "user" or "model"

        public Content(String text) {
            this.parts.add(new Part(text));
            this.role = "user";
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Part {
        private String text;
    }

    @Data
    @NoArgsConstructor
    public static class GenerationConfig {
        private Double temperature;
        private Double topP;
        private Integer maxOutputTokens;
        private String responseMimeType; // "application/json" for structured output
        
        public GenerationConfig(Double temperature, Integer maxOutputTokens) {
            this.temperature = temperature;
            this.maxOutputTokens = maxOutputTokens;
            this.responseMimeType = "application/json";
        }
        
        public GenerationConfig(Double temperature, Double topP, Integer maxOutputTokens) {
            this.temperature = temperature;
            this.topP = topP;
            this.maxOutputTokens = maxOutputTokens;
            this.responseMimeType = "application/json";
        }
        
        public GenerationConfig(Double temperature, Double topP, Integer maxOutputTokens, String responseMimeType) {
            this.temperature = temperature;
            this.topP = topP;
            this.maxOutputTokens = maxOutputTokens;
            this.responseMimeType = responseMimeType;
        }
    }

    /**
     * Create a simple request with user message
     */
    public static GeminiRequest create(String userMessage, Double temperature, Integer maxTokens) {
        GeminiRequest request = new GeminiRequest();
        request.getContents().add(new Content(userMessage));
        request.setGenerationConfig(new GenerationConfig(temperature, maxTokens));
        return request;
    }

    /**
     * Create a simple request with user message and topP
     */
    public static GeminiRequest create(String userMessage, Double temperature, Double topP, Integer maxTokens) {
        GeminiRequest request = new GeminiRequest();
        request.getContents().add(new Content(userMessage));
        request.setGenerationConfig(new GenerationConfig(temperature, topP, maxTokens));
        return request;
    }

    /**
     * Create request with system prompt and user message
     */
    public static GeminiRequest createWithSystemPrompt(String systemPrompt, String userMessage, Double temperature, Integer maxTokens) {
        GeminiRequest request = new GeminiRequest();
        
        // Add system prompt as first message
        Content systemContent = new Content(systemPrompt);
        systemContent.setRole("model"); // System instructions as model response
        request.getContents().add(systemContent);
        
        // Add user message
        request.getContents().add(new Content(userMessage));
        
        request.setGenerationConfig(new GenerationConfig(temperature, maxTokens));
        return request;
    }

    /**
     * Create request with system prompt, user message, and topP
     */
    public static GeminiRequest createWithSystemPrompt(String systemPrompt, String userMessage, Double temperature, Double topP, Integer maxTokens) {
        GeminiRequest request = new GeminiRequest();
        
        // Add system prompt as first message
        Content systemContent = new Content(systemPrompt);
        systemContent.setRole("model"); // System instructions as model response
        request.getContents().add(systemContent);
        
        // Add user message
        request.getContents().add(new Content(userMessage));
        
        request.setGenerationConfig(new GenerationConfig(temperature, topP, maxTokens));
        return request;
    }

    /**
     * Create request with system prompt, user message, and full configuration
     */
    public static GeminiRequest createWithSystemPrompt(String systemPrompt, String userMessage, Double temperature, Double topP, Integer maxTokens, String responseMimeType) {
        GeminiRequest request = new GeminiRequest();
        
        // Add system prompt as first message
        Content systemContent = new Content(systemPrompt);
        systemContent.setRole("model"); // System instructions as model response
        request.getContents().add(systemContent);
        
        // Add user message
        request.getContents().add(new Content(userMessage));
        
        request.setGenerationConfig(new GenerationConfig(temperature, topP, maxTokens, responseMimeType));
        return request;
    }
}
