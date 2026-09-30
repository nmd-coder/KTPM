-- =============================================
-- ADDITIONAL ORDERS DATA - RECENT ORDERS
-- Thêm nhiều đơn hàng cho ngày hôm qua và hôm nay
-- Format ID: DDMMYY + sequence number (theo OrderIdGeneratorService)
-- =============================================

BEGIN;

-- ===== ĐƠN HÀNG NGÀY HÔM QUA (23/10/2025) =====

-- Order 1: 08:30 - Nguyễn An mua hoa tình yêu
INSERT INTO Orders (
    id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee, notes
)
VALUES (
    '2310251', 1, 970000, 'COMPLETED', '2025-10-23 08:30:00', 1, 'MOMO', 20000, 'Giao sớm cho sinh nhật bạn gái'
);
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('2310251', 1, 1, 450000),
       ('2310251', 2, 1, 520000);

-- Order 2: 09:45 - Tai Nguyen mua hoa trang trí
INSERT INTO Orders (
    id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee, notes
)
VALUES (
    '2310252', 9, 630000, 'COMPLETED', '2025-10-23 09:45:00', 1, 'COD', 30000, 'Trang trí văn phòng'
);
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('2310252', 3, 1, 600000);

-- Order 3: 10:15 - Lê Chi mua lan hồ điệp
INSERT INTO Orders (
    id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee
)
VALUES (
    '2310253', 3, 1230000, 'COMPLETED', '2025-10-23 10:15:00', 3, 'BANK_TRANSFER', 30000
);
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('2310253', 5, 1, 1200000);

-- Order 4: 11:30 - Tai Nguyen mua hoa sinh nhật
INSERT INTO Orders (
    id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee, notes
)
VALUES (
    '2310254', 9, 1000000, 'COMPLETED', '2025-10-23 11:30:00', 1, 'MOMO', 20000, 'Sinh nhật mẹ'
);
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('2310254', 13, 1, 580000),
       ('2310254', 6, 1, 390000);

-- Order 5: 13:20 - Võ Hà mua hoa chúc mừng
INSERT INTO Orders (
    id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee
)
VALUES (
    '2310255', 5, 1650000, 'COMPLETED', '2025-10-23 13:20:00', 5, 'BANK_TRANSFER', 50000
);
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('2310255', 15, 1, 1600000);

-- Order 6: 14:45 - Tai Nguyen mua combo hoa
INSERT INTO Orders (
    id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee, notes
)
VALUES (
    '2310256', 9, 1420000, 'COMPLETED', '2025-10-23 14:45:00', 1, 'COD', 20000, 'Tặng kèm thiệp'
);
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('2310256', 10, 1, 320000),
       ('2310256', 11, 1, 680000),
       ('2310256', 12, 1, 420000);

-- Order 7: 15:30 - Tai Nguyen mua hoa lavender
INSERT INTO Orders (
    id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee
)
VALUES (
    '2310257', 9, 350000, 'COMPLETED', '2025-10-23 15:30:00', 1, 'MOMO', 30000
);
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('2310257', 10, 1, 320000);

-- Order 8: 16:15 - Võ Giang mua hoa cưới
INSERT INTO Orders (
    id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee, notes
)
VALUES (
    '2310258', 11, 880000, 'SHIPPED', '2025-10-23 16:15:00', 1, 'BANK_TRANSFER', 30000, 'Cần giao đúng giờ'
);
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('2310258', 7, 1, 850000);

-- Order 9: 17:00 - Hoàng Hương mua hoa trang trí
INSERT INTO Orders (
    id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee
)
VALUES (
    '2310259', 12, 750000, 'SHIPPED', '2025-10-23 17:00:00', 1, 'COD', 0
);
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('2310259', 4, 1, 750000);

-- Order 10: 18:30 - Đỗ Khánh mua hoa peony
INSERT INTO Orders (
    id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee
)
VALUES (
    '23102510', 13, 710000, 'PROCESSING', '2025-10-23 18:30:00', 1, 'MOMO', 30000
);
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('23102510', 11, 1, 680000);

-- ===== ĐƠN HÀNG NGÀY HÔM NAY (24/10/2025) =====

-- Order 1: 07:30 - Bùi Lan mua hoa sáng sớm
INSERT INTO Orders (
    id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee, notes
)
VALUES (
    '2410251', 14, 470000, 'COMPLETED', '2025-10-24 07:30:00', 1, 'MOMO', 20000, 'Giao trước 9h'
);
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('2410251', 1, 1, 450000);

-- Order 2: 08:15 - Tai Nguyen mua hoa tình yêu
INSERT INTO Orders (
    id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee
)
VALUES (
    '2410252', 9, 540000, 'COMPLETED', '2025-10-24 08:15:00', 1, 'COD', 20000
);
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('2410252', 2, 1, 520000);

-- Order 3: 09:00 - Ngô Nga mua giỏ hoa hướng dương
INSERT INTO Orders (
    id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee, notes
)
VALUES (
    '2410253', 16, 630000, 'COMPLETED', '2025-10-24 09:00:00', 6, 'BANK_TRANSFER', 30000, 'Khách VIP - Ưu tiên'
);
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('2410253', 3, 1, 600000);

-- Order 4: 09:45 - Mai Oanh mua combo hoa sinh nhật
INSERT INTO Orders (
    id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee
)
VALUES (
    '2410254', 17, 1270000, 'COMPLETED', '2025-10-24 09:45:00', 7, 'MOMO', 20000
);
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('2410254', 13, 1, 580000),
       ('2410254', 14, 1, 650000);

-- Order 5: 10:30 - Tai Nguyen mua hoa chúc mừng
INSERT INTO Orders (
    id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee, notes
)
VALUES (
    '2410255', 9, 1650000, 'COMPLETED', '2025-10-24 10:30:00', 1, 'BANK_TRANSFER', 50000, 'Khai trương cửa hàng'
);
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('2410255', 15, 1, 1600000);

-- Order 6: 11:15 - Trịnh Quỳnh mua hoa trang trí
INSERT INTO Orders (
    id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee
)
VALUES (
    '2410256', 22, 750000, 'COMPLETED', '2025-10-24 11:15:00', 1, 'COD', 0
);
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('2410256', 16, 1, 720000);

-- Order 7: 12:00 - Phạm Thảo (VIP) mua hoa cao cấp
INSERT INTO Orders (
    id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee, notes
)
VALUES (
    '2410257', 19, 2050000, 'SHIPPED', '2025-10-24 12:00:00', 9, 'BANK_TRANSFER', 50000, 'Khách VIP - Giao tận nơi'
);
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('2410257', 5, 1, 1200000),
       ('2410257', 7, 1, 850000);

-- Order 8: 13:30 - Hoàng Uyên (VIP) mua hoa premium
INSERT INTO Orders (
    id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee
)
VALUES (
    '2410258', 20, 1880000, 'SHIPPED', '2025-10-24 13:30:00', 10, 'MOMO', 80000
);
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('2410258', 9, 1, 1800000);

-- Order 9: 14:15 - Nguyễn An mua thêm hoa
INSERT INTO Orders (
    id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee, notes
)
VALUES (
    '2410259', 1, 1070000, 'PROCESSING', '2025-10-24 14:15:00', 1, 'MOMO', 20000, 'Giao chiều'
);
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('2410259', 11, 1, 680000),
       ('2410259', 12, 1, 420000);

-- Order 10: 15:00 - Tai Nguyen mua hoa lavender
INSERT INTO Orders (
    id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee
)
VALUES (
    '24102510', 9, 350000, 'PROCESSING', '2025-10-24 15:00:00', 1, 'COD', 30000
);
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('24102510', 10, 1, 320000);

-- Order 11: 15:45 - Lê Chi mua hoa gerbera
INSERT INTO Orders (
    id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee
)
VALUES (
    '24102511', 3, 450000, 'PROCESSING', '2025-10-24 15:45:00', 3, 'BANK_TRANSFER', 30000
);
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('24102511', 12, 1, 420000);

-- Order 12: 16:30 - Tai Nguyen mua hoa tulip
INSERT INTO Orders (
    id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee, notes
)
VALUES (
    '24102512', 9, 780000, 'PENDING', '2025-10-24 16:30:00', 1, 'MOMO', 30000, 'Chờ xác nhận'
);
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('24102512', 4, 1, 750000);

-- Order 13: 17:15 - Võ Hà mua combo hoa
INSERT INTO Orders (
    id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee
)
VALUES (
    '24102513', 5, 1100000, 'PENDING', '2025-10-24 17:15:00', 5, 'COD', 20000
);
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('24102513', 1, 1, 450000),
       ('24102513', 3, 1, 600000);

-- Order 14: 18:00 - Tai Nguyen mua hoa cẩm chướng
INSERT INTO Orders (
    id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee, notes
)
VALUES (
    '24102514', 9, 420000, 'PENDING', '2025-10-24 18:00:00', 1, 'BANK_TRANSFER', 30000, 'Giao tối'
);
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('24102514', 6, 1, 390000);

-- Order 15: 18:45 - Tai Nguyen mua hoa thử
INSERT INTO Orders (
    id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee, notes
)
VALUES (
    '24102515', 9, 470000, 'PENDING', '2025-10-24 18:45:00', 1, 'MOMO', 20000, 'Khách hàng thân thiết'
);
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('24102515', 1, 1, 450000);

COMMIT;

-- =============================================
-- SUMMARY
-- =============================================
-- Đã thêm:
-- - 10 đơn hàng cho ngày 23/10/2025 (hôm qua)
-- - 15 đơn hàng cho ngày 24/10/2025 (hôm nay)
-- - Tổng: 25 đơn hàng mới
-- - Các trạng thái: COMPLETED, SHIPPED, PROCESSING, PENDING
-- - Đa dạng sản phẩm và khách hàng
-- - Bao gồm cả khách VIP và khách thường
-- 
-- ĐƠN HÀNG CỦA TAI NGUYEN (USER ID: 9):
-- - Ngày 23/10: 4 đơn hàng (2310252, 2310254, 2310256, 2310257)
-- - Ngày 24/10: 6 đơn hàng (2410252, 2410255, 24102510, 24102512, 24102514, 24102515)
-- - Tổng: 10 đơn hàng cho Tai Nguyen
-- - Tổng giá trị: ~8,210,000 VND
-- =============================================
