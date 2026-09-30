package com.example.demo.service;

import com.example.demo.client.GeminiClient;
import com.example.demo.dto.gemini.GeminiResponse;
import com.example.demo.entity.Catalog;
import com.example.demo.repository.CatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * Service for AI-powered product description generation
 * Completely separate from AiPromptService (used for AI chat)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminProductAiService {

    private final GeminiClient geminiClient;
    private final CatalogRepository catalogRepository;

    /**
     * Generate product description using AI
     * 
     * @param productName Name of the product
     * @param catalogId Catalog ID (optional)
     * @param keywords Keywords for AI (optional)
     * @return Generated description
     * @throws Exception if generation fails
     */
    public String generateProductDescription(String productName, Long catalogId, String keywords) throws Exception {
        log.info("Generating AI description for product: {}", productName);
        
        // Get catalog name if catalogId provided
        String catalogName = "";
        if (catalogId != null) {
            try {
                Catalog catalog = catalogRepository.findById(catalogId).orElse(null);
                if (catalog != null) {
                    catalogName = catalog.getValue();
                }
            } catch (Exception e) {
                log.warn("Could not fetch catalog name for ID: {}", catalogId, e);
            }
        }
        
        // Build prompt
        String prompt = buildPrompt(productName, catalogName, keywords);
        log.debug("Built prompt for AI (length: {} chars)", prompt.length());
        
        // Call Gemini API with retry mechanism
        String description = callGeminiWithRetry(prompt, productName, catalogName, keywords);
        
        // Trust AI to follow the 300-word limit, no character truncation needed
        
        log.info("Successfully generated description ({} chars)", description.length());
        return description;
    }
    
    /**
     * Build marketing prompt for product description
     */
    private String buildPrompt(String productName, String catalogName, String keywords) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Bạn là chuyên gia marketing shop hoa trực tuyến.\n");
        prompt.append("Viết mô tả bán hàng HOÀN CHỈNH cho sản phẩm:\n");
        prompt.append("- Tên: ").append(productName).append("\n");
        prompt.append("- Danh mục: ").append(catalogName).append("\n");
        prompt.append("- Từ khóa: ").append(keywords).append("\n\n");
        
        prompt.append("YÊU CẦU:\n");
        prompt.append("- Giọng văn: Lãng mạn, tiếng Việt tự nhiên\n");
        prompt.append("- Độ dài: CHÍNH XÁC 250-300 TỪ (không phải ký tự)\n");
        prompt.append("- Cấu trúc: 5-6 câu hoàn chỉnh, mỗi câu 40-50 từ\n");
        prompt.append("- Tối ưu SEO: Tích hợp từ khóa tự nhiên\n");
        prompt.append("- Kết thúc: Call-to-action mềm mại\n");
        prompt.append("- YÊU CẦU QUAN TRỌNG: Mô tả phải thật chi tiết và hấp dẫn, với độ dài lý tưởng khoảng 250-300 từ. TUYỆT ĐỐI KHÔNG VIẾT NGẮN HƠN 200 TỪ HOẶC DÀI HƠN 300 TỪ.\n\n");
        
        prompt.append("QUAN TRỌNG: \n");
        prompt.append("- Viết ĐẦY ĐỦ, SÚC TÍCH, đúng 250-300 từ\n");
        prompt.append("- Chỉ trả về nội dung mô tả thuần túy, KHÔNG có JSON, KHÔNG có dấu ngoặc kép\n");
        prompt.append("- VIẾT HOÀN CHỈNH, KHÔNG BỊ CẮT CỤT\n");
        prompt.append("- Kết thúc tự nhiên, đầy đủ ý nghĩa\n");
        prompt.append("- CHÍNH XÁC 5-6 CÂU HOÀN CHỈNH!");
        
        return prompt.toString();
    }
    
    /**
     * Call Gemini API with exponential backoff retry
     */
    private String callGeminiWithRetry(String prompt, String productName, String catalogName, String keywords) throws Exception {
        int maxRetries = 3;
        long[] delays = {100, 400, 900}; // milliseconds
        
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                log.debug("Gemini API attempt {} of {}", attempt + 1, maxRetries);
                
                GeminiResponse response = geminiClient.generateContent(prompt);
                
                if (response != null && response.isSuccessful()) {
                    String textResponse = response.getTextResponse();
                    if (textResponse != null && !textResponse.trim().isEmpty()) {
                        // Clean up response - remove JSON formatting if present
                        String cleanedResponse = cleanAiResponse(textResponse.trim());
                        return cleanedResponse;
                    }
                }
                
                log.warn("Gemini API returned empty response on attempt {}", attempt + 1);
                
            } catch (Exception e) {
                log.warn("Gemini API attempt {} failed: {}", attempt + 1, e.getMessage());
                
                // If this is the last attempt, throw the exception
                if (attempt == maxRetries - 1) {
                    throw e;
                }
            }
            
            // Wait before retry (except on last attempt)
            if (attempt < maxRetries - 1) {
                try {
                    TimeUnit.MILLISECONDS.sleep(delays[attempt]);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new Exception("Request interrupted", ie);
                }
            }
        }
        
        // Return fallback description if all attempts fail
        log.warn("All Gemini API attempts failed, returning fallback description");
        return generateDetailedFallback(productName, catalogName, keywords);
    }
    
    /**
     * Clean AI response to extract plain text description
     */
    private String cleanAiResponse(String response) {
        try {
            String trimmed = response.trim();
            
            // Case 1: JSON array - e.g. ["text content"]
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                log.debug("Detected JSON array response, extracting text");
                
                // Remove outer brackets and quotes
                String content = trimmed.substring(1, trimmed.length() - 1).trim();
                
                // If it starts and ends with quotes, remove them
                if (content.startsWith("\"") && content.endsWith("\"")) {
                    content = content.substring(1, content.length() - 1);
                }
                
                // Unescape JSON escapes
                content = content.replaceAll("\\\\n", "\n")
                                .replaceAll("\\\\\"", "\"")
                                .replaceAll("\\\\\\\\", "\\\\");
                
                return content.trim();
            }
            
            // Case 2: JSON object - e.g. {"product_description": "text"}
            if (trimmed.startsWith("{")) {
                log.debug("Detected JSON object response, extracting product_description field");
                
                response = response.replaceAll("\\\\n", "\n")
                                 .replaceAll("\\\\\"", "\"");
                
                // Simple extraction of product_description field
                String pattern = "\"product_description\"\\s*:\\s*\"([^\"]+)\"";
                java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
                java.util.regex.Matcher m = p.matcher(response);
                
                if (m.find()) {
                    return m.group(1).replaceAll("\\\\n", "\n");
                }
            }
            
            // Case 3: Plain text - return as is
            return response;
            
        } catch (Exception e) {
            log.warn("Error cleaning AI response, returning as is: {}", e.getMessage());
            return response;
        }
    }
    
    /**
     * Generate detailed fallback description with 5 sentences
     */
    private String generateDetailedFallback(String productName, String catalogName, String keywords) {
        StringBuilder fallback = new StringBuilder();
        
        // Sentence 1: Product introduction
        fallback.append(productName).append(" - một tuyệt tác của thiên nhiên, mang đến vẻ đẹp tinh tế và sang trọng. ");
        
        // Sentence 2: Category context
        if (catalogName != null && !catalogName.trim().isEmpty()) {
            fallback.append("Thuộc danh mục ").append(catalogName).append(", sản phẩm này được chọn lọc kỹ lưỡng từ những bông hoa tươi nhất. ");
        } else {
            fallback.append("Được chọn lọc kỹ lưỡng từ những bông hoa tươi nhất, mang đến hương thơm quyến rũ. ");
        }
        
        // Sentence 3: Quality and features
        if (keywords != null && !keywords.trim().isEmpty()) {
            String[] keywordArray = keywords.split(",");
            if (keywordArray.length > 0) {
                fallback.append("Với đặc điểm ").append(keywordArray[0].trim());
                if (keywordArray.length > 1) {
                    fallback.append(" và ").append(keywordArray[1].trim());
                }
                fallback.append(", sản phẩm này sẽ làm hài lòng mọi khách hàng khó tính. ");
            } else {
                fallback.append("Chất lượng cao cấp, phù hợp cho mọi dịp đặc biệt trong cuộc sống. ");
            }
        } else {
            fallback.append("Chất lượng cao cấp, phù hợp cho mọi dịp đặc biệt trong cuộc sống. ");
        }
        
        // Sentence 4: Emotional appeal
        fallback.append("Mỗi cánh hoa như một lời yêu thương, truyền tải cảm xúc chân thành và sâu sắc. ");
        
        // Sentence 5: Call to action
        fallback.append("Hãy để ").append(productName).append(" trở thành món quà ý nghĩa nhất cho người thân yêu của bạn.");
        
        String result = fallback.toString();
        
        // Trust fallback to be reasonable length, no truncation needed
        return result;
    }
    
}
