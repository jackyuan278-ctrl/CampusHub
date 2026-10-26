import axios from 'axios'

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 8000,
  headers: { 'Content-Type': 'application/json' }
})

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('campushub_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export function unwrapResponse(response) {
  return response?.data?.data ?? response?.data ?? response
}

export async function withMockFallback(request, fallback, delay = 220) {
  try {
    return unwrapResponse(await request())
  } catch (error) {
    await new Promise((resolve) => setTimeout(resolve, delay))
    return typeof fallback === 'function' ? fallback(error) : fallback
  }
}
