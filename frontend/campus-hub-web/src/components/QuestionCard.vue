<template>
  <article class="question-card question-card-rich">
    <div class="avatar">{{ question.avatar }}</div>
    <div class="question-body"><div class="question-meta"><strong>{{ question.author }}</strong><span>{{ question.time }}</span></div><h3>{{ question.title }}</h3><p>{{ question.desc }}</p><div class="question-footer"><div class="tag-row"><span v-for="tag in question.tags" :key="tag" class="outline-tag"># {{ tag }}</span></div><div class="question-stats"><span><el-icon><ChatDotRound /></el-icon>{{ question.answers }} 回答</span><span><el-icon><Pointer /></el-icon>{{ question.likes }} 赞</span></div></div></div>
  </article>
</template>
<script setup>
defineProps({ question: { type: Object, required: true } })
</script>
