# Architecture

## Architecture Style

Use a modular layered monolith.

The project should not use microservices. A modular monolith keeps the system easier to build, test, understand, and explain while still allowing clean domain boundaries.

## Tech Stack

The planned stack is:

- Java 21
- Spring Boot 3.5.x
- Maven
- PostgreSQL
- Flyway
- Spring Security
- JWT
- OpenAPI / Swagger
- JUnit 5
- Spring Boot Test
- Testcontainers
- Docker Compose

## Package Organization

Organize code by business domain, not by technical layer alone.

Suggested top-level modules or packages:

- `common`
- `auth`
- `merchant`
- `customer`
- `invoice`
- `payment`
- `webhook`
- `refund`
- `balance`
- `settlement`
- `reconciliation`
- `audit`

Each domain module may contain:

- `controller`
- `dto`
- `service`
- `repository`
- `domain` or `entity`
- `mapper` if needed

## Main Modules

### Common

Shared primitives and helpers that are genuinely cross-cutting, such as error responses, base exceptions, money utilities, and shared validation support.

### Auth

Authentication, login, JWT issuing, password handling, and authenticated principal handling.

### Merchant

Merchant registration, merchant profile, merchant status, and merchant ownership.

### Customer

Merchant-owned customer records, including South African-friendly contact information.

### Invoice

Invoice creation, invoice status lifecycle, invoice cancellation, and invoice payment state.

### Payment

Payment attempts, simulated payment methods, provider references, idempotency support, and controlled payment status transitions.

### Webhook

Simulated provider event receiving, storage, validation, duplicate handling, and safe processing.

### Refund

Refund requests, refund status lifecycle, refund limits, and links to original payments.

### Balance

Simple merchant balance tracking for pending, available, settled, refunded, fee, gross, and net amounts. The first version should remain simple but leave room for a future ledger-style model.

### Settlement

Manual settlement batch creation, eligible payment selection, gross and net settlement totals, and settlement status tracking.

### Reconciliation

Generated mock provider reports, internal-versus-external comparison, and reconciliation exceptions.

### Audit

Append-only-style records of important user, system, webhook, and financial actions.

## Database Approach

Use PostgreSQL as the primary database.

Use Flyway for schema migrations. Every schema change should be captured in a migration.

Database design should prioritize:

- Clear ownership by merchant
- Strong foreign keys
- Useful indexes for lookup and isolation
- Unique constraints for idempotency keys and provider references where appropriate
- Explicit status fields
- Auditable timestamps
- Database-backed guarantees for duplicate prevention
- Explicit created and updated timestamps on mutable domain records
- Append-only-style storage for audit events and webhook events

Merchant-owned tables should either contain `merchant_id` directly or have a mandatory relationship to a parent record that contains `merchant_id`. Code should not depend on the client to tell the truth about ownership.

## Authentication Approach

Use Spring Security with JWT for API authentication.

Version one starts with one merchant owner account per merchant. The schema should not block adding multiple merchant users and roles later.

Future roles should include:

- `OWNER`
- `FINANCE`
- `SUPPORT`
- `VIEWER`

Authentication answers who the caller is. Authorization answers whether they can access a specific merchant-owned resource.

## Money-Handling Rules

Never use `float` or `double` for money.

Use `BigDecimal` in Java unless a clear reason is given to use integer cents. The database representation must be chosen deliberately and documented before financial persistence is implemented.

Default currency is ZAR.

Financial calculations must be:

- Deterministic
- Rounded deliberately
- Tested
- Easy to explain

Every amount should have currency context. Important values include gross amount, fee amount, refund amount, net amount, pending balance, available balance, and settled balance.

Before implementing persistence for money, record the database decision in `docs/DECISIONS.md`. The decision should explain:

- Whether amounts are stored as `NUMERIC` values or integer minor units
- Scale and rounding rules
- How ZAR currency is represented
- How fee rounding is handled
- How historical amounts are protected from accidental recalculation

## Idempotency And Consistency

Payment creation and webhook processing must be safe under retries.

Important operations should use database constraints and transactions, not only application-level checks. For example, idempotency keys should be unique within the documented scope, and provider event IDs should not be processed twice.

Idempotency records should store:

- Merchant context
- Operation name
- Idempotency key
- Request fingerprint or hash
- Result reference
- Creation timestamp

If the same key is reused with a different request fingerprint, the request should be rejected and audited.

## Auditability

Audit logging is a core architecture concern, not an afterthought.

Audit records should include:

- Actor type
- Actor ID when available
- Merchant ID when applicable
- Action
- Target type
- Target ID
- Previous state where useful
- New state where useful
- Timestamp
- Correlation or request ID when available

Audit logs should be append-only in normal application behavior. They should help explain important financial and security events without storing sensitive secrets.

## Settlement Architecture

Settlement must be modeled separately from payment success.

Settlement batches should be merchant-scoped and should contain enough detail to explain:

- Which payments were included
- Which payments were excluded
- Gross total
- Fee total
- Refund or deduction total
- Net settlement total
- Settlement status

A payment should not be settled twice. This should be protected by both business logic and persistence rules where practical.

## Reconciliation Architecture

Reconciliation compares internal records with generated mock provider records.

Reconciliation should not silently update payment records to match the provider report. It should produce clear reconciliation results for investigation.

Matching should use stable identifiers, especially provider reference and merchant context. Amount, currency, status, and date should be compared deliberately.

## Testing Strategy

Testing should include:

- Unit tests for domain rules and status transitions
- Service tests for business workflows
- Integration tests for database behavior
- Testcontainers PostgreSQL tests for realistic persistence checks
- Security tests for merchant data isolation
- Edge-case tests for duplicate payments, duplicate webhooks, refund limits, settlement totals, and reconciliation mismatches

Every milestone should include tests before moving forward.

## API Documentation

Use OpenAPI / Swagger to document endpoints.

API documentation should include practical request and response examples, especially for payment, webhook, refund, settlement, and reconciliation workflows.

Document error responses for fintech-relevant failures such as duplicate idempotency key reuse, invalid status transition, cross-merchant access, over-refund, already settled payment, and reconciliation mismatch generation.

## Deployment Direction

Use Docker Compose for local development.

Later deployment can target a Docker-friendly platform such as Render, Railway, Fly.io, or a VPS. Deployment should remain secondary until the backend is stable and tested.
