<template>
  <article class="activity-row" :class="{ 'has-cover': Boolean(activity.coverUrl) }" @click="$emit('open', activity)">
    <div v-if="activity.coverUrl" class="activity-cover">
      <img :src="activity.coverUrl" :alt="activity.title" loading="lazy" />
    </div>
    <div class="date-tile" :class="tone">
      <b>{{ parts.day }}</b>
      <span>{{ parts.month }}</span>
    </div>
    <div class="activity-main">
      <div class="row-top">
        <span class="soft-tag">校园活动</span>
        <StatusTag :text="status.text" :tone="status.tone" />
      </div>
      <h3>{{ activity.title }}</h3>
      <p>
        <el-icon><Location /></el-icon>{{ activity.location || '地点待通知' }}
        <i>·</i>{{ formatDate(activity.startTime, true) || '时间待定' }}
        <template v-if="hasQuota">
          <i>·</i>{{ signedCount }}/{{ activity.quota }} 人已报名
        </template>
      </p>
      <div v-if="hasQuota" class="activity-progress">
        <span :style="{ width: `${percent}%` }"></span>
      </div>
    </div>
    <div class="activity-actions">
      <small>{{ quotaLabel }}</small>
      <el-button round size="small" @click.stop="$emit('open', activity)">查看详情</el-button>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import StatusTag from './StatusTag.vue'
import { ACTIVITY_STATUS, dateParts, formatDate, statusMeta } from '@/utils/format'

const props = defineProps({
  activity: { type: Object, required: true }
})
defineEmits(['open'])

const parts = computed(() => dateParts(props.activity.startTime))
const status = computed(() => statusMeta(ACTIVITY_STATUS, props.activity.status))
const hasQuota = computed(() => typeof props.activity.quota === 'number' && props.activity.quota > 0)
const signedCount = computed(() => {
  if (!hasQuota.value) return 0
  const remaining = typeof props.activity.remainingQuota === 'number' ? props.activity.remainingQuota : props.activity.quota
  return Math.max(0, props.activity.quota - remaining)
})
const percent = computed(() => (hasQuota.value ? Math.min(100, (signedCount.value / props.activity.quota) * 100) : 0))
const quotaLabel = computed(() => {
  if (!hasQuota.value) return '点击查看详情'
  return `剩余 ${Math.max(0, props.activity.remainingQuota ?? props.activity.quota)} 个名额`
})
const tone = computed(() => {
  if (props.activity.status === 2) return 'orange'
  if (props.activity.status === 3 || props.activity.status === 4) return 'blue'
  return 'green'
})
</script>
