-- Add RECEIVED status to order_status enum
-- This must be run to support Shopee-like order confirmation flow

BEGIN;

-- Add RECEIVED value to the enum type
ALTER TYPE order_status ADD VALUE IF NOT EXISTS 'RECEIVED';

-- Add received_at column if not exists (should already exist from previous migration)
ALTER TABLE Orders ADD COLUMN IF NOT EXISTS received_at TIMESTAMP;

-- Add index for performance
CREATE INDEX IF NOT EXISTS idx_orders_received_at ON Orders(received_at);

-- Add comment
COMMENT ON COLUMN Orders.received_at IS 'Timestamp when user confirmed order received';

-- Add media_urls column to reviews table for media uploads
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS media_urls TEXT;

-- Add comment for media_urls column
COMMENT ON COLUMN reviews.media_urls IS 'Comma-separated URLs of uploaded media files (images/videos) for the review';

COMMIT;
