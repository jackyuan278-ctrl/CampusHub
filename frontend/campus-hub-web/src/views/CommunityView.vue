<template>
  <div class="page-container">
    <SectionHeading eyebrow="COMMUNITY" title="问答社区" description="遇到问题就来问问，校园经验在这里流动起来。">
      <template #actions>
        <el-button type="primary" round @click="openAsk">
          <el-icon><EditPen /></el-icon>我要提问
        </el-button>
      </template>
    </SectionHeading>

    <section class="community-toolbar">
      <div class="community-tabs">
        <button :class="{ active: sort === 'new' }" @click="sort = 'new'">最新</button>
        <button :class="{ active: sort === 'hot' }" @click="sort = 'hot'">最热</button>
      </div>
      <label class="search-shell compact">
        <el-icon><Search /></el-icon>
        <input v-model="keyword" type="search" placeholder="搜索问题或话题" />
        <button v-if="keyword" type="button" aria-label="清空搜索" @click="keyword = ''">
          <el-icon><CircleClose /></el-icon>
        </button>
      </label>
    </section>

    <div class="tag-filter-row">
      <button class="topic-chip small" :class="{ active: !activeTag }" @click="selectTag('')">全部话题</button>
      <button
        v-for="tag in tags"
        :key="tag.id"
        class="topic-chip small"
        :class="{ active: activeTag === tag.id }"
        @click="selectTag(tag.id)"
      >
        # {{ tag.name }}
      </button>
    </div>

    <div class="community-layout">
      <div>
        <div class="community-summary">
          <span>共 <b>{{ total }}</b> 个问题</span>
          <span v-if="activeTagName" class="summary-tip"><el-icon><CollectionTag /></el-icon>#{{ activeTagName }}</span>
        </div>

        <StatePanel v-if="loading" type="loading" class="panel-offset" />
        <StatePanel
          v-else-if="error"
          type="error"
          title="社区内容加载失败"
          :description="error"
          action-text="重新加载"
          class="panel-offset"
          @action="reload"
        />
        <div v-else-if="questions.length" class="question-list">
          <QuestionCard v-for="question in questions" :key="question.id" :question="question" @open="openQuestion" />
        </div>
        <StatePanel v-else type="empty" title="还没有相关的问题" description="不如你来提第一个？" class="panel-offset" />

        <PagerBar :total="total" :page="page" :page-size="pageSize" @change="changePage" />
      </div>

      <aside class="community-aside">
        <div class="aside-card topic-card">
          <span class="aside-kicker">HOT TOPICS</span>
          <h3>大家最近在聊</h3>
          <button v-for="tag in hotTags" :key="tag.id" @click="selectTag(tag.id)">
            # {{ tag.name }}
            <span>{{ tag.questionCount || 0 }}</span>
          </button>
        </div>
        <div class="aside-card join-card">
          <span class="join-icon"><el-icon><ChatDotRound /></el-icon></span>
          <h3>分享你的校园经验</h3>
          <p>一个回答，也许能帮到正在找答案的同学。</p>
          <el-button round @click="openAsk">去提问</el-button>
        </div>
      </aside>
    </div>

    <el-dialog v-model="askVisible" title="提一个问题" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="问题标题" prop="title">
          <el-input v-model="form.title" maxlength="128" show-word-limit placeholder="把问题说清楚，更容易得到回答" />
        </el-form-item>
        <el-form-item label="问题描述" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="5" placeholder="补充背景信息：你试过什么、卡在哪里" />
        </el-form-item>
        <el-form-item label="选择话题（可多选）">
          <el-select v-model="form.tagIds" multiple filterable placeholder="选择 1-3 个话题" class="full-width">
            <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="askVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAsk">发布问题</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { questionApi, tagApi } from '@/api/community'
import { useRequireLogin } from '@/composables/useRequireLogin'
import SectionHeading from '@/components/SectionHeading.vue'
import QuestionCard from '@/components/QuestionCard.vue'
import StatePanel from '@/components/StatePanel.vue'
import PagerBar from '@/components/PagerBar.vue'

const route = useRoute()
const router = useRouter()
const requireLogin = useRequireLogin()

const questions = ref([])
const tags = ref([])
const sort = ref('new')
const keyword = ref('')
const activeTag = ref('')
const page = ref(1)
const pageSize = 8
const total = ref(0)
const loading = ref(false)
const error = ref('')
let searchTimer = null

const askVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const form = reactive({ title: '', content: '', tagIds: [] })

const rules = {
  title: [{ required: true, message: '请填写问题标题', trigger: 'blur' }],
  content: [{ required: true, message: '请补充问题描述', trigger: 'blur' }]
}

const hotTags = computed(() => [...tags.value].sort((a, b) => (b.questionCount || 0) - (a.questionCount || 0)).slice(0, 5))
const activeTagName = computed(() => tags.value.find((tag) => tag.id === activeTag.value)?.name || '')

async function loadTags() {
  try {
    const data = await tagApi.list({ silent: true })
    tags.value = Array.isArray(data) ? data : []
  } catch {
    tags.value = []
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const data = await questionApi.page(
      {
        page: page.value,
        pageSize,
        keyword: keyword.value.trim() || undefined,
        tagId: activeTag.value || undefined,
        sort: sort.value
      },
      { silent: true }
    )
    questions.value = data?.list || []
    total.value = Number(data?.total || 0)
  } catch (err) {
    error.value = err.message || '请稍后重试'
    questions.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function reload() {
  page.value = 1
  load()
}

function changePage(next) {
  page.value = next
  load()
}

function selectTag(id) {
  activeTag.value = activeTag.value === id ? '' : id
}

function openQuestion(question) {
  router.push(`/community/${question.id}`)
}

function openAsk() {
  if (!requireLogin('登录后才能提问')) return
  Object.assign(form, { title: '', content: '', tagIds: activeTag.value ? [activeTag.value] : [] })
  askVisible.value = true
}

async function submitAsk() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const id = await questionApi.publish({ ...form })
    askVisible.value = false
    ElMessage.success('问题已发布')
    if (id) router.push(`/community/${id}`)
    else reload()
  } catch {
    // 统一错误提示
  } finally {
    submitting.value = false
  }
}

watch(sort, reload)
watch(activeTag, reload)
watch(keyword, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(reload, 350)
})
watch(
  () => route.query.tagId,
  (value) => {
    if (value && value !== activeTag.value) activeTag.value = value
  }
)

onMounted(async () => {
  if (route.query.tagId) activeTag.value = route.query.tagId
  await loadTags()
  await load()
  if (route.query.action === 'publish' && requireLogin('登录后才能提问')) openAsk()
})

onBeforeUnmount(() => clearTimeout(searchTimer))
</script>
