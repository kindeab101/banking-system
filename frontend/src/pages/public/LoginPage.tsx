import { FormEvent, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const user = await login(username, password)
      if (user.roles.includes('ADMINISTRATOR')) navigate('/admin/dashboard')
      else if (user.roles.includes('BANK_EMPLOYEE')) navigate('/staff/dashboard')
      else navigate('/app/dashboard')
    } catch {
      setError('Sign-in failed. Check your details and try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-shell">
      <form className="login-card" onSubmit={onSubmit} aria-labelledby="login-title">
        <p className="muted" style={{ marginTop: 0 }}>DEMO / TEST</p>
        <h1 id="login-title" className="brand">SecureBank Portal</h1>
        <p className="muted">Simulated banking access for internship demonstration.</p>
        <label htmlFor="username">Username or email</label>
        <input id="username" name="username" autoComplete="username" required value={username} onChange={(e) => setUsername(e.target.value)} />
        <label htmlFor="password">Password</label>
        <input id="password" name="password" type="password" autoComplete="current-password" required value={password} onChange={(e) => setPassword(e.target.value)} />
        {error && <p className="error" role="alert">{error}</p>}
        <button className="btn btn-primary" disabled={loading}>{loading ? 'Signing in…' : 'Sign in securely'}</button>
        <p className="muted" style={{ fontSize: '0.85rem' }}>Development demo users are listed in the project README. Do not use real banking credentials.</p>
      </form>
    </div>
  )
}

export function UnauthorizedPage() {
  return (
    <div className="main">
      <div className="card">
        <h1>Access denied</h1>
        <p>Your role is not permitted to open this page. This decision is enforced by the server as well as the portal.</p>
        <a href="/login">Return to sign in</a>
      </div>
    </div>
  )
}
