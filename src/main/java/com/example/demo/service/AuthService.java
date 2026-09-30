package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.entity.enums.UserRole;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authentication Service for user login and JWT token generation
 * Following rules.mdc specifications for business logic tier
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    
    // In-memory storage for reset tokens
    // In production, consider using Redis or database
    private final Map<String, ResetTokenData> resetTokenStorage = new ConcurrentHashMap<>();

    /**
     * Authenticate user with email and password
     * @param email User email
     * @param password Plain text password
     * @return JWT token if authentication successful, null otherwise
     */
    public String authenticateUser(String email, String password) {
        log.info("Authentication attempt for email: {}", email);
        
        try {
            // Validate input parameters
            if (email == null || email.trim().isEmpty()) {
                log.warn("Authentication failed: email is null or empty");
                return null;
            }
            
            if (password == null || password.trim().isEmpty()) {
                log.warn("Authentication failed: password is null or empty for email: {}", email);
                return null;
            }
            
            // Find user by email
            Optional<User> userOptional = userRepository.findByEmail(email.trim().toLowerCase());
            if (userOptional.isEmpty()) {
                log.warn("Authentication failed: user not found for email: {}", email);
                return null;
            }
            
            User user = userOptional.get();
            
            // Check if user account is active
            if (user.getIsActive() == null || !user.getIsActive()) {
                log.warn("Authentication failed: user account is disabled for email: {}", email);
                return null;
            }
            
            // Debug: Log password check
            log.info("DEBUG: Checking password for user {}", email);
            log.info("DEBUG: Stored password hash: {}", user.getPassword().substring(0, 10) + "...");
            log.info("DEBUG: Password encoder type: {}", passwordEncoder.getClass().getSimpleName());
            
            // Validate password using BCrypt
            boolean passwordMatches = passwordEncoder.matches(password, user.getPassword());
            log.info("DEBUG: Password matches result: {}", passwordMatches);
            
            if (!passwordMatches) {
                log.warn("Authentication failed: invalid password for email: {}", email);
                // Additional debug - try encoding the provided password
                String encodedPassword = passwordEncoder.encode(password);
                log.info("DEBUG: If password '{}' was encoded now, it would be: {}", password, encodedPassword.substring(0, 10) + "...");
                return null;
            }
            
            // Generate JWT token with user claims
            String token = jwtService.generateToken(user.getEmail(), user.getRole(), user.getId());
            
            log.info("Authentication successful for user: {} with role: {}", email, user.getRole());
            return token;
            
        } catch (Exception e) {
            log.error("Authentication error for email {}: {}", email, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Find user by email
     * @param email User email
     * @return User entity if found, null otherwise
     */
    @Transactional(readOnly = true)
    public User findUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        
        try {
            return userRepository.findByEmail(email.trim().toLowerCase()).orElse(null);
        } catch (Exception e) {
            log.error("Error finding user by email {}: {}", email, e.getMessage());
            return null;
        }
    }

    /**
     * Find user by phone number
     * @param phone Phone number
     * @return User entity if found, null otherwise
     */
    @Transactional(readOnly = true)
    public User findUserByPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }
        
        try {
            return userRepository.findByPhone(phone.trim()).orElse(null);
        } catch (Exception e) {
            log.error("Error finding user by phone {}: {}", phone, e.getMessage());
            return null;
        }
    }

    /**
     * Find user by ID
     * @param userId User ID
     * @return User entity if found, null otherwise
     */
    @Transactional(readOnly = true)
    public User findUserById(Long userId) {
        if (userId == null) {
            return null;
        }
        
        try {
            return userRepository.findById(userId).orElse(null);
        } catch (Exception e) {
            log.error("Error finding user by ID {}: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * Validate user credentials without generating token
     * @param email User email
     * @param password Plain text password
     * @return true if credentials are valid, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean validateCredentials(String email, String password) {
        
        try {
            if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
                return false;
            }
            
            Optional<User> userOptional = userRepository.findByEmail(email.trim().toLowerCase());
            if (userOptional.isEmpty()) {
                return false;
            }
            
            User user = userOptional.get();
            return passwordEncoder.matches(password, user.getPassword());
            
        } catch (Exception e) {
            log.error("Error validating credentials for email {}: {}", email, e.getMessage());
            return false;
        }
    }

    /**
     * Check if user exists by email
     * @param email User email
     * @return true if user exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean userExists(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        try {
            return userRepository.existsByEmail(email.trim().toLowerCase());
        } catch (Exception e) {
            log.error("Error checking user existence for email {}: {}", email, e.getMessage());
            return false;
        }
    }

    /**
     * Refresh JWT token for existing user
     * @param currentToken Current JWT token
     * @return New JWT token if valid, null otherwise
     */
    public String refreshToken(String currentToken) {
        try {
            if (currentToken == null || currentToken.trim().isEmpty()) {
                return null;
            }
            
            // Validate current token
            if (!jwtService.validateToken(currentToken)) {
                log.warn("Token refresh failed: invalid current token");
                return null;
            }
            
            // Extract user information from current token
            String email = jwtService.extractEmail(currentToken);
            
            // Find user to get current role (in case it was updated)
            User user = findUserByEmail(email);
            if (user == null) {
                log.warn("Token refresh failed: user not found for email: {}", email);
                return null;
            }
            
            // Generate new token
            String newToken = jwtService.generateToken(user.getEmail(), user.getRole(), user.getId());
            
            log.info("Token refreshed successfully for user: {}", email);
            return newToken;
            
        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Validate JWT token and return user information
     * @param token JWT token
     * @return User entity if token is valid, null otherwise
     */
    @Transactional(readOnly = true)
    public User validateTokenAndGetUser(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return null;
            }
            
            // Validate token
            if (!jwtService.validateToken(token)) {
                return null;
            }
            
            // Extract email from token
            String email = jwtService.extractEmail(token);
            
            // Find and return user
            return findUserByEmail(email);
            
        } catch (Exception e) {
            log.error("Error validating token and getting user: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Register new user
     * @param email User email
     * @param password Plain text password
     * @param firstname User first name
     * @param lastname User last name
     * @return User entity if registration successful, null otherwise
     */
    public User registerUser(String email, String password, String firstname, String lastname, String phone) {
        log.info("Registration attempt for email: {}", email);
        
        try {
            // Validate input
            if (email == null || email.trim().isEmpty()) {
                log.warn("Registration failed: email is null or empty");
                return null;
            }
            
            if (password == null || password.length() < 8) {
                log.warn("Registration failed: password is too short for email: {}", email);
                return null;
            }
            
            if (firstname == null || firstname.trim().isEmpty()) {
                log.warn("Registration failed: firstname is null or empty for email: {}", email);
                return null;
            }
            
            if (lastname == null || lastname.trim().isEmpty()) {
                log.warn("Registration failed: lastname is null or empty for email: {}", email);
                return null;
            }
            
            if (phone == null || phone.trim().isEmpty()) {
                log.warn("Registration failed: phone is null or empty for email: {}", email);
                return null;
            }
            
            // Check if user already exists
            if (userExists(email)) {
                log.warn("Registration failed: user already exists for email: {}", email);
                return null;
            }
            
            // Create new user
            User newUser = new User();
            newUser.setEmail(email.trim().toLowerCase());
            newUser.setPassword(passwordEncoder.encode(password));
            newUser.setFirstname(firstname.trim());
            newUser.setLastname(lastname.trim());
            newUser.setPhone(phone.trim());
            newUser.setRole(UserRole.CUSTOMER); // Default role
            newUser.setCreatedAt(LocalDateTime.now());
            newUser.setUpdatedAt(LocalDateTime.now());
            
            // Save user
            User savedUser = userRepository.save(newUser);
            
            log.info("Registration successful for email: {}", email);
            return savedUser;
            
        } catch (Exception e) {
            log.error("Registration error for email {}: {}", email, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Generate reset token for password reset
     * @param email User email
     * @return Reset token if user exists, null otherwise
     */
    public String generateResetToken(String email) {
        log.info("Generating reset token for email: {}", email);
        
        try {
            // Check if user exists
            User user = findUserByEmail(email);
            if (user == null) {
                log.warn("Reset token generation failed: user not found for email: {}", email);
                return null;
            }
            
            // Generate unique token
            String token = UUID.randomUUID().toString();
            LocalDateTime expiryTime = LocalDateTime.now().plusHours(1); // 1 hour expiry
            
            // Store token with user email and expiry
            ResetTokenData tokenData = new ResetTokenData(email, expiryTime);
            resetTokenStorage.put(token, tokenData);
            
            log.info("Reset token generated successfully for email: {}", email);
            return token;
            
        } catch (Exception e) {
            log.error("Error generating reset token for email {}: {}", email, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Reset password using reset token
     * @param token Reset token
     * @param newPassword New password
     * @return true if reset successful, false otherwise
     */
    public boolean resetPasswordWithToken(String token, String newPassword) {
        log.info("Password reset attempt with token: {}", token.substring(0, 10) + "...");
        
        try {
            // Validate input
            if (token == null || token.trim().isEmpty()) {
                log.warn("Password reset failed: token is null or empty");
                return false;
            }
            
            if (newPassword == null || newPassword.length() < 8) {
                log.warn("Password reset failed: new password is too short");
                return false;
            }
            
            // Get token data
            ResetTokenData tokenData = resetTokenStorage.get(token);
            if (tokenData == null) {
                log.warn("Password reset failed: invalid token");
                return false;
            }
            
            // Check if token has expired
            if (LocalDateTime.now().isAfter(tokenData.expiryTime)) {
                resetTokenStorage.remove(token);
                log.warn("Password reset failed: token has expired");
                return false;
            }
            
            // Find user
            User user = findUserByEmail(tokenData.email);
            if (user == null) {
                resetTokenStorage.remove(token);
                log.warn("Password reset failed: user not found for email: {}", tokenData.email);
                return false;
            }
            
            // Update password
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            
            // Remove used token
            resetTokenStorage.remove(token);
            
            log.info("Password reset successful for email: {}", tokenData.email);
            return true;
            
        } catch (Exception e) {
            log.error("Error resetting password with token: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get email from reset token
     * @param token Reset token
     * @return Email if token is valid, null otherwise
     */
    public String getEmailFromResetToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        
        ResetTokenData tokenData = resetTokenStorage.get(token);
        if (tokenData == null) {
            return null;
        }
        
        // Check if token has expired
        if (LocalDateTime.now().isAfter(tokenData.expiryTime)) {
            resetTokenStorage.remove(token);
            return null;
        }
        
        return tokenData.email;
    }

    /**
     * Clean up expired reset tokens (should be called periodically)
     */
    public void cleanupExpiredResetTokens() {
        LocalDateTime now = LocalDateTime.now();
        final int[] removedCount = {0};
        
        resetTokenStorage.entrySet().removeIf(entry -> {
            if (now.isAfter(entry.getValue().expiryTime)) {
                removedCount[0]++;
                return true;
            }
            return false;
        });
        
        if (removedCount[0] > 0) {
            log.info("Cleaned up {} expired reset tokens", removedCount[0]);
        }
    }

    /**
     * Data class to store reset token information
     */
    private static class ResetTokenData {
        final String email;
        final LocalDateTime expiryTime;
        
        ResetTokenData(String email, LocalDateTime expiryTime) {
            this.email = email;
            this.expiryTime = expiryTime;
        }
    }
}
