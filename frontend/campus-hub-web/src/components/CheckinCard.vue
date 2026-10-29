<template>
  <div class="checkin-card">
    <div class="checkin-head">
      <div>
        <span class="eyebrow">DAILY CHECK-IN</span>
        <h3>校园签到</h3>
        <p>连续 {{ checkin.streak || 0 }} 天，坚持的人运气不会太差。</p>
      </div>
      <span class="checkin-streak"><b>{{ checkin.streak || 0 }}</b><small>连续天数</small></span>
    </div>

    <div class="checkin-week">
      <div v-for="day in week" :key="day.key" class="checkin-day" :class="{ 'is-signed': day.signed, 'is-today': day.today }">
        <small>{{ day.label }}</small>
        <span>{{ day.day }}</span>
      </div>
    </div>

    <el-button type="primary" round class="checkin-button" :loading="loading" :disabled="checkin.signedToday" @click="$emit('checkin')">
      {{ checkin.signedToday ? '今天已签到' : '立即签到' }}
    </el-button>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  checkin: { type: Object, default: () => ({ signedToday: false, streak: 0, checkinDates: [] }) },
  loading: Boolean
})
defineEmits(['checkin'])

const LABELS = ['日', '一', '二', '三', '四', '五', '六']

const week = computed(() => {
  const signed = new Set(props.checkin.checkinDates || [])
  const today = new Date()
  const cells = []
  for (let offset = 6; offset >= 0; offset -= 1) {
    const date = new Date(today)
    date.setDate(today.getDate() - offset)
    const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
    cells.push({
      key,
      day: date.getDate(),
      label: LABELS[date.getDay()],
      signed: signed.has(key),
      today: offset === 0
    })
  }
  return cells
})
</script>
