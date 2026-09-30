package com.example.demo.service;

import com.example.demo.client.GeminiClient;
import com.example.demo.dto.AiInsightResponse;
import com.example.demo.dto.DashboardInsightsData;
import com.example.demo.dto.ReviewAiAnalysisResponse;
import com.example.demo.dto.VoucherSuggestionResponse;
import com.example.demo.dto.gemini.GeminiResponse;
import com.example.demo.entity.Product;
import com.example.demo.entity.Review;
import com.example.demo.entity.Voucher;
import com.example.demo.entity.enums.DiscountType;
import com.example.demo.entity.enums.OrderStatus;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ReviewRepository;
import com.example.demo.repository.VoucherRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service riêng biệt cho AI Insights của Admin Dashboard
 * KHÔNG can thiệp vào AiAnalyticsService (AI chat)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminAiInsightsService {

    private final GeminiClient geminiClient;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final VoucherRepository voucherRepository;
    private final ObjectMapper objectMapper;

    /**
     * Lấy AI insights cho dashboard với caching
     */
    @Cacheable(value = "adminAiInsights", key = "'dashboard'")
    public AiInsightResponse getAiInsights() {
        log.info("Generating AI insights for admin dashboard");
        
        try {
            // 1. Thu thập raw data từ repositories
            DashboardInsightsData rawData = collectRawData();
            log.debug("Collected raw data: {}", rawData);
            
            // 2. Build prompt với data thực
            String prompt = buildPrompt(rawData);
            log.debug("Built prompt for AI");
            
            // 3. Call Gemini API
            GeminiResponse response = geminiClient.generateContentWithRetry(prompt, 2);
            if (response == null || !response.isSuccessful()) {
                log.warn("Gemini API failed, returning fallback insights");
                return getFallbackInsights();
            }
            
            // 4. Parse JSON response with robust error handling
            String jsonResponse = response.getTextResponse();
            log.debug("AI response: {}", jsonResponse);
            
            // Validate JSON response before parsing
            if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
                log.warn("AI returned empty response, using fallback");
                return getFallbackInsights();
            }
            
            // Clean and validate JSON response
            String cleanedJsonResponse = cleanJsonResponse(jsonResponse);
            if (cleanedJsonResponse == null) {
                log.warn("Failed to clean JSON response, using fallback");
                return getFallbackInsights();
            }
            
            AiInsightResponse aiResponse;
            try {
                aiResponse = objectMapper.readValue(cleanedJsonResponse, AiInsightResponse.class);
            } catch (Exception parseException) {
                log.error("Failed to parse AI response JSON: {}", parseException.getMessage());
                log.debug("Raw response that failed to parse: {}", jsonResponse);
                log.debug("Cleaned response that failed to parse: {}", cleanedJsonResponse);
                return getFallbackInsights();
            }
            
            // 5. Validate response
            if (aiResponse.getInsights() == null || aiResponse.getInsights().isEmpty()) {
                log.warn("AI returned empty insights, using fallback");
                return getFallbackInsights();
            }
            
            log.info("Successfully generated {} AI insights", aiResponse.getInsights().size());
            return aiResponse;
            
        } catch (Exception e) {
            log.error("Error generating AI insights", e);
            return getFallbackInsights();
        }
    }

    /**
     * Thu thập raw data từ database
     */
    private DashboardInsightsData collectRawData() {
        // Revenue data - 24h gần nhất vs 24h trước đó
        BigDecimal revenueYesterday = orderRepository.getYesterdayRevenue(); // 24h gần nhất
        BigDecimal revenuePreviousDay = orderRepository.getPreviousDayRevenue(); // 24h trước đó
        BigDecimal revenueLastWeek = orderRepository.getLastWeekRevenue(); // 7 ngày gần nhất
        
        // DEBUG: Log raw revenue values
        log.info("=== AI INSIGHTS DEBUG ===");
        log.info("Revenue Last 24h (raw from DB): {}", revenueYesterday);
        log.info("Revenue Previous 24h (raw from DB): {}", revenuePreviousDay);
        log.info("Revenue Last 7 days (raw from DB): {}", revenueLastWeek);
        
        // Tính growth: so sánh 24h gần nhất với 24h trước đó
        BigDecimal revenueGrowth = BigDecimal.ZERO;
        if (revenuePreviousDay.compareTo(BigDecimal.ZERO) > 0) {
            revenueGrowth = revenueYesterday.subtract(revenuePreviousDay)
                    .divide(revenuePreviousDay, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        } else if (revenueYesterday.compareTo(BigDecimal.ZERO) > 0) {
            // Nếu 24h trước = 0 nhưng 24h gần nhất > 0 => tăng trưởng 100%
            revenueGrowth = BigDecimal.valueOf(100);
        }
        
        // Top products
        List<String> topProducts = productRepository.findTopSellingProductsLast7Days().stream()
                .map(row -> (String) row[0])
                .limit(3)
                .collect(Collectors.toList());
        
        // Order data
        Long cancelledOrders = orderRepository.getCancelledOrdersLast7Days();
        Long totalOrders = orderRepository.getTotalOrdersLast7Days();
        Double cancelRate = 0.0;
        if (totalOrders > 0) {
            cancelRate = (cancelledOrders.doubleValue() / totalOrders.doubleValue()) * 100;
        }
        
        Long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);
        
        // Inventory data
        List<String> lowStockProducts = productRepository.findLowStockProducts(10).stream()
                .map(Product::getName)
                .limit(3)
                .collect(Collectors.toList());
        
        List<String> highStockProducts = productRepository.findHighStockProducts(100).stream()
                .map(Product::getName)
                .limit(3)
                .collect(Collectors.toList());
        
        // Review data
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        Long totalReviews = reviewRepository.countByCreatedAtAfter(weekAgo);
        Long positiveReviews = reviewRepository.countByRatingGreaterThanEqualAndCreatedAtAfter(4, weekAgo);
        Double positiveRate = 0.0;
        if (totalReviews > 0) {
            positiveRate = (positiveReviews.doubleValue() / totalReviews.doubleValue()) * 100;
        }
        
        // Get recent reviews for complaints analysis
        List<Review> recentReviews = reviewRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 10)).getContent();
        List<String> complaints = recentReviews.stream()
                .filter(r -> r.getRating() <= 2)
                .map(Review::getComment)
                .filter(comment -> comment != null && !comment.trim().isEmpty())
                .limit(3)
                .collect(Collectors.toList());
        
        return DashboardInsightsData.builder()
                .revenueYesterday(revenueYesterday)
                .revenueLastWeek(revenueLastWeek)
                .revenueGrowth(revenueGrowth)
                .topProducts(topProducts)
                .cancelRate(cancelRate)
                .cancelReasons(List.of("Giao hàng chậm", "Sản phẩm không đúng", "Thay đổi ý định")) // Mock data
                .pendingOrders(pendingOrders)
                .totalOrdersLast7Days(totalOrders)
                .lowStockProducts(lowStockProducts)
                .highStockProducts(highStockProducts)
                .reviewCount(totalReviews)
                .positiveReviewRate(positiveRate)
                .complaints(complaints)
                .build();
    }

    /**
     * Build prompt cho AI với data thực
     */
    private String buildPrompt(DashboardInsightsData data) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Bạn là AI Business Analyst chuyên sâu về e-commerce hoa tươi.\n");
        prompt.append("Nhiệm vụ: Phân tích dữ liệu và đưa ra 3-4 insights HÀNH ĐỘNG được, không chỉ mô tả số liệu.\n\n");
        prompt.append("NGUYÊN TẮC PHÂN TÍCH:\n");
        prompt.append("1. KHÔNG chỉ nói \"Doanh thu tăng X%\" - Hãy giải thích TẠI SAO và NÊN LÀM GÌ\n");
        prompt.append("2. Tìm PATTERN ẩn: xu hướng, bất thường, cơ hội, rủi ro\n");
        prompt.append("3. So sánh với BENCHMARK ngành hoa (tỷ lệ hủy <5%, review ≥4.5 sao, tồn kho 2-4 tuần)\n");
        prompt.append("4. Đưa ra HÀNH ĐỘNG cụ thể: giảm giá, nhập thêm, liên hệ khách, tối ưu quy trình\n");
        prompt.append("5. Ưu tiên vấn đề ẢNH HƯỞNG LỚN đến doanh thu/lợi nhuận/trải nghiệm khách\n\n");
        
        prompt.append("YÊU CẦU QUAN TRỌNG: Trả về ĐÚNG format JSON sau:\n");
        prompt.append("{\n");
        prompt.append("  \"insights\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"type\": \"revenue|order|inventory|review\",\n");
        prompt.append("      \"icon\": \"📈|📉|⚠️|⭐|💰|📦|🚨\",\n");
        prompt.append("      \"title\": \"Tiêu đề ngắn gọn (max 50 ký tự)\",\n");
        prompt.append("      \"message\": \"Nội dung chi tiết với số liệu cụ thể\",\n");
        prompt.append("      \"severity\": \"success|warning|danger|info\",\n");
        prompt.append("      \"actionLink\": \"/admin/products hoặc /admin/orders (nếu cần)\",\n");
        prompt.append("      \"actionText\": \"Xem chi tiết|Kiểm tra ngay (nếu cần)\"\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n\n");
        
        prompt.append("QUY TẮC FORMAT:\n");
        prompt.append("1. Tối đa 4 insights, ưu tiên theo tác động: Doanh thu > Tồn kho > Đơn hàng > Review\n");
        prompt.append("2. Title: Ngắn gọn, hành động (VD: \"Cơ hội tăng 30% doanh thu\", \"Nguy cơ mất khách\")\n");
        prompt.append("3. Message: Cấu trúc 3 phần:\n");
        prompt.append("   - PHÁT HIỆN: Số liệu + so sánh (VD: \"Tỷ lệ hủy 8.5% cao hơn ngành 70%\")\n");
        prompt.append("   - NGUYÊN NHÂN: Giải thích tại sao (VD: \"Do giao hàng chậm, sản phẩm không đúng\")\n");
        prompt.append("   - HÀNH ĐỘNG: Gợi ý cụ thể (VD: \"Nên đổi đơn vị vận chuyển, cải thiện QC\")\n");
        prompt.append("4. severity: danger (mất tiền/khách), warning (cần chú ý), success (cơ hội), info (trung tính)\n");
        prompt.append("5. actionLink/actionText: Luôn có nếu đề cập sản phẩm/đơn hàng cụ thể\n\n");
        
        prompt.append("VÍ DỤ INSIGHT TỐT:\n");
        prompt.append("{\n");
        prompt.append("  \"type\": \"inventory\",\n");
        prompt.append("  \"icon\": \"🚨\",\n");
        prompt.append("  \"title\": \"Nguy cơ hết hàng sản phẩm bán chạy\",\n");
        prompt.append("  \"message\": \"Hoa Hồng Đỏ (top 1 doanh thu) chỉ còn 8 bông, dự kiến hết trong 2 ngày. Tuần trước bán 45 bông/ngày. Nên nhập gấp 200 bông để đáp ứng nhu cầu cuối tuần.\",\n");
        prompt.append("  \"severity\": \"danger\",\n");
        prompt.append("  \"actionLink\": \"/admin/products\",\n");
        prompt.append("  \"actionText\": \"Nhập hàng ngay\"\n");
        prompt.append("}\n\n");
        
        prompt.append("VÍ DỤ INSIGHT TỆ (TRÁNH):\n");
        prompt.append("- \"Doanh thu tăng 15%\" ❌ (Chỉ mô tả, không giải thích/hành động)\n");
        prompt.append("- \"Có 3 sản phẩm sắp hết\" ❌ (Không nói tên, không đánh giá tác động)\n");
        prompt.append("- \"Tỷ lệ hủy cao\" ❌ (Không có số liệu, nguyên nhân, giải pháp)\n\n");
        
        prompt.append("DỮ LIỆU CỬA HÀNG:\n\n");
        
        prompt.append("DOANH THU:\n");
        prompt.append("- 24h gần nhất: ").append(data.getRevenueYesterday()).append(" VND\n");
        prompt.append("- 7 ngày gần nhất: ").append(data.getRevenueLastWeek()).append(" VND\n");
        prompt.append("- Thay đổi so với 24h trước: ").append(data.getRevenueGrowth()).append("%\n");
        prompt.append("- Top sản phẩm (7 ngày): ").append(String.join(", ", data.getTopProducts())).append("\n\n");
        
        prompt.append("ĐỚN HÀNG (7 ngày gần đây):\n");
        prompt.append("- Tỷ lệ hủy: ").append(String.format("%.1f", data.getCancelRate())).append("%\n");
        prompt.append("- Lý do hủy chính: ").append(String.join(", ", data.getCancelReasons())).append("\n");
        prompt.append("- Đơn chờ xử lý: ").append(data.getPendingOrders()).append("\n\n");
        
        prompt.append("TỒN KHO:\n");
        prompt.append("- Sản phẩm sắp hết (< 10): ").append(String.join(", ", data.getLowStockProducts())).append("\n");
        prompt.append("- Sản phẩm tồn cao (> 100): ").append(String.join(", ", data.getHighStockProducts())).append("\n\n");
        
        prompt.append("ĐÁNH GIÁ (7 ngày gần đây):\n");
        prompt.append("- Tổng số đánh giá: ").append(data.getReviewCount()).append("\n");
        prompt.append("- Tỷ lệ tích cực (≥4 sao): ").append(String.format("%.1f", data.getPositiveReviewRate())).append("%\n");
        prompt.append("- Nội dung phàn nàn: ").append(String.join(", ", data.getComplaints())).append("\n\n");
        
        prompt.append("BENCHMARK NGÀNH HOA TƯƠI E-COMMERCE:\n");
        prompt.append("- Tỷ lệ hủy đơn: <5% (tốt), 5-8% (trung bình), >8% (kém)\n");
        prompt.append("- Tỷ lệ review tích cực: >90% (xuất sắc), 80-90% (tốt), <80% (cần cải thiện)\n");
        prompt.append("- Tồn kho: 2-4 tuần (tối ưu), <1 tuần (thiếu hàng), >6 tuần (ứ đọng)\n");
        prompt.append("- Tốc độ xử lý đơn: <2h (nhanh), 2-6h (bình thường), >6h (chậm)\n");
        prompt.append("- AOV (giá trị đơn trung bình): 300-500k (thấp), 500-800k (tốt), >800k (cao)\n\n");
        
        prompt.append("CÂU HỎI PHÂN TÍCH (trả lời trong insights):\n");
        prompt.append("1. Doanh thu tăng/giảm do ĐÂU? (sản phẩm hot, mùa vụ, marketing, giá cả?)\n");
        prompt.append("2. Sản phẩm nào đang BỊ BỎ QUA nhưng có tiềm năng? (tồn cao + review tốt)\n");
        prompt.append("3. Nguy cơ MẤT KHÁCH nào? (tỷ lệ hủy cao, review kém, giao chậm?)\n");
        prompt.append("4. Cơ hội TĂNG DOANH THU nào? (sản phẩm bán chạy thiếu hàng, combo, upsell?)\n");
        prompt.append("5. Vấn đề VẬN HÀNH nào cần xử lý GẤP? (đơn chờ lâu, tồn kho mất cân đối?)\n\n");
        
        prompt.append("Hãy phân tích THÔNG MINH dựa trên dữ liệu và benchmark, trả về JSON insights.");
        
        return prompt.toString();
    }

    /**
     * Fallback insights khi AI fail
     */
    private AiInsightResponse getFallbackInsights() {
        AiInsightResponse.InsightItem fallbackItem = AiInsightResponse.InsightItem.builder()
                .type("info")
                .icon("🤖")
                .title("Đang cập nhật phân tích")
                .message("Hệ thống AI đang xử lý dữ liệu. Vui lòng quay lại sau.")
                .severity("info")
                .build();
        
        return AiInsightResponse.builder()
                .insights(List.of(fallbackItem))
                .build();
    }

    /**
     * Analyze review using AI to get sentiment and suggested replies
     * 
     * @param reviewId ID of the review to analyze
     * @return ReviewAiAnalysisResponse with sentiment and suggested replies
     */
    @Transactional
    public ReviewAiAnalysisResponse analyzeReview(Long reviewId) {
        log.info("Analyzing review {} with AI", reviewId);
        
        try {
            // 1. Get review from database
            Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found with ID: " + reviewId));
            
            // 2. Check if sentiment already exists
            if (review.getSentiment() != null) {
                log.debug("Review {} already has sentiment: {}", reviewId, review.getSentiment());
                // If sentiment exists, we can still call AI for fresh suggestions
                // or reuse existing sentiment - for now, let's call AI for suggestions
            }
            
            // 3. Build prompt and call AI
            String prompt = buildReviewAnalysisPrompt(review.getComment(), review.getRating());
            log.debug("Built prompt for AI analysis (length: {} chars)", prompt.length());
            
            // 4. Call Gemini API
            GeminiResponse response = geminiClient.generateContentWithRetry(prompt, 2);
            if (response == null || !response.isSuccessful()) {
                log.warn("Gemini API failed for review {}, using fallback", reviewId);
                return createFallbackAnalysis(review.getRating());
            }
            
            // 5. Parse JSON response with robust error handling
            String jsonResponse = response.getTextResponse();
            log.debug("AI response: {}", jsonResponse);
            
            // Validate JSON response before parsing
            if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
                log.warn("AI returned empty response for review {}, using fallback", reviewId);
                return createFallbackAnalysis(review.getRating());
            }
            
            // Clean and validate JSON response
            String cleanedJsonResponse = cleanJsonResponse(jsonResponse);
            if (cleanedJsonResponse == null) {
                log.warn("Failed to clean JSON response for review {}, using fallback", reviewId);
                return createFallbackAnalysis(review.getRating());
            }
            
            ReviewAiAnalysisResponse aiResponse;
            try {
                aiResponse = objectMapper.readValue(cleanedJsonResponse, ReviewAiAnalysisResponse.class);
            } catch (Exception parseException) {
                log.error("Failed to parse AI response JSON for review {}: {}", reviewId, parseException.getMessage());
                log.debug("Raw response that failed to parse: {}", jsonResponse);
                log.debug("Cleaned response that failed to parse: {}", cleanedJsonResponse);
                return createFallbackAnalysis(review.getRating());
            }
            
            // 6. Validate and save sentiment if not exists
            if (review.getSentiment() == null && aiResponse.getSentiment() != null) {
                review.setSentiment(aiResponse.getSentiment());
                reviewRepository.save(review);
                log.info("Saved sentiment {} for review {}", aiResponse.getSentiment(), reviewId);
            }
            
            // 7. Set sentiment label based on sentiment
            if (aiResponse.getSentiment() != null) {
                aiResponse.setSentimentLabel(getSentimentLabel(aiResponse.getSentiment()));
            }
            
            log.info("Successfully analyzed review {} with sentiment: {}", reviewId, aiResponse.getSentiment());
            return aiResponse;
            
        } catch (Exception e) {
            log.error("Error analyzing review {}: {}", reviewId, e.getMessage(), e);
            // Return fallback based on rating
            Review review = reviewRepository.findById(reviewId).orElse(null);
            Integer rating = review != null ? review.getRating() : 3; // Default to neutral
            return createFallbackAnalysis(rating);
        }
    }
    
    /**
     * Build prompt for AI review analysis
     */
    private String buildReviewAnalysisPrompt(String comment, Integer rating) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Bạn là chuyên gia CSKH cửa hàng hoa trực tuyến.\n");
        prompt.append("Phân tích đánh giá này và trả về JSON:\n\n");
        
        prompt.append("{\n");
        prompt.append("  \"sentiment\": \"POSITIVE|NEUTRAL|NEGATIVE\",\n");
        prompt.append("  \"mainIssue\": \"Tóm tắt vấn đề chính (nếu tiêu cực/trung tính)\",\n");
        prompt.append("  \"suggestedReplies\": [\n");
        prompt.append("    {\"label\": \"Lịch sự\", \"content\": \"...\"},\n");
        prompt.append("    {\"label\": \"Kèm đền bù\", \"content\": \"...\"},\n");
        prompt.append("    {\"label\": \"Chuyên nghiệp\", \"content\": \"...\"}\n");
        prompt.append("  ]\n");
        prompt.append("}\n\n");
        
        prompt.append("REVIEW:\n");
        prompt.append("- Rating: ").append(rating).append("/5\n");
        prompt.append("- Comment: ").append(comment != null ? comment : "Không có bình luận").append("\n\n");
        
        prompt.append("QUY TẮC:\n");
        prompt.append("1. POSITIVE: rating 4-5 và comment tốt\n");
        prompt.append("2. NEGATIVE: rating 1-2 hoặc có từ khóa phàn nàn\n");
        prompt.append("3. NEUTRAL: còn lại\n");
        prompt.append("4. suggestedReplies: 2-3 options, mỗi cái 50-80 từ\n");
        prompt.append("5. Giọng văn: Tiếng Việt, thân thiện, chuyên nghiệp\n");
        prompt.append("6. Label phải ngắn gọn: \"Lịch sự\", \"Kèm đền bù\", \"Chuyên nghiệp\", \"Xin lỗi chân thành\"\n");
        prompt.append("7. Chỉ trả về JSON, không có text khác");
        
        return prompt.toString();
    }
    
    /**
     * Create fallback analysis when AI fails
     */
    private ReviewAiAnalysisResponse createFallbackAnalysis(Integer rating) {
        String sentiment;
        String sentimentLabel;
        
        if (rating <= 2) {
            sentiment = "NEGATIVE";
            sentimentLabel = "Tiêu cực 😡";
        } else if (rating == 3) {
            sentiment = "NEUTRAL";
            sentimentLabel = "Trung tính 😐";
        } else {
            sentiment = "POSITIVE";
            sentimentLabel = "Tích cực 😊";
        }
        
        return ReviewAiAnalysisResponse.builder()
                .sentiment(sentiment)
                .sentimentLabel(sentimentLabel)
                .mainIssue("Không thể tải phân tích AI.")
                .suggestedReplies(Collections.emptyList())
                .build();
    }
    
    /**
     * Get sentiment label with emoji
     */
    private String getSentimentLabel(String sentiment) {
        switch (sentiment) {
            case "POSITIVE":
                return "Tích cực 😊";
            case "NEUTRAL":
                return "Trung tính 😐";
            case "NEGATIVE":
                return "Tiêu cực 😡";
            default:
                return "Không xác định";
        }
    }

    /**
     * Suggest voucher configuration using AI based on business objectives
     * 
     * @param objective The business objective: NEW_CUSTOMER, INCREASE_AOV, CLEAR_INVENTORY
     * @param targetProduct Optional product name for inventory clearance
     * @return VoucherSuggestionResponse with AI-generated voucher configuration
     */
    @Transactional
    public VoucherSuggestionResponse suggestVoucher(String objective, String targetProduct) {
        log.info("Generating AI voucher suggestion for objective: {}, targetProduct: {}", objective, targetProduct);
        
        try {
            // 1. Collect real business data
            VoucherSuggestionData businessData = collectVoucherBusinessData(objective, targetProduct);
            log.debug("Collected business data: AOV={}, activeVouchers={}, inventoryProducts={}", 
                businessData.getAov(), businessData.getActiveVouchers().size(), businessData.getInventoryProducts().size());
            
            // 2. Build AI prompt with real data
            String prompt = buildVoucherSuggestionPrompt(objective, targetProduct, businessData);
            log.debug("Built AI prompt for voucher suggestion");
            
            // 3. Call Gemini API
            GeminiResponse response = geminiClient.generateContentWithRetry(prompt, 2);
            if (response == null || !response.isSuccessful()) {
                log.warn("Gemini API failed for voucher suggestion, using fallback");
                return createFallbackVoucherSuggestion(objective, businessData);
            }
            
            // 4. Parse JSON response
            String jsonResponse = response.getTextResponse();
            log.debug("AI voucher response: {}", jsonResponse);
            
            VoucherSuggestionResponse aiResponse = objectMapper.readValue(jsonResponse, VoucherSuggestionResponse.class);
            
            // 5. Validate and enhance response
            validateAndEnhanceSuggestion(aiResponse, businessData);
            
            log.info("Successfully generated AI voucher suggestion: {}", aiResponse.getCode());
            return aiResponse;
            
        } catch (Exception e) {
            log.error("Error generating voucher suggestion", e);
            return createFallbackVoucherSuggestion(objective, null);
        }
    }
    
    /**
     * Collect business data for voucher suggestion
     */
    private VoucherSuggestionData collectVoucherBusinessData(String objective, String targetProduct) {
        // Calculate AOV for last 30 days
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        BigDecimal aov = orderRepository.getAverageOrderValue(thirtyDaysAgo);
        if (aov == null) aov = BigDecimal.valueOf(350000); // Default fallback
        
        // Get active vouchers for conflict checking
        List<Voucher> activeVouchers = voucherRepository.findByIsActiveTrueAndExpiryDateAfter(LocalDate.now());
        
        // Get inventory data based on objective
        List<Product> inventoryProducts = Collections.emptyList();
        if ("CLEAR_INVENTORY".equals(objective)) {
            if (targetProduct != null && !targetProduct.trim().isEmpty()) {
                // Find specific product
                inventoryProducts = productRepository.findProductsByName(targetProduct.trim());
            } else {
                // Find high stock, low sales products
                inventoryProducts = productRepository.findHighStockLowSalesProducts(thirtyDaysAgo);
            }
        }
        
        return VoucherSuggestionData.builder()
                .aov(aov)
                .activeVouchers(activeVouchers)
                .inventoryProducts(inventoryProducts)
                .build();
    }
    
    /**
     * Build AI prompt for voucher suggestion
     */
    private String buildVoucherSuggestionPrompt(String objective, String targetProduct, VoucherSuggestionData data) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Bạn là chuyên gia marketing cho cửa hàng hoa trực tuyến.\n");
        prompt.append("Nhiệm vụ: Tạo cấu hình voucher tối ưu dựa trên dữ liệu kinh doanh thực tế.\n\n");
        
        // Calculate example future date
        String exampleFutureDate = LocalDate.now().plusDays(14).toString();
        
        prompt.append("YÊU CẦU QUAN TRỌNG: Trả về ĐÚNG format JSON sau:\n");
        prompt.append("{\n");
        prompt.append("  \"code\": \"OBJ-CAT-MMDD-XXXX\",\n");
        prompt.append("  \"name\": \"Tên voucher ngắn gọn\",\n");
        prompt.append("  \"description\": \"Mô tả chi tiết về voucher\",\n");
        prompt.append("  \"discountType\": \"PERCENTAGE|FIXED\",\n");
        prompt.append("  \"discountValue\": 20.0,\n");
        prompt.append("  \"maxDiscountAmount\": 100000.0,\n");
        prompt.append("  \"minOrderValue\": 450000.0,\n");
        prompt.append("  \"expiryDate\": \"").append(exampleFutureDate).append("\",\n");
        prompt.append("  \"maxUses\": 100,\n");
        prompt.append("  \"explanation\": \"Giải thích lý do đề xuất cấu hình này\",\n");
        prompt.append("  \"warnings\": [\"Cảnh báo nếu có xung đột\"],\n");
        prompt.append("  \"dataUsed\": \"Tóm tắt dữ liệu đã sử dụng\"\n");
        prompt.append("}\n\n");
        
        prompt.append("QUY TẮC:\n");
        prompt.append("1. code: Format OBJ-CAT-MMDD-XXXX (VD: AOV-FLOWER-1019-A3B2), chỉ chữ IN HOA, số và dấu gạch ngang\n");
        prompt.append("2. name: Tên ngắn gọn, hấp dẫn (VD: 'Giảm 20% cho đơn hàng lớn')\n");
        prompt.append("3. description: Mô tả chi tiết điều kiện áp dụng và lợi ích\n");
        prompt.append("4. discountType: PERCENTAGE cho tăng AOV, FIXED cho thu hút KH mới, PERCENTAGE cao cho đẩy tồn\n");
        prompt.append("5. discountValue: không vượt quá 50% cho PERCENTAGE, không vượt biên lợi nhuận (~30%)\n");
        prompt.append("6. maxDiscountAmount: Giới hạn giảm tối đa cho PERCENTAGE (VD: 100000 VND), null cho FIXED\n");
        prompt.append("7. minOrderValue: ≈ AOV * 1.25 (để tăng AOV nhưng vẫn đạt tỷ lệ chuyển đổi tốt)\n");
        prompt.append("8. expiryDate: Ngày hết hạn format YYYY-MM-DD, PHẢI là ngày trong TƯƠNG LAI (ít nhất 7-30 ngày từ hôm nay)\n");
        prompt.append("9. maxUses: Số lượt sử dụng tối đa (VD: 50-200), null nếu không giới hạn\n");
        prompt.append("10. explanation: ngắn gọn, có số liệu cụ thể, lý do rõ ràng\n");
        prompt.append("11. warnings: chỉ thêm nếu có xung đột với voucher đang chạy\n");
        prompt.append("12. Ngôn ngữ: Tiếng Việt tự nhiên, chuyên nghiệp\n\n");
        
        prompt.append("DỮ LIỆU KINH DOANH:\n\n");
        prompt.append("NGÀY HÔM NAY: ").append(LocalDate.now()).append("\n");
        prompt.append("MỤC TIÊU: ").append(objective).append("\n");
        if (targetProduct != null && !targetProduct.trim().isEmpty()) {
            prompt.append("SẢN PHẨM MỤC TIÊU: ").append(targetProduct).append("\n");
        }
        prompt.append("AOV (Giá trị đơn hàng trung bình): ").append(data.getAov()).append(" VND\n");
        
        prompt.append("VOUCHER ĐANG CHẠY:\n");
        if (data.getActiveVouchers().isEmpty()) {
            prompt.append("- Không có voucher nào đang chạy\n");
        } else {
            for (Voucher voucher : data.getActiveVouchers()) {
                prompt.append("- ").append(voucher.getCode()).append(": ")
                      .append(voucher.getDiscountType()).append(" ")
                      .append(voucher.getDiscountValue())
                      .append(voucher.getDiscountType() == DiscountType.PERCENTAGE ? "%" : " VND")
                      .append(", đơn tối thiểu ").append(voucher.getMinOrderValue()).append(" VND\n");
            }
        }
        
        if ("CLEAR_INVENTORY".equals(objective)) {
            prompt.append("SẢN PHẨM TỒN KHO:\n");
            if (data.getInventoryProducts().isEmpty()) {
                prompt.append("- Không tìm thấy sản phẩm tồn kho cao\n");
            } else {
                for (Product product : data.getInventoryProducts().stream().limit(5).collect(Collectors.toList())) {
                    prompt.append("- ").append(product.getName()).append(": tồn ").append(product.getStockQuantity()).append(" sản phẩm\n");
                }
            }
        }
        
        prompt.append("\nHãy phân tích và trả về JSON voucher suggestion.");
        
        return prompt.toString();
    }
    
    /**
     * Validate and enhance AI suggestion
     */
    private void validateAndEnhanceSuggestion(VoucherSuggestionResponse suggestion, VoucherSuggestionData data) {
        // Validate discount value
        if (suggestion.getDiscountType() == DiscountType.PERCENTAGE && 
            suggestion.getDiscountValue().compareTo(BigDecimal.valueOf(50)) > 0) {
            suggestion.setDiscountValue(BigDecimal.valueOf(30)); // Cap at 30%
        }
        
        // Validate expiry date - must be in the future
        if (suggestion.getExpiryDate() != null) {
            try {
                LocalDate expiryDate = LocalDate.parse(suggestion.getExpiryDate());
                LocalDate today = LocalDate.now();
                
                // If expiry date is today or in the past, set it to 14 days from now
                if (!expiryDate.isAfter(today)) {
                    suggestion.setExpiryDate(today.plusDays(14).toString());
                    log.warn("AI suggested past expiry date {}, corrected to {}", expiryDate, suggestion.getExpiryDate());
                }
            } catch (Exception e) {
                // If date parsing fails, set default to 14 days from now
                suggestion.setExpiryDate(LocalDate.now().plusDays(14).toString());
                log.error("Invalid expiry date format from AI: {}, using default", suggestion.getExpiryDate(), e);
            }
        } else {
            // If no expiry date provided, set default to 14 days from now
            suggestion.setExpiryDate(LocalDate.now().plusDays(14).toString());
        }
        
        // Check for code conflicts
        if (voucherRepository.existsByCode(suggestion.getCode())) {
            // Generate new code
            String newCode = generateUniqueVoucherCode(suggestion.getCode());
            suggestion.setCode(newCode);
        }
        
        // Check for minOrderValue conflicts with active vouchers
        List<String> warnings = suggestion.getWarnings() != null ? suggestion.getWarnings() : Collections.emptyList();
        for (Voucher activeVoucher : data.getActiveVouchers()) {
            if (suggestion.getMinOrderValue().equals(activeVoucher.getMinOrderValue())) {
                warnings.add("Có voucher khác đang chạy với cùng điều kiện đơn tối thiểu: " + activeVoucher.getCode());
            }
        }
        suggestion.setWarnings(warnings);
        
        // Set dataUsed summary
        suggestion.setDataUsed(String.format("AOV: %s VND, %d voucher đang chạy, %d sản phẩm tồn kho", 
            data.getAov(), data.getActiveVouchers().size(), data.getInventoryProducts().size()));
    }
    
    /**
     * Generate unique voucher code
     */
    private String generateUniqueVoucherCode(String baseCode) {
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(8); // Last 5 digits
        return baseCode.substring(0, Math.min(baseCode.length() - 4, 8)) + "-" + timestamp;
    }
    
    /**
     * Create fallback suggestion when AI fails
     */
    private VoucherSuggestionResponse createFallbackVoucherSuggestion(String objective, VoucherSuggestionData data) {
        BigDecimal aov = data != null ? data.getAov() : BigDecimal.valueOf(350000);
        BigDecimal minOrderValue = aov.multiply(BigDecimal.valueOf(1.25));
        
        String code = generateFallbackCode(objective);
        DiscountType discountType = "INCREASE_AOV".equals(objective) ? DiscountType.PERCENTAGE : DiscountType.FIXED;
        BigDecimal discountValue = "INCREASE_AOV".equals(objective) ? BigDecimal.valueOf(20) : BigDecimal.valueOf(50000);
        
        // Generate name and description based on objective
        String name;
        String description;
        if ("NEW_CUSTOMER".equals(objective)) {
            name = "Ưu đãi khách hàng mới";
            description = "Giảm giá đặc biệt dành cho khách hàng mới đặt hàng lần đầu. Áp dụng cho đơn hàng từ " + minOrderValue.intValue() + " VND.";
        } else if ("INCREASE_AOV".equals(objective)) {
            name = "Giảm " + discountValue.intValue() + "% cho đơn hàng lớn";
            description = "Tăng giá trị đơn hàng với ưu đãi giảm giá hấp dẫn. Áp dụng cho đơn hàng từ " + minOrderValue.intValue() + " VND.";
        } else {
            name = "Thanh lý hàng tồn kho";
            description = "Giảm giá đặc biệt để thanh lý hàng tồn kho. Số lượng có hạn, nhanh tay đặt hàng!";
        }
        
        // Calculate expiry date (14 days from now)
        String expiryDate = java.time.LocalDate.now().plusDays(14).toString();
        
        return VoucherSuggestionResponse.builder()
                .code(code)
                .name(name)
                .description(description)
                .discountType(discountType)
                .discountValue(discountValue)
                .maxDiscountAmount(discountType == DiscountType.PERCENTAGE ? BigDecimal.valueOf(100000) : null)
                .minOrderValue(minOrderValue)
                .expiryDate(expiryDate)
                .maxUses(100)
                .explanation("Gợi ý mặc định: AOV hiện tại " + aov + " VND. Đề xuất voucher " + 
                    discountType + " " + discountValue + 
                    (discountType == DiscountType.PERCENTAGE ? "%" : " VND") + 
                    " cho đơn từ " + minOrderValue + " VND.")
                .warnings(Collections.singletonList("Đây là gợi ý mặc định do AI không khả dụng"))
                .dataUsed("Fallback template")
                .build();
    }
    
    /**
     * Generate fallback voucher code
     */
    private String generateFallbackCode(String objective) {
        String prefix = "NEW".equals(objective) ? "NEW" : 
                       "AOV".equals(objective) ? "AOV" : "INV";
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(8);
        return prefix + "-FALLBACK-" + timestamp;
    }
    
    /**
     * Clean and validate JSON response from AI
     * Handles common JSON formatting issues from Gemini API
     */
    private String cleanJsonResponse(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Remove any leading/trailing whitespace
            String cleaned = jsonResponse.trim();
            
            // If response doesn't start with {, try to extract JSON from markdown or other formats
            if (!cleaned.startsWith("{")) {
                // Try to find JSON block in markdown
                int jsonStart = cleaned.indexOf("```json");
                if (jsonStart != -1) {
                    jsonStart = cleaned.indexOf("{", jsonStart);
                } else {
                    jsonStart = cleaned.indexOf("{");
                }
                
                if (jsonStart != -1) {
                    int jsonEnd = cleaned.lastIndexOf("}");
                    if (jsonEnd > jsonStart) {
                        cleaned = cleaned.substring(jsonStart, jsonEnd + 1);
                    }
                }
            }
            
            // Fix common JSON issues
            cleaned = fixCommonJsonIssues(cleaned);
            
            // Try to fix truncated JSON
            cleaned = fixTruncatedJson(cleaned);
            
            // Validate that it's valid JSON by attempting to parse it
            try {
                objectMapper.readTree(cleaned);
                return cleaned;
            } catch (Exception e) {
                log.warn("Cleaned JSON is still invalid: {}", e.getMessage());
                return null;
            }
            
        } catch (Exception e) {
            log.error("Error cleaning JSON response: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Fix common JSON formatting issues from AI responses
     */
    private String fixCommonJsonIssues(String json) {
        if (json == null) return null;
        
        // Fix unclosed strings by finding and closing them
        json = fixUnclosedStrings(json);
        
        // Fix common escape issues
        json = json.replace("\\\"", "\"")
                  .replace("\\n", "\n")
                  .replace("\\t", "\t")
                  .replace("\\r", "\r");
        
        // Remove any trailing commas before closing braces/brackets
        json = json.replaceAll(",\\s*([}\\]])", "$1");
        
        return json;
    }
    
    /**
     * Fix truncated JSON responses
     */
    private String fixTruncatedJson(String json) {
        if (json == null) return null;
        
        try {
            // Count opening and closing braces/brackets
            int openBraces = 0;
            int openBrackets = 0;
            boolean inString = false;
            boolean escaped = false;
            
            for (int i = 0; i < json.length(); i++) {
                char c = json.charAt(i);
                
                if (escaped) {
                    escaped = false;
                    continue;
                }
                
                if (c == '\\') {
                    escaped = true;
                    continue;
                }
                
                if (c == '"') {
                    inString = !inString;
                    continue;
                }
                
                if (!inString) {
                    if (c == '{') {
                        openBraces++;
                    } else if (c == '}') {
                        openBraces--;
                    } else if (c == '[') {
                        openBrackets++;
                    } else if (c == ']') {
                        openBrackets--;
                    }
                }
            }
            
            // If we have unclosed structures, try to close them
            StringBuilder result = new StringBuilder(json);
            
            // Close any unclosed strings first
            if (inString) {
                result.append('"');
            }
            
            // Close unclosed brackets
            for (int i = 0; i < openBrackets; i++) {
                result.append(']');
            }
            
            // Close unclosed braces
            for (int i = 0; i < openBraces; i++) {
                result.append('}');
            }
            
            if (openBraces > 0 || openBrackets > 0 || inString) {
                log.warn("Fixed truncated JSON - closed {} braces, {} brackets, string: {}", 
                    openBraces, openBrackets, inString);
            }
            
            return result.toString();
            
        } catch (Exception e) {
            log.error("Error fixing truncated JSON: {}", e.getMessage());
            return json; // Return original if fixing fails
        }
    }

    /**
     * Fix unclosed string values in JSON
     */
    private String fixUnclosedStrings(String json) {
        if (json == null) return null;
        
        try {
            StringBuilder result = new StringBuilder();
            boolean inString = false;
            boolean escaped = false;
            int lineNumber = 1;
            int columnNumber = 1;
            
            for (int i = 0; i < json.length(); i++) {
                char c = json.charAt(i);
                
                if (c == '\n') {
                    lineNumber++;
                    columnNumber = 1;
                } else {
                    columnNumber++;
                }
                
                if (escaped) {
                    result.append(c);
                    escaped = false;
                    continue;
                }
                
                if (c == '\\') {
                    escaped = true;
                    result.append(c);
                    continue;
                }
                
                if (c == '"') {
                    inString = !inString;
                    result.append(c);
                    continue;
                }
                
                result.append(c);
            }
            
            // If we ended in a string, close it
            if (inString) {
                result.append('"');
                log.warn("Fixed unclosed string in JSON response at line {}, column {}", lineNumber, columnNumber);
            }
            
            return result.toString();
            
        } catch (Exception e) {
            log.error("Error fixing unclosed strings: {}", e.getMessage());
            return json; // Return original if fixing fails
        }
    }

    /**
     * Data class for voucher suggestion business data
     */
    /**
     * Analyze all reviews in an order with AI
     * 
     * @param orderId ID of the order to analyze reviews for
     * @return ReviewAiAnalysisResponse with sentiment and suggested replies for the entire order
     */
    @Transactional
    public ReviewAiAnalysisResponse analyzeOrderReviews(String orderId) {
        log.info("Analyzing all reviews for order {} with AI", orderId);
        
        try {
            // 1. Get all reviews for the order
            List<Review> reviews = reviewRepository.findByOrderId(orderId);
            
            if (reviews.isEmpty()) {
                throw new IllegalArgumentException("Không tìm thấy đánh giá nào cho đơn hàng: " + orderId);
            }
            
            // 2. Combine all reviews into a single analysis
            StringBuilder combinedComments = new StringBuilder();
            double totalRating = 0;
            int reviewCount = 0;
            
            for (Review review : reviews) {
                if (review.getComment() != null && !review.getComment().trim().isEmpty()) {
                    combinedComments.append("- ").append(review.getComment()).append("\n");
                }
                totalRating += review.getRating() != null ? review.getRating() : 3; // Default to neutral
                reviewCount++;
            }
            
            double averageRating = totalRating / reviewCount;
            int roundedRating = (int) Math.round(averageRating);
            
            // 3. Build prompt and call AI
            String prompt = buildOrderReviewAnalysisPrompt(combinedComments.toString(), roundedRating, reviewCount);
            log.debug("Built prompt for order review AI analysis (length: {} chars)", prompt.length());
            
            // 4. Call Gemini API
            GeminiResponse response = geminiClient.generateContentWithRetry(prompt, 2);
            if (response == null || !response.isSuccessful()) {
                log.warn("Gemini API failed for order {}, using fallback", orderId);
                return createFallbackAnalysis(roundedRating);
            }
            
            // 5. Parse JSON response with robust error handling
            String jsonResponse = response.getTextResponse();
            log.debug("AI response: {}", jsonResponse);
            
            if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
                log.warn("AI returned empty response for order {}, using fallback", orderId);
                return createFallbackAnalysis(roundedRating);
            }
            
            String cleanedJsonResponse = cleanJsonResponse(jsonResponse);
            if (cleanedJsonResponse == null) {
                log.warn("Failed to clean JSON response for order {}, using fallback", orderId);
                return createFallbackAnalysis(roundedRating);
            }
            
            ReviewAiAnalysisResponse aiResponse;
            try {
                aiResponse = objectMapper.readValue(cleanedJsonResponse, ReviewAiAnalysisResponse.class);
            } catch (Exception parseException) {
                log.error("Failed to parse AI response JSON for order {}: {}", orderId, parseException.getMessage());
                log.debug("Raw response that failed to parse: {}", jsonResponse);
                log.debug("Cleaned response that failed to parse: {}", cleanedJsonResponse);
                return createFallbackAnalysis(roundedRating);
            }
            
            // 6. Set sentiment label based on sentiment
            if (aiResponse.getSentiment() != null) {
                aiResponse.setSentimentLabel(getSentimentLabel(aiResponse.getSentiment()));
            } else {
                // If no sentiment from AI, determine from average rating
                String sentiment = roundedRating <= 2 ? "NEGATIVE" : roundedRating >= 4 ? "POSITIVE" : "NEUTRAL";
                aiResponse.setSentiment(sentiment);
                aiResponse.setSentimentLabel(getSentimentLabel(sentiment));
            }
            
            log.info("Successfully analyzed {} reviews for order {} with sentiment: {}", 
                    reviewCount, orderId, aiResponse.getSentiment());
            return aiResponse;
            
        } catch (Exception e) {
            log.error("Error analyzing order reviews for order {}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Có lỗi xảy ra khi phân tích đánh giá đơn hàng: " + e.getMessage(), e);
        }
    }

    /**
     * Build prompt for AI analysis of order reviews
     */
    private String buildOrderReviewAnalysisPrompt(String combinedComments, int averageRating, int reviewCount) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Bạn là chuyên gia CSKH cửa hàng hoa trực tuyến.\n");
        prompt.append("Phân tích các đánh giá sau đây từ cùng một đơn hàng và trả về JSON:\n\n");
        
        prompt.append("{\n");
        prompt.append("  \"sentiment\": \"POSITIVE|NEUTRAL|NEGATIVE\",\n");
        prompt.append("  \"mainIssue\": \"Tóm tắt vấn đề chính (nếu tiêu cực/trung tính)\",\n");
        prompt.append("  \"suggestedReplies\": [\n");
        prompt.append("    {\"label\": \"Lịch sự\", \"content\": \"...\"},\n");
        prompt.append("    {\"label\": \"Kèm đền bù\", \"content\": \"...\"},\n");
        prompt.append("    {\"label\": \"Chuyên nghiệp\", \"content\": \"...\"}\n");
        prompt.append("  ]\n");
        prompt.append("}\n\n");
        
        prompt.append("THÔNG TIN ĐƠN HÀNG:\n");
        prompt.append("- Điểm đánh giá trung bình: ").append(averageRating).append("/5\n");
        prompt.append("- Số lượng đánh giá: ").append(reviewCount).append("\n\n");
        
        prompt.append("NỘI DUNG ĐÁNH GIÁ:\n");
        if (combinedComments.toString().trim().isEmpty()) {
            prompt.append("(Không có bình luận cụ thể)\n\n");
        } else {
            prompt.append(combinedComments).append("\n\n");
        }
        
        prompt.append("QUY TẮC PHÂN TÍCH:\n");
        prompt.append("1. Xác định sentiment chung của tất cả đánh giá:\n");
        prompt.append("   - POSITIVE: đa số đánh giá tốt (4-5 sao)\n");
        prompt.append("   - NEGATIVE: có đánh giá 1-2 sao hoặc nhiều phàn nàn\n");
        prompt.append("   - NEUTRAL: còn lại hoặc ý kiến trái chiều\n");
        prompt.append("2. Tóm tắt vấn đề chính (nếu có) từ tất cả đánh giá\n");
        prompt.append("3. Gợi ý 2-3 mẫu phản hồi phù hợp, mỗi mẫu 50-100 từ\n");
        prompt.append("4. Giọng văn: Tiếng Việt, thân thiện, chuyên nghiệp\n");
        prompt.append("5. Label phải ngắn gọn: \"Lịch sự\", \"Kèm đền bù\", \"Chuyên nghiệp\", \"Xin lỗi chân thành\"\n");
        prompt.append("6. Phản hồi phải phù hợp với việc trả lời cho TẤT CẢ đánh giá trong đơn hàng\n");
        prompt.append("7. Chỉ trả về JSON, không có text khác");
        
        return prompt.toString();
    }

    @lombok.Data
    @lombok.Builder
    private static class VoucherSuggestionData {
        private BigDecimal aov;
        private List<Voucher> activeVouchers;
        private List<Product> inventoryProducts;
    }
}
