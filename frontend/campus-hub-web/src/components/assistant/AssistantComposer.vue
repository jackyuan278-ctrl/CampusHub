<template>
  <div class="asst-dock">
    <div class="asst-mascot-slot">
      <AgentMascot :busy="busy" />
    </div>

    <div class="asst-composer" :class="{ 'is-dragging': dragging }" @dragover.prevent="dragging = true" @dragleave.prevent="dragging = false" @drop.prevent="onDrop">
      <!-- 功能选择：输入框上方 -->
      <div class="asst-chips" role="tablist" aria-label="选择功能">
        <button
          v-for="item in ASSISTANT_MODES"
          :key="item.key"
          type="button"
          role="tab"
          class="asst-chip"
          :class="[`tone-${item.key}`, { 'is-active': item.key === mode }]"
          :aria-selected="item.key === mode"
          @click="$emit('update:mode', item.key)"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </button>
      </div>

      <!-- 附件预览 -->
      <div v-if="attachments.length" class="asst-attach-list">
        <div v-for="file in attachments" :key="file.id" class="asst-attach" :title="file.name">
          <img v-if="file.preview" :src="file.preview" :alt="file.name" class="asst-attach-thumb" />
          <span v-else class="asst-attach-icon">
            <el-icon><component :is="fileIcon(file.name, file.type)" /></el-icon>
          </span>
          <span class="asst-attach-meta">
            <b>{{ file.name }}</b>
            <small>{{ formatSize(file.size) }}</small>
          </span>
          <button type="button" class="asst-attach-remove" aria-label="移除附件" @click="removeFile(file.id)">
            <el-icon><Close /></el-icon>
          </button>
        </div>
      </div>

      <textarea
        ref="textareaRef"
        v-model="draft"
        class="asst-textarea"
        rows="1"
        :placeholder="current.placeholder"
        :maxlength="2000"
        @keydown="onKeydown"
        @input="resize"
      />

      <div class="asst-bar">
        <div class="asst-tools">
          <input ref="fileInputRef" type="file" multiple :accept="ATTACHMENT_RULES.accept" class="asst-file-input" @change="onPick" />
          <button type="button" class="asst-tool-btn" title="上传图片 / PDF" @click="fileInputRef?.click()">
            <el-icon><Plus /></el-icon>
          </button>
          <span class="asst-tool-tip">支持图片、PDF、Word，单个 ≤ 10MB</span>
        </div>

        <div class="asst-send-area">
          <span class="asst-count" v-if="draft.length > 1200">{{ draft.length }}/2000</span>
          <button v-if="busy" type="button" class="asst-send is-stop" title="停止生成" @click="$emit('stop')">
            <span class="asst-stop-square" />
          </button>
          <button v-else type="button" class="asst-send" :disabled="!canSend" title="发送" @click="submit">
            <el-icon><Top /></el-icon>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import AgentMascot from './AgentMascot.vue'
import { uploadAttachment } from '@/api/assistant'
import { getToken } from '@/api/http'
import { ASSISTANT_MODES, ATTACHMENT_RULES, fileIcon, formatSize, modeMeta } from '@/utils/assistantModes'

const props = defineProps({
  mode: { type: String, default: 'chat' },
  busy: { type: Boolean, default: false }
})
const emit = defineEmits(['update:mode', 'send', 'stop'])

const draft = ref('')
const attachments = ref([])
const dragging = ref(false)
const uploading = ref(false)
const textareaRef = ref(null)
const fileInputRef = ref(null)

const current = computed(() => modeMeta(props.mode))
const canSend = computed(() => Boolean(draft.value.trim() || attachments.value.length))

// 文档类附件后端会走 MinerU 解析（图片走视觉模型，不在此列）。
// 这里的判定必须和后端 doc_ingest.is_document 保持一致，否则会出现
// 「前端不提示、后端默默解析了 40 秒」的情况。
const DOC_SUFFIXES = ['.pdf', '.doc', '.docx', '.txt', '.md']
const DOC_TYPES = [
  'application/pdf',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'text/plain',
  'text/markdown'
]

function isDocument(item) {
  const type = String(item?.type || '').toLowerCase()
  const name = String(item?.name || '').toLowerCase()
  return DOC_TYPES.includes(type) || DOC_SUFFIXES.some((suffix) => name.endsWith(suffix))
}

let seq = 0

function resize() {
  const el = textareaRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = `${Math.min(el.scrollHeight, 168)}px`
}

function addFiles(fileList) {
  const incoming = Array.from(fileList || [])
  for (const file of incoming) {
    if (attachments.value.length >= ATTACHMENT_RULES.maxCount) {
      ElMessage.warning(`最多上传 ${ATTACHMENT_RULES.maxCount} 个附件`)
      break
    }
    if (file.size > ATTACHMENT_RULES.maxSize) {
      ElMessage.warning(`${file.name} 超过 10MB，已跳过`)
      continue
    }
    const isImage = file.type.startsWith('image/')
    attachments.value.push({
      id: `f_${Date.now()}_${(seq += 1)}`,
      name: file.name,
      size: file.size,
      type: file.type,
      file,
      preview: isImage ? URL.createObjectURL(file) : ''
    })
  }
}

function onPick(event) {
  addFiles(event.target.files)
  event.target.value = ''
}

function onDrop(event) {
  dragging.value = false
  addFiles(event.dataTransfer?.files)
}

function removeFile(id) {
  const index = attachments.value.findIndex((item) => item.id === id)
  if (index < 0) return
  const [removed] = attachments.value.splice(index, 1)
  if (removed.preview) URL.revokeObjectURL(removed.preview)
}

function onKeydown(event) {
  if (event.key !== 'Enter') return
  if (event.shiftKey || event.isComposing) return
  event.preventDefault()
  submit()
}

async function submit() {
  if (props.busy || uploading.value || !canSend.value) return

  // 附件先传 OSS：失败就不发消息、也不清输入框，用户改完可以直接重试。
  // （之前放在 store 里，失败时 draft 已经被清空，等于让用户重打一遍字）
  let ready = []
  if (attachments.value.length) {
    uploading.value = true
    try {
      ready = await Promise.all(
        attachments.value.map(async (item) => {
          const meta = { name: item.name, type: item.type, size: item.size }
          if (!item.file) return { ...meta, url: item.url }

          // 文档要等 MinerU 解析，一份 PDF 可能十几到几十秒。先说一声，
          // 不然用户只看到转圈，会以为卡死了。
          if (isDocument(item)) {
            ElMessage.info(`正在解析「${item.name}」，文档较大时需要几十秒…`)
          }

          const result = await uploadAttachment(item.file, getToken())

          // 后端返回 parsed：{status: 'ok'|'failed'|'skipped', reason?, chunks?}
          // 解析失败不影响发送，但必须让用户知道——否则他只会得到一句
          // 「文档里没找到」，还以为是模型笨。
          const parsed = result?.parsed
          if (parsed?.status === 'failed') {
            ElMessage.warning(
              `「${item.name}」解析失败：${parsed.reason || '未知原因'}。仍可发送，但模型读不到正文`
            )
          }
          return { ...meta, url: result.url }
        })
      )
    } catch (err) {
      ElMessage.error(err?.message || '附件上传失败')
      return
    } finally {
      uploading.value = false
    }
  }

  emit('send', { text: draft.value.trim(), attachments: ready })
  draft.value = ''
  attachments.value.forEach((item) => item.preview && URL.revokeObjectURL(item.preview))
  attachments.value = []
  nextTick(resize)
}

function fill(text) {
  draft.value = text
  nextTick(() => {
    resize()
    textareaRef.value?.focus()
  })
}

/** 切会话 / 新建会话时用：连草稿和附件一起清掉，避免上一轮的残留 */
function reset() {
  draft.value = ''
  attachments.value.forEach((item) => item.preview && URL.revokeObjectURL(item.preview))
  attachments.value = []
  dragging.value = false
  nextTick(resize)
}

function focus() {
  textareaRef.value?.focus()
}

watch(() => props.mode, () => nextTick(() => textareaRef.value?.focus()))

onBeforeUnmount(() => {
  attachments.value.forEach((item) => item.preview && URL.revokeObjectURL(item.preview))
})

defineExpose({ fill, focus, reset })
</script>
