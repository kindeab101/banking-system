import { NavLink, Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export function RequireAuth({ roles }: { roles?: string[] }) {
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  if (roles && !roles.some((r) => user.roles.includes(r))) return <Navigate to="/unauthorized" replace />
  return <Outlet />
}

export function AppLayout() {
  const { user, logout, hasRole } = useAuth()
  return (
    <>
      <div className="banner">DEMO / TEST ENVIRONMENT — Simulated ETB transactions. Not a production banking system.</div>
      <div className="app">
        <nav className="nav" aria-label="Primary">
          <p className="brand" style={{ color: '#fff', padding: '0 0.8rem' }}>SecureBank</p>
          <p className="muted" style={{ padding: '0 0.8rem 0.8rem', color: '#9fb0c7' }}>{user?.fullName}</p>
          {hasRole('CUSTOMER') && (
            <>
              <NavLink to="/app/dashboard">Dashboard</NavLink>
              <NavLink to="/app/profile">Profile</NavLink>
              <NavLink to="/app/accounts">Accounts</NavLink>
              <NavLink to="/app/transfer">Transfer</NavLink>
              <NavLink to="/app/statements">Statements</NavLink>
              <NavLink to="/app/notifications">Notifications</NavLink>
            </>
          )}
          {(hasRole('BANK_EMPLOYEE') || hasRole('ADMINISTRATOR')) && (
            <>
              <NavLink to="/staff/dashboard">Operations</NavLink>
              <NavLink to="/staff/customers">Customers</NavLink>
              <NavLink to="/staff/accounts">Accounts</NavLink>
              <NavLink to="/staff/transactions">Transactions</NavLink>
              <NavLink to="/staff/reports">Reports</NavLink>
            </>
          )}
          {hasRole('ADMINISTRATOR') && (
            <>
              <NavLink to="/admin/dashboard">Admin home</NavLink>
              <NavLink to="/admin/users">Users</NavLink>
              <NavLink to="/admin/roles">Roles</NavLink>
              <NavLink to="/admin/audit">Audit logs</NavLink>
              <NavLink to="/admin/settings">Settings</NavLink>
            </>
          )}
          <button className="btn btn-gold" style={{ width: '100%', marginTop: '1rem' }} onClick={() => logout()}>Log out</button>
        </nav>
        <main className="main">
          <Outlet />
        </main>
      </div>
    </>
  )
}
