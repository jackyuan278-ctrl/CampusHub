/**
 * 开发期演示数据层。
 *
 * 设计原则：
 * 1. 仅在「后端完全连不上」或显式设置 VITE_API_MOCK=on 时生效，真实接口永远优先；
 * 2. 返回结构与后端 VO 完全一致（金额为分、ID 为字符串、时间 yyyy-MM-dd HH:mm:ss），
 *    因此切换回真实后端时页面无需改动；
 * 3. 一旦启用，页面顶部会显示明显的「演示数据」横幅，避免误判为真实数据。
 */
import { reactive } from 'vue'

const MODE = import.meta.env.VITE_API_MOCK || 'auto'

export const mockState = reactive({ active: false })

export function isMockEnabled() {
  if (MODE === 'off') return false
  if (MODE === 'on') return true
  return Boolean(import.meta.env.DEV)
}

const pad = (value) => String(value).padStart(2, '0')

function stamp(dayOffset = 0, hour = 10, minute = 0) {
  const date = new Date()
  date.setDate(date.getDate() + dayOffset)
  date.setHours(hour, minute, 0, 0)
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(hour)}:${pad(minute)}:00`
}

function sequential(prefix, index) {
  return String(Number(prefix) + index)
}

const db = {
  user: {
    id: '1001',
    username: 'demo',
    nickname: '林知夏',
    phone: '13800000000',
    avatar: '',
    balance: 126800
  },
  addresses: [
    {
      id: '2001',
      receiver: '林知夏',
      phone: '13800000000',
      province: '江西省',
      city: '南昌市',
      district: '红谷滩区',
      detail: '南昌大学前湖校区 30 栋 412',
      isDefault: 1
    },
    {
      id: '2002',
      receiver: '林知夏',
      phone: '13800000000',
      province: '江西省',
      city: '南昌市',
      district: '新建区',
      detail: '南昌大学前湖校区 图书馆驿站',
      isDefault: 0
    }
  ],
  categories: [
    { id: '11', name: '数码产品' },
    { id: '12', name: '教材书籍' },
    { id: '13', name: '生活用品' },
    { id: '14', name: '运动户外' }
  ],
  items: [
    { id: '3001', sellerId: '1002', name: '九成新机械键盘', price: 12900, image: '', categoryId: '11', categoryName: '数码产品', conditionDesc: '九成新', tradeLocation: '东门菜鸟驿站', status: 1, description: '自用半年，青轴手感正常，键帽无打油。宿舍楼下自提，可小刀。' },
    { id: '3002', sellerId: '1003', name: '高等数学教材（上下册）', price: 2500, image: '', categoryId: '12', categoryName: '教材书籍', conditionDesc: '有笔记', tradeLocation: '主教楼一层', status: 1, description: '同济第七版，上册有少量铅笔笔记，下册几乎全新，配套习题册一同出。' },
    { id: '3003', sellerId: '1004', name: '静音宿舍小风扇', price: 3500, image: '', categoryId: '13', categoryName: '生活用品', conditionDesc: '八成新', tradeLocation: '30 栋楼下', status: 1, description: '三档风力，夜间模式几乎无声。毕业清仓，可插充电宝。' },
    { id: '3004', sellerId: '1005', name: '便携显示器 15.6 寸', price: 26000, image: '', categoryId: '11', categoryName: '数码产品', conditionDesc: '九成新', tradeLocation: '图书馆门口', status: 1, description: 'Type-C 一线连，宿舍写代码很舒服，附送保护套。' },
    { id: '3005', sellerId: '1006', name: '考研英语真题套装', price: 3000, image: '', categoryId: '12', categoryName: '教材书籍', conditionDesc: '全新', tradeLocation: '文科楼', status: 1, description: '买重复了一套，未拆封，含近十年真题与解析。' },
    { id: '3006', sellerId: '1007', name: '羽毛球拍两支装', price: 4800, image: '', categoryId: '14', categoryName: '运动户外', conditionDesc: '八成新', tradeLocation: '体育馆 2 号门', status: 1, description: '碳素拍，拉线 24 磅，附球拍包，周末可约球试打。' },
    { id: '3007', sellerId: '1008', name: '折叠书桌（可调高度）', price: 5500, image: '', categoryId: '13', categoryName: '生活用品', conditionDesc: '九成新', tradeLocation: '32 栋楼下', status: 1, description: '宿舍床边桌，可调 3 档高度，承重良好，仅限自提。' },
    { id: '3008', sellerId: '1009', name: '降噪蓝牙耳机', price: 15900, image: '', categoryId: '11', categoryName: '数码产品', conditionDesc: '九五新', tradeLocation: '一食堂门口', status: 1, description: '换代出，续航 30 小时，配件盒齐全，可现场试听。' }
  ],
  cart: { 3001: 1, 3004: 1 },
  orders: [
    {
      id: '8101',
      totalFee: 8000,
      paymentType: 1,
      status: 4,
      receiver: '林知夏',
      phone: '13800000000',
      address: '江西省南昌市红谷滩区南昌大学前湖校区 30 栋 412',
      remark: '周三下午在图书馆门口面交',
      createTime: stamp(-6, 20, 12),
      payTime: stamp(-6, 20, 15),
      goods: [
        { itemId: '3002', name: '高等数学教材（上下册）', price: 2500, num: 1, image: '', spec: '有笔记' },
        { itemId: '3007', name: '折叠书桌（可调高度）', price: 5500, num: 1, image: '', spec: '九成新' }
      ]
    }
  ],
  activities: [
    { id: '4001', title: '周末前湖骑行：环艾溪湖 20 公里', description: '校自行车协会组织的周末骑行，全程约 20 公里，配保障车。新手可参加，需自备头盔。', coverUrl: '', location: '学校南门集合', startTime: stamp(3, 8, 30), endTime: stamp(3, 12, 0), signupStartTime: stamp(-2, 9, 0), signupEndTime: stamp(3, 8, 0), quota: 30, status: 1, publisherId: '1002' },
    { id: '4002', title: '开源技术分享会：从 0 写一个 RPC 框架', description: '计算机学院技术沙龙，讲透序列化、动态代理与网络通信，现场有手写代码环节。', coverUrl: '', location: '信息楼 B201', startTime: stamp(1, 19, 0), endTime: stamp(1, 21, 0), signupStartTime: stamp(-5, 9, 0), signupEndTime: stamp(1, 17, 0), quota: 120, status: 1, publisherId: '1003' },
    { id: '4003', title: '校园歌手大赛初赛', description: '一年一度的校园歌手大赛，初赛开放 200 个观众席位，扫码入场。', coverUrl: '', location: '大学生活动中心', startTime: stamp(5, 18, 30), endTime: stamp(5, 21, 30), signupStartTime: stamp(-1, 9, 0), signupEndTime: stamp(5, 12, 0), quota: 200, status: 1, publisherId: '1004' },
    { id: '4004', title: '考研经验分享：专业课如何三轮复习', description: '上岸学长学姐面对面，讲时间线、资料选择与心态调整。', coverUrl: '', location: '图书馆报告厅', startTime: stamp(-3, 19, 0), endTime: stamp(-3, 21, 0), signupStartTime: stamp(-10, 9, 0), signupEndTime: stamp(-4, 18, 0), quota: 80, status: 3, publisherId: '1005' },
    { id: '4005', title: '校园义务植树活动', description: '后勤集团联合志愿协会，在湖边种下 100 棵树苗，提供工具与手套。', coverUrl: '', location: '北区湖岸', startTime: stamp(4, 9, 0), endTime: stamp(4, 11, 30), signupStartTime: stamp(-2, 9, 0), signupEndTime: stamp(4, 8, 0), quota: 60, status: 1, publisherId: '1006' }
  ],
  signups: [
    { id: '8201', activityId: '4002', status: 1, createTime: stamp(-2, 21, 10) },
    { id: '8202', activityId: '4003', status: 2, createTime: stamp(-1, 9, 30) }
  ],
  checkinDates: [stamp(-1, 9, 0).slice(0, 10), stamp(-2, 9, 0).slice(0, 10), stamp(-3, 9, 0).slice(0, 10)],
  tags: [
    { id: '501', name: '校园周边', questionCount: 28 },
    { id: '502', name: '宿舍生活', questionCount: 21 },
    { id: '503', name: '课程学习', questionCount: 17 },
    { id: '504', name: '活动报名', questionCount: 12 },
    { id: '505', name: '求职实习', questionCount: 9 },
    { id: '506', name: '考研', questionCount: 8 }
  ],
  questions: [
    { id: '6001', userId: '1002', authorNickname: '陈屿', authorAvatar: '', title: '前湖校区哪里的打印店最便宜？', content: '这周要打印课程设计报告，大概 80 页双面，想问问大家平时都去哪家，价格大概多少。', tags: ['校园周边'], viewCount: 328, likeCount: 24, answerCount: 2, commentCount: 0, liked: false, createTime: stamp(-1, 16, 20) },
    { id: '6002', userId: '1003', authorNickname: '苏晚', authorAvatar: '', title: '大二下想找 Java 后端日常实习，项目该怎么准备？', content: '目前会 SSM，做过一个单体项目，投了几家都没回音。想知道简历里项目部分应该写到什么颗粒度，面试一般问多深。', tags: ['求职实习', '课程学习'], viewCount: 892, likeCount: 76, answerCount: 2, commentCount: 0, liked: false, createTime: stamp(-2, 10, 5) },
    { id: '6003', userId: '1004', authorNickname: '许一鸣', authorAvatar: '', title: '宿舍限电 800W，能用小功率电煮锅吗？', content: '想买个 600W 的电煮锅煮面，会不会触发跳闸？有同学在宿舍用过吗？', tags: ['宿舍生活'], viewCount: 561, likeCount: 33, answerCount: 1, commentCount: 0, liked: false, createTime: stamp(-3, 21, 40) },
    { id: '6004', userId: '1005', authorNickname: '周舟', authorAvatar: '', title: '校园卡丢了去哪里补办？需要带什么材料？', content: '昨天在二食堂丢了校园卡，补办流程和费用大概是多少，能不能当天拿到？', tags: ['校园周边'], viewCount: 214, likeCount: 15, answerCount: 0, commentCount: 0, liked: false, createTime: stamp(-4, 8, 15) }
  ],
  answers: [
    { id: '7001', questionId: '6001', userId: '1006', authorNickname: '李清和', authorAvatar: '', content: '一食堂后面那家「文印中心」，双面 0.15 元一面，80 页大概 12 块，可以微信传文件，半小时取。', likeCount: 18, commentCount: 1, liked: false, createTime: stamp(-1, 17, 2) },
    { id: '7002', questionId: '6001', userId: '1007', authorNickname: '赵小满', authorAvatar: '', content: '图书馆一楼的自主打印更便宜，0.1 元一面，但机器少，考试周要排队。', likeCount: 11, commentCount: 0, liked: false, createTime: stamp(-1, 18, 30) },
    { id: '7003', questionId: '6002', userId: '1008', authorNickname: '顾南', authorAvatar: '', content: '中小厂日常实习更看项目能不能自圆其说：选一个模块讲清「为什么这么设计」，比如订单超时关单为什么用延迟消息而不是定时扫表。八股问到第一层就够，项目能深挖两层就赢一半。', likeCount: 42, commentCount: 2, liked: false, createTime: stamp(-2, 12, 10) },
    { id: '7004', questionId: '6002', userId: '1009', authorNickname: '沈亦', authorAvatar: '', content: '补充一点：简历上别写「参与」，写你独立负责的部分和量化结果，比如接口 P99 从 800ms 降到 120ms。', likeCount: 27, commentCount: 0, liked: false, createTime: stamp(-2, 15, 45) },
    { id: '7005', questionId: '6003', userId: '1010', authorNickname: '王一诺', authorAvatar: '', content: '600W 一般没问题，但别和吹风机、热水壶同时用。我们宿舍用了两年，跳过一次闸。', likeCount: 9, commentCount: 0, liked: false, createTime: stamp(-3, 22, 5) }
  ],
  comments: [
    { id: '8001', answerId: '7001', userId: '1002', authorNickname: '陈屿', authorAvatar: '', content: '收到，谢谢！明天就去试试。', createTime: stamp(-1, 17, 20) },
    { id: '8002', answerId: '7003', userId: '1003', authorNickname: '苏晚', authorAvatar: '', content: '「为什么这么设计」这个角度一下点醒我了，感谢学长。', createTime: stamp(-2, 13, 0) },
    { id: '8003', answerId: '7003', userId: '1011', authorNickname: '何夕', authorAvatar: '', content: '同问，延迟消息那块有推荐的文章吗？', createTime: stamp(-2, 14, 20) }
  ],
  follows: []
}

let idSeed = 9000
function nextId() {
  idSeed += 1
  return String(idSeed)
}

function pageOf(list, page = 1, pageSize = 10) {
  const current = Math.max(1, Number(page) || 1)
  const size = Math.max(1, Number(pageSize) || 10)
  const start = (current - 1) * size
  return { total: String(list.length), list: list.slice(start, start + size), page: current, pageSize: size }
}

function cartList() {
  return Object.entries(db.cart).map(([itemId, num]) => {
    const item = db.items.find((entry) => entry.id === itemId) || {}
    return {
      itemId,
      name: item.name || '商品已下架',
      price: item.price || 0,
      image: item.image || '',
      spec: item.conditionDesc || '',
      num
    }
  })
}

function questionVO(question) {
  const answers = db.answers.filter((answer) => answer.questionId === question.id)
  return {
    ...question,
    answerCount: answers.length,
    tags: question.tags || []
  }
}

function activityVO(activity) {
  const signed = db.signups.filter((entry) => entry.activityId === activity.id && entry.status !== 3)
  const used = signed.filter((entry) => entry.status === 1).length
  const mine = db.signups.find((entry) => entry.activityId === activity.id && entry.status !== 3)
  return {
    ...activity,
    remainingQuota: Math.max(0, activity.quota - used),
    mySignupStatus: mine ? mine.status : null
  }
}

function checkinVO() {
  const today = stamp(0, 9, 0).slice(0, 10)
  const signedToday = db.checkinDates.includes(today)
  return {
    signedToday,
    streak: db.checkinDates.length ? Math.min(db.checkinDates.length, 7) : 0,
    checkinDates: [...db.checkinDates].sort()
  }
}

function orderVO(order) {
  return { ...order, goods: order.goods.map((goods) => ({ ...goods })) }
}

function fail(message, code = 400) {
  const error = new Error(message)
  error.mockCode = code
  throw error
}

/** 路由表：method + 正则，顺序敏感（具体路径写在参数路径之前） */
const routes = [
  // ---------- 用户 ----------
  ['post', /^\/users\/login$/, ({ body }) => {
    if (!body?.username || !body?.password) fail('请输入账号和密码')
    if (!db.user.username) fail('账号或密码错误')
    return { token: 'mock-token-campushub', userId: db.user.id, username: body.username || db.user.username }
  }],
  ['post', /^\/users\/register$/, ({ body }) => {
    if (!body?.username || !body?.phone || !body?.password) fail('请填写完整的注册信息')
    db.user.username = body.username
    db.user.phone = body.phone
    return null
  }],
  ['get', /^\/users\/me$/, () => ({ ...db.user })],
  ['put', /^\/users\/me$/, ({ body }) => {
    Object.assign(db.user, {
      nickname: body?.nickname || db.user.nickname,
      phone: body?.phone || db.user.phone,
      avatar: body?.avatar ?? db.user.avatar
    })
    return null
  }],
  ['put', /^\/users\/password$/, ({ body }) => {
    if (!body?.oldPassword || !body?.newPassword) fail('请填写原密码与新密码')
    return null
  }],
  ['get', /^\/users\/addresses$/, () => db.addresses.map((entry) => ({ ...entry }))],
  ['post', /^\/users\/addresses$/, ({ body }) => {
    const address = { ...body, id: nextId(), isDefault: body?.isDefault ? 1 : 0 }
    if (address.isDefault) db.addresses.forEach((entry) => { entry.isDefault = 0 })
    db.addresses.push(address)
    return null
  }],
  ['put', /^\/users\/addresses\/([^/]+)\/default$/, ({ params: [id] }) => {
    db.addresses.forEach((entry) => { entry.isDefault = entry.id === id ? 1 : 0 })
    return null
  }],
  ['put', /^\/users\/addresses\/([^/]+)$/, ({ params: [id], body }) => {
    const target = db.addresses.find((entry) => entry.id === id)
    if (!target) fail('地址不存在')
    Object.assign(target, body)
    return null
  }],
  ['delete', /^\/users\/addresses\/([^/]+)$/, ({ params: [id] }) => {
    db.addresses = db.addresses.filter((entry) => entry.id !== id)
    return null
  }],

  // ---------- 二手商品 ----------
  ['get', /^\/items\/hot$/, () => db.items.slice(0, 6).map((entry) => ({ ...entry }))],
  ['get', /^\/items\/categories$/, () => db.categories.map((entry) => ({ ...entry }))],
  ['get', /^\/items$/, ({ query }) => {
    let list = db.items.filter((entry) => entry.status === 1)
    if (query.keyword) list = list.filter((entry) => entry.name.includes(query.keyword))
    if (query.categoryId) list = list.filter((entry) => entry.categoryId === String(query.categoryId))
    if (query.sort === 'priceAsc') list = [...list].sort((a, b) => a.price - b.price)
    if (query.sort === 'priceDesc') list = [...list].sort((a, b) => b.price - a.price)
    const pageSize = Number(query.pageSize) || 12
    if (query.cursorId) {
      const index = list.findIndex((entry) => entry.id === String(query.cursorId))
      const start = index + 1
      const slice = list.slice(start, start + pageSize)
      return {
        list: slice,
        hasMore: start + pageSize < list.length,
        nextCursor: slice.length ? slice[slice.length - 1].id : null,
        nextCursorSortValue: query.sort ? slice[slice.length - 1]?.price ?? null : null
      }
    }
    return { ...pageOf(list, query.page, pageSize), hasMore: false }
  }],
  ['get', /^\/items\/([^/]+)$/, ({ params: [id] }) => {
    const item = db.items.find((entry) => entry.id === id)
    if (!item) fail('商品不存在')
    return { ...item }
  }],

  // ---------- 购物车 ----------
  ['get', /^\/cart$/, () => cartList()],
  ['post', /^\/cart$/, ({ body }) => {
    const item = db.items.find((entry) => entry.id === String(body?.itemId))
    if (!item) fail('商品不存在')
    const num = Math.min(99, (db.cart[item.id] || 0) + (Number(body.num) || 1))
    db.cart[item.id] = num
    return null
  }],
  ['put', /^\/cart\/([^/]+)$/, ({ params: [itemId], body }) => {
    if (!db.cart[itemId]) fail('购物车条目不存在')
    db.cart[itemId] = Math.min(99, Number(body?.num) || 1)
    return null
  }],
  ['delete', /^\/cart\/([^/]+)$/, ({ params: [itemId] }) => {
    delete db.cart[itemId]
    return null
  }],
  ['delete', /^\/cart$/, () => {
    db.cart = {}
    return null
  }],

  // ---------- 订单与支付 ----------
  ['post', /^\/orders$/, ({ body }) => {
    if (!body?.receiver || !body?.phone || !body?.address) fail('收货信息不完整')
    const goods = (body.goods || []).map((entry) => {
      const item = db.items.find((row) => row.id === String(entry.itemId)) || {}
      return {
        itemId: String(entry.itemId),
        name: item.name || entry.name,
        price: item.price ?? entry.price,
        num: Number(entry.num) || 1,
        image: item.image || '',
        spec: item.conditionDesc || ''
      }
    })
    if (!goods.length) fail('下单商品不能为空')
    const totalFee = goods.reduce((sum, entry) => sum + entry.price * entry.num, 0)
    const order = {
      id: nextId(),
      totalFee,
      paymentType: null,
      status: 1,
      receiver: body.receiver,
      phone: body.phone,
      address: body.address,
      remark: body.remark || '',
      createTime: stamp(0, new Date().getHours(), new Date().getMinutes()),
      payTime: null,
      goods
    }
    db.orders.unshift(order)
    return orderVO(order)
  }],
  ['get', /^\/orders$/, () => db.orders.map(orderVO)],
  ['get', /^\/orders\/([^/]+)$/, ({ params: [id] }) => {
    const order = db.orders.find((entry) => entry.id === id)
    if (!order) fail('订单不存在')
    return orderVO(order)
  }],
  ['post', /^\/pay\/orders\/([^/]+)\/pay$/, ({ params: [id], body }) => {
    const order = db.orders.find((entry) => entry.id === id)
    if (!order) fail('订单不存在')
    if (order.status !== 1) fail('订单状态不允许支付')
    const payType = body?.payType
    if (payType === 'balance') {
      if (db.user.balance < order.totalFee) fail('余额不足')
      db.user.balance -= order.totalFee
      order.paymentType = 1
    } else {
      order.paymentType = 2
    }
    order.status = 2
    order.payTime = stamp(0, new Date().getHours(), new Date().getMinutes())
    return null
  }],

  // ---------- 活动与报名签到 ----------
  ['get', /^\/activities\/search$/, ({ query }) => {
    const keyword = String(query.keyword || '').trim()
    if (!keyword) return { total: '0', list: [] }
    const hit = db.activities.filter((entry) => `${entry.title}${entry.description}${entry.location}`.includes(keyword))
    return {
      ...pageOf(hit.map((entry) => ({
        id: entry.id,
        title: entry.title,
        description: entry.description,
        location: entry.location,
        startTime: entry.startTime,
        status: entry.status,
        coverUrl: entry.coverUrl,
        highlights: { title: [entry.title.replace(keyword, `<em>${keyword}</em>`)] }
      })), query.page, query.pageSize)
    }
  }],
  ['get', /^\/activities$/, ({ query }) => {
    let list = db.activities
    if (query.status) list = list.filter((entry) => String(entry.status) === String(query.status))
    if (query.keyword) list = list.filter((entry) => `${entry.title}${entry.description}`.includes(query.keyword))
    list = [...list].sort((a, b) => a.startTime.localeCompare(b.startTime))
    return pageOf(list.map(activityVO), query.page, query.pageSize)
  }],
  ['post', /^\/activities$/, ({ body }) => {
    const activity = {
      id: nextId(),
      title: body?.title,
      description: body?.description,
      coverUrl: body?.coverUrl || '',
      location: body?.location,
      startTime: body?.startTime,
      endTime: body?.endTime,
      signupStartTime: body?.signupStartTime,
      signupEndTime: body?.signupEndTime,
      quota: Number(body?.quota) || 1,
      status: 1,
      publisherId: db.user.id
    }
    db.activities.unshift(activity)
    return activity.id
  }],
  ['put', /^\/activities\/([^/]+)\/cancel$/, ({ params: [id] }) => {
    const activity = db.activities.find((entry) => entry.id === id)
    if (!activity) fail('活动不存在')
    activity.status = 4
    return null
  }],
  ['get', /^\/activities\/([^/]+)$/, ({ params: [id] }) => {
    const activity = db.activities.find((entry) => entry.id === id)
    if (!activity) fail('活动不存在')
    return activityVO(activity)
  }],
  ['post', /^\/signups$/, ({ body }) => {
    const activity = db.activities.find((entry) => entry.id === String(body?.activityId))
    if (!activity) fail('活动不存在')
    const exist = db.signups.find((entry) => entry.activityId === activity.id)
    if (exist && exist.status !== 3) fail('你已经报名该活动')
    if (activity.quota - db.signups.filter((entry) => entry.activityId === activity.id && entry.status === 1).length <= 0) {
      fail('名额已满，可以加入候补')
    }
    if (exist) {
      exist.status = 1
      exist.createTime = stamp(0, 12, 0)
    } else {
      db.signups.push({ id: nextId(), activityId: activity.id, status: 1, createTime: stamp(0, 12, 0) })
    }
    return null
  }],
  ['post', /^\/signups\/waitlist$/, ({ body }) => {
    const activity = db.activities.find((entry) => entry.id === String(body?.activityId))
    if (!activity) fail('活动不存在')
    const exist = db.signups.find((entry) => entry.activityId === activity.id)
    if (exist && exist.status !== 3) fail('你已在报名或候补中')
    if (exist) {
      exist.status = 2
      exist.createTime = stamp(0, 12, 30)
    } else {
      db.signups.push({ id: nextId(), activityId: activity.id, status: 2, createTime: stamp(0, 12, 30) })
    }
    return null
  }],
  ['get', /^\/signups\/mine$/, () => db.signups.map((entry) => {
    const activity = db.activities.find((row) => row.id === entry.activityId) || {}
    const queue = db.signups.filter((row) => row.activityId === entry.activityId && row.status === 2)
    return {
      id: entry.id,
      activityId: entry.activityId,
      activityTitle: activity.title || '活动已下架',
      coverUrl: activity.coverUrl || '',
      location: activity.location || '',
      startTime: activity.startTime || '',
      status: entry.status,
      waitPosition: entry.status === 2 ? queue.findIndex((row) => row.id === entry.id) + 1 : null,
      createTime: entry.createTime
    }
  })],
  ['delete', /^\/signups\/([^/]+)$/, ({ params: [activityId] }) => {
    const exist = db.signups.find((entry) => entry.activityId === activityId)
    if (!exist) fail('未找到报名记录')
    exist.status = 3
    return null
  }],
  ['post', /^\/checkins$/, () => {
    const today = stamp(0, 9, 0).slice(0, 10)
    if (!db.checkinDates.includes(today)) db.checkinDates.push(today)
    return checkinVO()
  }],
  ['get', /^\/checkins\/mine$/, () => checkinVO()],

  // ---------- 社区 ----------
  ['get', /^\/tags$/, () => db.tags.map((entry) => ({ ...entry }))],
  ['get', /^\/questions\/hot$/, ({ query }) => {
    const top = Number(query.top) || 10
    return [...db.questions]
      .sort((a, b) => b.likeCount + b.viewCount / 10 - (a.likeCount + a.viewCount / 10))
      .slice(0, top)
      .map(questionVO)
  }],
  ['post', /^\/questions$/, ({ body }) => {
    if (!body?.title || !body?.content) fail('标题和内容不能为空')
    const tagNames = (body.tagIds || []).map((id) => db.tags.find((entry) => String(entry.id) === String(id))?.name).filter(Boolean)
    const question = {
      id: nextId(),
      userId: db.user.id,
      authorNickname: db.user.nickname,
      authorAvatar: db.user.avatar,
      title: body.title,
      content: body.content,
      tags: tagNames,
      viewCount: 0,
      likeCount: 0,
      answerCount: 0,
      commentCount: 0,
      liked: false,
      createTime: stamp(0, new Date().getHours(), new Date().getMinutes())
    }
    db.questions.unshift(question)
    return question.id
  }],
  ['get', /^\/questions$/, ({ query }) => {
    let list = [...db.questions]
    if (query.keyword) list = list.filter((entry) => `${entry.title}${entry.content}${(entry.tags || []).join('')}`.includes(query.keyword))
    if (query.tagId) {
      const name = db.tags.find((entry) => String(entry.id) === String(query.tagId))?.name
      if (name) list = list.filter((entry) => (entry.tags || []).includes(name))
    }
    if (query.sort === 'hot') list.sort((a, b) => b.likeCount - a.likeCount)
    else list.sort((a, b) => b.createTime.localeCompare(a.createTime))
    return pageOf(list.map(questionVO), query.page, query.pageSize)
  }],
  ['post', /^\/questions\/([^/]+)\/like$/, ({ params: [id] }) => {
    const question = db.questions.find((entry) => entry.id === id)
    if (!question) fail('问题不存在')
    if (!question.liked) {
      question.liked = true
      question.likeCount += 1
    }
    return null
  }],
  ['delete', /^\/questions\/([^/]+)\/like$/, ({ params: [id] }) => {
    const question = db.questions.find((entry) => entry.id === id)
    if (!question) fail('问题不存在')
    if (question.liked) {
      question.liked = false
      question.likeCount = Math.max(0, question.likeCount - 1)
    }
    return null
  }],
  ['get', /^\/questions\/([^/]+)\/answers$/, ({ params: [questionId] }) => db.answers
    .filter((entry) => entry.questionId === questionId)
    .map((entry) => ({ ...entry }))],
  ['post', /^\/questions\/([^/]+)\/answers$/, ({ params: [questionId], body }) => {
    if (!body?.content) fail('回答内容不能为空')
    const answer = {
      id: nextId(),
      questionId,
      userId: db.user.id,
      authorNickname: db.user.nickname,
      authorAvatar: db.user.avatar,
      content: body.content,
      likeCount: 0,
      commentCount: 0,
      liked: false,
      createTime: stamp(0, new Date().getHours(), new Date().getMinutes())
    }
    db.answers.unshift(answer)
    return answer.id
  }],
  ['get', /^\/questions\/([^/]+)$/, ({ params: [id] }) => {
    const question = db.questions.find((entry) => entry.id === id)
    if (!question) fail('问题不存在')
    return questionVO(question)
  }],
  ['post', /^\/answers\/([^/]+)\/like$/, ({ params: [id] }) => {
    const answer = db.answers.find((entry) => entry.id === id)
    if (!answer) fail('回答不存在')
    if (!answer.liked) {
      answer.liked = true
      answer.likeCount += 1
    }
    return null
  }],
  ['delete', /^\/answers\/([^/]+)\/like$/, ({ params: [id] }) => {
    const answer = db.answers.find((entry) => entry.id === id)
    if (!answer) fail('回答不存在')
    if (answer.liked) {
      answer.liked = false
      answer.likeCount = Math.max(0, answer.likeCount - 1)
    }
    return null
  }],
  ['get', /^\/answers\/([^/]+)\/comments$/, ({ params: [answerId] }) => db.comments
    .filter((entry) => entry.answerId === answerId)
    .map((entry) => ({ ...entry }))],
  ['post', /^\/answers\/([^/]+)\/comments$/, ({ params: [answerId], body }) => {
    if (!body?.content) fail('评论内容不能为空')
    const comment = {
      id: nextId(),
      answerId,
      userId: db.user.id,
      authorNickname: db.user.nickname,
      authorAvatar: db.user.avatar,
      content: body.content,
      createTime: stamp(0, new Date().getHours(), new Date().getMinutes())
    }
    db.comments.push(comment)
    const answer = db.answers.find((entry) => entry.id === answerId)
    if (answer) answer.commentCount += 1
    return comment.id
  }],
  ['get', /^\/follows\/mine$/, () => db.follows.map((userId) => ({
    id: userId,
    username: `user_${userId}`,
    nickname: `同学 ${userId.slice(-3)}`,
    avatarUrl: '',
    role: 0
  }))],
  ['post', /^\/follows\/([^/]+)$/, ({ params: [userId] }) => {
    if (!db.follows.includes(userId)) db.follows.push(userId)
    return null
  }],
  ['delete', /^\/follows\/([^/]+)$/, ({ params: [userId] }) => {
    db.follows = db.follows.filter((entry) => entry !== userId)
    return null
  }]
]

function parseConfig(config) {
  const rawUrl = config.url || ''
  // 社区接口经网关 StripPrefix=2，浏览器侧是 /community/questions，这里对齐成服务内路径
  const url = rawUrl.split('?')[0].replace(/^\/community(?=\/)/, '')
  let query = { ...(config.params || {}) }
  const search = rawUrl.split('?')[1]
  if (search) {
    new URLSearchParams(search).forEach((value, key) => { query[key] = value })
  }
  let body = config.data
  if (typeof body === 'string') {
    try { body = JSON.parse(body) } catch { body = null }
  }
  return { method: String(config.method || 'get').toLowerCase(), url, query, body }
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

/**
 * 尝试用演示数据响应请求；未命中路由时返回 undefined，由调用方抛出原始网络错误。
 */
export async function resolveMock(config) {
  const { method, url, query, body } = parseConfig(config)
  const route = routes.find(([verb, pattern]) => verb === method && pattern.test(url))
  if (!route) return undefined
  const matched = url.match(route[1])
  await sleep(120)
  try {
    const data = route[2]({ params: (matched || []).slice(1), query, body })
    mockState.active = true
    return data === undefined ? null : data
  } catch (error) {
    if (error.mockCode) {
      // 模拟后端业务错误：与真实 Result 一致地抛出，页面逻辑无需区分
      const apiError = new Error(error.message)
      apiError.code = error.mockCode
      mockState.active = true
      throw apiError
    }
    throw error
  }
}
