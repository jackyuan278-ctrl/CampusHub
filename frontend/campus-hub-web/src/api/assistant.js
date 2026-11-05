import { ApiError } from './http'

/**
 * 智能助手：campus-agent（FastAPI，默认 8000）
 * 接口：POST /assistant/chat（SSE 流式）、GET/DELETE /assistant/chat/messages
 *
 * 这里不走 api/http.js 的 axios 实例：SSE 需要读取 response.body 流，
 * 而 axios 的浏览器适配层会把响应整体缓冲，拿不到增量。
 */
const AGENT_BASE = (import.meta.env.VITE_AGENT_BASE_URL || 'http://127.0.0.1:8000').replace(/\/+$/, '')

export { AGENT_BASE }

/**
 * 发起一次流式对话。
 * @param {{message:string, thread_id:string, mode?:string, attachments?:Array}} payload
 * @param {{token?:string, signal?:AbortSignal, onDelta?:(delta:string, full:string)=>void}} options
 * @returns {Promise<string>} 完整回复文本
 */
export async function streamChat(payload, options = {}) {
  const { token, signal, onDelta } = options
  let response
  try {
    response = await fetch(`${AGENT_BASE}/assistant/chat`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      body: JSON.stringify(payload),
      signal
    })
  } catch (error) {
    if (error?.name === 'AbortError') throw error
    throw new ApiError('无法连接智能体服务', 0)
  }

  if (!response.ok) {
    // 后端会把「额度用完」这类原因放在 message 里，别丢掉
    const body = await response.json().catch(() => null)
    throw new ApiError(body?.message || `智能体服务返回 ${response.status}`, response.status)
  }

  const contentType = String(response.headers.get('content-type') || '')

  // 兼容两种返回：SSE 流 与 普通 JSON（后端先落地 JSON 也能跑）
  if (!contentType.includes('text/event-stream') || !response.body) {
    const body = await response.json().catch(() => null)
    if (!body) throw new ApiError('智能体返回格式无法解析', 0)
    const text = body.data?.content ?? body.content ?? body.message ?? body.answer ?? ''
    if (!text) throw new ApiError('智能体返回了空回复', 0)
    onDelta?.(text, text)
    return text
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let full = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })

    let index = buffer.indexOf('\n\n')
    while (index >= 0) {
      const chunk = buffer.slice(0, index)
      buffer = buffer.slice(index + 2)
      for (const line of chunk.split('\n')) {
        if (!line.startsWith('data:')) continue
        const data = line.slice(5).trim()
        if (!data) continue
        if (data === '[DONE]') return full
        let delta = ''
        try {
          const parsed = JSON.parse(data)
          delta = parsed.delta ?? parsed.content ?? parsed.text ?? parsed.data?.content ?? ''
        } catch {
          // 后端直接推纯文本时也能兜住
          delta = data
        }
        if (delta) {
          full += delta
          onDelta?.(delta, full)
        }
      }
      index = buffer.indexOf('\n\n')
    }
  }

  return full
}

/** 上传一个附件，返回 {url, key, size}（后端存 OSS，url 可直接喂给视觉模型） */
export async function uploadAttachment(file, token) {
  const form = new FormData()
  form.append('file', file)

  let response
  try {
    response = await fetch(`${AGENT_BASE}/assistant/upload`, {
      method: 'POST',
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      body: form
    })
  } catch {
    throw new ApiError('附件上传失败：连不上智能体服务', 0)
  }

  const body = await response.json().catch(() => null)
  if (!response.ok) {
    throw new ApiError(body?.message || `附件上传失败（${response.status}）`, response.status)
  }
  return body?.data || {}
}

/** 拉取历史消息（后端实现后可替换本地缓存） */
export async function fetchMessages(threadId, options = {}) {
  try {
    const response = await fetch(`${AGENT_BASE}/assistant/chat/messages?thread_id=${encodeURIComponent(threadId)}`, {
      headers: options.token ? { Authorization: `Bearer ${options.token}` } : {}
    })
    if (!response.ok) return []
    const body = await response.json()
    return Array.isArray(body) ? body : body?.data || []
  } catch {
    return []
  }
}

/** 清空服务端历史 */
export async function clearMessages(threadId, options = {}) {
  try {
    await fetch(`${AGENT_BASE}/assistant/chat/messages?thread_id=${encodeURIComponent(threadId)}`, {
      method: 'DELETE',
      headers: options.token ? { Authorization: `Bearer ${options.token}` } : {}
    })
  } catch {
    // 服务端没实现时静默失败，本地清空即可
  }
}

export default { streamChat, uploadAttachment, fetchMessages, clearMessages }
