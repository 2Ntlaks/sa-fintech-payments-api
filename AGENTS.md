# AGENTS.md

## Project Identity

This project is called `sa-fintech-payments-api`.

It is a South African fintech backend learning lab. The system simulates merchant payments, settlement, reconciliation, refunds, webhooks, balances, and audit logs for learning, portfolio, and interview preparation.

This project must never process real money. Do not connect it to real banks, real card processors, real PayShap services, real debit order systems, or production payment providers. All payment methods, provider references, webhook events, bank account examples, settlements, and reconciliation reports are simulations.

## Codex Behavior In This Repository

- Do not rush into code.
- First inspect the repository before making changes.
- Read this `AGENTS.md` file and the relevant files in `docs/` before editing.
- Work milestone by milestone according to `docs/ROADMAP.md`.
- Keep changes small, reviewable, and easy to explain.
- Prefer safe, simple, realistic backend design.
- Explain what changed and why after each meaningful change.
- Update documentation when architecture, behavior, API contracts, or domain rules change.
- Ask for clarification only when necessary.
- Do not introduce unrelated refactors.
- Do not overwrite user work or revert changes unless explicitly asked.
- Never introduce code that processes real money.

## Tech Stack

Use this stack unless the product owner explicitly changes it:

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

## Architecture

Use a modular layered monolith.

Do not create microservices for this project. The goal is to learn realistic fintech backend design without unnecessary distributed-system complexity.

Organize packages by business domain.

Suggested modules:

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

Layering inside modules:

- `controller`
- `dto`
- `service`
- `repository`
- `domain` or `entity`
- `mapper` if needed

Prefer clear domain boundaries over clever abstractions. Keep shared utilities in `common` only when they are genuinely shared.

## Money Rules

- Never use `float` or `double` for money.
- Use `BigDecimal` in Java unless a clear reason is given to use integer cents.
- Use ZAR as the default currency.
- Explain the chosen database money representation before implementing financial persistence.
- Financial calculations must be deterministic and tested.
- Monetary values must have explicit currency context.
- Rounding rules must be deliberate, documented, and tested.
- Fees, refunds, balances, settlements, and reconciliation totals must be covered by tests.
- Do not compare `BigDecimal` values with `equals` when scale differences could matter; use deliberate comparison helpers.
- Normalize persisted money values to the documented scale and rounding mode.
- Store gross amount, fee amount, refund amount, and net amount separately when they have different business meanings.
- Never silently recalculate historical financial amounts in a way that changes past records.

## Security Rules

- Merchant data isolation is mandatory.
- A merchant must never access another merchant's data.
- Protected endpoints must require authentication.
- Future merchant roles should include `OWNER`, `FINANCE`, `SUPPORT`, and `VIEWER`.
- Do not log passwords, JWTs, refresh tokens, secrets, or sensitive authentication data.
- Authorization checks must be tested, especially for cross-merchant access attempts.
- Treat authentication and authorization as separate concerns.
- Merchant-owned tables must include an explicit merchant ownership path, either directly with `merchant_id` or through a required parent that has `merchant_id`.
- Repository queries for merchant-owned data must be scoped by merchant unless there is a documented platform-admin use case.
- Do not rely only on client-supplied IDs for ownership decisions.

## Payment Rules

- Payment status transitions must be controlled.
- Invalid payment transitions must be rejected.
- Duplicate payment processing must be prevented with idempotency keys.
- Every important payment status change must create an audit log.
- A successful payment is not the same as a settlement.
- Failed payment attempts should remain recorded for auditability.
- A full paid invoice must not receive another successful payment in version one.
- Payment creation must be atomic: idempotency check, payment creation, and invoice eligibility checks must not allow duplicate successful payment outcomes.
- Provider references must be unique where the simulated provider contract says they are unique.
- Status transition code should be centralized instead of scattered across controllers.

## Idempotency Rules

- Idempotency keys must be scoped to the merchant and operation.
- Reusing the same key with the same request should return the original result.
- Reusing the same key with a materially different request should be rejected and audited.
- Store enough request fingerprint data to detect unsafe idempotency-key reuse.
- Database constraints must back important idempotency guarantees.

## Webhook Rules

- Webhooks may arrive late, duplicated, or out of order.
- Webhook events must be stored.
- Duplicate webhook events must not process financial actions twice.
- Webhook processing decisions must be auditable.
- Simulated webhook signatures should be supported later.
- Webhook handlers must not blindly trust external payloads.
- Webhook processing must be idempotent using provider event IDs or equivalent simulated identifiers.
- Store raw payload, processing status, provider event ID, event type, and related provider reference where possible.
- Never apply a webhook state change that would move a payment, refund, or settlement backward.

## Audit Rules

- Audit logs must be append-only in normal application behavior.
- Audit logs must record actor type, actor ID where available, merchant ID where applicable, action, target type, target ID, timestamp, and relevant before/after state.
- Audit logs must not contain passwords, JWTs, secrets, or full sensitive payloads.
- Failed financial and security-sensitive attempts should be audited when useful for investigation.
- Financial status changes must be explainable from audit logs and stored domain records.

## Settlement Rules

- Settlement must remain separate from payment success.
- A payment must not be settled twice.
- Settlement batches must be merchant-scoped.
- Settlement totals must show gross amount, fees, refunds or deductions, and net amount.
- Fully refunded payments must not be paid out as if still valid.
- Settlement calculations must be deterministic and covered by tests.

## Reconciliation Rules

- Reconciliation compares internal records against simulated external provider records.
- Reconciliation must report mismatches; it must not silently mutate payments to force a match.
- Matching should use stable identifiers such as provider reference plus merchant context.
- Reconciliation results must identify missing internal records, missing external records, amount mismatches, status mismatches, and duplicate provider references.
- Reconciliation reports must be merchant-scoped and auditable.

## Testing Rules

- Every milestone must include tests.
- Use Testcontainers for PostgreSQL database integration tests.
- Test edge cases, not only happy paths.
- Tests must prove important fintech behavior.
- Include tests for merchant isolation, duplicate payment prevention, invalid status transitions, refund limits, settlement calculations, and reconciliation mismatches.
- Prefer focused tests that explain the business rule being protected.
- Do not mark a milestone complete if core financial edge-case tests for that milestone are missing.
- Add regression tests for every bug fix involving money, security, status transitions, idempotency, webhooks, settlement, or reconciliation.

## Documentation Rules

- The README must be recruiter-friendly.
- Each milestone must include short interview notes.
- Document what was learned from each fintech concept.
- Keep docs practical and beginner-friendly.
- Update `docs/DECISIONS.md` whenever a major architecture or product decision is made.

## Current Build Direction

Build the project in milestones:

1. Project foundation and domain design
2. Merchant registration, login, and authentication
3. Customers and invoices
4. Payment attempts and payment statuses
5. Webhook simulation and idempotency
6. Refunds
7. Fees and merchant balances
8. Manual settlement batches
9. Reconciliation using mock provider reports
10. Audit logs, security hardening, documentation, and portfolio README

Before application code is added, keep the focus on domain clarity, workflow design, and project guidance.
