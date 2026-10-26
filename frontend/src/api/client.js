import axios from 'axios'

export const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export function getWebSocketUrl(path = '') {
  const wsProto = baseURL.startsWith('https') ? 'wss' : 'ws'
  const cleanBase = baseURL.replace(/^https?:\/\//, '')
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${wsProto}://${cleanBase}${normalizedPath}`
}

const api = axios.create({ baseURL })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('aes_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      localStorage.removeItem('aes_token')
      localStorage.removeItem('aes_user')
      if (window.location.pathname !== '/login') window.location.assign('/login')
    }
    const responseData = error.response?.data
    const message = status === 403
      ? 'You do not have permission to perform this action.'
      : responseData?.message ||
        (error.code === 'ERR_NETWORK'
          ? 'Cannot reach the backend. Is it running?'
          : 'Something went wrong. Please try again.')
    const normalized = new Error(message)
    normalized.status = status
    normalized.code = error.code
    normalized.requestId = responseData?.requestId
    return Promise.reject(normalized)
  }
)

export default api
