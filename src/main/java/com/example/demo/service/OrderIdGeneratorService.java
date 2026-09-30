package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class OrderIdGeneratorService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Generate Order ID in format DDMMYYXXX
     * Where XXX is the order number for that day (starting from 1)
     * Example: 1510251 (first order on 15/10/2025)
     */
    @Transactional
    public String generateOrderId() {
        LocalDate today = LocalDate.now();
        String dateStr = today.format(DateTimeFormatter.ofPattern("ddMMyy"));
        
        // Get next sequence number for today
        String sql = "SELECT nextval('daily_order_counter')";
        Long sequenceNumber = jdbcTemplate.queryForObject(sql, Long.class);
        
        // Format: DDMMYY + sequence number (no padding)
        return dateStr + sequenceNumber;
    }

    /**
     * Get the next order number for today (for testing purposes)
     */
    @Transactional(readOnly = true)
    public Long getNextOrderNumber() {
        String sql = "SELECT nextval('daily_order_counter')";
        return jdbcTemplate.queryForObject(sql, Long.class);
    }
}
