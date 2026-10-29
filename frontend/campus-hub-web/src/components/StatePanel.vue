<template>
  <div class="state-panel" :class="`is-${type}`">
    <span class="state-icon">
      <el-icon :size="24" :class="{ spinning: type === 'loading' }">
        <component :is="icon" />
      </el-icon>
    </span>
    <p class="state-title">{{ title || defaultTitle }}</p>
    <p v-if="description" class="state-desc">{{ description }}</p>
    <slot />
    <el-button v-if="actionText" round size="small" @click="$emit('action')">{{ actionText }}</el-button>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  type: { type: String, default: 'empty' },
  title: String,
  description: String,
  actionText: String
})
defineEmits(['action'])

const defaultTitleMap = {
  loading: '正在加载…',
  empty: '这里还没有内容',
  error: '加载失败',
  success: '操作成功'
}

const iconMap = {
  loading: 'Loading',
  empty: 'Box',
  error: 'WarningFilled',
  success: 'CircleCheck'
}

const icon = computed(() => iconMap[props.type] || 'InfoFilled')
const defaultTitle = computed(() => defaultTitleMap[props.type] || '暂无内容')
</script>
