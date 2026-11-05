import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { clearMessages, streamChat } from '@/api/assistant'
import { getToken, notifyUnauthorized } from '@/api/http'
import { pickDemoReply, streamDemo } from '@/utils/agentDemo'

/**
 * 会话状态。
 * 说明：线程列表（标题、更新时间）仍然只在本地——agent 侧没有「列出我的会话」
 * 接口，只有按 thread_id 取消息。所以历史消息仍以 localStorage 为准，
 * 但清空时会同步清服务端，避免「界面空了、模型还记得」。
 */
const STORAGE_KEY = 'campushub_ai_threads_v1'
const THEME_KEY = 'campushub_ai_theme'
const THEMES = ['default', 'light', 'dark']
const MAX_THREADS = 24
const MAX_MESSAGES = 80

function readTheme() {
  const value = localStorage.getItem(THEME_KEY)
  return THEMES.includes(value) ? value : 'default'
}

function uid(prefix = 't') {
  return `${prefix}_${Date.now().toString(36)}${Math.random().toString(36).slice(2, 7)}`
}

function readStorage() {
  try {
    const list = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]')
    if (!Array.isArray(list)) return []
    return list
      .filter((thread) => thread && thread.id)
      .map((thread) => ({
        id: thread.id,
        title: thread.title || '新对话',
        updatedAt: thread.updatedAt || Date.now(),
        mode: thread.mode || 'chat',
        messages: Array.isArray(thread.messages)
          ? thread.messages.map((message) => ({ ...message, streaming: false }))
          : []
      }))
  } catch {
    return []
  }
}

/** 附件只持久化元信息：File / blob: 链接刷新后就失效了 */
function plainAttachments(list = []) {
  // url 是后端返回的 OSS 公网地址，agent 靠它读图；没上传成功的附件只有元信息
  return list.map((item) => ({ name: item.name, type: item.type, size: item.size, url: item.url }))
}

export const useAssistantStore = defineStore('assistant', () => {
  const threads = ref(readStorage())
  const activeId = ref(threads.value[0]?.id || '')
  const mode = ref(threads.value[0]?.mode || 'chat')
  const theme = ref(readTheme())
  const sending = ref(false)
  const error = ref('')

  let controller = null

  const activeThread = computed(() => threads.value.find((thread) => thread.id === activeId.value) || null)
  const messages = computed(() => activeThread.value?.messages || [])
  const currentTitle = computed(() => activeThread.value?.title || '新对话')
  const isEmpty = computed(() => messages.value.length === 0)

  function persist() {
    try {
      const snapshot = threads.value.slice(0, MAX_THREADS).map((thread) => ({
        id: thread.id,
        title: thread.title,
        updatedAt: thread.updatedAt,
        mode: thread.mode,
        messages: thread.messages.slice(-MAX_MESSAGES).map((message) => ({
          id: message.id,
          role: message.role,
          content: message.content,
          attachments: message.attachments || [],
          demo: Boolean(message.demo),
          stopped: Boolean(message.stopped),
          createdAt: message.createdAt
        }))
      }))
      localStorage.setItem(STORAGE_KEY, JSON.stringify(snapshot))
    } catch {
      // 超配额时放弃持久化，不影响当前会话
    }
  }

  function newThread() {
    stop()
    // 当前已经是空会话时直接复用，避免连点「新对话」堆出一串空白条目
    const current = activeThread.value
    if (current && current.messages.length === 0) {
      current.mode = mode.value
      current.updatedAt = Date.now()
      error.value = ''
      persist()
      return current
    }
    const thread = { id: uid('th'), title: '新对话', updatedAt: Date.now(), mode: mode.value, messages: [] }
    threads.value.unshift(thread)
    activeId.value = thread.id
    error.value = ''
    persist()
    // 必须返回数组里的响应式引用，而不是上面这个原始对象：
    // 原始对象被改时 Vue 的代理拦截不到，界面不会更新
    return threads.value[0]
  }

  function openThread(id) {
    if (!threads.value.some((thread) => thread.id === id)) return
    stop()
    activeId.value = id
    const thread = threads.value.find((item) => item.id === id)
    if (thread) mode.value = thread.mode || 'chat'
    error.value = ''
  }

  function removeThread(id) {
    const index = threads.value.findIndex((thread) => thread.id === id)
    if (index < 0) return
    // 服务端的业务记录和 checkpoint 一起删掉，否则同一个 thread_id 再被用上时
    // 模型还会接上旧上下文
    void clearMessages(id, { token: getToken() })
    threads.value.splice(index, 1)
    if (activeId.value === id) activeId.value = threads.value[0]?.id || ''
    persist()
  }

  function clearActive() {
    const thread = activeThread.value
    if (!thread) return
    stop()
    // 只清本地不够：服务端 checkpointer 还留着上文，界面空了但模型仍然记得
    void clearMessages(thread.id, { token: getToken() })
    thread.messages = []
    thread.title = '新对话'
    thread.updatedAt = Date.now()
    error.value = ''
    persist()
  }

  function clearAll() {
    stop()
    threads.value = []
    activeId.value = ''
    error.value = ''
    try {
      localStorage.removeItem(STORAGE_KEY)
    } catch {
      // 忽略
    }
  }

  function setMode(next) {
    mode.value = next
    if (activeThread.value) activeThread.value.mode = next
  }

  function setTheme(next) {
    if (!THEMES.includes(next)) return
    theme.value = next
    try {
      localStorage.setItem(THEME_KEY, next)
    } catch {
      // 存不了也不影响当前会话
    }
  }

  /** 有会话就继续当前会话，没有才新建（空会话天然复用，不会堆一串「新对话」） */
  function ensureThread() {
    if (activeThread.value) return activeThread.value
    return newThread()
  }

  function titleFrom(text) {
    const clean = String(text || '').replace(/\s+/g, ' ').trim()
    if (!clean) return '新对话'
    return clean.length > 18 ? `${clean.slice(0, 18)}…` : clean
  }

  function stop() {
    if (controller) {
      controller.abort()
      controller = null
    }
  }

  async function send(rawText, attachments = []) {
    const text = String(rawText || '').trim()
    if (sending.value) return
    if (!text && attachments.length === 0) return
    // 兜底：助手页已由路由守卫要求登录，这里再拦一道，防止将来放开路由后漏掉
    if (!getToken()) {
      error.value = '请先登录后再使用智能助手'
      return
    }

    const thread = ensureThread()
    thread.mode = mode.value
    if (thread.messages.length === 0) thread.title = titleFrom(text || attachments[0]?.name)

    // 附件的上传在 AssistantComposer 里完成（失败会保留输入内容），
    // 走到这里时 attachments 已经带 OSS url，直接透传给 agent。
    thread.messages.push({
      id: uid('m'),
      role: 'user',
      content: text,
      attachments: plainAttachments(attachments),
      createdAt: Date.now()
    })

    thread.messages.push({
      id: uid('m'),
      role: 'assistant',
      content: '',
      demo: false,
      stopped: false,
      streaming: true,
      createdAt: Date.now()
    })
    // Vue 3 陷阱：push 进去的是原始对象，直接改它不会触发响应式更新
    // （代理只拦通过代理的读写）。从数组里取回来才是响应式引用，
    // 之后所有增量都走这个引用，界面才能逐字刷新。
    const answer = thread.messages[thread.messages.length - 1]

    thread.updatedAt = Date.now()
    sending.value = true
    error.value = ''
    controller = new AbortController()
    const signal = controller.signal
    const payload = {
      message: text,
      thread_id: thread.id,
      mode: mode.value,
      attachments: plainAttachments(attachments)
    }

    try {
      const full = await streamChat(payload, {
        token: getToken(),
        signal,
        onDelta: (delta) => {
          answer.content += delta
        }
      })
      if (!full && !answer.content) throw new Error('empty')
    } catch (err) {
      const code = Number(err?.code)
      if (err?.name === 'AbortError') {
        answer.stopped = true
        if (!answer.content) answer.content = '（已停止生成）'
      } else if (code === 401) {
        // 登录态失效 ≠ 服务不可用。以前这里会塞一段演示回复，用户既不知道要
        // 重新登录，也分不清哪句是真的——先走登出流程，再把原因说清楚。
        answer.content = '（登录已过期，请重新登录后再试）'
        error.value = '登录已过期，请重新登录'
        notifyUnauthorized()
      } else if (code === 0) {
        // 真的连不上（fetch 直接失败）：给演示回复兜住界面，但必须标明是假的
        answer.demo = true
        error.value = `未连接到智能体服务（${err?.message || '连接失败'}），下面是演示回复`
        try {
          await streamDemo(pickDemoReply({ mode: payload.mode, text, attachments: payload.attachments }), {
            signal,
            onDelta: (delta) => {
              answer.content += delta
            }
          })
        } catch {
          answer.stopped = true
        }
      } else {
        // 其余都是服务端给的明确答复（额度用完 429、模型不可用 502…）。
        // 原样告诉用户，绝不用演示回复盖掉真实原因。
        const reason = err?.message || '请求失败，请稍后重试'
        answer.content = `（${reason}）`
        error.value = reason
      }
    } finally {
      answer.streaming = false
      sending.value = false
      controller = null
      thread.updatedAt = Date.now()
      persist()
    }
  }

  return {
    threads,
    activeId,
    mode,
    theme,
    sending,
    error,
    activeThread,
    messages,
    currentTitle,
    isEmpty,
    newThread,
    openThread,
    removeThread,
    clearActive,
    clearAll,
    setMode,
    setTheme,
    send,
    stop
  }
})
