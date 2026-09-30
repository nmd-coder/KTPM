-- Migration: Add RECEIVED status and received_at column
-- Purpose: Implement Shopee-like order confirmation flow

-- Add received_at column to Orders table
ALTER TABLE Orders 
ADD COLUMN IF NOT EXISTS received_at TIMESTAMP;

-- Add comment for documentation
COMMENT ON COLUMN Orders.received_at IS 'Timestamp when user confirmed order received';

-- Create index for performance
CREATE INDEX IF NOT EXISTS idx_orders_received_at ON Orders(received_at);

-- Update existing COMPLETED orders to have received_at = updated_at (optional migration)
-- This is for backward compatibility with existing data
-- Comment out if you don't want to auto-migrate existing orders
-- UPDATE Orders 
-- SET received_at = updated_at 
-- WHERE status = 'COMPLETED' AND received_at IS NULL;
