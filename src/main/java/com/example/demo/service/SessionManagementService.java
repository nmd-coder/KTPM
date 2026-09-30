package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for managing user sessions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionManagementService {

    private final SessionRegistry sessionRegistry;

    /**
     * Invalidate all active sessions for a specific user
     * @param userEmail User email to invalidate sessions for
     */
    public void invalidateUserSessions(String userEmail) {
        log.info("Invalidating all sessions for user: {}", userEmail);
        
        try {
            // Get all principals (logged in users)
            List<Object> allPrincipals = sessionRegistry.getAllPrincipals();
            
            for (Object principal : allPrincipals) {
                String principalName = null;
                
                // Extract username from different principal types
                if (principal instanceof UserDetails) {
                    principalName = ((UserDetails) principal).getUsername();
                } else if (principal instanceof String) {
                    principalName = (String) principal;
                } else {
                    principalName = principal.toString();
                }
                
                // If this principal matches the user we want to invalidate
                if (userEmail.equals(principalName)) {
                    // Get all sessions for this user and expire them
                    sessionRegistry.getAllSessions(principal, false)
                        .forEach(sessionInfo -> {
                            log.info("Expiring session {} for user: {}", sessionInfo.getSessionId(), userEmail);
                            sessionInfo.expireNow();
                        });
                }
            }
            
            log.info("Successfully invalidated all sessions for user: {}", userEmail);
            
        } catch (Exception e) {
            log.error("Error invalidating sessions for user {}: {}", userEmail, e.getMessage(), e);
        }
    }

    /**
     * Get count of active sessions for a user
     * @param userEmail User email
     * @return Number of active sessions
     */
    public int getActiveSessionCount(String userEmail) {
        try {
            List<Object> allPrincipals = sessionRegistry.getAllPrincipals();
            
            for (Object principal : allPrincipals) {
                String principalName = null;
                
                if (principal instanceof UserDetails) {
                    principalName = ((UserDetails) principal).getUsername();
                } else if (principal instanceof String) {
                    principalName = (String) principal;
                } else {
                    principalName = principal.toString();
                }
                
                if (userEmail.equals(principalName)) {
                    return sessionRegistry.getAllSessions(principal, false).size();
                }
            }
            
            return 0;
        } catch (Exception e) {
            log.error("Error getting active session count for user {}: {}", userEmail, e.getMessage());
            return 0;
        }
    }
}
