-- =============================================
-- SEED DATA (VIETNAMESE) - FLOWER STORE MANAGEMENT SYSTEM
-- Phiên bản: PostgreSQL - Updated for GHN Integration
-- Mục tiêu: Tạo dữ liệu mẫu phù hợp với schema hiện tại
-- Bao gồm: GHN fields, BCrypt passwords, shipping dimensions, AI config
-- =============================================
BEGIN;
-- ========== 1) USERS ==========
-- 5 khách hàng, 2 nhân viên, 1 admin với BCrypt passwords
INSERT INTO Users(
        firstname,
        lastname,
        email,
        password,
        phone,
        avatar,
        role,
        created_at
    )
VALUES (
        'Nguyễn',
        'An',
        'an.nguyen@example.com',
        '$2a$10$agFzx5FVfCDVt1rdzQTdcOV4Hy6I/4q9SiQRPbvh9r5umm4EOKuPC',
        '0901000001',
        NULL,
        'CUSTOMER',
        NOW() - INTERVAL '60 days'
    ),
    (
        'Trần',
        'Bình',
        'binh.tran@example.com',
        '$2a$10$agFzx5FVfCDVt1rdzQTdcOV4Hy6I/4q9SiQRPbvh9r5umm4EOKuPC',
        '0901000002',
        NULL,
        'CUSTOMER',
        NOW() - INTERVAL '58 days'
    ),
    (
        'Lê',
        'Chi',
        'chi.le@example.com',
        '$2a$10$agFzx5FVfCDVt1rdzQTdcOV4Hy6I/4q9SiQRPbvh9r5umm4EOKuPC',
        '0901000003',
        NULL,
        'CUSTOMER',
        NOW() - INTERVAL '40 days'
    ),
    (
        'Phạm',
        'Dũng',
        'dung.pham@example.com',
        '$2a$10$agFzx5FVfCDVt1rdzQTdcOV4Hy6I/4q9SiQRPbvh9r5umm4EOKuPC',
        '0901000004',
        NULL,
        'CUSTOMER',
        NOW() - INTERVAL '35 days'
    ),
    (
        'Võ',
        'Hà',
        'ha.vo@example.com',
        '$2a$10$agFzx5FVfCDVt1rdzQTdcOV4Hy6I/4q9SiQRPbvh9r5umm4EOKuPC',
        '0901000005',
        NULL,
        'CUSTOMER',
        NOW() - INTERVAL '30 days'
    ),
    (
        'Hoàng',
        'Lan',
        'lan.hoang@example.com',
        '$2a$10$agFzx5FVfCDVt1rdzQTdcOV4Hy6I/4q9SiQRPbvh9r5umm4EOKuPC',
        '0901000006',
        NULL,
        'STAFF',
        NOW() - INTERVAL '90 days'
    ),
    (
        'Đỗ',
        'Minh',
        'minh.do@example.com',
        '$2a$10$agFzx5FVfCDVt1rdzQTdcOV4Hy6I/4q9SiQRPbvh9r5umm4EOKuPC',
        '0901000007',
        NULL,
        'STAFF',
        NOW() - INTERVAL '90 days'
    ),
    (
        'Admin',
        'Root',
        'admin@example.com',
        '$2a$10$agFzx5FVfCDVt1rdzQTdcOV4Hy6I/4q9SiQRPbvh9r5umm4EOKuPC',
        '0901000008',
        NULL,
        'ADMIN',
        NOW() - INTERVAL '100 days'
    ),
    (
        'Tai',
        'Nguyen',
        'tai@gmail.com',
        '$2a$10$agFzx5FVfCDVt1rdzQTdcOV4Hy6I/4q9SiQRPbvh9r5umm4EOKuPC',
        '0901000009',
        NULL,
        'CUSTOMER',
        NOW() - INTERVAL '1 days'
    ),
    (
        'Phạm',
        'Dũng',
        'dung.pham@email.com',
        '$2a$10$agFzx5FVfCDVt1rdzQTdcOV4Hy6I/4q9SiQRPbvh9r5umm4EOKuPC',
        '0912000006',
        NULL,
        'CUSTOMER',
        NOW() - INTERVAL '55 days'
    ),
    (
        'Võ',
        'Giang',
        'giang.vo@provider.net',
        '$2a$10$agFzx5FVfCDVt1rdzQTdcOV4Hy6I/4q9SiQRPbvh9r5umm4EOKuPC',
        '0912000007',
        NULL,
        'CUSTOMER',
        NOW() - INTERVAL '50 days'
    ),
    (
        'Hoàng',
        'Hương',
        'huong.hoang@domain.org',
        '$2a$10$agFzx5FVfCDVt1rdzQTdcOV4Hy6I/4q9SiQRPbvh9r5umm4EOKuPC',
        '0912000008',
        NULL,
        'CUSTOMER',
        NOW() - INTERVAL '45 days'
    ),
    (
        'Đỗ',
        'Khánh',
        'khanh.do@mail.vn',
        '$2a$10$agFzx5FVfCDVt1rdzQTdcOV4Hy6I/4q9SiQRPbvh9r5umm4EOKuPC',
        '0912000009',
        NULL,
        'CUSTOMER',
        NOW() - INTERVAL '40 days'
    ),
    (
        'Bùi',
        'Lan',
        'lan.bui@email.com',
        '$2a$10$agFzx5FVfCDVt1rdzQTdcOV4Hy6I/4q9SiQRPbvh9r5umm4EOKuPC',
        '0912000010',
        NULL,
        'CUSTOMER',
        NOW() - INTERVAL '35 days'
    ),
    (
        'Dương',
        'Minh',
        'minh.duong@provider.net',
        '$2a$10$agFzx5FVfCDVt1rdzQTdcOV4Hy6I/4q9SiQRPbvh9r5umm4EOKuPC',
        '0912000011',
        NULL,
        'CUSTOMER',
        NOW() - INTERVAL '30 days'
    ),
    (
        'Ngô',
        'Nga',
        'nga.ngo@domain.org',
        '$2a$10$agFzx5FVfCDVt1rdzQTdcOV4Hy6I/4q9SiQRPbvh9r5umm4EOKuPC',
        '0912000012',
        NULL,
        'CUSTOMER',
        NOW() - INTERVAL '25 days'
    ),
    (
        'Mai',
        'Oanh',
        'oanh.mai@mail.vn',
        '$2a$10$agFzx5FVfCDVt1rdzQTdcOV4Hy6I/4q9SiQRPbvh9r5umm4EOKuPC',
        '0912000013',
        NULL,
        'CUSTOMER',
        NOW() - INTERVAL '20 days'
    ),
    -- AT_RISK Customers (Khách hàng sắp mất - có đơn hàng nhưng không mua >= 90 ngày)
    (
        'Nguyễn',
        'Tony',
        'ngtony1025@gmail.com',
        '$2a$10$agFzx5FVfCDVt1rdzQTdcOV4Hy6I/4q9SiQRPbvh9r5umm4EOKuPC',
        '0913000001',
        NULL,
        'CUSTOMER',
        NOW() - INTERVAL '150 days'
    ),
    (
        'Trần',
        'Quỳnh',
        'nghuynhat077@gmail.com',
        '$2a$10$agFzx5FVfCDVt1rdzQTdcOV4Hy6I/4q9SiQRPbvh9r5umm4EOKuPC',
        '0913000002',
        NULL,
        'CUSTOMER',
        NOW() - INTERVAL '120 days'
    ),
    (
        'Lê',
        'Sơn',
        'son.le@inactive.net',
        '$2a$10$agFzx5FVfCDVt1rdzQTdcOV4Hy6I/4q9SiQRPbvh9r5umm4EOKuPC',
        '0913000003',
        NULL,
        'CUSTOMER',
        NOW() - INTERVAL '110 days'
    ),
    (
        'Lý',
        'Phúc',
        'phuc.ly@email.com',
        '$2a$10$agFzx5FVfCDVt1rdzQTdcOV4Hy6I/4q9SiQRPbvh9r5umm4EOKuPC',
        '0912000014',
        NULL,
        'CUSTOMER',
        NOW() - INTERVAL '15 days'
    ),
    (
        'Trịnh',
        'Quỳnh',
        'quynh.trinh@provider.net',
        '$2a$10$agFzx5FVfCDVt1rdzQTdcOV4Hy6I/4q9SiQRPbvh9r5umm4EOKuPC',
        '0912000015',
        NULL,
        'CUSTOMER',
        NOW() - INTERVAL '10 days'
    ),
    -- VIP Customers (Khách hàng VIP - chi tiêu >5M và >=3 đơn COMPLETED)
    (
        'Phạm',
        'Thảo',
        'thao.pham@vip.com',
        '$2a$10$agFzx5FVfCDVt1rdzQTdcOV4Hy6I/4q9SiQRPbvh9r5umm4EOKuPC',
        '0914000001',
        NULL,
        'CUSTOMER',
        NOW() - INTERVAL '180 days'
    ),
    (
        'Hoàng',
        'Uyên',
        'uyen.hoang@premium.net',
        '$2a$10$agFzx5FVfCDVt1rdzQTdcOV4Hy6I/4q9SiQRPbvh9r5umm4EOKuPC',
        '0914000002',
        NULL,
        'CUSTOMER',
        NOW() - INTERVAL '200 days'
    );
-- ========== 2) DELIVERY UNITS ==========
INSERT INTO DeliveryUnits(name, estimated_time, is_active, created_at)
VALUES (
        'Giao Nhanh 2H',
        '2-4 giờ nội thành',
        TRUE,
        NOW() - INTERVAL '80 days'
    ),
    (
        'GHN',
        '1-2 ngày',
        TRUE,
        NOW() - INTERVAL '80 days'
    ),
    (
        'Viettel Post',
        '2-3 ngày',
        TRUE,
        NOW() - INTERVAL '80 days'
    );
-- ========== 3) CATALOGS ==========
-- Danh mục sản phẩm dựa trên chủ đề
INSERT INTO Catalogs(value, image, created_at, updated_at)
VALUES (
        'Tình yêu',
        'https://images.pexels.com/photos/66896/tulips-flowers-fish-eye-red-66896.jpeg',
        NOW() - INTERVAL '70 days',
        NULL
    ),
    -- id: 1
    (
        'Trang trí',
        'https://images.pexels.com/photos/32069314/pexels-photo-32069314.jpeg',
        NOW() - INTERVAL '70 days',
        NULL
    ),
    -- id: 2
    (
        'Hoa cưới',
        'https://media.istockphoto.com/id/929904308/vi/anh/b%C3%B3-hoa-tr%C3%AAn-b%C3%A3i-bi%E1%BB%83n.jpg?b=1&s=612x612&w=0&k=20&c=ZY5Ue05N97p7KanUmfPUxsnQyR71wUGQIu3YZq_Nl7s=',
        NOW() - INTERVAL '70 days',
        NULL
    ),
    (
        'Sinh nhật',
        'https://images.pexels.com/photos/2072153/pexels-photo-2072153.jpeg',
        NOW() - INTERVAL '70 days',
        NULL
    ),
    -- id: 5
    (
        'Chúc mừng',
        'https://images.pexels.com/photos/5841240/pexels-photo-5841240.jpeg',
        NOW() - INTERVAL '70 days',
        NULL
    ),
    -- id: 6      -- id: 3
    (
        'Đám tang',
        'https://images.pexels.com/photos/8963916/pexels-photo-8963916.jpeg',
        NOW() - INTERVAL '70 days',
        NULL
    );
-- id: 4
-- ========== 4) PRODUCTS ==========
-- Thêm GHN shipping dimensions và catalog_id cho tất cả products
INSERT INTO Products(
        name,
        description,
        price,
        image,
        stock_quantity,
        status,
        weight_g,
        length_cm,
        width_cm,
        height_cm,
        catalog_id,
        created_at,
        updated_at
    )
VALUES (
        'Bó hồng đỏ Classic',
        'Bó 12 bông hồng đỏ giấy kraft, tượng trưng cho tình yêu nồng nàn và lãng mạn. Phù hợp cho các dịp đặc biệt như sinh nhật, kỷ niệm tình yêu.',
        450000,
        'https://images.pexels.com/photos/34174876/pexels-photo-34174876.jpeg',
        50,
        'ACTIVE',
        800,
        60,
        25,
        40,
        1,
        NOW() - INTERVAL '60 days',
        NOW() - INTERVAL '60 days'
    ),
    (
        'Bó hồng phấn Sweetie',
        'Hồng phấn baby kèm thiệp, mang vẻ đẹp dịu dàng và thanh lịch. Thích hợp để tặng bạn gái, vợ trong các dịp lãng mạn.',
        520000,
        'https://images.pexels.com/photos/3782745/pexels-photo-3782745.jpeg',
        40,
        'ACTIVE',
        750,
        55,
        25,
        38,
        1,
        NOW() - INTERVAL '59 days',
        NOW() - INTERVAL '59 days'
    ),
    (
        'Giỏ hoa hướng dương',
        'Giỏ 8 bông hướng dương tươi, mang ý nghĩa của sự lạc quan và hy vọng. Thích hợp để tặng bạn bè, đồng nghiệp.',
        600000,
        'https://images.pexels.com/photos/54267/sunflower-blossom-bloom-flowers-54267.jpeg',
        35,
        'ACTIVE',
        1200,
        40,
        30,
        35,
        2,
        NOW() - INTERVAL '58 days',
        NOW() - INTERVAL '58 days'
    ),
    (
        'Bình tulip Hà Lan',
        '10 tulip phối nơ, tượng trưng cho sự giàu có và thành công. Thường được chọn cho các dịp khai trương, chúc mừng.',
        750000,
        'https://images.pexels.com/photos/34200702/pexels-photo-34200702.jpeg',
        30,
        'ACTIVE',
        1500,
        25,
        25,
        45,
        2,
        NOW() - INTERVAL '57 days',
        NOW() - INTERVAL '57 days'
    ),
    (
        'Lan hồ điệp 3 cành',
        'Chậu lan sang trọng, thể hiện sự quý phái và đẳng cấp. Thường được chọn cho các sự kiện quan trọng và trang trí văn phòng.',
        1200000,
        'https://images.pexels.com/photos/19838583/pexels-photo-19838583.jpeg',
        20,
        'ACTIVE',
        2000,
        30,
        30,
        50,
        2,
        NOW() - INTERVAL '56 days',
        NOW() - INTERVAL '56 days'
    ),
    (
        'Bó cẩm chướng Pastel',
        'Tone pastel nhẹ nhàng, thể hiện tình cảm chân thành và sự biết ơn. Phù hợp để tặng mẹ, bà trong các dịp đặc biệt.',
        390000,
        'https://images.pexels.com/photos/136255/pexels-photo-136255.jpeg',
        60,
        'ACTIVE',
        600,
        50,
        20,
        35,
        1,
        NOW() - INTERVAL '55 days',
        NOW() - INTERVAL '55 days'
    ),
    (
        'Hoa cưới',
        'Bó cưới tone trắng kem, tượng trưng cho sự trong trắng và hạnh phúc. Được thiết kế đặc biệt cho ngày cưới.',
        850000,
        'https://images.pexels.com/photos/540522/pexels-photo-540522.jpeg',
        12,
        'ACTIVE',
        1000,
        50,
        30,
        45,
        3,
        NOW() - INTERVAL '51 days',
        NOW() - INTERVAL '51 days'
    ),
    (
        'Vòng hoa chia buồn',
        'Trang trọng, lịch sự, thể hiện sự tôn trọng và chia sẻ nỗi buồn. Thường được sử dụng trong các buổi lễ tang.',
        1500000,
        'https://images.pexels.com/photos/33327309/pexels-photo-33327309.jpeg',
        10,
        'ACTIVE',
        3000,
        80,
        80,
        20,
        4,
        NOW() - INTERVAL '50 days',
        NOW() - INTERVAL '50 days'
    ),
    (
        'Hoa mừng khai trương',
        'Kệ hoa chúc mừng, mang ý nghĩa thịnh vượng và thành công. Thích hợp cho các dịp khai trương cửa hàng, công ty.',
        1800000,
        'https://images.pexels.com/photos/2741479/pexels-photo-2741479.jpeg',
        8,
        'ACTIVE',
        5000,
        100,
        60,
        150,
        2,
        NOW() - INTERVAL '49 days',
        NOW() - INTERVAL '49 days'
    ),
    (
        'Hoa Lavender',
        'Hoa lavender tím nhạt với hương thơm dịu nhẹ, có tác dụng thư giãn tinh thần. Thích hợp để trang trí phòng ngủ.',
        320000,
        'https://images.pexels.com/photos/29554274/pexels-photo-29554274.jpeg',
        25,
        'ACTIVE',
        300,
        40,
        15,
        25,
        1,
        NOW() - INTERVAL '48 days',
        NOW() - INTERVAL '48 days'
    ),
    (
        'Hoa Peony Hồng',
        'Hoa peony hồng đậm đà, tượng trưng cho sự thịnh vượng và may mắn. Thường được chọn cho các dịp cưới hỏi.',
        680000,
        'https://images.pexels.com/photos/18881375/pexels-photo-18881375.jpeg',
        18,
        'ACTIVE',
        700,
        50,
        25,
        35,
        1,
        NOW() - INTERVAL '47 days',
        NOW() - INTERVAL '47 days'
    ),
    (
        'Hoa Gerbera Cam',
        'Hoa gerbera cam rực rỡ, mang năng lượng tích cực và niềm vui. Thích hợp để tặng trong các dịp vui mừng.',
        420000,
        'https://images.pexels.com/photos/34345125/pexels-photo-34345125.jpeg',
        35,
        'ACTIVE',
        500,
        45,
        20,
        30,
        2,
        NOW() - INTERVAL '46 days',
        NOW() - INTERVAL '46 days'
    ),
    (
        'Bó hoa Hạnh Phúc',
        'Bó hoa baby, cẩm chướng và hồng, tone màu rực rỡ, hoàn hảo cho một ngày sinh nhật vui vẻ.',
        580000,
        'https://images.pexels.com/photos/2232569/pexels-photo-2232569.jpeg',
        30,
        'ACTIVE',
        850,
        55,
        25,
        40,
        5,
        NOW() - INTERVAL '45 days',
        NOW() - INTERVAL '45 days'
    ),
    (
        'Giỏ hoa Mặt Trời Nhỏ',
        'Giỏ hoa hướng dương và cúc trắng, mang lại năng lượng tích cực và lời chúc sinh nhật ý nghĩa.',
        650000,
        'https://images.pexels.com/photos/23857345/pexels-photo-23857345.jpeg',
        28,
        'ACTIVE',
        1300,
        40,
        30,
        40,
        5,
        NOW() - INTERVAL '44 days',
        NOW() - INTERVAL '44 days'
    ),
    (
        'Kệ hoa Thành Công',
        'Kệ hoa đồng tiền và lan vũ nữ, thiết kế sang trọng để chúc mừng thành tựu, thăng chức.',
        1600000,
        'https://images.pexels.com/photos/8499569/pexels-photo-8499569.jpeg',
        15,
        'ACTIVE',
        4500,
        90,
        50,
        140,
        6,
        NOW() - INTERVAL '43 days',
        NOW() - INTERVAL '43 days'
    ),
    (
        'Bình hoa Tươi Sáng',
        'Bình hoa tulip và hồng vàng, tượng trưng cho sự khởi đầu mới và lời chúc may mắn.',
        720000,
        'https://images.pexels.com/photos/4790263/pexels-photo-4790263.jpeg',
        22,
        'ACTIVE',
        1600,
        30,
        30,
        50,
        6,
        NOW() - INTERVAL '42 days',
        NOW() - INTERVAL '42 days'
    );
-- ========== 5) ADDRESSES ==========
-- Thêm addresses cho users với GHN fields (mix OLD và NEW mode)
INSERT INTO Addresses(
        user_id,
        street,
        city,
        province,
        is_default,
        province_id,
        district_id,
        ward_code,
        address_detail,
        province_name,
        district_name,
        ward_name,
        address_mode,
        created_at,
        updated_at
    )
VALUES -- User 1: Nguyễn An (OLD mode - HCM)
    (
        1,
        '123 Nguyễn Văn Cừ, Phường 4, Quận 5',
        'Thành phố Hồ Chí Minh',
        'TP. Hồ Chí Minh',
        TRUE,
        202,
        1450,
        '21211',
        '123 Nguyễn Văn Cừ',
        'Thành phố Hồ Chí Minh',
        'Quận 5',
        'Phường 04',
        'OLD',
        NOW() - INTERVAL '50 days',
        NOW() - INTERVAL '50 days'
    ),
    -- User 2: Trần Bình (NEW mode - HN)
    (
        2,
        '456 Hoàng Hoa Thám, Phường Liễu Giai',
        'Hà Nội',
        'Hà Nội',
        TRUE,
        201,
        NULL,
        '1A0208',
        '456 Hoàng Hoa Thám',
        'Hà Nội',
        NULL,
        'Phường Liễu Giai',
        'NEW',
        NOW() - INTERVAL '45 days',
        NOW() - INTERVAL '45 days'
    ),
    -- User 3: Lê Chi (OLD mode - HCM)
    (
        3,
        '789 Lê Văn Sỹ, Phường 1, Quận Tân Bình',
        'Thành phố Hồ Chí Minh',
        'TP. Hồ Chí Minh',
        TRUE,
        202,
        1463,
        '21301',
        '789 Lê Văn Sỹ',
        'Thành phố Hồ Chí Minh',
        'Quận Tân Bình',
        'Phường 01',
        'OLD',
        NOW() - INTERVAL '35 days',
        NOW() - INTERVAL '35 days'
    ),
    -- User 4: Phạm Dũng (NEW mode - DN)
    (
        4,
        '321 Trần Phú, Phường Hải Châu I',
        'Đà Nẵng',
        'Đà Nẵng',
        TRUE,
        291,
        NULL,
        '90107',
        '321 Trần Phú',
        'Đà Nẵng',
        NULL,
        'Phường Hải Châu I',
        'NEW',
        NOW() - INTERVAL '30 days',
        NOW() - INTERVAL '30 days'
    ),
    -- User 5: Võ Hà (OLD mode - HCM) + thêm address phụ
    (
        5,
        '654 Võ Văn Tần, Phường 6, Quận 3',
        'Thành phố Hồ Chí Minh',
        'TP. Hồ Chí Minh',
        TRUE,
        202,
        1442,
        '21006',
        '654 Võ Văn Tần',
        'Thành phố Hồ Chí Minh',
        'Quận 3',
        'Phường 06',
        'OLD',
        NOW() - INTERVAL '25 days',
        NOW() - INTERVAL '25 days'
    ),
    (
        5,
        '987 Nguyễn Thị Minh Khai, Phường Đa Kao, Quận 1',
        'Thành phố Hồ Chí Minh',
        'TP. Hồ Chí Minh',
        FALSE,
        202,
        1440,
        '21012',
        '987 Nguyễn Thị Minh Khai',
        'Thành phố Hồ Chí Minh',
        'Quận 1',
        'Phường Đa Kao',
        'OLD',
        NOW() - INTERVAL '20 days',
        NOW() - INTERVAL '20 days'
    ),
    -- AT_RISK Customers Addresses
    -- User 16: Nguyễn Tony (ngtony1025@gmail.com)
    (
        16,
        '100 Lê Lợi, Phường Bến Nghé, Quận 1',
        'Thành phố Hồ Chí Minh',
        'TP. Hồ Chí Minh',
        TRUE,
        202,
        1440,
        '21001',
        '100 Lê Lợi',
        'Thành phố Hồ Chí Minh',
        'Quận 1',
        'Phường Bến Nghé',
        'OLD',
        NOW() - INTERVAL '150 days',
        NOW() - INTERVAL '150 days'
    ),
    -- User 17: Trần Quỳnh
    (
        17,
        '200 Trần Hưng Đạo, Phường 5, Quận 5',
        'Thành phố Hồ Chí Minh',
        'TP. Hồ Chí Minh',
        TRUE,
        202,
        1450,
        '21205',
        '200 Trần Hưng Đạo',
        'Thành phố Hồ Chí Minh',
        'Quận 5',
        'Phường 05',
        'OLD',
        NOW() - INTERVAL '120 days',
        NOW() - INTERVAL '120 days'
    ),
    -- User 18: Lê Sơn
    (
        18,
        '300 Hai Bà Trưng, Phường Tân Định, Quận 1',
        'Thành phố Hồ Chí Minh',
        'TP. Hồ Chí Minh',
        TRUE,
        202,
        1440,
        '21009',
        '300 Hai Bà Trưng',
        'Thành phố Hồ Chí Minh',
        'Quận 1',
        'Phường Tân Định',
        'OLD',
        NOW() - INTERVAL '110 days',
        NOW() - INTERVAL '110 days'
    ),
    -- VIP Customers Addresses
    -- User 19: Phạm Thảo
    (
        19,
        '500 Nguyễn Trãi, Phường 7, Quận 5',
        'Thành phố Hồ Chí Minh',
        'TP. Hồ Chí Minh',
        TRUE,
        202,
        1450,
        '21207',
        '500 Nguyễn Trãi',
        'Thành phố Hồ Chí Minh',
        'Quận 5',
        'Phường 07',
        'OLD',
        NOW() - INTERVAL '180 days',
        NOW() - INTERVAL '180 days'
    ),
    -- User 20: Hoàng Uyên
    (
        20,
        '88 Đồng Khởi, Phường Bến Nghé, Quận 1',
        'Thành phố Hồ Chí Minh',
        'TP. Hồ Chí Minh',
        TRUE,
        202,
        1440,
        '21001',
        '88 Đồng Khởi',
        'Thành phố Hồ Chí Minh',
        'Quận 1',
        'Phường Bến Nghé',
        'OLD',
        NOW() - INTERVAL '200 days',
        NOW() - INTERVAL '200 days'
    );
-- ========== 6) CARTS ==========
-- Tạo carts cho customers
INSERT INTO Carts(
        user_id,
        total_amount,
        total_quantity,
        created_at,
        updated_at
    )
VALUES (
        1,
        0,
        0,
        NOW() - INTERVAL '60 days',
        NOW() - INTERVAL '1 day'
    ),
    (
        2,
        0,
        0,
        NOW() - INTERVAL '58 days',
        NOW() - INTERVAL '2 days'
    ),
    (
        3,
        0,
        0,
        NOW() - INTERVAL '40 days',
        NOW() - INTERVAL '3 days'
    ),
    (
        4,
        0,
        0,
        NOW() - INTERVAL '35 days',
        NOW() - INTERVAL '4 days'
    ),
    (
        5,
        0,
        0,
        NOW() - INTERVAL '30 days',
        NOW() - INTERVAL '5 days'
    );
-- ========== 7) CART ITEMS ==========
-- Thêm một số items vào carts để demo
INSERT INTO cart_items(
        cart_id,
        product_id,
        quantity,
        created_at,
        updated_at
    )
VALUES (
        1,
        1,
        2,
        NOW() - INTERVAL '2 days',
        NOW() - INTERVAL '2 days'
    ),
    (
        1,
        3,
        1,
        NOW() - INTERVAL '1 day',
        NOW() - INTERVAL '1 day'
    ),
    (
        2,
        2,
        1,
        NOW() - INTERVAL '3 days',
        NOW() - INTERVAL '3 days'
    ),
    (
        3,
        5,
        1,
        NOW() - INTERVAL '4 days',
        NOW() - INTERVAL '4 days'
    ),
    (
        3,
        7,
        2,
        NOW() - INTERVAL '2 days',
        NOW() - INTERVAL '2 days'
    );
-- Update cart totals after adding items
UPDATE Carts
SET total_amount = (
        SELECT COALESCE(SUM(ci.quantity * p.price), 0)
        FROM cart_items ci
            JOIN Products p ON ci.product_id = p.id
        WHERE ci.cart_id = Carts.id
    ),
    total_quantity = (
        SELECT COALESCE(SUM(ci.quantity), 0)
        FROM cart_items ci
        WHERE ci.cart_id = Carts.id
    ),
    updated_at = NOW()
WHERE id IN (1, 2, 3, 4, 5);
-- ========== 8) FOLLOWS (WISHLIST) ==========
-- Thêm wishlist data cho users
INSERT INTO Follows(user_id, product_id, followed_at)
VALUES (1, 1, NOW() - INTERVAL '10 days'),
    (1, 5, NOW() - INTERVAL '8 days'),
    (1, 10, NOW() - INTERVAL '5 days'),
    (2, 2, NOW() - INTERVAL '12 days'),
    (2, 4, NOW() - INTERVAL '7 days'),
    (3, 3, NOW() - INTERVAL '15 days'),
    (3, 6, NOW() - INTERVAL '9 days'),
    (3, 8, NOW() - INTERVAL '3 days'),
    (4, 7, NOW() - INTERVAL '6 days'),
    (4, 9, NOW() - INTERVAL '4 days'),
    (5, 11, NOW() - INTERVAL '2 days'),
    (5, 12, NOW() - INTERVAL '1 day');
-- ========== 9) VOUCHERS ==========
INSERT INTO Vouchers(
        code,
        name,
        description,
        discount_type,
        discount_value,
        max_discount_amount,
        min_order_value,
        max_uses,
        uses,
        expiry_date,
        is_active,
        created_at
    )
VALUES (
        'WELCOME10',
        'Chào mừng 10%',
        'Giảm 10% cho đơn hàng từ 500k, tối đa 100k',
        'PERCENTAGE',
        10,
        100000,
        500000,
        100,
        15,
        (NOW() + INTERVAL '30 days')::date,
        TRUE,
        NOW() - INTERVAL '30 days'
    ),
    (
        'NEWUSER20',
        'Khách mới 20%',
        'Giảm 20% cho khách hàng mới, tối đa 200k',
        'PERCENTAGE',
        20,
        200000,
        300000,
        50,
        8,
        (NOW() + INTERVAL '25 days')::date,
        TRUE,
        NOW() - INTERVAL '25 days'
    ),
    (
        'SAVE50K',
        'Tiết kiệm 50k',
        'Giảm cố định 50k cho đơn hàng từ 800k',
        'FIXED',
        50000,
        NULL,
        800000,
        200,
        25,
        (NOW() + INTERVAL '20 days')::date,
        TRUE,
        NOW() - INTERVAL '20 days'
    ),
    (
        'VIP15',
        'VIP 15%',
        'Giảm 15% cho khách VIP, tối đa 300k',
        'PERCENTAGE',
        15,
        300000,
        1000000,
        30,
        5,
        (NOW() + INTERVAL '15 days')::date,
        TRUE,
        NOW() - INTERVAL '15 days'
    );
-- ========== 10) AI CHAT CONFIGURATION ==========
-- Store information
INSERT INTO ai_chat_config (
        config_key,
        config_value,
        config_type,
        description
    )
VALUES (
        'store.name',
        'StarShop - Hoa tươi cao cấp',
        'STRING',
        'Tên cửa hàng'
    ),
    (
        'store.address',
        '01 Võ Văn Ngân, Phường Linh Chiểu, TP. Thủ Đức, TP.HCM',
        'STRING',
        'Địa chỉ cửa hàng'
    ),
    (
        'store.hotline',
        '1900 1900',
        'STRING',
        'Số hotline'
    ),
    (
        'store.email',
        'starshop.a.6868@gmail.com',
        'STRING',
        'Email liên hệ'
    ),
    (
        'store.hours',
        '8:00 - 22:00 hàng ngày',
        'STRING',
        'Giờ mở cửa'
    ),
    (
        'store.description',
        'Chuyên cung cấp hoa tươi cao cấp, thiết kế theo yêu cầu, giao hàng nhanh toàn TP.HCM',
        'TEXT',
        'Mô tả cửa hàng'
    );
-- AI behavior configuration
INSERT INTO ai_chat_config (
        config_key,
        config_value,
        config_type,
        description
    )
VALUES (
        'ai.confidence_threshold_auto',
        '0.80',
        'NUMBER',
        'Ngưỡng confidence để AI tự động trả lời (≥0.80)'
    ),
    (
        'ai.confidence_threshold_suggest',
        '0.65',
        'NUMBER',
        'Ngưỡng confidence để AI gợi ý chuyển staff (0.65-0.79)'
    ),
    (
        'ai.confidence_threshold_handoff',
        '0.65',
        'NUMBER',
        'Ngưỡng confidence để bắt buộc chuyển staff (<0.65)'
    ),
    (
        'ai.max_conversation_history',
        '10',
        'NUMBER',
        'Số tin nhắn tối đa trong lịch sử hội thoại gửi cho AI'
    ),
    (
        'ai.response_timeout_seconds',
        '30',
        'NUMBER',
        'Timeout cho AI response (seconds)'
    ),
    (
        'ai.enable_auto_handoff',
        'true',
        'BOOLEAN',
        'Tự động chuyển staff khi cần thiết'
    ),
    (
        'ai.enable_pii_detection',
        'true',
        'BOOLEAN',
        'Bật phát hiện thông tin cá nhân'
    ),
    (
        'ai.enable_product_suggestions',
        'true',
        'BOOLEAN',
        'Bật gợi ý sản phẩm'
    ),
    (
        'ai.max_product_suggestions',
        '3',
        'NUMBER',
        'Số sản phẩm tối đa gợi ý mỗi lần'
    ),
    (
        'ai.default_shipping_weight',
        '500',
        'NUMBER',
        'Trọng lượng mặc định để tính phí ship (gram)'
    );
-- AI personality and prompts
INSERT INTO ai_chat_config (
        config_key,
        config_value,
        config_type,
        description
    )
VALUES (
        'ai.bot_name',
        'Hoa AI',
        'STRING',
        'Tên của AI chatbot'
    ),
    (
        'ai.bot_emoji',
        '🌸',
        'STRING',
        'Emoji đại diện cho bot'
    ),
    (
        'ai.greeting_message',
        'Xin chào! 👋 Mình là Hoa AI, trợ lý ảo của StarShop. Mình có thể giúp gì cho bạn hôm nay?',
        'TEXT',
        'Tin nhắn chào mừng'
    ),
    (
        'ai.personality',
        'Lịch sự, thân thiện, nhiệt tình, chuyên nghiệp. Sử dụng ngôn ngữ gần gũi nhưng tôn trọng khách hàng.',
        'TEXT',
        'Tính cách của AI'
    );
-- Policies
INSERT INTO ai_chat_config (
        config_key,
        config_value,
        config_type,
        description
    )
VALUES (
        'policy.shipping',
        'Miễn phí ship đơn từ 500k trong bán kính 5km. Giao hàng trong 2-4 giờ. Hỗ trợ giao toàn TP.HCM.',
        'TEXT',
        'Chính sách vận chuyển'
    ),
    (
        'policy.return',
        'Đổi trả trong 24h nếu hoa không đúng mô tả hoặc bị héo. Hoàn tiền 100% nếu giao trễ quá 1 giờ.',
        'TEXT',
        'Chính sách đổi trả'
    ),
    (
        'policy.payment',
        'Hỗ trợ thanh toán: COD, MoMo, chuyển khoản ngân hàng, thẻ tín dụng.',
        'TEXT',
        'Phương thức thanh toán'
    );
-- ========== 11) UPDATE EXISTING USERS ==========
-- Update existing users to set is_active to TRUE
UPDATE Users
SET is_active = TRUE
WHERE is_active IS NULL;


-- =============================================
-- SEED DATA FOR ORDERS & ORDER ITEMS (FOR CHARTING)
-- PHIÊN BẢN ĐÃ SỬA LỖI ID - Tương thích với OrderIdGeneratorService.java
-- ID Format: ddMMyy + sequence_number
-- =============================================

-- ===== ĐƠN HÀNG THÁNG 08/2025 =====
-- Order 1
INSERT INTO Orders (
        id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee, notes
    )
VALUES (
        '0108251', 1, 500000, 'COMPLETED', '2025-08-01 10:30:00', 1, 'MOMO', 30000, 'Giao hàng nhanh'
    );
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('0108251', 1, 1, 450000);

-- Order 2
INSERT INTO Orders (
        id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee
    )
VALUES (
        '0508252', 3, 1250000, 'COMPLETED', '2025-08-05 14:00:00', 3, 'BANK_TRANSFER', 0
    );
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('0508252', 5, 1, 1200000);

-- Order 3
INSERT INTO Orders (
        id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee, notes
    )
VALUES (
        '1008253', 5, 420000, 'CANCELLED', '2025-08-10 09:15:00', 5, 'COD', 30000, 'Khách hàng báo hủy'
    );
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('1008253', 6, 1, 390000);

-- ===== ĐƠN HÀNG THÁNG 09/2025 =====
-- Order 4
INSERT INTO Orders (
        id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee
    )
VALUES (
        '0209254', 2, 1100000, 'COMPLETED', '2025-09-02 11:20:00', 2, 'MOMO', 0
    );
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('0209254', 2, 1, 520000),
       ('0209254', 3, 1, 600000);

-- Order 5
INSERT INTO Orders (
        id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee
    )
VALUES (
        '1509255', 7, 1850000, 'COMPLETED', '2025-09-15 18:00:00', 3, 'BANK_TRANSFER', 50000
    );
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('1509255', 9, 1, 1800000);

-- Order 6
INSERT INTO Orders (
        id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee
    )
VALUES (
        '2009256', 8, 750000, 'COMPLETED', '2025-09-20 16:45:00', 4, 'COD', 0
    );
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('2009256', 4, 1, 750000);

-- Order 7
INSERT INTO Orders (
        id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee
    )
VALUES (
        '2809257', 1, 870000, 'COMPLETED', '2025-09-28 12:00:00', 1, 'MOMO', 20000
    );
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('2809257', 7, 1, 850000);

-- ===== ĐƠN HÀNG THÁNG 10/2025 (THÁNG HIỆN TẠI) =====
-- Order 8
INSERT INTO Orders (
        id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee, notes
    )
VALUES (
        '0110258', 10, 1520000, 'COMPLETED', '2025-10-01 20:00:00', 1, 'COD', 20000, 'Tặng kèm thiệp chúc mừng'
    );
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('0110258', 8, 1, 1500000);

-- Order 9
INSERT INTO Orders (
        id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee
    )
VALUES (
        '0510259', 12, 970000, 'COMPLETED', '2025-10-05 13:10:00', 4, 'BANK_TRANSFER', 20000
    );
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('0510259', 1, 1, 450000),
       ('0510259', 2, 1, 520000);

-- Order 10
INSERT INTO Orders (
        id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee
    )
VALUES (
        '10102510', 4, 1650000, 'SHIPPED', '2025-10-10 08:00:00', 4, 'MOMO', 50000
    );
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('10102510', 9, 1, 1600000);

-- Order 11
INSERT INTO Orders (
        id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee
    )
VALUES (
        '18102511', 6, 340000, 'SHIPPED', '2025-10-18 17:00:00', 1, 'COD', 20000
    );
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('18102511', 10, 1, 320000);

-- Order 12
INSERT INTO Orders (
        id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee, notes
    )
VALUES (
        '20102512', 1, 450000, 'PROCESSING', '2025-10-20 09:45:00', 1, 'MOMO', 0, 'Giao trong giờ hành chính'
    );
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('20102512', 1, 1, 450000);

-- Order 13
INSERT INTO Orders (
        id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee
    )
VALUES (
        '21102513', 9, 710000, 'PENDING', '2025-10-21 11:00:00', 5, 'COD', 30000
    );
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('21102513', 11, 1, 680000);

-- ===== ĐƠN HÀNG CỦA KHÁCH AT_RISK (Đơn cũ >= 90 ngày) =====
-- Order 14: Nguyễn Tony (ngtony1025@gmail.com) - User 16
INSERT INTO Orders (
        id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee, notes
    )
VALUES (
        '01062514', 16, 1250000, 'COMPLETED', NOW() - INTERVAL '120 days', 6, 'BANK_TRANSFER', 30000, 'Đơn hàng cũ'
    );
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('01062514', 5, 1, 1200000);

-- Order 15: Nguyễn Tony - Đơn thứ 2
INSERT INTO Orders (
        id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee
    )
VALUES (
        '15062515', 16, 920000, 'COMPLETED', NOW() - INTERVAL '100 days', 6, 'MOMO', 20000
    );
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('15062515', 1, 2, 450000);

-- Order 16: Trần Quỳnh (quynh.tran@oldcustomer.com) - User 17
INSERT INTO Orders (
        id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee
    )
VALUES (
        '10072516', 17, 1350000, 'COMPLETED', NOW() - INTERVAL '110 days', 7, 'COD', 50000
    );
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('10072516', 4, 1, 750000),
       ('10072516', 2, 1, 520000);

-- Order 17: Lê Sơn (son.le@inactive.net) - User 18
INSERT INTO Orders (
        id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee, notes
    )
VALUES (
        '20072517', 18, 1820000, 'COMPLETED', NOW() - INTERVAL '95 days', 8, 'BANK_TRANSFER', 20000, 'Giao nhanh'
    );
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('20072517', 9, 1, 1800000);

-- ===== NÂNG CẤP NGUYỄN TONY LÊN VIP =====
-- Thêm 2 đơn hàng gần đây để đủ điều kiện VIP (>5M VND và >=3 đơn COMPLETED)
-- Order 18: Nguyễn Tony - Đơn thứ 3 (gần đây)
INSERT INTO Orders (
        id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee, notes
    )
VALUES (
        '15102518', 16, 1850000, 'COMPLETED', NOW() - INTERVAL '15 days', 6, 'MOMO', 50000, 'Khách VIP - Ưu tiên giao hàng'
    );
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('15102518', 9, 1, 1800000);

-- Order 19: Nguyễn Tony - Đơn thứ 4 (mới nhất)
INSERT INTO Orders (
        id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee, notes
    )
VALUES (
        '20102519', 16, 1580000, 'COMPLETED', NOW() - INTERVAL '5 days', 6, 'BANK_TRANSFER', 80000, 'Giao tận nơi - Khách VIP'
    );
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('20102519', 8, 1, 1500000);

-- Tổng chi tiêu của Nguyễn Tony: 1,250,000 + 920,000 + 1,850,000 + 1,580,000 = 5,600,000 VND
-- Tổng đơn COMPLETED: 4 đơn
-- => ĐỦ ĐIỀU KIỆN VIP (>5M VND và >=3 đơn)

-- ===== ĐƠN HÀNG CỦA KHÁCH VIP KHÁC =====
-- Order 20-22: Phạm Thảo (thao.pham@vip.com) - User 19
INSERT INTO Orders (
        id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee, notes
    )
VALUES (
        '10082520', 19, 1520000, 'COMPLETED', NOW() - INTERVAL '60 days', 9, 'BANK_TRANSFER', 20000, 'Khách VIP'
    );
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('10082520', 8, 1, 1500000);

INSERT INTO Orders (
        id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee
    )
VALUES (
        '05092521', 19, 1850000, 'COMPLETED', NOW() - INTERVAL '45 days', 9, 'MOMO', 50000
    );
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('05092521', 9, 1, 1800000);

INSERT INTO Orders (
        id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee
    )
VALUES (
        '10102522', 19, 2050000, 'COMPLETED', NOW() - INTERVAL '10 days', 9, 'BANK_TRANSFER', 50000
    );
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('10102522', 5, 1, 1200000),
       ('10102522', 4, 1, 750000);

-- Tổng chi tiêu Phạm Thảo: 1,520,000 + 1,850,000 + 2,050,000 = 5,420,000 VND
-- Tổng đơn COMPLETED: 3 đơn => VIP

-- Order 23-25: Hoàng Uyên (uyen.hoang@premium.net) - User 20
INSERT INTO Orders (
        id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee, notes
    )
VALUES (
        '15072523', 20, 2050000, 'COMPLETED', NOW() - INTERVAL '80 days', 10, 'BANK_TRANSFER', 50000, 'Khách hàng thân thiết'
    );
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('15072523', 9, 1, 1800000),
       ('15072523', 1, 1, 450000);

INSERT INTO Orders (
        id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee
    )
VALUES (
        '20082524', 20, 1770000, 'COMPLETED', NOW() - INTERVAL '50 days', 10, 'MOMO', 20000
    );
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('20082524', 8, 1, 1500000),
       ('20082524', 6, 1, 390000);

INSERT INTO Orders (
        id, user_id, total_amount, status, order_date, address_id, payment_method, shipping_fee
    )
VALUES (
        '12102525', 20, 1880000, 'COMPLETED', NOW() - INTERVAL '8 days', 10, 'BANK_TRANSFER', 80000
    );
INSERT INTO OrderItems (order_id, product_id, quantity, price)
VALUES ('12102525', 9, 1, 1800000);

-- Tổng chi tiêu Hoàng Uyên: 2,050,000 + 1,770,000 + 1,880,000 = 5,700,000 VND
-- Tổng đơn COMPLETED: 3 đơn => VIP

COMMIT;