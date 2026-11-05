<template>
  <div class="asst-shell" :class="{ 'is-side-open': sideOpen }" :data-theme="store.theme">
    <AssistantSidebar
      :threads="store.threads"
      :active-id="store.activeId"
      :mode="store.mode"
      @new-thread="createThread"
      @open-thread="openThread"
      @remove-thread="store.removeThread"
      @clear-all="store.clearAll()"
      @update:mode="store.setMode"
      @close="sideOpen = false"
    />
    <div class="asst-backdrop" @click="sideOpen = false" />

    <section class="asst-main">
      <header class="asst-topbar">
        <button type="button" class="asst-menu-btn" aria-label="打开侧栏" @click="sideOpen = true">
          <el-icon><Expand /></el-icon>
        </button>
        <div class="asst-topbar-title">
          <strong>{{ store.currentTitle }}</strong>
          <span v-if="store.sending" class="asst-typing">正在思考…</span>
        </div>
        <div class="asst-themes" role="group" aria-label="切换主题">
          <button
            v-for="item in ASSISTANT_THEMES"
            :key="item.key"
            type="button"
            class="asst-theme-dot"
            :class="[`is-${item.key}`, { 'is-active': store.theme === item.key }]"
            :title="`主题：${item.label}`"
            :aria-pressed="store.theme === item.key"
            @click="store.setTheme(item.key)"
          />
        </div>
        <div class="asst-topbar-actions">
          <el-button v-if="store.messages.length" text size="small" @click="clearActive">
            <el-icon><Delete /></el-icon>清空
          </el-button>
          <el-button text size="small" @click="createThread">
            <el-icon><Plus /></el-icon>新对话
          </el-button>
        </div>
      </header>

      <div ref="scrollerRef" class="asst-scroll" @scroll="onScroll">
        <div v-if="store.isEmpty" class="asst-hero">
          <div class="asst-hero-mark">
            <el-icon :size="28"><MagicStick /></el-icon>
          </div>
          <h1>今天想聊点什么？</h1>
          <p>CampusHub 智能助手 · 找二手、挑活动、读资料，都可以交给我</p>

          <div class="asst-suggests">
            <button
              v-for="item in SUGGESTIONS"
              :key="item.title"
              type="button"
              class="asst-suggest"
              @click="useSuggestion(item)"
            >
              <el-icon class="asst-suggest-icon"><component :is="item.icon" /></el-icon>
              <span class="asst-suggest-copy">
                <b>{{ item.title }}</b>
                <small>{{ item.desc }}</small>
              </span>
              <el-icon class="asst-suggest-arrow"><Right /></el-icon>
            </button>
          </div>
        </div>

        <div v-else class="asst-thread">
          <AssistantMessage v-for="message in store.messages" :key="message.id" :message="message" />
          <div class="asst-thread-pad" />
        </div>
      </div>

      <div v-if="store.error" class="asst-banner">
        <el-icon><InfoFilled /></el-icon>
        <span>{{ store.error }}</span>
        <button type="button" @click="store.error = ''">知道了</button>
      </div>

      <AssistantComposer
        ref="composerRef"
        :mode="store.mode"
        :busy="store.sending"
        @update:mode="store.setMode"
        @send="onSend"
        @stop="store.stop()"
      />
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import AssistantSidebar from '@/components/assistant/AssistantSidebar.vue'
import AssistantComposer from '@/components/assistant/AssistantComposer.vue'
import AssistantMessage from '@/components/assistant/AssistantMessage.vue'
import { useAssistantStore } from '@/stores/assistant'
import { ASSISTANT_THEMES, SUGGESTIONS } from '@/utils/assistantModes'

const store = useAssistantStore()
const scrollerRef = ref(null)
const composerRef = ref(null)
const sideOpen = ref(false)
const stick = ref(true)

const lastContent = computed(() => {
  const list = store.messages
  const last = list[list.length - 1]
  return last ? last.content.length : 0
})

function onScroll() {
  const el = scrollerRef.value
  if (!el) return
  stick.value = el.scrollHeight - el.scrollTop - el.clientHeight < 90
}

function scrollToEnd() {
  const el = scrollerRef.value
  if (el) el.scrollTop = el.scrollHeight
}

function createThread() {
  store.newThread()
  // 新对话 = 真清屏：消息、输入框草稿、待发送的附件全部清掉
  composerRef.value?.reset()
  sideOpen.value = false
  stick.value = true
  nextTick(() => {
    scrollToEnd()
    composerRef.value?.focus()
  })
}

function openThread(id) {
  store.openThread(id)
  composerRef.value?.reset()
  sideOpen.value = false
  stick.value = true
  nextTick(scrollToEnd)
}

function clearActive() {
  store.clearActive()
  composerRef.value?.reset()
  stick.value = true
  nextTick(scrollToEnd)
}

function useSuggestion(item) {
  if (store.sending) {
    ElMessage.warning('正在生成回复，稍等一下')
    return
  }
  composerRef.value?.fill(item.text)
}

function onSend({ text, attachments }) {
  if (store.sending) return
  stick.value = true
  store.send(text, attachments)
  nextTick(scrollToEnd)
}

watch(lastContent, () => {
  if (stick.value || store.sending) nextTick(scrollToEnd)
})

watch(() => store.activeId, () => {
  stick.value = true
  nextTick(scrollToEnd)
})

onMounted(() => {
  scrollToEnd()
  composerRef.value?.focus()
})
</script>
