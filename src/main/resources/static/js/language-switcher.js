// Language Switcher for StarShop
// Handles language switching functionality with localStorage persistence

class LanguageSwitcher {
    constructor() {
        this.currentLanguage = localStorage.getItem('starshop-language') || 'vi';
        this.translations = {};
        this.init();
    }

    init() {
        this.loadTranslations();
        this.updateLanguageDisplay();
        this.bindEvents();
        this.applyLanguage();
    }

    // Load translation data
    loadTranslations() {
        this.translations = {
            vi: {
                // Header
                'search-placeholder': '🌸 Tìm hoa nhanh - Hoa sinh nhật, tình yêu, cưới...',
                'login': 'Đăng nhập',
                'register': 'Đăng ký',
                'profile': 'Thông tin cá nhân',
                'orders': 'Đơn hàng của tôi',
                'wishlist': 'Danh sách yêu thích',
                'settings': 'Cài đặt',
                'logout': 'Đăng xuất',
                'customer-role': 'Khách hàng',
                
                // Navigation
                'home': 'Trang chủ',
                'categories': 'Danh mục hoa',
                'products': 'Sản phẩm',
                'gifts': 'Quà tặng',
                'occasions': 'Dịp đặc biệt',
                'blog': 'Blog',
                'contact': 'Liên hệ',
                'nav-home': 'Trang chủ',
                'nav-categories': 'Danh mục hoa',
                'nav-products': 'Sản phẩm',
                'nav-voucher': 'Voucher',
                'nav-blog': 'Blog',
                'no-categories': 'Không có danh mục nào',
                'view-all-categories': 'Xem tất cả danh mục',
                
                // Categories
                'birthday-flowers': 'Hoa sinh nhật',
                'love-flowers': 'Hoa tình yêu',
                'wedding-flowers': 'Hoa cưới',
                'opening-flowers': 'Hoa khai trương',
                'sympathy-flowers': 'Hoa chia buồn',
                'mother-flowers': 'Hoa tặng mẹ',
                
                // Home page
                'hero-title': 'Mang Hoa Đến Niềm Vui Của Bạn',
                'hero-subtitle': 'Khám phá bộ sưu tập hoa tươi cao cấp được tuyển chọn kỹ lưỡng, mang đến những khoảnh khắc đặc biệt nhất trong cuộc sống',
                'explore-products': 'Khám phá sản phẩm',
                'view-catalog': 'Xem danh mục',
                'fresh-daily': 'Hoa tươi mỗi ngày',
                'hero-title-part1': 'Mang',
                'hero-title-part2': 'hoa đến',
                'hero-title-part3': 'niềm vui của bạn',
                'hero-description': 'Khám phá bộ sưu tập hoa tươi tuyệt đẹp tại StarShop. Chúng tôi mang đến những bó hoa tinh tế, được thiết kế đặc biệt cho mọi khoảnh khắc đáng nhớ của bạn.',
                'happy-customers': 'Khách hàng hài lòng',
                'unique-designs': 'Mẫu hoa độc đáo',
                'customer-support': 'Hỗ trợ khách hàng',
                'explore-now': 'Khám phá ngay',
                'view-categories': 'Xem danh mục',
                'rating': '4.9',
                'rating-desc': 'Đánh giá',
                'why-starshop-desc': 'Chúng tôi cam kết mang đến trải nghiệm tuyệt vời nhất',
                'featured-categories': 'Danh mục nổi bật',
                'featured-categories-desc': 'Tìm kiếm bó hoa hoàn hảo cho mọi dịp đặc biệt',
                'explore-collection': 'Khám phá bộ sưu tập',
                'explore': 'Khám phá',
                'view-all-categories': 'Xem tất cả danh mục',
                'featured-products-desc': 'Những bó hoa được yêu thích nhất tại StarShop',
                'out-of-stock': 'HẾT HÀNG',
                'out-of-stock-badge': 'Hết hàng',
                'low-stock': 'Sắp hết',
                'featured': 'Nổi bật',
                'remaining': 'Còn',
                'view-all-products': 'Xem tất cả sản phẩm',
                'special-offer': 'Ưu đãi đặc biệt',
                'discount-20': 'Giảm 20%',
                'first-order': 'cho đơn hàng đầu tiên',
                'promo-desc': 'Chào mừng bạn đến với StarShop! Nhận ngay ưu đãi hấp dẫn khi mua sắm lần đầu.',
                'shop-now': 'Mua sắm ngay',
                'free-shipping': 'Miễn phí giao hàng',
                'hcm-area': 'Nội thành TPHCM',
                'delivery-2h': 'Giao hàng 2 giờ',
                'fast-reliable': 'Nhanh chóng & tin cậy',
                'fresh-flowers-100': 'Hoa tươi 100%',
                'quality-guaranteed': 'Đảm bảo chất lượng',
                'five-star-rating': 'Đánh giá 5 sao',
                'from-customers': 'Từ khách hàng',
                'customer-reviews': 'Đánh giá từ khách hàng',
                'what-customers-say': 'Khách hàng nói gì về chúng tôi',
                'testimonials-desc': 'Hàng nghìn khách hàng đã tin tưởng và hài lòng với dịch vụ của StarShop',
                
                // Why StarShop
                'why-starshop': 'Tại sao chọn StarShop?',
                'fresh-flowers': 'Hoa tươi 100%',
                'fresh-desc': 'Cam kết hoa tươi nhập khẩu trực tiếp',
                'fast-delivery': 'Giao hàng nhanh',
                'fast-desc': 'Giao hàng trong 2 giờ nội thành HCM',
                'professional-design': 'Thiết kế chuyên nghiệp',
                'design-desc': 'Đội ngũ florist chuyên nghiệp',
                'support-247': 'Hỗ trợ 24/7',
                'support-desc': 'Tư vấn và hỗ trợ mọi lúc',
                
                // Footer
                'footer-description': 'Nơi tình yêu nở hoa - Chúng tôi mang đến những bó hoa tươi tuyệt đẹp cho mọi dịp đặc biệt trong cuộc sống của bạn. Chất lượng cao, giao hàng nhanh chóng.',
                'follow-us': 'Theo dõi chúng tôi:',
                'footer-products': 'Sản phẩm',
                'useful-links': 'Liên kết hữu ích',
                'your-account': 'Tài khoản của bạn',
                'shipping-policy': 'Chính sách giao hàng',
                'returns-refunds': 'Đổi trả & hoàn tiền',
                'flower-care': 'Hướng dẫn chăm sóc hoa',
                'faq': 'Câu hỏi thường gặp',
                'contact-connect': 'Liên hệ & Kết nối',
                'business-hours': 'Giờ làm việc:',
                'weekdays': 'Thứ 2 - Thứ 6',
                'weekend': 'Thứ 7 - Chủ nhật',
                'copyright': 'Bản quyền thuộc về',
                'privacy-policy': 'Chính sách bảo mật',
                'terms-of-use': 'Điều khoản sử dụng',
                
                // Products
                'featured-products': 'Sản phẩm nổi bật',
                'quick-add': 'Thêm nhanh',
                'adding': 'Đang thêm...',
                'added': 'Đã thêm',
                'add-to-cart': 'Thêm',
                'add-to-cart-success': 'Đã thêm sản phẩm vào giỏ hàng!',
                'add-to-cart-error': 'Không thể thêm sản phẩm vào giỏ hàng',
                
                // Categories page
                'categories-title': 'Khám Phá Thế Giới Hoa',
                'categories-description': 'Từ những đóa hồng lãng mạn đến cúc họa mi tinh khôi, hãy tìm nguồn cảm hứng cho mọi khoảnh khắc đặc biệt.',
                'no-categories-found': 'Hiện tại chưa có danh mục sản phẩm nào được tìm thấy.',
                
                // Products page
                'search-results': 'Tìm kiếm',
                'all-products': 'Tất cả sản phẩm',
                'found': 'Tìm thấy',
                'products-match': 'sản phẩm phù hợp',
                'has': 'Có',
                'products-in-category': 'sản phẩm trong danh mục này',
                'fresh-flowers-at-starshop': 'sản phẩm hoa tươi đẹp tại StarShop',
                'category': 'Danh mục',
                'all-categories': 'Tất cả danh mục',
                'search-by-name': 'Tìm kiếm theo tên',
                'enter-product-name': 'Nhập tên sản phẩm...',
                'sort': 'Sắp xếp',
                'newest': 'Mới nhất',
                'oldest': 'Cũ nhất',
                'name-az': 'Tên A-Z',
                'price-low-high': 'Giá thấp → cao',
                'price-high-low': 'Giá cao → thấp',
                'filter': 'Lọc',
                'clear-filter': 'Xóa bộ lọc',
                'showing': 'Hiển thị',
                'of-total': 'trong tổng số',
                'products': 'sản phẩm',
                'no-products-found': 'Không tìm thấy sản phẩm nào',
                'no-match-keyword': 'Không có sản phẩm nào phù hợp với từ khóa',
                'try-different-keyword': 'Hãy thử tìm kiếm với từ khóa khác',
                'no-products-yet': 'Hiện tại chưa có sản phẩm nào. Vui lòng quay lại sau.',
                'add-to-wishlist': 'Thêm vào yêu thích',
                'new': 'Mới',
                'reviews': 'đánh giá',
                'add-to-cart-btn': 'Thêm vào giỏ',
                'login-to-buy': 'Đăng nhập để mua',
                'product-pagination': 'Phân trang sản phẩm',
                'previous-page': 'Trang trước',
                'next-page': 'Trang sau',
                'page': 'Trang',
                
                // Product Detail page
                'product-not-found-title': '404 - Không tìm thấy sản phẩm!',
                'product-not-found-desc': 'Sản phẩm bạn đang tìm kiếm không tồn tại hoặc đã bị xóa.',
                'back-to-products': 'Về trang danh sách sản phẩm',
                'product-description': 'Mô tả sản phẩm',
                'buy-now': 'Mua ngay',
                'free-shipping': 'Giao hàng miễn phí',
                'free-shipping-desc': 'Miễn phí giao hàng nội thành TP.HCM',
                'fast-delivery': 'Giao hàng nhanh',
                'delivery-2-4h': 'Giao trong 2-4 giờ',
                'fresh-flowers-100': 'Hoa tươi 100%',
                'fresh-quality-guarantee': 'Cam kết hoa tươi, chất lượng cao',
                'detailed-info': 'Thông tin chi tiết',
                'learn-more-product': 'Tìm hiểu thêm về sản phẩm và cách chăm sóc',
                'detailed-description': 'Mô tả chi tiết',
                'reviews-tab': 'Đánh giá',
                'care-guide': 'Hướng dẫn chăm sóc',
                'full-product-info': 'Thông tin đầy đủ về sản phẩm',
                'customer-reviews': 'Đánh giá từ khách hàng',
                'customer-experience': 'Chia sẻ trải nghiệm của khách hàng về sản phẩm',
                
                // Vouchers page
                'vouchers-title': 'Mã Giảm Giá Đặc Biệt',
                'vouchers-subtitle': 'Chọn mã ưu đãi phù hợp nhất cho đơn hàng của bạn',
                'loading-vouchers': 'Đang tải danh sách ưu đãi...',
                'error-loading-data': 'Lỗi tải dữ liệu',
                'cannot-connect-api': 'Không thể kết nối tới API Voucher',
                'try-again': 'Thử lại',
                'discount-percent': 'Giảm %',
                'discount-amount': 'Giảm tiền',
                'times': 'lần',
                'unlimited': 'Không giới hạn',
                'code': 'Mã',
                'min-order': 'Đơn tối thiểu',
                'max-discount': 'Giảm tối đa',
                'expiry-date': 'HSD',
                'copy-code': 'Sao chép mã',
                'copied': 'Đã sao chép!',
                'no-vouchers': 'Hiện tại chưa có mã giảm giá nào',
                'check-back-later': 'Vui lòng quay lại sau hoặc liên hệ hỗ trợ',
                
                // Blog page
                'blog-title': 'Blog StarShop',
                'blog-subtitle': 'Khám phá nghệ thuật cắm hoa, chăm sóc hoa tươi và xu hướng trang trí mới nhất',
                'search-articles': 'Tìm kiếm bài viết...',
                'search': 'Tìm',
                'all': 'Tất cả',
                'flower-care': 'Chăm sóc hoa',
                'flower-arrangement': 'Nghệ thuật cắm hoa',
                'trends': 'Xu hướng',
                'special-occasions': 'Dịp đặc biệt',
                'tips': 'Mẹo hay',
                'featured': 'Nổi bật',
                'read-more': 'Đọc thêm',
                'art': 'Nghệ thuật',
                'min-read': 'phút đọc',
                'view-more': 'Xem thêm',
                'care': 'Chăm sóc',
                'knowledge': 'Kiến thức',
                'newsletter-title': 'Đăng ký nhận bản tin',
                'newsletter-desc': 'Nhận những bài viết mới nhất, mẹo chăm sóc hoa và ưu đãi đặc biệt qua email',
                'subscribe': 'Đăng ký',
                
                // Checkout page
                'checkout-title': 'Thanh toán',
                'checkout-subtitle': 'Hoàn tất đơn hàng của bạn',
                'profile-update-required': 'Cần cập nhật thông tin tài khoản',
                'update-info': 'Cập nhật thông tin',
                'manage-addresses': 'Quản lý địa chỉ',
                'order-payment-info': 'Thông tin đơn và thanh toán',
                'check-cart-payment': 'Kiểm tra giỏ hàng và chọn phương thức thanh toán phù hợp.',
                'cart': 'Giỏ hàng',
                'shipping-address': 'Địa chỉ giao hàng',
                'select-shipping-address': 'Chọn địa chỉ giao hàng',
                'payment-method': 'Phương thức thanh toán',
                'shipping-fee': 'Phí vận chuyển',
                'calculating-fee': 'Đang tính phí...',
                'please-select-address': 'Vui lòng chọn địa chỉ giao hàng',
                'order-notes': 'Ghi chú đơn hàng',
                'order-notes-placeholder': 'Ghi chú thêm cho đơn hàng (không bắt buộc)...',
                'order-notes-example': 'Ví dụ: thời gian nhận hàng mong muốn, lưu ý cho nhân viên giao hàng...',
                'voucher-code': 'Mã giảm giá',
                'enter-code': 'NHẬP MÃ',
                'apply': 'Áp dụng',
                'place-order': 'Đặt hàng',
                'security-notice': 'Thông tin của bạn được bảo mật và mã hóa',
                'please-update-profile': 'Vui lòng cập nhật thông tin tài khoản',
                'complete-profile-before-order': 'Bạn cần hoàn thiện thông tin cá nhân trước khi có thể đặt hàng.',
                'calculating-shipping-fee': 'Đang tính phí vận chuyển...',
                'fast-shipping-ghn': 'Giao hàng nhanh (GHN)',
                'free': 'Miễn phí',
                'cannot-calculate-ghn-fee': 'Không thể tính phí GHN',
                'cannot-calculate-shipping-fee': 'Không thể tính phí vận chuyển',
                'error-loading-payment-methods': 'Có lỗi xảy ra khi tải phương thức thanh toán',
                'cart-empty': 'Giỏ hàng trống',
                'no-products-in-cart': 'Không có sản phẩm nào trong giỏ hàng',
                'error-loading-cart': 'Có lỗi khi tải giỏ hàng',
                'error-loading-order-summary': 'Có lỗi khi tải tóm tắt đơn hàng',
                'order-summary': 'Tóm tắt đơn hàng',
                'discount': 'Giảm giá',
                'total': 'Tổng cộng',
                
                // Payment methods
                'payment-method-cod': 'Thanh toán khi nhận hàng',
                'payment-method-momo': 'Ví điện tử MoMo',
                'payment-method-bank': 'Chuyển khoản ngân hàng',
                'payment-method-card': 'Thẻ tín dụng',
                'status-available': 'Sẵn sàng',
                'status-not-supported': 'Chưa hỗ trợ',
                'status-unknown': 'Không xác định',
                
                // Cart page
                'cart-title': 'Giỏ hàng của bạn',
                'cart-subtitle': 'sản phẩm trong giỏ hàng',
                'select-all': 'Chọn tất cả sản phẩm',
                'cart-summary': 'Tổng cộng giỏ hàng',
                'unit-price': 'Đơn giá',
                'quantity': 'Số lượng',
                'subtotal': 'Thành tiền',
                'total-quantity': 'Tổng số lượng',
                'temporary-total': 'Tạm tính',
                'final-total': 'Tổng tiền',
                'selected-items': 'sản phẩm được chọn',
                'proceed-checkout': 'Tiến hành thanh toán',
                'continue-shopping': 'Tiếp tục mua sắm',
                'clear-cart': 'Xóa tất cả',
                'shipping-note': 'Phí vận chuyển sẽ được tính ở bước thanh toán',
                'empty-cart-title': 'Giỏ hàng trống',
                'empty-cart-desc': 'Bạn chưa có sản phẩm nào trong giỏ hàng. Hãy khám phá các sản phẩm tuyệt vời của chúng tôi!',
                'explore-products': 'Khám phá sản phẩm',
                
                // Wishlist page
                'wishlist-title': 'Sản phẩm yêu thích',
                'wishlist-empty-title': 'Danh sách yêu thích trống',
                'wishlist-empty-desc': 'Khám phá bộ sưu tập hoa tươi đẹp và lưu lại những sản phẩm yêu thích của bạn',
                'wishlist-count': 'sản phẩm đang được lưu',
                'clear-all': 'Xóa tất cả',
                'in-stock': 'Còn hàng',
                'out-of-stock': 'Hết hàng',
                'view-details': 'Xem chi tiết',
                'remove-from-wishlist': 'Xóa khỏi yêu thích',
                'saved': 'Đã lưu',
                'contact-us': 'Liên hệ',
                
                // Profile page
                'verified': 'Đã xác thực',
                'customer': 'Khách hàng',
                'edit': 'Chỉnh sửa',
                'profile-details': 'Thông tin chi tiết',
                'phone': 'Số điện thoại',
                'join-date': 'Ngày tham gia',
                'not-updated': 'Chưa cập nhật',
                'unknown': 'Không rõ',
                'overview': 'Tổng quan',
                'orders': 'Đơn hàng',
                'favorites': 'Yêu thích',
                'shipping-address': 'Địa chỉ giao hàng',
                'add-address': 'Thêm địa chỉ',
                'loading-addresses': 'Đang tải địa chỉ...',
                'edit-profile': 'Chỉnh sửa thông tin cá nhân',
                'firstname': 'Họ',
                'lastname': 'Tên',
                'enter-firstname': 'Nhập họ của bạn',
                'enter-lastname': 'Nhập tên của bạn',
                'enter-phone': 'Nhập số điện thoại (10 chữ số)',
                'phone-format': 'Định dạng: 10 chữ số (VD: 0912345678)',
                'email-cannot-change': 'Email không thể thay đổi',
                'cancel': 'Hủy',
                'save-changes': 'Lưu thay đổi',
                'add-address-title': 'Thêm địa chỉ giao hàng',
                'choose-address-mode': 'Chọn cách nhập địa chỉ',
                'address-3-level': '3 cấp (Tỉnh/Quận/Phường)',
                'address-2-level': '2 cấp (Tỉnh/Phường)',
                'province': 'Tỉnh/Thành phố',
                'district': 'Quận/Huyện',
                'ward': 'Phường/Xã',
                'select-province': '-- Chọn Tỉnh/Thành phố --',
                'select-district': '-- Chọn Quận/Huyện --',
                'select-ward': '-- Chọn Phường/Xã --',
                'address-detail': 'Địa chỉ chi tiết',
                'enter-address-detail': 'Số nhà, tên đường...',
                'set-as-default': 'Đặt làm địa chỉ mặc định',
                'default-address': 'Mặc định',
                'supports-fast-delivery': 'Hỗ trợ giao hàng nhanh',
                'no-address': 'Chưa có địa chỉ giao hàng',
                'add-first-address': 'Thêm địa chỉ đầu tiên',
                'error-loading-addresses': 'Không thể tải danh sách địa chỉ',
                'error-loading-provinces': 'Không thể tải danh sách tỉnh/thành phố',
                'error-loading-districts': 'Không thể tải danh sách quận/huyện',
                'error-loading-wards': 'Không thể tải danh sách phường/xã',
                'processing': 'Đang xử lý...',
                'address-added-success': 'Thêm địa chỉ thành công!',
                'error-occurred': 'Có lỗi xảy ra',
                'cannot-add-address': 'Không thể thêm địa chỉ. Vui lòng thử lại.',
                'confirm-delete-address': 'Bạn có chắc chắn muốn xóa địa chỉ này?',
                'address-deleted-success': 'Xóa địa chỉ thành công!',
                'cannot-delete-address': 'Không thể xóa địa chỉ',
                'cannot-delete-address-retry': 'Không thể xóa địa chỉ. Vui lòng thử lại.',
                
                // Orders page
                'my-orders-title': 'Đơn hàng của bạn',
                'my-orders-desc': 'Theo dõi trạng thái giao hàng, xem lịch sử mua hàng và quản lý đơn hàng của bạn một cách dễ dàng.',
                'all': 'Tất cả',
                'pending': 'Chờ xử lý',
                'processing': 'Đang xử lý',
                'shipping': 'Đang giao',
                'shipped': 'Đang giao',
                'completed': 'Hoàn thành',
                'cancelled': 'Đã hủy',
                'loading-orders': 'Đang tải đơn hàng...',
                'review-product': 'Đánh giá sản phẩm',
                'review-after-purchase': 'Đánh giá sản phẩm sau khi mua hàng',
                'your-rating': 'Đánh giá của bạn *',
                'choose-rating': 'Chọn số sao để đánh giá',
                'comment-optional': 'Nhận xét (tùy chọn)',
                'share-your-experience': 'Chia sẻ trải nghiệm của bạn về sản phẩm này...',
                'max-1000-chars': 'Tối đa 1000 ký tự',
                'submit-review': 'Gửi đánh giá',
                'cancel-order': 'Hủy đơn',
                'details': 'Chi tiết',
                'total-payment': 'Tổng thanh toán',
                'quantity': 'Số lượng',
                'review': 'Đánh giá',
                'reorder': 'Mua lại',
                'very-poor': 'Rất tệ',
                'poor': 'Tệ',
                'average': 'Bình thường',
                'good': 'Tốt',
                'excellent': 'Rất tốt',
                
                // Order detail page
                'order-detail-badge': 'Chi tiết đơn hàng',
                'order-title': 'Đơn hàng',
                'order-placed-at': 'Đặt hàng lúc',
                'back': 'Quay lại',
                'payment-success': 'Thanh toán thành công!',
                'transaction-id': 'Mã giao dịch',
                'payment-failed': 'Thanh toán thất bại',
                'order-status': 'Trạng thái đơn hàng',
                'current-status': 'Trạng thái hiện tại',
                'products-ordered': 'Sản phẩm đã đặt',
                'subtotal': 'Tạm tính',
                'shipping-fee': 'Phí vận chuyển',
                'shipping-info': 'Thông tin giao hàng',
                'recipient': 'Người nhận',
                'delivery-address': 'Địa chỉ giao hàng',
                'notes': 'Ghi chú',
                'payment-method': 'Phương thức thanh toán',
                
                // Footer
                'language': 'Ngôn ngữ',
                'vietnamese': 'Tiếng Việt',
                'english': 'English'
            },
            en: {
                // Header
                'search-placeholder': '🌸 Search flowers quickly - Birthday, love, wedding...',
                'login': 'Login',
                'register': 'Register',
                'profile': 'Profile',
                'orders': 'My Orders',
                'wishlist': 'Wishlist',
                'settings': 'Settings',
                'logout': 'Logout',
                'customer-role': 'Customer',
                
                // Navigation
                'home': 'Home',
                'categories': 'Categories',
                'products': 'Products',
                'gifts': 'Gifts',
                'occasions': 'Occasions',
                'blog': 'Blog',
                'contact': 'Contact',
                'nav-home': 'Home',
                'nav-categories': 'Flower Categories',
                'nav-products': 'Products',
                'nav-voucher': 'Voucher',
                'nav-blog': 'Blog',
                'no-categories': 'No categories available',
                'search': 'Search',
                'search-placeholder': 'Enter flower name, occasion, or keyword...',
                'search-flowers': 'Search flowers...',
                'close-search': 'Close search',
                'login': 'Login',
                'register': 'Register',
                'profile': 'Profile',
                'orders': 'My Orders',
                'logout': 'Logout',
                'cart': 'Cart',
                
                // Categories
                'birthday-flowers': 'Birthday Flowers',
                'love-flowers': 'Love Flowers',
                'wedding-flowers': 'Wedding Flowers',
                'opening-flowers': 'Opening Flowers',
                'sympathy-flowers': 'Sympathy Flowers',
                'mother-flowers': 'Mother Flowers',
                
                // Home page
                'hero-title': 'Bringing Flowers to Your Joy',
                'hero-subtitle': 'Discover our premium collection of fresh flowers carefully curated to bring the most special moments in life',
                'explore-products': 'Explore Products',
                'view-catalog': 'View Catalog',
                'fresh-daily': 'Fresh Daily Flowers',
                'hero-title-part1': 'Bringing',
                'hero-title-part2': 'flowers to',
                'hero-title-part3': 'your joy',
                'hero-description': 'Discover our beautiful collection of fresh flowers at StarShop. We bring you exquisite bouquets, specially designed for every memorable moment in your life.',
                'happy-customers': 'Happy Customers',
                'unique-designs': 'Unique Designs',
                'customer-support': 'Customer Support',
                'explore-now': 'Explore Now',
                'view-categories': 'View Categories',
                'rating': '4.9',
                'rating-desc': 'Rating',
                'why-starshop-desc': 'We are committed to bringing you the best experience',
                'featured-categories': 'Featured Categories',
                'featured-categories-desc': 'Find the perfect bouquet for every special occasion',
                'explore-collection': 'Explore Collection',
                'explore': 'Explore',
                'view-all-categories': 'View All Categories',
                'featured-products-desc': 'Most loved bouquets at StarShop',
                'out-of-stock': 'OUT OF STOCK',
                'out-of-stock-badge': 'Out of Stock',
                'low-stock': 'Low Stock',
                'featured': 'Featured',
                'remaining': 'Remaining',
                'view-all-products': 'View All Products',
                'special-offer': 'Special Offer',
                'discount-20': '20% Off',
                'first-order': 'for your first order',
                'promo-desc': 'Welcome to StarShop! Get amazing deals on your first purchase.',
                'shop-now': 'Shop Now',
                'free-shipping': 'Free Shipping',
                'hcm-area': 'Ho Chi Minh City Area',
                'delivery-2h': '2-Hour Delivery',
                'fast-reliable': 'Fast & Reliable',
                'fresh-flowers-100': '100% Fresh Flowers',
                'quality-guaranteed': 'Quality Guaranteed',
                'five-star-rating': '5-Star Rating',
                'from-customers': 'From Customers',
                'customer-reviews': 'Customer Reviews',
                'what-customers-say': 'What Our Customers Say',
                'testimonials-desc': 'Thousands of customers have trusted and been satisfied with StarShop services',
                
                // Why StarShop
                'why-starshop': 'Why Choose StarShop?',
                'fresh-flowers': '100% Fresh Flowers',
                'fresh-desc': 'Direct import fresh flowers guarantee',
                'fast-delivery': 'Fast Delivery',
                'fast-desc': '2-hour delivery in Ho Chi Minh City',
                'professional-design': 'Professional Design',
                'design-desc': 'Professional florist team',
                'support-247': '24/7 Support',
                'support-desc': 'Consultation and support anytime',
                
                // Footer
                'footer-description': 'Where love blooms - We bring you beautiful fresh flowers for every special occasion in your life. High quality, fast delivery.',
                'follow-us': 'Follow us:',
                'footer-products': 'Products',
                'useful-links': 'Useful Links',
                'your-account': 'Your Account',
                'shipping-policy': 'Shipping Policy',
                'returns-refunds': 'Returns & Refunds',
                'flower-care': 'Flower Care Guide',
                'faq': 'FAQ',
                'contact-connect': 'Contact & Connect',
                'business-hours': 'Business Hours:',
                'weekdays': 'Mon - Fri',
                'weekend': 'Sat - Sun',
                'copyright': 'Copyright belongs to',
                'privacy-policy': 'Privacy Policy',
                'terms-of-use': 'Terms of Use',
                
                // Products
                'featured-products': 'Featured Products',
                'quick-add': 'Quick Add',
                'adding': 'Adding...',
                'added': 'Added',
                'add-to-cart': 'Add',
                'add-to-cart-success': 'Product added to cart!',
                'add-to-cart-error': 'Cannot add product to cart',
                
                // Categories page
                'categories-title': 'Discover the World of Flowers',
                'categories-description': 'From romantic roses to delicate daisies, find inspiration for every special moment.',
                'no-categories-found': 'No product categories found at the moment.',
                
                // Products page
                'search-results': 'Search Results',
                'all-products': 'All Products',
                'found': 'Found',
                'products-match': 'matching products',
                'has': 'There are',
                'products-in-category': 'products in this category',
                'fresh-flowers-at-starshop': 'beautiful fresh flower products at StarShop',
                'category': 'Category',
                'all-categories': 'All Categories',
                'search-by-name': 'Search by name',
                'enter-product-name': 'Enter product name...',
                'sort': 'Sort',
                'newest': 'Newest',
                'oldest': 'Oldest',
                'name-az': 'Name A-Z',
                'price-low-high': 'Price Low → High',
                'price-high-low': 'Price High → Low',
                'filter': 'Filter',
                'clear-filter': 'Clear Filter',
                'showing': 'Showing',
                'of-total': 'of',
                'products': 'products',
                'no-products-found': 'No products found',
                'no-match-keyword': 'No products match the keyword',
                'try-different-keyword': 'Try searching with a different keyword',
                'no-products-yet': 'No products available at the moment. Please check back later.',
                'add-to-wishlist': 'Add to Wishlist',
                'new': 'New',
                'reviews': 'reviews',
                'add-to-cart-btn': 'Add to Cart',
                'login-to-buy': 'Login to Buy',
                'product-pagination': 'Product Pagination',
                'previous-page': 'Previous Page',
                'next-page': 'Next Page',
                'page': 'Page',
                
                // Product Detail page
                'product-not-found-title': '404 - Product Not Found!',
                'product-not-found-desc': 'The product you are looking for does not exist or has been deleted.',
                'back-to-products': 'Back to Product List',
                'product-description': 'Product Description',
                'buy-now': 'Buy Now',
                'free-shipping': 'Free Shipping',
                'free-shipping-desc': 'Free shipping in Ho Chi Minh City',
                'fast-delivery': 'Fast Delivery',
                'delivery-2-4h': 'Delivery in 2-4 hours',
                'fresh-flowers-100': '100% Fresh Flowers',
                'fresh-quality-guarantee': 'Guaranteed fresh, high quality flowers',
                'detailed-info': 'Detailed Information',
                'learn-more-product': 'Learn more about the product and care instructions',
                'detailed-description': 'Detailed Description',
                'reviews-tab': 'Reviews',
                'care-guide': 'Care Guide',
                'full-product-info': 'Complete product information',
                'customer-reviews': 'Customer Reviews',
                'customer-experience': 'Share customer experiences about the product',
                
                // Vouchers page
                'vouchers-title': 'Special Discount Codes',
                'vouchers-subtitle': 'Choose the best deal for your order',
                'loading-vouchers': 'Loading vouchers...',
                'error-loading-data': 'Error loading data',
                'cannot-connect-api': 'Cannot connect to Voucher API',
                'try-again': 'Try Again',
                'discount-percent': 'Discount %',
                'discount-amount': 'Discount',
                'times': 'times',
                'unlimited': 'Unlimited',
                'code': 'Code',
                'min-order': 'Min Order',
                'max-discount': 'Max Discount',
                'expiry-date': 'Expiry',
                'copy-code': 'Copy Code',
                'copied': 'Copied!',
                'no-vouchers': 'No vouchers available at the moment',
                'check-back-later': 'Please check back later or contact support',
                
                // Blog page
                'blog-title': 'StarShop Blog',
                'blog-subtitle': 'Discover the art of flower arrangement, fresh flower care and the latest decoration trends',
                'search-articles': 'Search articles...',
                'search': 'Search',
                'all': 'All',
                'flower-care': 'Flower Care',
                'flower-arrangement': 'Flower Arrangement',
                'trends': 'Trends',
                'special-occasions': 'Special Occasions',
                'tips': 'Tips',
                'featured': 'Featured',
                'read-more': 'Read More',
                'art': 'Art',
                'min-read': 'min read',
                'view-more': 'View More',
                'care': 'Care',
                'knowledge': 'Knowledge',
                'newsletter-title': 'Subscribe to Newsletter',
                'newsletter-desc': 'Get the latest articles, flower care tips and special offers via email',
                'subscribe': 'Subscribe',
                
                // Checkout page
                'checkout-title': 'Checkout',
                'checkout-subtitle': 'Complete your order',
                'profile-update-required': 'Profile Update Required',
                'update-info': 'Update Information',
                'manage-addresses': 'Manage Addresses',
                'order-payment-info': 'Order & Payment Information',
                'check-cart-payment': 'Review your cart and select a suitable payment method.',
                'cart': 'Cart',
                'shipping-address': 'Shipping Address',
                'select-shipping-address': 'Select shipping address',
                'payment-method': 'Payment Method',
                'shipping-fee': 'Shipping Fee',
                'calculating-fee': 'Calculating fee...',
                'please-select-address': 'Please select a shipping address',
                'order-notes': 'Order Notes',
                'order-notes-placeholder': 'Additional notes for your order (optional)...',
                'order-notes-example': 'E.g.: preferred delivery time, notes for delivery staff...',
                'voucher-code': 'Voucher Code',
                'enter-code': 'ENTER CODE',
                'apply': 'Apply',
                'place-order': 'Place Order',
                'security-notice': 'Your information is secure and encrypted',
                'please-update-profile': 'Please update your account information',
                'complete-profile-before-order': 'You need to complete your personal information before placing an order.',
                'calculating-shipping-fee': 'Calculating shipping fee...',
                'fast-shipping-ghn': 'Fast Shipping (GHN)',
                'free': 'Free',
                'cannot-calculate-ghn-fee': 'Cannot calculate GHN fee',
                'cannot-calculate-shipping-fee': 'Cannot calculate shipping fee',
                'error-loading-payment-methods': 'Error loading payment methods',
                'cart-empty': 'Cart is empty',
                'no-products-in-cart': 'No products in cart',
                'error-loading-cart': 'Error loading cart',
                'error-loading-order-summary': 'Error loading order summary',
                'order-summary': 'Order Summary',
                'discount': 'Discount',
                'total': 'Total',
                
                // Payment methods
                'payment-method-cod': 'Cash on Delivery',
                'payment-method-momo': 'MoMo Wallet',
                'payment-method-bank': 'Bank Transfer',
                'payment-method-card': 'Credit Card',
                'status-available': 'Available',
                'status-not-supported': 'Not Supported',
                'status-unknown': 'Unknown',
                
                // Cart page
                'cart-title': 'Your Cart',
                'cart-subtitle': 'items in cart',
                'select-all': 'Select all products',
                'cart-summary': 'Cart Summary',
                'unit-price': 'Unit Price',
                'quantity': 'Quantity',
                'subtotal': 'Subtotal',
                'total-quantity': 'Total Quantity',
                'temporary-total': 'Subtotal',
                'final-total': 'Total',
                'selected-items': 'items selected',
                'proceed-checkout': 'Proceed to Checkout',
                'continue-shopping': 'Continue Shopping',
                'clear-cart': 'Clear All',
                'shipping-note': 'Shipping fee will be calculated at checkout',
                'empty-cart-title': 'Empty Cart',
                'empty-cart-desc': 'You have no items in your cart. Explore our amazing products!',
                'explore-products': 'Explore Products',
                
                // Wishlist page
                'wishlist-title': 'Favorite Products',
                'wishlist-empty-title': 'Wishlist is Empty',
                'wishlist-empty-desc': 'Discover beautiful fresh flowers and save your favorite products',
                'wishlist-count': 'saved products',
                'clear-all': 'Clear All',
                'in-stock': 'In Stock',
                'out-of-stock': 'Out of Stock',
                'view-details': 'View Details',
                'remove-from-wishlist': 'Remove from Wishlist',
                'saved': 'Saved',
                'contact-us': 'Contact Us',
                
                // Profile page
                'verified': 'Verified',
                'customer': 'Customer',
                'edit': 'Edit',
                'profile-details': 'Profile Details',
                'phone': 'Phone',
                'join-date': 'Join Date',
                'not-updated': 'Not Updated',
                'unknown': 'Unknown',
                'overview': 'Overview',
                'shipping-address': 'Shipping Address',
                'loading-addresses': 'Loading addresses...',
                'edit-profile': 'Edit Personal Information',
                'firstname': 'First Name',
                'lastname': 'Last Name',
                'enter-firstname': 'Enter your first name',
                'enter-lastname': 'Enter your last name',
                'enter-phone': 'Enter phone number (10 digits)',
                'phone-format': 'Format: 10 digits (e.g: 0912345678)',
                'email-cannot-change': 'Email cannot be changed',
                'cancel': 'Cancel',
                'save-changes': 'Save Changes',
                'add-address-title': 'Add Shipping Address',
                'choose-address-mode': 'Choose address input method',
                'address-3-level': '3 levels (Province/District/Ward)',
                'address-2-level': '2 levels (Province/Ward)',
                'province': 'Province/City',
                'district': 'District',
                'ward': 'Ward',
                'select-province': '-- Select Province/City --',
                'select-district': '-- Select District --',
                'select-ward': '-- Select Ward --',
                'address-detail': 'Detail Address',
                'enter-address-detail': 'House number, street name...',
                'set-as-default': 'Set as default address',
                'default-address': 'Default',
                'supports-fast-delivery': 'Supports fast delivery',
                'no-address': 'No shipping address',
                'add-first-address': 'Add first address',
                'add-address': 'Add Address',
                'error-loading-addresses': 'Unable to load address list',
                'error-loading-provinces': 'Unable to load provinces list',
                'error-loading-districts': 'Unable to load districts list',
                'error-loading-wards': 'Unable to load wards list',
                'processing': 'Processing...',
                'address-added-success': 'Address added successfully!',
                'error-occurred': 'An error occurred',
                'cannot-add-address': 'Unable to add address. Please try again.',
                'confirm-delete-address': 'Are you sure you want to delete this address?',
                'address-deleted-success': 'Address deleted successfully!',
                'cannot-delete-address': 'Unable to delete address',
                'cannot-delete-address-retry': 'Unable to delete address. Please try again.',
                
                // Orders page
                'my-orders-title': 'Your Orders',
                'my-orders-desc': 'Track delivery status, view purchase history and manage your orders easily.',
                'all': 'All',
                'pending': 'Pending',
                'processing': 'Processing',
                'shipping': 'Shipping',
                'shipped': 'Shipping',
                'completed': 'Completed',
                'cancelled': 'Cancelled',
                'loading-orders': 'Loading orders...',
                'review-product': 'Review Product',
                'review-after-purchase': 'Review product after purchase',
                'your-rating': 'Your Rating *',
                'choose-rating': 'Choose stars to rate',
                'comment-optional': 'Comment (optional)',
                'share-your-experience': 'Share your experience with this product...',
                'max-1000-chars': 'Maximum 1000 characters',
                'submit-review': 'Submit Review',
                'cancel-order': 'Cancel Order',
                'details': 'Details',
                'total-payment': 'Total Payment',
                'quantity': 'Quantity',
                'review': 'Review',
                'reorder': 'Reorder',
                'very-poor': 'Very Poor',
                'poor': 'Poor',
                'average': 'Average',
                'good': 'Good',
                'excellent': 'Excellent',
                
                // Order detail page
                'order-detail-badge': 'Order Detail',
                'order-title': 'Order',
                'order-placed-at': 'Placed at',
                'back': 'Back',
                'payment-success': 'Payment Successful!',
                'transaction-id': 'Transaction ID',
                'payment-failed': 'Payment Failed',
                'order-status': 'Order Status',
                'current-status': 'Current Status',
                'products-ordered': 'Products Ordered',
                'subtotal': 'Subtotal',
                'shipping-fee': 'Shipping Fee',
                'shipping-info': 'Shipping Information',
                'recipient': 'Recipient',
                'delivery-address': 'Delivery Address',
                'notes': 'Notes',
                'payment-method': 'Payment Method',
                
                // Footer
                'language': 'Language',
                'vietnamese': 'Vietnamese',
                'english': 'English'
            }
        };
    }

    // Update language display in switcher
    updateLanguageDisplay() {
        // Update language display button (VI/EN)
        const languageDisplays = document.querySelectorAll('.language-display');
        languageDisplays.forEach(display => {
            display.textContent = this.currentLanguage.toUpperCase();
        });

        // Update active state in dropdown
        document.querySelectorAll('.language-option').forEach(option => {
            option.classList.remove('active');
            if (option.dataset.lang === this.currentLanguage) {
                option.classList.add('active');
            }
        });

        document.querySelectorAll('.mobile-language-option').forEach(option => {
            option.classList.remove('active');
            if (option.dataset.lang === this.currentLanguage) {
                option.classList.add('active');
            }
        });
    }

    // Bind click events
    bindEvents() {
        // Desktop language options
        document.querySelectorAll('.language-option').forEach(option => {
            option.addEventListener('click', (e) => {
                e.preventDefault();
                this.switchLanguage(option.dataset.lang);
            });
        });

        // Mobile language options
        document.querySelectorAll('.mobile-language-option').forEach(option => {
            option.addEventListener('click', (e) => {
                e.preventDefault();
                this.switchLanguage(option.dataset.lang);
            });
        });
    }

    // Switch language
    switchLanguage(language) {
        if (language === this.currentLanguage) return;
        
        this.currentLanguage = language;
        localStorage.setItem('starshop-language', language);
        
        this.updateLanguageDisplay();
        this.applyLanguage();
        
        // Re-render dynamic content if pages exist
        this.reRenderDynamicContent();
        
        // Show notification
        this.showLanguageChangeNotification();
    }
    
    // Re-render dynamic content that was loaded via AJAX
    reRenderDynamicContent() {
        // Re-render payment methods if on checkout page
        if (typeof window.loadPaymentMethods === 'function') {
            setTimeout(() => window.loadPaymentMethods(), 100);
        }
        
        // Re-render order summary if on checkout page
        if (typeof window.displayOrderSummary === 'function' && window.cartData) {
            setTimeout(() => window.displayOrderSummary(window.cartData), 100);
        }
        
        // Re-render cart items if on checkout page
        if (typeof window.displayCartItems === 'function' && window.cartData) {
            setTimeout(() => window.displayCartItems(window.cartData), 100);
        }
        
        // Re-render wishlist items if on wishlist page
        if (typeof loadWishlist === 'function' && window.location.pathname === '/wishlist') {
            setTimeout(() => loadWishlist(), 100);
        }
        
        // Re-render addresses if on profile page
        if (typeof window.loadAddresses === 'function' && window.location.pathname === '/account/profile') {
            setTimeout(() => {
                window.loadAddresses();
                // Apply language again after dynamic content is loaded
                setTimeout(() => this.applyLanguage(), 200);
            }, 100);
        }
        
        // Re-render orders if on orders page
        if (typeof window.loadOrders === 'function' && window.location.pathname === '/account/orders') {
            setTimeout(() => {
                window.loadOrders(window.currentStatus || 'all', window.currentPage || 0);
            }, 100);
        }
    }

    // Apply language to page elements
    applyLanguage() {
        const translations = this.translations[this.currentLanguage];
        
        // Update elements with data-translate attribute
        document.querySelectorAll('[data-translate]').forEach(element => {
            const key = element.dataset.translate;
            if (translations[key]) {
                if (element.tagName === 'INPUT' && element.type === 'text') {
                    element.placeholder = translations[key];
                } else {
                    element.textContent = translations[key];
                }
            }
        });

        // Update elements with data-translate-placeholder attribute
        document.querySelectorAll('[data-translate-placeholder]').forEach(element => {
            const key = element.dataset.translatePlaceholder;
            if (translations[key]) {
                element.placeholder = translations[key];
            }
        });

        // Update elements with data-translate-title attribute
        document.querySelectorAll('[data-translate-title]').forEach(element => {
            const key = element.dataset.translateTitle;
            if (translations[key]) {
                element.title = translations[key];
            }
        });

        // Update order status badge on detail page
        const orderStatusBadge = document.getElementById('orderStatusBadge');
        if (orderStatusBadge) {
            const status = orderStatusBadge.getAttribute('data-status');
            if (status) {
                const statusKey = status.toLowerCase();
                if (translations[statusKey]) {
                    const span = orderStatusBadge.querySelector('span');
                    if (span) {
                        span.textContent = translations[statusKey];
                    }
                }
            }
        }

        // Update specific elements by ID or class
        this.updateSpecificElements(translations);
    }

    // Update specific elements that need special handling
    updateSpecificElements(translations) {
        // Search input placeholder
        const searchInputs = document.querySelectorAll('.search-input-hero, .search-input');
        searchInputs.forEach(input => {
            input.placeholder = translations['search-placeholder'];
        });

        // User role text
        const userRoles = document.querySelectorAll('.user-role');
        userRoles.forEach(role => {
            role.textContent = translations['customer-role'];
        });

        // Update page title
        if (document.title.includes('StarShop')) {
            document.title = this.currentLanguage === 'vi' ? 
                'StarShop - Cửa hàng hoa tươi' : 
                'StarShop - Fresh Flower Shop';
        }
    }

    // Show language change notification
    showLanguageChangeNotification() {
        const message = this.currentLanguage === 'vi' ? 
            'Đã chuyển sang Tiếng Việt' : 
            'Switched to English';
        
        // Use existing toast function if available
        if (typeof showToast === 'function') {
            showToast(message, 'success');
        } else {
            // Fallback notification
            const notification = document.createElement('div');
            notification.className = 'language-notification';
            notification.textContent = message;
            notification.style.cssText = `
                position: fixed;
                top: 20px;
                right: 20px;
                background: var(--primary);
                color: white;
                padding: 12px 20px;
                border-radius: 8px;
                z-index: 9999;
                font-size: 14px;
                font-weight: 500;
                box-shadow: 0 4px 12px rgba(0,0,0,0.15);
                transform: translateX(100%);
                transition: transform 0.3s ease;
            `;
            
            document.body.appendChild(notification);
            
            // Animate in
            setTimeout(() => {
                notification.style.transform = 'translateX(0)';
            }, 100);
            
            // Remove after 3 seconds
            setTimeout(() => {
                notification.style.transform = 'translateX(100%)';
                setTimeout(() => {
                    document.body.removeChild(notification);
                }, 300);
            }, 3000);
        }
    }

    // Get current language
    getCurrentLanguage() {
        return this.currentLanguage;
    }

    // Get translation for a key
    translate(key) {
        return this.translations[this.currentLanguage][key] || key;
    }
}

// Initialize language switcher when DOM is ready
document.addEventListener('DOMContentLoaded', function() {
    window.languageSwitcher = new LanguageSwitcher();
});

// Export for use in other scripts
if (typeof module !== 'undefined' && module.exports) {
    module.exports = LanguageSwitcher;
}
