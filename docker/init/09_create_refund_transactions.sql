-- Create refund_transactions table
CREATE TABLE IF NOT EXISTS refund_transactions (
    id BIGSERIAL PRIMARY KEY,
    refund_id VARCHAR(255) UNIQUE NOT NULL,
    order_id VARCHAR(255) NOT NULL,
    momo_trans_id VARCHAR(255),
    amount DECIMAL(19,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    reason TEXT,
    momo_response TEXT,
    momo_result_code VARCHAR(50),
    momo_message TEXT,
    processed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT,
    
    CONSTRAINT fk_refund_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_refund_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_refund_transactions_order_id ON refund_transactions(order_id);
CREATE INDEX IF NOT EXISTS idx_refund_transactions_refund_id ON refund_transactions(refund_id);
CREATE INDEX IF NOT EXISTS idx_refund_transactions_status ON refund_transactions(status);
CREATE INDEX IF NOT EXISTS idx_refund_transactions_user_id ON refund_transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_refund_transactions_created_at ON refund_transactions(created_at);

-- Add comments
COMMENT ON TABLE refund_transactions IS 'Bảng lưu trữ thông tin hoàn tiền';
COMMENT ON COLUMN refund_transactions.refund_id IS 'ID duy nhất của giao dịch hoàn tiền';
COMMENT ON COLUMN refund_transactions.order_id IS 'ID đơn hàng được hoàn tiền';
COMMENT ON COLUMN refund_transactions.momo_trans_id IS 'ID giao dịch MoMo gốc';
COMMENT ON COLUMN refund_transactions.amount IS 'Số tiền hoàn';
COMMENT ON COLUMN refund_transactions.status IS 'Trạng thái hoàn tiền: PENDING, PROCESSING, SUCCESS, FAILED, CANCELLED';
COMMENT ON COLUMN refund_transactions.payment_method IS 'Phương thức thanh toán gốc';
COMMENT ON COLUMN refund_transactions.reason IS 'Lý do hoàn tiền';
COMMENT ON COLUMN refund_transactions.momo_response IS 'Response đầy đủ từ MoMo API';
COMMENT ON COLUMN refund_transactions.momo_result_code IS 'Mã kết quả từ MoMo';
COMMENT ON COLUMN refund_transactions.momo_message IS 'Thông báo từ MoMo';
COMMENT ON COLUMN refund_transactions.processed_at IS 'Thời gian xử lý hoàn tiền';
