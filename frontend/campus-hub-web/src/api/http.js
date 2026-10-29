import axios from 'axios'
import { ElMessage } from 'element-plus'
import { isMockEnabled, resolveMock } from './mock'

const TOKEN_KEY = 'campushub_token'
const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

/** 业务异常：后端统一返回 { code, msg, data }，code !== 200 视为失败 */
export class ApiError extends Error {
  constructor(message, code = 0) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

export const apiClient = axios.create({
  baseURL: BASE_URL,
  timeout: 8000,
  headers: { 'Content-Type': 'application/json' }
})

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(token) {
  if (token) localStorage.setItem(TOKEN_KEY, token)
  else localStorage.removeItem(TOKEN_KEY)
}

let unauthorizedHandler = null
/** 由 main.js 注入：token 失效时清理登录态并跳转登录页 */
export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler
}

apiClient.interceptors.request.use((config) => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

apiClient.interceptors.response.use(
  (response) => {
    const body = response.data
    // 文件流等非 Result 结构直接返回
    if (!body || typeof body !== 'object' || !('code' in body)) return body
    if (Number(body.code) === 200) return body.data
    throw new ApiError(body.msg || '请求失败', body.code)
  },
  async (error) => {
    const config = error.config
    /**
     * 只有「连不上后端」时才走演示数据：
     * - 无响应（ECONNREFUSED / 超时）
     * - 开发代理因上游不可用返回的 HTML 5xx（真正的业务错误是 JSON Result）
     */
    const contentType = String(error.response?.headers?.['content-type'] || '')
    // 真实业务错误一定是 JSON（Result），其余 5xx 视为网关/代理不可用
    const proxyFailure = Boolean(error.response) && error.response.status >= 500 && !contentType.includes('application/json')
    if ((!error.response || proxyFailure) && config && isMockEnabled()) {
      const mocked = await resolveMock(config)
      if (mocked !== undefined) return mocked
    }
    if (error.code === 'ECONNABORTED') {
      throw new ApiError('请求超时，请检查网络后重试', 0)
    }
    if (!error.response || proxyFailure) {
      throw new ApiError('无法连接后端服务，请确认网关已启动', 0)
    }
    const status = error.response.status
    if (status === 401) throw new ApiError('登录状态已失效，请重新登录', 401)
    const payload = error.response.data
    throw new ApiError(payload?.msg || `请求失败（${status}）`, payload?.code || status)
  }
)

/**
 * 统一请求入口。
 * @param {import('axios').AxiosRequestConfig} config
 * @param {{ silent?: boolean }} options silent 为 true 时不弹全局错误提示（由页面自行渲染错误态）
 */
export async function request(config, options = {}) {
  const { silent = false } = options
  try {
    return await apiClient.request(config)
  } catch (error) {
    const apiError = error instanceof ApiError ? error : new ApiError(error.message || '请求失败', 0)
    if (Number(apiError.code) === 401) unauthorizedHandler?.()
    if (!silent) ElMessage.error(apiError.message)
    throw apiError
  }
}

export default apiClient
