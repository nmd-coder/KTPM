package com.example.demo.service;

import org.springframework.stereotype.Component;

/**
 * Beautiful email templates for marketing campaigns
 * Using modern inline CSS (Tailwind-inspired)
 */
@Component
public class MarketingEmailTemplates {
    
    /**
     * VIP Campaign - Tri ân khách hàng VIP
     */
    public String buildVipTemplate(String customerName, String voucherCode, String expiryDate, int discountValue, String discountType) {
        String discountDisplay = discountType.equals("PERCENTAGE") ? discountValue + "%" : discountValue + "₫";
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Tri Ân Khách Hàng VIP</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background: linear-gradient(135deg, #FFD700 0%%, #FFA500 100%%);">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="background: #FFFFF0; border-radius: 20px; overflow: hidden; box-shadow: 0 20px 40px rgba(0,0,0,0.15);">
                        <!-- Header -->
                        <div style="background: linear-gradient(135deg, #FFD700 0%%, #FFA500 100%%); padding: 50px 30px; text-align: center; position: relative;">
                            <div style="font-size: 48px; margin-bottom: 10px;">👑</div>
                            <h1 style="color: white; font-size: 32px; font-weight: 700; margin: 0 0 10px 0;">KHÁCH HÀNG VIP</h1>
                            <p style="color: rgba(255,255,255,0.95); font-size: 18px; margin: 0;">Tri ân đặc biệt dành riêng cho bạn</p>
                        </div>
                        
                        <!-- Content -->
                        <div style="padding: 40px 30px;">
                            <p style="font-size: 20px; color: #2D3748; margin: 0 0 20px 0; text-align: center;">Kính gửi <strong>%s</strong>,</p>
                            
                            <p style="font-size: 16px; color: #4A5568; line-height: 1.8; margin-bottom: 30px; text-align: center;">
                                Cảm ơn bạn đã luôn tin tưởng và đồng hành cùng StarShop! 🌟<br>
                                Với tư cách là <strong style="color: #FFD700;">khách hàng VIP</strong>, chúng tôi xin gửi tặng bạn ưu đãi đặc biệt:
                            </p>
                            
                            <!-- Voucher Card -->
                            <div style="background: white; border-radius: 16px; padding: 30px; margin: 30px 0; box-shadow: 0 10px 30px rgba(255,215,0,0.2); border: 2px dashed #FFD700; text-align: center;">
                                <div style="font-size: 14px; color: #718096; font-weight: 600; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 15px;">Mã Giảm Giá Độc Quyền</div>
                                <div style="font-size: 42px; font-weight: 800; color: #FFD700; letter-spacing: 4px; margin: 20px 0; font-family: monospace;">%s</div>
                                <div style="font-size: 28px; font-weight: 700; color: #FFA500; margin: 15px 0;">GIẢM %s</div>
                                <div style="font-size: 14px; color: #E53E3E; margin-top: 15px; padding: 10px; background: #FFF5F5; border-radius: 8px;">
                                    ⏰ Có hiệu lực đến: <strong>%s</strong>
                                </div>
                            </div>
                            
                            <!-- Benefits Grid -->
                            <div style="margin: 30px 0;">
                                <h3 style="color: #2D3748; font-size: 18px; font-weight: 600; margin-bottom: 20px; text-align: center;">🎁 Quyền lợi VIP của bạn</h3>
                                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px;">
                                    <div style="background: #FFF9E6; padding: 20px; border-radius: 12px; text-align: center;">
                                        <div style="font-size: 28px; margin-bottom: 8px;">🚚</div>
                                        <div style="font-size: 14px; color: #2D3748; font-weight: 600;">Miễn phí ship</div>
                                        <div style="font-size: 12px; color: #718096;">Mọi đơn hàng</div>
                                    </div>
                                    <div style="background: #FFF9E6; padding: 20px; border-radius: 12px; text-align: center;">
                                        <div style="font-size: 28px; margin-bottom: 8px;">⚡</div>
                                        <div style="font-size: 14px; color: #2D3748; font-weight: 600;">Ưu tiên xử lý</div>
                                        <div style="font-size: 12px; color: #718096;">Giao nhanh hơn</div>
                                    </div>
                                    <div style="background: #FFF9E6; padding: 20px; border-radius: 12px; text-align: center;">
                                        <div style="font-size: 28px; margin-bottom: 8px;">🎂</div>
                                        <div style="font-size: 14px; color: #2D3748; font-weight: 600;">Quà sinh nhật</div>
                                        <div style="font-size: 12px; color: #718096;">Voucher 30%%</div>
                                    </div>
                                    <div style="background: #FFF9E6; padding: 20px; border-radius: 12px; text-align: center;">
                                        <div style="font-size: 28px; margin-bottom: 8px;">💎</div>
                                        <div style="font-size: 14px; color: #2D3748; font-weight: 600;">Sản phẩm độc quyền</div>
                                        <div style="font-size: 12px; color: #718096;">Mua trước</div>
                                    </div>
                                </div>
                            </div>
                            
                            <!-- CTA Button -->
                            <div style="text-align: center; margin: 35px 0;">
                                <a href="http://localhost:8080/catalog" style="display: inline-block; background: linear-gradient(135deg, #FFD700, #FFA500); color: white; text-decoration: none; padding: 18px 40px; border-radius: 12px; font-weight: 700; font-size: 18px; box-shadow: 0 6px 20px rgba(255,215,0,0.4);">
                                    ✨ MUA SẮM NGAY
                                </a>
                            </div>
                            
                            <p style="font-size: 14px; color: #718096; text-align: center; margin-top: 30px; font-style: italic;">
                                "Những khách hàng như bạn chính là động lực để chúng tôi ngày càng hoàn thiện hơn" ❤️
                            </p>
                        </div>
                        
                        <!-- Footer -->
                        <div style="background: #F7FAFC; padding: 30px; text-align: center; border-top: 1px solid #E2E8F0;">
                            <p style="font-size: 12px; color: #718096; margin: 5px 0;">© 2024 StarShop - Nơi kết nối yêu thương</p>
                            <p style="font-size: 12px; color: #718096; margin: 5px 0;">💌 Hotline: 1900-xxxx | Email: support@starshop.vn</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """, customerName, voucherCode, discountDisplay, expiryDate);
    }
    
    /**
     * NEW Customer Campaign - Chào mừng khách hàng mới
     */
    public String buildNewCustomerTemplate(String customerName, String voucherCode, String expiryDate, int discountValue, String discountType) {
        String discountDisplay = discountType.equals("PERCENTAGE") ? discountValue + "%" : discountValue + "₫";
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Chào Mừng Thành Viên Mới</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="background: #F8F9FF; border-radius: 20px; overflow: hidden; box-shadow: 0 20px 40px rgba(0,0,0,0.15);">
                        <!-- Header -->
                        <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 50px 30px; text-align: center;">
                            <div style="font-size: 48px; margin-bottom: 10px;">🎉</div>
                            <h1 style="color: white; font-size: 32px; font-weight: 700; margin: 0 0 10px 0;">CHÀO MỪNG BẠN!</h1>
                            <p style="color: rgba(255,255,255,0.95); font-size: 18px; margin: 0;">Bắt đầu hành trình mua sắm tuyệt vời</p>
                        </div>
                        
                        <!-- Content -->
                        <div style="padding: 40px 30px;">
                            <p style="font-size: 20px; color: #2D3748; margin: 0 0 20px 0; text-align: center;">Xin chào <strong>%s</strong>! 👋</p>
                            
                            <p style="font-size: 16px; color: #4A5568; line-height: 1.8; margin-bottom: 30px; text-align: center;">
                                Chào mừng bạn đến với <strong style="color: #667eea;">gia đình StarShop</strong>! 🌸<br>
                                Để chào đón bạn, chúng tôi xin tặng món quà đặc biệt cho đơn hàng đầu tiên:
                            </p>
                            
                            <!-- Voucher Card -->
                            <div style="background: white; border-radius: 16px; padding: 30px; margin: 30px 0; box-shadow: 0 10px 30px rgba(102,126,234,0.2); border: 2px dashed #667eea; text-align: center;">
                                <div style="font-size: 14px; color: #718096; font-weight: 600; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 15px;">🎁 Quà Tặng Chào Mừng</div>
                                <div style="font-size: 42px; font-weight: 800; color: #667eea; letter-spacing: 4px; margin: 20px 0; font-family: monospace;">%s</div>
                                <div style="font-size: 28px; font-weight: 700; color: #764ba2; margin: 15px 0;">GIẢM %s</div>
                                <div style="font-size: 14px; color: #E53E3E; margin-top: 15px; padding: 10px; background: #FFF5F5; border-radius: 8px;">
                                    ⏰ Có hiệu lực đến: <strong>%s</strong>
                                </div>
                            </div>
                            
                            <!-- Why Choose Us -->
                            <div style="background: #F0F4FF; border-radius: 12px; padding: 25px; margin: 30px 0;">
                                <h3 style="color: #2D3748; font-size: 18px; font-weight: 600; margin: 0 0 20px 0; text-align: center;">💝 Tại sao chọn StarShop?</h3>
                                <div style="margin: 15px 0;">
                                    <div style="display: flex; align-items: start; margin-bottom: 15px;">
                                        <div style="font-size: 24px; margin-right: 15px;">✨</div>
                                        <div>
                                            <div style="font-size: 15px; color: #2D3748; font-weight: 600;">Hoa tươi 100%%</div>
                                            <div style="font-size: 13px; color: #718096;">Nhập khẩu trực tiếp, đảm bảo chất lượng</div>
                                        </div>
                                    </div>
                                    <div style="display: flex; align-items: start; margin-bottom: 15px;">
                                        <div style="font-size: 24px; margin-right: 15px;">🚚</div>
                                        <div>
                                            <div style="font-size: 15px; color: #2D3748; font-weight: 600;">Giao hàng nhanh 2-4h</div>
                                            <div style="font-size: 13px; color: #718096;">Miễn phí ship cho đơn từ 500k</div>
                                        </div>
                                    </div>
                                    <div style="display: flex; align-items: start;">
                                        <div style="font-size: 24px; margin-right: 15px;">💯</div>
                                        <div>
                                            <div style="font-size: 15px; color: #2D3748; font-weight: 600;">Đổi trả trong 24h</div>
                                            <div style="font-size: 13px; color: #718096;">Nếu không hài lòng về chất lượng</div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            
                            <!-- CTA Button -->
                            <div style="text-align: center; margin: 35px 0;">
                                <a href="http://localhost:8080/catalog" style="display: inline-block; background: linear-gradient(135deg, #667eea, #764ba2); color: white; text-decoration: none; padding: 18px 40px; border-radius: 12px; font-weight: 700; font-size: 18px; box-shadow: 0 6px 20px rgba(102,126,234,0.4);">
                                    🛍️ KHÁM PHÁ NGAY
                                </a>
                            </div>
                            
                            <p style="font-size: 14px; color: #718096; text-align: center; margin-top: 30px;">
                                Bạn cần hỗ trợ? Liên hệ ngay với chúng tôi qua Hotline hoặc Chat!
                            </p>
                        </div>
                        
                        <!-- Footer -->
                        <div style="background: #F7FAFC; padding: 30px; text-align: center; border-top: 1px solid #E2E8F0;">
                            <p style="font-size: 12px; color: #718096; margin: 5px 0;">© 2024 StarShop - Nơi kết nối yêu thương</p>
                            <p style="font-size: 12px; color: #718096; margin: 5px 0;">💌 Hotline: 1900-xxxx | Email: support@starshop.vn</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """, customerName, voucherCode, discountDisplay, expiryDate);
    }
    
    /**
     * Win-back Campaign - Nhớ khách hàng cũ
     */
    public String buildWinBackTemplate(String customerName, String voucherCode, String expiryDate, int discountValue, String discountType) {
        String discountDisplay = discountType.equals("PERCENTAGE") ? discountValue + "%" : discountValue + "₫";
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Chúng Tôi Nhớ Bạn</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: linear-gradient(135deg, #f093fb 0%%, #f5576c 100%%);">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="background: #FFF5F7; border-radius: 20px; overflow: hidden; box-shadow: 0 20px 40px rgba(0,0,0,0.15);">
                        <!-- Header -->
                        <div style="background: linear-gradient(135deg, #f093fb 0%%, #f5576c 100%%); padding: 50px 30px; text-align: center;">
                            <div style="font-size: 48px; margin-bottom: 10px;">💐</div>
                            <h1 style="color: white; font-size: 32px; font-weight: 700; margin: 0 0 10px 0;">CHÚNG TÔI NHỚ BẠN!</h1>
                            <p style="color: rgba(255,255,255,0.95); font-size: 18px; margin: 0;">Hãy quay trở lại với chúng tôi</p>
                        </div>
                        
                        <!-- Content -->
                        <div style="padding: 40px 30px;">
                            <p style="font-size: 20px; color: #2D3748; margin: 0 0 20px 0; text-align: center;">Kính gửi <strong>%s</strong>,</p>
                            
                            <p style="font-size: 16px; color: #4A5568; line-height: 1.8; margin-bottom: 30px; text-align: center;">
                                Đã lâu rồi chúng tôi không gặp bạn... 🥺<br>
                                StarShop rất nhớ bạn và mong được phục vụ bạn một lần nữa!<br>
                                <strong style="color: #f5576c;">Hãy quay trở lại</strong> và nhận ưu đãi đặc biệt này nhé:
                            </p>
                            
                            <!-- Voucher Card -->
                            <div style="background: white; border-radius: 16px; padding: 30px; margin: 30px 0; box-shadow: 0 10px 30px rgba(245,87,108,0.2); border: 2px dashed #f5576c; text-align: center;">
                                <div style="font-size: 14px; color: #718096; font-weight: 600; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 15px;">💝 Quà Tặng Đặc Biệt</div>
                                <div style="font-size: 42px; font-weight: 800; color: #f5576c; letter-spacing: 4px; margin: 20px 0; font-family: monospace;">%s</div>
                                <div style="font-size: 28px; font-weight: 700; color: #f093fb; margin: 15px 0;">GIẢM %s</div>
                                <div style="font-size: 14px; color: #E53E3E; margin-top: 15px; padding: 10px; background: #FFF5F5; border-radius: 8px;">
                                    ⏰ Có hiệu lực đến: <strong>%s</strong>
                                </div>
                            </div>
                            
                            <!-- What's New -->
                            <div style="background: #FFF0F5; border-radius: 12px; padding: 25px; margin: 30px 0;">
                                <h3 style="color: #2D3748; font-size: 18px; font-weight: 600; margin: 0 0 20px 0; text-align: center;">🌟 Có gì mới tại StarShop?</h3>
                                <div style="margin: 15px 0;">
                                    <div style="padding: 12px; margin-bottom: 10px; background: white; border-radius: 8px; border-left: 4px solid #f5576c;">
                                        <div style="font-size: 15px; color: #2D3748; font-weight: 600;">🌹 Bộ sưu tập hoa mới</div>
                                        <div style="font-size: 13px; color: #718096;">Hơn 200+ mẫu hoa tươi độc đáo vừa cập nhật</div>
                                    </div>
                                    <div style="padding: 12px; margin-bottom: 10px; background: white; border-radius: 8px; border-left: 4px solid #f093fb;">
                                        <div style="font-size: 15px; color: #2D3748; font-weight: 600;">🎁 Dịch vụ quà tặng kèm</div>
                                        <div style="font-size: 13px; color: #718096;">Thiệp, bong bóng, gấu bông miễn phí</div>
                                    </div>
                                    <div style="padding: 12px; background: white; border-radius: 8px; border-left: 4px solid #f5576c;">
                                        <div style="font-size: 15px; color: #2D3748; font-weight: 600;">⚡ Giao hàng siêu tốc 1-2h</div>
                                        <div style="font-size: 13px; color: #718096;">Trong nội thành, đặt là có ngay</div>
                                    </div>
                                </div>
                            </div>
                            
                            <!-- CTA Button -->
                            <div style="text-align: center; margin: 35px 0;">
                                <a href="http://localhost:8080/catalog" style="display: inline-block; background: linear-gradient(135deg, #f093fb, #f5576c); color: white; text-decoration: none; padding: 18px 40px; border-radius: 12px; font-weight: 700; font-size: 18px; box-shadow: 0 6px 20px rgba(245,87,108,0.4);">
                                    💕 QUAY TRỞ LẠI NGAY
                                </a>
                            </div>
                            
                            <div style="background: #FFF0F5; border-radius: 12px; padding: 20px; margin-top: 30px; text-align: center;">
                                <p style="font-size: 14px; color: #2D3748; margin: 0; line-height: 1.6;">
                                    <strong style="color: #f5576c;">Lời hứa của chúng tôi:</strong><br>
                                    Chất lượng tốt hơn • Giá cả tốt hơn • Dịch vụ tốt hơn
                                </p>
                            </div>
                        </div>
                        
                        <!-- Footer -->
                        <div style="background: #F7FAFC; padding: 30px; text-align: center; border-top: 1px solid #E2E8F0;">
                            <p style="font-size: 12px; color: #718096; margin: 5px 0;">© 2024 StarShop - Nơi kết nối yêu thương</p>
                            <p style="font-size: 12px; color: #718096; margin: 5px 0;">💌 Hotline: 1900-xxxx | Email: support@starshop.vn</p>
                            <p style="font-size: 11px; color: #A0AEC0; margin: 10px 0;">Bạn nhận email này vì đã từng mua hàng tại StarShop</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """, customerName, voucherCode, discountDisplay, expiryDate);
    }
    
    /**
     * Generic campaign template
     */
    public String buildGenericTemplate(String customerName, String voucherCode, String expiryDate, 
                                      String campaignName, int discountValue, String discountType) {
        String discountDisplay = discountType.equals("PERCENTAGE") ? discountValue + "%" : discountValue + "₫";
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: linear-gradient(135deg, #FFB3D9 0%%, #C7A3E8 100%%);">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="background: #FFF9F5; border-radius: 20px; overflow: hidden; box-shadow: 0 20px 40px rgba(0,0,0,0.15);">
                        <div style="background: linear-gradient(135deg, #FFB3D9 0%%, #C7A3E8 100%%); padding: 50px 30px; text-align: center;">
                            <h1 style="color: white; font-size: 32px; font-weight: 700; margin: 0;">%s</h1>
                        </div>
                        <div style="padding: 40px 30px;">
                            <p style="font-size: 18px; color: #2D3748; text-align: center;">Xin chào <strong>%s</strong>!</p>
                            <div style="background: white; border-radius: 16px; padding: 30px; margin: 30px 0; box-shadow: 0 10px 30px rgba(199,163,232,0.2); text-align: center;">
                                <div style="font-size: 42px; font-weight: 800; color: #C7A3E8; letter-spacing: 4px; margin: 20px 0;">%s</div>
                                <div style="font-size: 28px; font-weight: 700; color: #FFB3D9; margin: 15px 0;">GIẢM %s</div>
                                <div style="font-size: 14px; color: #E53E3E; margin-top: 15px;">⏰ HSD: %s</div>
                            </div>
                            <div style="text-align: center;">
                                <a href="http://localhost:8080/catalog" style="display: inline-block; background: linear-gradient(135deg, #FFB3D9, #C7A3E8); color: white; text-decoration: none; padding: 18px 40px; border-radius: 12px; font-weight: 700; font-size: 18px;">MUA SẮM NGAY</a>
                            </div>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """, campaignName, campaignName, customerName, voucherCode, discountDisplay, expiryDate);
    }
}
