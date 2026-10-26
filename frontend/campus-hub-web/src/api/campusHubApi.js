import { apiClient, withMockFallback } from './http'

// 联调开关：true = 前端直接用 mock 数据（方便看效果）；false = 走真实后端接口
const USE_MOCK = true

const mockItems = [
  { id: 'item-1', title: '九成新机械键盘', desc: '自用半年，功能正常，校内面交。', category: '数码产品', categoryKey: 'digital', price: 129, originalPrice: 199, seller: '林同学', location: '东区 3 栋', condition: '九成新', cover: 'keyboard', accent: 'mint', icon: 'Keyboard', color: 'blue', description: '自用半年，功能正常，校内面交。' },
  { id: 'item-2', title: '高等数学教材套装', desc: '配套习题册，适合大一课程复习。', category: '教材书籍', categoryKey: 'books', price: 25, originalPrice: 68, seller: '陈同学', location: '图书馆门口', condition: '八成新', cover: 'book', accent: 'sky', icon: 'Reading', color: 'green', description: '配套习题册，适合大一课程复习。' },
  { id: 'item-3', title: '宿舍静音小风扇', desc: '静音三档风力，毕业出。', category: '生活用品', categoryKey: 'life', price: 35, originalPrice: 59, seller: '阿南', location: '南区宿舍', condition: '九成新', cover: 'fan', accent: 'peach', icon: 'WindPower', color: 'orange', description: '静音三档风力，毕业出。' },
  { id: 'item-4', title: '便携显示器', desc: '轻薄便携，适合宿舍和图书馆使用。', category: '数码产品', categoryKey: 'digital', price: 260, originalPrice: 399, seller: '周周', location: '大学生活动中心', condition: '九成新', cover: 'monitor', accent: 'violet', icon: 'Cellphone', color: 'purple', description: '轻薄便携，适合宿舍和图书馆使用。' },
  { id: 'item-5', title: '露营折叠椅两把', desc: '周末露营用过几次，结实好收纳。', category: '生活用品', categoryKey: 'life', price: 88, originalPrice: 129, seller: '小林同学', location: '西门取货点', condition: '八成新', cover: 'chair', accent: 'lemon', icon: 'Box', color: 'orange', description: '周末露营用过几次，结实好收纳。' },
  { id: 'item-6', title: '英语四级备考资料', desc: '真题、词汇书和笔记，一起带走。', category: '教材书籍', categoryKey: 'books', price: 18, originalPrice: 42, seller: '杨同学', location: '北区食堂', condition: '九成新', cover: 'notes', accent: 'rose', icon: 'Notebook', color: 'green', description: '真题、词汇书和笔记，一起带走。' },
  { id: 'item-7', title: '降噪耳机', desc: '自习和通勤必备，主动降噪效果好。', category: '数码产品', categoryKey: 'digital', price: 180, originalPrice: 299, seller: '王同学', location: '东区 5 栋', condition: '九成新', cover: 'earphone', accent: 'mint', icon: 'Headset', color: 'blue', description: '自习和通勤必备，主动降噪效果好。' },
  { id: 'item-8', title: '健身瑜伽垫', desc: '宿舍锻炼好帮手，附赠收纳带。', category: '生活用品', categoryKey: 'life', price: 45, originalPrice: 79, seller: '刘同学', location: '南区宿舍', condition: '八成新', cover: 'yoga', accent: 'sky', icon: 'Sunny', color: 'purple', description: '宿舍锻炼好帮手，附赠收纳带。' }
]

const mockActivities = [
  { id: 'activity-1', date: '2026-10-28', type: '校园活动', title: '秋日校园摄影分享会', location: '大学生活动中心', people: 36, capacity: 60, host: '摄影社', status: '报名中', tone: 'green', description: '带上相机或手机，和同学一起记录校园里的秋日色彩。' },
  { id: 'activity-2', date: '2026-11-02', type: '社团活动', title: '校园社团开放日', location: '南区广场', people: 128, capacity: 200, host: '学生社团联合会', status: '报名中', tone: 'blue', description: '认识新社团，找到志趣相投的伙伴。' },
  { id: 'activity-3', date: '2026-11-08', type: '讲座', title: '毕业生求职经验交流', location: '图书馆报告厅', people: 74, capacity: 120, host: '就业中心', status: '即将开始', tone: 'orange', description: '听听学长学姐的求职经验，提前做好职业规划。' },
  { id: 'activity-4', date: '2026-10-30', type: '校园活动', title: '新生篮球友谊赛', location: '东区篮球场', people: 48, capacity: 80, host: '校篮球队', status: '报名中', tone: 'green', description: '以球会友，新生和老生来一场友谊赛。' },
  { id: 'activity-5', date: '2026-11-05', type: '讲座', title: 'AI 与大模型技术入门', location: '信息楼报告厅', people: 152, capacity: 200, host: '计算机学院', status: '报名中', tone: 'blue', description: '带同学走进人工智能的世界，了解大模型原理与应用。' },
  { id: 'activity-6', date: '2026-11-12', type: '社团活动', title: '街舞社快闪表演', location: '中心广场', people: 45, capacity: 100, host: '街舞社', status: '即将开始', tone: 'orange', description: '一场惊喜的快闪，一起感受街舞的活力。' }
]

const mockQuestions = [
  { id: 'question-1', author: '小林同学', avatar: '林', time: '刚刚', title: '学校附近有哪些适合周末去的地方？', desc: '想和朋友在学校周边逛逛，欢迎大家分享自己的宝藏地点。', answers: 8, likes: 24, tags: ['校园周边', '周末'] },
  { id: 'question-2', author: '周周', avatar: '周', time: '1 小时前', title: '求推荐一款适合宿舍使用的打印机', desc: '主要打印课程资料，希望体积不要太大，耗材也方便购买。', answers: 5, likes: 16, tags: ['宿舍', '数码'] },
  { id: 'question-3', author: '阿南', avatar: '南', time: '昨天', title: '活动报名后临时有事不能参加怎么办？', desc: '已经报名了本周的活动，想了解一下取消报名的流程。', answers: 3, likes: 9, tags: ['活动', '报名'] },
  { id: 'question-4', author: '小陈同学', avatar: '陈', time: '2 小时前', title: '二手交易一般在哪里面交比较安全？', desc: '想出手一台旧电脑，担心安全问题，求推荐交易地点。', answers: 6, likes: 18, tags: ['二手', '安全'] },
  { id: 'question-5', author: '阿杰', avatar: '杰', time: '今天', title: '图书馆周末几点闭馆？', desc: '想趁周末去图书馆自习，问问具体的开放时间。', answers: 4, likes: 11, tags: ['图书馆', '学习'] }
]

export const campusHubApi = {
  async getHomeSummary() {
    if (USE_MOCK) return { items: mockItems.slice(0, 4), activities: mockActivities.slice(0, 3), questions: mockQuestions.slice(0, 3) }
    return withMockFallback(() => apiClient.get('/home/summary'), { items: mockItems.slice(0, 4), activities: mockActivities.slice(0, 3), questions: mockQuestions.slice(0, 2) })
  },
  async listSecondhand(params = {}) {
    if (USE_MOCK) {
      const keyword = (params.keyword || '').trim().toLowerCase()
      const category = params.category || 'all'
      return mockItems.filter((item) => (!keyword || `${item.title}${item.description}`.toLowerCase().includes(keyword)) && (category === 'all' || item.categoryKey === category))
    }
    return withMockFallback(() => apiClient.get('/secondhand/items', { params }), () => {
      const keyword = (params.keyword || '').trim().toLowerCase()
      const category = params.category || 'all'
      return mockItems.filter((item) => (!keyword || `${item.title}${item.description}`.toLowerCase().includes(keyword)) && (category === 'all' || item.categoryKey === category))
    })
  },
  async getSecondhandItem(id) {
    if (USE_MOCK) return mockItems.find((item) => item.id === id) || mockItems[0]
    return withMockFallback(() => apiClient.get(`/secondhand/items/${id}`), mockItems.find((item) => item.id === id) || mockItems[0])
  },
  async listActivities() {
    if (USE_MOCK) return mockActivities
    return withMockFallback(() => apiClient.get('/activities'), mockActivities)
  },
  async listQuestions() {
    if (USE_MOCK) return mockQuestions
    return withMockFallback(() => apiClient.get('/community/questions'), mockQuestions)
  },
  async login(payload) {
    if (USE_MOCK) return { token: 'mock-token', user: { id: 'user-1', name: payload.username || '校园用户', school: 'CampusHub 校园' } }
    return withMockFallback(() => apiClient.post('/auth/login', payload), { token: 'mock-token', user: { id: 'user-1', name: payload.username || '校园用户', school: 'CampusHub 校园' } })
  }
}
