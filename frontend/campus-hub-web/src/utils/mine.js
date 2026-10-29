/**
 * 后端 ActivityVO 未返回 publisherId，前端在发布成功后本地记一份，
 * 用于在详情页展示「取消活动」这类发布者操作。
 */
const KEY = 'campushub_published_activities'

function read() {
  try {
    const list = JSON.parse(localStorage.getItem(KEY) || '[]')
    return Array.isArray(list) ? list.map(String) : []
  } catch {
    return []
  }
}

export function rememberPublishedActivity(id) {
  if (!id) return
  const list = read()
  const value = String(id)
  if (!list.includes(value)) localStorage.setItem(KEY, JSON.stringify([...list, value]))
}

export function forgetPublishedActivity(id) {
  const value = String(id)
  localStorage.setItem(KEY, JSON.stringify(read().filter((entry) => entry !== value)))
}

export function isMyActivity(id) {
  return read().includes(String(id))
}
