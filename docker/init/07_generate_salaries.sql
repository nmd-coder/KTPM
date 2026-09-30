-- =============================================
-- GENERATE SALARIES FOR STAFF
-- Tính lương tự động từ tháng 4/2025 đến 10/2025
-- Dựa trên dữ liệu chấm công và salary_per_hour
-- =============================================

BEGIN;

-- Cập nhật salary_per_hour cho nhân viên (nếu chưa có)
UPDATE Users 
SET salary_per_hour = 50000.00 
WHERE id = 6 AND (salary_per_hour IS NULL OR salary_per_hour = 0);

UPDATE Users 
SET salary_per_hour = 55000.00 
WHERE id = 7 AND (salary_per_hour IS NULL OR salary_per_hour = 0);

-- Generate salaries cho từng tháng
CALL generate_monthly_salaries('2025-04-01');
CALL generate_monthly_salaries('2025-05-01');
CALL generate_monthly_salaries('2025-06-01');
CALL generate_monthly_salaries('2025-07-01');
CALL generate_monthly_salaries('2025-08-01');
CALL generate_monthly_salaries('2025-09-01');
CALL generate_monthly_salaries('2025-10-01');

-- Verify results
SELECT 
    u.firstname || ' ' || u.lastname as staff_name,
    u.salary_per_hour,
    TO_CHAR(s.month_year, 'YYYY-MM') as month,
    s.working_time as hours,
    s.total_salary,
    s.status
FROM Salaries s
JOIN Users u ON s.user_id = u.id
WHERE s.month_year >= '2025-04-01'
ORDER BY u.id, s.month_year;

COMMIT;

-- =============================================
-- KẾT QUẢ DỰ KIẾN:
-- - Hoàng Lan (ID=6): ~7 bản ghi lương (tháng 4-10)
-- - Đỗ Minh (ID=7): ~7 bản ghi lương (tháng 4-10)
-- - Mỗi bản ghi: working_time = tổng giờ làm trong tháng
-- - total_salary = salary_per_hour * working_time
-- =============================================
