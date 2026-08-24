-- ============================================================
-- PAWN BROKING SHOP — COMPLETE DATABASE SCHEMA (PostgreSQL)
-- ============================================================
-- Version: 1.0
-- Description: Full schema for pawn broking management system
-- Tables: 16 (auth, customer, pawn-business, audit)
-- ============================================================

-- ============================================================
-- 1. AUTHENTICATION TABLES
-- ============================================================

CREATE TABLE users (
    id          BIGSERIAL       PRIMARY KEY,
    username    VARCHAR(100)    NOT NULL UNIQUE,
    password    VARCHAR(255)    NOT NULL,
    role        VARCHAR(20)     NOT NULL,
    is_active   BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE credentials (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    credential_id   TEXT            NOT NULL UNIQUE,
    public_key      TEXT            NOT NULL,
    sign_count      BIGINT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_credentials_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_credentials_user_id ON credentials(user_id);

-- ============================================================
-- 2. CUSTOMER TABLES
-- ============================================================

CREATE TABLE customers (
    cust_id         BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(200)    NOT NULL,
    date_of_birth   DATE            NOT NULL,
    gender          VARCHAR(10)     NOT NULL,
    marital_status  VARCHAR(20),
    occupation      VARCHAR(100),
    pan_number      VARCHAR(10)     UNIQUE,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    image_url       VARCHAR(500),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);

CREATE TABLE contact_info (
    contact_id      BIGSERIAL       PRIMARY KEY,
    cust_id         BIGINT          NOT NULL,
    phone           VARCHAR(15),
    secondary_phone VARCHAR(15),
    whatsapp_phone  VARCHAR(15),
    email           VARCHAR(255),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),

    CONSTRAINT fk_contact_customer
        FOREIGN KEY (cust_id)
        REFERENCES customers(cust_id)
        ON DELETE CASCADE
);

CREATE TABLE address_info (
    address_id      BIGSERIAL       PRIMARY KEY,
    cust_id         BIGINT          NOT NULL,
    street          VARCHAR(255)    NOT NULL,
    city            VARCHAR(100)    NOT NULL,
    state           VARCHAR(100)    NOT NULL,
    zip_code        VARCHAR(20)     NOT NULL,
    country         VARCHAR(100)    NOT NULL DEFAULT 'India',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),

    CONSTRAINT fk_address_customer
        FOREIGN KEY (cust_id)
        REFERENCES customers(cust_id)
        ON DELETE CASCADE
);

CREATE TABLE id_proof_info (
    id_proof_id     BIGSERIAL       PRIMARY KEY,
    cust_id         BIGINT          NOT NULL,
    id_type         VARCHAR(50)     NOT NULL,
    id_number       VARCHAR(100)    NOT NULL,
    image_url       VARCHAR(500),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),

    CONSTRAINT fk_id_proof_customer
        FOREIGN KEY (cust_id)
        REFERENCES customers(cust_id)
        ON DELETE CASCADE
);

CREATE TABLE relative_info (
    relative_id     BIGSERIAL       PRIMARY KEY,
    cust_id         BIGINT          NOT NULL,
    name            VARCHAR(200)    NOT NULL,
    relationship    VARCHAR(50)     NOT NULL,
    contact_number  VARCHAR(15),
    image_url       VARCHAR(500),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),

    CONSTRAINT fk_relative_customer
        FOREIGN KEY (cust_id)
        REFERENCES customers(cust_id)
        ON DELETE CASCADE
);

CREATE TABLE occupation (
    id              BIGSERIAL       PRIMARY KEY,
    cust_id         BIGINT          NOT NULL,
    type            VARCHAR(100)    NOT NULL,
    income          DECIMAL(15,2),
    proof_amount    DECIMAL(15,2),
    employer        VARCHAR(255),
    appeared_date   DATE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_occupation_customer
        FOREIGN KEY (cust_id)
        REFERENCES customers(cust_id)
        ON DELETE CASCADE
);

-- Customer table indexes
CREATE INDEX idx_contact_cust_id     ON contact_info(cust_id);
CREATE INDEX idx_address_cust_id     ON address_info(cust_id);
CREATE INDEX idx_id_proof_cust_id    ON id_proof_info(cust_id);
CREATE INDEX idx_relative_cust_id    ON relative_info(cust_id);
CREATE INDEX idx_occupation_cust_id  ON occupation(cust_id);
CREATE INDEX idx_customers_status    ON customers(status);

-- ============================================================
-- 3. PAWN BUSINESS TABLES
-- ============================================================

CREATE TABLE accounts (
    id              BIGSERIAL       PRIMARY KEY,
    account_number  VARCHAR(50)     NOT NULL UNIQUE,
    bank_name       VARCHAR(100)    NOT NULL,
    account_type    VARCHAR(10)     NOT NULL CHECK (account_type IN ('CASH', 'BANK')),
    balance         DECIMAL(15,2)   NOT NULL DEFAULT 0.00,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ornaments (
    id                      BIGSERIAL       PRIMARY KEY,
    type                    VARCHAR(50)     NOT NULL UNIQUE,
    description             TEXT,
    image_url               VARCHAR(500),
    default_interest_rate   DECIMAL(5,2)    NOT NULL,
    default_amount_rate     DECIMAL(10,2),
    is_active               BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE items (
    id                  BIGSERIAL       PRIMARY KEY,
    ornament_id         BIGINT          NOT NULL,
    cust_id             BIGINT          NOT NULL,
    description         TEXT,
    image_url           VARCHAR(500),
    weight_gross        DECIMAL(10,3)   NOT NULL,
    weight_net          DECIMAL(10,3)   NOT NULL,
    amount_lended       DECIMAL(15,2)   NOT NULL,
    interest_rate       DECIMAL(5,2)    NOT NULL,
    paid_amount         DECIMAL(15,2)   NOT NULL DEFAULT 0.00,
    compound_interest   DECIMAL(15,2)   NOT NULL DEFAULT 0.00,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE'
                            CHECK (status IN ('ACTIVE', 'REDEEMED', 'DEFAULTED', 'HOLD', 'AUCTIONED')),
    location            VARCHAR(100),
    pledge_date         DATE            NOT NULL DEFAULT CURRENT_DATE,
    due_date            DATE            NOT NULL,
    grace_period_days   INTEGER         NOT NULL DEFAULT 30,
    redeemed_date       DATE,
    defaulted_date      DATE,
    auctioned_date      DATE,
    auction_amount      DECIMAL(15,2),
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_items_ornament
        FOREIGN KEY (ornament_id)
        REFERENCES ornaments(id),

    CONSTRAINT fk_items_customer
        FOREIGN KEY (cust_id)
        REFERENCES customers(cust_id)
);

CREATE TABLE bills (
    id                      BIGSERIAL       PRIMARY KEY,
    bill_id                 VARCHAR(50)     NOT NULL UNIQUE,
    cust_id                 BIGINT          NOT NULL,
    bill_type               VARCHAR(10)     NOT NULL CHECK (bill_type IN ('CREDIT', 'DEBIT')),
    total_amount_lended     DECIMAL(15,2)   NOT NULL DEFAULT 0.00,
    amount_paid             DECIMAL(15,2)   NOT NULL DEFAULT 0.00,
    interest_accumulated    DECIMAL(15,2)   NOT NULL DEFAULT 0.00,
    bill_date               DATE            NOT NULL DEFAULT CURRENT_DATE,
    notes                   TEXT,
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(100),

    CONSTRAINT fk_bills_customer
        FOREIGN KEY (cust_id)
        REFERENCES customers(cust_id)
);

CREATE TABLE bill_items (
    id          BIGSERIAL       PRIMARY KEY,
    bill_id     BIGINT          NOT NULL,
    item_id     BIGINT          NOT NULL,
    action      VARCHAR(20)     NOT NULL CHECK (action IN ('KEPT', 'RELEASED', 'AUCTIONED')),
    amount      DECIMAL(15,2)   NOT NULL,

    CONSTRAINT fk_bill_items_bill
        FOREIGN KEY (bill_id)
        REFERENCES bills(id),

    CONSTRAINT fk_bill_items_item
        FOREIGN KEY (item_id)
        REFERENCES items(id)
);

CREATE TABLE bill_accounts (
    id          BIGSERIAL       PRIMARY KEY,
    bill_id     BIGINT          NOT NULL,
    account_id  BIGINT          NOT NULL,
    amount      DECIMAL(15,2)   NOT NULL,
    direction   VARCHAR(10)     NOT NULL CHECK (direction IN ('IN', 'OUT')),

    CONSTRAINT fk_bill_accounts_bill
        FOREIGN KEY (bill_id)
        REFERENCES bills(id),

    CONSTRAINT fk_bill_accounts_account
        FOREIGN KEY (account_id)
        REFERENCES accounts(id)
);

-- ============================================================
-- 4. AUDIT & TRACKING TABLES
-- ============================================================

CREATE TABLE interest_ledger (
    id                      BIGSERIAL       PRIMARY KEY,
    item_id                 BIGINT          NOT NULL,
    ledger_date             DATE            NOT NULL,
    principal               DECIMAL(15,2)   NOT NULL,
    interest_amount         DECIMAL(15,2)   NOT NULL,
    cumulative_interest     DECIMAL(15,2)   NOT NULL,
    outstanding_balance     DECIMAL(15,2)   NOT NULL,
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_interest_ledger_item
        FOREIGN KEY (item_id)
        REFERENCES items(id)
);

CREATE TABLE transactions (
    id                  BIGSERIAL       PRIMARY KEY,
    account_id          BIGINT          NOT NULL,
    bill_id             BIGINT,
    transaction_type    VARCHAR(10)     NOT NULL CHECK (transaction_type IN ('CREDIT', 'DEBIT')),
    amount              DECIMAL(15,2)   NOT NULL,
    balance_after       DECIMAL(15,2)   NOT NULL,
    transaction_date    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description         TEXT,
    reference_id        VARCHAR(100),
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_transactions_account
        FOREIGN KEY (account_id)
        REFERENCES accounts(id),

    CONSTRAINT fk_transactions_bill
        FOREIGN KEY (bill_id)
        REFERENCES bills(id)
);

-- Business table indexes
CREATE INDEX idx_items_cust_id           ON items(cust_id);
CREATE INDEX idx_items_ornament_id       ON items(ornament_id);
CREATE INDEX idx_items_status            ON items(status);
CREATE INDEX idx_items_due_date          ON items(due_date);
CREATE INDEX idx_items_pledge_date       ON items(pledge_date);
CREATE INDEX idx_bills_cust_id           ON bills(cust_id);
CREATE INDEX idx_bills_bill_date         ON bills(bill_date);
CREATE INDEX idx_bills_bill_type         ON bills(bill_type);
CREATE INDEX idx_bill_items_bill_id      ON bill_items(bill_id);
CREATE INDEX idx_bill_items_item_id      ON bill_items(item_id);
CREATE INDEX idx_bill_accounts_bill_id   ON bill_accounts(bill_id);
CREATE INDEX idx_bill_accounts_acct_id   ON bill_accounts(account_id);
CREATE INDEX idx_interest_ledger_item    ON interest_ledger(item_id);
CREATE INDEX idx_interest_ledger_date    ON interest_ledger(ledger_date);
CREATE INDEX idx_transactions_account    ON transactions(account_id);
CREATE INDEX idx_transactions_bill       ON transactions(bill_id);
CREATE INDEX idx_transactions_date       ON transactions(transaction_date);

-- ============================================================
-- 5. TRIGGERS
-- ============================================================

-- Auto-update 'updated_at' on row modification
CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER trg_customers_updated_at
    BEFORE UPDATE ON customers
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER trg_contact_info_updated_at
    BEFORE UPDATE ON contact_info
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER trg_address_info_updated_at
    BEFORE UPDATE ON address_info
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER trg_id_proof_info_updated_at
    BEFORE UPDATE ON id_proof_info
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER trg_relative_info_updated_at
    BEFORE UPDATE ON relative_info
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER trg_accounts_updated_at
    BEFORE UPDATE ON accounts
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER trg_items_updated_at
    BEFORE UPDATE ON items
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER trg_bills_updated_at
    BEFORE UPDATE ON bills
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

-- Auto-update account balance on new transaction
CREATE OR REPLACE FUNCTION update_account_balance()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.transaction_type = 'CREDIT' THEN
        UPDATE accounts
        SET balance = balance + NEW.amount
        WHERE id = NEW.account_id;
    ELSIF NEW.transaction_type = 'DEBIT' THEN
        UPDATE accounts
        SET balance = balance - NEW.amount
        WHERE id = NEW.account_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_transaction_balance_update
    AFTER INSERT ON transactions
    FOR EACH ROW EXECUTE FUNCTION update_account_balance();

-- ============================================================
-- 6. SEED DATA (Default ornament types)
-- ============================================================

INSERT INTO ornaments (type, description, default_interest_rate, default_amount_rate) VALUES
    ('GOLD',     '24K/22K/18K gold ornaments',       1.50, 4500.00),
    ('SILVER',   'Silver ornaments and articles',     2.00,   75.00),
    ('DIAMOND',  'Diamond-studded jewelry',           1.75, NULL),
    ('PLATINUM', 'Platinum ornaments',                1.50, 3000.00);

-- Default cash account
INSERT INTO accounts (account_number, bank_name, account_type, balance) VALUES
    ('CASH_COUNTER', 'CASH', 'CASH', 0.00);
