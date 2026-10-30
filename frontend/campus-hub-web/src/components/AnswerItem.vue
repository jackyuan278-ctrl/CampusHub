<template>
  <article class="answer-item">
    <div class="avatar">{{ initial }}</div>
    <div class="answer-body">
      <div class="answer-meta">
        <strong>{{ answer.authorNickname || '同学' }}</strong>
        <span>{{ fromNow(answer.createTime) }}</span>
      </div>
      <p class="answer-content">{{ answer.content }}</p>
      <div class="answer-actions">
        <button type="button" class="text-action" :class="{ 'is-active': answer.liked }" @click="$emit('like', answer)">
          <el-icon><Pointer /></el-icon>{{ answer.likeCount || 0 }}
        </button>
        <button type="button" class="text-action" @click="toggleComments">
          <el-icon><ChatDotRound /></el-icon>{{ comments.length || answer.commentCount || 0 }} 条评论
        </button>
      </div>

      <div v-if="expanded" class="comment-block">
        <div v-if="loading" class="comment-loading">评论加载中…</div>
        <ul v-else-if="comments.length" class="comment-list">
          <li v-for="comment in comments" :key="comment.id">
            <b>{{ comment.authorNickname || '同学' }}</b>
            <span>{{ comment.content }}</span>
            <small>{{ fromNow(comment.createTime) }}</small>
          </li>
        </ul>
        <p v-else class="comment-empty">还没有评论，来说两句～</p>

        <div class="comment-editor">
          <el-input v-model="draft" size="small" maxlength="500" placeholder="友善评论，把问题说清楚" @keydown.enter="onEnter" />
          <el-button size="small" type="primary" plain round :loading="submitting" @click="submit">发送</el-button>
        </div>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { commentApi } from '@/api/community'
import { fromNow } from '@/utils/format'

const props = defineProps({
  answer: { type: Object, required: true }
})
defineEmits(['like'])

const expanded = ref(false)
const loading = ref(false)
const submitting = ref(false)
const loaded = ref(false)
const comments = ref([])
const draft = ref('')

const initial = computed(() => (props.answer.authorNickname || '同').slice(0, 1))

async function toggleComments() {
  expanded.value = !expanded.value
  if (!expanded.value || loaded.value) return
  loading.value = true
  try {
    const data = await commentApi.list(props.answer.id, { silent: true })
    comments.value = Array.isArray(data) ? data : []
    loaded.value = true
  } catch {
    comments.value = []
  } finally {
    loading.value = false
  }
}

async function onEnter(e) {
  // 用中文输入法选词时按回车，keydown 也会触发，此时不能当成「发送」
  if (e.isComposing || e.keyCode === 229) return
  await submit()
}

async function submit() {
  const content = draft.value.trim()
  if (!content) return ElMessage.warning('请输入评论内容')
  submitting.value = true
  try {
    await commentApi.create(props.answer.id, content)
    const data = await commentApi.list(props.answer.id, { silent: true })
    comments.value = Array.isArray(data) ? data : []
    loaded.value = true
    draft.value = ''
    ElMessage.success('评论成功')
  } catch {
    // 错误提示已由请求层统一处理
  } finally {
    submitting.value = false
  }
}
</script>
