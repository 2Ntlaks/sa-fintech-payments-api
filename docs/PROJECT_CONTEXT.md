# Project Context

## Project Summary

`sa-fintech-payments-api` is an API-only South African fintech backend learning project. It simulates how a fintech platform could help merchants issue invoices, accept payments, process refunds, track balances, settle funds, receive webhooks, reconcile provider records, and maintain audit logs.

The project is designed for learning, portfolio building, and internship interview preparation.

## Safe Simulation

This project must not process real money.

It must not connect to:

- Real banks
- Real payment processors
- Real card networks
- Real PayShap services
- Real debit order systems
- Production financial APIs

All money movement is simulated. Payment methods, webhook events, settlement batches, provider references, bank account details, and reconciliation reports are fake data used to learn backend fintech concepts safely.

## South African Fintech Context

The system should use realistic South African examples where helpful:

- Currency: ZAR
- Customer phone numbers: `+27` format
- Example merchants: spaza shop, tutoring business, online course seller, small retailer
- Simulated payment methods: card, EFT, PayShap, debit order
- Simulated South African merchant bank accounts

The goal is not to perfectly reproduce South African banking infrastructure. The goal is to build a realistic learning model of how fintech backend systems think about payments, trust, records, statuses, reconciliation, and auditability.

## Problem The Project Solves

Merchants need a way to request and track payments from customers. A fintech platform must record what happened, prevent duplicate processing, keep merchant data isolated, process unreliable webhook events safely, support refunds, calculate fees and balances, settle merchant funds, and reconcile internal records with external provider records.

This project models those responsibilities in a safe backend simulation.

## Learning Goal

The project should help explain:

- Money movement
- Payment lifecycle design
- Invoice lifecycle design
- Merchant access control
- Payment status transitions
- Duplicate payment prevention
- Webhook reliability
- Refund safety
- Settlement vs payment
- Reconciliation
- Audit trails
- Security boundaries
- Financial edge-case testing

## Product Owner Role

The user acts as product owner, learner, tester, reviewer, and explainer.

Codex writes the code and maintains the project with guidance from the product owner. Codex should explain what is being built and why, especially when implementing fintech concepts.
