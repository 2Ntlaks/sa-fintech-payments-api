# sa-fintech-payments-api

A South African fintech backend learning lab for simulated merchant payments, refunds, webhooks, balances, settlements, reconciliation, and audit logs.

This project is a safe simulation. It must never process real money or connect to real banks, card processors, PayShap services, debit order systems, or production payment providers.

## Current Status

Milestone 1 is in progress: project foundation and domain design.

Implemented so far:

- Spring Boot project scaffold
- Maven Wrapper for local builds without system Maven
- Modular package skeleton for the planned domain modules
- Public health endpoint at `GET /api/v1/health`
- Basic stateless Spring Security configuration
- Foundation tests for health and protected endpoint behavior

## Tech Stack

- Java 21 target
- Spring Boot 3.5.x
- Maven
- PostgreSQL later
- Flyway later
- Spring Security
- JWT later
- OpenAPI / Swagger later
- JUnit 5
- Spring Boot Test
- Testcontainers later

## Run Tests

On Windows:

```powershell
.\mvnw.cmd test
```

The current foundation tests do not require Docker. Database integration tests will use PostgreSQL with Testcontainers in later milestones.

## Run Locally Before Docker

Until PostgreSQL and Docker Compose are added, run the foundation app with the `local` profile:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Then open:

```text
http://localhost:8080/api/v1/health
```

## API Example

Health check:

```http
GET /api/v1/health
```

Example response:

```json
{
  "status": "UP",
  "service": "sa-fintech-payments-api",
  "timestamp": "2026-05-16T12:00:00Z"
}
```

## Interview Notes

This project is being built milestone by milestone to show backend API design and fintech domain thinking. The first foundation proves the project can compile, run tests, expose a safe public health endpoint, and enforce protected-by-default API behavior before payment features are added.
