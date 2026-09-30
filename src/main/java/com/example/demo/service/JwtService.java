package com.example.demo.service;

import com.example.demo.entity.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT Service for token generation and validation
 * Following rules.mdc specifications for JJWT 0.12.6
 */
@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate JWT token with user email and role claims
     * @param email User email
     * @param role User role
     * @return JWT token string
     */
    public String generateToken(String email, UserRole role) {
        return generateToken(email, role, null);
    }

    /**
     * Generate JWT token with user email, role and userId claims
     * @param email User email
     * @param role User role
     * @param userId User ID
     * @return JWT token string
     */
    public String generateToken(String email, UserRole role, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role.name());
        if (userId != null) {
            claims.put("userId", userId);
        }
        
        log.info("Generating JWT token for user: {} with role: {}", email, role);
        
        return createToken(claims, email);
    }

    /**
     * Create JWT token with claims and subject
     * @param claims Token claims
     * @param subject Token subject (usually email)
     * @return JWT token string
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + jwtExpiration);
        
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extract email from JWT token
     * @param token JWT token
     * @return User email
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract user role from JWT token
     * @param token JWT token
     * @return User role
     */
    public UserRole extractRole(String token) {
        String roleStr = extractClaim(token, claims -> claims.get("role", String.class));
        return roleStr != null ? UserRole.valueOf(roleStr) : null;
    }

    /**
     * Extract user ID from JWT token
     * @param token JWT token
     * @return User ID
     */
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    /**
     * Extract expiration date from JWT token
     * @param token JWT token
     * @return Expiration date
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract specific claim from JWT token
     * @param token JWT token
     * @param claimsResolver Function to resolve claim
     * @param <T> Claim type
     * @return Claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract all claims from JWT token
     * @param token JWT token
     * @return All claims
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("Error extracting claims from JWT token: {}", e.getMessage());
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    /**
     * Check if JWT token is expired
     * @param token JWT token
     * @return true if expired, false otherwise
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Validate JWT token against user email
     * @param token JWT token
     * @param email User email
     * @return true if valid, false otherwise
     */
    public Boolean validateToken(String token, String email) {
        try {
            final String tokenEmail = extractEmail(token);
            boolean isValid = tokenEmail.equals(email) && !isTokenExpired(token);
            
            if (isValid) {
            } else {
                log.warn("JWT token validation failed for user: {}", email);
            }
            
            return isValid;
        } catch (Exception e) {
            log.error("JWT token validation error for user {}: {}", email, e.getMessage());
            return false;
        }
    }

    /**
     * Validate JWT token without user email check
     * @param token JWT token
     * @return true if valid, false otherwise
     */
    public Boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            boolean isValid = !isTokenExpired(token);
            
            if (isValid) {
            } else {
                log.warn("JWT token is expired");
            }
            
            return isValid;
        } catch (Exception e) {
            log.error("JWT token validation error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get remaining time until token expiration in milliseconds
     * @param token JWT token
     * @return Remaining time in milliseconds
     */
    public Long getTokenRemainingTime(String token) {
        try {
            Date expiration = extractExpiration(token);
            long remaining = expiration.getTime() - new Date().getTime();
            return Math.max(remaining, 0);
        } catch (Exception e) {
            log.error("Error getting token remaining time: {}", e.getMessage());
            return 0L;
        }
    }
}
