<template>
  <div class="page-container">
    <nav class="breadcrumb">
      <RouterLink to="/home">首页</RouterLink>
      <el-icon><ArrowRight /></el-icon>
      <RouterLink to="/activities">校园活动</RouterLink>
      <el-icon><ArrowRight /></el-icon>
      <span>{{ activity?.title || '活动详情' }}</span>
    </nav>

    <StatePanel v-if="loading" type="loading" class="panel-offset" />
    <StatePanel
      v-else-if="error"
      type="error"
      title="没能打开这场活动"
      :description="error"
      action-text="返回活动列表"
      class="panel-offset"
      @action="router.push('/activities')"
    />

    <template v-else-if="activity">
      <section class="activity-hero">
        <div class="activity-hero-copy">
          <div class="detail-tag-row">
            <span class="soft-tag">校园活动</span>
            <StatusTag :text="status.text" :tone="status.tone" />
          </div>
          <h1>{{ activity.title }}</h1>
          <div class="activity-hero-meta">
            <span><el-icon><Calendar /></el-icon>{{ formatDate(activity.startTime, true) }} - {{ formatDate(activity.endTime, true) }}</span>
            <span><el-icon><Location /></el-icon>{{ activity.location }}</span>
            <span><el-icon><User /></el-icon>{{ signedCount }} / {{ activity.quota }} 人已报名</span>
          </div>
        </div>
        <div class="activity-hero-orb">
          <b>{{ remaining }}</b>
          <small>剩余名额</small>
        </div>
      </section>

      <section class="activity-detail-layout">
        <div class="activity-detail-main">
          <div class="panel">
            <h2 class="panel-title">活动介绍</h2>
            <p class="panel-text preserve">{{ activity.description || '主办方还没有补充更多细节。' }}</p>
          </div>

          <div class="panel">
            <h2 class="panel-title">时间安排</h2>
            <div class="timeline">
              <div class="timeline-item">
                <span class="timeline-dot"></span>
                <div><b>报名开始</b><small>{{ formatDate(activity.signupStartTime, true) || '未设置' }}</small></div>
              </div>
              <div class="timeline-item">
                <span class="timeline-dot"></span>
                <div><b>报名截止</b><small>{{ formatDate(activity.signupEndTime, true) || '未设置' }}</small></div>
              </div>
              <div class="timeline-item">
                <span class="timeline-dot accent"></span>
                <div><b>活动开始</b><small>{{ formatDate(activity.startTime, true) || '未设置' }}</small></div>
              </div>
              <div class="timeline-item">
                <span class="timeline-dot"></span>
                <div><b>活动结束</b><small>{{ formatDate(activity.endTime, true) || '未设置' }}</small></div>
              </div>
            </div>
          </div>

          <div class="panel">
            <h2 class="panel-title">报名须知</h2>
            <ul class="notice-list">
              <li>名额释放后，候补同学会按排队顺序自动补位，请留意站内消息。</li>
              <li>报名成功后如无法参加，请及时取消，避免占用其他同学的名额。</li>
              <li>活动现场请配合签到，连续参与可以获得签到记录。</li>
            </ul>
          </div>
        </div>

        <aside class="activity-detail-aside">
          <div class="signup-card">
            <div class="signup-head">
              <span class="eyebrow">SIGN UP</span>
              <h3>{{ statusText }}</h3>
              <p v-if="myStatus === 1">你已报名，记得按时到场。</p>
              <p v-else-if="myStatus === 2">你在候补队列中，有名额释放会优先通知你。</p>
              <p v-else>名额有限，报名后请按时参加。</p>
            </div>

            <div class="quota-block">
              <div class="quota-numbers">
                <strong>{{ remaining }}</strong>
                <span>/ {{ activity.quota }} 剩余</span>
              </div>
              <div class="activity-progress tall"><span :style="{ width: `${percent}%` }"></span></div>
            </div>

            <el-button
              v-if="myStatus === 1 || myStatus === 2"
              type="danger"
              plain
              round
              class="full-width"
              :loading="acting"
              @click="cancelSignup"
            >
              取消{{ myStatus === 2 ? '候补' : '报名' }}
            </el-button>
            <template v-else>
              <el-button v-if="remaining > 0" type="primary" round class="full-width" :loading="acting" :disabled="!signupOpen" @click="signup">
                {{ signupOpen ? '立即报名' : '报名未开放' }}
              </el-button>
              <el-button v-else type="warning" round class="full-width" :loading="acting" @click="joinWaitlist">
                名额已满，加入候补
              </el-button>
            </template>

            <p class="signup-window">
              <el-icon><Clock /></el-icon>
              报名时间：{{ formatDate(activity.signupStartTime, true) || '—' }} 至 {{ formatDate(activity.signupEndTime, true) || '—' }}
            </p>

            <el-button v-if="mine" type="danger" text class="cancel-activity" @click="cancelActivity">取消这场活动（发布者操作）</el-button>
          </div>

          <div class="panel side-panel">
            <h3 class="panel-title">活动信息</h3>
            <dl class="meta-list">
              <div><dt>活动地点</dt><dd>{{ activity.location }}</dd></div>
              <div><dt>总名额</dt><dd>{{ activity.quota }} 人</dd></div>
              <div><dt>已报名</dt><dd>{{ signedCount }} 人</dd></div>
              <div><dt>活动编号</dt><dd>{{ activity.id }}</dd></div>
            </dl>
          </div>
        </aside>
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { activityApi, signupApi } from '@/api/activity'
import { useRequireLogin } from '@/composables/useRequireLogin'
import { ACTIVITY_STATUS, formatDate, parseTime, statusMeta } from '@/utils/format'
import { isMyActivity } from '@/utils/mine'
import StatePanel from '@/components/StatePanel.vue'
import StatusTag from '@/components/StatusTag.vue'

const route = useRoute()
const router = useRouter()
const requireLogin = useRequireLogin()

const activity = ref(null)
const loading = ref(true)
const error = ref('')
const acting = ref(false)

const status = computed(() => statusMeta(ACTIVITY_STATUS, activity.value?.status))
const statusText = computed(() => {
  if (myStatus.value === 1) return '已报名'
  if (myStatus.value === 2) return '候补中'
  if (activity.value?.status === 3) return '活动已结束'
  if (activity.value?.status === 4) return '活动已取消'
  return status.value.text
})
const myStatus = computed(() => activity.value?.mySignupStatus || null)
const remaining = computed(() => Math.max(0, activity.value?.remainingQuota ?? activity.value?.quota ?? 0))
const signedCount = computed(() => Math.max(0, (activity.value?.quota || 0) - remaining.value))
const percent = computed(() => {
  const quota = activity.value?.quota || 0
  return quota ? Math.min(100, (signedCount.value / quota) * 100) : 0
})
const mine = computed(() => (activity.value ? isMyActivity(activity.value.id) : false))
const signupOpen = computed(() => {
  if (activity.value?.status !== 1) return false
  const now = Date.now()
  const start = parseTime(activity.value.signupStartTime)
  const end = parseTime(activity.value.signupEndTime)
  if (start && now < start.getTime()) return false
  if (end && now > end.getTime()) return false
  return true
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    activity.value = await activityApi.detail(route.params.id, { silent: true })
  } catch (err) {
    error.value = err.message || '请稍后重试'
  } finally {
    loading.value = false
  }
}

async function signup() {
  if (!requireLogin('登录后才能报名活动')) return
  acting.value = true
  try {
    await signupApi.signup(activity.value.id)
    ElMessage.success('报名成功，记得按时参加')
    await load()
  } catch {
    // 统一错误提示（名额已满时后端会提示加入候补）
  } finally {
    acting.value = false
  }
}

async function joinWaitlist() {
  if (!requireLogin('登录后才能加入候补')) return
  acting.value = true
  try {
    await signupApi.joinWaitlist(activity.value.id)
    ElMessage.success('已加入候补队列，有名额释放会优先通知你')
    await load()
  } catch {
    // 统一错误提示
  } finally {
    acting.value = false
  }
}

async function cancelSignup() {
  try {
    await ElMessageBox.confirm('取消后名额会释放给候补同学，确定取消吗？', '取消报名', {
      confirmButtonText: '确定取消',
      cancelButtonText: '再想想',
      type: 'warning'
    })
  } catch {
    return
  }
  acting.value = true
  try {
    await signupApi.cancel(activity.value.id)
    ElMessage.success('已取消报名')
    await load()
  } catch {
    // 统一错误提示
  } finally {
    acting.value = false
  }
}

async function cancelActivity() {
  try {
    await ElMessageBox.confirm('取消后活动将不再展示报名入口，确定取消这场活动吗？', '取消活动', {
      confirmButtonText: '确定取消活动',
      cancelButtonText: '先不取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await activityApi.cancel(activity.value.id)
    ElMessage.success('活动已取消')
    await load()
  } catch {
    // 统一错误提示
  }
}

watch(() => route.params.id, load)
onMounted(load)
</script>
