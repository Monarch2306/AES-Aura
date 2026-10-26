import { createContext, useContext, useState, useCallback } from 'react'
import api from '../api/client'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      const raw = localStorage.getItem('aes_user')
      return raw ? JSON.parse(raw) : null
    } catch {
      localStorage.removeItem('aes_user')
      localStorage.removeItem('aes_token')
      return null
    }
  })

  const login = useCallback(async (email, password) => {
    const res = await api.post('/api/auth/login', { email, password })
    const data = res.data.data
    localStorage.setItem('aes_token', data.token)
    localStorage.setItem('aes_user', JSON.stringify(data))
    setUser(data)
    return data
  }, [])

  const register = useCallback(async (name, email, password, role) => {
    const res = await api.post('/api/auth/register', { name, email, password, role })
    const data = res.data.data
    localStorage.setItem('aes_token', data.token)
    localStorage.setItem('aes_user', JSON.stringify(data))
    setUser(data)
    return data
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem('aes_token')
    localStorage.removeItem('aes_user')
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider value={{ user, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
