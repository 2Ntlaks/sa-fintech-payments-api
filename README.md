# sa-fintech-payments-api

A South African fintech backend learning lab for simulated merchant payments, refunds, webhooks, balances, settlements, reconciliation, and audit logs.

This project is a safe simulation. It must never process real money or connect to real banks, card processors, PayShap services, debit order systems, or production payment providers.

## Current Status

Milestone 10 is in progress: audit, security hardening, and portfolio polish.

Implemented so far:

- Spring Boot project scaffold
- Maven Wrapper for local builds without system Maven
- Modular package skeleton for the planned domain modules
- PostgreSQL and Flyway foundation
- Initial merchant and merchant-user database migration
- Merchant owner registration endpoint
- Merchant owner login endpoint
- JWT-based protected merchant profile endpoint
- Merchant-scoped customer creation, listing, and lookup
- Merchant-scoped invoice creation, listing, lookup, and cancellation
- ZAR invoice amounts stored with `BigDecimal` and PostgreSQL `NUMERIC(19,2)`
- Simulated payment attempts for issued invoices
- Idempotency-key support for payment creation retries
- Controlled payment status transitions
- Invoice marked `PAID` only after a successful payment status
- Minimal audit log records for payment creation and payment status changes
- Simulated payment webhook endpoint with provider event deduplication
- Webhook event storage with duplicate and out-of-order handling
- Full and partial simulated refunds for successful payments
- Refund records linked to original payment attempts
- Refund limits that prevent over-refunding a payment
- Payment and invoice refund states: `PARTIALLY_REFUNDED` and `REFUNDED`
- Simple platform fee calculation on successful payments
- Payment gross, fee, and net amounts stored separately
- Merchant balance summary with gross, fee, refunded, available, and settled totals
- Manual settlement batch creation for eligible merchant payments
- Settlement items that preserve gross, fee, refund, and net totals
- Duplicate settlement prevention for already-settled payments
- Mock provider reconciliation reports
- Reconciliation mismatch detection for missing, duplicate, amount, and status differences
- Merchant-scoped audit log read endpoint
- Protected-by-default API routes with focused security tests
- Public health endpoint at `GET /api/v1/health`
- Basic stateless Spring Security configuration
- Tests for foundation, authentication, merchant profile, customer, invoice, payment, and database migration behavior

## Tech Stack

- Java 21 target
- Spring Boot 3.5.x
- Maven
- PostgreSQL
- Flyway
- Spring Security
- JWT
- OpenAPI / Swagger UI
- JUnit 5
- Spring Boot Test
- Testcontainers
- Docker Compose

## Run Tests

On Windows:

```powershell
.\mvnw.cmd test
```

The database migration tests use PostgreSQL through Testcontainers. If Docker is not available, those tests are skipped; Docker must be running for the full database test coverage.

## Run Locally Before Docker

Until PostgreSQL and Docker Compose are added, run the health-only foundation app with the `local` profile:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Then open:

```text
http://localhost:8080/api/v1/health
http://localhost:8080/swagger-ui.html
http://localhost:8080/v3/api-docs
```

The `local` profile intentionally disables database-backed merchant/auth endpoints. Registration and login require PostgreSQL.

## Run With PostgreSQL

After Docker Desktop is installed and running, start PostgreSQL:

```powershell
docker compose up -d postgres
```

Then run the API without the `local` profile:

```powershell
.\mvnw.cmd spring-boot:run
```

The app uses these default database settings:

```text
DB_URL=jdbc:postgresql://localhost:5432/sa_fintech_payments
DB_USERNAME=sa_fintech
DB_PASSWORD=sa_fintech
```

Flyway will create the database tables automatically when the app starts.

Swagger UI is available at `http://localhost:8080/swagger-ui.html`, and the OpenAPI JSON document is available at `http://localhost:8080/v3/api-docs`. The docs endpoints are public, while business API routes remain protected by JWT unless explicitly documented as public.

To stop PostgreSQL:

```powershell
docker compose down
```

To remove the local database volume as well:

```powershell
docker compose down -v
```

## Database Foundation

The Flyway migrations currently create:

- `merchants`
- `merchant_users`
- `customers`
- `invoices`
- `payment_attempts`
- `idempotency_records`
- `webhook_events`
- `refunds`
- `merchant_balances`
- `settlement_batches`
- `settlement_batch_items`
- `reconciliation_reports`
- `reconciliation_report_items`
- `audit_logs`

The schema uses UUID primary keys, PostgreSQL constraints, `created_at` and `updated_at` timestamps, and indexes that support future merchant-scoped access control.

`V2__align_merchant_currency_columns_with_jpa.sql` shows an important migration habit: once a migration has been applied, fix schema drift with a new migration rather than editing migration history.

The first merchant-user model supports one owner now, while leaving room for future roles:

- `OWNER`
- `FINANCE`
- `SUPPORT`
- `VIEWER`

Invoice money is represented as Java `BigDecimal` and PostgreSQL `NUMERIC(19,2)`. Version one stores ZAR only and rejects invoice amounts with more than two decimal places rather than silently rounding them.

Payment attempts copy the invoice amount and currency. The client does not submit the payment amount, because the invoice is the source of truth for what is owed.

Payment creation accepts an optional `Idempotency-Key` header. Reusing the same key with the same payment request returns the original payment attempt. Reusing the same key with different request details is rejected and audited.

Simulated webhook events are stored by provider event ID. Duplicate provider events return an `IGNORED_DUPLICATE` decision, and out-of-order events that would move a payment backward are stored as `IGNORED_OUT_OF_ORDER`.

Refunds are stored as ZAR `NUMERIC(19,2)` records linked to the original merchant-owned payment attempt. Successful refunds update both the payment and invoice refund state. A payment may have multiple partial refunds, but the total successful refund amount may never exceed the original payment amount.

Successful payments calculate a simulated platform fee of `2.9% + R1.00`, rounded to two decimal places with `HALF_UP`. Payment attempts store gross, fee, and net amounts separately. Merchant balances add net amounts when payments succeed and deduct refund amounts when refunds succeed. Version one does not automatically reverse platform fees on refunds.

Settlement batches preserve calculated totals at creation time. Eligible payments are merchant-owned, not already settled, and in `SUCCEEDED` or `PARTIALLY_REFUNDED` state. Fully refunded payments are excluded from settlement. Each payment can appear in only one settlement batch.

Reconciliation reports compare internal merchant payment records with a submitted mock provider report. Matching uses provider reference. Mismatches are stored as report items and audited, but reconciliation does not mutate payment records.

Audit logs are merchant-scoped and available to authenticated merchant users. They record structured action and state information without storing passwords, JWTs, webhook secrets, or raw sensitive payloads.

## API Example

Health check:

```http
GET /api/v1/health
```

Example response:

```json
{
  "status": "UP",
  "service": "sa-fintech-payments-api",
  "timestamp": "2026-05-16T12:00:00Z"
}
```

Register merchant owner:

```http
POST /api/v1/auth/register
Content-Type: application/json
```

```json
{
  "businessName": "Mpho Tutoring",
  "tradingName": "Mpho Maths",
  "merchantType": "TUTORING_BUSINESS",
  "ownerFullName": "Mpho Dlamini",
  "ownerEmail": "mpho@example.com",
  "password": "strongPass123"
}
```

Login:

```http
POST /api/v1/auth/login
Content-Type: application/json
```

```json
{
  "email": "mpho@example.com",
  "password": "strongPass123"
}
```

Current merchant profile:

```http
GET /api/v1/merchants/me
Authorization: Bearer <access-token>
```

Create customer:

```http
POST /api/v1/customers
Authorization: Bearer <access-token>
Content-Type: application/json
```

```json
{
  "fullName": "Lerato Khumalo",
  "email": "lerato@example.com",
  "phoneNumber": "+27821234567"
}
```

Create invoice:

```http
POST /api/v1/invoices
Authorization: Bearer <access-token>
Content-Type: application/json
```

```json
{
  "customerId": "<customer-id>",
  "description": "Maths tutoring lesson",
  "amount": 250.00,
  "dueDate": "2026-05-30"
}
```

Cancel invoice:

```http
POST /api/v1/invoices/<invoice-id>/cancel
Authorization: Bearer <access-token>
```

Create simulated payment attempt:

```http
POST /api/v1/payments
Authorization: Bearer <access-token>
Idempotency-Key: payment-create-001
Content-Type: application/json
```

```json
{
  "invoiceId": "<invoice-id>",
  "paymentMethod": "PAYSHAP_SIMULATED"
}
```

Move a payment through its status lifecycle:

```http
POST /api/v1/payments/<payment-id>/status
Authorization: Bearer <access-token>
Content-Type: application/json
```

```json
{
  "status": "PROCESSING"
}
```

Then:

```json
{
  "status": "SUCCEEDED"
}
```

Process a simulated provider payment webhook:

```http
POST /api/v1/webhooks/simulated/payments
X-Simulated-Webhook-Secret: dev-only-simulated-webhook-secret
Content-Type: application/json
```

```json
{
  "providerEventId": "evt-pay-001",
  "eventType": "payment.status_changed",
  "providerReference": "SIM-PAY-123",
  "paymentStatus": "SUCCEEDED"
}
```

Create a simulated refund:

```http
POST /api/v1/refunds
Authorization: Bearer <access-token>
Content-Type: application/json
```

```json
{
  "paymentId": "<payment-id>",
  "amount": 50.00,
  "reason": "Customer returned one item"
}
```

View merchant balance:

```http
GET /api/v1/merchant-balance
Authorization: Bearer <access-token>
```

Create a manual settlement batch:

```http
POST /api/v1/settlement-batches
Authorization: Bearer <access-token>
```

List settlement batches:

```http
GET /api/v1/settlement-batches
Authorization: Bearer <access-token>
```

Create a reconciliation report from mock provider records:

```http
POST /api/v1/reconciliation-reports
Authorization: Bearer <access-token>
Content-Type: application/json
```

```json
{
  "records": [
    {
      "providerReference": "SIM-PAY-123",
      "amount": 250.00,
      "currency": "ZAR",
      "status": "SUCCEEDED"
    }
  ]
}
```

List reconciliation reports:

```http
GET /api/v1/reconciliation-reports
Authorization: Bearer <access-token>
```

List audit logs:

```http
GET /api/v1/audit-logs
Authorization: Bearer <access-token>
```

## Interview Notes

This project is being built milestone by milestone to show backend API design and fintech domain thinking. The first foundation proves the project can compile, run tests, expose a safe public health endpoint, and enforce protected-by-default API behavior before payment features are added.

Milestone 2 starts the merchant identity model. Registration creates a merchant and one owner user. Login returns a JWT containing the merchant ID and merchant user ID, which is important because future financial records must be merchant-scoped.

Milestone 3 adds the first merchant-owned business records. Customers and invoices are always looked up through the authenticated merchant, invoice amounts are ZAR-only, and paid invoices cannot be cancelled. This sets up the next milestone: simulated payment attempts against issued invoices.

Milestone 4 introduces the difference between an invoice and a payment attempt. Creating a payment does not automatically mean the customer paid. The system only marks an invoice `PAID` after a controlled transition to `SUCCEEDED`, and it prevents more than one successful payment for the same full-payment invoice.

Milestone 5 adds retry and provider-event safety. Payment creation can be retried with an idempotency key, webhook events are stored before processing decisions are returned, duplicate provider events do not reprocess financial actions, and late or backward status updates are ignored instead of mutating successful payments.

Milestone 6 adds refunds. Refunds are separate records linked to successful payments, partial refunds update payment and invoice state, full refunds move both records to `REFUNDED`, and over-refunds are rejected before any financial state changes.

Milestone 7 starts the balance model. Successful payments calculate and store gross, fee, and net amounts, and merchant balances track available funds after successful payments and refunds. The first version stays intentionally simple so settlement can build on clear totals before a full ledger is introduced.

Milestone 8 starts manual settlement. Settlement batches include eligible successful or partially refunded payments, preserve gross/fee/refund/net totals, prevent the same payment from being settled twice, and move the settled net amount from available balance into settled balance.

Milestone 9 starts reconciliation. Mock provider reports are compared against internal payments by provider reference. The system records matched items and exceptions such as missing internal records, missing external records, amount mismatches, status mismatches, and duplicate provider references without changing payment state.

Milestone 10 hardens the portfolio finish. Audit logs are queryable by the authenticated merchant, security tests prove protected routes require valid JWT merchant context, and the documentation explains the financial safeguards, limitations, and interview story.
