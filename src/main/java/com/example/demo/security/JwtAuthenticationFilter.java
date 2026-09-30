package com.example.demo.security;

import com.example.demo.entity.User;
import com.example.demo.entity.enums.UserRole;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import jakarta.servlet.http.Cookie;
import org.springframework.lang.NonNull;

/**
 * JWT Authentication Filter extending OncePerRequestFilter
 * Following rules.mdc specifications for JWT security
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, 
                                  @NonNull HttpServletResponse response, 
                                  @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        String jwt = null;
        final String userEmail;
        
        // Try to get JWT from Authorization header first
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
        } else {
            // Try to get JWT from cookie
            jwt = getJwtFromCookie(request);
        }
        
        // Skip filter if no JWT token found
        if (jwt == null) {
            
            // Check if there's a session-based authentication
            String sessionToken = (String) request.getSession().getAttribute("authToken");
            String sessionEmail = (String) request.getSession().getAttribute("userEmail");
            
            if (sessionToken != null && sessionEmail != null) {
                try {
                    if (jwtService.validateToken(sessionToken)) {
                        var tokenRole = jwtService.extractRole(sessionToken);
                        var userId = jwtService.extractUserId(sessionToken);
                        
                        // Check current role and active status in database
                        Optional<User> userOptional = userRepository.findByEmail(sessionEmail);
                        if (userOptional.isPresent()) {
                            User user = userOptional.get();
                            UserRole dbRole = user.getRole();
                            
                            // Check if user account is still active
                            if (user.getIsActive() == null || !user.getIsActive()) {
                                log.warn("User account is disabled for session email: {}", sessionEmail);
                                SecurityContextHolder.clearContext();
                                
                                // Clear session and cookies
                                if (request.getSession(false) != null) {
                                    request.getSession().invalidate();
                                }
                                
                                // Clear auth cookie
                                Cookie clearCookie = new Cookie("authToken", "");
                                clearCookie.setHttpOnly(true);
                                clearCookie.setSecure(false);
                                clearCookie.setPath("/");
                                clearCookie.setMaxAge(0);
                                response.addCookie(clearCookie);
                                
                                filterChain.doFilter(request, response);
                                return;
                            }
                            
                            // If role in DB is different from token, refresh the token
                            if (!dbRole.equals(tokenRole)) {
                                log.info("Session role mismatch detected for user {}: token role={}, DB role={}. Refreshing token.", 
                                    sessionEmail, tokenRole, dbRole);
                                
                                // Generate new token with updated role
                                String newToken = jwtService.generateToken(user.getEmail(), user.getRole(), user.getId());
                                
                                // Update cookie with new token
                                Cookie authCookie = new Cookie("authToken", newToken);
                                authCookie.setHttpOnly(true);
                                authCookie.setSecure(false); // Set to false for localhost development
                                authCookie.setPath("/");
                                authCookie.setMaxAge(4 * 60 * 60); // 4 hours
                                response.addCookie(authCookie);
                                
                                // Update session with new token
                                request.getSession().setAttribute("authToken", newToken);
                                request.getSession().setAttribute("userRole", dbRole.name());
                                
                                // Use the updated role from DB
                                tokenRole = dbRole;
                                log.info("Session token refreshed for user {} with new role: {}", sessionEmail, dbRole);
                            }
                        } else {
                            log.warn("User not found in database for session email: {}", sessionEmail);
                            filterChain.doFilter(request, response);
                            return;
                        }
                        
                        List<SimpleGrantedAuthority> authorities = List.of(
                            new SimpleGrantedAuthority("ROLE_" + tokenRole.name().toUpperCase())
                        );
                        
                        UsernamePasswordAuthenticationToken authToken = 
                            new UsernamePasswordAuthenticationToken(sessionEmail, null, authorities);
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        
                        request.setAttribute("userId", userId);
                        request.setAttribute("userRole", tokenRole);
                        
                    // Also set session attributes for Thymeleaf access
                    request.getSession().setAttribute("userId", userId);
                    request.getSession().setAttribute("userRole", tokenRole.name());
                    log.info("Set session attributes - userId: {}, userRole: {}", userId, tokenRole.name());
                    
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.info("Session-based JWT authentication successful for user: {} with role: {}", sessionEmail, tokenRole);
                    }
                } catch (Exception e) {
                    log.warn("Session token validation failed: {}", e.getMessage());
                }
            }
            
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            // Extract email from JWT token
            userEmail = jwtService.extractEmail(jwt);
            
            // If token is valid and user is not already authenticated
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                // Validate token
                if (jwtService.validateToken(jwt)) {
                    
                    // Extract user role from token
                    var tokenRole = jwtService.extractRole(jwt);
                    var userId = jwtService.extractUserId(jwt);
                    
                    // Check current role and active status in database
                    Optional<User> userOptional = userRepository.findByEmail(userEmail);
                    if (userOptional.isPresent()) {
                        User user = userOptional.get();
                        UserRole dbRole = user.getRole();
                        
                        // Check if user account is still active
                        if (user.getIsActive() == null || !user.getIsActive()) {
                            log.warn("User account is disabled for email: {}", userEmail);
                            SecurityContextHolder.clearContext();
                            
                            // Clear session and cookies
                            if (request.getSession(false) != null) {
                                request.getSession().invalidate();
                            }
                            
                            // Clear auth cookie
                            Cookie clearCookie = new Cookie("authToken", "");
                            clearCookie.setHttpOnly(true);
                            clearCookie.setSecure(false);
                            clearCookie.setPath("/");
                            clearCookie.setMaxAge(0);
                            response.addCookie(clearCookie);
                            
                            filterChain.doFilter(request, response);
                            return;
                        }
                        
                        // If role in DB is different from token, refresh the token
                        if (!dbRole.equals(tokenRole)) {
                            log.info("Role mismatch detected for user {}: token role={}, DB role={}. Refreshing token.", 
                                userEmail, tokenRole, dbRole);
                            
                            // Generate new token with updated role
                            String newToken = jwtService.generateToken(user.getEmail(), user.getRole(), user.getId());
                            
                            // Update cookie with new token
                            Cookie authCookie = new Cookie("authToken", newToken);
                            authCookie.setHttpOnly(true);
                            authCookie.setSecure(false); // Set to false for localhost development
                            authCookie.setPath("/");
                            authCookie.setMaxAge(4 * 60 * 60); // 4 hours
                            response.addCookie(authCookie);
                            
                            // Update session with new token
                            request.getSession().setAttribute("authToken", newToken);
                            request.getSession().setAttribute("userRole", dbRole.name());
                            
                            // Use the updated role from DB
                            tokenRole = dbRole;
                            log.info("Token refreshed for user {} with new role: {}", userEmail, dbRole);
                        }
                    } else {
                        log.warn("User not found in database for email: {}", userEmail);
                        SecurityContextHolder.clearContext();
                        filterChain.doFilter(request, response);
                        return;
                    }
                    
                    // Create authority based on user role (now updated from DB)
                    List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + tokenRole.name().toUpperCase())
                    );
                    
                    // Create authentication token
                    UsernamePasswordAuthenticationToken authToken = 
                        new UsernamePasswordAuthenticationToken(userEmail, null, authorities);
                    
                    // Set additional details
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // Add user ID to authentication details for easy access
                    request.setAttribute("userId", userId);
                    request.setAttribute("userRole", tokenRole);
                    
                    // Also set session attributes for Thymeleaf access
                    request.getSession().setAttribute("userId", userId);
                    request.getSession().setAttribute("userRole", tokenRole.name());
                    log.info("Set session attributes from JWT - userId: {}, userRole: {}", userId, tokenRole.name());
                    
                    // Set authentication in security context
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    log.info("JWT authentication successful for user: {} with role: {} and authorities: {}", 
                        userEmail, tokenRole, authorities);
                    
                } else {
                    log.warn("Invalid JWT token for user: {}", userEmail);
                    // Clear any existing authentication
                    SecurityContextHolder.clearContext();
                }
            } else if (userEmail == null) {
                log.warn("Could not extract email from JWT token");
            }
            
        } catch (Exception e) {
            log.error("JWT authentication error: {}", e.getMessage());
            // Clear security context on error
            SecurityContextHolder.clearContext();
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Extract JWT token from authToken cookie
     */
    private String getJwtFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("authToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        
        // Skip JWT filter only for truly public endpoints and static resources
        return path.equals("/health") ||
               path.equals("/info") ||
               path.equals("/error") ||
               path.startsWith("/api/auth/") ||
               path.startsWith("/public/") ||
               path.startsWith("/static/") ||
               path.startsWith("/css/") ||
               path.startsWith("/js/") ||
               path.startsWith("/images/") ||
               path.startsWith("/login") ||
               path.startsWith("/register") ||
               path.startsWith("/forgot-password") ||
               path.startsWith("/reset-password") ||
               path.startsWith("/oauth2/") ||
               path.startsWith("/h2-console") ||
               path.startsWith("/ws") ||
               path.startsWith("/actuator/");
    }
}
