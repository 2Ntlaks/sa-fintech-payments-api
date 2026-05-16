# Decisions

This file records major product and architecture decisions for `sa-fintech-payments-api`.

Use this format for future decisions:

```text
## YYYY-MM-DD: Decision Title

Decision:

Reason:

Alternatives considered:

Tradeoffs:
```

## 2026-05-16: Build A Safe South African Fintech Simulation

Decision:

Build `sa-fintech-payments-api` as a safe simulation of a South African merchant payments and reconciliation backend. It must never process real money.

Reason:

The project is intended for learning, portfolio building, and interview preparation. Simulating payments allows the project to explore real fintech backend concepts without legal, financial, or operational risk.

Alternatives considered:

- Integrating a real payment provider
- Building a generic CRUD payments API

Tradeoffs:

- The project will not demonstrate production payment processor onboarding.
- The project can focus deeply on domain modeling, state transitions, idempotency, webhooks, refunds, settlement, reconciliation, audit logs, and security.

## 2026-05-16: Start API-Only

Decision:

Start as an API-only backend. Add a dashboard later only after the backend is stable.

Reason:

The primary learning goal is fintech backend engineering. API-only development keeps the early project focused on domain logic, tests, and financial workflows.

Alternatives considered:

- Building a dashboard from the beginning
- Building a full-stack application first

Tradeoffs:

- The project will initially be less visual.
- The backend will be stronger, easier to test, and easier to explain in technical interviews.

## 2026-05-16: Use Java, Spring Boot, And PostgreSQL

Decision:

Use Java 21, Spring Boot 3.5.x, Maven, PostgreSQL, Flyway, Spring Security, JWT, OpenAPI / Swagger, JUnit 5, Spring Boot Test, Testcontainers, and Docker Compose.

Reason:

This stack is strong for structured backend APIs, financial workflows, authentication, database transactions, testing, and portfolio value in fintech or enterprise environments.

Alternatives considered:

- Node.js with Express or NestJS
- Python with FastAPI or Django
- .NET with ASP.NET Core

Tradeoffs:

- Java and Spring Boot require more setup than smaller frameworks.
- The stack is highly relevant for serious backend systems and helps model enterprise-style fintech architecture.

## 2026-05-16: Use A Modular Layered Monolith

Decision:

Use a modular layered monolith organized by business domain.

Reason:

The project needs clear domain boundaries without the operational complexity of microservices.

Alternatives considered:

- Microservices
- Single flat package structure
- Pure technical-layer packaging

Tradeoffs:

- The system will not demonstrate service-to-service communication early.
- The codebase will be easier to build, test, refactor, and explain.

## 2026-05-16: Start With Full Invoice Payments Only

Decision:

Version one supports full invoice payment only. Partial invoice payments are deferred.

Reason:

Full payments keep the first payment lifecycle simpler while still allowing realistic payment, webhook, refund, settlement, and reconciliation behavior.

Alternatives considered:

- Supporting partial invoice payments from the beginning

Tradeoffs:

- Some real-world invoice behavior is deferred.
- The first version is more likely to be completed and tested well.

## 2026-05-16: Include Simple Fees In Version One

Decision:

Include a simple platform fee on successful payments in version one.

Reason:

Fees make settlement and balances more realistic by showing the difference between gross amount, fee amount, and net amount.

Alternatives considered:

- Deferring fees until settlement
- Omitting fees entirely

Tradeoffs:

- Fee calculations add complexity and require careful rounding tests.
- The project better demonstrates real payment platform economics.

## 2026-05-16: Start With Simple Balances, Not A Full Ledger

Decision:

Use a simple merchant balance model first, but design it so it can evolve into a ledger-style system later.

Reason:

The learner wants to understand balance concepts before implementing a more advanced ledger.

Alternatives considered:

- Full double-entry ledger from the beginning
- No balance model until settlement

Tradeoffs:

- The first balance model may need refactoring later.
- The project remains approachable while still teaching pending, available, settled, refunded, fee, gross, and net concepts.

## 2026-05-16: Require Database-Backed Financial Safety Guarantees

Decision:

Important financial safety rules must be backed by database constraints and transactions where practical, especially idempotency keys, provider event deduplication, provider references, merchant ownership, and settlement inclusion.

Reason:

Application checks alone are easy to bypass under retries, concurrency, or future refactors. Fintech systems need persistence-level safeguards for duplicate prevention and data integrity.

Alternatives considered:

- Enforcing all safety rules only in service code
- Deferring constraints until later milestones

Tradeoffs:

- Schema design and tests require more care.
- The project becomes more realistic and safer under retry and duplicate-event scenarios.

## 2026-05-16: Reconciliation Reports Mismatches Instead Of Mutating Payments

Decision:

Reconciliation should produce matched results and exceptions, but it should not silently rewrite payment records to match simulated provider reports.

Reason:

Reconciliation is an investigation and proof workflow. Automatically changing financial records during reconciliation can hide data-quality problems and make audit trails harder to trust.

Alternatives considered:

- Automatically updating internal payment statuses from reconciliation reports
- Treating provider reports as always correct

Tradeoffs:

- Some mismatches require explicit follow-up instead of automatic correction.
- The system better demonstrates real fintech operational controls and auditability.

## 2026-05-16: Use Maven Wrapper For Builds

Decision:

Use the Maven Wrapper so the project can build even when Maven is not installed globally.

Reason:

The local machine has Java available but does not have `mvn` on the PATH. The wrapper makes setup simpler for the learner and for recruiters cloning the project.

Alternatives considered:

- Requiring global Maven installation
- Deferring build setup until later

Tradeoffs:

- The wrapper adds a few generated files to the repository.
- The project becomes easier to run consistently.

## 2026-05-16: Add A Temporary Local Profile Without Database Autoconfiguration

Decision:

Add a `local` Spring profile that disables database, JPA, and Flyway autoconfiguration until Docker/PostgreSQL setup is added.

Reason:

The project includes persistence dependencies from the start, but Docker will be handled later. The local profile lets the health endpoint and foundation app run before database infrastructure exists.

Alternatives considered:

- Removing database dependencies until milestone 2
- Requiring PostgreSQL immediately
- Letting the app fail until Docker is added

Tradeoffs:

- The local profile is temporary and should not be mistaken for the real persistence setup.
- It keeps the early learning loop smooth while preserving the planned production stack.

## 2026-05-16: Use UUID Primary Keys For Domain Tables

Decision:

Use UUID primary keys for core domain tables, starting with `merchants` and `merchant_users`.

Reason:

UUIDs avoid exposing sequential database IDs in APIs, work well for future distributed or imported records, and are common in backend systems that need stable public identifiers.

Alternatives considered:

- Auto-incrementing integer IDs
- Natural keys such as email or registration number

Tradeoffs:

- UUIDs are larger than integer IDs and can make indexes heavier.
- They provide safer public identifiers and fit the long-term API design better.

## 2026-05-16: Start Database Schema With Merchants And Merchant Users

Decision:

The first Flyway migration creates `merchants` and `merchant_users`.

Reason:

Merchant ownership is the root of the system. Starting with these tables establishes the tenant-isolation model before customers, invoices, payments, refunds, settlements, reconciliation, and audit logs are added.

Alternatives considered:

- Starting with payments first
- Delaying database schema until all domain models are designed
- Creating all planned tables immediately

Tradeoffs:

- The schema is incomplete until later milestones add more tables.
- The project gets a scalable foundation without overbuilding unused financial tables too early.

## 2026-05-16: Use JWTs Carrying Merchant Identity

Decision:

Use JWT access tokens for API authentication. Tokens should include the merchant user ID as the subject and include the merchant ID as a claim.

Reason:

The backend is API-only, and future financial records must be scoped to the authenticated merchant. Carrying both user and merchant identity in the token makes protected endpoints straightforward to authorize while still allowing future roles.

Alternatives considered:

- Session-based authentication
- Tokens containing only the user ID
- API keys for merchants

Tradeoffs:

- JWTs require careful secret management and expiry handling.
- Including merchant identity gives future service methods a clear tenant context for merchant data isolation.

## 2026-05-16: Use Docker Compose For Local PostgreSQL

Decision:

Add a `compose.yaml` file with a PostgreSQL 16 service for local development.

Reason:

The backend now has real database-backed merchant and authentication workflows. Docker Compose gives the learner a repeatable way to run PostgreSQL locally without manually installing and configuring PostgreSQL.

Alternatives considered:

- Installing PostgreSQL directly on Windows
- Continuing with only Testcontainers
- Using an in-memory database for local development

Tradeoffs:

- Docker Desktop must be installed and running.
- Local database setup becomes repeatable and closer to the future deployment model.

## 2026-05-16: Preserve Applied Flyway Migrations

Decision:

Do not edit a Flyway migration after it has been applied to a database. Fix schema drift with a new migration instead.

Reason:

Flyway tracks checksums for applied migrations. Editing an applied migration causes validation failures and makes environments disagree about schema history.

Alternatives considered:

- Editing `V1` after it had already run locally
- Running Flyway repair for normal schema evolution
- Dropping the local database volume whenever a migration changes

Tradeoffs:

- The migration history may include small corrective migrations.
- The project models professional database change management more accurately.

## 2026-05-16: Store Version-One Invoice Money As BigDecimal And NUMERIC(19,2)

Decision:

Use Java `BigDecimal` for invoice amounts and PostgreSQL `NUMERIC(19,2)` for persisted invoice money. Version-one invoices are ZAR-only, and amounts with more than two decimal places are rejected instead of rounded.

Reason:

Financial amounts must be deterministic and explainable. `BigDecimal` avoids floating-point precision errors, and `NUMERIC(19,2)` matches the version-one need to store South African rand and cents clearly.

Alternatives considered:

- Integer cents in Java and PostgreSQL
- PostgreSQL `MONEY`
- Java `double` or `float`

Tradeoffs:

- `BigDecimal` requires deliberate scale handling and careful comparisons.
- Integer cents may be introduced later if the project needs stricter minor-unit arithmetic.
- Rejecting unsafe scale is stricter than silent rounding, but it protects financial records from accidental hidden changes.

## 2026-05-16: Enforce Invoice Customer Ownership In The Database

Decision:

Invoices store `merchant_id` and `customer_id`, and the database enforces that the referenced customer belongs to the same merchant through a composite foreign key.

Reason:

Service-layer merchant checks are necessary, but fintech tenant isolation should also be protected by persistence constraints where practical.

Alternatives considered:

- Relying only on service methods such as `findByIdAndMerchantId`
- Omitting `merchant_id` from invoices and deriving ownership only through customers

Tradeoffs:

- The schema has one extra composite uniqueness constraint on customers.
- Invoice queries remain straightforward and tenant isolation becomes harder to break through future code changes.

## 2026-05-16: Model Payment Attempts Separately From Invoices

Decision:

Create a `payment_attempts` table and payment module instead of storing payment state only on invoices. Payment attempts copy invoice amount and currency, generate simulated provider references internally, and move through controlled statuses.

Reason:

Real payment systems need to preserve failed attempts, provider references, status history, and retry behavior. An invoice describes what is owed; a payment attempt describes an attempt to settle that invoice.

Alternatives considered:

- Storing only a `paid` flag on invoices
- Updating invoice status directly from the API without payment attempt records

Tradeoffs:

- More tables and service logic are required.
- The system can now explain payment attempts, failures, provider references, and successful payment proof more realistically.

## 2026-05-16: Add Minimal Audit Logging During Payment Milestone

Decision:

Add a simple `audit_logs` table and service in Milestone 4 for payment attempt creation and payment status changes.

Reason:

Payment status changes are financially meaningful. They should be traceable immediately, even though the full audit-log hardening milestone comes later.

Alternatives considered:

- Waiting until Milestone 10 to create audit logs
- Logging payment status changes only to application logs

Tradeoffs:

- The audit model starts simple and will need expansion later.
- Payment behavior is already explainable from persisted records instead of console logs.

## 2026-05-16: Prevent Duplicate Successful Full Payments With A Partial Unique Index

Decision:

Use a PostgreSQL partial unique index on `payment_attempts(invoice_id)` where status is `SUCCEEDED`.

Reason:

Version one supports full invoice payment only. A full-payment invoice must not have more than one successful payment, even under retries or future code mistakes.

Alternatives considered:

- Service-only checks
- Waiting for idempotency keys in Milestone 5

Tradeoffs:

- This is specific to the full-payment version-one model.
- It provides a database-backed financial safety guarantee before the full idempotency milestone.
