<template>
  <aside class="asst-side">
    <div class="asst-side-head">
      <RouterLink to="/home" class="asst-brand" title="返回 CampusHub">
        <span class="asst-brand-mark">C</span>
        <span class="asst-brand-text">
          <strong>CampusHub</strong>
          <small>智能助手</small>
        </span>
      </RouterLink>
      <button type="button" class="asst-side-close" aria-label="收起侧栏" @click="$emit('close')">
        <el-icon><Close /></el-icon>
      </button>
    </div>

    <button type="button" class="asst-new-btn" @click="$emit('new-thread')">
      <el-icon><EditPen /></el-icon>新对话
    </button>

    <label class="asst-side-search">
      <el-icon><Search /></el-icon>
      <input v-model="keyword" type="search" placeholder="搜索会话" />
      <button v-if="keyword" type="button" aria-label="清空搜索" @click="keyword = ''">
        <el-icon><Close /></el-icon>
      </button>
    </label>

    <nav class="asst-side-nav">
      <p class="asst-side-label">功能</p>
      <button
        v-for="item in ASSISTANT_MODES"
        :key="item.key"
        type="button"
        class="asst-side-mode"
        :class="{ 'is-active': item.key === mode }"
        @click="$emit('update:mode', item.key)"
      >
        <el-icon><component :is="item.icon" /></el-icon>
        <span>{{ item.label }}</span>
        <small>{{ item.hint }}</small>
      </button>
    </nav>

    <div class="asst-side-threads">
      <p class="asst-side-label">
        最近对话
        <button v-if="threads.length" type="button" class="asst-side-clear" @click="$emit('clear-all')">清空全部</button>
      </p>

      <p v-if="!visibleThreads.length" class="asst-side-empty">
        {{ threads.length ? '没有匹配的会话' : '还没有对话，点「新对话」开始' }}
      </p>

      <template v-for="group in grouped" :key="group.label">
        <p class="asst-side-group">{{ group.label }}</p>
        <div
          v-for="thread in group.items"
          :key="thread.id"
          class="asst-thread-item"
          :class="{ 'is-active': thread.id === activeId }"
          role="button"
          tabindex="0"
          @click="$emit('open-thread', thread.id)"
          @keydown.enter="$emit('open-thread', thread.id)"
        >
          <el-icon class="asst-thread-icon"><ChatLineRound /></el-icon>
          <span class="asst-thread-title">{{ thread.title }}</span>
          <button type="button" class="asst-thread-del" aria-label="删除对话" @click.stop="$emit('remove-thread', thread.id)">
            <el-icon><Delete /></el-icon>
          </button>
        </div>
      </template>
    </div>

    <div class="asst-side-foot">
      <RouterLink v-if="auth.isLoggedIn" to="/profile" class="asst-user">
        <el-avatar :size="30" :src="auth.user?.avatar || defaultAvatar" />
        <span>
          <b>{{ auth.displayName }}</b>
          <small>查看个人中心</small>
        </span>
      </RouterLink>
      <RouterLink v-else :to="{ path: '/login', query: { redirect: '/assistant' } }" class="asst-user is-guest">
        <span class="asst-user-guest">
          <el-icon><User /></el-icon>
        </span>
        <span>
          <b>未登录</b>
          <small>登录后可用二手与活动数据</small>
        </span>
      </RouterLink>
    </div>
  </aside>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import defaultAvatar from '@/assets/avatar-default.svg'
import { ASSISTANT_MODES } from '@/utils/assistantModes'

const props = defineProps({
  threads: { type: Array, default: () => [] },
  activeId: { type: String, default: '' },
  mode: { type: String, default: 'chat' }
})
defineEmits(['new-thread', 'open-thread', 'remove-thread', 'update:mode', 'clear-all', 'close'])

const auth = useAuthStore()
const keyword = ref('')

const DAY = 24 * 60 * 60 * 1000

const visibleThreads = computed(() => {
  const key = keyword.value.trim()
  if (!key) return props.threads
  return props.threads.filter((thread) => String(thread.title || '').includes(key))
})

/** 按「今天 / 近 7 天 / 更早」分组，和主流对话产品的侧栏一致 */
const grouped = computed(() => {
  const now = Date.now()
  const buckets = { 今天: [], '近 7 天': [], 更早: [] }
  for (const thread of visibleThreads.value) {
    const age = now - (thread.updatedAt || 0)
    if (age < DAY) buckets['今天'].push(thread)
    else if (age < 7 * DAY) buckets['近 7 天'].push(thread)
    else buckets['更早'].push(thread)
  }
  return Object.entries(buckets)
    .filter(([, items]) => items.length)
    .map(([label, items]) => ({ label, items }))
})
</script>
