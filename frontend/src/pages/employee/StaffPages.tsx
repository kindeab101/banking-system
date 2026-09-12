import { FormEvent, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import api from '../../services/api'

export function StaffDashboard() {
  const [data, setData] = useState<any>(null)
  useEffect(() => { api.get('/api/staff/dashboard').then((r) => setData(r.data)) }, [])
  if (!data) return <p>Loading operations dashboard…</p>
  return (
    <>
      <h1>Operations dashboard</h1>
      <div className="grid">
        <Metric label="Customers" value={data.totalCustomers} />
        <Metric label="Active accounts" value={data.activeAccounts} />
        <Metric label="Transactions today" value={data.transactionsToday} />
        <Metric label="Failed transactions" value={data.failedTransactions} />
      </div>
    </>
  )
}

function Metric({ label, value }: { label: string, value: number }) {
  return <div className="card"><p className="muted">{label}</p><h2>{value}</h2></div>
}

export function CustomerManagePage() {
  const [q, setQ] = useState('')
  const [rows, setRows] = useState<any[]>([])
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [form, setForm] = useState({ firstName: '', lastName: '', email: '', username: '', temporaryPassword: 'DemoTemp#2026x' })
  async function search(e?: FormEvent) {
    e?.preventDefault()
    const { data } = await api.get('/api/staff/customers', { params: { q } })
    setRows(data.content)
  }
  useEffect(() => { search() }, [])
  async function create(e: FormEvent) {
    e.preventDefault()
    setError('')
    setMessage('')
    try {
      const { data } = await api.post('/api/staff/customers', form)
      setMessage(`Customer ${data.username} can sign in now. Account ${data.primaryAccountNumber} is ready.`)
      setForm({ firstName: '', lastName: '', email: '', username: '', temporaryPassword: 'DemoTemp#2026x' })
      search()
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Could not create customer')
    }
  }
  return (
    <>
      <h1>Customer management</h1>
      <form className="card row" onSubmit={search}>
        <div style={{ flex: 1 }}>
          <label htmlFor="q">Search</label>
          <input id="q" value={q} onChange={(e) => setQ(e.target.value)} />
        </div>
        <button className="btn btn-primary" style={{ width: 'auto' }}>Search</button>
      </form>
      <div className="card" style={{ marginTop: '1rem' }}>
        <table className="table">
          <thead><tr><th>Number</th><th>Name</th><th>Email</th><th>Status</th></tr></thead>
          <tbody>
            {rows.map((c) => (
              <tr key={c.id}><td><Link to={`/staff/customers/${c.id}`}>{c.customerNumber}</Link></td><td>{c.firstName} {c.lastName}</td><td>{c.email}</td><td>{c.status}</td></tr>
            ))}
          </tbody>
        </table>
      </div>
      <form className="card" style={{ marginTop: '1rem', maxWidth: 560 }} onSubmit={create}>
        <h3>Create fictional customer</h3>
        <label>First name</label><input required value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })} />
        <label>Last name</label><input required value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })} />
        <label>Email</label><input type="email" required value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
        <label>Username</label><input required value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} />
        <label>Password</label>
        <input required minLength={10} value={form.temporaryPassword} onChange={(e) => setForm({ ...form, temporaryPassword: e.target.value })} />
        <p className="muted">At least 10 characters. The customer signs in with this username and password immediately. A savings account is opened at the same time.</p>
        {error && <p className="error" role="alert">{error}</p>}
        {message && <p className="ok">{message}</p>}
        <button className="btn btn-primary">Create customer</button>
      </form>
    </>
  )
}

export function CustomerDetailPage() {
  const { id } = useParams()
  const [c, setC] = useState<any>(null)
  const [accounts, setAccounts] = useState<any[]>([])
  const [status, setStatus] = useState('ACTIVE')
  useEffect(() => {
    api.get(`/api/staff/customers/${id}`).then((r) => { setC(r.data); setStatus(r.data.status) })
    api.get(`/api/staff/customers/${id}/accounts`).then((r) => setAccounts(r.data))
  }, [id])
  if (!c) return <p>Loading…</p>
  return (
    <>
      <h1>{c.firstName} {c.lastName}</h1>
      <div className="card">
        <p>{c.customerNumber} · {c.email} · {c.status}</p>
        <div className="row">
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option>ACTIVE</option><option>INACTIVE</option>
          </select>
          <button className="btn btn-primary" style={{ width: 'auto' }} onClick={() => api.patch(`/api/staff/customers/${id}`, { status }).then(() => location.reload())}>Update status</button>
        </div>
      </div>
      <div className="card" style={{ marginTop: '1rem' }}>
        <h3>Accounts</h3>
        <table className="table">
          <thead><tr><th>Number</th><th>Type</th><th>Balance</th><th>Status</th></tr></thead>
          <tbody>{accounts.map((a) => <tr key={a.accountNumber}><td>{a.accountNumber}</td><td>{a.accountType}</td><td>{a.balance}</td><td>{a.status}</td></tr>)}</tbody>
        </table>
      </div>
    </>
  )
}

export function StaffAccountsPage() {
  const [rows, setRows] = useState<any[]>([])
  const [form, setForm] = useState({ customerNumber: 'CUS-000001', accountType: 'SAVINGS', openingBalance: '0' })
  useEffect(() => { api.get('/api/staff/accounts').then((r) => setRows(r.data.content)) }, [])
  async function create(e: FormEvent) {
    e.preventDefault()
    await api.post('/api/staff/accounts', { ...form, openingBalance: Number(form.openingBalance) })
    const { data } = await api.get('/api/staff/accounts')
    setRows(data.content)
  }
  async function changeStatus(accountNumber: string, status: string) {
    if (!confirm(`Set ${accountNumber} to ${status}?`)) return
    await api.patch(`/api/staff/accounts/${accountNumber}/status`, { status })
    const { data } = await api.get('/api/staff/accounts')
    setRows(data.content)
  }
  return (
    <>
      <h1>Account management</h1>
      <form className="card row" onSubmit={create}>
        <div><label>Customer number</label><input value={form.customerNumber} onChange={(e) => setForm({ ...form, customerNumber: e.target.value })} /></div>
        <div><label>Type</label><select value={form.accountType} onChange={(e) => setForm({ ...form, accountType: e.target.value })}><option>SAVINGS</option><option>CURRENT</option></select></div>
        <div><label>Opening</label><input value={form.openingBalance} onChange={(e) => setForm({ ...form, openingBalance: e.target.value })} /></div>
        <button className="btn btn-primary" style={{ width: 'auto' }}>Open account</button>
      </form>
      <div className="card" style={{ marginTop: '1rem' }}>
        <table className="table">
          <thead><tr><th>Number</th><th>Customer</th><th>Balance</th><th>Status</th><th></th></tr></thead>
          <tbody>
            {rows.map((a) => (
              <tr key={a.accountNumber}>
                <td>{a.accountNumber}</td><td>{a.customerName}</td><td>{a.balance}</td><td>{a.status}</td>
                <td>
                  <button className="btn btn-ghost" onClick={() => changeStatus(a.accountNumber, 'BLOCKED')}>Block</button>
                  <button className="btn btn-ghost" onClick={() => changeStatus(a.accountNumber, 'ACTIVE')}>Activate</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  )
}

export function StaffTransactionsPage() {
  const [rows, setRows] = useState<any[]>([])
  const [q, setQ] = useState('')
  const [status, setStatus] = useState('')
  const [post, setPost] = useState({ accountNumber: '1000000001', amount: '100.00', description: 'Demo posting' })
  async function load(e?: FormEvent) {
    e?.preventDefault()
    const { data } = await api.get('/api/staff/transactions', { params: { q, status: status || undefined } })
    setRows(data.content)
  }
  useEffect(() => { load() }, [])
  return (
    <>
      <h1>Transaction monitoring</h1>
      <form className="card row" onSubmit={load}>
        <div><label>Search</label><input value={q} onChange={(e) => setQ(e.target.value)} /></div>
        <div><label>Status</label>
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">All</option>
            <option>COMPLETED</option><option>FAILED</option><option>PENDING</option>
          </select>
        </div>
        <button className="btn btn-primary" style={{ width: 'auto' }}>Filter</button>
      </form>
      <div className="card" style={{ marginTop: '1rem' }}>
        <table className="table">
          <thead><tr><th>Ref</th><th>Type</th><th>Amount</th><th>Status</th></tr></thead>
          <tbody>{rows.map((t) => <tr key={t.reference}><td>{t.reference}</td><td>{t.transactionType}</td><td>{t.amount}</td><td>{t.status}</td></tr>)}</tbody>
        </table>
      </div>
      <form className="card" style={{ marginTop: '1rem' }} onSubmit={async (e) => { e.preventDefault(); await api.post('/api/staff/transactions/deposit', { ...post, amount: Number(post.amount) }); load() }}>
        <h3>Simulated deposit</h3>
        <label>Account</label><input value={post.accountNumber} onChange={(e) => setPost({ ...post, accountNumber: e.target.value })} />
        <label>Amount</label><input value={post.amount} onChange={(e) => setPost({ ...post, amount: e.target.value })} />
        <button className="btn btn-primary">Post deposit</button>
      </form>
    </>
  )
}

export function ReportsPage() {
  return (
    <>
      <h1>Reports</h1>
      <div className="card">
        <p>Download the current transaction extract (CSV). Figures come from the database, not placeholders.</p>
        <button className="btn btn-primary" style={{ width: 'auto' }} onClick={() => {
          api.get('/api/staff/reports/transactions.csv', { responseType: 'blob' }).then((r) => {
            const url = URL.createObjectURL(r.data)
            const a = document.createElement('a')
            a.href = url
            a.download = 'transactions-demo.csv'
            a.click()
          })
        }}>Export CSV</button>
      </div>
    </>
  )
}
