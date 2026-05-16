# Roadmap

## Build Philosophy

Build this project milestone by milestone. Each milestone should be small enough to finish, test, document, and explain.

Each milestone must include:

- Working code
- Tests
- API examples
- Short interview notes
- Explanation of the fintech concept learned
- Edge cases covered
- What to improve in the next milestone

Milestones that introduce financial behavior must not be marked complete until money handling, merchant isolation, idempotency or duplicate-prevention behavior, auditability, and important edge cases for that milestone are tested.

## Milestone 1: Project Foundation And Domain Design

Goal: define the project guidance, domain model, architecture, testing approach, and roadmap before writing application code.

Expected outputs:

- `AGENTS.md`
- Project context documentation
- Architecture documentation
- Domain model documentation
- Roadmap
- Testing strategy
- Codex workflow guide
- Interview notes
- Decision log

Fintech concepts:

- Payment systems are stateful
- Financial actions require traceability
- Safe simulations can model real fintech thinking without moving money

## Milestone 2: Merchant Registration, Login, And Authentication

Goal: allow a merchant owner to register, log in, and access protected merchant-scoped endpoints.

Features:

- Merchant registration
- Merchant owner creation
- Login
- JWT authentication
- Protected endpoints
- Basic merchant profile

Edge cases:

- Invalid login
- Unauthenticated access
- Cross-merchant access prevention once merchant-owned records exist

## Milestone 3: Customers And Invoices

Goal: allow authenticated merchant owners to create customers and issue full-payment invoices.

Features:

- Create customer
- List customers
- Create invoice
- List invoices
- View invoice
- Cancel invoice
- Use ZAR
- Store money safely

Edge cases:

- Invalid invoice amount
- Invalid customer phone number
- Cancel paid invoice rejected
- Merchant cannot access another merchant's customer or invoice

## Milestone 4: Payment Attempts And Payment Statuses

Goal: create simulated payment attempts and control payment status changes.

Features:

- Create payment attempt
- Support simulated payment methods
- Track provider reference
- Update payment status through controlled rules
- Mark invoice paid after successful full payment
- Centralize payment status transition rules
- Create audit records for important payment status changes

Edge cases:

- Unsupported payment method
- Payment against cancelled invoice
- Duplicate successful payment for same invoice
- Invalid payment status transition

## Milestone 5: Webhook Simulation And Idempotency

Goal: process simulated provider webhooks safely and prevent duplicate payment creation.

Features:

- Internal webhook simulation endpoint
- Webhook event storage
- Duplicate webhook detection
- Idempotency key support
- Out-of-order event handling
- Late webhook handling rule
- Request fingerprint checking for idempotency-key reuse
- Database-backed uniqueness for idempotency and webhook event processing

Edge cases:

- Same idempotency key used twice
- Duplicate webhook
- Invalid webhook secret
- Failed webhook after successful payment
- Processing webhook after success

## Milestone 6: Refunds

Goal: allow merchant owners to refund successful payments.

Features:

- Full refunds
- Partial refunds
- Refund status tracking
- Link refunds to original payment
- Update payment and invoice refund state

Edge cases:

- Refund failed payment rejected
- Over-refund rejected
- Multiple partial refunds capped at paid amount
- Refund status transitions controlled

## Milestone 7: Fees And Merchant Balances

Goal: introduce simple fee calculation and merchant balance tracking.

Features:

- Apply platform fee on successful payment
- Store gross, fee, and net amounts
- Maintain simple merchant balance
- Update balance after refunds

Edge cases:

- Fee rounding
- Refund reduces balance
- Balance summary matches payment and refund history
- No `float` or `double` money calculations

## Milestone 8: Manual Settlement Batches

Goal: manually settle eligible merchant funds.

Features:

- Create settlement batch through endpoint
- Include eligible payments
- Exclude already settled payments
- Exclude fully refunded payments
- Calculate gross, fee, refund, and net totals
- Preserve settlement batch totals after creation
- Record why payments were included or excluded where useful

Edge cases:

- Payment settled twice rejected
- Merchant cannot settle another merchant's payments
- Settlement totals reflect refunds and fees

## Milestone 9: Reconciliation Using Mock Provider Reports

Goal: compare internal records against generated mock provider reports.

Features:

- Generate mock provider report
- Match records by provider reference
- Detect reconciliation exceptions
- View reconciliation report
- Avoid silently mutating payment records during reconciliation
- Audit reconciliation mismatch detection

Edge cases:

- Missing internal record
- Missing external record
- Amount mismatch
- Status mismatch
- Duplicate provider reference

## Milestone 10: Audit Logs, Security Hardening, Documentation, And Portfolio README

Goal: polish the project into a recruiter-friendly fintech backend portfolio project.

Features:

- Audit logs for important financial actions
- Security-focused tests
- Strong API documentation
- Recruiter-friendly README
- Interview notes per milestone
- Known limitations and future improvements

Edge cases:

- Audit log created for payment success
- Audit log created for refund
- Audit log created for webhook
- Audit log created for settlement
- Cross-merchant access rejected
- Invalid state transitions rejected
