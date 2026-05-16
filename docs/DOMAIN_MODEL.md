# Domain Model

## Merchant

A merchant is a business using the platform to accept simulated payments.

Examples:

- Spaza shop
- Tutoring business
- Online course seller
- Small retailer

Merchants own customers, invoices, payments, refunds, balances, settlements, reconciliation reports, and audit logs.

The database stores merchants with UUID primary keys, South African simulation defaults, a merchant type, and a lifecycle status. Version one can activate merchants directly, while leaving room for verification and suspension workflows later.

Merchant types:

- `SPAZA_SHOP`
- `TUTORING_BUSINESS`
- `ONLINE_COURSE_SELLER`
- `SMALL_RETAILER`
- `OTHER`

Merchant statuses:

- `PENDING_VERIFICATION`
- `ACTIVE`
- `SUSPENDED`
- `CLOSED`

## Merchant User

A merchant user is a person who can log in for a merchant.

Version one starts with one owner account, but the database supports future multi-user access.

Merchant user roles:

- `OWNER`
- `FINANCE`
- `SUPPORT`
- `VIEWER`

Merchant user statuses:

- `ACTIVE`
- `DISABLED`
- `INVITED`

Version one authentication creates an `OWNER` user during merchant registration. Login returns a JWT that identifies both the merchant user and the merchant. This identity context will later protect customers, invoices, payments, refunds, settlements, reconciliation reports, and audit logs.

## Customer

A customer is a person or business that pays a merchant.

Customer records are merchant-scoped. A customer should never be shared globally between merchants in version one.

South African examples should use phone numbers in `+27` format where appropriate.

Version one requires a customer full name and South African `+27` phone number. Email is optional, but if present it must be unique within the same merchant.

## Invoice

An invoice is a request for payment.

An invoice is not a payment. It represents what the customer owes. Version one supports full invoice payment only.

Version one invoices are merchant-scoped, customer-linked, ZAR-only, and created directly in the `ISSUED` state. Invoice amounts use Java `BigDecimal` and PostgreSQL `NUMERIC(19,2)`. Amounts with more than two decimal places are rejected instead of rounded.

Possible invoice statuses:

- `ISSUED`
- `PAID`
- `EXPIRED`
- `CANCELLED`
- `PARTIALLY_REFUNDED`
- `REFUNDED`

Deferred statuses:

- `DRAFT`
- `PARTIALLY_PAID`

Current version-one invoice rules:

- Only an `ISSUED` invoice can be cancelled.
- A `PAID` invoice cannot be cancelled.
- A merchant cannot create an invoice for another merchant's customer.
- Partial payments are deferred until later.

## Payment

A payment attempt is a customer's attempt to pay an invoice.

One invoice may have multiple failed payment attempts, but version one should prevent multiple successful full payments for the same invoice.

Version one payment attempts are simulated. They do not connect to real payment providers and they do not move real money. The system generates a simulated provider reference internally.

Payment attempts copy the invoice amount and currency at creation time. The API client does not provide the amount, because the invoice is the trusted source of what the customer owes.

Simulated payment methods:

- `CARD_SIMULATED`
- `EFT_SIMULATED`
- `PAYSHAP_SIMULATED`
- `DEBIT_ORDER_SIMULATED`

## Payment Statuses

Payment statuses must be controlled by explicit rules.

Suggested statuses:

- `CREATED`
- `PENDING`
- `PROCESSING`
- `SUCCEEDED`
- `FAILED`
- `CANCELLED`
- `EXPIRED`
- `PARTIALLY_REFUNDED`
- `REFUNDED`

Invalid transitions must be rejected. For example, a `FAILED` payment should not casually become `SUCCEEDED` without a deliberate recovery rule.

Suggested version-one transition rules:

- `CREATED` may move to `PENDING`, `PROCESSING`, `CANCELLED`, or `EXPIRED`
- `PENDING` may move to `PROCESSING`, `SUCCEEDED`, `FAILED`, `CANCELLED`, or `EXPIRED`
- `PROCESSING` may move to `SUCCEEDED`, `FAILED`, or `EXPIRED`
- `SUCCEEDED` may move to `PARTIALLY_REFUNDED` or `REFUNDED`
- `PARTIALLY_REFUNDED` may move to `REFUNDED`
- Terminal states such as `FAILED`, `CANCELLED`, `EXPIRED`, and `REFUNDED` should not move to another state without an explicit documented recovery rule

These rules may evolve, but changes must be documented and tested.

Current version-one payment rules:

- Only issued invoices can receive payment attempts.
- Creating a payment attempt starts it in `CREATED`.
- Payment status changes must use the controlled transition policy.
- Moving a payment to `SUCCEEDED` marks the linked invoice `PAID`.
- A full-payment invoice may have at most one successful payment.
- Payment creation and status changes create audit records.

## Refund

A refund returns part or all of a successful payment.

Refunds must link to the original payment. Refunds cannot exceed the successful paid amount.

Suggested refund statuses:

- `REQUESTED`
- `PROCESSING`
- `SUCCEEDED`
- `FAILED`
- `CANCELLED`

Version one allows merchant owners to issue refunds directly. Approval workflows can be added later.

## Webhook Event

A webhook event is a simulated provider notification.

Webhook events may arrive:

- Late
- Duplicated
- Out of order
- With invalid data

Webhook events must be stored and processed safely. Duplicate events must not trigger duplicate financial actions.

Suggested webhook processing statuses:

- `RECEIVED`
- `PROCESSED`
- `IGNORED_DUPLICATE`
- `IGNORED_OUT_OF_ORDER`
- `FAILED_VALIDATION`
- `FAILED_PROCESSING`

## Idempotency

Idempotency prevents duplicate processing when a request is retried.

Example:

If a merchant sends the same payment creation request twice with the same idempotency key, the system should return the first result instead of creating another payment attempt.

Idempotency is especially important for payment creation and webhook processing.

An idempotency key should be scoped to a merchant and operation. The same key used with the same request should return the same result. The same key used with a different request should be rejected because it may hide a dangerous client bug.

Idempotency must be backed by persistence, not only in-memory state.

## Balance

A merchant balance explains money available or owed to a merchant.

Version one should use a simple balance model, but it should be designed so it can evolve into a ledger-style system later.

Useful balance concepts:

- Pending amount
- Available amount
- Settled amount
- Refunded amount
- Fee amount
- Gross amount
- Net amount

Balance changes must be explainable and tested.

Version one may use a simple balance summary, but every balance-changing event must still be traceable to a payment, refund, fee, or settlement record. Do not create unexplained balance edits.

## Settlement

Settlement is the process of paying merchant funds out.

A successful payment is not the same as a settlement. A customer may have paid, but the merchant may not have received the payout yet.

Version one uses manual settlement through an endpoint.

Suggested settlement statuses:

- `CREATED`
- `PROCESSING`
- `SETTLED`
- `FAILED`
- `CANCELLED`

Settlement batches should include gross amount, fee amount, refund deductions, and net settlement amount.

Settlement eligibility should be explicit. A payment is generally eligible only when it is successful, merchant-owned, not already settled, and not fully refunded. If partial refunds are present, the settlement must use the correct net amount according to the documented rule.

A settlement batch should preserve its calculated totals. Later refunds or changes should not silently rewrite historical settlement totals.

## Reconciliation

Reconciliation compares internal payment records against simulated external provider records.

The system should detect:

- Matched records
- Missing internal records
- Missing external records
- Amount mismatches
- Status mismatches
- Duplicate external references

Suggested reconciliation statuses:

- `PENDING`
- `MATCHED`
- `MISSING_INTERNAL`
- `MISSING_EXTERNAL`
- `AMOUNT_MISMATCH`
- `STATUS_MISMATCH`
- `DUPLICATE_EXTERNAL_REFERENCE`
- `RESOLVED`

Reconciliation should report mismatches for investigation rather than blindly changing financial records.

Reconciliation is evidence gathering. It should show whether the internal system agrees with the simulated provider. A mismatch should create a report item or exception, not automatically rewrite the original payment.

Matching should use provider reference and merchant context first, then compare amount, currency, status, and relevant dates.

## Audit Log

Audit logs record important actions and state changes.

Audit logs should answer:

- What happened?
- Who or what caused it?
- When did it happen?
- Which merchant was affected?
- Which record changed?
- What was the previous state?
- What is the new state?

Important audited actions include merchant registration, login events, invoice creation, payment status changes, webhook processing, refunds, settlements, reconciliation mismatches, and security-sensitive access attempts.

Audit logs should be append-only in normal application behavior. They should avoid sensitive secrets and should include enough context to reconstruct why an important financial decision was made.
