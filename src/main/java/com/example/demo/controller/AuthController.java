package com.example.demo.controller;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.ResponseWrapper;
import com.example.demo.dto.UserInfoResponse;
import com.example.demo.entity.User;
import com.example.demo.service.AuthService;
import com.example.demo.service.WebSocketService;
import com.example.demo.service.EmailService;
import com.example.demo.service.OtpService;
import com.example.demo.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Authentication Controller for login endpoints
 * Following rules.mdc specifications for RESTful API design
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "🔐 Authentication", description = "Authentication APIs - Login, Register, OTP verification")
public class AuthController {

    private final AuthService authService;
    private final WebSocketService webSocketService;
    private final EmailService emailService;
    private final OtpService otpService;
    private final RegistrationService registrationService;
    private final PasswordEncoder passwordEncoder;

    /**
     * User login endpoint
     * POST /api/auth/login
     * @param loginRequest Login credentials
     * @param bindingResult Validation results
     * @return JWT token if successful, error message if failed
     */
    @Operation(
        summary = "Đăng nhập",
        description = "Đăng nhập với email và mật khẩu. Trả về JWT token để sử dụng cho các API khác."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Đăng nhập thành công, trả về JWT token"),
        @ApiResponse(responseCode = "400", description = "Thông tin đăng nhập không hợp lệ"),
        @ApiResponse(responseCode = "401", description = "Email hoặc mật khẩu không đúng")
    })
    @PostMapping("/login")
    public ResponseEntity<ResponseWrapper<LoginResponse>> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Thông tin đăng nhập (email và password)",
                required = true,
                content = @Content(schema = @Schema(implementation = LoginRequest.class))
            )
            @Valid @RequestBody LoginRequest loginRequest,
            @Parameter(hidden = true) BindingResult bindingResult,
            @Parameter(hidden = true) HttpServletResponse response) {
        
        log.info("Login attempt for email: {}", loginRequest.getEmail());
        
        try {
            // Check for validation errors
            if (bindingResult.hasErrors()) {
                String errorMessage = bindingResult.getFieldErrors().stream()
                        .map(error -> error.getField() + ": " + error.getDefaultMessage())
                        .collect(Collectors.joining(", "));
                
                log.warn("Login validation failed for email {}: {}", loginRequest.getEmail(), errorMessage);
                return ResponseEntity.badRequest()
                        .body(ResponseWrapper.error("Validation failed: " + errorMessage));
            }
            
            // Authenticate user
            String token = authService.authenticateUser(loginRequest.getEmail(), loginRequest.getPassword());
            
            if (token != null) {
                log.info("Login successful for email: {}", loginRequest.getEmail());
                
                // Get user information
                User user = authService.findUserByEmail(loginRequest.getEmail());
                if (user == null) {
                    log.error("User not found after successful authentication for email: {}", loginRequest.getEmail());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ResponseWrapper.error("An error occurred during login"));
                }
                
                // Send welcome message via WebSocket
                try {
                    webSocketService.sendWelcomeMessage(user.getId(), user.getFullName());
                } catch (Exception e) {
                    log.warn("Failed to send welcome message for user {}: {}", loginRequest.getEmail(), e.getMessage());
                    // Don't fail the login if WebSocket message fails
                }
                
                // Create user info response
                UserInfoResponse userInfo = UserInfoResponse.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .firstname(user.getFirstname())
                    .lastname(user.getLastname())
                    .phone(user.getPhone())
                    .avatar(user.getAvatar())
                    .role(user.getRole())
                    .isActive(user.getIsActive())
                    .build();
                
                // Set token in httpOnly cookie for security
                Cookie authCookie = new Cookie("authToken", token);
                authCookie.setHttpOnly(true);
                authCookie.setSecure(false); // Set to false for localhost development
                authCookie.setPath("/");
                authCookie.setMaxAge(4 * 60 * 60); // 4 hours instead of 24 hours
                response.addCookie(authCookie);
                
                // Create login response without token (for security)
                LoginResponse loginResponse = LoginResponse.of(null, userInfo);
                
                return ResponseEntity.ok(ResponseWrapper.<LoginResponse>success(loginResponse));
            } else {
                log.warn("Login failed for email: {} - Invalid credentials", loginRequest.getEmail());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ResponseWrapper.error("Invalid email or password"));
            }
            
        } catch (Exception e) {
            log.error("Login error for email {}: {}", loginRequest.getEmail(), e.getMessage(), e);
            
            // Log error in JSON format as per rules.mdc
            log.error("{{ \"error\": \"{}\", \"timestamp\": \"{}\" }}", 
                     e.getMessage(), LocalDateTime.now());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseWrapper.error("An error occurred during login. Please try again."));
        }
    }

    /**
     * Token refresh endpoint
     * POST /api/auth/refresh
     * @param currentToken Current JWT token
     * @return New JWT token if successful
     */
    @Operation(
        summary = "Refresh JWT token",
        description = "Làm mới JWT token khi sắp hết hạn. Trả về token mới với thời gian sống mới."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Refresh thành công, trả về token mới"),
        @ApiResponse(responseCode = "401", description = "Token không hợp lệ hoặc đã hết hạn")
    })
    @PostMapping("/refresh")
    public ResponseEntity<ResponseWrapper<String>> refreshToken(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "JWT token hiện tại cần refresh",
                required = true
            )
            @RequestBody String currentToken) {
        
        log.info("Token refresh attempt");
        
        try {
            String newToken = authService.refreshToken(currentToken);
            
            if (newToken != null) {
                log.info("Token refresh successful");
                return ResponseEntity.ok(ResponseWrapper.success(newToken));
            } else {
                log.warn("Token refresh failed - Invalid token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ResponseWrapper.error("Invalid or expired token"));
            }
            
        } catch (Exception e) {
            log.error("Token refresh error: {}", e.getMessage(), e);
            
            // Log error in JSON format as per rules.mdc
            log.error("{{ \"error\": \"{}\", \"timestamp\": \"{}\" }}", 
                     e.getMessage(), LocalDateTime.now());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseWrapper.error("An error occurred during token refresh"));
        }
    }

    /**
     * Token validation endpoint
     * GET /api/auth/validate
     * @param token JWT token from Authorization header
     * @return User information if token is valid
     */
    @Operation(
        summary = "Validate JWT token",
        description = "Kiểm tra tính hợp lệ của JWT token và trả về thông tin user nếu token còn hiệu lực."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token hợp lệ, trả về thông tin user"),
        @ApiResponse(responseCode = "401", description = "Token không hợp lệ hoặc đã hết hạn")
    })
    @GetMapping("/validate")
    public ResponseEntity<ResponseWrapper<Object>> validateToken(
            @Parameter(description = "JWT token trong header Authorization (Bearer <token>)", required = false)
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ResponseWrapper.error("Missing or invalid Authorization header"));
            }
            
            // Extract token from "Bearer " prefix
            String jwtToken = token.substring(7);
            
            var user = authService.validateTokenAndGetUser(jwtToken);
            
            if (user != null) {
                
                // Return user info without sensitive data
                UserInfoResponse userInfo = UserInfoResponse.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .firstname(user.getFirstname())
                    .lastname(user.getLastname())
                    .phone(user.getPhone())
                    .avatar(user.getAvatar())
                    .role(user.getRole())
                    .isActive(user.getIsActive())
                    .build();
                
                return ResponseEntity.ok(ResponseWrapper.success(userInfo));
            } else {
                log.warn("Token validation failed");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ResponseWrapper.error("Invalid or expired token"));
            }
            
        } catch (Exception e) {
            log.error("Token validation error: {}", e.getMessage(), e);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseWrapper.error("An error occurred during token validation"));
        }
    }

    /**
     * Logout endpoint (client-side token removal)
     * POST /api/auth/logout
     * @return Success message
     */
    @Operation(
        summary = "Đăng xuất",
        description = "Lưu ý: JWT là stateless nên đăng xuất chỉ cần xóa token ở client. Endpoint này chỉ để logging."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Đăng xuất thành công")
    })
    @PostMapping("/logout")
    public ResponseEntity<ResponseWrapper<String>> logout() {
        log.info("Logout request received");
        
        // Since JWT is stateless, logout is handled client-side by removing the token
        // This endpoint can be used for logging purposes or future token blacklisting
        
        return ResponseEntity.ok(ResponseWrapper.success("Logged out successfully"));
    }

    /**
     * User registration endpoint - Step 1: Send OTP
     * POST /api/auth/register
     */
    @Operation(
        summary = "Đăng ký tài khoản - Bước 1",
        description = "Gửi OTP đến email để xác thực. Cần cung cấp đầy đủ: email, password, firstname, lastname, phone."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OTP đã gửi đến email"),
        @ApiResponse(responseCode = "400", description = "Thông tin không hợp lệ hoặc email đã tồn tại")
    })
    @PostMapping("/register")
    public ResponseEntity<ResponseWrapper<String>> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Thông tin đăng ký (email, password, firstname, lastname, phone)",
                required = true
            )
            @RequestBody RegisterRequest request) {
        log.info("Registration OTP request for email: {}", request.getEmail());
        
        try {
            // Validate input
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ResponseWrapper.error("Email không được để trống"));
            }
            
            if (request.getPassword() == null || request.getPassword().length() < 8) {
                return ResponseEntity.badRequest()
                        .body(ResponseWrapper.error("Mật khẩu phải có ít nhất 8 ký tự"));
            }
            
            if (request.getFirstname() == null || request.getFirstname().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ResponseWrapper.error("Họ không được để trống"));
            }
            
            if (request.getLastname() == null || request.getLastname().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ResponseWrapper.error("Tên không được để trống"));
            }
            
            if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ResponseWrapper.error("Số điện thoại không được để trống"));
            }
            
            // Validate phone format (Vietnamese phone number)
            if (!request.getPhone().matches("^(\\+84|0)[3|5|7|8|9][0-9]{8}$")) {
                return ResponseEntity.badRequest()
                        .body(ResponseWrapper.error("Số điện thoại không hợp lệ"));
            }
            
            // Check if user already exists
            User existingUser = authService.findUserByEmail(request.getEmail());
            if (existingUser != null) {
                log.warn("Registration failed - email already exists: {}", request.getEmail());
                return ResponseEntity.badRequest()
                        .body(ResponseWrapper.error("Email này đã được sử dụng"));
            }
            
            // Store pending registration data
            registrationService.storePendingRegistration(
                request.getEmail(),
                request.getPassword(),
                request.getFirstname(),
                request.getLastname(),
                request.getPhone()
            );
            
            // Generate and send OTP
            String otp = otpService.generateOtp(request.getEmail());
            emailService.sendOtpEmail(request.getEmail(), otp, request.getFirstname());
            
            log.info("Registration OTP sent successfully for email: {}", request.getEmail());
            return ResponseEntity.ok(ResponseWrapper.success("Mã xác thực đã được gửi đến email của bạn. Vui lòng kiểm tra và nhập mã để hoàn tất đăng ký."));
            
        } catch (Exception e) {
            log.error("Registration OTP error for email {}: {}", request.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseWrapper.error("Có lỗi xảy ra. Vui lòng thử lại sau."));
        }
    }

    /**
     * Verify OTP and complete registration - Step 2
     * POST /api/auth/verify-registration
     */
    @Operation(
        summary = "Đăng ký tài khoản - Bước 2",
        description = "Xác thực OTP và hoàn tất đăng ký. Cần nhập OTP đã nhận qua email."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Đăng ký thành công"),
        @ApiResponse(responseCode = "400", description = "OTP không hợp lệ hoặc đã hết hạn")
    })
    @PostMapping("/verify-registration")
    public ResponseEntity<ResponseWrapper<String>> verifyRegistration(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Email và OTP để xác thực",
                required = true
            )
            @RequestBody VerifyRegistrationRequest request) {
        log.info("Registration OTP verification attempt for email: {}", request.getEmail());
        
        try {
            // Validate input
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ResponseWrapper.error("Email không được để trống"));
            }
            
            if (request.getOtp() == null || request.getOtp().length() != 6) {
                return ResponseEntity.badRequest()
                        .body(ResponseWrapper.error("Mã OTP phải có 6 số"));
            }
            
            // Verify OTP
            OtpService.OtpVerificationResult result = otpService.verifyOtp(request.getEmail(), request.getOtp());
            
            if (!result.isSuccess()) {
                log.warn("Registration OTP verification failed for email: {} - {}", request.getEmail(), result.getMessage());
                return ResponseEntity.badRequest()
                        .body(ResponseWrapper.error(result.getMessage()));
            }
            
            // Get pending registration data
            RegistrationService.PendingRegistration pendingReg = registrationService.getPendingRegistration(request.getEmail());
            if (pendingReg == null) {
                log.warn("No pending registration found for email: {}", request.getEmail());
                return ResponseEntity.badRequest()
                        .body(ResponseWrapper.error("Không tìm thấy thông tin đăng ký hoặc đã hết hạn. Vui lòng đăng ký lại."));
            }
            
            // Check if email already exists
            if (authService.findUserByEmail(pendingReg.getEmail()) != null) {
                log.warn("Email already exists during registration verification: {}", pendingReg.getEmail());
                return ResponseEntity.badRequest()
                        .body(ResponseWrapper.error("Email này đã được đăng ký. Vui lòng sử dụng email khác hoặc đăng nhập."));
            }
            
            // Check if phone already exists
            if (authService.findUserByPhone(pendingReg.getPhone()) != null) {
                log.warn("Phone already exists during registration verification: {}", pendingReg.getPhone());
                return ResponseEntity.badRequest()
                        .body(ResponseWrapper.error("Số điện thoại này đã được đăng ký. Vui lòng sử dụng số khác."));
            }
            
            // Create new user
            User newUser = authService.registerUser(
                pendingReg.getEmail(),
                pendingReg.getPassword(),
                pendingReg.getFirstname(),
                pendingReg.getLastname(),
                pendingReg.getPhone()
            );
            
            if (newUser != null) {
                // Remove pending registration
                registrationService.removePendingRegistration(request.getEmail());
                
                log.info("Registration completed successfully for email: {}", request.getEmail());
                
                // Send welcome email
                try {
                    emailService.sendWelcomeEmail(newUser.getEmail(), newUser.getFirstname());
                } catch (Exception e) {
                    log.warn("Failed to send welcome email for user {}: {}", request.getEmail(), e.getMessage());
                }
                
                return ResponseEntity.ok(ResponseWrapper.success("Đăng ký thành công! Bạn có thể đăng nhập ngay bây giờ."));
            } else {
                log.error("Failed to create user for email: {}", request.getEmail());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ResponseWrapper.error("Đăng ký thất bại. Vui lòng thử lại."));
            }
            
        } catch (Exception e) {
            log.error("Registration OTP verification error for email {}: {}", request.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseWrapper.error("Có lỗi xảy ra. Vui lòng thử lại sau."));
        }
    }

    /**
     * Forgot password endpoint - send OTP
     * POST /api/auth/forgot-password
     */
    @Operation(
        summary = "Quên mật khẩu - Bước 1",
        description = "Gửi OTP đến email để xác thực quên mật khẩu. Nếu email tồn tại, OTP sẽ được gửi."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OTP đã gửi (hoặc email không tồn tại)"),
        @ApiResponse(responseCode = "400", description = "Email không hợp lệ")
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<ResponseWrapper<String>> forgotPassword(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Email cần reset password",
                required = true
            )
            @RequestBody ForgotPasswordRequest request) {
        log.info("Forgot password request for email: {}", request.getEmail());
        
        try {
            // Validate email
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ResponseWrapper.error("Email không được để trống"));
            }
            
            // Check if user exists
            User user = authService.findUserByEmail(request.getEmail());
            if (user == null) {
                log.warn("Forgot password request for non-existent email: {}", request.getEmail());
                return ResponseEntity.badRequest()
                        .body(ResponseWrapper.error("Email này chưa được đăng ký trong hệ thống. Vui lòng kiểm tra lại hoặc đăng ký tài khoản mới."));
            }
            
            // Generate and send OTP
            String otp = otpService.generateOtp(request.getEmail());
            emailService.sendOtpEmail(request.getEmail(), otp, user.getFirstname());
            
            log.info("OTP sent successfully for email: {}", request.getEmail());
            return ResponseEntity.ok(ResponseWrapper.success("Mã xác thực đã được gửi đến email của bạn."));
            
        } catch (Exception e) {
            log.error("Forgot password error for email {}: {}", request.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseWrapper.error("Có lỗi xảy ra. Vui lòng thử lại sau."));
        }
    }

    /**
     * Verify OTP endpoint
     * POST /api/auth/verify-otp
     */
    @Operation(
        summary = "Quên mật khẩu - Bước 2",
        description = "Xác thực OTP và nhận reset token. Token này dùng cho bước reset password."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Xác thực thành công, trả về reset token"),
        @ApiResponse(responseCode = "400", description = "OTP không hợp lệ hoặc đã hết hạn")
    })
    @PostMapping("/verify-otp")
    public ResponseEntity<ResponseWrapper<VerifyOtpResponse>> verifyOtp(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Email và OTP để xác thực",
                required = true
            )
            @RequestBody VerifyOtpRequest request) {
        log.info("OTP verification attempt for email: {}", request.getEmail());
        
        try {
            // Validate input
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ResponseWrapper.error("Email không được để trống"));
            }
            
            if (request.getOtp() == null || request.getOtp().length() != 6) {
                return ResponseEntity.badRequest()
                        .body(ResponseWrapper.error("Mã OTP phải có 6 số"));
            }
            
            // Verify OTP
            OtpService.OtpVerificationResult result = otpService.verifyOtp(request.getEmail(), request.getOtp());
            
            if (result.isSuccess()) {
                // Generate reset token
                String resetToken = authService.generateResetToken(request.getEmail());
                log.info("OTP verification successful for email: {}", request.getEmail());
                
                VerifyOtpResponse response = new VerifyOtpResponse(resetToken);
                return ResponseEntity.ok(ResponseWrapper.success(response));
            } else {
                log.warn("OTP verification failed for email: {} - {}", request.getEmail(), result.getMessage());
                return ResponseEntity.badRequest()
                        .body(ResponseWrapper.error(result.getMessage()));
            }
            
        } catch (Exception e) {
            log.error("OTP verification error for email {}: {}", request.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseWrapper.error("Có lỗi xảy ra. Vui lòng thử lại sau."));
        }
    }

    /**
     * Reset password endpoint
     * POST /api/auth/reset-password
     */
    @Operation(
        summary = "Quên mật khẩu - Bước 3",
        description = "Reset mật khẩu mới sử dụng reset token từ bước 2. Mật khẩu mới phải ít nhất 8 ký tự."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reset mật khẩu thành công"),
        @ApiResponse(responseCode = "400", description = "Token không hợp lệ hoặc mật khẩu không đủ mạnh")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<ResponseWrapper<String>> resetPassword(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Reset token và mật khẩu mới",
                required = true
            )
            @RequestBody ResetPasswordRequest request) {
        log.info("Password reset attempt for token: {}", request.getToken().substring(0, 10) + "...");
        
        try {
            // Validate input
            if (request.getToken() == null || request.getToken().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ResponseWrapper.error("Token không hợp lệ"));
            }
            
            if (request.getNewPassword() == null || request.getNewPassword().length() < 8) {
                return ResponseEntity.badRequest()
                        .body(ResponseWrapper.error("Mật khẩu phải có ít nhất 8 ký tự"));
            }
            
            // Reset password using token
            boolean success = authService.resetPasswordWithToken(request.getToken(), request.getNewPassword());
            
            if (success) {
                // Get user info for confirmation email
                String email = authService.getEmailFromResetToken(request.getToken());
                if (email != null) {
                    User user = authService.findUserByEmail(email);
                    if (user != null) {
                        try {
                            emailService.sendPasswordResetConfirmationEmail(user.getEmail(), user.getFirstname());
                        } catch (Exception e) {
                            log.warn("Failed to send password reset confirmation email: {}", e.getMessage());
                        }
                    }
                }
                
                log.info("Password reset successful");
                return ResponseEntity.ok(ResponseWrapper.success("Mật khẩu đã được đặt lại thành công!"));
            } else {
                log.warn("Password reset failed - invalid or expired token");
                return ResponseEntity.badRequest()
                        .body(ResponseWrapper.error("Token không hợp lệ hoặc đã hết hạn"));
            }
            
        } catch (Exception e) {
            log.error("Password reset error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseWrapper.error("Có lỗi xảy ra. Vui lòng thử lại sau."));
        }
    }

    /**
     * Debug endpoint to check user data
     * GET /api/auth/debug-user?email=xxx
     */
    @Operation(
        summary = "[DEBUG] Kiểm tra thông tin user",
        description = "⚠️ DEBUG ONLY: Lấy thông tin chi tiết của user theo email. Endpoint này chỉ dùng cho development/testing."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Trả về thông tin user hoặc lỗi")
    })
    @GetMapping("/debug-user")
    public ResponseEntity<ResponseWrapper<Object>> debugUser(
            @Parameter(description = "Email của user cần kiểm tra", required = true, example = "user@example.com")
            @RequestParam String email) {
        try {
            User user = authService.findUserByEmail(email);
            if (user == null) {
                return ResponseEntity.ok(ResponseWrapper.error("User not found"));
            }
            
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("email", user.getEmail());
            debugInfo.put("passwordHash", user.getPassword().substring(0, 10) + "...");
            debugInfo.put("role", user.getRole());
            debugInfo.put("firstName", user.getFirstname());
            debugInfo.put("lastName", user.getLastname());
            debugInfo.put("phone", user.getPhone());
            debugInfo.put("createdAt", user.getCreatedAt());
            
            return ResponseEntity.ok(ResponseWrapper.success(debugInfo));
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseWrapper.error("Error: " + e.getMessage()));
        }
    }

    /**
     * Debug endpoint to test password encoding
     * POST /api/auth/test-password
     */
    @Operation(
        summary = "[DEBUG] Test password encoding",
        description = "⚠️ DEBUG ONLY: Kiểm tra password encoding và so sánh với hash trong database. Chỉ dùng cho debugging."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Trả về kết quả test password")
    })
    @PostMapping("/test-password")
    public ResponseEntity<ResponseWrapper<Object>> testPassword(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Email và password cần test",
                required = true
            )
            @RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String password = request.get("password");
            
            User user = authService.findUserByEmail(email);
            if (user == null) {
                return ResponseEntity.ok(ResponseWrapper.error("User not found"));
            }
            
            boolean matches = passwordEncoder.matches(password, user.getPassword());
            String newHash = passwordEncoder.encode(password);
            
            Map<String, Object> result = new HashMap<>();
            result.put("passwordMatches", matches);
            result.put("storedHash", user.getPassword().substring(0, 10) + "...");
            result.put("newHashOfSamePassword", newHash.substring(0, 10) + "...");
            result.put("passwordEncoderType", passwordEncoder.getClass().getSimpleName());
            
            return ResponseEntity.ok(ResponseWrapper.success(result));
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseWrapper.error("Error: " + e.getMessage()));
        }
    }

    // Request DTOs
    public static class RegisterRequest {
        private String email;
        private String password;
        private String firstname;
        private String lastname;
        private String phone;
        
        // Getters and setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getFirstname() { return firstname; }
        public void setFirstname(String firstname) { this.firstname = firstname; }
        public String getLastname() { return lastname; }
        public void setLastname(String lastname) { this.lastname = lastname; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }
    
    public static class ForgotPasswordRequest {
        private String email;
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
    
    public static class VerifyRegistrationRequest {
        private String email;
        private String otp;
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getOtp() { return otp; }
        public void setOtp(String otp) { this.otp = otp; }
    }
    
    public static class VerifyOtpRequest {
        private String email;
        private String otp;
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getOtp() { return otp; }
        public void setOtp(String otp) { this.otp = otp; }
    }
    
    public static class ResetPasswordRequest {
        private String token;
        private String newPassword;
        
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
    
    public static class VerifyOtpResponse {
        private String resetToken;
        
        public VerifyOtpResponse(String resetToken) {
            this.resetToken = resetToken;
        }
        
        public String getResetToken() { return resetToken; }
        public void setResetToken(String resetToken) { this.resetToken = resetToken; }
    }

    // Debug endpoint để test authentication
    @GetMapping("/debug/status")
    public ResponseEntity<Map<String, Object>> debugStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("auth_service", "available");
        result.put("timestamp", LocalDateTime.now().toString());
        result.put("message", "Authentication debug endpoint working");
        return ResponseEntity.ok(result);
    }

    // Temporary endpoint để tạo test user
    @PostMapping("/debug/create-user")
    public ResponseEntity<Map<String, Object>> createTestUser() {
        Map<String, Object> result = new HashMap<>();
        try {
            // Tạo user qua AuthService
            String email = "vuthang@example.com";
            String password = "88888888";
            String firstName = "Vũ Toàn";
            String lastName = "Thắng";
            
            // Check if user exists
            User existingUser = authService.findUserByEmail(email);
            if (existingUser != null) {
                result.put("message", "User already exists");
                result.put("user_id", existingUser.getId());
                result.put("user_email", existingUser.getEmail());
                return ResponseEntity.ok(result);
            }
            
            // Create new user (this would normally be done via registration)
            // For now, just return info that user creation is needed
            result.put("message", "User creation endpoint - implement user creation logic here");
            result.put("email", email);
            result.put("status", "needs_implementation");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * Get current authenticated user info
     * GET /api/auth/me
     * @return Current user information
     */
    @Operation(
        summary = "Lấy thông tin người dùng hiện tại",
        description = "Lấy thông tin của người dùng đang đăng nhập"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Thành công"),
        @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    })
    @GetMapping("/me")
    public ResponseEntity<ResponseWrapper<UserInfoResponse>> getCurrentUser(
            @Parameter(hidden = true) org.springframework.security.core.Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated() || 
                authentication.getName().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ResponseWrapper.error("Chưa đăng nhập"));
            }

            // Get user from database
            User user = authService.findUserByEmail(authentication.getName());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ResponseWrapper.error("Người dùng không tồn tại"));
            }

            // Create response
            UserInfoResponse userInfo = UserInfoResponse.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .firstname(user.getFirstname())
                    .lastname(user.getLastname())
                    .phone(user.getPhone())
                    .avatar(user.getAvatar())
                    .role(user.getRole())
                    .isActive(user.getIsActive())
                    .build();

            return ResponseEntity.ok(ResponseWrapper.success(userInfo));

        } catch (Exception e) {
            log.error("Error getting current user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseWrapper.error("Có lỗi xảy ra. Vui lòng thử lại sau."));
        }
    }
}
