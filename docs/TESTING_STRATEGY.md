# Testing Strategy

## Testing Philosophy

This project must test fintech behavior, not only controller happy paths.

Tests should prove that the system protects money-related workflows, merchant data, status transitions, duplicate prevention, and auditability.

Every milestone must include tests.

A milestone should not be considered complete until the tests for its core business rules and edge cases pass.

## Unit Tests

Use unit tests for small domain rules that do not require Spring or a database.

Examples:

- Payment status transition rules
- Invoice status transition rules
- Refund amount validation
- Fee calculation
- Money rounding rules
- Reconciliation comparison logic

Unit tests should be fast and focused.

## Service Tests

Use service tests for business workflows.

Examples:

- Creating an invoice for a merchant customer
- Creating a payment attempt
- Marking a payment as successful
- Updating invoice state after payment success
- Creating a refund
- Calculating settlement totals

Service tests should verify business rules and side effects.

## Integration Tests

Use integration tests when behavior depends on persistence, transactions, constraints, or security wiring.

Examples:

- Unique idempotency keys
- Unique provider references
- Merchant data isolation queries
- Database constraints
- Authentication-required endpoints
- Transaction rollback behavior
- Audit records created with domain changes
- Webhook event deduplication

## Testcontainers PostgreSQL Tests

Use Testcontainers with PostgreSQL for realistic database integration tests.

This is important because financial systems rely on database behavior such as:

- Transactions
- Unique constraints
- Foreign keys
- Indexes
- Locking or concurrency controls later

Avoid relying only on in-memory database behavior for money-critical workflows.

## Financial Edge Cases

Tests should cover edge cases explicitly.

Required areas:

- Duplicate payment prevention
- Duplicate webhook handling
- Invalid payment status transitions
- Payment after invoice cancellation
- Successful payment after invoice expiry
- Refund on failed payment
- Over-refund prevention
- Multiple partial refunds
- Fee rounding
- Settlement amount calculations
- Reconciliation mismatches
- Idempotency key reused with a different request
- Webhook attempting to move a payment backward
- Cross-merchant access using a valid authenticated user

## Merchant Data Isolation

Merchant isolation must be tested throughout the project.

Examples:

- Merchant A cannot view Merchant B's customers
- Merchant A cannot view Merchant B's invoices
- Merchant A cannot refund Merchant B's payment
- Merchant A cannot settle Merchant B's funds
- Merchant A cannot view Merchant B's reconciliation reports

These tests are security tests, not only business logic tests.

## Duplicate Payment Prevention

Idempotency tests should prove that retries do not create duplicate financial actions.

Examples:

- Same idempotency key returns the existing payment attempt
- Same idempotency key with different request details is rejected
- Different idempotency key may create a new attempt if the invoice is not already paid
- A paid invoice cannot receive another successful full payment
- Concurrent or repeated requests cannot create duplicate successful outcomes

## Invalid Status Transitions

Status transition tests should prove that records cannot move into impossible states.

Examples:

- `FAILED` payment cannot become `PROCESSING` without an explicit rule
- `SUCCEEDED` payment cannot become `PENDING`
- `CANCELLED` invoice cannot become `PAID`
- `REFUNDED` payment cannot be refunded again beyond the original amount

## Refund Limits

Refund tests should prove:

- Only successful payments can be refunded
- Total refund amount cannot exceed paid amount
- Full refund updates payment and invoice status
- Partial refund updates payment and invoice status
- Refund attempts are auditable

## Settlement Calculations

Settlement tests should prove:

- Eligible payments are included
- Already settled payments are excluded
- Fully refunded payments are excluded or netted correctly according to the documented rule
- Gross, fee, refund, and net totals are correct
- Settlement is merchant-scoped
- Settlement batch totals remain stable after creation
- Settlement cannot include payments from another merchant

## Reconciliation Mismatches

Reconciliation tests should prove detection of:

- Matched records
- Missing internal records
- Missing external records
- Amount mismatches
- Status mismatches
- Duplicate provider references

Reconciliation should report mismatches for investigation, not silently mutate financial records.

## Audit Log Tests

Audit log tests should prove:

- Payment status changes create audit records
- Refund attempts and successful refunds create audit records
- Webhook receipt and duplicate webhook decisions create audit records
- Settlement creation creates audit records
- Reconciliation mismatches create audit records
- Sensitive values such as passwords, JWTs, and secrets are not stored in audit logs

## Documentation Checks

When a feature changes API behavior, domain rules, status transitions, or financial calculations, tests should be accompanied by documentation updates.

Documentation does not need to be excessive, but it must keep the project explainable to a recruiter and future Codex sessions.

## Test Naming

Test names should describe the business rule.

Good example:

`shouldRejectOverRefundWhenRefundTotalExceedsPaymentAmount`

Avoid vague names such as:

`testRefund1`
