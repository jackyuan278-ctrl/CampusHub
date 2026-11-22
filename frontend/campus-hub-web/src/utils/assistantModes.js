/**
 * 智能助手的功能档位。
 * 说明：mode 会随对话请求一起发给 agent（payload.mode），
 * 后端可用它切换 system prompt / 可选工具集；前端只用它决定文案与建议项。
 */
export const ASSISTANT_MODES = [
  {
    key: 'campus',
    label: '校园问题',
    icon: 'School',
    hint: '二手、活动、图书馆、教务处 FAQ 一站式',
    placeholder: '问问校园生活，比如「图书馆期中考开到几点」「这周末有什么活动」'
  },
  {
    key: 'travel',
    label: '旅游规划',
    icon: 'Compass',
    hint: '天气、航班、酒店、攻略一键搞',
    placeholder: '说走就走，比如「下周去厦门玩三天，预算 3000」'
  },
  {
    key: 'research',
    label: '学术调研',
    icon: 'Reading',
    hint: '语义搜论文 + 引用图 + 脉络梳理',
    placeholder: '帮我搜「transformer attention mechanism」的经典论文'
  },
  {
    key: 'finance',
    label: '行情资讯',
    icon: 'TrendCharts',
    hint: '个股行情、财报、相关新闻',
    placeholder: '查一下贵州茅台 600519 最近行情和财报',
    riskWarning: true
  },
  {
    key: 'chat',
    label: '智能问答',
    icon: 'ChatDotRound',
    hint: '跨场景兜底，代码/面试/英语/健身/周报都行',
    placeholder: '随便问点什么，比如「为什么 Redis 的 Hash 比 String 省内存」'
  },
  {
    key: 'casual',
    label: '随便聊聊',
    icon: 'ChatLineRound',
    hint: '什么都能聊，需要时替你上网查',
    placeholder: '最近有什么好看的科幻电影？'
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
  { icon: 'ChatDotRound', title: '这道题怎么理解', desc: '把题目发我，一起拆', text: '帮我看一道题：为什么 Redis 的 Hash 比 String 更省内存？' },
  { icon: 'TrendCharts', title: '查一下茅台最近行情', desc: '只读公开数据，不做投资建议', text: '帮我查一下贵州茅台（600519）最近的行情和财报' },
  { icon: 'Compass', title: '周末去厦门玩三天', desc: '天气、航班、攻略帮我搞', text: '下周去厦门玩三天，帮我查天气、航班和攻略' },
  { icon: 'Money', title: 'Sony WH-1000XM5 比价', desc: '京东/天猫/拼多多哪家划算', text: '帮我对比 Sony WH-1000XM5 在京东、天猫、拼多多的价格' },
  { icon: 'Reading', title: '帮我找注意力机制的论文', desc: '给我 3 篇最经典的', text: '帮我搜索 attention mechanism 相关的经典论文，列 3 篇最重要的' },
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
