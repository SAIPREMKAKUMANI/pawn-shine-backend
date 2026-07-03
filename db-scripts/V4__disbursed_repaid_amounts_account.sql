-- ============================================================
-- V4: Adding new columns to the accounts table for disbursed and repaid amounts
-- ============================================================

ALTER TABLE accounts
ADD COLUMN disbursed_amount NUMERIC(15, 2) DEFAULT 0,
ADD COLUMN repaid_amount NUMERIC(15, 2) DEFAULT 0;