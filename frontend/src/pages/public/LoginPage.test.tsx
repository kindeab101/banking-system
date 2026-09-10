import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { AuthProvider } from '../context/AuthContext'
import { LoginPage } from './LoginPage'

test('login form exposes labelled fields', () => {
  render(
    <MemoryRouter>
      <AuthProvider>
        <LoginPage />
      </AuthProvider>
    </MemoryRouter>
  )
  expect(screen.getByLabelText(/username or email/i)).toBeTruthy()
  expect(screen.getByLabelText(/password/i)).toBeTruthy()
})
