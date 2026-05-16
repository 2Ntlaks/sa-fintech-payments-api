# Merchant Dashboard Frontend

React + TypeScript + Vite dashboard for the `sa-fintech-payments-api` learning lab.

The dashboard is intentionally operational rather than marketing-focused. It lets you exercise the backend workflow from merchant registration through customers, invoices, payments, refunds, settlement, reconciliation, and audit logs.

## Run Locally

Start the backend first from the repository root:

```powershell
docker compose up -d postgres
.\mvnw.cmd spring-boot:run
```

Then start the frontend:

```powershell
cd frontend
npm ci
npm run dev
```

Open:

```text
http://localhost:5173
```

## Configuration

The frontend calls the backend at `http://localhost:8080` by default.

To override it:

```powershell
$env:VITE_API_BASE_URL="http://localhost:8080"
npm run dev
```

The dashboard stores the demo JWT in browser `localStorage` so the local demo survives refreshes. This is not production-grade browser auth storage.

## Checks

```powershell
npm run lint
npm run build
```
