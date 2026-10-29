<template>
  <div class="page-container">
    <nav class="breadcrumb">
      <RouterLink to="/home">首页</RouterLink>
      <el-icon><ArrowRight /></el-icon>
      <RouterLink to="/community">问答社区</RouterLink>
      <el-icon><ArrowRight /></el-icon>
      <span>问题详情</span>
    </nav>

    <StatePanel v-if="loading" type="loading" class="panel-offset" />
    <StatePanel
      v-else-if="error"
      type="error"
      title="没能打开这个问题"
      :description="error"
      action-text="返回社区"
      class="panel-offset"
      @action="router.push('/community')"
    />

    <template v-else-if="question">
      <section class="question-hero">
        <div class="question-hero-main">
          <div class="question-author">
            <span class="avatar">{{ (question.authorNickname || '同').slice(0, 1) }}</span>
            <div>
              <strong>{{ question.authorNickname || '匿名同学' }}</strong>
              <small>{{ fromNow(question.createTime) }} 提问</small>
            </div>
            <el-button
              v-if="question.userId && question.userId !== auth.user?.id"
              size="small"
              round
              :type="following ? 'default' : 'primary'"
              :plain="following"
              @click="toggleFollow"
            >
              {{ following ? '已关注' : '关注作者' }}
            </el-button>
          </div>

          <h1>{{ question.title }}</h1>
          <p class="question-hero-content">{{ question.content }}</p>

          <div class="question-hero-foot">
            <div class="tag-row">
              <span v-for="tag in question.tags || []" :key="tag" class="outline-tag"># {{ tag }}</span>
            </div>
            <div class="question-stats large">
              <button type="button" class="text-action" :class="{ 'is-active': question.liked }" @click="toggleLike">
                <el-icon><Pointer /></el-icon>{{ question.likeCount || 0 }} 赞
              </button>
              <span><el-icon><View /></el-icon>{{ question.viewCount || 0 }}</span>
              <span><el-icon><ChatDotRound /></el-icon>{{ answers.length }} 回答</span>
            </div>
          </div>
        </div>
      </section>

      <section class="answer-layout">
        <div>
          <div class="answer-header">
            <h2>{{ answers.length }} 条回答</h2>
            <span class="eyebrow">ANSWERS</span>
          </div>

          <div v-if="auth.isLoggedIn" class="answer-editor">
            <el-input v-model="draft" type="textarea" :rows="3" maxlength="2000" show-word-limit placeholder="写下你的经验，越具体越有帮助" />
            <div class="answer-editor-foot">
              <span>用真实经历回答，比「我也不知道」更有用。</span>
              <el-button type="primary" round :loading="submitting" @click="submitAnswer">发布回答</el-button>
            </div>
          </div>
          <div v-else class="answer-login-hint">
            <span><el-icon><InfoFilled /></el-icon>登录后即可回答这个问题</span>
            <el-button size="small" round type="primary" @click="router.push({ path: '/login', query: { redirect: route.fullPath } })">去登录</el-button>
          </div>

          <div v-if="answers.length" class="answer-list">
            <AnswerItem v-for="answer in answers" :key="answer.id" :answer="answer" @like="toggleAnswerLike" />
          </div>
          <StatePanel v-else type="empty" title="还没有人回答" description="你的经验也许正是提问者需要的。" />
        </div>

        <aside class="community-aside">
          <div class="aside-card">
            <span class="aside-kicker">ABOUT</span>
            <h3>关于提问</h3>
            <p class="aside-text">问题会同步到社区话题，被更多同学看到。回答通过点赞排序，优质内容会进入热点榜。</p>
          </div>
          <div class="aside-card topic-card">
            <span class="aside-kicker">TAGS</span>
            <h3>相关话题</h3>
            <button v-for="tag in question.tags || []" :key="tag" @click="router.push({ path: '/community', query: { tagId: tagIdOf(tag) } })">
              # {{ tag }}
              <span>去看看</span>
            </button>
          </div>
        </aside>
      </section>
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { answerApi, followApi, questionApi, tagApi } from '@/api/community'
import { useAuthStore } from '@/stores/auth'
import { useRequireLogin } from '@/composables/useRequireLogin'
import { fromNow } from '@/utils/format'
import StatePanel from '@/components/StatePanel.vue'
import AnswerItem from '@/components/AnswerItem.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const requireLogin = useRequireLogin()

const question = ref(null)
const answers = ref([])
const tags = ref([])
const following = ref(false)
const draft = ref('')
const loading = ref(true)
const error = ref('')
const submitting = ref(false)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [detail, answerList] = await Promise.all([
      questionApi.detail(route.params.id, { silent: true }),
      answerApi.list(route.params.id, { silent: true })
    ])
    question.value = detail
    answers.value = Array.isArray(answerList) ? answerList : []
    if (!tags.value.length) {
      const tagList = await tagApi.list({ silent: true }).catch(() => [])
      tags.value = Array.isArray(tagList) ? tagList : []
    }
    await loadFollowState()
  } catch (err) {
    error.value = err.message || '请稍后重试'
  } finally {
    loading.value = false
  }
}

async function loadFollowState() {
  if (!auth.isLoggedIn || !question.value?.userId) return
  try {
    const list = await followApi.mine({ silent: true })
    following.value = (Array.isArray(list) ? list : []).some((user) => String(user.id) === String(question.value.userId))
  } catch {
    following.value = false
  }
}

async function toggleFollow() {
  if (!requireLogin('登录后才能关注作者')) return
  try {
    if (following.value) {
      await followApi.unfollow(question.value.userId)
      following.value = false
      ElMessage.success('已取消关注')
    } else {
      await followApi.follow(question.value.userId)
      following.value = true
      ElMessage.success('已关注该作者')
    }
  } catch {
    // 统一错误提示
  }
}

async function toggleLike() {
  if (!requireLogin('登录后才能点赞')) return
  const liked = question.value.liked
  try {
    if (liked) await questionApi.unlike(question.value.id)
    else await questionApi.like(question.value.id)
    question.value.liked = !liked
    question.value.likeCount = Math.max(0, (question.value.likeCount || 0) + (liked ? -1 : 1))
  } catch {
    // 统一错误提示
  }
}

async function toggleAnswerLike(answer) {
  if (!requireLogin('登录后才能点赞')) return
  try {
    if (answer.liked) await answerApi.unlike(answer.id)
    else await answerApi.like(answer.id)
    answer.liked = !answer.liked
    answer.likeCount = Math.max(0, (answer.likeCount || 0) + (answer.liked ? 1 : -1))
  } catch {
    // 统一错误提示
  }
}

async function submitAnswer() {
  if (!requireLogin('登录后才能回答问题')) return
  const content = draft.value.trim()
  if (!content) return ElMessage.warning('请先写下你的回答')
  submitting.value = true
  try {
    await answerApi.create(question.value.id, content)
    draft.value = ''
    ElMessage.success('回答已发布')
    const list = await answerApi.list(question.value.id, { silent: true })
    answers.value = Array.isArray(list) ? list : []
  } catch {
    // 统一错误提示
  } finally {
    submitting.value = false
  }
}

function tagIdOf(name) {
  return tags.value.find((tag) => tag.name === name)?.id || ''
}

watch(() => route.params.id, load)
onMounted(load)
</script>
