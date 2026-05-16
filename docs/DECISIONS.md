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
