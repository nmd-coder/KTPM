package com.example.demo.config;

import com.example.demo.security.JwtAuthenticationFilter;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Security Configuration with JWT and OAuth2 support
 * Following rules.mdc specifications for Spring Security 6.3.4
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final com.example.demo.service.CustomUserDetailsService customUserDetailsService;

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                .csrfTokenRepository(org.springframework.security.web.csrf.CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/h2-console/**", "/ws/**", "/api/auth/**", "/logout", "/api/wishlist/**", "/api/favorite/**", "/api/cart/**", "/api/orders/**", "/api/payment/**", "/api/locations/**", "/api/addresses/**", "/api/shipping/**", "/api/catalogs/**", "/admin/orders/api/**", "/admin/products/api/**", "/admin/api/users/**", "/admin/api/employees/**", "/admin/api/**", "/api/staff/**", "/staff/api/**", "/api/chat/**", "/sse/**", "/swagger-ui/**", "/v3/api-docs/**")
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                // Configure fixation first so we don't need to call back with .and()
                .sessionFixation(sessionFixation -> sessionFixation.migrateSession())
                // Configure concurrency last; avoid deprecated .and()
                .maximumSessions(1)
                    .sessionRegistry(sessionRegistry())
            )
            .authorizeHttpRequests(auth -> auth
                // Public endpoints as per rules.mdc
                .requestMatchers("/", "/health", "/info", "/error").permitAll()
                .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/verify-registration", "/api/auth/forgot-password", "/api/auth/verify-otp", "/api/auth/reset-password").permitAll()
                // Payment callbacks - MUST BE FIRST to avoid authentication
                .requestMatchers("/payment/momo/**").permitAll()
                .requestMatchers("/test/**").permitAll() // For testing
                
                // Public pages
                .requestMatchers("/", "/home", "/about", "/contact", "/help/**").permitAll()
                
                // H2 Console (for development)
                .requestMatchers("/h2-console/**").permitAll()
                
                // OAuth2 endpoints
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers("/oauth2/authorization/**").permitAll()
                
                // Static resources
                .requestMatchers("/static/**", "/css/**", "/js/**", "/images/**", "/uploads/**").permitAll()
                .requestMatchers("/login", "/register", "/forgot-password", "/reset-password", "/logout").permitAll()
                
                // Swagger UI and API Docs
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
                
                // WebSocket endpoints
                .requestMatchers("/ws/**").permitAll()
                
                // Location APIs - public access for address forms
                .requestMatchers("/api/locations/**").permitAll()
                
                // Catalog APIs - public read, admin write
                .requestMatchers("/api/catalogs").permitAll()
                .requestMatchers("/api/catalogs/{id}").permitAll()
                
                // Account pages - require authentication
                .requestMatchers("/account/**").authenticated()
                // Order pages - require authentication
                .requestMatchers("/orders", "/orders/**", "/checkout").authenticated()
                // Staff pages - require STAFF role
                .requestMatchers("/staff/**").hasRole("STAFF")
                // Admin pages - require ADMIN role
                .requestMatchers("/admin/**").hasRole("ADMIN")
                
                // Products pages
                .requestMatchers("/products", "/products/**").permitAll()
                
                // Categories page
                .requestMatchers("/categories").permitAll()
                
                // Vouchers page - public access
                .requestMatchers("/vouchers").permitAll()
                
                // Blog page - public access
                .requestMatchers("/blog", "/blog/**").permitAll()
                
                // Voucher API - public access for viewing available vouchers
                .requestMatchers("/api/vouchers/available", "/api/vouchers/validate/**").permitAll()
                .requestMatchers("/api/vouchers/apply").hasAnyRole("CUSTOMER", "STAFF", "ADMIN")
                
                // Protected API endpoints as per rules.mdc
                .requestMatchers("/api/users/**").hasAnyRole("CUSTOMER", "STAFF", "ADMIN")
                .requestMatchers("/api/products/**").hasAnyRole("CUSTOMER", "STAFF", "ADMIN")
                .requestMatchers("/api/orders/**").hasAnyRole("CUSTOMER", "STAFF", "ADMIN")
                .requestMatchers("/api/payment/**").hasAnyRole("CUSTOMER", "STAFF", "ADMIN")
                .requestMatchers("/api/addresses/**").hasAnyRole("CUSTOMER", "STAFF", "ADMIN")
                .requestMatchers("/api/shipping/**").hasAnyRole("CUSTOMER", "STAFF", "ADMIN")
                .requestMatchers("/sse/**").hasAnyRole("CUSTOMER", "STAFF", "ADMIN")
                .requestMatchers("/api/cart/**").hasAnyRole("CUSTOMER", "STAFF", "ADMIN")
                .requestMatchers("/api/wishlist/**").hasAnyRole("CUSTOMER", "STAFF", "ADMIN")
                .requestMatchers("/api/reviews/**").hasRole("CUSTOMER")
                .requestMatchers("/api/favorite/**").hasAnyRole("CUSTOMER", "STAFF", "ADMIN")
                .requestMatchers("/api/messages/**").hasAnyRole("CUSTOMER", "STAFF", "ADMIN")
                .requestMatchers("/api/chat/**").hasAnyRole("CUSTOMER", "STAFF", "ADMIN")
                .requestMatchers("/api/staff/**").hasAnyRole("STAFF", "ADMIN")
                
                // Staff API endpoints - all under /staff/api/**
                .requestMatchers("/staff/api/**").hasAnyRole("STAFF", "ADMIN")
                
                // Admin API endpoints - all under /admin/api/** or /admin/{module}/api/**
                .requestMatchers("/admin/api/**").hasRole("ADMIN")
                .requestMatchers("/admin/orders/api/**").hasRole("ADMIN")
                .requestMatchers("/admin/products/api/**").hasRole("ADMIN")
                
                // Admin page routes
                .requestMatchers("/admin/**").hasRole("ADMIN")
                
                .anyRequest().authenticated()
            )
            .userDetailsService(customUserDetailsService)
            .formLogin(form -> form
                .loginPage("/login")
                .permitAll()
                .successHandler(formLoginSuccessHandler())
                .failureUrl("/login?error=true")
            )
            .rememberMe(remember -> remember
                .key("unique-and-secret")
                .tokenValiditySeconds(3600) // 1 hour instead of 24 hours
                .userDetailsService(username -> {
                    // Simple UserDetails implementation for remember-me
                    User user = userRepository.findByEmail(username).orElse(null);
                    if (user != null) {
                        return org.springframework.security.core.userdetails.User.builder()
                            .username(user.getEmail())
                            .password(user.getPassword())
                            .authorities("ROLE_" + user.getRole().name())
                            .build();
                    }
                    return null;
                })
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .successHandler(oauth2SuccessHandler())
                .failureHandler(oauth2FailureHandler())
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService())
                )
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("authToken", "JSESSIONID", "SPRING_SECURITY_REMEMBER_ME_COOKIE")
                .addLogoutHandler((request, response, authentication) -> {
                    // Clear session attributes before session invalidation
                    if (request.getSession(false) != null) {
                        request.getSession().removeAttribute("authToken");
                        request.getSession().removeAttribute("userEmail");
                        request.getSession().removeAttribute("userRole");
                        request.getSession().removeAttribute("userId");
                    }
                    // Clear SecurityContext
                    org.springframework.security.core.context.SecurityContextHolder.clearContext();
                })
                .permitAll()
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    try {
                        // Check if this is an API request (including admin API endpoints)
                        if (request.getRequestURI().startsWith("/api/") || 
                            request.getRequestURI().startsWith("/admin/") && request.getRequestURI().contains("/api/") ||
                            request.getRequestURI().startsWith("/staff/") && request.getRequestURI().contains("/api/")) {
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"success\":false,\"message\":\"Vui lòng đăng nhập để sử dụng tính năng này\"}");
                        } else {
                            // Handle authentication entry point - redirect to login for web pages
                            response.sendRedirect("/login?error=authentication_required");
                        }
                    } catch (IOException e) {
                        // Log error and send basic error response
                        response.setStatus(500);
                    }
                })
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin()) // For H2 console
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public com.example.demo.config.CustomOAuth2UserService customOAuth2UserService() {
        return new CustomOAuth2UserService(userRepository);
    }

    @Bean
    public AuthenticationSuccessHandler formLoginSuccessHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                    org.springframework.security.core.Authentication authentication) throws IOException {
                try {
                    String username = authentication.getName();
                    
                    // Find user by email
                    User user = userRepository.findByEmail(username).orElse(null);
                    
                    if (user == null) {
                        log.error("User not found after form login: {}", username);
                        response.sendRedirect("/login?error=user_not_found");
                        return;
                    }
                    
                    // Generate JWT token
                    String token = jwtService.generateToken(user.getEmail(), user.getRole(), user.getId());
                    
                    // Create JWT cookie
                    jakarta.servlet.http.Cookie authCookie = new jakarta.servlet.http.Cookie("authToken", token);
                    authCookie.setHttpOnly(true);
                    authCookie.setSecure(false); // Set to false for localhost development
                    authCookie.setPath("/");
                    authCookie.setMaxAge(4 * 60 * 60); // 4 hours instead of 24 hours
                    response.addCookie(authCookie);
                    
                    // Set authentication in session as backup
                    request.getSession().setAttribute("authToken", token);
                    request.getSession().setAttribute("userEmail", user.getEmail());
                    request.getSession().setAttribute("userRole", user.getRole().name());
                    request.getSession().setAttribute("userId", user.getId());
                    
                    // Update the authentication in SecurityContext with proper authorities
                    var authorities = java.util.List.of(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                    );
                    var newAuth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        user.getEmail(), null, authorities
                    );
                    org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(newAuth);
                    
                    // Redirect based on user role
                    String redirectUrl = "/";
                    if (user.getRole() == com.example.demo.entity.enums.UserRole.ADMIN) {
                        redirectUrl = "/admin/dashboard";
                    } else if (user.getRole() == com.example.demo.entity.enums.UserRole.STAFF) {
                        redirectUrl = "/staff/dashboard";
                    }
                    
                    log.info("Form login successful for user: {} with role: {}", user.getEmail(), user.getRole());
                    response.sendRedirect(redirectUrl);
                    
                } catch (Exception e) {
                    log.error("Form login error: {}", e.getMessage(), e);
                    response.sendRedirect("/login?error=processing_failed");
                }
            }
        };
    }

    @Bean
    public AuthenticationSuccessHandler oauth2SuccessHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, 
                    org.springframework.security.core.Authentication authentication) throws IOException {
                try {
                    OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
                    String email = oauth2User.getAttribute("email");
                    
                    if (email == null || email.isEmpty()) {
                        log.error("No email found in OAuth2 response");
                        response.sendRedirect("/login?error=oauth2_no_email");
                        return;
                    }
                    
                    // Find user (should exist after CustomOAuth2UserService processing)
                    User user = userRepository.findByEmail(email).orElse(null);
                    if (user == null) {
                        log.error("User not found after OAuth2 login: {}", email);
                        response.sendRedirect("/login?error=oauth2_user_not_found");
                        return;
                    }
                    
                    // Generate JWT token
                    String token = jwtService.generateToken(user.getEmail(), user.getRole(), user.getId());
                    
                    // Create JWT cookie
                    jakarta.servlet.http.Cookie authCookie = new jakarta.servlet.http.Cookie("authToken", token);
                    authCookie.setHttpOnly(true);
                    authCookie.setSecure(false); // Set to false for localhost development
                    authCookie.setPath("/");
                    authCookie.setMaxAge(4 * 60 * 60); // 4 hours instead of 24 hours
                    response.addCookie(authCookie);
                    
                    // Set authentication in session
                    request.getSession().setAttribute("authToken", token);
                    request.getSession().setAttribute("userEmail", user.getEmail());
                    request.getSession().setAttribute("userRole", user.getRole().name());
                    request.getSession().setAttribute("userId", user.getId());
                    
                    // Update the authentication in SecurityContext with proper authorities
                    var authorities = java.util.List.of(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                    );
                    var newAuth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        user.getEmail(), null, authorities
                    );
                    org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(newAuth);
                    
                    // Redirect based on user role
                    String redirectUrl = "/";
                    if (user.getRole() == com.example.demo.entity.enums.UserRole.ADMIN) {
                        redirectUrl = "/admin/dashboard";
                    } else if (user.getRole() == com.example.demo.entity.enums.UserRole.STAFF) {
                        redirectUrl = "/staff/dashboard";
                    }
                    
                    log.info("OAuth2 login successful for user: {} with role: {}", user.getEmail(), user.getRole());
                    response.sendRedirect(redirectUrl);
                    
                } catch (Exception e) {
                    log.error("OAuth2 login error: {}", e.getMessage(), e);
                    response.sendRedirect("/login?error=oauth2_processing");
                }
            }
        };
    }

    @Bean
    public AuthenticationFailureHandler oauth2FailureHandler() {
        return (HttpServletRequest request, HttpServletResponse response, 
                org.springframework.security.core.AuthenticationException exception) -> {
            try {
                response.sendRedirect("/login?error=oauth2_failed");
            } catch (IOException e) {
                // Log error
                response.sendRedirect("/login?error=oauth2");
            }
        };
    }
}
