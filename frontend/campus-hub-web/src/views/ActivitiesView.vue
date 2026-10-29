<template>
  <div class="page-container">
    <SectionHeading eyebrow="CAMPUS EVENTS" title="校园活动" description="去参加，去认识，去留下属于你的校园记忆。">
      <template #actions>
        <el-button type="primary" round @click="openPublish">
          <el-icon><Plus /></el-icon>发布活动
        </el-button>
      </template>
    </SectionHeading>

    <section class="event-intro">
      <div>
        <span class="eyebrow">THIS WEEK</span>
        <h2>校园里，总有一件事值得出发。</h2>
        <p>活动报名、候补与签到统一沉淀在 CampusHub，名额满了也可以先排队等一个机会。</p>
      </div>
      <div class="event-orb">
        <el-icon :size="30"><Calendar /></el-icon>
        <b>{{ total }}</b>
        <small>场活动</small>
      </div>
    </section>

    <div class="filter-bar">
      <div class="filter-tabs">
        <button v-for="tab in statusTabs" :key="tab.key" :class="{ active: status === tab.key }" @click="status = tab.key">
          {{ tab.label }}
        </button>
      </div>
      <label class="search-shell compact">
        <el-icon><Search /></el-icon>
        <input v-model="keyword" type="search" placeholder="搜索活动关键词（走 ES 检索）" />
        <button v-if="keyword" type="button" aria-label="清空搜索" @click="keyword = ''">
          <el-icon><CircleClose /></el-icon>
        </button>
      </label>
    </div>

    <p v-if="keyword" class="search-tip">
      <el-icon><Search /></el-icon>
      正在通过 campus-activity-service 的全文检索查找「{{ keyword }}」
    </p>

    <StatePanel v-if="loading" type="loading" class="panel-offset" />
    <StatePanel
      v-else-if="error"
      type="error"
      title="活动列表加载失败"
      :description="error"
      action-text="重新加载"
      class="panel-offset"
      @action="reload"
    />
    <div v-else-if="activities.length" class="activity-list">
      <ActivityRow v-for="activity in activities" :key="activity.id" :activity="activity" @open="openActivity" />
    </div>
    <StatePanel v-else type="empty" title="暂无符合条件的活动" description="换个筛选条件，或者自己发起一场。" class="panel-offset" />

    <PagerBar :total="total" :page="page" :page-size="pageSize" @change="changePage" />

    <el-dialog v-model="publishVisible" title="发起校园活动" width="640px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="event-form">
        <el-form-item label="活动标题" prop="title">
          <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="一句话说清活动是什么" />
        </el-form-item>
        <el-form-item label="活动介绍" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="内容安排、集合方式、需要携带的物品…" />
        </el-form-item>
        <div class="form-row">
          <el-form-item label="活动地点" prop="location">
            <el-input v-model="form.location" placeholder="如：信息楼 B201" />
          </el-form-item>
          <el-form-item label="总名额" prop="quota">
            <el-input-number v-model="form.quota" :min="1" :max="2000" class="full-width" />
          </el-form-item>
        </div>
        <div class="form-row">
          <el-form-item label="开始时间" prop="startTime">
            <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择开始时间" class="full-width" />
          </el-form-item>
          <el-form-item label="结束时间" prop="endTime">
            <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择结束时间" class="full-width" />
          </el-form-item>
        </div>
        <div class="form-row">
          <el-form-item label="报名开始" prop="signupStartTime">
            <el-date-picker v-model="form.signupStartTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择报名开始时间" class="full-width" />
          </el-form-item>
          <el-form-item label="报名截止" prop="signupEndTime">
            <el-date-picker v-model="form.signupEndTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择报名截止时间" class="full-width" />
          </el-form-item>
        </div>
        <el-form-item label="封面图（可选）">
          <ImageUploader v-model="form.coverUrl" :upload="activityApi.uploadCover" tip="上传活动封面" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishVisible = false">再想想</el-button>
        <el-button type="primary" :loading="publishing" @click="submitPublish">发布活动</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { activityApi } from '@/api/activity'
import { useRequireLogin } from '@/composables/useRequireLogin'
import { rememberPublishedActivity } from '@/utils/mine'
import { toDateTimeValue } from '@/utils/format'
import SectionHeading from '@/components/SectionHeading.vue'
import ActivityRow from '@/components/ActivityRow.vue'
import ImageUploader from '@/components/ImageUploader.vue'
import StatePanel from '@/components/StatePanel.vue'
import PagerBar from '@/components/PagerBar.vue'

const route = useRoute()
const router = useRouter()
const requireLogin = useRequireLogin()

const statusTabs = [
  { key: '', label: '全部活动' },
  { key: '1', label: '报名中' },
  { key: '2', label: '进行中' },
  { key: '3', label: '已结束' }
]

const activities = ref([])
const status = ref('')
const keyword = ref('')
const page = ref(1)
const pageSize = 8
const total = ref(0)
const loading = ref(false)
const error = ref('')
const searching = ref(false)
let searchTimer = null

const publishVisible = ref(false)
const publishing = ref(false)
const formRef = ref(null)
const form = reactive({
  title: '',
  description: '',
  coverUrl: '',
  location: '',
  startTime: '',
  endTime: '',
  signupStartTime: '',
  signupEndTime: '',
  quota: 30
})

const rules = {
  title: [{ required: true, message: '请填写活动标题', trigger: 'blur' }],
  description: [{ required: true, message: '请填写活动介绍', trigger: 'blur' }],
  location: [{ required: true, message: '请填写活动地点', trigger: 'blur' }],
  quota: [{ required: true, message: '请填写总名额', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  signupStartTime: [{ required: true, message: '请选择报名开始时间', trigger: 'change' }],
  signupEndTime: [{ required: true, message: '请选择报名截止时间', trigger: 'change' }]
}

async function loadList() {
  loading.value = true
  error.value = ''
  searching.value = false
  try {
    const data = await activityApi.page(
      { page: page.value, pageSize, status: status.value || undefined },
      { silent: true }
    )
    activities.value = data?.list || []
    total.value = Number(data?.total || 0)
  } catch (err) {
    error.value = err.message || '请稍后重试'
    activities.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function loadSearch() {
  const text = keyword.value.trim()
  if (!text) return loadList()
  loading.value = true
  error.value = ''
  searching.value = true
  try {
    const data = await activityApi.search({ keyword: text, page: page.value, pageSize }, { silent: true })
    activities.value = data?.list || []
    total.value = Number(data?.total || 0)
  } catch (err) {
    error.value = err.message || '检索失败，请稍后重试'
    activities.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function reload() {
  page.value = 1
  if (keyword.value.trim()) loadSearch()
  else loadList()
}

function changePage(next) {
  page.value = next
  if (keyword.value.trim()) loadSearch()
  else loadList()
}

function openActivity(activity) {
  router.push(`/activities/${activity.id}`)
}

function openPublish() {
  if (!requireLogin('登录后才能发布活动')) return
  const start = new Date()
  start.setDate(start.getDate() + 3)
  start.setHours(19, 0, 0, 0)
  const end = new Date(start)
  end.setHours(21, 0, 0, 0)
  const signupStart = new Date()
  const signupEnd = new Date(start)
  signupEnd.setHours(start.getHours() - 2, 0, 0, 0)
  Object.assign(form, {
    title: '',
    description: '',
    coverUrl: '',
    location: '',
    startTime: toDateTimeValue(start),
    endTime: toDateTimeValue(end),
    signupStartTime: toDateTimeValue(signupStart),
    signupEndTime: toDateTimeValue(signupEnd),
    quota: 30
  })
  publishVisible.value = true
}

async function submitPublish() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (form.endTime <= form.startTime) return ElMessage.warning('结束时间需要晚于开始时间')
  if (form.signupEndTime > form.startTime) return ElMessage.warning('报名截止时间应早于活动开始时间')
  publishing.value = true
  try {
    const id = await activityApi.publish({ ...form })
    rememberPublishedActivity(id)
    publishVisible.value = false
    ElMessage.success('活动发布成功')
    if (id) router.push(`/activities/${id}`)
    else reload()
  } catch {
    // 统一错误提示
  } finally {
    publishing.value = false
  }
}

watch(status, reload)
watch(keyword, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(reload, 400)
})

onMounted(() => {
  reload()
  if (route.query.action === 'publish') {
    if (requireLogin('登录后才能发布活动')) openPublish()
  }
})

onBeforeUnmount(() => clearTimeout(searchTimer))
</script>
