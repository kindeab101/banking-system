import { FormEvent, useEffect, useState } from 'react'
import api from '../../services/api'

export function AdminDashboard() {
  const [data, setData] = useState<any>(null)
  useEffect(() => { api.get('/api/admin/dashboard').then((r) => setData(r.data)) }, [])
  if (!data) return <p>Loading admin dashboard…</p>
  return (
    <>
      <h1>Administrator dashboard</h1>
      <div className="grid">
        <div className="card"><p className="muted">Customers</p><h2>{data.totalCustomers}</h2></div>
        <div className="card"><p className="muted">Active accounts</p><h2>{data.activeAccounts}</h2></div>
        <div className="card"><p className="muted">Successful today</p><h2>{data.successfulToday}</h2></div>
        <div className="card"><p className="muted">Failed transactions</p><h2>{data.failedTransactions}</h2></div>
      </div>
      <div className="card" style={{ marginTop: '1rem' }}>
        <h3>Recent audit events</h3>
        <table className="table">
          <thead><tr><th>Action</th><th>Actor</th><th>Result</th><th>When</th></tr></thead>
          <tbody>
            {(data.recentAudit || []).map((a: any, i: number) => (
              <tr key={i}><td>{a.action}</td><td>{a.actor}</td><td>{a.result}</td><td>{a.createdAt}</td></tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  )
}

export function UsersPage() {
  const [rows, setRows] = useState<any[]>([])
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [form, setForm] = useState({ username: '', email: '', fullName: '', temporaryPassword: 'DemoTemp#2026x', roleCode: 'CUSTOMER' })
  async function load() {
    const { data } = await api.get('/api/admin/users')
    setRows(data.content)
  }
  useEffect(() => { load() }, [])
  async function create(e: FormEvent) {
    e.preventDefault()
    setError('')
    setMessage('')
    try {
      const { data } = await api.post('/api/admin/users', form)
      const roleNote = form.roleCode === 'CUSTOMER'
        ? ' They can sign in now with this username and password. A savings account was opened for them.'
        : ' They can sign in now with this username and password.'
      setMessage(`Created ${data.username}.${roleNote}`)
      setForm({ ...form, username: '', email: '', fullName: '' })
      load()
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Could not create user')
    }
  }
  return (
    <>
      <h1>User management</h1>
      <div className="card">
        <table className="table">
          <thead><tr><th>Username</th><th>Name</th><th>Status</th><th>Roles</th><th></th></tr></thead>
          <tbody>
            {rows.map((u) => (
              <tr key={u.id}>
                <td>{u.username}</td><td>{u.fullName}</td><td>{u.status}</td><td>{u.roles?.join(', ')}</td>
                <td>
                  <button className="btn btn-ghost" onClick={() => api.patch(`/api/admin/users/${u.id}/status`, { status: u.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE' }).then(load)}>
                    Toggle status
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <form className="card" style={{ marginTop: '1rem', maxWidth: 560 }} onSubmit={create}>
        <h3>Create user</h3>
        <p className="muted">Customers receive a login and a savings account immediately. Give them the username and password you enter here.</p>
        <label>Username</label><input required value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} />
        <label>Email</label><input type="email" required value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
        <label>Full name</label><input required value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
        <label>Password</label>
        <input required minLength={10} value={form.temporaryPassword} onChange={(e) => setForm({ ...form, temporaryPassword: e.target.value })} />
        <p className="muted">At least 10 characters. This is the password they use to sign in.</p>
        <label>Role</label>
        <select value={form.roleCode} onChange={(e) => setForm({ ...form, roleCode: e.target.value })}>
          <option value="CUSTOMER">CUSTOMER</option>
          <option value="BANK_EMPLOYEE">BANK_EMPLOYEE</option>
          <option value="ADMINISTRATOR">ADMINISTRATOR</option>
        </select>
        {error && <p className="error" role="alert">{error}</p>}
        {message && <p className="ok">{message}</p>}
        <button className="btn btn-primary">Create user</button>
      </form>
    </>
  )
}

export function RolesPage() {
  const [rows, setRows] = useState<any[]>([])
  useEffect(() => { api.get('/api/admin/roles').then((r) => setRows(r.data)) }, [])
  return (
    <>
      <h1>Roles and permissions</h1>
      {rows.map((r) => (
        <div className="card" key={r.code} style={{ marginBottom: '1rem' }}>
          <h3>{r.name} ({r.code})</h3>
          <p>{(r.permissions || []).join(', ')}</p>
        </div>
      ))}
    </>
  )
}

export function AuditPage() {
  const [q, setQ] = useState('')
  const [rows, setRows] = useState<any[]>([])
  async function load(e?: FormEvent) {
    e?.preventDefault()
    const { data } = await api.get('/api/admin/audit-logs', { params: { q } })
    setRows(data.content)
  }
  useEffect(() => { load() }, [])
  return (
    <>
      <h1>Audit logs</h1>
      <p className="muted">Append-only. There is no edit or delete API.</p>
      <form className="card row" onSubmit={load}>
        <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Filter action or reference" />
        <button className="btn btn-primary" style={{ width: 'auto' }}>Search</button>
      </form>
      <div className="card" style={{ marginTop: '1rem' }}>
        <table className="table">
          <thead><tr><th>When</th><th>Actor</th><th>Action</th><th>Entity</th><th>Result</th></tr></thead>
          <tbody>
            {rows.map((a) => (
              <tr key={a.id}><td>{a.createdAt}</td><td>{a.actorUsername}</td><td>{a.action}</td><td>{a.entityReference}</td><td>{a.result}</td></tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  )
}

export function SettingsPage() {
  const [rows, setRows] = useState<any[]>([])
  useEffect(() => { api.get('/api/admin/settings').then((r) => setRows(r.data)) }, [])
  return (
    <>
      <h1>System settings</h1>
      {rows.map((s) => (
        <form key={s.key} className="card row" onSubmit={async (e) => {
          e.preventDefault()
          await api.patch(`/api/admin/settings/${s.key}`, { value: s.value })
        }}>
          <div style={{ flex: 1 }}>
            <label>{s.key}</label>
            <input value={s.value} onChange={(e) => setRows(rows.map((x) => x.key === s.key ? { ...x, value: e.target.value } : x))} />
            <p className="muted">{s.description}</p>
          </div>
          <button className="btn btn-primary" style={{ width: 'auto' }}>Save</button>
        </form>
      ))}
    </>
  )
}
