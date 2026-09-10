import { Navigate, Route, Routes } from 'react-router-dom'
import { AppLayout, RequireAuth } from './layouts/AppLayout'
import { LoginPage, UnauthorizedPage } from './pages/public/LoginPage'
import {
  AccountDetailPage, AccountsPage, CustomerDashboard, NotificationsPage,
  ProfilePage, StatementsPage, TransactionDetailPage, TransferPage
} from './pages/customer/CustomerPages'
import {
  CustomerDetailPage, CustomerManagePage, ReportsPage, StaffAccountsPage,
  StaffDashboard, StaffTransactionsPage
} from './pages/employee/StaffPages'
import { AdminDashboard, AuditPage, RolesPage, SettingsPage, UsersPage } from './pages/admin/AdminPages'
import { useAuth } from './context/AuthContext'

function HomeRedirect() {
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  if (user.roles.includes('ADMINISTRATOR')) return <Navigate to="/admin/dashboard" replace />
  if (user.roles.includes('BANK_EMPLOYEE')) return <Navigate to="/staff/dashboard" replace />
  return <Navigate to="/app/dashboard" replace />
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/unauthorized" element={<UnauthorizedPage />} />
      <Route element={<RequireAuth />}>
        <Route element={<AppLayout />}>
          <Route path="/" element={<HomeRedirect />} />
        </Route>
      </Route>
      <Route element={<RequireAuth roles={['CUSTOMER']} />}>
        <Route element={<AppLayout />}>
          <Route path="/app/dashboard" element={<CustomerDashboard />} />
          <Route path="/app/profile" element={<ProfilePage />} />
          <Route path="/app/accounts" element={<AccountsPage />} />
          <Route path="/app/accounts/:accountNumber" element={<AccountDetailPage />} />
          <Route path="/app/transfer" element={<TransferPage />} />
          <Route path="/app/statements" element={<StatementsPage />} />
          <Route path="/app/notifications" element={<NotificationsPage />} />
          <Route path="/app/transactions/:reference" element={<TransactionDetailPage />} />
        </Route>
      </Route>
      <Route element={<RequireAuth roles={['BANK_EMPLOYEE', 'ADMINISTRATOR']} />}>
        <Route element={<AppLayout />}>
          <Route path="/staff/dashboard" element={<StaffDashboard />} />
          <Route path="/staff/customers" element={<CustomerManagePage />} />
          <Route path="/staff/customers/:id" element={<CustomerDetailPage />} />
          <Route path="/staff/accounts" element={<StaffAccountsPage />} />
          <Route path="/staff/transactions" element={<StaffTransactionsPage />} />
          <Route path="/staff/reports" element={<ReportsPage />} />
        </Route>
      </Route>
      <Route element={<RequireAuth roles={['ADMINISTRATOR']} />}>
        <Route element={<AppLayout />}>
          <Route path="/admin/dashboard" element={<AdminDashboard />} />
          <Route path="/admin/users" element={<UsersPage />} />
          <Route path="/admin/roles" element={<RolesPage />} />
          <Route path="/admin/audit" element={<AuditPage />} />
          <Route path="/admin/settings" element={<SettingsPage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
