import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
})

api.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export default api

export type UserSummary = {
  id: number
  username: string
  email: string
  fullName: string
  status: string
  roles: string[]
}

export type AuthResponse = {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresInSeconds: number
  user: UserSummary
}

export type Page<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
