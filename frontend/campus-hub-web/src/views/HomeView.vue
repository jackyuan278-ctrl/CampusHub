<template>
  <div class="page-container home-page">
    <section class="home-hero">
      <div class="hero-copy-block"><div class="hero-kicker"><span class="pulse-dot"></span> 今天，校园里有什么新鲜事？</div><h1>把校园生活<br /><em>连成一张网。</em></h1><p>二手好物、校园活动和同学问答，都在 CampusHub 找到彼此。</p><div class="hero-actions"><el-button type="primary" size="large" round @click="router.push('/secondhand')">去逛校园二手 <el-icon><ArrowRight /></el-icon></el-button><el-button size="large" round @click="router.push('/activities')">发现活动</el-button></div><div class="hero-trust"><span><el-icon><CircleCheck /></el-icon> 同校更方便</span><span><el-icon><Location /></el-icon> 校内好交付</span><span><el-icon><ChatDotRound /></el-icon> 真实交流</span></div></div><div class="hero-visual"><div class="hero-orbit orbit-one"></div><div class="hero-orbit orbit-two"></div><div class="hero-center-card"><span class="hero-center-icon"><el-icon :size="30"><Connection /></el-icon></span><strong>Campus<br />Hub</strong><small>发现 · 分享 · 连接</small></div><div class="floating-note note-one"><el-icon><Goods /></el-icon><span><b>{{ summary.items.length || 0 }}+</b><small>今日新物品</small></span></div><div class="floating-note note-two"><el-icon><Calendar /></el-icon><span><b>{{ summary.activities.length || 0 }}</b><small>近期活动</small></span></div></div>
    </section>

    <section class="metric-strip"><div v-for="metric in metrics" :key="metric.label" class="metric"><span class="metric-icon"><el-icon><component :is="metric.icon" /></el-icon></span><span><strong>{{ metric.value }}</strong><small>{{ metric.label }}</small></span></div></section>

    <section class="content-section"><div class="section-title-row"><div><span class="eyebrow">EXPLORE CAMPUSHUB</span><h2>从你感兴趣的地方开始</h2></div><RouterLink to="/secondhand" class="text-link">查看全部 <el-icon><ArrowRight /></el-icon></RouterLink></div><div class="entry-grid"><button v-for="entry in entries" :key="entry.title" class="entry-card" :class="entry.tone" @click="router.push(entry.to)"><span class="entry-icon"><el-icon :size="25"><component :is="entry.icon" /></el-icon></span><span class="entry-copy"><b>{{ entry.title }}</b><small>{{ entry.desc }}</small></span><el-icon class="entry-arrow"><TopRight /></el-icon></button></div></section>

    <section class="content-section split-section"><div class="section-title-row"><div><span class="eyebrow">JUST LISTED</span><h2>校园里正在流动的好物</h2></div><RouterLink to="/secondhand" class="text-link">逛逛二手市场 <el-icon><ArrowRight /></el-icon></RouterLink></div><div class="item-grid compact-grid"><SecondhandItemCard v-for="item in summary.items" :key="item.id" :item="item" @add="addToCart" @open="openItem" /></div></section>

    <section class="content-section home-columns"><div><div class="section-title-row"><div><span class="eyebrow">UPCOMING</span><h2>接下来，去参加</h2></div><RouterLink to="/activities" class="text-link">全部活动 <el-icon><ArrowRight /></el-icon></RouterLink></div><div class="mini-activity-list"><ActivityRow v-for="activity in summary.activities.slice(0, 2)" :key="activity.id" :activity="activity" @signup="signup" /></div></div><div><div class="section-title-row"><div><span class="eyebrow">COMMUNITY</span><h2>同学们在讨论</h2></div><RouterLink to="/community" class="text-link">去社区看看 <el-icon><ArrowRight /></el-icon></RouterLink></div><div class="mini-question-list"><QuestionCard v-for="question in summary.questions" :key="question.id" :question="question" /></div></div></section>

    <el-dialog v-model="detailVisible" title="物品详情" width="520px"><div v-if="selectedItem" class="detail-dialog"><div class="detail-cover" :class="selectedItem.accent"><el-icon :size="62"><Goods /></el-icon></div><div><span class="soft-tag">{{ selectedItem.category }}</span><h2>{{ selectedItem.title }}</h2><p>{{ selectedItem.description }}</p><div class="detail-price">¥{{ selectedItem.price }} <del>¥{{ selectedItem.originalPrice }}</del></div><div class="detail-meta"><span><el-icon><User /></el-icon>{{ selectedItem.seller }}</span><span><el-icon><Location /></el-icon>{{ selectedItem.location }}</span></div></div></div><template #footer><el-button @click="detailVisible = false">先看看</el-button><el-button type="primary" @click="addSelected">加入购物车</el-button></template></el-dialog>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { campusHubApi } from '@/api/campusHubApi'
import { useAppStore } from '@/stores/app'
import SecondhandItemCard from '@/components/SecondhandItemCard.vue'
import ActivityRow from '@/components/ActivityRow.vue'
import QuestionCard from '@/components/QuestionCard.vue'

const router = useRouter(); const appStore = useAppStore()
const summary = reactive({ items: [], activities: [], questions: [] }); const selectedItem = ref(null); const detailVisible = ref(false)
const entries = [{ title: '校园二手', desc: '让闲置流动起来', icon: 'Goods', tone: 'entry-mint', to: '/secondhand' }, { title: '校园活动', desc: '去见面，去参与', icon: 'Calendar', tone: 'entry-sky', to: '/activities' }, { title: '问答社区', desc: '和同学一起找到答案', icon: 'ChatDotRound', tone: 'entry-peach', to: '/community' }]
const metrics = [{ value: '24h', label: '信息持续更新', icon: 'Timer' }, { value: '同校', label: '更近的交易距离', icon: 'Location' }, { value: '一站', label: '连接校园生活', icon: 'Connection' }]
onMounted(async () => Object.assign(summary, await campusHubApi.getHomeSummary()))
function addToCart(item) { appStore.addToCart(item); ElMessage.success(`已将“${item.title}”加入购物车`) }
function openItem(item) { selectedItem.value = item; detailVisible.value = true }
function addSelected() { addToCart(selectedItem.value); detailVisible.value = false }
function signup(activity) { ElMessage.success(`已打开“${activity.title}”的活动详情`) }
</script>

