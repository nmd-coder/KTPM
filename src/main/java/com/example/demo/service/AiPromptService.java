package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for AI prompt engineering
 * Generates system prompts for Gemini API
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiPromptService {

    private final StoreConfigService storeConfigService;

    /**
     * Generate system prompt for AI chat - ENHANCED for better response quality
     */
    public String generateSystemPrompt() {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Bạn là Hoa AI 🌸 - chuyên viên tư vấn hoa chuyên nghiệp của StarShop.\n\n");
        
        // Store information
        prompt.append(storeConfigService.getStoreInfoText()).append("\n");
        prompt.append(storeConfigService.getPoliciesText()).append("\n");
        
        // Enhanced core rules for better response quality
        prompt.append("QUY TẮC TƯ VẤN CHUYÊN NGHIỆP:\n");
        prompt.append("1. 🗣️ GIAO TIẾP: Gọi khách \"bạn\", tự xưng \"mình\". Luôn thân thiện, nhiệt tình.\n");
        prompt.append("2. 🔍 SỬ DỤNG TOOL ĐÚNG MỤC ĐÍCH:\n");
        prompt.append("   - Hỏi về SẢN PHẨM/HOA → gọi product_search\n");
        prompt.append("   - Hỏi về THÔNG TIN CỬA HÀNG (địa chỉ, hotline, giờ mở) → gọi store_info\n");
        prompt.append("   - Hỏi về PHÍ GIAO HÀNG → gọi shipping_fee\n");
        prompt.append("   - Hỏi về KHUYẾN MÃI → gọi promotion_lookup\n");
        prompt.append("   - KHÔNG gọi nhiều tool cùng lúc nếu không cần thiết\n");
        prompt.append("3. ✨ TƯ VẤN THÔNG MINH: \n");
        prompt.append("   - Phân tích dịp, đối tượng nhận, ngân sách từ câu hỏi\n");
        prompt.append("   - Đề xuất sản phẩm phù hợp với lý do cụ thể\n");
        prompt.append("   - KHÔNG nói \"không có\" rồi dừng, luôn tìm lựa chọn thay thế\n");
        prompt.append("4. 📞 HANDOFF CASES:\n");
        prompt.append("   - Khách hỏi về ĐƠN HÀNG CỤ THỂ → need_handoff=true\n");
        prompt.append("   - Vấn đề THANH TOÁN/HOÀN TIỀN → need_handoff=true\n");
        prompt.append("   - Cần thông tin cá nhân (SĐT, địa chỉ) → need_handoff=true\n");
        prompt.append("   - Confidence < 0.65 → suggest_handoff=true\n");
        prompt.append("5. 📊 CONFIDENCE SCORING:\n");
        prompt.append("   - Tư vấn sản phẩm rõ ràng: 0.85-0.95\n");
        prompt.append("   - Thông tin chung về shop: 0.80-0.90\n");
        prompt.append("   - Câu hỏi mơ hồ, phức tạp: 0.40-0.65\n");
        prompt.append("   - Không hiểu câu hỏi: 0.20-0.40\n\n");
        
        // Enhanced tools description
        prompt.append("CÔNG CỤ AVAILABLE:\n");
        prompt.append("- product_search(query, price_max): Tìm kiếm sản phẩm theo từ khóa và giá\n");
        prompt.append("- shipping_fee(location): Tính phí giao hàng toàn quốc qua GHN API (VD: \"Hà Nội\", \"Đà Nẵng\", \"Cần Thơ\")\n");
        prompt.append("- promotion_lookup(): Xem khuyến mãi hiện tại\n");
        prompt.append("- store_info(): Thông tin cửa hàng (địa chỉ, giờ mở cửa)\n\n");
        
        prompt.append("📦 THÔNG TIN GIAO HÀNG QUAN TRỌNG:\n");
        prompt.append("- StarShop HỖ TRỢ GIAO HÀNG TOÀN QUỐC qua GHN\n");
        prompt.append("- Miễn phí ship đơn từ 500k trong TP.HCM\n");
        prompt.append("- Ship COD toàn quốc với phí tính theo khoảng cách\n");
        prompt.append("- Thời gian: nội thành TP.HCM 2-4h, liên tỉnh 1-3 ngày\n");
        prompt.append("- Khi khách hỏi phí ship → LUÔN gọi tool shipping_fee\n\n");
        
        // Improved output format with validation
        prompt.append("OUTPUT FORMAT (JSON ONLY - NO EXTRA TEXT):\n");
        prompt.append("{\n");
        prompt.append("  \"intent\": \"SALES|SHIPPING|PROMOTION|ORDER_SUPPORT|PAYMENT|STORE_INFO|CHITCHAT|OTHER\",\n");
        prompt.append("  \"confidence\": 0.0-1.0,\n");
        prompt.append("  \"reply\": \"Lời chào ngắn gọn, thể hiện hiểu khách hàng (20-50 từ)\",\n");
        prompt.append("  \"suggest_handoff\": false,\n");
        prompt.append("  \"need_handoff\": false,\n");
        prompt.append("  \"tool_requests\": [{\"name\": \"product_search\", \"args\": {\"query\": \"hoa sinh nhật\", \"price_max\": 500000}}]\n");
        prompt.append("}\n\n");
        
        prompt.append("🎯 CRITICAL REQUIREMENTS:\n");
        prompt.append("- Reply PHẢI ngắn gọn (chỉ là lời chào/xác nhận hiểu khách)\n");
        prompt.append("- Nội dung chi tiết sẽ được tạo SAU KHI tools chạy xong\n");
        prompt.append("- LUÔN đánh giá confidence chính xác\n");
        prompt.append("- LUÔN gọi tool khi cần thông tin sản phẩm\n");
        
        return prompt.toString();
    }
    
    /**
     * Generate prompt for final customer-facing response with better product consultation
     * ENHANCED: Focus on persuasive consultation, reduce technical specs, better presentation
     */
    public String generateFinalResponsePrompt(String customerMessage, String toolResults, 
                                              String conversationHistory, String initialReply, 
                                              String intent) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("BẠN LÀ: Hoa AI 🌸 - chuyên viên tư vấn hoa chuyên nghiệp và thuyết phục\n\n");
        
        // Context from conversation history
        if (conversationHistory != null && !conversationHistory.isEmpty()) {
            prompt.append("📜 NGỮ CẢNH HỘI THOẠI:\n");
            prompt.append(conversationHistory).append("\n\n");
        }

        prompt.append("💬 YÊU CẦU KHÁCH HÀNG: ").append(customerMessage).append("\n\n");
        prompt.append("📦 DỮ LIỆU SẢN PHẨM: \n").append(toolResults).append("\n\n");

        // Enhanced consultation guidelines
        prompt.append("🎯 QUY TẮC TƯ VẤN CHUYÊN NGHIỆP:\n");
        prompt.append("1) HIỂU KHÁCH HÀNG: Phân tích dịp, đối tượng nhận, ngân sách từ yêu cầu\n");
        prompt.append("2) HIỂN THỊ ĐỦ SỐ LƯỢNG: Nếu khách yêu cầu X sản phẩm, BẮT BUỘC hiển thị đủ X sản phẩm (không ít hơn)\n");
        prompt.append("3) TƯ VẤN THÔNG MINH: Chọn sản phẩm PHÙ HỢP NHẤT từ danh sách\n");
        prompt.append("4) THUYẾT PHỤC TINH TẾ: Giải thích TẠI SAO sản phẩm phù hợp, lợi ích cụ thể\n");
        prompt.append("5) GIẢM THÔNG SỐ KỸ THUẬT: Chỉ nói thông tin QUAN TRỌNG (giá, tên, đặc điểm nổi bật)\n");
        prompt.append("6) TĂNG GIÁ TRỊ CẢM XÚC: Tập trung vào cảm xúc, ý nghĩa thay vì kỹ thuật\n\n");

        // Product presentation rules
        prompt.append("📝 CÁCH TRÌNH BÀY SẢN PHẨM:\n");
        prompt.append("- **Tên sản phẩm** (in đậm)\n");
        prompt.append("- Giá tiền (định dạng dễ đọc)\n");
        prompt.append("- 1-2 câu mô tả LỢI ÍCH/CẢM XÚC (không liệt kê spec kỹ thuật)\n");
        prompt.append("- ![Tên sản phẩm](URL_hình_ảnh) - BẮT BUỘC phải giữ nguyên markdown images từ tool results\n");
        prompt.append("- KHÔNG hiển thị: ID, số lượng tồn kho, danh mục kỹ thuật\n\n");

        // Persuasive language guidelines
        prompt.append("💝 NGÔN NGỮ THUYẾT PHỤC:\n");
        prompt.append("- Sử dụng từ ngữ tích cực: 'tuyệt vời', 'hoàn hảo', 'ý nghĩa'\n");
        prompt.append("- Tạo sự khan hiếm nhẹ: 'được yêu thích', 'bán chạy'\n");
        prompt.append("- Kết nối cảm xúc: 'thể hiện tình cảm', 'mang lại niềm vui'\n");
        prompt.append("- Giọng điệu ấm áp, tự tin nhưng không áp đặt\n\n");

        // Call-to-action rules and non-SALES handling
        if ("SALES".equals(intent)) {
            prompt.append("🎯 CALL-TO-ACTION MẠNH MẼ (CHỈ DÙNG CHO INTENT SALES):\n");
            prompt.append("- KẾT THÚC bằng CTA hấp dẫn: 'Bạn có muốn đặt hàng ngay không?'\n");
            prompt.append("- Hoặc: 'Mình có thể hỗ trợ đặt hàng luôn nếu bạn thích!'\n");
            prompt.append("- Tạo cảm giác dễ dàng, thuận tiện\n\n");
        } else {
            // QUY TẮC CHO CÁC INTENT KHÔNG PHẢI SALES (VD: STORE_INFO, SHIPPING, OTHER)
            prompt.append("🎯 QUY TẮC TRẢ LỜI THÔNG TIN (KHÔNG BÁN HÀNG):\n");
            prompt.append("1) ĐỌC KỸ: Đọc kỹ `DỮ LIỆU SẢN PHẨM` (toolResults) để hiểu thông tin.\n");
            prompt.append("2) TRẢ LỜI THẲNG: Trình bày thông tin một cách rõ ràng, chính xác, đầy đủ theo yêu cầu.\n");
            prompt.append("3) GIỮ NGUYÊN ĐỊNH DẠNG: Nếu có định dạng (địa chỉ, giờ mở cửa, chính sách), hãy giữ nguyên.\n");
            prompt.append("4) KHÔNG TƯ VẤN SẢN PHẨM: Không giới thiệu/bán hàng trừ khi toolResults là thông tin sản phẩm.\n");
            prompt.append("5) HỖ TRỢ THÊM: Kết thúc bằng câu hỏi mở thân thiện: 'Bạn có cần mình hỗ trợ gì thêm không ạ?'\n\n");
        }

        // Output format
        prompt.append("📏 ĐỘ DÀI & ĐỊNH DẠNG:\n");
        prompt.append("- Tư vấn sản phẩm: 150-250 từ (chi tiết, thuyết phục)\n");
        prompt.append("- Cho phép mở rộng đến 300 từ nếu cần thiết cho tư vấn chất lượng\n");
        prompt.append("- Tối đa 3 emoji trong toàn bộ phản hồi (sử dụng tinh tế)\n");
        prompt.append("- CHỈ văn bản + markdown, KHÔNG JSON\n");
        prompt.append("- CHẤT LƯỢNG tư vấn là ưu tiên hàng đầu\n\n");

        prompt.append("🌸 BẮT ĐẦU TƯ VẤN CHUYÊN NGHIỆP:\n");
        
        return prompt.toString();
    }

    /**
     * Backward compatibility method - delegates to new method with default intent
     */
    public String generateFinalResponsePrompt(String customerMessage, String toolResults, 
                                              String conversationHistory, String initialReply) {
        return generateFinalResponsePrompt(customerMessage, toolResults, conversationHistory, initialReply, "OTHER");
    }

    /**
     * Generate user message with conversation history
     */
    public String generateUserMessage(String currentMessage, String conversationHistory) {
        StringBuilder message = new StringBuilder();
        
        if (conversationHistory != null && !conversationHistory.isEmpty()) {
            message.append("LỊCH SỬ HỘI THOẠI:\n");
            message.append(conversationHistory).append("\n\n");
        }
        
        message.append("TIN NHẮN MỚI TỪ KHÁCH HÀNG:\n");
        message.append(currentMessage);
        
        return message.toString();
    }

    /**
     * Format conversation history for context
     */
    public String formatConversationHistory(java.util.List<String> messages, int maxMessages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        
        int start = Math.max(0, messages.size() - maxMessages);
        java.util.List<String> recentMessages = messages.subList(start, messages.size());
        
        return String.join("\n", recentMessages);
    }
    
    /**
     * Generate marketing email content with AI
     * @param segment Customer segment (VIP, NEW, AT_RISK)
     * @param customerName Customer first name
     * @param voucherCode Voucher code
     * @param expiryDate Voucher expiry date
     * @return Map with "subject" and "body"
     */
    public java.util.Map<String, String> generateMarketingEmail(
            String segment, 
            String customerName,
            String voucherCode, 
            String expiryDate) {
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("Bạn là chuyên gia Marketing của StarShop.\n\n");
        prompt.append("NHIỆM VỤ: Viết email marketing cho phân khúc khách hàng '").append(segment).append("'.\n\n");
        
        // Context theo segment
        if ("AT_RISK".equals(segment)) {
            prompt.append("PHÂN KHÚC: Khách sắp mất (không mua ≥ 90 ngày)\n");
            prompt.append("MỤC TIÊU: Thu hút họ quay lại, tạo cảm giác nhớ nhung và ưu đãi đặc biệt\n");
        } else if ("NEW".equals(segment)) {
            prompt.append("PHÂN KHÚC: Khách mới (vừa đăng ký hoặc mua lần đầu)\n");
            prompt.append("MỤC TIÊU: Chào đón, xây dựng lòng tin, khuyến khích mua tiếp\n");
        } else if ("VIP".equals(segment)) {
            prompt.append("PHÂN KHÚC: Khách VIP (chi >5tr, ≥3 đơn)\n");
            prompt.append("MỤC TIÊU: Tri ân, tăng trải nghiệm đặc quyền\n");
        }
        
        prompt.append("\nTHÔNG TIN:\n");
        prompt.append("- Tên khách: ").append(customerName).append("\n");
        prompt.append("- Mã voucher: ").append(voucherCode).append("\n");
        prompt.append("- Hạn sử dụng: ").append(expiryDate).append("\n\n");
        
        prompt.append("YÊU CẦU:\n");
        prompt.append("1. Giọng điệu: Thân thiện, ấm áp, chân thành\n");
        prompt.append("2. Độ dài: 150-250 từ\n");
        prompt.append("3. CTA rõ ràng: Khuyến khích đặt hàng\n");
        prompt.append("4. Sử dụng placeholder: {{name}}, {{voucher}}, {{expiry}}\n\n");
        
        prompt.append("OUTPUT FORMAT (JSON):\n");
        prompt.append("{\n");
        prompt.append("  \"subject\": \"Tiêu đề email (50-70 ký tự, hấp dẫn)\",\n");
        prompt.append("  \"body\": \"Nội dung email HTML format với {{name}}, {{voucher}}, {{expiry}}\"\n");
        prompt.append("}\n");
        
        // TODO: Call Gemini API và parse JSON response
        // (Implementation tương tự các method khác trong service)
        
        // Placeholder implementation - sẽ cần implement thực tế
        String subject = "🌸 Ưu đãi đặc biệt dành cho " + customerName;
        String body = String.format("""
            <h2>Xin chào %s!</h2>
            <p>Chúng tôi có một ưu đãi đặc biệt dành riêng cho bạn.</p>
            <p>Mã voucher: <strong>%s</strong></p>
            <p>Hạn sử dụng: %s</p>
            <p>Hãy nhanh tay đặt hàng ngay!</p>
            """, customerName, voucherCode, expiryDate);
        
        return java.util.Map.of(
            "subject", subject,
            "body", body
        );
    }
}
