/**
 * 智能助手的功能档位。
 * 说明：mode 会随对话请求一起发给 agent（payload.mode），
 * 后端可用它切换 system prompt / 可选工具集；前端只用它决定文案与建议项。
 */
export const ASSISTANT_MODES = [
  {
    key: 'casual',
    label: '随便聊聊',
    icon: 'ChatLineRound',
    hint: '什么都能聊，需要时替你上网查',
    placeholder: '想聊什么都行，比如「最近有什么好看的电影」'
  },
  {
    key: 'chat',
    label: '智能问答',
    icon: 'ChatDotRound',
    hint: '校园生活里的问题都可以问',
    placeholder: '随便问点什么，比如「图书馆期中考期间开放到几点」'
  },
  {
    key: 'market',
    label: '二手估价',
    icon: 'Goods',
    hint: '描述成色，给你一个参考价',
    placeholder: '描述一下你的闲置物品，比如「罗技 G304，9 成新，有包装」'
  },
  {
    key: 'activity',
    label: '活动推荐',
    icon: 'Calendar',
    hint: '按兴趣和时间挑活动',
    placeholder: '说说你的兴趣和时间，比如「周末下午，想认识新朋友」'
  },
  {
    key: 'doc',
    label: '资料解读',
    icon: 'Document',
    hint: '上传 PDF / 图片，划重点',
    placeholder: '上传讲义或截图，然后告诉我你想看哪部分'
  }
]

export function modeMeta(key) {
  return ASSISTANT_MODES.find((item) => item.key === key) || ASSISTANT_MODES[0]
}

/** 助手页主题：默认薄荷 / 白（黑白极简）/ 黑 */
export const ASSISTANT_THEMES = [
  { key: 'default', label: '默认' },
  { key: 'light', label: '白' },
  { key: 'dark', label: '黑' }
]

/** 附件白名单与体积上限（前端先拦一道，后端仍需校验） */
export const ATTACHMENT_RULES = {
  accept: 'image/*,.pdf,.doc,.docx,.txt,.md',
  maxSize: 10 * 1024 * 1024,
  maxCount: 5
}

export const SUGGESTIONS = [
  { icon: 'Goods', title: '找一台二手显示器', desc: '预算 500 以内，宿舍用', text: '帮我找一台二手显示器，预算 500 以内，宿舍用，最好 24 寸' },
  { icon: 'Calendar', title: '这周末有什么活动', desc: '想认识新朋友的那种', text: '这周末学校有什么活动？我想找能认识新朋友的那种' },
  { icon: 'Document', title: '帮我总结一份讲义', desc: '上传 PDF，我划重点', text: '我上传了一份讲义，帮我总结重点并列出考前要看的部分' },
  { icon: 'ChatDotRound', title: '这道题怎么理解', desc: '把题目发我，一起拆', text: '帮我看一道题：为什么 Redis 的 Hash 比 String 更省内存？' }
]

/** 按扩展名/类型挑一个图标，避免附件列表里出现清一色方块 */
export function fileIcon(name = '', type = '') {
  const text = `${type} ${name}`.toLowerCase()
  if (text.includes('image') || /\.(png|jpe?g|webp|gif|bmp)$/.test(text)) return 'Picture'
  if (text.includes('pdf')) return 'Document'
  if (/\.(doc|docx)$/.test(text)) return 'Tickets'
  return 'Files'
}

export function formatSize(bytes) {
  const value = Number(bytes) || 0
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(0)} KB`
  return `${(value / 1024 / 1024).toFixed(1)} MB`
}
