-- ============================================================
-- V2: WALLET DEPOSIT ALLOCATION TRACKING
-- ============================================================
-- Adds FIFO/LIFO tracking of which deposits are consumed
-- during wallet withdrawals (redemptions).
-- ============================================================

-- 1. Add remaining_balance to wallet_transactions
--    Tracks how much of each DEPOSIT is still available for use.
ALTER TABLE wallet_transactions
ADD COLUMN remaining_balance DECIMAL(15,2);

-- Backfill: set remaining_balance = amount for all existing DEPOSIT transactions
UPDATE wallet_transactions
SET remaining_balance = amount
WHERE type = 'DEPOSIT';

-- 2. Allocation ledger: links a withdrawal to the specific deposits it consumed
CREATE TABLE wallet_deposit_allocations (
    id                  BIGSERIAL       PRIMARY KEY,
    withdrawal_tx_id    BIGINT          NOT NULL,
    deposit_tx_id       BIGINT          NOT NULL,
    amount_used         DECIMAL(15,2)   NOT NULL,
    allocation_type     VARCHAR(20)     NOT NULL CHECK (allocation_type IN ('PRINCIPAL', 'INTEREST')),
    bill_id             BIGINT,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_alloc_withdrawal
        FOREIGN KEY (withdrawal_tx_id) REFERENCES wallet_transactions(id),
    CONSTRAINT fk_alloc_deposit
        FOREIGN KEY (deposit_tx_id) REFERENCES wallet_transactions(id),
    CONSTRAINT fk_alloc_bill
        FOREIGN KEY (bill_id) REFERENCES bills(id)
);

CREATE INDEX idx_alloc_withdrawal ON wallet_deposit_allocations(withdrawal_tx_id);
CREATE INDEX idx_alloc_deposit ON wallet_deposit_allocations(deposit_tx_id);
CREATE INDEX idx_alloc_bill ON wallet_deposit_allocations(bill_id);
