import type { FormEvent, ReactNode } from 'react'
import { useEffect, useMemo, useState } from 'react'
import {
  Activity,
  Banknote,
  ClipboardCheck,
  FileText,
  LogOut,
  ReceiptText,
  RefreshCcw,
  RotateCcw,
  ShieldCheck,
  Users,
  Wallet,
} from 'lucide-react'
import './App.css'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

type AuthResponse = {
  accessToken: string
  tokenType: string
  merchantId: string
  merchantUserId: string
  role: string
}

type MerchantProfile = {
  businessName: string
  tradingName: string | null
  merchantType: string
  status: string
  defaultCurrency: string
  userRole: string
}

type Customer = {
  id: string
  fullName: string
  email: string | null
  phoneNumber: string
}

type Invoice = {
  id: string
  customerId: string
  invoiceNumber: string
  description: string | null
  amount: number
  currency: string
  status: string
  dueDate: string | null
}

type Payment = {
  id: string
  invoiceId: string
  amount: number
  grossAmount: number | null
  feeAmount: number | null
  netAmount: number | null
  currency: string
  paymentMethod: string
  status: string
  providerReference: string
  failureReason: string | null
}

type MerchantBalance = {
  currency: string
  grossAmount: number
  feeAmount: number
  refundedAmount: number
  availableAmount: number
  settledAmount: number
}

type Refund = {
  id: string
  paymentId: string
  amount: number
  currency: string
  status: string
  reason: string | null
}

type SettlementBatch = {
  id: string
  status: string
  currency: string
  grossAmount: number
  feeAmount: number
  refundAmount: number
  netAmount: number
  items: unknown[]
}

type ReconciliationReport = {
  id: string
  status: string
  totalRecords: number
  matchedCount: number
  exceptionCount: number
}

type AuditLog = {
  id: string
  action: string
  targetType: string
  previousState: string | null
  newState: string | null
  createdAt: string
}

type DashboardData = {
  profile: MerchantProfile | null
  customers: Customer[]
  invoices: Invoice[]
  payments: Payment[]
  balance: MerchantBalance | null
  refunds: Refund[]
  settlements: SettlementBatch[]
  reports: ReconciliationReport[]
  auditLogs: AuditLog[]
}

const emptyData: DashboardData = {
  profile: null,
  customers: [],
  invoices: [],
  payments: [],
  balance: null,
  refunds: [],
  settlements: [],
  reports: [],
  auditLogs: [],
}

async function apiRequest<T>(path: string, token: string | null, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers)
  headers.set('Accept', 'application/json')

  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(`${API_BASE_URL}${path}`, { ...options, headers })
  const contentType = response.headers.get('content-type') ?? ''
  const payload = contentType.includes('application/json') ? await response.json() : await response.text()

  if (!response.ok) {
    const message = typeof payload === 'string' ? payload : payload.message ?? payload.error ?? 'Request failed'
    throw new Error(message)
  }

  return payload as T
}

function formValue(form: HTMLFormElement, name: string) {
  return String(new FormData(form).get(name) ?? '').trim()
}

function money(value: number | null | undefined, currency = 'ZAR') {
  return new Intl.NumberFormat('en-ZA', {
    style: 'currency',
    currency,
  }).format(Number(value ?? 0))
}

function shortId(id: string) {
  return id.slice(0, 8)
}

const columnLabels: Record<string, string> = {
  action: 'Action',
  amount: 'Amount',
  createdAt: 'Created',
  dueDate: 'Due date',
  email: 'Email',
  exceptionCount: 'Exceptions',
  feeAmount: 'Fee',
  fullName: 'Customer',
  grossAmount: 'Gross',
  invoiceNumber: 'Invoice',
  matchedCount: 'Matched',
  netAmount: 'Net',
  newState: 'New state',
  paymentId: 'Payment',
  phoneNumber: 'Phone',
  previousState: 'Previous state',
  providerReference: 'Provider reference',
  reason: 'Reason',
  refundAmount: 'Refunded',
  status: 'Status',
  targetType: 'Target',
  totalRecords: 'Records',
}

function labelFor(column: string) {
  return columnLabels[column] ?? column
}

function isMoneyColumn(column: string) {
  return ['amount', 'feeAmount', 'grossAmount', 'netAmount', 'refundAmount'].includes(column)
}

function isCountColumn(column: string) {
  return ['totalRecords', 'matchedCount', 'exceptionCount'].includes(column)
}

function isStatusColumn(column: string) {
  return ['status', 'previousState', 'newState'].includes(column)
}

function App() {
  const [auth, setAuth] = useState<AuthResponse | null>(() => {
    const raw = localStorage.getItem('sa-fintech-auth')
    return raw ? JSON.parse(raw) as AuthResponse : null
  })
  const [data, setData] = useState<DashboardData>(emptyData)
  const [authMode, setAuthMode] = useState<'login' | 'register'>('login')
  const [activeView, setActiveView] = useState('overview')
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState('Ready')
  const token = auth?.accessToken ?? null

  const eligibleInvoices = useMemo(
    () => data.invoices.filter((invoice) => invoice.status === 'ISSUED'),
    [data.invoices],
  )
  const refundablePayments = useMemo(
    () => data.payments.filter((payment) => ['SUCCEEDED', 'PARTIALLY_REFUNDED'].includes(payment.status)),
    [data.payments],
  )

  async function loadDashboard(nextToken = token) {
    if (!nextToken) {
      setData(emptyData)
      return
    }

    setBusy(true)
    try {
      const [profile, customers, invoices, payments, balance, refunds, settlements, reports, auditLogs] = await Promise.all([
        apiRequest<MerchantProfile>('/api/v1/merchants/me', nextToken),
        apiRequest<Customer[]>('/api/v1/customers', nextToken),
        apiRequest<Invoice[]>('/api/v1/invoices', nextToken),
        apiRequest<Payment[]>('/api/v1/payments', nextToken),
        apiRequest<MerchantBalance>('/api/v1/merchant-balance', nextToken),
        apiRequest<Refund[]>('/api/v1/refunds', nextToken),
        apiRequest<SettlementBatch[]>('/api/v1/settlement-batches', nextToken),
        apiRequest<ReconciliationReport[]>('/api/v1/reconciliation-reports', nextToken),
        apiRequest<AuditLog[]>('/api/v1/audit-logs', nextToken),
      ])
      setData({ profile, customers, invoices, payments, balance, refunds, settlements, reports, auditLogs })
      setMessage('Dashboard refreshed')
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Unable to load dashboard')
    } finally {
      setBusy(false)
    }
  }

  useEffect(() => {
    if (token) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      void loadDashboard(token)
    }
    // The saved token needs one initial fetch after React mounts.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token])

  async function handleLogin(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = event.currentTarget
    setBusy(true)
    try {
      const response = await apiRequest<AuthResponse>('/api/v1/auth/login', null, {
        method: 'POST',
        body: JSON.stringify({
          email: formValue(form, 'email'),
          password: formValue(form, 'password'),
        }),
      })
      localStorage.setItem('sa-fintech-auth', JSON.stringify(response))
      setAuth(response)
      setMessage('Signed in')
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Login failed')
    } finally {
      setBusy(false)
    }
  }

  async function handleRegister(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = event.currentTarget
    setBusy(true)
    try {
      const response = await apiRequest<AuthResponse>('/api/v1/auth/register', null, {
        method: 'POST',
        body: JSON.stringify({
          businessName: formValue(form, 'businessName'),
          tradingName: formValue(form, 'tradingName'),
          merchantType: formValue(form, 'merchantType'),
          ownerFullName: formValue(form, 'ownerFullName'),
          ownerEmail: formValue(form, 'ownerEmail'),
          password: formValue(form, 'password'),
        }),
      })
      localStorage.setItem('sa-fintech-auth', JSON.stringify(response))
      setAuth(response)
      setMessage('Merchant registered')
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Registration failed')
    } finally {
      setBusy(false)
    }
  }

  async function submitJson(event: FormEvent<HTMLFormElement>, path: string, body: Record<string, unknown>, success: string, headers?: HeadersInit) {
    event.preventDefault()
    const form = event.currentTarget
    setBusy(true)
    try {
      await apiRequest(path, token, {
        method: 'POST',
        headers,
        body: JSON.stringify(body),
      })
      form.reset()
      setMessage(success)
      await loadDashboard()
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Request failed')
    } finally {
      setBusy(false)
    }
  }

  async function postJson(path: string, body: Record<string, unknown>, success: string) {
    setBusy(true)
    try {
      await apiRequest(path, token, {
        method: 'POST',
        body: JSON.stringify(body),
      })
      setMessage(success)
      await loadDashboard()
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Request failed')
    } finally {
      setBusy(false)
    }
  }

  function logout() {
    localStorage.removeItem('sa-fintech-auth')
    setAuth(null)
    setData(emptyData)
    setMessage('Signed out')
  }

  if (!auth) {
    return (
      <main className="auth-page">
        <section className="auth-panel">
          <div className="brand-row">
            <div className="brand-mark"><Wallet size={24} /></div>
            <div>
              <h1>SA Fintech Payments</h1>
              <p>Merchant operations dashboard</p>
            </div>
          </div>

          <div className="segmented">
            <button className={authMode === 'login' ? 'active' : ''} onClick={() => setAuthMode('login')}>Login</button>
            <button className={authMode === 'register' ? 'active' : ''} onClick={() => setAuthMode('register')}>Register</button>
          </div>

          {authMode === 'login' ? (
            <form className="form-grid" onSubmit={handleLogin}>
              <label>Email<input name="email" type="email" required placeholder="mpho@example.com" /></label>
              <label>Password<input name="password" type="password" required placeholder="strongPass123" /></label>
              <button className="primary-action" disabled={busy}>Sign In</button>
            </form>
          ) : (
            <form className="form-grid" onSubmit={handleRegister}>
              <label>Business Name<input name="businessName" required placeholder="Mpho Tutoring" /></label>
              <label>Trading Name<input name="tradingName" placeholder="Mpho Maths" /></label>
              <label>Merchant Type<input name="merchantType" required defaultValue="TUTORING_BUSINESS" /></label>
              <label>Owner Name<input name="ownerFullName" required placeholder="Mpho Dlamini" /></label>
              <label>Owner Email<input name="ownerEmail" type="email" required placeholder="mpho@example.com" /></label>
              <label>Password<input name="password" type="password" required minLength={8} placeholder="strongPass123" /></label>
              <button className="primary-action" disabled={busy}>Create Merchant</button>
            </form>
          )}
          <p className="status-line">{message}</p>
        </section>
      </main>
    )
  }

  const navItems = [
    ['overview', Activity],
    ['customers', Users],
    ['invoices', FileText],
    ['payments', ReceiptText],
    ['refunds', RotateCcw],
    ['settlement', Banknote],
    ['reconciliation', ClipboardCheck],
    ['audit', ShieldCheck],
  ] as const

  return (
    <main className="app-shell">
      <aside className="sidebar">
        <div className="brand-row compact">
          <div className="brand-mark"><Wallet size={22} /></div>
          <div>
            <h1>Payments API</h1>
            <p>{data.profile?.businessName ?? 'Merchant dashboard'}</p>
          </div>
        </div>
        <nav>
          {navItems.map(([item, Icon]) => (
            <button key={item} className={activeView === item ? 'active' : ''} onClick={() => setActiveView(item)}>
              <Icon size={17} />
              <span>{item}</span>
            </button>
          ))}
        </nav>
      </aside>

      <section className="workspace">
        <header className="topbar">
          <div>
            <p className="eyebrow">Simulation only</p>
            <h2>{data.profile?.tradingName || data.profile?.businessName || 'Merchant operations'}</h2>
          </div>
          <div className="toolbar">
            <button className="icon-button" title="Refresh" onClick={() => loadDashboard()} disabled={busy}><RefreshCcw size={18} /></button>
            <button className="icon-button" title="Sign out" onClick={logout}><LogOut size={18} /></button>
          </div>
        </header>

        <p className="status-line">{busy ? 'Working...' : message}</p>

        {activeView === 'overview' && (
          <div className="view-stack">
            <section className="insight-strip">
              <Insight label="Customers" value={data.customers.length} />
              <Insight label="Invoices" value={data.invoices.length} />
              <Insight label="Payments" value={data.payments.length} />
              <Insight label="Reports" value={data.reports.length} />
            </section>
            <section className="metric-grid">
              <Metric label="Available" value={money(data.balance?.availableAmount, data.balance?.currency)} />
              <Metric label="Gross" value={money(data.balance?.grossAmount, data.balance?.currency)} />
              <Metric label="Fees" value={money(data.balance?.feeAmount, data.balance?.currency)} />
              <Metric label="Settled" value={money(data.balance?.settledAmount, data.balance?.currency)} />
            </section>
            <section className="split-grid">
              <DataTable title="Recent Payments" rows={data.payments.slice(0, 6)} columns={['providerReference', 'status', 'amount']} emptyText="Create an invoice payment to see it here." />
              <DataTable title="Reconciliation" rows={data.reports.slice(0, 6)} columns={['status', 'totalRecords', 'exceptionCount']} emptyText="Run a mock provider report to check payment records." />
            </section>
          </div>
        )}

        {activeView === 'customers' && (
          <CrudView
            title="Customers"
            description="Create merchant-owned customers before raising invoices."
            form={(
              <form className="inline-form" onSubmit={(event) => submitJson(event, '/api/v1/customers', {
                fullName: formValue(event.currentTarget, 'fullName'),
                email: formValue(event.currentTarget, 'email'),
                phoneNumber: formValue(event.currentTarget, 'phoneNumber'),
              }, 'Customer created')}>
                <input name="fullName" placeholder="Full name" required />
                <input name="email" type="email" placeholder="email@example.com" />
                <input name="phoneNumber" placeholder="+27821234567" required />
                <button>Create</button>
              </form>
            )}
            table={<DataTable rows={data.customers} columns={['fullName', 'email', 'phoneNumber']} emptyText="No customers yet. Add one to start the payment flow." />}
          />
        )}

        {activeView === 'invoices' && (
          <CrudView
            title="Invoices"
            description="Invoices define the amount owed. Payments are created against issued invoices."
            form={(
              <form className="inline-form" onSubmit={(event) => submitJson(event, '/api/v1/invoices', {
                customerId: formValue(event.currentTarget, 'customerId'),
                description: formValue(event.currentTarget, 'description'),
                amount: Number(formValue(event.currentTarget, 'amount')),
                dueDate: formValue(event.currentTarget, 'dueDate') || null,
              }, 'Invoice created')}>
                <select name="customerId" required>
                  <option value="">Customer</option>
                  {data.customers.map((customer) => <option key={customer.id} value={customer.id}>{customer.fullName}</option>)}
                </select>
                <input name="description" placeholder="Description" />
                <input name="amount" type="number" step="0.01" min="0.01" placeholder="250.00" required />
                <input name="dueDate" type="date" />
                <button>Create</button>
              </form>
            )}
            table={<DataTable rows={data.invoices} columns={['invoiceNumber', 'status', 'amount', 'dueDate']} emptyText="No invoices yet. Create one for an existing customer." />}
          />
        )}

        {activeView === 'payments' && (
          <CrudView
            title="Payments"
            description="Create simulated payment attempts, then move them through controlled statuses."
            form={(
              <>
                <form className="inline-form" onSubmit={(event) => submitJson(event, '/api/v1/payments', {
                  invoiceId: formValue(event.currentTarget, 'invoiceId'),
                  paymentMethod: formValue(event.currentTarget, 'paymentMethod'),
                }, 'Payment attempt created', { 'Idempotency-Key': formValue(event.currentTarget, 'idempotencyKey') })}>
                  <select name="invoiceId" required>
                    <option value="">Issued invoice</option>
                    {eligibleInvoices.map((invoice) => <option key={invoice.id} value={invoice.id}>{invoice.invoiceNumber}</option>)}
                  </select>
                  <input name="paymentMethod" defaultValue="PAYSHAP_SIMULATED" required />
                  <input name="idempotencyKey" placeholder="payment-create-001" required />
                  <button>Create</button>
                </form>
                <form className="inline-form" onSubmit={(event) => submitJson(event, `/api/v1/payments/${formValue(event.currentTarget, 'paymentId')}/status`, {
                  status: formValue(event.currentTarget, 'status'),
                  failureReason: formValue(event.currentTarget, 'failureReason') || null,
                }, 'Payment status updated')}>
                  <select name="paymentId" required>
                    <option value="">Payment</option>
                    {data.payments.map((payment) => <option key={payment.id} value={payment.id}>{payment.providerReference} - {payment.status}</option>)}
                  </select>
                  <select name="status" required>
                    <option>PROCESSING</option>
                    <option>SUCCEEDED</option>
                    <option>FAILED</option>
                  </select>
                  <input name="failureReason" placeholder="Failure reason" />
                  <button>Update</button>
                </form>
              </>
            )}
            table={<DataTable rows={data.payments} columns={['providerReference', 'status', 'amount', 'feeAmount', 'netAmount']} emptyText="No payments yet. Create one from an issued invoice." />}
          />
        )}

        {activeView === 'refunds' && (
          <CrudView
            title="Refunds"
            description="Refund successful payments while keeping the original payment record intact."
            form={(
              <form className="inline-form" onSubmit={(event) => submitJson(event, '/api/v1/refunds', {
                paymentId: formValue(event.currentTarget, 'paymentId'),
                amount: Number(formValue(event.currentTarget, 'amount')),
                reason: formValue(event.currentTarget, 'reason'),
              }, 'Refund created')}>
                <select name="paymentId" required>
                  <option value="">Successful payment</option>
                  {refundablePayments.map((payment) => <option key={payment.id} value={payment.id}>{payment.providerReference}</option>)}
                </select>
                <input name="amount" type="number" step="0.01" min="0.01" placeholder="50.00" required />
                <input name="reason" placeholder="Customer returned item" />
                <button>Create</button>
              </form>
            )}
            table={<DataTable rows={data.refunds} columns={['paymentId', 'status', 'amount', 'reason']} emptyText="No refunds yet. Refund a successful payment to test limits." />}
          />
        )}

        {activeView === 'settlement' && (
          <CrudView
            title="Settlement"
            description="Move eligible successful payment net amounts from available balance into settled balance."
            form={<button className="primary-action fit" onClick={() => postJson('/api/v1/settlement-batches', {}, 'Settlement batch created')}>Create Settlement Batch</button>}
            table={<DataTable rows={data.settlements} columns={['status', 'grossAmount', 'refundAmount', 'netAmount']} emptyText="No settlement batches yet. Successful or partially refunded payments are eligible." />}
          />
        )}

        {activeView === 'reconciliation' && (
          <CrudView
            title="Reconciliation"
            description="Compare internal payments with a mock provider report without changing payment records."
            form={(
              <form className="inline-form" onSubmit={(event) => submitJson(event, '/api/v1/reconciliation-reports', {
                records: [{
                  providerReference: formValue(event.currentTarget, 'providerReference'),
                  amount: Number(formValue(event.currentTarget, 'amount')),
                  currency: 'ZAR',
                  status: formValue(event.currentTarget, 'status'),
                }],
              }, 'Reconciliation report created')}>
                <input name="providerReference" placeholder="SIM-PAY-123" required />
                <input name="amount" type="number" step="0.01" min="0.01" placeholder="250.00" required />
                <select name="status" required>
                  <option>SUCCEEDED</option>
                  <option>FAILED</option>
                  <option>PROCESSING</option>
                </select>
                <button>Create</button>
              </form>
            )}
            table={<DataTable rows={data.reports} columns={['status', 'totalRecords', 'matchedCount', 'exceptionCount']} emptyText="No reports yet. Submit a provider reference to compare records." />}
          />
        )}

        {activeView === 'audit' && (
          <CrudView
            title="Audit Logs"
            description="Trace important financial and security-sensitive actions for this merchant."
            table={<DataTable rows={data.auditLogs} columns={['action', 'targetType', 'previousState', 'newState', 'createdAt']} emptyText="No audit logs yet. Financial actions will appear here." />}
          />
        )}
      </section>
    </main>
  )
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <article className="metric">
      <p>{label}</p>
      <strong>{value}</strong>
    </article>
  )
}

function Insight({ label, value }: { label: string; value: number }) {
  return (
    <article className="insight">
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  )
}

function CrudView({ title, description, form, table }: { title: string; description?: string; form?: ReactNode; table: ReactNode }) {
  return (
    <section className="view-stack">
      <div className="section-heading">
        <h3>{title}</h3>
        {description && <p>{description}</p>}
      </div>
      {form && <div className="tool-panel">{form}</div>}
      {table}
    </section>
  )
}

function DataTable<T extends Record<string, unknown>>({
  title,
  rows,
  columns,
  emptyText = 'No records yet',
}: {
  title?: string
  rows: T[]
  columns: string[]
  emptyText?: string
}) {
  return (
    <section className="data-table">
      {title && <h3>{title}</h3>}
      <div className="table-scroll">
        <table>
          <thead>
            <tr>{columns.map((column) => <th key={column}>{labelFor(column)}</th>)}</tr>
          </thead>
          <tbody>
            {rows.length === 0 ? (
              <tr><td className="empty-cell" colSpan={columns.length}>{emptyText}</td></tr>
            ) : rows.map((row, index) => (
              <tr key={String(row.id ?? index)}>
                {columns.map((column) => <td key={column}>{formatCell(row[column], column, row)}</td>)}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}

function StatusBadge({ value }: { value: string }) {
  const className = `status-badge ${value.toLowerCase().replaceAll('_', '-')}`
  return <span className={className}>{value.replaceAll('_', ' ')}</span>
}

function formatCell(value: unknown, column: string, row: Record<string, unknown>) {
  if (value === null || value === undefined || value === '') return '-'
  if (isStatusColumn(column)) return <StatusBadge value={String(value)} />
  if (typeof value === 'number' && isMoneyColumn(column)) return money(value, String(row.currency ?? 'ZAR'))
  if (typeof value === 'number' && isCountColumn(column)) return String(value)
  if (typeof value === 'number') return value.toFixed(2)
  if (column === 'createdAt' && typeof value === 'string') return new Date(value).toLocaleString('en-ZA')
  if (column === 'paymentId' && typeof value === 'string') return <code className="inline-code">{shortId(value)}</code>
  if (column === 'providerReference' && typeof value === 'string') return <code className="provider-reference">{value}</code>
  if (typeof value === 'string' && value.length > 24 && /^[0-9a-f-]{36}$/i.test(value)) return shortId(value)
  return String(value)
}

export default App
