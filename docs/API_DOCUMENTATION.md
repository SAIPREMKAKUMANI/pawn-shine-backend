# Pawn Broking System — Complete Documentation

> **Purpose**: This document serves as the single source of truth for building the UI. It covers every API endpoint, request/response schema, database table, real-world use case, user flow, and edge case.

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [User Roles & Personas](#2-user-roles--personas)
3. [Database Schema Reference](#3-database-schema-reference)
4. [Enum Reference](#4-enum-reference)
5. [Module 1: Authentication](#5-module-1-authentication)
6. [Module 2: Customer Onboarding](#6-module-2-customer-onboarding)
7. [Module 3: Accounts](#7-module-3-accounts)
8. [Module 4: Pledge (Ornaments & Items)](#8-module-4-pledge-ornaments--items)
9. [Module 5: Billing (Bills, Payments, Interest)](#9-module-5-billing-bills-payments-interest)
10. [Complete User Flows (Real-World Scenarios)](#10-complete-user-flows-real-world-scenarios)
11. [Edge Cases & Error Handling](#11-edge-cases--error-handling)
12. [UI Pages Mapping](#12-ui-pages-mapping)

---

## 1. System Overview

This is a **Pawn Broking Management System** — a business application for a pawn shop owner (your father) who lends money to customers by keeping their valuable items (gold, silver, diamond ornaments) as collateral.

### Core Business Flow
```
Customer brings items → Owner appraises items → Owner lends money →
Customer gets cash, Owner keeps items → Customer pays back later →
Owner returns items → If customer doesn't pay, items are defaulted/auctioned
```

### Tech Stack
- **Backend**: Spring Boot 3.4.9, Java 17, PostgreSQL, JPA/Hibernate
- **Security**: JWT + WebAuthn (biometric)
- **Caching**: Caffeine (in-memory)
- **Base URL**: `http://localhost:8080`
- **Auth**: All endpoints (except `/api/auth/**`) require `Authorization: Bearer <JWT>` header

---

## 2. User Roles & Personas

### Owner (Primary User — Your Father)
The shop owner who:
- Logs in every morning to the system
- Registers new customers who walk in
- Appraises ornaments and decides how much to lend
- Creates pledge bills (lending money out)
- Sets interest amounts manually (his own formulation)
- Creates redemption bills when customers pay back
- Tracks which items are overdue
- Manages his bank and cash accounts
- Reviews transaction history for accounting
- Auctions defaulted items to recover money

### Staff (Secondary User — if applicable)
A shop employee who:
- Can log in with their own credentials
- Handles customer onboarding
- Creates bills under the owner's supervision
- Cannot modify accounts or interest rates

> **Note**: The current system does not enforce role-based access control beyond authentication. All logged-in users have full access. Role-based restrictions can be added in the UI by checking `UserDetails.role`.

---

## 3. Database Schema Reference

### 3.1 Table Relationships

```
┌──────────┐     ┌──────────────┐     ┌─────────────┐
│  users   │────>│  credentials │     │   accounts  │
└──────────┘     └──────────────┘     └──────┬──────┘
                                              │
┌──────────────┐                    ┌─────────┴──────────┐
│  customers   │<───────────────────│   transactions     │
├──────────────┤                    └─────────┬──────────┘
│ contact_info │                              │
│ address_info │     ┌───────────┐   ┌────────┴───────┐
│ id_proof_info│     │ ornaments │   │     bills      │
│ relative_info│     └─────┬─────┘   ├────────────────┤
│ occupation   │           │         │  bill_items    │
└──────┬───────┘           │         │  bill_accounts │
       │              ┌────┴────┐    └────────────────┘
       └─────────────>│  items  │
                      └────┬────┘
                           │
                  ┌────────┴──────────┐
                  │  interest_ledger  │
                  └───────────────────┘
```

### 3.2 Table Summary

| Table | PK | Purpose | Parent FK |
|-------|----|---------|-----------|
| `users` | id | System users (owner, staff) | — |
| `credentials` | id | WebAuthn biometric credentials | users.id |
| `customers` | cust_id | Pawn shop customers | — |
| `contact_info` | contact_id | Customer phone/email | customers.cust_id |
| `address_info` | address_id | Customer addresses | customers.cust_id |
| `id_proof_info` | id_proof_id | Aadhar, PAN, etc. | customers.cust_id |
| `relative_info` | relative_id | Customer's relatives (guarantors) | customers.cust_id |
| `occupation` | id | Customer's job details | customers.cust_id |
| `accounts` | id | Owner's bank/cash accounts | — |
| `ornaments` | id | Catalog of item types | — |
| `items` | id | Pledged collateral items | ornaments.id, customers.cust_id |
| `bills` | id | Pledge/redemption bills | customers.cust_id |
| `bill_items` | id | Items in a bill | bills.id, items.id |
| `bill_accounts` | id | Accounts used in a bill | bills.id, accounts.id |
| `interest_ledger` | id | Interest history per item | items.id |
| `transactions` | id | Account money movement audit | accounts.id, bills.id |

---

## 4. Enum Reference

| Enum | Values | Used In |
|------|--------|---------|
| `Gender` | `MALE`, `FEMALE`, `OTHER` | CustomerDto |
| `MaritalStatus` | `SINGLE`, `MARRIED`, `DIVORCED`, `WIDOWED`, `SEPARATED` | CustomerDto |
| `MediaType` | `PROFILE`, `ID_PROOF`, `RELATIVE`, `AADHAR`, `PAN`, `PASSPORT`, `VOTER_ID`, `DRIVING_LICENSE` | IdProofDto |
| `AccountType` | `CASH`, `BANK` | AccountDto |
| `TransactionType` | `CREDIT`, `DEBIT` | TransactionDto |
| `ItemStatus` | `ACTIVE`, `REDEEMED`, `DEFAULTED`, `HOLD`, `AUCTIONED` | ItemDto |
| `BillType` | `CREDIT` (lending out), `DEBIT` (receiving back) | BillDto |
| `BillItemAction` | `KEPT` (item kept as collateral), `RELEASED` (returned to customer), `AUCTIONED` | BillItemDto |
| `PaymentDirection` | `IN` (money coming in), `OUT` (money going out) | BillAccountDto |

---

## 5. Module 1: Authentication

### 5.1 Login (Username + Password)

**`POST /api/auth/login`**

| Field | Description |
|-------|-------------|
| **When** | Owner opens the app for the first time each day |
| **Who** | Owner or staff |
| **What happens** | Validates credentials, returns JWT token |

**Request:**
```json
{
  "username": "admin",
  "password": "secret123"
}
```

**Success Response (200):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Failure Response (401):**
```json
{
  "token": null
}
```

**UI Notes:**
- Store the token in `localStorage` or a secure cookie
- Attach as `Authorization: Bearer <token>` to all subsequent requests
- Token expires after 10 hours (`jwt.expiration=36000000ms`)
- On 401, redirect to login page

---

### 5.2 WebAuthn (Biometric Login)

**Register Options — `POST /api/auth/webauthn/register/options`**
```json
{ "username": "admin" }
```
Header: `Authorization: Bearer <jwt>` (must be logged in first)

Returns: WebAuthn challenge options for the browser's `navigator.credentials.create()`.

---

**Register Verify — `POST /api/auth/webauthn/register/verify`**
```json
{
  "credential": { /* browser credential response */ },
  "username": "admin"
}
```
Header: `Authorization: Bearer <jwt>`

Returns: `{ "status": "ok" }` on success. Stores the biometric credential.

---

**Login Options — `POST /api/auth/webauthn/login/options`**
```json
{ "username": "admin" }
```
Returns: Challenge for `navigator.credentials.get()`.

---

**Login Verify — `POST /api/auth/webauthn/login/verify`**
```json
{
  "credential": { /* browser assertion response */ }
}
```
Returns: `{ "token": "eyJ..." }` — same JWT as password login.

**UI Flow:**
1. First time: Owner logs in with password → registers biometric
2. Next time: Owner just scans fingerprint → gets JWT directly

---

## 6. Module 2: Customer Onboarding

### 6.1 Create Customer

**`POST /api/customer/onboard`**
`Content-Type: application/json`

**When**: A new customer walks into the shop for the first time.

**Request:**
```json
{
  "name": "Ramesh Kumar",
  "date_of_birth": "1985-03-15",
  "gender": "MALE",
  "marital_status": "MARRIED",
  "occupation": "Farmer",
  "contacts": [
    {
      "phone": "9876543210",
      "secondary_phone": "9876543211",
      "whatsapp_phone": "9876543210",
      "email": "ramesh@example.com"
    }
  ],
  "addresses": [
    {
      "street": "123 Main Street",
      "city": "Hyderabad",
      "state": "Telangana",
      "postal_code": "500001",
      "country": "India"
    }
  ],
  "id_proofs": [
    {
      "id_type": "AADHAR",
      "id_number": "1234-5678-9012"
    }
  ],
  "relatives": [
    {
      "name": "Suresh Kumar",
      "relationship": "Brother",
      "contact_number": "9876543299"
    }
  ]
}
```

**Response (201):**
```json
{
  "customer_id": 1,
  "status": "SUCCESS",
  "message": "Customer onboarded successfully"
}
```

**Why relatives?** Pawn shops need a guarantor — if the customer disappears after pledging, the shop contacts the relative.

---

### 6.2 Update Customer

**`PUT /api/customer/update/{id}`**
`Content-Type: multipart/form-data`

**When**: Customer comes back, phone number changed, new address, or owner wants to add a photo.

**Form Fields**: Same as create + `image` (MultipartFile for profile photo).

**Response (200):**
```json
{
  "customer_id": 1,
  "status": "SUCCESS",
  "message": "Customer updated successfully"
}
```

---

### 6.3 Get All Customers (List View)

**`GET /api/customer/get`**

**When**: Owner opens the customers page, wants to see all registered customers.

**Response (200):**
```json
{
  "customers": [
    {
      "cust_id": 1,
      "name": "Ramesh Kumar",
      "date_of_birth": "1985-03-15",
      "gender": "MALE",
      "status": "ACTIVE",
      "image_url": "/projects/pawn-images/1/PROFILE.jpeg"
    },
    {
      "cust_id": 2,
      "name": "Lakshmi Devi",
      "date_of_birth": "1990-07-22",
      "gender": "FEMALE",
      "status": "ACTIVE",
      "image_url": null
    }
  ]
}
```

**Note**: This returns the **base view** (minimal fields for list/card display). Uses `@JsonView(View.CustomerBase)` — no contacts/addresses/relatives.

---

### 6.4 Get Customer Detail

**`GET /api/customer/{id}`**

**When**: Owner clicks on a customer in the list to see full profile.

**Response (200):** Returns the full `CustomerDto` with all nested contacts, addresses, id_proofs, and relatives.

---

### 6.5 Get Customer Count

**`GET /api/customer/count`**

**When**: Dashboard widget showing total registered customers.

**Response (200):** `42` (plain Long)

---

## 7. Module 3: Accounts

### 7.1 Create Account

**`POST /api/accounts`**

**When**: Owner wants to register a new bank account (e.g., just opened an SBI account for the shop).

**Request:**
```json
{
  "account_number": "SBI123456789",
  "bank_name": "State Bank of India",
  "account_type": "BANK"
}
```

**Response (201):**
```json
{
  "id": 2,
  "account_number": "SBI123456789",
  "bank_name": "State Bank of India",
  "account_type": "BANK",
  "balance": 0.00,
  "is_active": true,
  "created_at": "2026-04-24T10:30:00",
  "updated_at": "2026-04-24T10:30:00"
}
```

**Seed Data**: A `CASH_COUNTER` account (type=CASH) is pre-created by the schema.

---

### 7.2 Get All Accounts

**`GET /api/accounts`** — Active accounts only (default)
**`GET /api/accounts?active_only=false`** — All accounts including deactivated

**When**: Owner opens the accounts page or needs to select an account during bill creation.

**Response (200):**
```json
[
  {
    "id": 1,
    "account_number": "CASH_COUNTER",
    "bank_name": "CASH",
    "account_type": "CASH",
    "balance": 150000.00,
    "is_active": true
  },
  {
    "id": 2,
    "account_number": "SBI123456789",
    "bank_name": "State Bank of India",
    "account_type": "BANK",
    "balance": 500000.00,
    "is_active": true
  }
]
```

---

### 7.3 Get Single Account

**`GET /api/accounts/{id}`**

---

### 7.4 Update Account

**`PUT /api/accounts/{id}`**

**When**: Owner wants to rename or change account type.

**Request:**
```json
{
  "bank_name": "SBI Savings"
}
```

Only non-null fields are updated. `account_number` cannot be changed (unique key). `balance` is never set directly — only changed through transactions.

---

### 7.5 Deactivate Account

**`DELETE /api/accounts/{id}`**

**When**: Owner closes a bank account. Soft-delete — sets `is_active=false`. Account and its transaction history remain in the system.

**Response**: `204 No Content`

---

### 7.6 Get Transactions by Account (Paginated)

**`GET /api/accounts/{accountId}/transactions?page=0&size=20`**

**When**: Owner clicks on an account to see its ledger/passbook.

**Response (200):**
```json
{
  "content": [
    {
      "id": 1,
      "account_id": 1,
      "account_number": "CASH_COUNTER",
      "bill_id": 5,
      "transaction_type": "DEBIT",
      "amount": 50000.00,
      "balance_after": 100000.00,
      "transaction_date": "2026-04-24T10:30:00",
      "description": "Pledge bill BILL-20260424-103000-0001 - money lent",
      "reference_id": "BILL-20260424-103000-0001"
    }
  ],
  "totalElements": 45,
  "totalPages": 3,
  "number": 0,
  "size": 20
}
```

---

### 7.7 Get Transactions by Date Range

**`GET /api/accounts/{accountId}/transactions/range?from=2026-04-01T00:00:00&to=2026-04-30T23:59:59`**

**When**: Owner wants monthly/weekly account statement.

---

### 7.8 Get All Transactions (Global Audit)

**`GET /api/accounts/transactions?page=0&size=20`**

**When**: Accountant needs to see all money movement across all accounts.

---

## 8. Module 4: Pledge (Ornaments & Items)

### 8.1 Create Ornament Type

**`POST /api/ornaments`**

**When**: Owner wants to add a new jewelry type to the catalog (e.g., "RUBY").

**Request:**
```json
{
  "type": "RUBY",
  "description": "Ruby-studded gold ornaments",
  "default_interest_rate": 2.00,
  "default_amount_rate": 3500.00
}
```

- `default_interest_rate`: The rate shown in the UI by default when the owner creates a pledge for this ornament type. Owner can override per-item.
- `default_amount_rate`: Suggested rate per gram for this ornament type. Helps the owner calculate the lending amount.

**Response (201):** Returns the saved `OrnamentDto`.

**Seed Data**: GOLD (₹4500/g, 1.5%), SILVER (₹75/g, 2%), DIAMOND (1.75%), PLATINUM (₹3000/g, 1.5%)

---

### 8.2 Get All Ornaments

**`GET /api/ornaments`** — Active only (default)
**`GET /api/ornaments?active_only=false`** — All including deactivated

**When**: During pledge bill creation, the UI shows a dropdown of ornament types.

---

### 8.3 Update Ornament

**`PUT /api/ornaments/{id}`**

**When**: Gold rate changes, owner updates the default_amount_rate.

---

### 8.4 Get Item Detail

**`GET /api/items/{id}`**

**When**: Owner clicks on a specific pledged item to see full details.

**Response (200):**
```json
{
  "id": 1,
  "ornament_id": 1,
  "ornament_type": "GOLD",
  "cust_id": 1,
  "customer_name": "Ramesh Kumar",
  "description": "22K gold chain, 3 strands",
  "image_url": null,
  "weight_gross": 25.500,
  "weight_net": 24.200,
  "amount_lended": 108900.00,
  "interest_rate": 1.50,
  "paid_amount": 0.00,
  "compound_interest": 5000.00,
  "outstanding_balance": 113900.00,
  "status": "ACTIVE",
  "location": "Locker A3",
  "pledge_date": "2026-01-15",
  "due_date": "2026-07-15",
  "grace_period_days": 30,
  "redeemed_date": null,
  "defaulted_date": null,
  "auctioned_date": null,
  "auction_amount": null
}
```

**Key calculated field**: `outstanding_balance` = `amount_lended` + `compound_interest` - `paid_amount`

---

### 8.5 Get Items by Customer

**`GET /api/items/customer/{custId}`** — All items (all statuses)
**`GET /api/items/customer/{custId}/active`** — Only active pledges

**When**: Owner opens a customer's profile and wants to see their pledged items.

---

### 8.6 Get Items by Status (Paginated)

**`GET /api/items/status/ACTIVE?page=0&size=20`**
**`GET /api/items/status/DEFAULTED?page=0&size=20`**

**When**: Owner wants to see all active pledges, or all defaulted items ready for auction.

---

### 8.7 Dashboard Stats

**`GET /api/items/dashboard`**

**When**: Main dashboard page on app load.

**Response (200):**
```json
{
  "total_outstanding": 2500000.00,
  "active_count": 47,
  "defaulted_count": 3,
  "hold_count": 5,
  "total_lended_active": 2200000.00
}
```

**UI Usage**:
- "Total Outstanding" → big number card
- "Active Items" → count badge
- "Defaulted" → red warning badge
- "On Hold" → yellow warning badge

---

## 9. Module 5: Billing (Bills, Payments, Interest)

### 9.1 Create Pledge Bill (Owner Lends Money)

**`POST /api/bills/pledge`**

**When**: Customer brings ornaments, owner appraises them, decides amount, hands cash.

**This is the most important API** — it does everything in one atomic transaction:
1. Creates item records (collateral)
2. Creates the bill
3. Records money-out transactions on accounts
4. Updates account balances

**Request:**
```json
{
  "cust_id": 1,
  "notes": "Customer needs money for daughter's wedding",
  "bill_date": "2026-04-24",
  "items": [
    {
      "ornament_id": 1,
      "description": "22K gold chain, 3 strands, with pendant",
      "weight_gross": 25.500,
      "weight_net": 24.200,
      "amount": 108900.00,
      "interest_rate": 1.50,
      "location": "Locker A3",
      "due_date": "2026-10-24",
      "grace_period_days": 30
    },
    {
      "ornament_id": 1,
      "description": "22K gold bangles set of 4",
      "weight_gross": 40.000,
      "weight_net": 38.500,
      "amount": 173250.00,
      "interest_rate": 1.50,
      "location": "Locker A3",
      "due_date": "2026-10-24",
      "grace_period_days": 30
    }
  ],
  "accounts": [
    {
      "account_id": 1,
      "amount": 200000.00
    },
    {
      "account_id": 2,
      "amount": 82150.00
    }
  ]
}
```

**Explanation**:
- **items**: Each ornament the customer is pledging. `amount` is how much the owner is lending for that item. `interest_rate` is the agreed rate (can differ from ornament default). `location` is where the item is stored physically (locker number).
- **accounts**: How the money is split across accounts. Here ₹2,00,000 from cash counter + ₹82,150 from SBI account. **Total of accounts must equal total of items amounts** (₹2,82,150).
- **due_date**: When the customer must pay back. After this date, owner can move item to HOLD status.
- **grace_period_days**: Extra days after due_date before item is considered DEFAULTED.

**Response (201):**
```json
{
  "id": 1,
  "bill_id": "BILL-20260424-103000-0001",
  "cust_id": 1,
  "customer_name": "Ramesh Kumar",
  "bill_type": "CREDIT",
  "total_amount_lended": 282150.00,
  "amount_paid": 0.00,
  "interest_accumulated": 0.00,
  "bill_date": "2026-04-24",
  "notes": "Customer needs money for daughter's wedding",
  "created_by": "admin",
  "created_at": "2026-04-24T10:30:00",
  "items": [
    { "item_id": 1, "action": "KEPT", "amount": 108900.00 },
    { "item_id": 2, "action": "KEPT", "amount": 173250.00 }
  ],
  "accounts": [
    { "account_id": 1, "amount": 200000.00, "direction": "OUT" },
    { "account_id": 2, "amount": 82150.00, "direction": "OUT" }
  ]
}
```

---

### 9.2 Record Interest (Owner Sets Manually)

**`POST /api/bills/interest/record`**

**When**: Owner calculates interest based on his own formula and records it. This could be monthly, quarterly, or whenever the customer visits.

> **Important**: Interest is NOT auto-calculated by the system. The owner decides the exact amount based on his personal formulation strategy (could be simple interest, compound interest, flat fee, etc.).

**Request:**
```json
{
  "item_id": 1,
  "interest_amount": 5000.00
}
```

This **adds** ₹5,000 to the existing compound_interest on item 1.

**Response (201):**
```json
{
  "id": 1,
  "item_id": 1,
  "ledger_date": "2026-05-24",
  "principal": 108900.00,
  "interest_amount": 5000.00,
  "cumulative_interest": 5000.00,
  "outstanding_balance": 113900.00
}
```

---

### 9.3 Override Total Interest

**`PUT /api/bills/interest/set`**

**When**: Owner made a mistake in the last interest entry and wants to correct it.

**Request:**
```json
{
  "item_id": 1,
  "interest_amount": 4500.00
}
```

This **replaces** the cumulative interest with ₹4,500 (not additive).

---

### 9.4 Get Interest History

**`GET /api/bills/interest/{itemId}`**

**When**: Owner or customer wants to see how interest has accumulated over time.

**Response (200):**
```json
[
  {
    "id": 3,
    "item_id": 1,
    "ledger_date": "2026-07-24",
    "principal": 108900.00,
    "interest_amount": 5000.00,
    "cumulative_interest": 15000.00,
    "outstanding_balance": 123900.00
  },
  {
    "id": 2,
    "item_id": 1,
    "ledger_date": "2026-06-24",
    "principal": 108900.00,
    "interest_amount": 5000.00,
    "cumulative_interest": 10000.00,
    "outstanding_balance": 118900.00
  },
  {
    "id": 1,
    "item_id": 1,
    "ledger_date": "2026-05-24",
    "principal": 108900.00,
    "interest_amount": 5000.00,
    "cumulative_interest": 5000.00,
    "outstanding_balance": 113900.00
  }
]
```

---

### 9.5 Get Interest by Date Range

**`GET /api/bills/interest/{itemId}/range?from=2026-05-01&to=2026-07-31`**

---

### 9.6 Get Current Interest State

**`GET /api/bills/interest/{itemId}/current`**

**When**: Quick-check widget on item detail page.

Returns the latest `InterestLedgerDto` or `204 No Content` if no interest has been recorded yet.

---

### 9.7 Create Redemption Bill (Customer Pays Back)

**`POST /api/bills/redeem`**

**When**: Customer comes back to pick up their items. They pay the outstanding balance (principal + interest - any partial payments already made).

**Request:**
```json
{
  "cust_id": 1,
  "item_ids": [1, 2],
  "notes": "Full redemption, customer paid in cash",
  "bill_date": "2026-07-24",
  "accounts": [
    {
      "account_id": 1,
      "amount": 296150.00
    }
  ]
}
```

**What happens internally:**
1. Reads the owner-set `compound_interest` from each item
2. Computes `outstanding_balance` = principal + interest - paid
3. Marks items as `REDEEMED`, sets `redeemed_date`
4. Creates DEBIT bill showing what was paid
5. Records CREDIT transactions on the accounts (money coming IN)

**Response (201):**
```json
{
  "id": 2,
  "bill_id": "BILL-20260724-143000-0002",
  "cust_id": 1,
  "customer_name": "Ramesh Kumar",
  "bill_type": "DEBIT",
  "total_amount_lended": 282150.00,
  "amount_paid": 296150.00,
  "interest_accumulated": 14000.00,
  "bill_date": "2026-07-24",
  "items": [
    { "item_id": 1, "action": "RELEASED", "amount": 118900.00 },
    { "item_id": 2, "action": "RELEASED", "amount": 177250.00 }
  ],
  "accounts": [
    { "account_id": 1, "amount": 296150.00, "direction": "IN" }
  ]
}
```

---

### 9.8 Bill Query APIs

| Endpoint | When |
|----------|------|
| `GET /api/bills/{id}` | View specific bill by DB ID |
| `GET /api/bills/ref/{billId}` | View by human-readable bill ID (e.g., `BILL-20260424-...`) |
| `GET /api/bills/customer/{custId}?page=0&size=20` | All bills for a customer (bill history) |
| `GET /api/bills/type/CREDIT?page=0&size=20` | All pledge bills |
| `GET /api/bills/type/DEBIT?page=0&size=20` | All redemption bills |
| `GET /api/bills?page=0&size=20` | All bills (master ledger) |

---

## 10. Complete User Flows (Real-World Scenarios)

### Flow 1: Morning Opening

```
Owner arrives at shop
  ├─ Opens app → Login page
  ├─ Scans fingerprint (WebAuthn) OR enters password
  ├─ Lands on Dashboard
  │   ├─ Total Outstanding: ₹25,00,000
  │   ├─ Active Items: 47
  │   ├─ Defaulted: 3 (RED badge → needs attention)
  │   ├─ On Hold: 5 (YELLOW badge → follow up soon)
  │   └─ Account Balances: Cash ₹1,50,000 | SBI ₹5,00,000
  └─ Checks defaulted items → decides which to auction
```

### Flow 2: New Customer Walks In

```
New person arrives, wants to pledge gold necklace
  ├─ Owner opens Customers → "Add New"
  ├─ Fills: Name, DOB, Gender, Phone, Address
  ├─ Takes Aadhar photo → uploads as ID proof
  ├─ Takes relative details (brother/father)
  ├─ POST /api/customer/onboard → Customer created (cust_id: 15)
  └─ Now proceeds to create pledge bill
```

### Flow 3: Creating a Pledge (Lending Money)

```
Customer (existing, cust_id: 15) brings 22K gold chain
  ├─ Owner weighs on scale: Gross 25.5g, Net 24.2g
  ├─ Owner checks today's gold rate, decides to lend ₹1,08,900
  ├─ Owner sets interest rate: 1.5% per month
  ├─ Owner puts item in Locker A3
  ├─ Owner sets due date: 6 months from today
  ├─ Owner decides: Give ₹1,00,000 cash + ₹8,900 to SBI GPay
  ├─ POST /api/bills/pledge
  │   ├─ Item created (status: ACTIVE)
  │   ├─ Bill created (BILL-20260424-...)
  │   ├─ CASH_COUNTER debited ₹1,00,000
  │   └─ SBI account debited ₹8,900
  ├─ Owner prints bill receipt for customer
  └─ Customer leaves with ₹1,08,900, item stays in locker
```

### Flow 4: Monthly Interest Recording

```
End of month, owner calculates interest
  ├─ Opens Items → filter by ACTIVE
  ├─ For each item, owner calculates interest:
  │   (using his own formula — not the system's)
  │   e.g., ₹1,08,900 × 1.5% = ₹1,633.50
  ├─ POST /api/bills/interest/record
  │   { "item_id": 1, "interest_amount": 1633.50 }
  ├─ Item's compound_interest updated: 0 → 1633.50
  ├─ Item's outstanding_balance: ₹1,10,533.50
  └─ Repeat for all active items
```

### Flow 5: Customer Paying Partial Interest

```
Customer visits, pays ₹5,000 toward interest
  ├─ This is handled as a partial payment bill
  ├─ Owner creates a DEBIT bill but does NOT redeem items:
  │   The item's paid_amount increases by ₹5,000
  │   This reduces outstanding_balance
  └─ Transaction recorded on CASH_COUNTER
```

> **Note**: The current API redeems items fully. For partial payments, we'll need a `POST /api/bills/partial-payment` endpoint in the next iteration. For now, track partial payments via the interest ledger or manual notes.

### Flow 6: Customer Redeems (Pays Back Fully)

```
Customer comes back after 6 months, wants to take items back
  ├─ Owner opens customer profile
  ├─ Sees active items: Gold chain (outstanding: ₹1,18,900)
  ├─ Customer pays ₹1,18,900 in cash
  ├─ POST /api/bills/redeem
  │   { "cust_id": 15, "item_ids": [1], "accounts": [{ "account_id": 1, "amount": 118900 }] }
  │   ├─ Item status: ACTIVE → REDEEMED
  │   ├─ CASH_COUNTER credited ₹1,18,900
  │   └─ Bill created showing principal + interest breakdown
  ├─ Owner retrieves items from Locker A3
  ├─ Owner prints receipt, hands items back
  └─ Customer leaves with their jewelry
```

### Flow 7: Item Becomes Overdue (Grace Period)

```
Due date passes, customer hasn't returned
  ├─ Dashboard shows item under "On Hold" (due_date < today)
  ├─ Owner calls customer's phone number
  ├─ If customer answers: "Please come pay within 30 days"
  ├─ If no response: Owner calls relative contact
  ├─ Owner manually changes item status to HOLD
  │   (this is tracked but item isn't defaulted yet)
  └─ Owner continues recording interest
```

### Flow 8: Item Defaults

```
Grace period (30 days after due date) also passes
  ├─ Owner marks item as DEFAULTED
  ├─ Dashboard "Defaulted" count increases
  ├─ Owner can now proceed to auction
  └─ Interest may or may not continue (owner's choice)
```

### Flow 9: Auctioning a Defaulted Item

```
Owner decides to auction a defaulted gold chain
  ├─ Finds a buyer, sells for ₹1,20,000
  ├─ Item status: DEFAULTED → AUCTIONED
  ├─ auction_amount: ₹1,20,000, auctioned_date: today
  ├─ Owner records incoming money via account transaction
  └─ If auction_amount > outstanding, owner profits
      If auction_amount < outstanding, owner takes a loss
```

### Flow 10: Split Payment Across Accounts

```
Customer redeems, pays ₹50,000 cash + ₹68,900 via GPay to SBI
  ├─ POST /api/bills/redeem
  │   accounts: [
  │     { "account_id": 1, "amount": 50000 },
  │     { "account_id": 2, "amount": 68900 }
  │   ]
  ├─ CASH_COUNTER credited ₹50,000
  └─ SBI credited ₹68,900
```

### Flow 11: End-of-Day Reconciliation

```
Owner closes shop, needs to verify cash
  ├─ GET /api/accounts/1 → CASH_COUNTER balance
  ├─ GET /api/accounts/1/transactions → today's transactions
  ├─ Counts physical cash in drawer
  ├─ Compares with system balance
  └─ If mismatch → reviews transaction log to find discrepancy
```

### Flow 12: Monthly Report / Audit

```
End of month, owner or accountant reviews
  ├─ GET /api/accounts/transactions → all account activity
  ├─ GET /api/bills/type/CREDIT → all pledge bills this month
  ├─ GET /api/bills/type/DEBIT → all redemption bills this month
  ├─ GET /api/items/dashboard → current state summary
  └─ Export data for tax filing
```

---

## 11. Edge Cases & Error Handling

### Authentication Edge Cases

| Scenario | Behavior | HTTP | UI Action |
|----------|----------|------|-----------|
| Wrong password | Returns null token | 401 | Show "Invalid credentials" |
| Expired JWT token | Filter rejects request | 401 | Redirect to login |
| Malformed JWT | Filter logs and rejects | 401 | Redirect to login |
| WebAuthn on unsupported browser | Browser API fails | — | Show "Use password login" |
| User tries WebAuthn before registering | No credential found | 400 | Show "Register biometric first" |

### Customer Edge Cases

| Scenario | Behavior | HTTP | UI Action |
|----------|----------|------|-----------|
| Duplicate Aadhar number | Validation fails | 400 | Show "ID proof already registered" |
| Missing required fields (name, DOB) | Validation fails | 400 | Highlight missing fields |
| Customer not found by ID | Exception thrown | 400 | Show "Customer not found" |
| Upload very large image (>10MB) | Spring rejects | 413 | Show "Image too large" |
| Customer has active pledges, try to delete | Not supported | — | Don't show delete button for customers with active items |

### Account Edge Cases

| Scenario | Behavior | HTTP | UI Action |
|----------|----------|------|-----------|
| Duplicate account number | Validation fails | 400 | Show "Account number exists" |
| Deactivate account with balance > 0 | Allows deactivation, balance preserved | 204 | Warn "Account has ₹X balance" |
| Try to use deactivated account in bill | Transaction still works (no check) | 200 | Filter out inactive accounts from dropdown |
| Account balance goes negative (over-lending) | System allows it | — | Show warning in UI if balance < 0 |

### Pledge/Item Edge Cases

| Scenario | Behavior | HTTP | UI Action |
|----------|----------|------|-----------|
| Ornament type doesn't exist | Exception | 400 | Dropdown only shows valid types |
| Customer doesn't exist | Exception | 400 | Customer must be selected from existing |
| Zero weight | System allows | — | UI should validate > 0 |
| Zero amount | System allows | — | UI should validate > 0 |
| Due date in the past | System allows | — | UI should warn "Date is in the past" |
| Same item pledged twice | Not possible — items are created fresh per bill | — | — |
| Redeem already-redeemed item | Exception: wrong status | 400 | Don't show redeemed items in redeem flow |
| Redeem defaulted item directly | Exception: wrong status (must be ACTIVE or HOLD) | 400 | Show "Item is defaulted, auction instead" |
| Auction non-defaulted item | Exception: wrong status | 400 | Only show auction button for DEFAULTED items |

### Billing Edge Cases

| Scenario | Behavior | HTTP | UI Action |
|----------|----------|------|-----------|
| Empty items list in pledge | Exception | 400 | Disable submit until ≥1 item added |
| Empty accounts in pledge | Exception | 400 | Require ≥1 account selected |
| Account amounts don't match item totals | System doesn't validate this (money is already with customer) | — | UI should warn if sum mismatch |
| Bill ID collision (extremely unlikely) | Regenerates ID | — | Transparent to user |
| Record negative interest | System allows | — | UI should validate ≥ 0 |
| Record interest on redeemed item | System allows (no status check) | — | UI should not show interest button for redeemed items |
| Customer has no active items, try to redeem | Empty bill, exception on empty items list | 400 | Don't show redeem if no active items |

### Interest Edge Cases

| Scenario | Behavior | HTTP | UI Action |
|----------|----------|------|-----------|
| No interest ever recorded | `GET /interest/{id}/current` returns `204` | 204 | Show "No interest recorded yet" |
| Owner enters wrong interest | Use `PUT /interest/set` to override | 200 | Show "Edit Interest" button |
| Interest reduces outstanding below 0 | Rare but possible if paid_amount is high | — | Show negative balance as "credit" |

---

## 12. UI Pages Mapping

Based on all the above APIs and flows, here are the recommended UI pages:

### Page 1: Login
- Password form + WebAuthn (fingerprint) button
- APIs: `POST /api/auth/login`, `POST /api/auth/webauthn/login/*`

### Page 2: Dashboard (Home)
- Stats cards: Outstanding, Active, Defaulted, Hold
- Account balance cards
- Recent bills list
- APIs: `GET /api/items/dashboard`, `GET /api/accounts`, `GET /api/bills?size=5`

### Page 3: Customers List
- Searchable table/card grid of all customers
- "Add New" button
- APIs: `GET /api/customer/get`, `GET /api/customer/count`

### Page 4: Customer Detail
- Full profile with tabs: Info, Contacts, Addresses, ID Proofs, Relatives
- Section: "Active Pledges" showing items
- Section: "Bill History"
- APIs: `GET /api/customer/{id}`, `GET /api/items/customer/{id}`, `GET /api/bills/customer/{id}`

### Page 5: Customer Onboarding Form
- Multi-step form: Personal → Contact → Address → ID Proof → Relative
- APIs: `POST /api/customer/onboard`

### Page 6: Accounts
- List of all accounts with balances
- Click to see transaction ledger
- "Add Account" modal
- APIs: `GET /api/accounts`, `GET /api/accounts/{id}/transactions`, `POST /api/accounts`

### Page 7: Ornaments Catalog
- Grid of ornament types with rates
- Edit rate inline
- APIs: `GET /api/ornaments`, `POST /api/ornaments`, `PUT /api/ornaments/{id}`

### Page 8: Create Pledge Bill
- Select customer (search/dropdown)
- Add items: ornament type, weight, amount, interest rate, due date, location
- Select payment accounts with amounts
- Review summary → Submit
- APIs: `GET /api/customer/get`, `GET /api/ornaments`, `GET /api/accounts`, `POST /api/bills/pledge`

### Page 9: Redeem Items
- Select customer → show their active items
- Check items to redeem
- System shows outstanding per item (principal + owner-set interest - paid)
- Select payment accounts
- Submit
- APIs: `GET /api/items/customer/{id}/active`, `GET /api/bills/interest/{item}/current`, `POST /api/bills/redeem`

### Page 10: Items Management
- Tabbed view: Active | Hold | Defaulted | Redeemed | Auctioned
- For each item: detail view, interest history
- Interest recording form
- APIs: `GET /api/items/status/{status}`, `GET /api/items/{id}`, `POST /api/bills/interest/record`

### Page 11: Bills / Ledger
- All bills with filters (date, type, customer)
- Click to see bill detail (items + accounts + transactions)
- Print/export bill
- APIs: `GET /api/bills`, `GET /api/bills/{id}`, `GET /api/accounts/{id}/transactions`

### Page 12: Reports
- Date range filter
- Total lending vs total received
- Interest earned
- Defaulted items summary
- Account-wise summary
- APIs: Combine `GET /api/bills/type/*`, `GET /api/items/dashboard`, `GET /api/accounts/transactions`

---

## Appendix: Error Response Format

All errors from `GlobalExceptionHandler` return:

```json
{
  "error": "Validation Error",
  "message": "Customer not found with ID: 99"
}
```

| Error Source | HTTP Status |
|-------------|-------------|
| `CustomerValidationException` | 400 |
| `IllegalArgumentException` | 500 (consider adding handler for 400) |
| `GenericCustomerOnboardingException` | 500 |
| `MethodArgumentNotValidException` | 400 |
| `AuthenticationException` | 401 |
| Unhandled exception | 500 |

> **Recommendation for UI**: Always check `response.ok` before processing. Display the `message` field in a toast/snackbar on errors.
