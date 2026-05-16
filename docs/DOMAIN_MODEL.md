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
- Payment creation may include an idempotency key for safe retries.
- Payment status changes must use the controlled transition policy.
- Moving a payment to `SUCCEEDED` marks the linked invoice `PAID`.
- A full-payment invoice may have at most one successful payment.
- A refunded or partially refunded successful payment still counts as the invoice's successful payment.
- Payment creation and status changes create audit records.
- Successful payments calculate a simulated platform fee and store gross, fee, and net amounts separately.

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

Current refund rules:

- Refunds are simulated and never move real money.
- Refunds are merchant-scoped through the original payment attempt.
- Only `SUCCEEDED` or `PARTIALLY_REFUNDED` payments can be refunded.
- Refund amounts use ZAR `BigDecimal` values with two decimal places.
- Multiple partial refunds are allowed.
- Total successful refund amount cannot exceed the original payment amount.
- Partial refunds update the payment and invoice to `PARTIALLY_REFUNDED`.
- Full refunds update the payment and invoice to `REFUNDED`.
- Failed, cancelled, expired, and already fully refunded payments cannot be refunded.

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

Current webhook rules:

- Simulated payment webhooks are accepted at `POST /api/v1/webhooks/simulated/payments`.
- Webhooks require `X-Simulated-Webhook-Secret`.
- Provider event IDs are unique.
- Duplicate provider event IDs return `IGNORED_DUPLICATE`.
- Webhooks find payments by simulated provider reference.
- Webhooks may move payments only through the normal payment transition rules.
- A late webhook that would move a successful payment backward is stored as `IGNORED_OUT_OF_ORDER`.
- Unknown provider references are stored as `FAILED_VALIDATION`.

## Idempotency

Idempotency prevents duplicate processing when a request is retried.

Example:

If a merchant sends the same payment creation request twice with the same idempotency key, the system should return the first result instead of creating another payment attempt.

Idempotency is especially important for payment creation and webhook processing.

An idempotency key should be scoped to a merchant and operation. The same key used with the same request should return the same result. The same key used with a different request should be rejected because it may hide a dangerous client bug.

Idempotency must be backed by persistence, not only in-memory state.

Current idempotency rules:

- Payment creation accepts `Idempotency-Key`.
- Keys are scoped to merchant plus `CREATE_PAYMENT_ATTEMPT`.
- The stored request hash is based on invoice ID and payment method.
- Same key and same request returns the original payment attempt.
- Same key and different request is rejected and audited.

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

Current balance rules:

- Successful payments use a simulated platform fee of `2.9% + R1.00`.
- Fees are rounded to two decimal places with `HALF_UP`.
- Payment attempts store gross amount, fee amount, and net amount separately.
- Successful payments add gross, fee, and net totals to the merchant balance.
- Successful refunds increase refunded amount and reduce available amount by the refund amount.
- Version one does not automatically reverse platform fees when a refund is created.
- Balance reads are merchant-scoped at `GET /api/v1/merchant-balance`.

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

Current settlement rules:

- Settlement batches are created manually at `POST /api/v1/settlement-batches`.
- Eligible payments must belong to the authenticated merchant.
- Eligible payments must be `SUCCEEDED` or `PARTIALLY_REFUNDED`.
- Fully refunded payments are excluded.
- A payment can appear in only one settlement batch.
- Settlement item net amount is `gross amount - fee amount - successful refund amount`.
- Settlement batch totals are preserved after creation.
- Creating a settlement moves the batch net amount from available balance to settled balance.

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

Current reconciliation rules:

- Reconciliation reports are created at `POST /api/v1/reconciliation-reports`.
- The request contains simulated provider payment records.
- Matching uses provider reference inside the authenticated merchant context.
- Reports store matched records and exceptions.
- Exceptions include missing internal records, missing external records, amount mismatches, status mismatches, and duplicate external provider references.
- Reconciliation does not mutate payment records.
- Reconciliation exceptions are audited.

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

Current audit rules:

- Audit logs are available at `GET /api/v1/audit-logs`.
- Audit reads are scoped to the authenticated merchant.
- Audit records store actor type, actor ID, action, target type, target ID, previous state, new state, merchant ID, and timestamp.
- Audit records do not store passwords, JWTs, webhook secrets, or raw sensitive payloads.
