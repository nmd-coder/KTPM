-- =============================================
-- SEED DATA FOR REVIEWS - FLOWER STORE MANAGEMENT SYSTEM
-- Phiên bản: PostgreSQL
-- Mục tiêu: Tạo dữ liệu review từ các đơn hàng đã COMPLETED
-- Logic: Chỉ review được sản phẩm đã mua (order_item_id)
-- =============================================

BEGIN;

-- =============================================
-- REVIEWS FROM COMPLETED ORDERS
-- Đảm bảo user_id khớp với người đã đặt hàng
-- order_item_id UNIQUE - mỗi OrderItem chỉ review 1 lần
-- =============================================

-- ===== REVIEWS CHO ORDER 0108251 (User 1) =====
-- OrderItem 1: Product 1 (Bó hồng đỏ Classic)
INSERT INTO Reviews (
    user_id, product_id, rating, comment, 
    order_item_id, sentiment, created_at, updated_at
) VALUES (
    1, 1, 5, 
    'Hoa đẹp lắm! Bó hồng đỏ tươi tắn, giao đúng giờ, đóng gói rất cẩn thận. Vợ mình rất thích, sẽ ủng hộ shop lâu dài ❤️',
    1, 'POSITIVE', 
    '2025-08-02 14:30:00', '2025-08-02 14:30:00'
);

-- ===== REVIEWS CHO ORDER 0508252 (User 3) =====
-- OrderItem 2: Product 5 (Lan hồ điệp 3 cành)
INSERT INTO Reviews (
    user_id, product_id, rating, comment, 
    order_item_id, sentiment, created_at, updated_at
) VALUES (
    3, 5, 5, 
    'Chậu lan sang trọng quá! Đặt tặng sếp nhân dịp khai trương, sếp khen ngợi hết lời. Chất lượng 10 điểm, giá hợp lý.',
    2, 'POSITIVE', 
    '2025-08-06 10:15:00', '2025-08-06 10:15:00'
);

-- ===== REVIEWS CHO ORDER 0209254 (User 2) =====
-- OrderItem 4: Product 2 (Bó hồng phấn Sweetie)
INSERT INTO Reviews (
    user_id, product_id, rating, comment, 
    order_item_id, sentiment, created_at, updated_at
) VALUES (
    2, 2, 4, 
    'Hoa đẹp, màu hồng phấn dịu dàng như mô tả. Tuy nhiên giao hơi chậm so với dự kiến khoảng 30 phút. Nhưng nhìn chung vẫn hài lòng.',
    4, 'POSITIVE', 
    '2025-09-03 16:20:00', '2025-09-03 16:20:00'
);

-- OrderItem 5: Product 3 (Giỏ hoa hướng dương)
INSERT INTO Reviews (
    user_id, product_id, rating, comment, 
    order_item_id, sentiment, created_at, updated_at
) VALUES (
    2, 3, 5, 
    'Giỏ hướng dương tươi rói, màu vàng rực rỡ mang lại năng lượng tích cực. Tặng bạn sinh nhật, bạn rất thích. Sẽ quay lại ủng hộ!',
    5, 'POSITIVE', 
    '2025-09-03 16:25:00', '2025-09-03 16:25:00'
);

-- ===== REVIEWS CHO ORDER 1509255 (User 7 - STAFF) =====
-- OrderItem 6: Product 9 (Hoa mừng khai trương)
INSERT INTO Reviews (
    user_id, product_id, rating, comment, 
    order_item_id, sentiment, created_at, updated_at
) VALUES (
    7, 9, 5, 
    'Kệ hoa khai trương rất hoành tráng và đẹp mắt! Đặt tặng bạn mở quán cà phê, ai cũng khen. Thiết kế chuyên nghiệp, xứng đáng 5 sao.',
    6, 'POSITIVE', 
    '2025-09-16 09:30:00', '2025-09-16 09:30:00'
);

-- ===== REVIEWS CHO ORDER 2009256 (User 8 - ADMIN) =====
-- OrderItem 7: Product 4 (Bình tulip Hà Lan)
INSERT INTO Reviews (
    user_id, product_id, rating, comment, 
    order_item_id, sentiment, created_at, updated_at
) VALUES (
    8, 4, 5, 
    'Tulip Hà Lan đẹp xuất sắc! Màu sắc tươi sáng, bó hoa phối nơ rất tinh tế. Đặt trang trí văn phòng, không gian thêm sang trọng.',
    7, 'POSITIVE', 
    '2025-09-21 11:00:00', '2025-09-21 11:00:00'
);

-- ===== REVIEWS CHO ORDER 2809257 (User 1) =====
-- OrderItem 8: Product 7 (Hoa cưới)
INSERT INTO Reviews (
    user_id, product_id, rating, comment, 
    order_item_id, sentiment, created_at, updated_at
) VALUES (
    1, 7, 5, 
    'Bó hoa cưới tone trắng kem tuyệt đẹp! Đúng như mơ ước của em gái mình. Cảm ơn shop đã tạo nên khoảnh khắc đáng nhớ trong ngày trọng đại 💐',
    8, 'POSITIVE', 
    '2025-09-29 15:45:00', '2025-09-29 15:45:00'
);

-- ===== REVIEWS CHO ORDER 0110258 (User 10) =====
-- OrderItem 9: Product 8 (Vòng hoa chia buồn)
INSERT INTO Reviews (
    user_id, product_id, rating, comment, 
    order_item_id, sentiment, created_at, updated_at
) VALUES (
    10, 8, 5, 
    'Vòng hoa trang trọng, lịch sự, thể hiện sự tôn trọng. Shop giao đúng giờ, hỗ trợ nhiệt tình trong lúc gia đình khó khăn. Cảm ơn rất nhiều.',
    9, 'POSITIVE', 
    '2025-10-02 08:20:00', '2025-10-02 08:20:00'
);

-- ===== REVIEWS CHO ORDER 0510259 (User 12) =====
-- OrderItem 10: Product 1 (Bó hồng đỏ Classic)
INSERT INTO Reviews (
    user_id, product_id, rating, comment, 
    order_item_id, sentiment, created_at, updated_at
) VALUES (
    12, 1, 4, 
    'Hoa đẹp, tươi, nhưng có 1-2 bông hơi nhỏ hơn mong đợi. Tuy nhiên nhìn chung vẫn ok, bạn gái vẫn thích. Giá hợp lý.',
    10, 'POSITIVE', 
    '2025-10-06 18:30:00', '2025-10-06 18:30:00'
);

-- OrderItem 11: Product 2 (Bó hồng phấn Sweetie)
INSERT INTO Reviews (
    user_id, product_id, rating, comment, 
    order_item_id, sentiment, created_at, updated_at
) VALUES (
    12, 2, 5, 
    'Hồng phấn baby xinh xắn lắm! Màu sắc dịu dàng, hương thơm nhẹ nhàng. Đóng gói đẹp, kèm thiệp viết tay rất chu đáo 🌸',
    11, 'POSITIVE', 
    '2025-10-06 18:35:00', '2025-10-06 18:35:00'
);

-- =============================================
-- THÊM MỘT SỐ REVIEWS KHÔNG CÓ order_item_id
-- (Có thể là reviews cũ từ hệ thống trước, hoặc reviews từ mua tại shop)
-- =============================================

-- Review từ User 3 cho Product 6 (Bó cẩm chướng Pastel)
INSERT INTO Reviews (
    user_id, product_id, rating, comment, 
    order_item_id, sentiment, created_at, updated_at
) VALUES (
    3, 6, 5, 
    'Cẩm chướng tone pastel nhẹ nhẹ, rất hợp để tặng mẹ. Mẹ mình rất thích, nói hoa đẹp và thơm. Sẽ đặt thêm dịp 20/10!',
    NULL, 'POSITIVE', 
    '2025-09-10 14:00:00', '2025-09-10 14:00:00'
);

-- Review từ User 4 cho Product 10 (Hoa Lavender)
INSERT INTO Reviews (
    user_id, product_id, rating, comment, 
    order_item_id, sentiment, created_at, updated_at
) VALUES (
    4, 10, 4, 
    'Lavender thơm dịu nhẹ, để phòng ngủ rất thư giãn. Tuy nhiên hoa hơi ít so với giá tiền. Nhưng chất lượng tốt.',
    NULL, 'POSITIVE', 
    '2025-09-18 20:15:00', '2025-09-18 20:15:00'
);

-- Review từ User 5 cho Product 11 (Hoa Peony Hồng)
INSERT INTO Reviews (
    user_id, product_id, rating, comment, 
    order_item_id, sentiment, created_at, updated_at
) VALUES (
    5, 11, 3, 
    'Hoa peony đẹp nhưng không tươi bằng hình trên web. Có vài bông hơi héo. Giá hơi cao so với chất lượng nhận được.',
    NULL, 'NEUTRAL', 
    '2025-09-25 11:30:00', '2025-09-25 11:30:00'
);

-- Review từ User 1 cho Product 12 (Hoa Gerbera Cam)
INSERT INTO Reviews (
    user_id, product_id, rating, comment, 
    order_item_id, sentiment, created_at, updated_at
) VALUES (
    1, 12, 5, 
    'Gerbera cam rực rỡ, mang lại năng lượng tích cực cho cả nhà! Đặt trang trí phòng khách, ai vào cũng khen đẹp. Giá tốt!',
    NULL, 'POSITIVE', 
    '2025-10-08 16:00:00', '2025-10-08 16:00:00'
);

-- Review từ User 2 cho Product 13 (Bó hoa Hạnh Phúc)
INSERT INTO Reviews (
    user_id, product_id, rating, comment, 
    order_item_id, sentiment, created_at, updated_at
) VALUES (
    2, 13, 5, 
    'Bó hoa sinh nhật tone rực rỡ, phối màu đẹp mắt! Tặng em gái sinh nhật, em rất vui. Shop tư vấn nhiệt tình, giao nhanh.',
    NULL, 'POSITIVE', 
    '2025-10-12 13:45:00', '2025-10-12 13:45:00'
);

-- Review từ User 11 cho Product 14 (Giỏ hoa Mặt Trời Nhỏ)
INSERT INTO Reviews (
    user_id, product_id, rating, comment, 
    order_item_id, sentiment, created_at, updated_at
) VALUES (
    11, 14, 4, 
    'Giỏ hoa hướng dương và cúc trắng đẹp, tươi. Giao đúng hẹn. Chỉ có điều giỏ hơi nhỏ so với tưởng tượng. Nhưng vẫn ổn.',
    NULL, 'POSITIVE', 
    '2025-10-15 10:20:00', '2025-10-15 10:20:00'
);

-- Review từ User 13 cho Product 15 (Kệ hoa Thành Công)
INSERT INTO Reviews (
    user_id, product_id, rating, comment, 
    order_item_id, sentiment, created_at, updated_at
) VALUES (
    13, 15, 5, 
    'Kệ hoa chúc mừng thăng chức rất sang trọng và bắt mắt! Đồng nghiệp rất thích, đặt ngay góc văn phòng. Thiết kế chuyên nghiệp!',
    NULL, 'POSITIVE', 
    '2025-10-17 14:00:00', '2025-10-17 14:00:00'
);

-- Review từ User 14 cho Product 16 (Bình hoa Tươi Sáng)
INSERT INTO Reviews (
    user_id, product_id, rating, comment, 
    order_item_id, sentiment, created_at, updated_at
) VALUES (
    14, 16, 2, 
    'Hoa không được tươi lắm, có mấy bông tulip bị héo. Giao trễ hơn 1 tiếng so với hẹn. Hơi thất vọng với lần đặt này.',
    NULL, 'NEGATIVE', 
    '2025-10-19 17:30:00', '2025-10-19 17:30:00'
);

-- =============================================
-- ADMIN RESPONSES
-- Admin (user_id = 8) phản hồi một số reviews
-- =============================================

-- Response cho review rating 5 đầu tiên (order_item_id = 1)
UPDATE Reviews 
SET admin_response = 'Cảm ơn anh/chị đã tin tưởng và ủng hộ StarShop! Rất vui vì anh/chị hài lòng với sản phẩm. Chúc anh/chị và gia đình luôn hạnh phúc! ❤️🌹',
    admin_response_at = '2025-08-02 16:00:00',
    admin_response_by = 8
WHERE order_item_id = 1;

-- Response cho review rating 4 (review về giao chậm - review id sẽ là 3)
UPDATE Reviews 
SET admin_response = 'Xin lỗi anh/chị về sự chậm trễ trong lần giao hàng này. StarShop sẽ cải thiện quy trình giao hàng để phục vụ anh/chị tốt hơn. Cảm ơn anh/chị đã góp ý! 🙏',
    admin_response_at = '2025-09-04 09:00:00',
    admin_response_by = 8
WHERE order_item_id = 4;

-- Response cho review rating 5 về hoa cưới (order_item_id = 8)
UPDATE Reviews 
SET admin_response = 'Cảm ơn anh/chị đã cho StarShop cơ hội được đồng hành trong ngày trọng đại! Chúc cô dâu chú rể trăm năm hạnh phúc! 💐💑',
    admin_response_at = '2025-09-29 17:00:00',
    admin_response_by = 8
WHERE order_item_id = 8;

-- Response cho review rating 5 về vòng hoa chia buồn (order_item_id = 9)
UPDATE Reviews 
SET admin_response = 'StarShop xin chia buồn cùng gia đình anh/chị. Cảm ơn anh/chị đã tin tưởng trong lúc khó khăn. Chúc gia đình sớm vượt qua nỗi đau. 🙏',
    admin_response_at = '2025-10-02 10:00:00',
    admin_response_by = 8
WHERE order_item_id = 9;

-- Response cho review rating 3 (neutral) - User 5, Product 11
UPDATE Reviews 
SET admin_response = 'Xin lỗi anh/chị về trải nghiệm chưa được như mong đợi. StarShop sẽ kiểm tra kỹ hơn chất lượng hoa trước khi giao. Mong anh/chị cho shop cơ hội phục vụ lại! 🌸',
    admin_response_at = '2025-09-25 14:00:00',
    admin_response_by = 8
WHERE user_id = 5 AND product_id = 11;

-- Response cho review rating 2 (negative) - User 14, Product 16
UPDATE Reviews 
SET admin_response = 'StarShop chân thành xin lỗi anh/chị về trải nghiệm không tốt này. Chúng tôi đã ghi nhận và sẽ cải thiện nghiêm túc. Xin liên hệ hotline 1900 xxxx để được hỗ trợ hoàn tiền/đổi hoa. 🙏',
    admin_response_at = '2025-10-19 19:00:00',
    admin_response_by = 8
WHERE user_id = 14 AND product_id = 16;

COMMIT;

-- =============================================
-- SUMMARY
-- =============================================
-- Total Reviews: 18
-- - Reviews with order_item_id: 10 (từ đơn hàng đã mua)
-- - Reviews without order_item_id: 8 (reviews cũ/mua tại shop)
-- Rating Distribution:
-- - 5 stars: 13 reviews (72%)
-- - 4 stars: 3 reviews (17%)
-- - 3 stars: 1 review (5%)
-- - 2 stars: 1 review (6%)
-- Sentiment:
-- - POSITIVE: 16 reviews
-- - NEUTRAL: 1 review
-- - NEGATIVE: 1 review
-- Admin Responses: 6 reviews
-- =============================================
