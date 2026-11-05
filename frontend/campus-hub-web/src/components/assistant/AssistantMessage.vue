<template>
  <div class="asst-msg" :class="`is-${message.role}`">
    <div class="asst-msg-avatar">
      <svg v-if="message.role === 'assistant'" viewBox="0 0 40 40" width="34" height="34" aria-label="AI助手">
        <!-- 猫耳 -->
        <path d="M 12 11 L 9 4 L 17 8 Z" fill="#e8ecef" />
        <path d="M 28 11 L 31 4 L 23 8 Z" fill="#e8ecef" />
        <!-- 圆头 -->
        <circle cx="20" cy="21" r="14" fill="#e8ecef" />
        <!-- 黑屏幕 -->
        <rect x="11" y="15" width="18" height="13" rx="6" fill="#1a1d22" />
        <!-- 绿眼睛 -->
        <rect x="15" y="18.5" width="3" height="6" rx="1.5" fill="#2ee6a8" />
        <rect x="22" y="18.5" width="3" height="6" rx="1.5" fill="#2ee6a8" />
      </svg>
      <span v-else>{{ initial }}</span>
    </div>

    <div class="asst-msg-body">
      <div v-if="message.attachments?.length" class="asst-msg-files">
        <span v-for="(file, i) in message.attachments" :key="`${file.name}-${file.size}-${i}`" class="asst-msg-file">
          <el-icon><component :is="fileIcon(file.name, file.type)" /></el-icon>{{ file.name }}
        </span>
      </div>

      <p v-if="message.role === 'user'" class="asst-bubble">{{ message.content }}</p>

      <template v-else>
        <div v-if="message.content" class="asst-md" v-html="renderMarkdown(message.content)" />
        <div v-else-if="message.streaming" class="asst-dots"><span /><span /><span /></div>
        <p v-else class="asst-bubble asst-msg-empty">（没有返回内容）</p>
        <span v-if="message.demo" class="asst-msg-tag">演示回复</span>
        <span v-else-if="message.stopped" class="asst-msg-tag">已停止</span>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { renderMarkdown } from '@/utils/markdown'
import { fileIcon } from '@/utils/assistantModes'

const props = defineProps({
  message: { type: Object, required: true }
})

const auth = useAuthStore()
const initial = computed(() => (auth.displayName || '我').slice(0, 1))
</script>
