# sa-fintech-payments-api

A South African fintech backend learning lab for simulated merchant payments, refunds, webhooks, balances, settlements, reconciliation, and audit logs.

This project is a safe simulation. It must never process real money or connect to real banks, card processors, PayShap services, debit order systems, or production payment providers.

## Current Status

Milestone 4 is in progress: payment attempts and payment statuses.

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
- Controlled payment status transitions
- Invoice marked `PAID` only after a successful payment status
- Minimal audit log records for payment creation and payment status changes
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
- OpenAPI / Swagger later
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

## Interview Notes

This project is being built milestone by milestone to show backend API design and fintech domain thinking. The first foundation proves the project can compile, run tests, expose a safe public health endpoint, and enforce protected-by-default API behavior before payment features are added.

Milestone 2 starts the merchant identity model. Registration creates a merchant and one owner user. Login returns a JWT containing the merchant ID and merchant user ID, which is important because future financial records must be merchant-scoped.

Milestone 3 adds the first merchant-owned business records. Customers and invoices are always looked up through the authenticated merchant, invoice amounts are ZAR-only, and paid invoices cannot be cancelled. This sets up the next milestone: simulated payment attempts against issued invoices.

Milestone 4 introduces the difference between an invoice and a payment attempt. Creating a payment does not automatically mean the customer paid. The system only marks an invoice `PAID` after a controlled transition to `SUCCEEDED`, and it prevents more than one successful payment for the same full-payment invoice.
