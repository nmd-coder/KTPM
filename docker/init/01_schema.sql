-- =============================================
-- FLOWER STORE MANAGEMENT SYSTEM - SCHEMA
-- PostgreSQL Database Schema
-- Version: 2.0 - Consolidated Schema
-- =============================================

-- Create ENUM types (uppercase to match Java enums)
CREATE TYPE user_role AS ENUM ('CUSTOMER', 'STAFF', 'ADMIN');
CREATE TYPE order_status AS ENUM ('PENDING', 'PROCESSING', 'SHIPPED', 'COMPLETED', 'CANCELLED');
CREATE TYPE discount_type AS ENUM ('PERCENTAGE', 'FIXED');
CREATE TYPE payment_method AS ENUM ('COD', 'MOMO', 'BANK_TRANSFER', 'CREDIT_CARD');
CREATE TYPE product_status AS ENUM ('ACTIVE', 'INACTIVE', 'OUT_OF_STOCK', 'DISCONTINUED');
CREATE TYPE salary_status AS ENUM ('PENDING', 'PAID', 'OVERDUE');

-- Table: Users
CREATE TABLE Users (
    id BIGSERIAL PRIMARY KEY,
    firstname VARCHAR(100) NOT NULL,
    lastname VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20) UNIQUE NOT NULL,
    avatar VARCHAR(255) DEFAULT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    -- Employee-specific fields
    employee_code VARCHAR(20) UNIQUE,
    hire_date DATE,
    position VARCHAR(100),
    department VARCHAR(100),
    salary_per_hour NUMERIC(10,2) DEFAULT 0.00 CHECK (salary_per_hour >= 0),
    is_active BOOLEAN DEFAULT TRUE,
    last_login TIMESTAMP,
    -- AI Customer Segmentation fields
    customer_segment VARCHAR(20) DEFAULT NULL,
    customer_segment_updated_at TIMESTAMP DEFAULT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CHECK (LENGTH(password) >= 8)
);
COMMENT ON TABLE Users IS 'Stores user information including customers, staff, and admins';
COMMENT ON COLUMN Users.employee_code IS 'Unique employee code for staff identification';
COMMENT ON COLUMN Users.hire_date IS 'Date when employee was hired';
COMMENT ON COLUMN Users.position IS 'Employee position/job title';
COMMENT ON COLUMN Users.department IS 'Employee department';
COMMENT ON COLUMN Users.salary_per_hour IS 'Hourly wage for employee';
COMMENT ON COLUMN Users.is_active IS 'Whether user account is active';
COMMENT ON COLUMN Users.last_login IS 'Last login timestamp';
COMMENT ON COLUMN Users.customer_segment IS 'AI-generated customer segment: VIP, NEW, AT_RISK, or NULL';
COMMENT ON COLUMN Users.customer_segment_updated_at IS 'Timestamp when customer segment was last updated';
CREATE INDEX idx_users_email ON Users(email);
CREATE INDEX idx_users_phone ON Users(phone);
CREATE INDEX idx_users_role ON Users(role);
CREATE INDEX idx_users_employee_code ON Users(employee_code) WHERE employee_code IS NOT NULL;
CREATE INDEX idx_users_is_active ON Users(is_active);
CREATE INDEX idx_users_role_active ON Users(role, is_active);
CREATE INDEX idx_users_customer_segment ON Users(customer_segment) WHERE customer_segment IS NOT NULL;

-- Table: Addresses
CREATE TABLE Addresses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    street VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    province VARCHAR(100) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    -- GHN Address Integration (2-level & 3-level support)
    province_id INT,
    district_id INT,
    ward_code VARCHAR(20),
    address_detail VARCHAR(255),
    province_name VARCHAR(100),
    district_name VARCHAR(100),
    ward_name VARCHAR(100),
    address_mode VARCHAR(8) DEFAULT 'OLD' CHECK (address_mode IN ('OLD', 'NEW')),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE
);
COMMENT ON TABLE Addresses IS 'User delivery addresses with GHN support (OLD=3-level, NEW=2-level)';
CREATE INDEX idx_addresses_user_id ON Addresses(user_id);
CREATE INDEX idx_addresses_district_id ON Addresses(district_id);
CREATE INDEX idx_addresses_ward_code ON Addresses(ward_code);
-- Unique constraint: each user can only have ONE default address
CREATE UNIQUE INDEX ux_addresses_user_default ON Addresses(user_id) WHERE is_default = TRUE;

-- Table: Catalogs (Danh mục sản phẩm)
CREATE TABLE Catalogs (
    id BIGSERIAL PRIMARY KEY,
    value VARCHAR(100) UNIQUE NOT NULL,
    image VARCHAR(255) DEFAULT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT NULL
);
COMMENT ON TABLE Catalogs IS 'Product catalogs/categories (e.g., Tình yêu, Khai trương, Hoa cưới, Đám tang)';
COMMENT ON COLUMN Catalogs.image IS 'Catalog/category image path or URL';
CREATE INDEX idx_catalogs_value ON Catalogs(value);
CREATE INDEX idx_catalogs_image ON Catalogs(image) WHERE image IS NOT NULL;

-- Table: Products
CREATE TABLE Products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT DEFAULT NULL,
    price NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    image VARCHAR(255) DEFAULT NULL,
    stock_quantity INTEGER DEFAULT 0,
    status VARCHAR(50) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'OUT_OF_STOCK', 'DISCONTINUED')),
    -- GHN Shipping Dimensions (for shipping fee calculation)
    weight_g INT DEFAULT 500 CHECK (weight_g > 0),
    length_cm INT DEFAULT 20 CHECK (length_cm > 0),
    width_cm INT DEFAULT 20 CHECK (width_cm > 0),
    height_cm INT DEFAULT 30 CHECK (height_cm > 0),
    catalog_id BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CHECK (price >= 0),
    CHECK (stock_quantity >= 0),
    FOREIGN KEY (catalog_id) REFERENCES Catalogs(id) ON DELETE SET NULL
);
COMMENT ON TABLE Products IS 'Flower products catalog with shipping dimensions for GHN';
CREATE INDEX idx_products_name ON Products(name);
CREATE INDEX idx_products_catalog_id ON Products(catalog_id);
CREATE INDEX idx_products_stock_quantity ON Products(stock_quantity);

-- Table: DeliveryUnits
CREATE TABLE DeliveryUnits (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    estimated_time VARCHAR(50) DEFAULT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);
COMMENT ON TABLE DeliveryUnits IS 'Delivery service providers';
CREATE INDEX idx_deliveryunits_is_active ON DeliveryUnits(is_active);

-- Table: Vouchers
CREATE TABLE Vouchers (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL DEFAULT '',
    description TEXT,
    discount_value NUMERIC(10,2) NOT NULL,
    discount_type discount_type NOT NULL,
    max_discount_amount NUMERIC(10,2),
    expiry_date DATE NOT NULL,
    min_order_value NUMERIC(10,2) DEFAULT 0.00,
    max_uses INTEGER DEFAULT NULL,
    uses INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CHECK (discount_value > 0),
    CHECK (max_uses IS NULL OR uses <= max_uses)
);
COMMENT ON TABLE Vouchers IS 'Discount vouchers';
COMMENT ON COLUMN Vouchers.name IS 'Display name for voucher';
COMMENT ON COLUMN Vouchers.description IS 'Voucher description and terms';
COMMENT ON COLUMN Vouchers.max_discount_amount IS 'Maximum discount amount for percentage vouchers';
CREATE INDEX idx_vouchers_code ON Vouchers(code);
CREATE INDEX idx_vouchers_is_active ON Vouchers(is_active);

-- Table: Orders
CREATE TABLE Orders (
    id VARCHAR(20) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    total_amount NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    status order_status NOT NULL DEFAULT 'PENDING',
    order_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delivery_unit_id BIGINT DEFAULT NULL,
    voucher_id BIGINT DEFAULT NULL,
    address_id BIGINT NOT NULL,
    payment_method payment_method NOT NULL DEFAULT 'COD',
    notes TEXT DEFAULT NULL,
    -- GHN Shipping Fee (tracked separately for transparency)
    shipping_fee NUMERIC(10,2) NOT NULL DEFAULT 0.00 CHECK (shipping_fee >= 0),
    -- MoMo transaction ID for refund processing
    momo_trans_id VARCHAR(255) DEFAULT NULL,
    -- JPA Auditing columns (BaseEntity) - Required for Voucher.orders relationship
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES Users(id),
    FOREIGN KEY (delivery_unit_id) REFERENCES DeliveryUnits(id),
    FOREIGN KEY (voucher_id) REFERENCES Vouchers(id),
    FOREIGN KEY (address_id) REFERENCES Addresses(id),
    CHECK (total_amount >= 0)
);
COMMENT ON TABLE Orders IS 'Customer orders with status, payment info, and GHN shipping fee';
COMMENT ON COLUMN Orders.momo_trans_id IS 'MoMo transaction ID for refund processing';
CREATE INDEX idx_orders_user_id ON Orders(user_id);
CREATE INDEX idx_orders_status ON Orders(status);
CREATE INDEX idx_orders_user_status ON Orders(user_id, status);
CREATE INDEX idx_orders_total_amount ON Orders(total_amount);
CREATE INDEX idx_orders_shipping_fee ON Orders(shipping_fee);
CREATE INDEX idx_orders_momo_trans_id ON Orders(momo_trans_id);

-- Table: OrderItems
CREATE TABLE OrderItems (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(20) NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    price NUMERIC(10,2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES Orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES Products(id),
    CHECK (quantity > 0)
);
COMMENT ON TABLE OrderItems IS 'Items within an order';
CREATE INDEX idx_orderitems_order_id ON OrderItems(order_id);

-- Table: Carts
CREATE TABLE Carts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL,
    total_amount NUMERIC(10,2) DEFAULT 0.00,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    total_quantity INTEGER DEFAULT 0 CHECK (total_quantity >= 0),
    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE,
    CHECK (total_amount >= 0)
);
COMMENT ON TABLE Carts IS 'User shopping carts';

-- Table: cart_items
CREATE TABLE cart_items (
    id BIGSERIAL PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    FOREIGN KEY (cart_id) REFERENCES Carts(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES Products(id),
    CHECK (quantity > 0),
    UNIQUE (cart_id, product_id)
);
COMMENT ON TABLE cart_items IS 'Items in user carts';
CREATE UNIQUE INDEX idx_cartitems_unique ON cart_items(cart_id, product_id);

-- Table: Reviews
CREATE TABLE Reviews (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    rating SMALLINT NOT NULL,
    comment TEXT DEFAULT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    order_item_id BIGINT UNIQUE,
    -- Admin response fields
    admin_response TEXT DEFAULT NULL,
    admin_response_at TIMESTAMP DEFAULT NULL,
    admin_response_by BIGINT DEFAULT NULL,
    -- AI sentiment analysis
    sentiment VARCHAR(20) DEFAULT NULL,
    FOREIGN KEY (user_id) REFERENCES Users(id),
    FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE CASCADE,
    FOREIGN KEY (order_item_id) REFERENCES OrderItems(id) ON DELETE CASCADE,
    FOREIGN KEY (admin_response_by) REFERENCES Users(id) ON DELETE SET NULL,
    CHECK (rating BETWEEN 1 AND 5)
);
COMMENT ON TABLE Reviews IS 'Product reviews and ratings with admin response support';
COMMENT ON COLUMN Reviews.admin_response IS 'Admin response to customer review';
COMMENT ON COLUMN Reviews.admin_response_at IS 'Timestamp when admin responded';
COMMENT ON COLUMN Reviews.admin_response_by IS 'Admin user who responded';
COMMENT ON COLUMN Reviews.sentiment IS 'AI-analyzed sentiment: POSITIVE, NEUTRAL, NEGATIVE';
CREATE INDEX idx_reviews_product_id ON Reviews(product_id);
CREATE INDEX idx_reviews_admin_response_by ON Reviews(admin_response_by);
CREATE INDEX idx_reviews_admin_response_at ON Reviews(admin_response_at);

-- Table: Follows
CREATE TABLE Follows (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    followed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES Users(id),
    FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE CASCADE,
    UNIQUE (user_id, product_id)
);
COMMENT ON TABLE Follows IS 'User follows on products';
CREATE UNIQUE INDEX idx_follows_unique ON Follows(user_id, product_id);
CREATE INDEX idx_follows_product_id ON Follows(product_id);

-- Table: Messages
CREATE TABLE Messages (
    id BIGSERIAL PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT,
    content TEXT NOT NULL,
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    -- Chat system enhancements
    conversation_id BIGINT,
    message_type VARCHAR(20) DEFAULT 'TEXT',
    is_ai_generated BOOLEAN DEFAULT FALSE,
    attachments TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (sender_id) REFERENCES Users(id),
    FOREIGN KEY (receiver_id) REFERENCES Users(id)
);
COMMENT ON TABLE Messages IS 'User-to-user messaging with chat system enhancements';
COMMENT ON COLUMN Messages.conversation_id IS 'Links message to a conversation';
COMMENT ON COLUMN Messages.message_type IS 'Message type: TEXT, SYSTEM, AI_RESPONSE';
COMMENT ON COLUMN Messages.is_ai_generated IS 'Flag indicating if message was generated by AI';
COMMENT ON COLUMN Messages.attachments IS 'JSON string of file attachments';
CREATE INDEX idx_messages_conversation ON Messages(sender_id, receiver_id, sent_at);
CREATE INDEX idx_messages_sent_at ON Messages(sent_at);
CREATE INDEX idx_messages_conversation_id ON Messages(conversation_id);
CREATE INDEX idx_messages_is_read ON Messages(is_read);
CREATE INDEX idx_messages_message_type ON Messages(message_type);

-- Table: TimeSheets
CREATE TABLE TimeSheets (
    id BIGSERIAL PRIMARY KEY,
    staff_id BIGINT NOT NULL,
    check_in TIMESTAMP NOT NULL,
    check_out TIMESTAMP DEFAULT NULL,
    date DATE NOT NULL,
    hours_worked NUMERIC(5,2) DEFAULT 0.00,
    notes TEXT DEFAULT NULL,
    FOREIGN KEY (staff_id) REFERENCES Users(id),
    CHECK (hours_worked >= 0),
    CHECK (check_out IS NULL OR check_out > check_in)
);
COMMENT ON TABLE TimeSheets IS 'Staff work hours tracking';
CREATE INDEX idx_timesheets_staff_date ON TimeSheets(staff_id, date);

-- Table: Salaries
CREATE TABLE Salaries (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    base_salary NUMERIC(10,2) NOT NULL CHECK (base_salary >= 0),
    working_time NUMERIC(5,2) NOT NULL DEFAULT 0.00 CHECK (working_time >= 0),
    total_salary NUMERIC(10,2) GENERATED ALWAYS AS (base_salary * working_time) STORED,
    month_year DATE NOT NULL,
    status salary_status NOT NULL DEFAULT 'PENDING',
    notes TEXT DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE,
    UNIQUE (user_id, month_year)
);
COMMENT ON TABLE Salaries IS 'Simplified staff salary records, based on hourly rate and total work hours';
CREATE INDEX idx_salaries_user_id ON Salaries(user_id);
CREATE INDEX idx_salaries_month_year ON Salaries(month_year);

-- =============================================
-- CHAT SYSTEM TABLES
-- =============================================

-- Table: Conversations
CREATE TABLE Conversations (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    assigned_staff_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    priority VARCHAR(20) DEFAULT 'NORMAL',
    last_message_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    closed_at TIMESTAMP,
    notes TEXT,
    CONSTRAINT fk_conversation_customer FOREIGN KEY (customer_id) REFERENCES Users(id) ON DELETE CASCADE,
    CONSTRAINT fk_conversation_staff FOREIGN KEY (assigned_staff_id) REFERENCES Users(id) ON DELETE SET NULL,
    CONSTRAINT chk_conversation_status CHECK (status IN ('OPEN', 'ASSIGNED', 'CLOSED')),
    CONSTRAINT chk_conversation_priority CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT'))
);
COMMENT ON TABLE Conversations IS 'Conversation threads between customers and staff';
COMMENT ON COLUMN Conversations.customer_id IS 'Customer who initiated the conversation';
COMMENT ON COLUMN Conversations.assigned_staff_id IS 'Staff member assigned to handle this conversation';
COMMENT ON COLUMN Conversations.status IS 'Conversation status: OPEN (unassigned), ASSIGNED (active), CLOSED';
COMMENT ON COLUMN Conversations.priority IS 'Conversation priority level';
COMMENT ON COLUMN Conversations.last_message_at IS 'Timestamp of most recent message';
COMMENT ON COLUMN Conversations.notes IS 'Internal notes for staff about this conversation';
CREATE INDEX idx_conversations_status ON Conversations(status);
CREATE INDEX idx_conversations_assigned_staff ON Conversations(assigned_staff_id);
CREATE INDEX idx_conversations_customer ON Conversations(customer_id);
CREATE INDEX idx_conversations_last_message ON Conversations(last_message_at DESC);
CREATE INDEX idx_conversations_status_staff ON Conversations(status, assigned_staff_id);
CREATE INDEX idx_conversations_status_created ON Conversations(status, created_at DESC);

-- =============================================
-- AI CHAT SYSTEM TABLES
-- =============================================

-- Table: routing_decisions
CREATE TABLE routing_decisions (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES Conversations(id) ON DELETE CASCADE,
    message_id BIGINT REFERENCES Messages(id) ON DELETE SET NULL,
    intent VARCHAR(50) NOT NULL,
    confidence NUMERIC(4,3) NOT NULL CHECK (confidence >= 0 AND confidence <= 1),
    need_handoff BOOLEAN NOT NULL DEFAULT FALSE,
    suggest_handoff BOOLEAN NOT NULL DEFAULT FALSE,
    handoff_reason VARCHAR(50),
    ai_response TEXT,
    tools_used TEXT[], -- Array of tool names used
    processing_time_ms INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT chk_intent CHECK (intent IN ('SALES', 'SHIPPING', 'PROMOTION', 'ORDER_SUPPORT', 'PAYMENT', 'STORE_INFO', 'CHITCHAT', 'OTHER'))
);
COMMENT ON TABLE routing_decisions IS 'AI routing decisions and analysis history for chat messages';
COMMENT ON COLUMN routing_decisions.intent IS 'Detected intent type from AI analysis';
COMMENT ON COLUMN routing_decisions.confidence IS 'AI confidence score (0.0 to 1.0)';
COMMENT ON COLUMN routing_decisions.need_handoff IS 'Whether message requires staff intervention';
COMMENT ON COLUMN routing_decisions.suggest_handoff IS 'Whether AI suggests staff handoff';
COMMENT ON COLUMN routing_decisions.handoff_reason IS 'Reason for handoff: LOW_CONFIDENCE, PII_DETECTED, ORDER_INQUIRY, PAYMENT_ISSUE, EXPLICIT_REQUEST';
COMMENT ON COLUMN routing_decisions.tools_used IS 'Array of AI tools executed (product_search, shipping_fee, etc.)';
CREATE INDEX idx_routing_decisions_conversation ON routing_decisions(conversation_id);
CREATE INDEX idx_routing_decisions_intent ON routing_decisions(intent);
CREATE INDEX idx_routing_decisions_confidence ON routing_decisions(confidence);
CREATE INDEX idx_routing_decisions_created_at ON routing_decisions(created_at DESC);
CREATE INDEX idx_routing_decisions_need_handoff ON routing_decisions(need_handoff);

-- Table: handoff_queue
CREATE TABLE handoff_queue (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT UNIQUE NOT NULL REFERENCES Conversations(id) ON DELETE CASCADE,
    priority INTEGER NOT NULL DEFAULT 0 CHECK (priority >= 0 AND priority <= 10),
    handoff_reason VARCHAR(50) NOT NULL,
    tags TEXT[] DEFAULT '{}',
    customer_message TEXT,
    ai_context TEXT, -- AI's analysis/context for staff
    enqueued_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    assigned_at TIMESTAMPTZ,
    assigned_to_staff_id BIGINT REFERENCES Users(id) ON DELETE SET NULL,
    resolved_at TIMESTAMPTZ,
    wait_time_seconds INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT chk_handoff_reason CHECK (handoff_reason IN ('LOW_CONFIDENCE', 'PII_DETECTED', 'ORDER_INQUIRY', 'PAYMENT_ISSUE', 'EXPLICIT_REQUEST', 'AI_ERROR', 'COMPLEX_QUERY'))
);
COMMENT ON TABLE handoff_queue IS 'Queue of conversations waiting for or assigned to staff';
COMMENT ON COLUMN handoff_queue.priority IS 'Priority level (0=low, 5=normal, 10=urgent)';
COMMENT ON COLUMN handoff_queue.handoff_reason IS 'Reason why conversation was handed off to staff';
COMMENT ON COLUMN handoff_queue.tags IS 'Tags for categorization (e.g., payment, shipping, product)';
COMMENT ON COLUMN handoff_queue.ai_context IS 'AI analysis and context to help staff understand the situation';
COMMENT ON COLUMN handoff_queue.wait_time_seconds IS 'Time customer waited before staff assignment';
CREATE INDEX idx_handoff_queue_priority ON handoff_queue(priority DESC, enqueued_at ASC);
CREATE INDEX idx_handoff_queue_assigned_staff ON handoff_queue(assigned_to_staff_id);
CREATE INDEX idx_handoff_queue_enqueued_at ON handoff_queue(enqueued_at DESC);
CREATE INDEX idx_handoff_queue_reason ON handoff_queue(handoff_reason);

-- Table: staff_presence
CREATE TABLE staff_presence (
    staff_id BIGINT PRIMARY KEY REFERENCES Users(id) ON DELETE CASCADE,
    online BOOLEAN NOT NULL DEFAULT FALSE,
    workload INTEGER NOT NULL DEFAULT 0 CHECK (workload >= 0),
    max_workload INTEGER NOT NULL DEFAULT 5 CHECK (max_workload > 0),
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_activity_at TIMESTAMPTZ,
    status VARCHAR(20) DEFAULT 'AVAILABLE',
    status_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT chk_status CHECK (status IN ('AVAILABLE', 'BUSY', 'AWAY', 'OFFLINE'))
);
COMMENT ON TABLE staff_presence IS 'Real-time presence and workload tracking for staff members';
COMMENT ON COLUMN staff_presence.online IS 'Whether staff is currently online';
COMMENT ON COLUMN staff_presence.workload IS 'Current number of active conversations assigned';
COMMENT ON COLUMN staff_presence.max_workload IS 'Maximum conversations staff can handle simultaneously';
COMMENT ON COLUMN staff_presence.status IS 'Current status: AVAILABLE, BUSY, AWAY, OFFLINE';
CREATE INDEX idx_staff_presence_online ON staff_presence(online);
CREATE INDEX idx_staff_presence_workload ON staff_presence(workload);
CREATE INDEX idx_staff_presence_status ON staff_presence(status);

-- Table: ai_chat_config
CREATE TABLE ai_chat_config (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(100) UNIQUE NOT NULL,
    config_value TEXT NOT NULL,
    config_type VARCHAR(20) NOT NULL DEFAULT 'STRING',
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT chk_config_type CHECK (config_type IN ('STRING', 'NUMBER', 'BOOLEAN', 'JSON', 'TEXT'))
);
COMMENT ON TABLE ai_chat_config IS 'Configuration settings for AI chat system';
COMMENT ON COLUMN ai_chat_config.config_key IS 'Unique configuration key';
COMMENT ON COLUMN ai_chat_config.config_value IS 'Configuration value (stored as text)';
COMMENT ON COLUMN ai_chat_config.config_type IS 'Data type: STRING, NUMBER, BOOLEAN, JSON, TEXT';
CREATE INDEX idx_ai_chat_config_key ON ai_chat_config(config_key);
CREATE INDEX idx_ai_chat_config_active ON ai_chat_config(is_active);

-- =============================================
-- SEQUENCES
-- =============================================

-- Sequence for daily order counter
CREATE SEQUENCE daily_order_counter START 1;
