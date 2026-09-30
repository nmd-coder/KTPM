package com.example.demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.Components;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger3 / OpenAPI 3 Configuration
 * Provides API documentation and testing interface
 * Access: http://localhost:8080/swagger-ui.html
 */
@Configuration
public class SwaggerConfig {

    @Value("${app.base-url}")
    private String localServerUrl;

    @Value("${swagger.server.prod.url}")
    private String prodServerUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        // Define security scheme for JWT Bearer token
        final String securitySchemeName = "bearerAuth";
        
        return new OpenAPI()
                // API Information
                .info(new Info()
                        .title("Flower Shop API Documentation")
                        .version("1.0.0")
                        .description("REST API Documentation for Flower Shop E-commerce System\n\n" +
                                "**Features:**\n" +
                                "- 🛒 Shopping Cart Management\n" +
                                "- ❤️ Wishlist/Favorites\n" +
                                "- 📦 Order Management\n" +
                                "- 💳 Payment Integration (MoMo)\n" +
                                "- 🔐 Authentication & Authorization\n" +
                                "- 👤 User Account Management\n" +
                                "- 🌸 Product & Category Management\n\n" +
                                "**Authentication:**\n" +
                                "Most endpoints require JWT Bearer token. Use /api/auth/login to get token.")
                        .contact(new Contact()
                                .name("Flower Shop Development Team")
                                .email("support@flowershop.com")
                                .url("https://flowershop.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                
                // Server Configuration
                .servers(List.of(
                        new Server()
                                .url(localServerUrl)
                                .description("Local Development Server"),
                        new Server()
                                .url(prodServerUrl)
                                .description("Production Server")
                ))
                
                // Security Configuration
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT Bearer Token Authentication\n\n" +
                                                "**How to use:**\n" +
                                                "1. Call POST /api/auth/login with credentials\n" +
                                                "2. Copy the token from response\n" +
                                                "3. Click 'Authorize' button above\n" +
                                                "4. Paste token (without 'Bearer' prefix)\n" +
                                                "5. Click 'Authorize' and 'Close'\n" +
                                                "6. All authenticated requests will include the token")))
                
                // Apply security globally
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName));
    }

    /**
     * Group: Public/Customer APIs
     * Includes authentication, catalog browsing, cart, orders, payment, etc.
     */
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("1-public-customer")
                .pathsToMatch(
                        "/api/auth/**",
                        "/api/catalogs/**",
                        "/api/products/**",
                        "/api/cart/**",
                        "/api/orders/**",
                        "/api/payment/**",
                        "/api/locations/**",
                        "/api/addresses/**",
                        "/api/shipping/**",
                        "/api/reviews/**",
                        "/api/vouchers/**",
                        "/api/wishlist/**",
                        "/api/favorite/**",
                        "/api/chat/**",
                        "/payment/**",
                        "/sse/**"
                )
                .pathsToExclude(
                        "/api/staff/**",
                        "/api/admin/**",
                        "/admin/**"
                )
                .build();
    }

    /**
     * Group: Admin APIs
     * All admin management endpoints
     */
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("3-admin")
                .pathsToMatch(
                        "/admin/api/**",
                        "/admin/orders/api/**",
                        "/admin/products/api/**",
                        "/api/admin/**"
                )
                .build();
    }

    /**
     * Group: Staff APIs
     * Staff order management and operations
     */
    @Bean
    public GroupedOpenApi staffApi() {
        return GroupedOpenApi.builder()
                .group("2-staff")
                .pathsToMatch("/api/staff/**")
                .build();
    }

    /**
     * Group: WebSocket APIs
     * Real-time communication endpoints
     */
    @Bean
    public GroupedOpenApi websocketApi() {
        return GroupedOpenApi.builder()
                .group("4-websocket")
                .pathsToMatch("/ws/**")
                .build();
    }
}
