package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RegistrationService {
    
    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);
    
    // Temporary storage for pending registrations
    private final Map<String, PendingRegistration> pendingRegistrations = new ConcurrentHashMap<>();
    
    public static class PendingRegistration {
        private String email;
        private String password;
        private String firstname;
        private String lastname;
        private String phone;
        private LocalDateTime createdAt;
        private LocalDateTime expiresAt;
        
        public PendingRegistration(String email, String password, String firstname, String lastname, String phone) {
            this.email = email;
            this.password = password;
            this.firstname = firstname;
            this.lastname = lastname;
            this.phone = phone;
            this.createdAt = LocalDateTime.now();
            this.expiresAt = LocalDateTime.now().plusMinutes(10); // 10 minutes expiry
        }
        
        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
        
        // Getters
        public String getEmail() { return email; }
        public String getPassword() { return password; }
        public String getFirstname() { return firstname; }
        public String getLastname() { return lastname; }
        public String getPhone() { return phone; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
    }
    
    /**
     * Store pending registration data
     */
    public void storePendingRegistration(String email, String password, String firstname, String lastname, String phone) {
        PendingRegistration pending = new PendingRegistration(email, password, firstname, lastname, phone);
        pendingRegistrations.put(email.toLowerCase().trim(), pending);
        log.info("Stored pending registration for email: {}", email);
    }
    
    /**
     * Get pending registration data
     */
    public PendingRegistration getPendingRegistration(String email) {
        String key = email.toLowerCase().trim();
        PendingRegistration pending = pendingRegistrations.get(key);
        
        if (pending == null) {
            log.warn("No pending registration found for email: {}", email);
            return null;
        }
        
        if (pending.isExpired()) {
            log.warn("Pending registration expired for email: {}", email);
            pendingRegistrations.remove(key);
            return null;
        }
        
        return pending;
    }
    
    /**
     * Remove pending registration data
     */
    public void removePendingRegistration(String email) {
        String key = email.toLowerCase().trim();
        pendingRegistrations.remove(key);
        log.info("Removed pending registration for email: {}", email);
    }
    
    /**
     * Cleanup expired registrations
     */
    public void cleanupExpiredRegistrations() {
        pendingRegistrations.entrySet().removeIf(entry -> {
            if (entry.getValue().isExpired()) {
                log.info("Cleaned up expired pending registration for email: {}", entry.getKey());
                return true;
            }
            return false;
        });
    }
}
