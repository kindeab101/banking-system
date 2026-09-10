import { FormEvent, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import api from '../../services/api'

type Account = {
  accountNumber: string
  accountType: string
  currency: string
  balance: number
  status: string
  customerName?: string
}

type Txn = {
  reference: string
  sourceAccountNumber?: string
  destinationAccountNumber?: string
  amount: number
  currency: string
  transactionType: string
  status: string
  description?: string
  createdAt: string
}

export function CustomerDashboard() {
  const [data, setData] = useState<any>(null)
  const [error, setError] = useState('')
  useEffect(() => {
    api.get('/api/dashboard').then((r) => setData(r.data)).catch(() => setError('Unable to load dashboard'))
  }, [])
  if (error) return <p className="error">{error}</p>
  if (!data) return <p>Loading dashboard…</p>
  return (
    <>
      <div className="topbar">
        <h1>Good day, {data.fullName}</h1>
        <Link className="btn btn-gold" to="/app/transfer">Transfer money</Link>
      </div>
      <div className="grid">
        <div className="card">
          <p className="muted">Total available</p>
          <h2>{Number(data.totalAvailableBalance).toLocaleString()} {data.currency}</h2>
        </div>
        <div className="card">
          <p className="muted">Accounts</p>
          <h2>{data.accounts?.length || 0}</h2>
        </div>
      </div>
      <div className="card" style={{ marginTop: '1rem' }}>
        <h3>Account summaries</h3>
        {data.accounts?.length ? (
          <table className="table">
            <thead><tr><th>Number</th><th>Type</th><th>Balance</th><th>Status</th></tr></thead>
            <tbody>
              {data.accounts.map((a: Account) => (
                <tr key={a.accountNumber}>
                  <td><Link to={`/app/accounts/${a.accountNumber}`}>{a.accountNumber}</Link></td>
                  <td>{a.accountType}</td>
                  <td>{Number(a.balance).toLocaleString()} {a.currency}</td>
                  <td><span className="badge">{a.status}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : <p className="empty">No accounts yet.</p>}
      </div>
      <div className="card" style={{ marginTop: '1rem' }}>
        <h3>Recent transactions</h3>
        <TxnTable rows={data.recentTransactions || []} />
      </div>
    </>
  )
}

export function ProfilePage() {
  const [form, setForm] = useState({ phone: '', addressLine: '', city: '', firstName: '', email: '', customerNumber: '' })
  const [message, setMessage] = useState('')
  useEffect(() => {
    api.get('/api/me').then((r) => setForm({
      phone: r.data.phone || '',
      addressLine: r.data.addressLine || '',
      city: r.data.city || '',
      firstName: r.data.fullName,
      email: r.data.email,
      customerNumber: r.data.customerNumber
    }))
  }, [])
  async function save(e: FormEvent) {
    e.preventDefault()
    await api.patch('/api/me', { phone: form.phone, addressLine: form.addressLine, city: form.city })
    setMessage('Profile updated.')
  }
  return (
    <form className="card" onSubmit={save} style={{ maxWidth: 560 }}>
      <h1>My profile</h1>
      <p className="muted">Customer {form.customerNumber} · {form.email}</p>
      <label htmlFor="phone">Phone</label>
      <input id="phone" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
      <label htmlFor="address">Address</label>
      <input id="address" value={form.addressLine} onChange={(e) => setForm({ ...form, addressLine: e.target.value })} />
      <label htmlFor="city">City</label>
      <input id="city" value={form.city} onChange={(e) => setForm({ ...form, city: e.target.value })} />
      {message && <p className="ok">{message}</p>}
      <button className="btn btn-primary">Save permitted details</button>
    </form>
  )
}

export function AccountsPage() {
  const [rows, setRows] = useState<Account[]>([])
  useEffect(() => { api.get('/api/accounts').then((r) => setRows(r.data)) }, [])
  return (
    <>
      <h1>My accounts</h1>
      <div className="card">
        <table className="table">
          <thead><tr><th>Account</th><th>Type</th><th>Balance</th><th>Status</th></tr></thead>
          <tbody>
            {rows.map((a) => (
              <tr key={a.accountNumber}>
                <td><Link to={`/app/accounts/${a.accountNumber}`}>{a.accountNumber}</Link></td>
                <td>{a.accountType}</td>
                <td>{Number(a.balance).toLocaleString()} {a.currency}</td>
                <td>{a.status}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  )
}

export function AccountDetailPage() {
  const { accountNumber } = useParams()
  const [account, setAccount] = useState<Account | null>(null)
  const [tx, setTx] = useState<Txn[]>([])
  useEffect(() => {
    api.get(`/api/accounts/${accountNumber}`).then((r) => setAccount(r.data))
    api.get(`/api/accounts/${accountNumber}/transactions`).then((r) => setTx(r.data.content))
  }, [accountNumber])
  if (!account) return <p>Loading account…</p>
  return (
    <>
      <h1>Account {account.accountNumber}</h1>
      <div className="grid">
        <div className="card"><p className="muted">Balance</p><h2>{Number(account.balance).toLocaleString()} {account.currency}</h2></div>
        <div className="card"><p className="muted">Type / status</p><h2>{account.accountType} · {account.status}</h2></div>
      </div>
      <div className="card" style={{ marginTop: '1rem' }}>
        <h3>Transactions</h3>
        <TxnTable rows={tx} />
      </div>
    </>
  )
}

export function TransferPage() {
  const [accounts, setAccounts] = useState<Account[]>([])
  const [step, setStep] = useState<'form' | 'confirm' | 'done'>('form')
  const [form, setForm] = useState({ sourceAccountNumber: '', destinationAccountNumber: '', amount: '', description: '' })
  const [result, setResult] = useState<any>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  useEffect(() => {
    api.get('/api/accounts').then((r) => {
      setAccounts(r.data)
      if (r.data[0]) setForm((f) => ({ ...f, sourceAccountNumber: r.data[0].accountNumber }))
    })
  }, [])

  async function submit() {
    setLoading(true)
    setError('')
    try {
      const { data } = await api.post('/api/transactions/transfer', {
        sourceAccountNumber: form.sourceAccountNumber,
        destinationAccountNumber: form.destinationAccountNumber,
        amount: Number(form.amount),
        description: form.description
      }, { headers: { 'Idempotency-Key': crypto.randomUUID() } })
      setResult(data)
      setStep('done')
    } catch (err: any) {
      setError(err.response?.data?.message || 'Transfer could not be completed')
    } finally {
      setLoading(false)
    }
  }

  if (step === 'done' && result) {
    return (
      <div className="card">
        <h1>Transfer result</h1>
        <p className="ok">{result.message}</p>
        <p>Reference: <strong>{result.transactionReference}</strong></p>
        <p>Status: {result.status} · {Number(result.amount).toLocaleString()} {result.currency}</p>
        <Link to="/app/dashboard">Back to dashboard</Link>
      </div>
    )
  }

  if (step === 'confirm') {
    return (
      <div className="card" style={{ maxWidth: 520 }}>
        <h1>Confirm transfer</h1>
        <p>From {form.sourceAccountNumber} to {form.destinationAccountNumber}</p>
        <p><strong>{form.amount} ETB</strong></p>
        <p>{form.description}</p>
        {error && <p className="error">{error}</p>}
        <div className="row">
          <button className="btn btn-ghost" type="button" onClick={() => setStep('form')}>Edit</button>
          <button className="btn btn-primary" style={{ width: 'auto' }} disabled={loading} onClick={submit}>
            {loading ? 'Processing…' : 'Confirm and send'}
          </button>
        </div>
      </div>
    )
  }

  return (
    <form className="card" style={{ maxWidth: 560 }} onSubmit={(e) => { e.preventDefault(); setError(''); setStep('confirm') }}>
      <h1>Transfer money</h1>
      <p className="alert">Simulated internal transfer in ETB. This does not move real funds.</p>
      <label htmlFor="source">From account</label>
      <select id="source" required value={form.sourceAccountNumber} onChange={(e) => setForm({ ...form, sourceAccountNumber: e.target.value })}>
        {accounts.map((a) => <option key={a.accountNumber} value={a.accountNumber}>{a.accountNumber} ({a.accountType})</option>)}
      </select>
      <label htmlFor="dest">Destination account number</label>
      <input id="dest" required minLength={8} value={form.destinationAccountNumber} onChange={(e) => setForm({ ...form, destinationAccountNumber: e.target.value })} />
      <label htmlFor="amount">Amount (ETB)</label>
      <input id="amount" type="number" min="0.01" step="0.01" required value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} />
      <label htmlFor="desc">Description</label>
      <input id="desc" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
      <button className="btn btn-primary">Review transfer</button>
    </form>
  )
}

export function StatementsPage() {
  const [accounts, setAccounts] = useState<Account[]>([])
  const [accountNumber, setAccountNumber] = useState('')
  const [from, setFrom] = useState('2026-01-01')
  const [to, setTo] = useState('2026-12-31')
  const [statement, setStatement] = useState<any>(null)
  useEffect(() => {
    api.get('/api/accounts').then((r) => {
      setAccounts(r.data)
      if (r.data[0]) setAccountNumber(r.data[0].accountNumber)
    })
  }, [])
  async function load(e: FormEvent) {
    e.preventDefault()
    const { data } = await api.get(`/api/accounts/${accountNumber}/statement`, { params: { from, to } })
    setStatement(data)
  }
  return (
    <>
      <h1>Account statements</h1>
      <form className="card row" onSubmit={load}>
        <div>
          <label htmlFor="acc">Account</label>
          <select id="acc" value={accountNumber} onChange={(e) => setAccountNumber(e.target.value)}>
            {accounts.map((a) => <option key={a.accountNumber}>{a.accountNumber}</option>)}
          </select>
        </div>
        <div>
          <label htmlFor="from">From</label>
          <input id="from" type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
        </div>
        <div>
          <label htmlFor="to">To</label>
          <input id="to" type="date" value={to} onChange={(e) => setTo(e.target.value)} />
        </div>
        <button className="btn btn-primary" style={{ width: 'auto' }}>View</button>
        {accountNumber && (
          <a className="btn btn-ghost" href={`/api/accounts/${accountNumber}/statement.pdf?from=${from}&to=${to}`} onClick={(e) => {
            e.preventDefault()
            api.get(`/api/accounts/${accountNumber}/statement.pdf`, { params: { from, to }, responseType: 'blob' })
              .then((r) => {
                const url = URL.createObjectURL(r.data)
                window.open(url)
              })
          }}>Download PDF</a>
        )}
      </form>
      {statement && (
        <div className="card" style={{ marginTop: '1rem' }}>
          <p>Opening {statement.openingBalance} · Closing {statement.closingBalance} {statement.currency}</p>
          <table className="table">
            <thead><tr><th>Date</th><th>Ref</th><th>Debit</th><th>Credit</th><th>Balance</th></tr></thead>
            <tbody>
              {statement.lines.map((l: any) => (
                <tr key={l.reference + l.date}>
                  <td>{l.date}</td><td>{l.reference}</td>
                  <td>{l.debit}</td><td>{l.credit}</td><td>{l.runningBalance}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  )
}

export function NotificationsPage() {
  const [rows, setRows] = useState<any[]>([])
  useEffect(() => { api.get('/api/notifications').then((r) => setRows(r.data.content)) }, [])
  return (
    <>
      <h1>Notifications</h1>
      <div className="card">
        {rows.length === 0 && <p className="empty">No notifications yet.</p>}
        {rows.map((n) => (
          <p key={n.id}><strong>{n.title}</strong> — {n.message}</p>
        ))}
      </div>
    </>
  )
}

function TxnTable({ rows }: { rows: Txn[] }) {
  if (!rows.length) return <p className="empty">No transactions in this view.</p>
  return (
    <table className="table">
      <thead><tr><th>Reference</th><th>Type</th><th>Amount</th><th>Status</th><th>When</th></tr></thead>
      <tbody>
        {rows.map((t) => (
          <tr key={t.reference}>
            <td><Link to={`/app/transactions/${t.reference}`}>{t.reference}</Link></td>
            <td>{t.transactionType}</td>
            <td>{Number(t.amount).toLocaleString()} {t.currency}</td>
            <td>{t.status}</td>
            <td>{new Date(t.createdAt).toLocaleString()}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

export function TransactionDetailPage() {
  const { reference } = useParams()
  const [t, setT] = useState<Txn | null>(null)
  useEffect(() => { api.get(`/api/transactions/${reference}`).then((r) => setT(r.data)) }, [reference])
  if (!t) return <p>Loading…</p>
  return (
    <div className="card">
      <h1>{t.reference}</h1>
      <p>{t.transactionType} · {t.status}</p>
      <p>{Number(t.amount).toLocaleString()} {t.currency}</p>
      <p>From {t.sourceAccountNumber || '—'} to {t.destinationAccountNumber || '—'}</p>
      <p>{t.description}</p>
    </div>
  )
}
