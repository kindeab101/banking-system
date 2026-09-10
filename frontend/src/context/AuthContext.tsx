import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'
import api, { type AuthResponse, type UserSummary } from '../services/api'

type AuthState = {
  user: UserSummary | null
  login: (username: string, password: string) => Promise<UserSummary>
  logout: () => Promise<void>
  hasRole: (role: string) => boolean
}

const AuthContext = createContext<AuthState | undefined>(undefined)

function readUser(): UserSummary | null {
  const raw = sessionStorage.getItem('user')
  return raw ? JSON.parse(raw) as UserSummary : null
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserSummary | null>(readUser)

  const value = useMemo<AuthState>(() => ({
    user,
    async login(username, password) {
      const { data } = await api.post<AuthResponse>('/api/auth/login', { username, password })
      sessionStorage.setItem('accessToken', data.accessToken)
      sessionStorage.setItem('refreshToken', data.refreshToken)
      sessionStorage.setItem('user', JSON.stringify(data.user))
      setUser(data.user)
      return data.user
    },
    async logout() {
      try {
        await api.post('/api/auth/logout', { refreshToken: sessionStorage.getItem('refreshToken') })
      } catch {
        /* still clear local session */
      }
      sessionStorage.clear()
      setUser(null)
    },
    hasRole(role) {
      return !!user?.roles.includes(role)
    }
  }), [user])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('AuthProvider missing')
  return ctx
}
