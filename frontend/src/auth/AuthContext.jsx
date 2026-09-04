import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import api from '../api/client'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem('helpdesk_user')
    return raw ? JSON.parse(raw) : null
  })
  const [authLoading, setAuthLoading] = useState(() => Boolean(localStorage.getItem('helpdesk_token')))

  useEffect(() => {
    const token = localStorage.getItem('helpdesk_token')
    if (!token) return
    api.get('/auth/me')
      .then(({ data }) => {
        localStorage.setItem('helpdesk_user', JSON.stringify(data))
        setUser(data)
      })
      .catch(() => {
        localStorage.removeItem('helpdesk_token')
        localStorage.removeItem('helpdesk_user')
        setUser(null)
      })
      .finally(() => setAuthLoading(false))
  }, [])

  const value = useMemo(() => ({
    user,
    authLoading,
    async login(email, password) {
      const { data } = await api.post('/auth/login', { email, password })
      localStorage.setItem('helpdesk_token', data.token)
      localStorage.setItem('helpdesk_user', JSON.stringify(data))
      setUser(data)
      return data
    },
    logout() {
      localStorage.removeItem('helpdesk_token')
      localStorage.removeItem('helpdesk_user')
      setUser(null)
    }
  }), [user, authLoading])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  return useContext(AuthContext)
}

export function homeFor(role) {
  if (role === 'ADMIN') return '/admin'
  if (role === 'IT_STAFF') return '/staff'
  return '/app'
}
