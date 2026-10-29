/**
 * 展示层格式化工具。
 * 后端约定：金额一律为「分」；Long 型 ID 序列化为字符串；时间为 yyyy-MM-dd HH:mm:ss。
 */

/** 分 -> 元（数值） */
export function toYuan(cents) {
  const value = Number(cents)
  return Number.isFinite(value) ? value / 100 : 0
}

/** 分 -> 元（展示用，整数不带小数位） */
export function money(cents) {
  const value = toYuan(cents)
  if (!Number.isFinite(value)) return '0'
  return Number.isInteger(value) ? String(value) : value.toFixed(2)
}

/** 元 -> 分（提交前兜底） */
export function toCents(yuan) {
  const value = Number(yuan)
  return Number.isFinite(value) ? Math.round(value * 100) : 0
}

/**
 * 兼容后端 "yyyy-MM-dd HH:mm:ss" 与 ISO 8601 两种时间串。
 * 直接 new Date('2026-10-28 14:30:00') 在部分内核下会解析失败，这里显式转 ISO。
 */
export function parseTime(value) {
  if (!value) return null
  if (value instanceof Date) return Number.isNaN(value.getTime()) ? null : value
  if (typeof value === 'number') return new Date(value)
  const text = String(value).trim()
  if (!text) return null
  const normalized = text.includes('T') ? text : text.replace(' ', 'T')
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? null : date
}

function pad(value) {
  return String(value).padStart(2, '0')
}

/** 格式化日期；withTime 为 true 时输出 10-28 14:30 */
export function formatDate(value, withTime = false) {
  const date = parseTime(value)
  if (!date) return ''
  const base = `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
  if (!withTime) return base
  return `${base} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/** 只取 月 日，用于活动日期牌 */
export function dateParts(value) {
  const date = parseTime(value)
  if (!date) return { day: '--', month: '' }
  return { day: pad(date.getDate()), month: `${date.getMonth() + 1} 月` }
}

/** 相对时间：刚刚 / 3 小时前 / 2 天前 / 具体日期 */
export function fromNow(value) {
  const date = parseTime(value)
  if (!date) return ''
  const diff = Date.now() - date.getTime()
  if (diff < 0) return formatDate(value, true)
  const minute = 60 * 1000
  if (diff < minute) return '刚刚'
  if (diff < 60 * minute) return `${Math.floor(diff / minute)} 分钟前`
  if (diff < 24 * 60 * minute) return `${Math.floor(diff / (60 * minute))} 小时前`
  if (diff < 7 * 24 * 60 * minute) return `${Math.floor(diff / (24 * 60 * minute))} 天前`
  return formatDate(value)
}

/** 生成 el-date-picker 需要的 value-format 字符串 */
export function toDateTimeValue(date) {
  if (!date) return ''
  const value = date instanceof Date ? date : parseTime(date)
  if (!value) return ''
  return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())} ${pad(value.getHours())}:${pad(value.getMinutes())}:${pad(value.getSeconds())}`
}

export const ITEM_STATUS = {
  0: { text: '已下架', tone: 'muted' },
  1: { text: '在售', tone: 'ok' },
  2: { text: '已预订', tone: 'warn' },
  3: { text: '已售出', tone: 'muted' }
}

export const ACTIVITY_STATUS = {
  1: { text: '报名中', tone: 'ok' },
  2: { text: '进行中', tone: 'warn' },
  3: { text: '已结束', tone: 'muted' },
  4: { text: '已取消', tone: 'muted' }
}

export const SIGNUP_STATUS = {
  1: { text: '已报名', tone: 'ok' },
  2: { text: '候补中', tone: 'warn' },
  3: { text: '已取消', tone: 'muted' }
}

export const ORDER_STATUS = {
  1: { text: '待付款', tone: 'warn' },
  2: { text: '已付款', tone: 'ok' },
  3: { text: '已发货', tone: 'ok' },
  4: { text: '已完成', tone: 'muted' },
  5: { text: '已关闭', tone: 'muted' }
}

export const PAYMENT_TYPE = {
  1: '余额支付',
  2: '模拟支付'
}

/** 取状态文案，缺省兜底「未知」 */
export function statusMeta(map, value) {
  return map[value] || { text: '未知', tone: 'muted' }
}

/** 按分类/编号挑一个视觉色调，保证同一 id 每次渲染一致 */
const TONES = ['mint', 'sky', 'peach', 'violet', 'lemon', 'rose']
export function toneOf(key) {
  const text = String(key ?? '')
  let hash = 0
  for (let i = 0; i < text.length; i += 1) hash = (hash * 31 + text.charCodeAt(i)) % 997
  return TONES[hash % TONES.length]
}

const CATEGORY_ICONS = [
  { match: ['数码', '电子', '电器', '手机', '电脑'], icon: 'Monitor' },
  { match: ['书', '教材', '资料', '笔记'], icon: 'Reading' },
  { match: ['生活', '日用', '宿舍', '家居'], icon: 'WindPower' },
  { match: ['运动', '健身', '球'], icon: 'Basketball' },
  { match: ['服饰', '衣', '鞋', '包'], icon: 'ShoppingBag' },
  { match: ['乐器', '音乐'], icon: 'Headset' },
  { match: ['美妆', '护肤'], icon: 'MagicStick' }
]

/** 用分类名推断封面图标，避免商品图缺失时出现空白 */
export function iconOfCategory(categoryName) {
  const name = String(categoryName || '')
  const hit = CATEGORY_ICONS.find((entry) => entry.match.some((word) => name.includes(word)))
  return hit ? hit.icon : 'Goods'
}

/** 千分位，用于统计数字 */
export function thousands(value) {
  const number = Number(value)
  if (!Number.isFinite(number)) return '0'
  return number.toLocaleString('zh-CN')
}
