# Interview Notes

## Project Story

`sa-fintech-payments-api` is a simulated South African merchant payments and reconciliation API. It models how a fintech backend handles merchant onboarding, invoices, simulated payments, webhooks, refunds, fees, balances, settlements, reconciliation, and audit logs.

The project does not process real money. It is designed to show backend engineering and fintech domain understanding.

## What This Project Proves

### Backend API Design

This project demonstrates structured API design for a multi-tenant financial system. It separates merchants, customers, invoices, payments, refunds, settlements, reconciliation, and audit logs into clear domain areas.

### Fintech Domain Understanding

The project shows understanding that financial systems are not simple CRUD systems. They require controlled state, traceability, duplicate prevention, reconciliation, and strong access control.

### Payment Lifecycle Thinking

The project models payment attempts and payment statuses. It shows why failed attempts are preserved, why successful payment is a controlled state change, and why an invoice should not be paid twice.

### Settlement Vs Payment

The project separates customer payment from merchant settlement.

A payment means the customer has paid. A settlement means funds have been batched and paid out to the merchant in the simulation.

### Reconciliation

The project compares internal records against mock external provider reports. It detects missing records, amount mismatches, status mismatches, and duplicate provider references.

This proves understanding that fintech systems must verify records against external sources of truth.

### Idempotency

The project uses idempotency keys to prevent duplicate payment processing when requests are retried.

This demonstrates distributed-systems thinking and financial safety.

### Audit Logs

The project records important user, system, webhook, and financial actions.

Audit logs help answer who did what, when it happened, what changed, and which merchant was affected.

### Security

The project enforces merchant data isolation. A merchant must never access another merchant's financial data.

This demonstrates understanding of multi-tenant security in financial systems.

### Testing Financial Systems

The project includes tests for:

- Edge cases
- Merchant isolation
- Duplicate payment prevention
- Invalid status transitions
- Refund limits
- Settlement calculations
- Reconciliation mismatches

This shows that tests protect business rules, not only code paths.

### AI-Native Development Workflow

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

## Milestone 2 Merchant Identity

The merchant identity slice introduces the first real multi-tenant fintech boundary.

It proves:

- Merchant registration creates both a business record and an owner user
- Passwords are hashed instead of stored directly
- Duplicate owner emails are rejected
- Login returns a JWT
- The JWT carries merchant and merchant-user identity
- Protected merchant endpoints require authentication

This matters because future invoices, payments, refunds, settlements, reconciliation reports, and audit logs must all be tied back to the correct merchant.

## Milestone 3 Customers And Invoices

The customer and invoice slice introduces the first merchant-owned business records.

It proves:

- Customer data is scoped to the authenticated merchant
- South African phone numbers use `+27` format
- Invoices are requests for payment, not payments themselves
- Invoice money is ZAR-only and stored with `BigDecimal` plus PostgreSQL `NUMERIC(19,2)`
- Unsafe money scale is rejected instead of silently rounded
- A merchant cannot create an invoice for another merchant's customer
- Only issued invoices can be cancelled

This matters because payments in the next milestone need a trustworthy invoice foundation before payment attempts, provider references, idempotency, webhooks, fees, balances, settlement, and reconciliation are added.

## Milestone 4 Payment Attempts And Statuses

The payment slice introduces controlled financial state changes.

It proves:

- Payment attempts are separate from invoices
- Creating a payment attempt does not mean the invoice is paid
- Payment amounts are copied from the invoice instead of trusted from the client
- Simulated provider references are generated internally
- Status transitions are centralized and tested
- A successful payment marks the invoice `PAID`
- The database prevents two successful full payments for one invoice
- Payment creation and status changes create audit records

This matters because fintech systems need evidence and control around every state change. The next milestone will add idempotency keys and webhook simulation so retries and provider events can be handled safely.

## Milestone 5 Webhook Simulation And Idempotency

The webhook and idempotency slice introduces retry safety and provider-event safety.

It proves:

- Payment creation can be retried with an idempotency key
- Idempotency keys are scoped by merchant and operation
- The same idempotency key with a different request is rejected
- Simulated provider webhook events are stored
- Duplicate provider event IDs do not process financial actions twice
- Late or backward webhook status changes are ignored
- Webhook decisions create audit records where merchant context is known

This matters because real payment systems receive repeated client requests and repeated or out-of-order provider events. Financial APIs need durable records of both the original action and the decision made during retries.

## Milestone 6 Refunds

The refund slice introduces reversal behavior without pretending that a refund deletes the original payment.

It proves:

- Refunds are separate records linked to original payments
- Only successful payments with remaining refundable amount can be refunded
- Multiple partial refunds are capped at the original payment amount
- Full refunds move payment and invoice state to `REFUNDED`
- Partial refunds move payment and invoice state to `PARTIALLY_REFUNDED`
- Refunded payments still count as the successful payment for duplicate-payment prevention
- Refund actions are audited

This matters because financial systems preserve the original payment and record compensating actions. Refunds must be traceable, bounded by the original amount, and reflected in later balance and settlement calculations.

## Milestone 7 Fees And Merchant Balances

The balance slice introduces basic payment economics.

It proves:

- Successful payments store gross, fee, and net amounts separately
- Fees are calculated deterministically with documented rounding
- Merchant balances are updated when payments succeed
- Refunds reduce available balance and increase refunded totals
- The model stays simple enough to explain before introducing settlement or a full ledger

This matters because a payment platform needs to explain how much the customer paid, how much the platform kept as fees, how much is available to the merchant, and how refunds change those totals.

## Milestone 8 Manual Settlement Batches

The settlement slice introduces payout-style batching without moving real money.

It proves:

- Settlement is separate from payment success
- Only eligible merchant-owned payments are included
- Fully refunded payments are excluded
- Partial refunds reduce settlement net amount
- A payment cannot be settled twice
- Settlement totals are preserved at batch creation time

This matters because fintech systems need to show what funds are ready to pay out, why each payment was included, and how gross, fees, refunds, and net payout amounts were calculated.

## Milestone 9 Reconciliation Using Mock Provider Reports

The reconciliation slice introduces internal-versus-provider comparison.

It proves:

- Matching uses stable provider references
- Missing internal records are detected
- Missing external records are detected
- Amount and status mismatches are detected
- Duplicate provider references are detected
- Reconciliation reports issues without mutating payment state
- Reconciliation exceptions are audited

This matters because fintech systems need evidence that internal records agree with external provider records. Reconciliation is an investigation workflow, not an automatic rewrite of financial history.

## Milestone 10 Audit, Security Hardening, And Portfolio Polish

The final slice makes the project easier to inspect and explain.

It proves:

- Audit logs can be queried by the authenticated merchant
- Audit records are structured around actions and state changes
- Sensitive values are not written through the audit service surface
- Protected endpoints require authentication
- The README and notes explain the project as a fintech backend portfolio piece

This matters because interviewers should be able to see not only that the workflows work, but also why the project was designed around traceability, merchant isolation, duplicate prevention, and safe simulation.

## Example Interview Pitch

I built a simulated South African fintech merchant payments API. It supports ZAR invoices, simulated payment methods like card, EFT, PayShap, and debit order, idempotent payment creation, webhook handling, refunds, fees, merchant balances, manual settlements, reconciliation against mock provider reports, and audit logs.

The main focus was not just CRUD. I wanted to model how fintech systems think about trust, payment statuses, duplicate prevention, merchant data isolation, settlement, reconciliation, and proof that a transaction happened.
