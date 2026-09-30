package com.example.demo.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Scheduled task to automatically generate monthly salaries
 * Runs on the last day of each month at 23:00
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SalaryScheduler {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Auto-generate salaries at 11:00 PM on the last day of each month
     * Cron: "0 0 23 L * ?" = At 23:00 on the last day of every month
     */
    @Scheduled(cron = "0 0 23 L * ?")
    public void generateMonthlySalaries() {
        try {
            LocalDate today = LocalDate.now();
            LocalDate firstDayOfMonth = today.withDayOfMonth(1);
            
            log.info("🔄 Starting automatic salary generation for month: {}", 
                    firstDayOfMonth.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")));
            
            // Call PostgreSQL procedure
            jdbcTemplate.execute(String.format(
                "CALL generate_monthly_salaries('%s'::DATE)", 
                firstDayOfMonth
            ));
            
            log.info("✅ Successfully generated salaries for month: {}", 
                    firstDayOfMonth.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")));
            
        } catch (Exception e) {
            log.error("❌ Error generating monthly salaries", e);
        }
    }
    
    /**
     * Manual trigger for generating salaries for a specific month
     * Can be called from admin API
     */
    public void generateSalariesForMonth(YearMonth yearMonth) {
        try {
            LocalDate firstDayOfMonth = yearMonth.atDay(1);
            
            log.info("🔄 Manually generating salaries for month: {}", yearMonth);
            
            jdbcTemplate.execute(String.format(
                "CALL generate_monthly_salaries('%s'::DATE)", 
                firstDayOfMonth
            ));
            
            log.info("✅ Successfully generated salaries for month: {}", yearMonth);
            
        } catch (Exception e) {
            log.error("❌ Error generating salaries for month: {}", yearMonth, e);
            throw new RuntimeException("Failed to generate salaries: " + e.getMessage());
        }
    }
}
