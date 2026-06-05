-- ============================================================
-- V3: Update bill_type values from CREDIT/DEBIT to PLEDGE/REDEEM
-- ============================================================

-- 1. Drop the existing CHECK constraint on bill_type
ALTER TABLE bills DROP CONSTRAINT IF EXISTS bills_bill_type_check;

-- 2. Update existing data
UPDATE bills SET bill_type = 'PLEDGE' WHERE bill_type = 'CREDIT';
UPDATE bills SET bill_type = 'REDEEM' WHERE bill_type = 'DEBIT';

-- 3. Add new CHECK constraint with PLEDGE/REDEEM values
ALTER TABLE bills ADD CONSTRAINT bills_bill_type_check
    CHECK (bill_type IN ('PLEDGE', 'REDEEM'));
