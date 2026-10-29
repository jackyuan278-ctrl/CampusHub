<template>
  <div class="page-container home-page">
    <section class="home-hero">
      <div class="hero-copy-block">
        <div class="hero-kicker"><span class="pulse-dot"></span> 今天，校园里有什么新鲜事？</div>
        <h1>把校园生活<br /><em>连成一张网。</em></h1>
        <p>二手好物、校园活动和同学问答，都在 CampusHub 找到彼此。</p>
        <div class="hero-actions">
          <el-button type="primary" size="large" round @click="router.push('/secondhand')">
            去逛校园二手 <el-icon><ArrowRight /></el-icon>
          </el-button>
          <el-button size="large" round @click="router.push('/activities')">发现活动</el-button>
        </div>
        <div class="hero-trust">
          <span><el-icon><CircleCheck /></el-icon> 同校更方便</span>
          <span><el-icon><Location /></el-icon> 校内好交付</span>
          <span><el-icon><ChatDotRound /></el-icon> 真实交流</span>
        </div>
      </div>
      <div class="hero-visual">
        <div class="hero-orbit orbit-one"></div>
        <div class="hero-orbit orbit-two"></div>
        <div class="hero-center-card">
          <span class="hero-center-icon"><el-icon :size="30"><Connection /></el-icon></span>
          <strong>Campus<br />Hub</strong>
          <small>发现 · 分享 · 连接</small>
        </div>
        <div class="floating-note note-one">
          <el-icon><Goods /></el-icon>
          <span><b>{{ hotItems.length }}</b><small>正在热卖</small></span>
        </div>
        <div class="floating-note note-two">
          <el-icon><Calendar /></el-icon>
          <span><b>{{ activityTotal }}</b><small>近期活动</small></span>
        </div>
      </div>
    </section>

    <section class="metric-strip four">
      <div v-for="metric in metrics" :key="metric.label" class="metric">
        <span class="metric-icon"><el-icon><component :is="metric.icon" /></el-icon></span>
        <span><strong>{{ metric.value }}</strong><small>{{ metric.label }}</small></span>
      </div>
    </section>

    <section class="content-section">
      <div class="section-title-row">
        <div><span class="eyebrow">EXPLORE CAMPUSHUB</span><h2>从你感兴趣的地方开始</h2></div>
        <RouterLink to="/secondhand" class="text-link">查看全部 <el-icon><ArrowRight /></el-icon></RouterLink>
      </div>
      <div class="entry-grid">
        <button v-for="entry in entries" :key="entry.title" class="entry-card" :class="entry.tone" @click="router.push(entry.to)">
          <span class="entry-icon"><el-icon :size="25"><component :is="entry.icon" /></el-icon></span>
          <span class="entry-copy"><b>{{ entry.title }}</b><small>{{ entry.desc }}</small></span>
          <el-icon class="entry-arrow"><TopRight /></el-icon>
        </button>
      </div>
    </section>

    <section class="content-section split-section">
      <div class="section-title-row">
        <div><span class="eyebrow">JUST LISTED</span><h2>校园里正在流动的好物</h2></div>
        <RouterLink to="/secondhand" class="text-link">逛逛二手市场 <el-icon><ArrowRight /></el-icon></RouterLink>
      </div>

      <StatePanel v-if="loading" type="loading" class="panel-offset" />
      <StatePanel v-else-if="error" type="error" title="校园数据没能加载出来" :description="error" action-text="重新加载" class="panel-offset" @action="load" />
      <div v-else-if="hotItems.length" class="product-grid home-grid">
        <ItemCard v-for="item in hotItems" :key="item.id" :item="item" @open="openItem" @add="addToCart" />
      </div>
      <StatePanel v-else type="empty" title="暂时还没有在售物品" description="发布一件闲置，让它在校园里继续流动。" class="panel-offset" />
    </section>

    <section class="content-section home-columns">
      <div>
        <div class="section-title-row">
          <div><span class="eyebrow">UPCOMING</span><h2>接下来，去参加</h2></div>
          <RouterLink to="/activities" class="text-link">全部活动 <el-icon><ArrowRight /></el-icon></RouterLink>
        </div>
        <div class="activity-list">
          <ActivityRow v-for="activity in activities" :key="activity.id" :activity="activity" @open="openActivity" />
        </div>
        <StatePanel v-if="!loading && !activities.length" type="empty" title="暂无近期活动" />
      </div>
      <div>
        <div class="section-title-row">
          <div><span class="eyebrow">COMMUNITY</span><h2>同学们在讨论</h2></div>
          <RouterLink to="/community" class="text-link">去社区看看 <el-icon><ArrowRight /></el-icon></RouterLink>
        </div>
        <div class="mini-question-list">
          <QuestionCard v-for="question in questions" :key="question.id" :question="question" @open="openQuestion" />
        </div>
        <StatePanel v-if="communityError && !communityLoading" type="error" title="社区内容没能加载出来" :description="communityError" action-text="重新加载" @action="reloadCommunity" />
        <StatePanel v-else-if="!loading && !communityLoading && !questions.length" type="empty" title="社区还很安静" description="第一个提问的人，往往最受欢迎。" />
      </div>
    </section>

    <section v-if="topics.length" class="content-section">
      <div class="section-title-row">
        <div><span class="eyebrow">TOPICS</span><h2>大家最近在聊</h2></div>
        <RouterLink to="/community" class="text-link">进入话题广场 <el-icon><ArrowRight /></el-icon></RouterLink>
      </div>
      <div class="topic-cloud">
        <button v-for="tag in topics" :key="tag.name" class="topic-chip" @click="openTopic(tag)">
          <span># {{ tag.name }}</span>
          <small>{{ tag.questionCount || 0 }} 个问题</small>
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { activityApi } from '@/api/activity'
import { itemApi } from '@/api/item'
import { questionApi, tagApi } from '@/api/community'
import { useAddToCart } from '@/composables/useAddToCart'
import ItemCard from '@/components/ItemCard.vue'
import ActivityRow from '@/components/ActivityRow.vue'
import QuestionCard from '@/components/QuestionCard.vue'
import StatePanel from '@/components/StatePanel.vue'

const router = useRouter()
const { addToCart } = useAddToCart()

const loading = ref(true)
const error = ref('')
const hotItems = ref([])
const activities = ref([])
const questions = ref([])
const tags = ref([])
const activityTotal = ref(0)
const communityError = ref('')
const communityLoading = ref(false)

// 标签接口偶尔为空（新库/话题还没建）：那就用问题里带的标签兜底，别让整个区块消失
const topics = computed(() => {
  if (tags.value.length) return tags.value.slice(0, 6)
  const counter = new Map()
  questions.value.forEach((question) => {
    ;(question.tags || []).forEach((name) => counter.set(name, (counter.get(name) || 0) + 1))
  })
  return [...counter.entries()].map(([name, questionCount]) => ({ name, questionCount }))
})

const entries = [
  { title: '校园二手', desc: '让闲置流动起来', icon: 'Goods', tone: 'entry-mint', to: '/secondhand' },
  { title: '校园活动', desc: '去见面，去参与', icon: 'Calendar', tone: 'entry-sky', to: '/activities' },
  { title: '问答社区', desc: '和同学一起找到答案', icon: 'ChatDotRound', tone: 'entry-peach', to: '/community' }
]

const metrics = computed(() => [
  { value: hotItems.value.length, label: '正在热卖的好物', icon: 'Goods' },
  { value: activityTotal.value, label: '可报名的活动', icon: 'Calendar' },
  { value: topics.value.length, label: '社区话题', icon: 'CollectionTag' },
  { value: '同校', label: '更近的交易距离', icon: 'Location' }
])

/** 社区问题：热榜优先；热榜空了（新库 / 热度账本缺货）就回退按热度分页，保证首页这块有内容 */
async function loadQuestions() {
  communityLoading.value = true
  communityError.value = ''
  try {
    const hot = await questionApi.hot(4, { silent: true })
    if (Array.isArray(hot) && hot.length) return hot
    const page = await questionApi.page({ page: 1, pageSize: 4, sort: 'hot' }, { silent: true })
    return page?.list || []
  } catch (err) {
    communityError.value = err?.message || '社区服务暂时不可用'
    return []
  } finally {
    communityLoading.value = false
  }
}

async function reloadCommunity() {
  questions.value = await loadQuestions()
  if (!communityError.value) {
    const tagList = await tagApi.list({ silent: true }).catch(() => [])
    tags.value = Array.isArray(tagList) ? tagList : []
  }
}

async function load() {
  loading.value = true
  error.value = ''
  // 四个板块各自独立：任何一块挂了都只降级自己，不能把整页拖空
  const [itemsResult, activityResult, questionResult, tagResult] = await Promise.allSettled([
    itemApi.hot({ silent: true }),
    activityApi.page({ page: 1, pageSize: 3, status: 1 }, { silent: true }),
    loadQuestions(),
    tagApi.list({ silent: true })
  ])
  hotItems.value = itemsResult.status === 'fulfilled' && Array.isArray(itemsResult.value) ? itemsResult.value : []
  const activityPage = activityResult.status === 'fulfilled' ? activityResult.value : null
  activities.value = activityPage?.list || []
  activityTotal.value = Number(activityPage?.total || activities.value.length)
  questions.value = questionResult.status === 'fulfilled' && Array.isArray(questionResult.value) ? questionResult.value : []
  tags.value = tagResult.status === 'fulfilled' && Array.isArray(tagResult.value) ? tagResult.value : []
  // 只有二手好物这块挂了才提示重试，其余板块空态自己会说话
  if (itemsResult.status === 'rejected') {
    error.value = itemsResult.reason?.message || '请检查网络后重试'
  }
  loading.value = false
}

function openItem(item) {
  router.push(`/secondhand/${item.id}`)
}

function openActivity(activity) {
  router.push(`/activities/${activity.id}`)
}

function openQuestion(question) {
  router.push(`/community/${question.id}`)
}

function openTopic(tag) {
  router.push({ path: '/community', query: tag.id ? { tagId: tag.id } : {} })
}

onMounted(load)
</script>
