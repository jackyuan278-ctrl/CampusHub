<template>
  <article class="activity-row">
    <div class="date-tile" :class="activity.tone"><b>{{ day }}</b><span>{{ month }}</span></div>
    <div class="activity-main"><div class="row-top"><span class="soft-tag">{{ activity.type }}</span><span class="activity-status">{{ activity.status }}</span></div><h3>{{ activity.title }}</h3><p><el-icon><Location /></el-icon>{{ activity.location }} <i>·</i> {{ activity.people || 0 }}/{{ activity.capacity || 1 }} 人已报名</p><div class="activity-progress"><span :style="{ width: `${Math.min(100, (activity.people || 0) / (activity.capacity || 1) * 100)}%` }"></span></div></div>
    <div class="activity-actions"><small>{{ activity.host }}</small><el-button round @click="$emit('signup', activity)">查看活动</el-button></div>
  </article>
</template>
<script setup>
import { computed } from 'vue'
const props = defineProps({ activity: { type: Object, required: true } })
defineEmits(['signup'])
const date = computed(() => { const d = new Date(props.activity.date); return isNaN(d.getTime()) ? new Date() : d })
const day = computed(() => String(date.value.getDate()).padStart(2, '0'))
const month = computed(() => `${date.value.getMonth() + 1} 月`)
</script>
