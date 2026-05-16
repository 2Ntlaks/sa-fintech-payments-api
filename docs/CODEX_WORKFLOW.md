# Codex Workflow

## How Codex Should Work In This Project

Codex should treat this repository as a fintech backend learning lab, not a quick CRUD demo.

## Before Coding

Codex must:

1. Inspect existing files.
2. Read `AGENTS.md`.
3. Read the relevant files in `docs/`.
4. Identify the current milestone.
5. Understand the fintech rule being implemented.
6. Check for existing patterns before adding new ones.
7. Identify the money, security, idempotency, audit, and merchant-isolation risks for the change.
8. Decide which tests prove the change is safe before or while implementing it.

## During Implementation

Codex should:

- Make small, reviewable changes.
- Keep code aligned with the modular layered monolith structure.
- Write tests with every feature.
- Prefer clear business rules over clever abstractions.
- Keep merchant data isolation in mind for every endpoint.
- Keep financial status transitions controlled.
- Keep idempotency and retry behavior in mind for payment and webhook workflows.
- Add audit logging for important financial and security-sensitive actions.
- Avoid unrelated refactors.
- Update documentation when behavior changes.

## After Changes

Codex should:

- Run the relevant tests.
- Explain what changed.
- Explain why the change matters for fintech learning.
- Mention edge cases covered.
- Mention any tests that could not be run.
- Update milestone notes when appropriate.
- Call out any remaining fintech risks or deferred edge cases.

## Financial Safety Rules

Codex must never:

- Add real payment processor integrations.
- Add real bank integrations.
- Add production PayShap integrations.
- Add code that moves real money.
- Use `float` or `double` for money.
- Skip edge cases for payment, refund, settlement, reconciliation, or webhook workflows.

## Documentation Rules

Codex should update docs when:

- A new major feature is implemented.
- A domain rule changes.
- A status lifecycle changes.
- A security rule changes.
- A new architectural decision is made.
- A milestone is completed.

## Explanation Style

Codex should explain changes in beginner-friendly language while still using professional backend terminology.

Each milestone explanation should include:

- What was built
- Why it matters
- What fintech concept was learned
- What edge cases were covered
- What comes next
