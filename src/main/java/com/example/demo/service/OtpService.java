package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {
    
    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;
    
    // In-memory storage for OTP data
    // In production, consider using Redis or database
    private final Map<String, OtpData> otpStorage = new ConcurrentHashMap<>();
    
    /**
     * Generate and store OTP for email
     */
    public String generateOtp(String email) {
        String otp = generateRandomOtp();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);
        
        OtpData otpData = new OtpData(otp, expiryTime, 0);
        otpStorage.put(email.toLowerCase(), otpData);
        
        logger.info("OTP generated for email: {} (expires at: {})", email, expiryTime);
        return otp;
    }
    
    /**
     * Verify OTP for email
     */
    public OtpVerificationResult verifyOtp(String email, String inputOtp) {
        String emailKey = email.toLowerCase();
        OtpData otpData = otpStorage.get(emailKey);
        
        // Debug logging
        logger.info("OTP verification attempt for email: {}, inputOtp: {}", email, inputOtp);
        logger.info("Stored OTP data exists: {}", otpData != null);
        if (otpData != null) {
            logger.info("Stored OTP: {}, Expiry: {}, Attempts: {}", otpData.otp, otpData.expiryTime, otpData.attempts);
        }
        
        if (otpData == null) {
            logger.warn("OTP verification failed - no OTP found for email: {}", email);
            return new OtpVerificationResult(false, "Không tìm thấy mã OTP. Vui lòng yêu cầu mã mới.");
        }
        
        // Check if OTP has expired
        if (LocalDateTime.now().isAfter(otpData.expiryTime)) {
            otpStorage.remove(emailKey);
            logger.warn("OTP verification failed - expired OTP for email: {}", email);
            return new OtpVerificationResult(false, "Mã OTP đã hết hạn. Vui lòng yêu cầu mã mới.");
        }
        
        // Check if max attempts exceeded
        if (otpData.attempts >= MAX_ATTEMPTS) {
            otpStorage.remove(emailKey);
            logger.warn("OTP verification failed - max attempts exceeded for email: {}", email);
            return new OtpVerificationResult(false, "Bạn đã nhập sai quá nhiều lần. Vui lòng yêu cầu mã mới.");
        }
        
        // Increment attempt counter
        otpData.attempts++;
        
        // Verify OTP - trim whitespace and ensure exact match
        String trimmedInputOtp = inputOtp.trim();
        String storedOtp = otpData.otp.trim();
        
        logger.info("Comparing OTPs - Input: '{}' (length: {}), Stored: '{}' (length: {})", 
                   trimmedInputOtp, trimmedInputOtp.length(), storedOtp, storedOtp.length());
        
        if (storedOtp.equals(trimmedInputOtp)) {
            // OTP is correct, remove from storage
            otpStorage.remove(emailKey);
            logger.info("OTP verification successful for email: {}", email);
            return new OtpVerificationResult(true, "Xác thực thành công!");
        } else {
            logger.warn("OTP verification failed - incorrect OTP for email: {} (attempt {}/{}). Expected: '{}', Got: '{}'", 
                       email, otpData.attempts, MAX_ATTEMPTS, storedOtp, trimmedInputOtp);
            
            int remainingAttempts = MAX_ATTEMPTS - otpData.attempts;
            String message = remainingAttempts > 0 
                ? String.format("Mã OTP không đúng. Còn lại %d lần thử.", remainingAttempts)
                : "Mã OTP không đúng. Bạn đã hết lượt thử.";
                
            return new OtpVerificationResult(false, message);
        }
    }
    
    /**
     * Check if OTP exists and is valid for email
     */
    public boolean hasValidOtp(String email) {
        String emailKey = email.toLowerCase();
        OtpData otpData = otpStorage.get(emailKey);
        
        if (otpData == null) {
            return false;
        }
        
        // Check if expired
        if (LocalDateTime.now().isAfter(otpData.expiryTime)) {
            otpStorage.remove(emailKey);
            return false;
        }
        
        // Check if max attempts exceeded
        if (otpData.attempts >= MAX_ATTEMPTS) {
            otpStorage.remove(emailKey);
            return false;
        }
        
        return true;
    }
    
    /**
     * Remove OTP for email (cleanup)
     */
    public void removeOtp(String email) {
        String emailKey = email.toLowerCase();
        otpStorage.remove(emailKey);
        logger.info("OTP removed for email: {}", email);
    }
    
    /**
     * Get remaining time for OTP in minutes
     */
    public long getRemainingTimeMinutes(String email) {
        String emailKey = email.toLowerCase();
        OtpData otpData = otpStorage.get(emailKey);
        
        if (otpData == null || LocalDateTime.now().isAfter(otpData.expiryTime)) {
            return 0;
        }
        
        return java.time.Duration.between(LocalDateTime.now(), otpData.expiryTime).toMinutes();
    }
    
    /**
     * Clean up expired OTPs (should be called periodically)
     */
    public void cleanupExpiredOtps() {
        LocalDateTime now = LocalDateTime.now();
        final int[] removedCount = {0};
        
        otpStorage.entrySet().removeIf(entry -> {
            if (now.isAfter(entry.getValue().expiryTime)) {
                removedCount[0]++;
                return true;
            }
            return false;
        });
        
        if (removedCount[0] > 0) {
            logger.info("Cleaned up {} expired OTPs", removedCount[0]);
        }
    }
    
    private String generateRandomOtp() {
        Random random = new Random();
        StringBuilder otp = new StringBuilder();
        
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        
        return otp.toString();
    }
    
    /**
     * Data class to store OTP information
     */
    private static class OtpData {
        final String otp;
        final LocalDateTime expiryTime;
        int attempts;
        
        OtpData(String otp, LocalDateTime expiryTime, int attempts) {
            this.otp = otp;
            this.expiryTime = expiryTime;
            this.attempts = attempts;
        }
    }
    
    /**
     * Result class for OTP verification
     */
    public static class OtpVerificationResult {
        private final boolean success;
        private final String message;
        
        public OtpVerificationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
    }
}
