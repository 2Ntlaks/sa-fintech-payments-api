# Interview Notes

## Project Story

`sa-fintech-payments-api` is a simulated South African merchant payments and reconciliation API. It models how a fintech backend handles merchant onboarding, invoices, simulated payments, webhooks, refunds, fees, balances, settlements, reconciliation, and audit logs.

The project does not process real money. It is designed to show backend engineering and fintech domain understanding.

## What This Project Proves

## Backend API Design

This project demonstrates structured API design for a multi-tenant financial system. It separates merchants, customers, invoices, payments, refunds, settlements, reconciliation, and audit logs into clear domain areas.

## Fintech Domain Understanding

The project shows understanding that financial systems are not simple CRUD systems. They require controlled state, traceability, duplicate prevention, reconciliation, and strong access control.

## Payment Lifecycle Thinking

The project models payment attempts and payment statuses. It shows why failed attempts are preserved, why successful payment is a controlled state change, and why an invoice should not be paid twice.

## Settlement Vs Payment

The project separates customer payment from merchant settlement.

A payment means the customer has paid. A settlement means funds have been batched and paid out to the merchant in the simulation.

## Reconciliation

The project compares internal records against mock external provider reports. It detects missing records, amount mismatches, status mismatches, and duplicate provider references.

This proves understanding that fintech systems must verify records against external sources of truth.

## Idempotency

The project uses idempotency keys to prevent duplicate payment processing when requests are retried.

This demonstrates distributed-systems thinking and financial safety.

## Audit Logs

The project records important user, system, webhook, and financial actions.

Audit logs help answer who did what, when it happened, what changed, and which merchant was affected.

## Security

The project enforces merchant data isolation. A merchant must never access another merchant's financial data.

This demonstrates understanding of multi-tenant security in financial systems.

## Testing Financial Systems

The project includes tests for:

- Edge cases
- Merchant isolation
- Duplicate payment prevention
- Invalid status transitions
- Refund limits
- Settlement calculations
- Reconciliation mismatches

This shows that tests protect business rules, not only code paths.

## AI-Native Development Workflow

The project uses Codex through `AGENTS.md` and supporting documentation to guide consistent agentic development.

This demonstrates an AI-assisted engineering workflow where the human acts as product owner, learner, tester, reviewer, and explainer while Codex implements carefully within documented constraints.

## Milestone 1 Foundation

The first implementation milestone establishes a Spring Boot foundation before domain features are added.

It proves:

- The project can compile and run tests through the Maven Wrapper
- The API has a public health endpoint
- API routes are protected by default
- The package structure reflects the planned fintech modules
- Docker and PostgreSQL can be added later without blocking early foundation tests

## Example Interview Pitch

I built a simulated South African fintech merchant payments API. It supports ZAR invoices, simulated payment methods like card, EFT, PayShap, and debit order, idempotent payment creation, webhook handling, refunds, fees, merchant balances, manual settlements, reconciliation against mock provider reports, and audit logs.

The main focus was not just CRUD. I wanted to model how fintech systems think about trust, payment statuses, duplicate prevention, merchant data isolation, settlement, reconciliation, and proof that a transaction happened.
