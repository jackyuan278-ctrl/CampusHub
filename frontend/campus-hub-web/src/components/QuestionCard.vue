<template>
  <article class="question-card question-card-rich" @click="$emit('open', question)">
    <div class="avatar">{{ avatarText }}</div>
    <div class="question-body">
      <div class="question-meta">
        <strong>{{ question.authorNickname || '匿名同学' }}</strong>
        <span>{{ fromNow(question.createTime) }}</span>
        <span v-if="question.viewCount" class="question-views">
          <el-icon><View /></el-icon>{{ question.viewCount }}
        </span>
      </div>
      <h3>{{ question.title }}</h3>
      <p>{{ question.content }}</p>
      <div class="question-footer">
        <div class="tag-row">
          <span v-for="tag in question.tags || []" :key="tag" class="outline-tag"># {{ tag }}</span>
        </div>
        <div class="question-stats">
          <span><el-icon><ChatDotRound /></el-icon>{{ question.answerCount || 0 }} 回答</span>
          <span><el-icon><Pointer /></el-icon>{{ question.likeCount || 0 }} 赞</span>
        </div>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import { fromNow } from '@/utils/format'

const props = defineProps({
  question: { type: Object, required: true }
})
defineEmits(['open'])

const avatarText = computed(() => (props.question.authorNickname || '同').slice(0, 1))
</script>
