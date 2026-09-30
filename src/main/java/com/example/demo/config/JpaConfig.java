package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(basePackages = "com.example.demo.repository")
@EnableJpaAuditing
@EnableTransactionManagement
public class JpaConfig {
    // This configuration enables JPA auditing for automatic timestamp management
    // and transaction management for @Transactional annotations
}
