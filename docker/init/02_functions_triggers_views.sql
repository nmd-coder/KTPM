-- =============================================
-- FLOWER STORE MANAGEMENT SYSTEM - FUNCTIONS, TRIGGERS & VIEWS
-- PostgreSQL Database Logic
-- Version: 2.0 - Consolidated Functions
-- =============================================

-- =============================================
-- 1. PRODUCT MANAGEMENT FUNCTIONS & TRIGGERS
-- =============================================

-- Function to update product status based on stock
CREATE OR REPLACE FUNCTION update_product_status_by_stock()
RETURNS TRIGGER AS $$
BEGIN
    -- Auto update status to OUT_OF_STOCK if stock is 0
    IF NEW.stock_quantity = 0 AND NEW.status != 'DISCONTINUED' THEN
        NEW.status = 'OUT_OF_STOCK';
    -- Auto update status to ACTIVE if stock > 0 and currently OUT_OF_STOCK
    ELSIF NEW.stock_quantity > 0 AND OLD.status = 'OUT_OF_STOCK' THEN
        NEW.status = 'ACTIVE';
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Function to validate product data
CREATE OR REPLACE FUNCTION validate_product_data()
RETURNS TRIGGER AS $$
BEGIN
    -- Validate price
    IF NEW.price <= 0 THEN
        RAISE EXCEPTION 'Product price must be greater than 0';
    END IF;
    
    -- Validate stock quantity
    IF NEW.stock_quantity < 0 THEN
        RAISE EXCEPTION 'Stock quantity cannot be negative';
    END IF;
    
    -- Validate name
    IF LENGTH(TRIM(NEW.name)) < 3 THEN
        RAISE EXCEPTION 'Product name must be at least 3 characters long';
    END IF;
    
    -- Validate shipping dimensions
    IF NEW.weight_g IS NOT NULL AND NEW.weight_g <= 0 THEN
        RAISE EXCEPTION 'Weight must be greater than 0';
    END IF;
    
    IF NEW.length_cm IS NOT NULL AND NEW.length_cm <= 0 THEN
        RAISE EXCEPTION 'Length must be greater than 0';
    END IF;
    
    IF NEW.width_cm IS NOT NULL AND NEW.width_cm <= 0 THEN
        RAISE EXCEPTION 'Width must be greater than 0';
    END IF;
    
    IF NEW.height_cm IS NOT NULL AND NEW.height_cm <= 0 THEN
        RAISE EXCEPTION 'Height must be greater than 0';
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers for product management
CREATE TRIGGER trigger_validate_product
    BEFORE INSERT OR UPDATE ON products
    FOR EACH ROW
    EXECUTE FUNCTION validate_product_data();

CREATE TRIGGER trigger_update_product_status
    BEFORE INSERT OR UPDATE ON products
    FOR EACH ROW
    EXECUTE FUNCTION update_product_status_by_stock();

-- =============================================
-- 2. EMPLOYEE & USER MANAGEMENT FUNCTIONS
-- =============================================

-- Function: Auto-generate Employee Code
CREATE OR REPLACE FUNCTION generate_employee_code() RETURNS TRIGGER AS $$
DECLARE
    new_code VARCHAR(20);
    max_num INTEGER;
BEGIN
    -- Only generate for STAFF and ADMIN roles
    IF NEW.role IN ('STAFF', 'ADMIN') AND NEW.employee_code IS NULL THEN
        -- Get the maximum employee number
        SELECT COALESCE(MAX(CAST(SUBSTRING(employee_code FROM 4) AS INTEGER)), 0)
        INTO max_num
        FROM Users
        WHERE employee_code LIKE 'EMP%';
        
        -- Generate new code (e.g., EMP0001, EMP0002, ...)
        new_code := 'EMP' || LPAD((max_num + 1)::TEXT, 4, '0');
        NEW.employee_code := new_code;
        
        -- Set hire date if not provided
        IF NEW.hire_date IS NULL THEN
            NEW.hire_date := CURRENT_DATE;
        END IF;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for employee code generation
CREATE TRIGGER trg_generate_employee_code
    BEFORE INSERT ON Users
    FOR EACH ROW
    EXECUTE FUNCTION generate_employee_code();

-- Function: Update last login timestamp
CREATE OR REPLACE FUNCTION update_last_login(user_id_param BIGINT) RETURNS VOID AS $$
BEGIN
    UPDATE Users 
    SET last_login = CURRENT_TIMESTAMP
    WHERE id = user_id_param;
END;
$$ LANGUAGE plpgsql;

-- Function: Get employee statistics
CREATE OR REPLACE FUNCTION get_employee_stats()
RETURNS TABLE(
    total_employees BIGINT,
    active_employees BIGINT,
    inactive_employees BIGINT,
    staff_count BIGINT,
    admin_count BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COUNT(*) FILTER (WHERE role IN ('STAFF', 'ADMIN')) as total_employees,
        COUNT(*) FILTER (WHERE role IN ('STAFF', 'ADMIN') AND is_active = TRUE) as active_employees,
        COUNT(*) FILTER (WHERE role IN ('STAFF', 'ADMIN') AND is_active = FALSE) as inactive_employees,
        COUNT(*) FILTER (WHERE role = 'STAFF') as staff_count,
        COUNT(*) FILTER (WHERE role = 'ADMIN') as admin_count
    FROM Users;
END;
$$ LANGUAGE plpgsql;

-- Function: Get customer statistics
CREATE OR REPLACE FUNCTION get_customer_stats()
RETURNS TABLE(
    total_customers BIGINT,
    active_customers BIGINT,
    inactive_customers BIGINT,
    customers_with_orders BIGINT,
    new_customers_this_month BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COUNT(*) FILTER (WHERE role = 'CUSTOMER') as total_customers,
        COUNT(*) FILTER (WHERE role = 'CUSTOMER' AND is_active = TRUE) as active_customers,
        COUNT(*) FILTER (WHERE role = 'CUSTOMER' AND is_active = FALSE) as inactive_customers,
        COUNT(DISTINCT o.user_id) as customers_with_orders,
        COUNT(*) FILTER (WHERE role = 'CUSTOMER' AND created_at >= DATE_TRUNC('month', CURRENT_DATE)) as new_customers_this_month
    FROM Users u
    LEFT JOIN Orders o ON u.id = o.user_id AND u.role = 'CUSTOMER';
END;
$$ LANGUAGE plpgsql;

-- Function: Get voucher statistics
CREATE OR REPLACE FUNCTION get_voucher_stats()
RETURNS TABLE(
    total_vouchers BIGINT,
    active_vouchers BIGINT,
    expired_vouchers BIGINT,
    used_up_vouchers BIGINT,
    total_uses BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COUNT(*) as total_vouchers,
        COUNT(*) FILTER (WHERE is_active = TRUE AND expiry_date >= CURRENT_DATE 
                        AND (max_uses IS NULL OR uses < max_uses)) as active_vouchers,
        COUNT(*) FILTER (WHERE expiry_date < CURRENT_DATE) as expired_vouchers,
        COUNT(*) FILTER (WHERE max_uses IS NOT NULL AND uses >= max_uses) as used_up_vouchers,
        COALESCE(SUM(uses), 0) as total_uses
    FROM Vouchers;
END;
$$ LANGUAGE plpgsql;

-- Function: Update voucher usage
CREATE OR REPLACE FUNCTION increment_voucher_usage() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.voucher_id IS NOT NULL THEN
        UPDATE Vouchers 
        SET uses = uses + 1
        WHERE id = NEW.voucher_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for voucher usage increment
CREATE TRIGGER trg_increment_voucher_usage
    AFTER INSERT ON Orders
    FOR EACH ROW
    WHEN (NEW.voucher_id IS NOT NULL)
    EXECUTE FUNCTION increment_voucher_usage();

-- Function: Calculate employee monthly salary
CREATE OR REPLACE FUNCTION calculate_employee_monthly_hours(
    employee_id BIGINT,
    month_date DATE
) RETURNS NUMERIC AS $$
DECLARE
    total_hours NUMERIC;
BEGIN
    SELECT COALESCE(SUM(hours_worked), 0)
    INTO total_hours
    FROM TimeSheets
    WHERE staff_id = employee_id
    AND DATE_TRUNC('month', date) = DATE_TRUNC('month', month_date);
    
    RETURN total_hours;
END;
$$ LANGUAGE plpgsql;

-- Procedure: Generate monthly salaries for all employees
CREATE OR REPLACE PROCEDURE generate_monthly_salaries(month_date DATE)
LANGUAGE plpgsql
AS $$
DECLARE
    emp_record RECORD;
    work_hours NUMERIC;
BEGIN
    -- Loop through all active employees
    FOR emp_record IN 
        SELECT id, salary_per_hour 
        FROM Users 
        WHERE role IN ('STAFF', 'ADMIN') 
        AND is_active = TRUE
        AND salary_per_hour > 0
    LOOP
        -- Calculate total hours worked
        work_hours := calculate_employee_monthly_hours(emp_record.id, month_date);
        
        -- Insert or update salary record
        INSERT INTO Salaries (user_id, base_salary, working_time, month_year, created_at)
        VALUES (emp_record.id, emp_record.salary_per_hour, work_hours, 
                DATE_TRUNC('month', month_date)::DATE, CURRENT_TIMESTAMP)
        ON CONFLICT (user_id, month_year) 
        DO UPDATE SET 
            base_salary = emp_record.salary_per_hour,
            working_time = work_hours,
            updated_at = CURRENT_TIMESTAMP;
    END LOOP;
    
    RAISE NOTICE 'Monthly salaries generated for %', TO_CHAR(month_date, 'YYYY-MM');
END;
$$;

-- =============================================
-- 3. CHAT SYSTEM FUNCTIONS & TRIGGERS
-- =============================================

-- Function to update conversation's last_message_at timestamp
CREATE OR REPLACE FUNCTION update_conversation_last_message() RETURNS TRIGGER AS $$
BEGIN
    UPDATE Conversations
    SET last_message_at = NEW.sent_at,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = NEW.conversation_id;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to automatically update conversation timestamp when new message is added
CREATE TRIGGER trg_update_conversation_last_message
    AFTER INSERT ON Messages
    FOR EACH ROW
    WHEN (NEW.conversation_id IS NOT NULL)
    EXECUTE FUNCTION update_conversation_last_message();

-- Function to get conversation statistics
CREATE OR REPLACE FUNCTION get_conversation_stats()
RETURNS TABLE(
    total_conversations BIGINT,
    open_conversations BIGINT,
    assigned_conversations BIGINT,
    closed_conversations BIGINT,
    avg_response_time_minutes NUMERIC
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COUNT(*) as total_conversations,
        COUNT(*) FILTER (WHERE status = 'OPEN') as open_conversations,
        COUNT(*) FILTER (WHERE status = 'ASSIGNED') as assigned_conversations,
        COUNT(*) FILTER (WHERE status = 'CLOSED') as closed_conversations,
        COALESCE(AVG(EXTRACT(EPOCH FROM (closed_at - created_at)) / 60), 0)::NUMERIC as avg_response_time_minutes
    FROM Conversations;
END;
$$ LANGUAGE plpgsql;

-- Function to get staff workload (v1 - from chat system)
CREATE OR REPLACE FUNCTION get_staff_workload(staff_id_param BIGINT)
RETURNS TABLE(
    active_conversations BIGINT,
    total_assigned BIGINT,
    unread_messages BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COUNT(*) FILTER (WHERE c.status IN ('OPEN', 'ASSIGNED')) as active_conversations,
        COUNT(*) as total_assigned,
        COALESCE((
            SELECT COUNT(*) 
            FROM Messages m 
            WHERE m.receiver_id = staff_id_param 
            AND m.is_read = FALSE
        ), 0) as unread_messages
    FROM Conversations c
    WHERE c.assigned_staff_id = staff_id_param;
END;
$$ LANGUAGE plpgsql;

-- Function to auto-assign conversation to available staff (v1 - from chat system)
CREATE OR REPLACE FUNCTION auto_assign_conversation(conversation_id_param BIGINT)
RETURNS BIGINT AS $$
DECLARE
    selected_staff_id BIGINT;
BEGIN
    -- Find staff with least active conversations
    SELECT u.id INTO selected_staff_id
    FROM Users u
    LEFT JOIN Conversations c ON c.assigned_staff_id = u.id AND c.status IN ('OPEN', 'ASSIGNED')
    WHERE u.role = 'STAFF' AND u.is_active = TRUE
    GROUP BY u.id
    ORDER BY COUNT(c.id) ASC, RANDOM()
    LIMIT 1;
    
    -- Assign conversation to selected staff
    IF selected_staff_id IS NOT NULL THEN
        UPDATE Conversations
        SET assigned_staff_id = selected_staff_id,
            status = 'ASSIGNED',
            updated_at = CURRENT_TIMESTAMP
        WHERE id = conversation_id_param;
    END IF;
    
    RETURN selected_staff_id;
END;
$$ LANGUAGE plpgsql;

-- =============================================
-- 4. AI CHAT SYSTEM FUNCTIONS & TRIGGERS
-- =============================================

-- Function to update staff workload (latest version from AI chat system)
CREATE OR REPLACE FUNCTION update_staff_workload() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.assigned_staff_id IS NOT NULL THEN
        -- Update workload when conversation is assigned
        INSERT INTO staff_presence (staff_id, workload, last_activity_at)
        VALUES (NEW.assigned_staff_id, 1, now())
        ON CONFLICT (staff_id) DO UPDATE
        SET workload = staff_presence.workload + 1,
            last_activity_at = now(),
            updated_at = now();
    END IF;
    
    IF OLD.assigned_staff_id IS NOT NULL AND NEW.status = 'CLOSED' THEN
        -- Decrease workload when conversation is closed
        UPDATE staff_presence
        SET workload = GREATEST(workload - 1, 0),
            last_activity_at = now(),
            updated_at = now()
        WHERE staff_id = OLD.assigned_staff_id;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to automatically update staff workload
CREATE TRIGGER trg_update_staff_workload
    AFTER INSERT OR UPDATE ON Conversations
    FOR EACH ROW
    EXECUTE FUNCTION update_staff_workload();

-- Function to calculate wait time in handoff queue
CREATE OR REPLACE FUNCTION calculate_handoff_wait_time() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.assigned_at IS NOT NULL AND OLD.assigned_at IS NULL THEN
        NEW.wait_time_seconds = EXTRACT(EPOCH FROM (NEW.assigned_at - NEW.enqueued_at))::INTEGER;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to calculate wait time
CREATE TRIGGER trg_calculate_handoff_wait_time
    BEFORE UPDATE ON handoff_queue
    FOR EACH ROW
    WHEN (NEW.assigned_at IS NOT NULL AND OLD.assigned_at IS NULL)
    EXECUTE FUNCTION calculate_handoff_wait_time();

-- Function to get AI chat statistics
CREATE OR REPLACE FUNCTION get_ai_chat_stats(
    start_date TIMESTAMPTZ DEFAULT now() - INTERVAL '7 days',
    end_date TIMESTAMPTZ DEFAULT now()
)
RETURNS TABLE(
    total_messages BIGINT,
    ai_handled BIGINT,
    staff_handoff BIGINT,
    ai_containment_rate NUMERIC,
    avg_confidence NUMERIC,
    avg_response_time_ms NUMERIC,
    top_intent VARCHAR,
    top_handoff_reason VARCHAR
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COUNT(*) as total_messages,
        COUNT(*) FILTER (WHERE need_handoff = FALSE) as ai_handled,
        COUNT(*) FILTER (WHERE need_handoff = TRUE) as staff_handoff,
        ROUND(
            (COUNT(*) FILTER (WHERE need_handoff = FALSE)::NUMERIC / NULLIF(COUNT(*), 0)) * 100, 
            2
        ) as ai_containment_rate,
        ROUND(AVG(confidence), 3) as avg_confidence,
        ROUND(AVG(processing_time_ms), 2) as avg_response_time_ms,
        (
            SELECT intent 
            FROM routing_decisions 
            WHERE created_at BETWEEN start_date AND end_date
            GROUP BY intent 
            ORDER BY COUNT(*) DESC 
            LIMIT 1
        ) as top_intent,
        (
            SELECT handoff_reason 
            FROM routing_decisions 
            WHERE created_at BETWEEN start_date AND end_date 
            AND need_handoff = TRUE
            AND handoff_reason IS NOT NULL
            GROUP BY handoff_reason 
            ORDER BY COUNT(*) DESC 
            LIMIT 1
        ) as top_handoff_reason
    FROM routing_decisions
    WHERE created_at BETWEEN start_date AND end_date;
END;
$$ LANGUAGE plpgsql;

-- Function to get available staff for auto-assignment
CREATE OR REPLACE FUNCTION get_available_staff_for_assignment()
RETURNS TABLE(
    staff_id BIGINT,
    staff_name TEXT,
    current_workload INTEGER,
    max_workload INTEGER,
    availability_score NUMERIC
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        u.id as staff_id,
        u.firstname || ' ' || u.lastname as staff_name,
        COALESCE(sp.workload, 0) as current_workload,
        COALESCE(sp.max_workload, 5) as max_workload,
        CASE 
            WHEN sp.online = TRUE AND sp.status = 'AVAILABLE' THEN
                (1.0 - (COALESCE(sp.workload, 0)::NUMERIC / COALESCE(sp.max_workload, 5))) * 100
            ELSE 0
        END as availability_score
    FROM Users u
    LEFT JOIN staff_presence sp ON u.id = sp.staff_id
    WHERE u.role = 'STAFF' 
    AND u.is_active = TRUE
    AND (sp.online = TRUE OR sp.online IS NULL)
    AND (sp.workload < sp.max_workload OR sp.workload IS NULL)
    ORDER BY availability_score DESC, COALESCE(sp.workload, 0) ASC;
END;
$$ LANGUAGE plpgsql;

-- Function to auto-assign conversation from handoff queue (latest version)
CREATE OR REPLACE FUNCTION auto_assign_from_handoff_queue(queue_id_param BIGINT)
RETURNS BIGINT AS $$
DECLARE
    selected_staff_id BIGINT;
    conv_id BIGINT;
BEGIN
    -- Get the best available staff
    SELECT staff_id INTO selected_staff_id
    FROM get_available_staff_for_assignment()
    WHERE availability_score > 0
    LIMIT 1;
    
    IF selected_staff_id IS NOT NULL THEN
        -- Get conversation ID from queue
        SELECT conversation_id INTO conv_id
        FROM handoff_queue
        WHERE id = queue_id_param;
        
        -- Assign conversation to staff
        UPDATE Conversations
        SET assigned_staff_id = selected_staff_id,
            status = 'ASSIGNED',
            updated_at = now()
        WHERE id = conv_id;
        
        -- Update handoff queue
        UPDATE handoff_queue
        SET assigned_to_staff_id = selected_staff_id,
            assigned_at = now()
        WHERE id = queue_id_param;
        
        RETURN selected_staff_id;
    END IF;
    
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- =============================================
-- 5. VIEWS FOR REPORTING
-- =============================================

-- View: Employee summary with work statistics
CREATE OR REPLACE VIEW employee_summary AS
SELECT 
    u.id,
    u.employee_code,
    u.firstname,
    u.lastname,
    u.email,
    u.phone,
    u.role,
    u.position,
    u.department,
    u.salary_per_hour,
    u.hire_date,
    u.is_active,
    u.last_login,
    u.created_at,
    COUNT(DISTINCT ts.id) as total_shifts,
    COALESCE(SUM(ts.hours_worked), 0) as total_hours_worked,
    COUNT(DISTINCT DATE_TRUNC('month', ts.date)) as months_worked
FROM Users u
LEFT JOIN TimeSheets ts ON u.id = ts.staff_id
WHERE u.role IN ('STAFF', 'ADMIN')
GROUP BY u.id;

COMMENT ON VIEW employee_summary IS 'Consolidated view of employee information with work statistics';

-- View: Customer summary with order statistics
CREATE OR REPLACE VIEW customer_summary AS
SELECT 
    u.id,
    u.firstname,
    u.lastname,
    u.email,
    u.phone,
    u.is_active,
    u.last_login,
    u.created_at,
    COUNT(DISTINCT o.id) as total_orders,
    COALESCE(SUM(o.total_amount), 0) as total_spent,
    MAX(o.order_date) as last_order_date,
    COUNT(DISTINCT r.id) as total_reviews
FROM Users u
LEFT JOIN Orders o ON u.id = o.user_id
LEFT JOIN Reviews r ON u.id = r.user_id
WHERE u.role = 'CUSTOMER'
GROUP BY u.id;

COMMENT ON VIEW customer_summary IS 'Consolidated view of customer information with purchase history';

-- View: Voucher summary with usage statistics
CREATE OR REPLACE VIEW voucher_summary AS
SELECT 
    v.id,
    v.code,
    v.name,
    v.description,
    v.discount_type,
    v.discount_value,
    v.max_discount_amount,
    v.min_order_value,
    v.expiry_date,
    v.max_uses,
    v.uses,
    v.is_active,
    v.created_at,
    CASE 
        WHEN v.expiry_date < CURRENT_DATE THEN 'EXPIRED'
        WHEN v.max_uses IS NOT NULL AND v.uses >= v.max_uses THEN 'USED_UP'
        WHEN v.is_active = FALSE THEN 'INACTIVE'
        ELSE 'ACTIVE'
    END as status,
    CASE 
        WHEN v.max_uses IS NULL THEN NULL
        ELSE ROUND((v.uses::NUMERIC / v.max_uses::NUMERIC) * 100, 2)
    END as usage_percentage,
    COUNT(DISTINCT o.id) as times_used_in_orders,
    COALESCE(SUM(o.total_amount), 0) as total_order_value
FROM Vouchers v
LEFT JOIN Orders o ON v.id = o.voucher_id
GROUP BY v.id;

COMMENT ON VIEW voucher_summary IS 'Consolidated view of voucher information with usage statistics';

-- View: Staff chat performance metrics
CREATE OR REPLACE VIEW staff_chat_performance AS
SELECT 
    u.id as staff_id,
    u.firstname || ' ' || u.lastname as staff_name,
    u.employee_code,
    COUNT(DISTINCT c.id) as total_conversations,
    COUNT(DISTINCT c.id) FILTER (WHERE c.status = 'ASSIGNED') as active_conversations,
    COUNT(DISTINCT c.id) FILTER (WHERE c.status = 'CLOSED') as closed_conversations,
    COUNT(DISTINCT m.id) as total_messages_sent,
    AVG(EXTRACT(EPOCH FROM (c.closed_at - c.created_at)) / 60) as avg_resolution_time_minutes
FROM Users u
LEFT JOIN Conversations c ON u.id = c.assigned_staff_id
LEFT JOIN Messages m ON u.id = m.sender_id AND m.conversation_id IS NOT NULL
WHERE u.role = 'STAFF'
GROUP BY u.id, u.firstname, u.lastname, u.employee_code;

COMMENT ON VIEW staff_chat_performance IS 'Performance metrics for staff chat support';

-- View: Conversation summary
CREATE OR REPLACE VIEW conversation_summary AS
SELECT 
    c.id,
    c.status,
    c.priority,
    c.created_at,
    c.last_message_at,
    c.closed_at,
    customer.id as customer_id,
    customer.firstname || ' ' || customer.lastname as customer_name,
    customer.email as customer_email,
    staff.id as staff_id,
    staff.firstname || ' ' || staff.lastname as staff_name,
    staff.employee_code as staff_code,
    (SELECT COUNT(*) FROM Messages WHERE conversation_id = c.id) as message_count,
    (SELECT COUNT(*) FROM Messages WHERE conversation_id = c.id AND is_read = FALSE) as unread_count
FROM Conversations c
INNER JOIN Users customer ON c.customer_id = customer.id
LEFT JOIN Users staff ON c.assigned_staff_id = staff.id;

COMMENT ON VIEW conversation_summary IS 'Detailed summary of all conversations with customer and staff info';

-- View: AI performance metrics
CREATE OR REPLACE VIEW ai_performance_summary AS
SELECT 
    DATE(rd.created_at) as date,
    COUNT(*) as total_decisions,
    COUNT(*) FILTER (WHERE rd.need_handoff = FALSE) as ai_handled,
    COUNT(*) FILTER (WHERE rd.need_handoff = TRUE) as staff_handoff,
    ROUND(AVG(rd.confidence), 3) as avg_confidence,
    ROUND(AVG(rd.processing_time_ms), 2) as avg_processing_time_ms,
    COUNT(DISTINCT rd.conversation_id) as unique_conversations
FROM routing_decisions rd
GROUP BY DATE(rd.created_at)
ORDER BY date DESC;

COMMENT ON VIEW ai_performance_summary IS 'Daily AI performance metrics';

-- View: Handoff queue status
CREATE OR REPLACE VIEW handoff_queue_status AS
SELECT 
    hq.id,
    hq.conversation_id,
    c.customer_id,
    customer.firstname || ' ' || customer.lastname as customer_name,
    hq.priority,
    hq.handoff_reason,
    hq.enqueued_at,
    hq.assigned_at,
    hq.assigned_to_staff_id,
    staff.firstname || ' ' || staff.lastname as staff_name,
    CASE 
        WHEN hq.resolved_at IS NOT NULL THEN 'RESOLVED'
        WHEN hq.assigned_at IS NOT NULL THEN 'ASSIGNED'
        ELSE 'WAITING'
    END as queue_status,
    EXTRACT(EPOCH FROM (COALESCE(hq.assigned_at, now()) - hq.enqueued_at))::INTEGER as wait_seconds
FROM handoff_queue hq
INNER JOIN Conversations c ON hq.conversation_id = c.id
INNER JOIN Users customer ON c.customer_id = customer.id
LEFT JOIN Users staff ON hq.assigned_to_staff_id = staff.id
ORDER BY hq.priority DESC, hq.enqueued_at ASC;

COMMENT ON VIEW handoff_queue_status IS 'Current status of handoff queue with customer and staff info';
