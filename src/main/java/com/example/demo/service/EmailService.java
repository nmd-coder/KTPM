package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    @Value("${spring.mail.from:starshop.a.6868@gmail.com}")
    private String fromEmail;
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private MarketingEmailTemplates marketingTemplates;
    
    /**
     * Send OTP email with HTML template
     */
    public void sendOtpEmail(String toEmail, String otp, String firstName) {
        try {
            logger.info("=== SENDING OTP EMAIL ===");
            logger.info("From: {} (StarShop 🌸)", fromEmail);
            logger.info("To: {}", toEmail);
            logger.info("Subject: 🌸 Mã xác thực StarShop - {}", otp);
            logger.info("OTP Code: {}", otp);
            logger.info("User: {}", firstName != null ? firstName : "N/A");
            
            String htmlContent = buildOtpEmailTemplate(otp, firstName);
            logger.info("Email content generated successfully (HTML length: {} chars)", htmlContent.length());
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, "StarShop 🌸");
            helper.setTo(toEmail);
            helper.setSubject("🌸 Mã xác thực StarShop - " + otp);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            
            logger.info("✅ OTP email sent successfully to: {}", toEmail);
            
        } catch (Exception e) {
            logger.error("❌ Failed to send OTP email to: {} - Error: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }
    
    /**
     * Send welcome email after successful registration
     */
    public void sendWelcomeEmail(String toEmail, String firstName) {
        try {
            logger.info("=== SENDING WELCOME EMAIL ===");
            logger.info("From: {} (StarShop 🌸)", fromEmail);
            logger.info("To: {}", toEmail);
            logger.info("Subject: 🌸 Chào mừng bạn đến với StarShop!");
            logger.info("User: {}", firstName != null ? firstName : "N/A");
            
            String htmlContent = buildWelcomeEmailTemplate(firstName);
            logger.info("Welcome email content generated (HTML length: {} chars)", htmlContent.length());
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, "StarShop 🌸");
            helper.setTo(toEmail);
            helper.setSubject("🌸 Chào mừng bạn đến với StarShop!");
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            
            logger.info("✅ Welcome email sent successfully to: {}", toEmail);
            
        } catch (Exception e) {
            logger.error("❌ Failed to send welcome email to: {} - Error: {}", toEmail, e.getMessage());
            // Don't throw exception for welcome email as it's not critical
        }
    }
    
    /**
     * Send password reset confirmation email
     */
    public void sendPasswordResetConfirmationEmail(String toEmail, String firstName) {
        try {
            logger.info("=== SENDING PASSWORD RESET CONFIRMATION EMAIL ===");
            logger.info("From: {} (StarShop 🌸)", fromEmail);
            logger.info("To: {}", toEmail);
            logger.info("Subject: 🔒 Mật khẩu đã được đặt lại thành công");
            logger.info("User: {}", firstName != null ? firstName : "N/A");
            
            String htmlContent = buildPasswordResetConfirmationTemplate(firstName);
            logger.info("Password reset confirmation email content generated (HTML length: {} chars)", htmlContent.length());
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, "StarShop 🌸");
            helper.setTo(toEmail);
            helper.setSubject("🔒 Mật khẩu đã được đặt lại thành công");
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            
            logger.info("✅ Password reset confirmation email sent successfully to: {}", toEmail);
            
        } catch (Exception e) {
            logger.error("❌ Failed to send password reset confirmation email to: {} - Error: {}", toEmail, e.getMessage());
            // Don't throw exception as password reset was successful
        }
    }
    
    private String buildOtpEmailTemplate(String otp, String firstName) {
        String currentDateTime = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>StarShop - Mã xác thực</title>
                <style>
                    @import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap');
                    
                    body {
                        font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        line-height: 1.6;
                        color: #2D3748;
                        margin: 0;
                        padding: 0;
                        background: linear-gradient(135deg, #FFB3D9 0%%, #C7A3E8 100%%);
                        min-height: 100vh;
                    }
                    
                    .email-wrapper {
                        max-width: 600px;
                        margin: 0 auto;
                        padding: 20px;
                    }
                    
                    .container {
                        background: #FFF9F5;
                        border-radius: 24px;
                        overflow: hidden;
                        box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
                    }
                    
                    .header {
                        background: linear-gradient(135deg, #f43f5e 0%%, #ec4899 100%%);
                        padding: 50px 30px;
                        text-align: center;
                        color: white;
                        position: relative;
                    }
                    
                    .header::after {
                        content: '';
                        position: absolute;
                        bottom: 0;
                        left: 0;
                        right: 0;
                        height: 4px;
                        background: linear-gradient(90deg, #f43f5e, #ec4899, #f43f5e);
                    }
                    
                    .logo {
                        font-size: 36px;
                        font-weight: 800;
                        margin-bottom: 10px;
                        letter-spacing: -1px;
                        text-shadow: 0 2px 10px rgba(0,0,0,0.1);
                    }
                    
                    .tagline {
                        font-size: 16px;
                        opacity: 0.9;
                        font-weight: 400;
                        margin: 0;
                    }
                    
                    .content {
                        padding: 40px 30px;
                    }
                    
                    .greeting {
                        font-size: 24px;
                        font-weight: 600;
                        color: #2D3748;
                        margin-bottom: 16px;
                        text-align: center;
                    }
                    
                    .description {
                        font-size: 16px;
                        color: #4A5568;
                        text-align: center;
                        margin-bottom: 32px;
                        line-height: 1.7;
                    }
                    
                    .otp-card {
                        background: white;
                        border-radius: 16px;
                        padding: 32px;
                        margin: 24px 0;
                        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
                        border: 1px solid #E2E8F0;
                        text-align: center;
                    }
                    
                    .otp-label {
                        font-size: 14px;
                        color: #718096;
                        font-weight: 500;
                        margin-bottom: 16px;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                    }
                    
                    .otp-code {
                        font-size: 42px;
                        font-weight: 800;
                        letter-spacing: 14px;
                        color: #2D3748;
                        margin: 20px 0;
                        font-family: 'SF Mono', Monaco, 'Cascadia Code', monospace;
                        background: linear-gradient(135deg, #f43f5e, #ec4899);
                        -webkit-background-clip: text;
                        -webkit-text-fill-color: transparent;
                        background-clip: text;
                        text-shadow: 0 4px 12px rgba(244, 63, 94, 0.2);
                    }
                    
                    .expiry-info {
                        font-size: 14px;
                        color: #E53E3E;
                        font-weight: 500;
                        margin-top: 16px;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        gap: 8px;
                    }
                    
                    .cta-button {
                        display: inline-block;
                        background: linear-gradient(135deg, #f43f5e 0%%, #ec4899 100%%);
                        color: white;
                        text-decoration: none;
                        padding: 18px 40px;
                        border-radius: 14px;
                        font-weight: 700;
                        font-size: 17px;
                        margin: 28px 0;
                        transition: all 0.3s ease;
                        box-shadow: 0 8px 24px rgba(244, 63, 94, 0.35);
                        letter-spacing: 0.3px;
                    }
                    
                    .security-card {
                        background: #F7FAFC;
                        border: 1px solid #E2E8F0;
                        border-radius: 12px;
                        padding: 24px;
                        margin: 24px 0;
                    }
                    
                    .security-title {
                        font-size: 16px;
                        font-weight: 600;
                        color: #2D3748;
                        margin-bottom: 12px;
                        display: flex;
                        align-items: center;
                        gap: 8px;
                    }
                    
                    .security-list {
                        list-style: none;
                        padding: 0;
                        margin: 0;
                    }
                    
                    .security-list li {
                        font-size: 14px;
                        color: #4A5568;
                        margin: 8px 0;
                        padding-left: 20px;
                        position: relative;
                    }
                    
                    .security-list li:before {
                        content: "•";
                        color: #f43f5e;
                        font-weight: bold;
                        position: absolute;
                        left: 0;
                        font-size: 18px;
                    }
                    
                    .footer {
                        background: #F7FAFC;
                        padding: 32px 30px;
                        text-align: center;
                        border-top: 1px solid #E2E8F0;
                    }
                    
                    .footer-info {
                        font-size: 12px;
                        color: #718096;
                        margin: 8px 0;
                    }
                    
                    .social-links {
                        margin: 24px 0;
                    }
                    
                    .social-link {
                        display: inline-block;
                        margin: 0 8px;
                        color: #718096;
                        text-decoration: none;
                        font-size: 14px;
                        transition: color 0.3s ease;
                    }
                    
                    .social-link:hover {
                        color: #f43f5e;
                    }
                    
                    @media (max-width: 600px) {
                        .email-wrapper {
                            padding: 10px;
                        }
                        
                        .header, .content, .footer {
                            padding: 24px 20px;
                        }
                        
                        .otp-code {
                            font-size: 28px;
                            letter-spacing: 8px;
                        }
                        
                        .logo {
                            font-size: 28px;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="email-wrapper">
                    <div class="container">
                        <div class="header">
                            <div class="logo">StarShop</div>
                            <p class="tagline">Nơi kết nối yêu thương qua từng đóa hoa</p>
                        </div>
                        
                        <div class="content">
                            <h1 class="greeting">Xin chào %s! 👋</h1>
                            <p class="description">Bạn đã yêu cầu mã xác thực để đặt lại mật khẩu. Đây là mã OTP của bạn:</p>
                            
                            <div class="otp-card">
                                <div class="otp-label">Mã xác thực của bạn</div>
                                <div class="otp-code">%s</div>
                                <div class="expiry-info">
                                    <span>⏰</span>
                                    <span>Mã có hiệu lực trong 10 phút</span>
                                </div>
                            </div>
                            
                            <div style="text-align: center;">
                                <a href="#" class="cta-button">Quay về trang đăng nhập</a>
                            </div>
                            
                            <div class="security-card">
                                <div class="security-title">
                                    <span>🔒</span>
                                    <span>Lưu ý bảo mật</span>
                                </div>
                                <ul class="security-list">
                                    <li>Không chia sẻ mã này với bất kỳ ai</li>
                                    <li>StarShop sẽ không bao giờ yêu cầu mã OTP qua điện thoại</li>
                                    <li>Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email</li>
                                </ul>
                            </div>
                        </div>
                        
                        <div class="footer">
                            <div class="social-links">
                                <a href="#" class="social-link">Facebook</a>
                                <a href="#" class="social-link">Instagram</a>
                                <a href="#" class="social-link">Email</a>
                            </div>
                            <div class="footer-info">Email được gửi lúc: %s</div>
                            <div class="footer-info">© 2024 StarShop. Tất cả quyền được bảo lưu.</div>
                            <div class="footer-info">💌 Email này được gửi từ hệ thống tự động, vui lòng không trả lời.</div>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """, firstName != null ? firstName : "bạn", otp, currentDateTime);
    }
    
    private String buildWelcomeEmailTemplate(String firstName) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Chào mừng đến với StarShop</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: linear-gradient(135deg, #f43f5e 0%%, #ec4899 100%%);">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="background: #FFF9F5; border-radius: 24px; overflow: hidden; box-shadow: 0 25px 50px rgba(0,0,0,0.15);">
                        <!-- Header -->
                        <div style="background: linear-gradient(135deg, #f43f5e 0%%, #ec4899 100%%); padding: 60px 30px; text-align: center; position: relative;">
                            <div style="font-size: 64px; margin-bottom: 15px;">🎉</div>
                            <h1 style="color: white; font-size: 40px; font-weight: 800; margin: 0 0 12px 0; letter-spacing: -1px; text-shadow: 0 2px 10px rgba(0,0,0,0.1);">CHÀO MỪNG BẠN!</h1>
                            <p style="color: rgba(255,255,255,0.95); font-size: 18px; margin: 0; font-weight: 500;">Bắt đầu hành trình mua sắm tuyệt vời cùng StarShop 🌸</p>
                            <div style="position: absolute; bottom: 0; left: 0; right: 0; height: 4px; background: linear-gradient(90deg, #f43f5e, #ec4899, #f43f5e);"></div>
                        </div>
                        
                        <!-- Content -->
                        <div style="padding: 50px 30px;">
                            <!-- Welcome Message -->
                            <div style="text-align: center; margin-bottom: 40px;">
                                <h2 style="font-size: 30px; font-weight: 800; color: #2D3748; margin: 0 0 16px 0; background: linear-gradient(135deg, #f43f5e, #ec4899); -webkit-background-clip: text; -webkit-text-fill-color: transparent;">
                                    Xin chào <span style="display: inline-block;">%s</span>! 👋
                                </h2>
                                <p style="font-size: 17px; color: #4A5568; line-height: 1.8; margin: 0;">
                                    Cảm ơn bạn đã tham gia <strong style="color: #f43f5e;">gia đình StarShop</strong>!<br>
                                    Hãy khám phá những trải nghiệm tuyệt vời đang chờ đón bạn 💝
                                </p>
                            </div>
                            
                            <!-- Special Welcome Offer -->
                            <div style="background: white; border-radius: 18px; padding: 35px; margin: 35px 0; box-shadow: 0 12px 35px rgba(244, 63, 94, 0.2); border: 3px solid #f43f5e; text-align: center;">
                                <div style="font-size: 22px; color: #f43f5e; font-weight: 800; margin-bottom: 15px;">🎁 QUÀ TẶNG CHÀO MỪNG</div>
                                <div style="font-size: 56px; font-weight: 900; background: linear-gradient(135deg, #f43f5e, #ec4899); -webkit-background-clip: text; -webkit-text-fill-color: transparent; margin: 20px 0;">10%%</div>
                                <div style="font-size: 17px; color: #4A5568; margin-bottom: 10px; font-weight: 600;">Giảm giá cho đơn hàng đầu tiên của bạn</div>
                                <div style="display: inline-block; background: linear-gradient(135deg, #fef2f2, #fce7f3); padding: 14px 28px; border-radius: 12px; margin-top: 10px; border: 2px dashed #f43f5e;">
                                    <div style="font-size: 13px; color: #718096; margin-bottom: 5px; font-weight: 600;">Mã giảm giá:</div>
                                    <div style="font-size: 26px; font-weight: 800; color: #f43f5e; letter-spacing: 4px; font-family: monospace;">WELCOME10</div>
                                </div>
                            </div>
                            
                            <!-- Benefits Grid -->
                            <div style="margin: 40px 0;">
                                <h3 style="color: #2D3748; font-size: 22px; font-weight: 700; margin: 0 0 25px 0; text-align: center;">✨ Quyền lợi thành viên của bạn</h3>
                                
                                <!-- Row 1 -->
                                <div style="display: table; width: 100%%; margin-bottom: 15px;">
                                    <div style="display: table-cell; width: 50%%; padding-right: 8px;">
                                        <div style="background: white; padding: 24px; border-radius: 14px; box-shadow: 0 6px 16px rgba(244, 63, 94, 0.1); border: 1px solid #fce7f3; text-align: center; height: 100%%;">
                                            <div style="font-size: 36px; margin-bottom: 12px;">🚚</div>
                                            <div style="font-size: 16px; color: #2D3748; font-weight: 700; margin-bottom: 6px;">Miễn phí ship</div>
                                            <div style="font-size: 13px; color: #718096;">Đơn hàng từ 500k</div>
                                        </div>
                                    </div>
                                    <div style="display: table-cell; width: 50%%; padding-left: 8px;">
                                        <div style="background: white; padding: 24px; border-radius: 14px; box-shadow: 0 6px 16px rgba(244, 63, 94, 0.1); border: 1px solid #fce7f3; text-align: center; height: 100%%;">
                                            <div style="font-size: 36px; margin-bottom: 12px;">📦</div>
                                            <div style="font-size: 16px; color: #2D3748; font-weight: 700; margin-bottom: 6px;">Theo dõi đơn hàng</div>
                                            <div style="font-size: 13px; color: #718096;">Cập nhật realtime</div>
                                        </div>
                                    </div>
                                </div>
                                
                                <!-- Row 2 -->
                                <div style="display: table; width: 100%%; margin-bottom: 15px;">
                                    <div style="display: table-cell; width: 50%%; padding-right: 8px;">
                                        <div style="background: white; padding: 24px; border-radius: 14px; box-shadow: 0 6px 16px rgba(244, 63, 94, 0.1); border: 1px solid #fce7f3; text-align: center; height: 100%%;">
                                            <div style="font-size: 36px; margin-bottom: 12px;">🎁</div>
                                            <div style="font-size: 16px; color: #2D3748; font-weight: 700; margin-bottom: 6px;">Ưu đãi sinh nhật</div>
                                            <div style="font-size: 13px; color: #718096;">Voucher 20%%</div>
                                        </div>
                                    </div>
                                    <div style="display: table-cell; width: 50%%; padding-left: 8px;">
                                        <div style="background: white; padding: 24px; border-radius: 14px; box-shadow: 0 6px 16px rgba(244, 63, 94, 0.1); border: 1px solid #fce7f3; text-align: center; height: 100%%;">
                                            <div style="font-size: 36px; margin-bottom: 12px;">💯</div>
                                            <div style="font-size: 16px; color: #2D3748; font-weight: 700; margin-bottom: 6px;">Đổi trả 24h</div>
                                            <div style="font-size: 13px; color: #718096;">Dễ dàng & nhanh chóng</div>
                                        </div>
                                    </div>
                                </div>
                                
                                <!-- Row 3 -->
                                <div style="display: table; width: 100%%;">
                                    <div style="display: table-cell; width: 50%%; padding-right: 8px;">
                                        <div style="background: white; padding: 24px; border-radius: 14px; box-shadow: 0 6px 16px rgba(244, 63, 94, 0.1); border: 1px solid #fce7f3; text-align: center; height: 100%%;">
                                            <div style="font-size: 36px; margin-bottom: 12px;">⭐</div>
                                            <div style="font-size: 16px; color: #2D3748; font-weight: 700; margin-bottom: 6px;">Tích điểm thưởng</div>
                                            <div style="font-size: 13px; color: #718096;">Đổi quà hấp dẫn</div>
                                        </div>
                                    </div>
                                    <div style="display: table-cell; width: 50%%; padding-left: 8px;">
                                        <div style="background: white; padding: 24px; border-radius: 14px; box-shadow: 0 6px 16px rgba(244, 63, 94, 0.1); border: 1px solid #fce7f3; text-align: center; height: 100%%;">
                                            <div style="font-size: 36px; margin-bottom: 12px;">💝</div>
                                            <div style="font-size: 16px; color: #2D3748; font-weight: 700; margin-bottom: 6px;">Deal độc quyền</div>
                                            <div style="font-size: 13px; color: #718096;">Ưu đãi đặc biệt</div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            
                            <!-- Next Steps -->
                            <div style="background: linear-gradient(135deg, #fef2f2 0%%, #fce7f3 100%%); border-radius: 14px; padding: 32px; margin: 40px 0; border-left: 5px solid #f43f5e;">
                                <h3 style="color: #2D3748; font-size: 20px; font-weight: 800; margin: 0 0 20px 0;">🚀 Bắt đầu ngay thôi!</h3>
                                <div style="margin: 15px 0;">
                                    <div style="display: flex; align-items: start; margin-bottom: 15px;">
                                        <div style="background: linear-gradient(135deg, #f43f5e, #ec4899); color: white; font-weight: 800; width: 32px; height: 32px; border-radius: 50%%; display: flex; align-items: center; justify-content: center; font-size: 15px; margin-right: 15px; flex-shrink: 0; box-shadow: 0 4px 12px rgba(244, 63, 94, 0.3);">1</div>
                                        <div>
                                            <div style="font-size: 15px; color: #2D3748; font-weight: 600; margin-bottom: 4px;">Khám phá sản phẩm</div>
                                            <div style="font-size: 14px; color: #718096;">Hơn 500+ loại hoa tươi đẹp đang chờ bạn</div>
                                        </div>
                                    </div>
                                    <div style="display: flex; align-items: start; margin-bottom: 15px;">
                                        <div style="background: linear-gradient(135deg, #f43f5e, #ec4899); color: white; font-weight: 800; width: 32px; height: 32px; border-radius: 50%%; display: flex; align-items: center; justify-content: center; font-size: 15px; margin-right: 15px; flex-shrink: 0; box-shadow: 0 4px 12px rgba(244, 63, 94, 0.3);">2</div>
                                        <div>
                                            <div style="font-size: 15px; color: #2D3748; font-weight: 600; margin-bottom: 4px;">Chọn và đặt hàng</div>
                                            <div style="font-size: 14px; color: #718096;">Thanh toán an toàn, nhiều phương thức</div>
                                        </div>
                                    </div>
                                    <div style="display: flex; align-items: start;">
                                        <div style="background: linear-gradient(135deg, #f43f5e, #ec4899); color: white; font-weight: 800; width: 32px; height: 32px; border-radius: 50%%; display: flex; align-items: center; justify-content: center; font-size: 15px; margin-right: 15px; flex-shrink: 0; box-shadow: 0 4px 12px rgba(244, 63, 94, 0.3);">3</div>
                                        <div>
                                            <div style="font-size: 15px; color: #2D3748; font-weight: 600; margin-bottom: 4px;">Nhận hoa tươi tại nhà</div>
                                            <div style="font-size: 14px; color: #718096;">Giao hàng nhanh 2-4h trong nội thành</div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            
                            <!-- CTA Buttons -->
                            <div style="text-align: center; margin: 40px 0;">
                                <a href="http://localhost:8080/catalog" style="display: inline-block; background: linear-gradient(135deg, #f43f5e, #ec4899); color: white; text-decoration: none; padding: 20px 48px; border-radius: 16px; font-weight: 800; font-size: 19px; box-shadow: 0 10px 30px rgba(244, 63, 94, 0.4); margin: 8px; letter-spacing: 0.5px;">
                                    🛍️ KHÁM PHÁ NGAY
                                </a>
                            </div>
                            
                            <!-- Trust Badge -->
                            <div style="text-align: center; margin-top: 40px;">
                                <div style="background: #FFF9E6; border-radius: 12px; padding: 20px; display: inline-block;">
                                    <div style="font-size: 14px; color: #92400E; font-weight: 600;">
                                        ⭐⭐⭐⭐⭐
                                    </div>
                                    <div style="font-size: 13px; color: #78350F; margin-top: 8px;">
                                        <strong>50,000+</strong> khách hàng hài lòng
                                    </div>
                                </div>
                            </div>
                            
                            <!-- Help Section -->
                            <div style="background: white; border-radius: 14px; padding: 28px; margin-top: 35px; border: 2px solid #fce7f3; text-align: center;">
                                <div style="font-size: 17px; color: #2D3748; font-weight: 700; margin-bottom: 12px;">💬 Cần hỗ trợ?</div>
                                <div style="font-size: 14px; color: #718096; line-height: 1.6;">
                                    Team hỗ trợ 24/7 luôn sẵn sàng giúp bạn!<br>
                                    <strong style="color: #f43f5e;">Hotline: 1900-xxxx</strong> | <strong style="color: #f43f5e;">support@starshop.vn</strong>
                                </div>
                            </div>
                        </div>
                        
                        <!-- Footer -->
                        <div style="background: #F7FAFC; padding: 35px 30px; text-align: center; border-top: 1px solid #E2E8F0;">
                            <div style="margin-bottom: 20px;">
                                <a href="#" style="display: inline-block; margin: 0 10px; color: #718096; text-decoration: none; font-size: 14px; font-weight: 500;">Facebook</a>
                                <span style="color: #CBD5E0;">•</span>
                                <a href="#" style="display: inline-block; margin: 0 10px; color: #718096; text-decoration: none; font-size: 14px; font-weight: 500;">Instagram</a>
                                <span style="color: #CBD5E0;">•</span>
                                <a href="#" style="display: inline-block; margin: 0 10px; color: #718096; text-decoration: none; font-size: 14px; font-weight: 500;">Zalo</a>
                            </div>
                            <div style="font-size: 12px; color: #718096; margin: 8px 0;">© 2024 StarShop - Nơi kết nối yêu thương qua từng đóa hoa 🌸</div>
                            <div style="font-size: 12px; color: #A0AEC0; margin: 8px 0;">💌 Chào mừng bạn đến với gia đình StarShop!</div>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """, firstName != null ? firstName : "bạn");
    }
    
    // Remove old CSS-based template
    private String buildWelcomeEmailTemplateOld(String firstName) {
        return String.format("""
            <!-- Old template removed, using new inline CSS version above -->
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    @import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap');
                    
                    body {
                        font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        line-height: 1.6;
                        color: #2D3748;
                        margin: 0;
                        padding: 0;
                        background: linear-gradient(135deg, #FFB3D9 0%%, #C7A3E8 100%%);
                        min-height: 100vh;
                    }
                    
                    .email-wrapper {
                        max-width: 600px;
                        margin: 0 auto;
                        padding: 20px;
                    }
                    
                    .container {
                        background: #FFF9F5;
                        border-radius: 24px;
                        overflow: hidden;
                        box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
                    }
                    
                    .header {
                        background: linear-gradient(135deg, #FFB3D9 0%%, #C7A3E8 100%%);
                        padding: 50px 30px;
                        text-align: center;
                        color: white;
                        position: relative;
                    }
                    
                    .header:before {
                        content: '';
                        position: absolute;
                        top: 0;
                        left: 0;
                        right: 0;
                        bottom: 0;
                        background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><circle cx="20" cy="20" r="2" fill="rgba(255,255,255,0.1)"/><circle cx="80" cy="40" r="1.5" fill="rgba(255,255,255,0.1)"/><circle cx="40" cy="80" r="1" fill="rgba(255,255,255,0.1)"/></svg>');
                        opacity: 0.3;
                    }
                    
                    .logo {
                        font-size: 36px;
                        font-weight: 700;
                        margin-bottom: 12px;
                        letter-spacing: -0.5px;
                        position: relative;
                        z-index: 1;
                    }
                    
                    .tagline {
                        font-size: 16px;
                        opacity: 0.9;
                        font-weight: 400;
                        margin: 0;
                        position: relative;
                        z-index: 1;
                    }
                    
                    .content {
                        padding: 50px 30px;
                    }
                    
                    .welcome-hero {
                        text-align: center;
                        margin-bottom: 40px;
                    }
                    
                    .welcome-title {
                        font-size: 32px;
                        font-weight: 700;
                        color: #2D3748;
                        margin-bottom: 16px;
                        background: linear-gradient(135deg, #FFB3D9, #C7A3E8);
                        -webkit-background-clip: text;
                        -webkit-text-fill-color: transparent;
                        background-clip: text;
                    }
                    
                    .welcome-subtitle {
                        font-size: 18px;
                        color: #4A5568;
                        margin-bottom: 32px;
                        line-height: 1.7;
                    }
                    
                    .benefits-grid {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
                        gap: 20px;
                        margin: 32px 0;
                    }
                    
                    .benefit-card {
                        background: white;
                        border-radius: 16px;
                        padding: 24px;
                        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
                        border: 1px solid #E2E8F0;
                        text-align: center;
                        transition: transform 0.3s ease;
                    }
                    
                    .benefit-icon {
                        font-size: 32px;
                        margin-bottom: 12px;
                        display: block;
                    }
                    
                    .benefit-title {
                        font-size: 16px;
                        font-weight: 600;
                        color: #2D3748;
                        margin-bottom: 8px;
                    }
                    
                    .benefit-description {
                        font-size: 14px;
                        color: #718096;
                        line-height: 1.5;
                    }
                    
                    .cta-section {
                        text-align: center;
                        margin: 40px 0;
                    }
                    
                    .cta-button {
                        display: inline-block;
                        background: linear-gradient(135deg, #FFB3D9 0%%, #C7A3E8 100%%);
                        color: white;
                        text-decoration: none;
                        padding: 18px 36px;
                        border-radius: 12px;
                        font-weight: 600;
                        font-size: 18px;
                        margin: 16px 8px;
                        transition: all 0.3s ease;
                        box-shadow: 0 6px 20px rgba(255, 179, 217, 0.3);
                    }
                    
                    .cta-button:hover {
                        transform: translateY(-2px);
                        box-shadow: 0 8px 25px rgba(255, 179, 217, 0.4);
                    }
                    
                    .secondary-button {
                        background: white;
                        color: #FFB3D9;
                        border: 2px solid #FFB3D9;
                    }
                    
                    .trust-badge {
                        background: #F7FAFC;
                        border: 1px solid #E2E8F0;
                        border-radius: 12px;
                        padding: 20px;
                        margin: 32px 0;
                        text-align: center;
                    }
                    
                    .trust-text {
                        font-size: 14px;
                        color: #4A5568;
                        margin: 0;
                    }
                    
                    .quick-links {
                        display: flex;
                        justify-content: center;
                        gap: 16px;
                        margin: 32px 0;
                        flex-wrap: wrap;
                    }
                    
                    .quick-link {
                        color: #718096;
                        text-decoration: none;
                        font-size: 14px;
                        padding: 8px 16px;
                        border: 1px solid #E2E8F0;
                        border-radius: 20px;
                        transition: all 0.3s ease;
                    }
                    
                    .quick-link:hover {
                        color: #FFB3D9;
                        border-color: #FFB3D9;
                    }
                    
                    .footer {
                        background: #F7FAFC;
                        padding: 40px 30px;
                        text-align: center;
                        border-top: 1px solid #E2E8F0;
                    }
                    
                    .footer-info {
                        font-size: 12px;
                        color: #718096;
                        margin: 8px 0;
                    }
                    
                    .social-links {
                        margin: 24px 0;
                    }
                    
                    .social-link {
                        display: inline-block;
                        margin: 0 12px;
                        color: #718096;
                        text-decoration: none;
                        font-size: 14px;
                        transition: color 0.3s ease;
                    }
                    
                    .social-link:hover {
                        color: #FFB3D9;
                    }
                    
                    @media (max-width: 600px) {
                        .email-wrapper {
                            padding: 10px;
                        }
                        
                        .header, .content, .footer {
                            padding: 30px 20px;
                        }
                        
                        .benefits-grid {
                            grid-template-columns: 1fr;
                        }
                        
                        .quick-links {
                            flex-direction: column;
                            align-items: center;
                        }
                        
                        .welcome-title {
                            font-size: 28px;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="email-wrapper">
                    <div class="container">
                        <div class="header">
                            <div class="logo">StarShop</div>
                            <p class="tagline">Nơi kết nối yêu thương qua từng đóa hoa</p>
                        </div>
                        
                        <div class="content">
                            <div class="welcome-hero">
                                <h1 class="welcome-title">Chào mừng %s! 🎉</h1>
                                <p class="welcome-subtitle">Cảm ơn bạn đã tham gia cộng đồng yêu hoa của chúng tôi! Hãy khám phá những trải nghiệm tuyệt vời đang chờ đón bạn.</p>
                            </div>
                            
                            <div class="benefits-grid">
                                <div class="benefit-card">
                                    <span class="benefit-icon">🎁</span>
                                    <div class="benefit-title">Ưu đãi sinh nhật</div>
                                    <div class="benefit-description">Nhận voucher 20%% cho ngày sinh nhật của bạn</div>
                                </div>
                                
                                <div class="benefit-card">
                                    <span class="benefit-icon">📦</span>
                                    <div class="benefit-title">Theo dõi đơn hàng</div>
                                    <div class="benefit-description">Cập nhật realtime từ lúc đặt đến khi giao</div>
                                </div>
                                
                                <div class="benefit-card">
                                    <span class="benefit-icon">💾</span>
                                    <div class="benefit-title">Lưu địa chỉ</div>
                                    <div class="benefit-description">Lưu nhiều địa chỉ giao hàng tiện lợi</div>
                                </div>
                                
                                <div class="benefit-card">
                                    <span class="benefit-icon">⭐</span>
                                    <div class="benefit-title">Tích điểm</div>
                                    <div class="benefit-description">Tích điểm cho mỗi đơn hàng và đổi quà</div>
                                </div>
                                
                                <div class="benefit-card">
                                    <span class="benefit-icon">🚚</span>
                                    <div class="benefit-title">Miễn phí ship</div>
                                    <div class="benefit-description">Giao hàng miễn phí từ 500k</div>
                                </div>
                                
                                <div class="benefit-card">
                                    <span class="benefit-icon">💝</span>
                                    <div class="benefit-title">Ưu đãi độc quyền</div>
                                    <div class="benefit-description">Nhận thông báo về deal hot nhất</div>
                                </div>
                            </div>
                            
                            <div class="cta-section">
                                <a href="#" class="cta-button">Khám phá ngay</a>
                                <a href="#" class="cta-button secondary-button">Xem sản phẩm mới</a>
                            </div>
                            
                            <div class="trust-badge">
                                <p class="trust-text">💌 Hơn 50,000 khách hàng tin dùng StarShop</p>
                            </div>
                            
                            <div class="quick-links">
                                <a href="#" class="quick-link">Sản phẩm mới</a>
                                <a href="#" class="quick-link">Ưu đãi hôm nay</a>
                                <a href="#" class="quick-link">Hướng dẫn mua hàng</a>
                                <a href="#" class="quick-link">Liên hệ hỗ trợ</a>
                            </div>
                        </div>
                        
                        <div class="footer">
                            <div class="social-links">
                                <a href="#" class="social-link">Facebook</a>
                                <a href="#" class="social-link">Instagram</a>
                                <a href="#" class="social-link">Email</a>
                            </div>
                            <div class="footer-info">© 2024 StarShop - Nơi kết nối yêu thương qua từng đóa hoa</div>
                            <div class="footer-info">💌 Chào mừng bạn đến với gia đình StarShop!</div>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """, firstName != null ? firstName : "bạn");
    }
    
    private String buildPasswordResetConfirmationTemplate(String firstName) {
        String currentDateTime = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Mật khẩu đã được đặt lại thành công</title>
                <style>
                    @import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap');
                    
                    body {
                        font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        line-height: 1.6;
                        color: #2D3748;
                        margin: 0;
                        padding: 0;
                        background: linear-gradient(135deg, #4CAF50 0%%, #81C784 100%%);
                        min-height: 100vh;
                    }
                    
                    .email-wrapper {
                        max-width: 600px;
                        margin: 0 auto;
                        padding: 20px;
                    }
                    
                    .container {
                        background: #FFF9F5;
                        border-radius: 24px;
                        overflow: hidden;
                        box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
                    }
                    
                    .header {
                        background: linear-gradient(135deg, #4CAF50 0%%, #81C784 100%%);
                        padding: 50px 30px;
                        text-align: center;
                        color: white;
                        position: relative;
                    }
                    
                    .header:before {
                        content: '';
                        position: absolute;
                        top: 0;
                        left: 0;
                        right: 0;
                        bottom: 0;
                        background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><circle cx="20" cy="20" r="2" fill="rgba(255,255,255,0.1)"/><circle cx="80" cy="40" r="1.5" fill="rgba(255,255,255,0.1)"/><circle cx="40" cy="80" r="1" fill="rgba(255,255,255,0.1)"/></svg>');
                        opacity: 0.3;
                    }
                    
                    .logo {
                        font-size: 32px;
                        font-weight: 700;
                        margin-bottom: 8px;
                        letter-spacing: -0.5px;
                        position: relative;
                        z-index: 1;
                    }
                    
                    .tagline {
                        font-size: 16px;
                        opacity: 0.9;
                        font-weight: 400;
                        margin: 0;
                        position: relative;
                        z-index: 1;
                    }
                    
                    .content {
                        padding: 50px 30px;
                    }
                    
                    .success-section {
                        text-align: center;
                        margin-bottom: 40px;
                    }
                    
                    .success-icon {
                        font-size: 64px;
                        margin-bottom: 24px;
                        display: block;
                        animation: pulse 2s infinite;
                    }
                    
                    @keyframes pulse {
                        0%% { transform: scale(1); }
                        50%% { transform: scale(1.05); }
                        100%% { transform: scale(1); }
                    }
                    
                    .success-title {
                        font-size: 28px;
                        font-weight: 700;
                        color: #2D3748;
                        margin-bottom: 16px;
                        background: linear-gradient(135deg, #4CAF50, #81C784);
                        -webkit-background-clip: text;
                        -webkit-text-fill-color: transparent;
                        background-clip: text;
                    }
                    
                    .success-subtitle {
                        font-size: 16px;
                        color: #4A5568;
                        margin-bottom: 32px;
                        line-height: 1.7;
                    }
                    
                    .message-card {
                        background: white;
                        border-radius: 16px;
                        padding: 32px;
                        margin: 24px 0;
                        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
                        border: 1px solid #E2E8F0;
                        text-align: left;
                    }
                    
                    .message-text {
                        font-size: 16px;
                        color: #2D3748;
                        margin: 16px 0;
                        line-height: 1.7;
                    }
                    
                    .highlight {
                        color: #4CAF50;
                        font-weight: 600;
                    }
                    
                    .security-card {
                        background: #F7FAFC;
                        border: 1px solid #E2E8F0;
                        border-radius: 12px;
                        padding: 24px;
                        margin: 24px 0;
                        border-left: 4px solid #4CAF50;
                    }
                    
                    .security-title {
                        font-size: 16px;
                        font-weight: 600;
                        color: #2D3748;
                        margin-bottom: 12px;
                        display: flex;
                        align-items: center;
                        gap: 8px;
                    }
                    
                    .security-text {
                        font-size: 14px;
                        color: #4A5568;
                        margin: 0;
                        line-height: 1.6;
                    }
                    
                    .cta-section {
                        text-align: center;
                        margin: 40px 0;
                    }
                    
                    .cta-button {
                        display: inline-block;
                        background: linear-gradient(135deg, #4CAF50 0%%, #81C784 100%%);
                        color: white;
                        text-decoration: none;
                        padding: 18px 36px;
                        border-radius: 12px;
                        font-weight: 600;
                        font-size: 18px;
                        margin: 16px 8px;
                        transition: all 0.3s ease;
                        box-shadow: 0 6px 20px rgba(76, 175, 80, 0.3);
                    }
                    
                    .cta-button:hover {
                        transform: translateY(-2px);
                        box-shadow: 0 8px 25px rgba(76, 175, 80, 0.4);
                    }
                    
                    .secondary-button {
                        background: white;
                        color: #4CAF50;
                        border: 2px solid #4CAF50;
                    }
                    
                    .timeline-card {
                        background: #F0FFF4;
                        border: 1px solid #C6F6D5;
                        border-radius: 12px;
                        padding: 20px;
                        margin: 24px 0;
                        text-align: center;
                    }
                    
                    .timeline-text {
                        font-size: 14px;
                        color: #2F855A;
                        margin: 0;
                        font-weight: 500;
                    }
                    
                    .footer {
                        background: #F7FAFC;
                        padding: 40px 30px;
                        text-align: center;
                        border-top: 1px solid #E2E8F0;
                    }
                    
                    .footer-info {
                        font-size: 12px;
                        color: #718096;
                        margin: 8px 0;
                    }
                    
                    .social-links {
                        margin: 24px 0;
                    }
                    
                    .social-link {
                        display: inline-block;
                        margin: 0 12px;
                        color: #718096;
                        text-decoration: none;
                        font-size: 14px;
                        transition: color 0.3s ease;
                    }
                    
                    .social-link:hover {
                        color: #4CAF50;
                    }
                    
                    @media (max-width: 600px) {
                        .email-wrapper {
                            padding: 10px;
                        }
                        
                        .header, .content, .footer {
                            padding: 30px 20px;
                        }
                        
                        .success-icon {
                            font-size: 48px;
                        }
                        
                        .success-title {
                            font-size: 24px;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="email-wrapper">
                    <div class="container">
                        <div class="header">
                            <div class="logo">StarShop</div>
                            <p class="tagline">Nơi kết nối yêu thương qua từng đóa hoa</p>
                        </div>
                        
                        <div class="content">
                            <div class="success-section">
                                <span class="success-icon">✅</span>
                                <h1 class="success-title">Mật khẩu đã được đặt lại thành công!</h1>
                                <p class="success-subtitle">Tài khoản của bạn đã được bảo mật và sẵn sàng sử dụng.</p>
                            </div>
                            
                            <div class="message-card">
                                <p class="message-text">Xin chào <strong>%s</strong>,</p>
                                <p class="message-text">Mật khẩu tài khoản StarShop của bạn đã được đặt lại thành công vào lúc <span class="highlight">%s</span>.</p>
                                <p class="message-text">Bây giờ bạn có thể đăng nhập bằng mật khẩu mới và tiếp tục mua sắm những bó hoa tuyệt đẹp.</p>
                            </div>
                            
                            <div class="timeline-card">
                                <p class="timeline-text">🕐 Thời gian thực hiện: %s</p>
                            </div>
                            
                            <div class="security-card">
                                <div class="security-title">
                                    <span>🔒</span>
                                    <span>Lưu ý bảo mật quan trọng</span>
                                </div>
                                <p class="security-text">Nếu bạn không thực hiện thao tác đặt lại mật khẩu này, vui lòng liên hệ ngay với đội ngũ hỗ trợ của chúng tôi để được hỗ trợ bảo mật tài khoản.</p>
                            </div>
                            
                            <div class="cta-section">
                                <a href="#" class="cta-button">Đăng nhập ngay</a>
                                <a href="#" class="cta-button secondary-button">Liên hệ hỗ trợ</a>
                            </div>
                        </div>
                        
                        <div class="footer">
                            <div class="social-links">
                                <a href="#" class="social-link">Facebook</a>
                                <a href="#" class="social-link">Instagram</a>
                                <a href="#" class="social-link">Email</a>
                            </div>
                            <div class="footer-info">© 2024 StarShop - Đội ngũ bảo mật</div>
                            <div class="footer-info">🔐 Tài khoản của bạn đã được bảo vệ an toàn</div>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """, firstName != null ? firstName : "bạn", currentDateTime, currentDateTime);
    }
    
    /**
     * Send marketing campaign email with beautiful template
     */
    public void sendMarketingEmail(
            String toEmail, 
            String customerName,
            String subject,
            String htmlBody) {
        try {
            logger.info("=== SENDING MARKETING EMAIL ===");
            logger.info("To: {}, Subject: {}", toEmail, subject);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, "StarShop 🌸");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            
            mailSender.send(message);
            logger.info("✅ Marketing email sent to: {}", toEmail);
            
        } catch (Exception e) {
            logger.error("❌ Failed to send marketing email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send marketing email", e);
        }
    }
    
    /**
     * Build marketing email template based on segment
     */
    public String buildMarketingEmailTemplate(
            String segment,
            String customerName, 
            String voucherCode,
            String expiryDate,
            String campaignName,
            int discountValue,
            String discountType) {
        
        // Choose template based on segment
        return switch (segment) {
            case "VIP" -> marketingTemplates.buildVipTemplate(customerName, voucherCode, expiryDate, discountValue, discountType);
            case "NEW" -> marketingTemplates.buildNewCustomerTemplate(customerName, voucherCode, expiryDate, discountValue, discountType);
            case "AT_RISK" -> marketingTemplates.buildWinBackTemplate(customerName, voucherCode, expiryDate, discountValue, discountType);
            default -> marketingTemplates.buildGenericTemplate(customerName, voucherCode, expiryDate, campaignName, discountValue, discountType);
        };
    }
}
