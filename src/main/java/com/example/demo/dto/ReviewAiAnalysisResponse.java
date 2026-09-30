package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for AI analysis response of reviews
 * Contains sentiment analysis and suggested replies
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewAiAnalysisResponse {
    
    private String sentiment; // "POSITIVE", "NEUTRAL", "NEGATIVE"
    private String sentimentLabel; // "Tích cực 😊", "Trung tính 😐", "Tiêu cực 😡"
    private String mainIssue; // Tóm tắt vấn đề chính
    private List<SuggestedReply> suggestedReplies;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SuggestedReply {
        private String label; // "Lịch sự", "Kèm đền bù", "Chuyên nghiệp"
        private String content; // Nội dung gợi ý
    }
}
