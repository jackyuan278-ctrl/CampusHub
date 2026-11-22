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
        <!-- 工具调用中间状态气泡 -->
        <div v-if="message.toolCalls?.length" class="asst-tool-calls">
          <div
            v-for="(tc, i) in message.toolCalls"
            :key="`${tc.name}-${i}`"
            class="asst-tool-call"
            :class="`is-${tc.status}`"
          >
            <span v-if="tc.status === 'running'" class="asst-spinner" />
            <span class="asst-tool-label">{{ toolLabel(tc.name) }}</span>
          </div>
        </div>

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

/** 工具名 → 中文标签（没匹配上就直接显示原名） */
const TOOL_LABELS = {
  // —— 校园库（原有）——
  search_secondhand_items_tool: '查询二手商品',
  get_secondhand_detail_tool: '查询商品详情',
  search_campus_activities: '查询校园活动',
  search_campus_activities_tool: '查询校园活动',
  campus_search_tool: '校园搜索',
  web_search_tool: '联网搜索',
  tavily_search: '联网搜索',
  process_image_tool: '图片识别',
  // —— 旅游规划 ——
  get_weather: '查询天气预报',
  search_flights: '查询航班',
  search_hotels: '查询酒店',
  search_travel_guide: '搜索旅游攻略',
  // —— 股票行情 ——
  get_stock_quote: '查询股票行情',
  get_stock_financials: '查询股票财报',
  get_stock_news: '查询股票新闻',
  // —— 学术论文 ——
  search_papers: '搜索学术论文',
  get_paper_detail: '查看论文详情',
  get_citation_graph: '查看引用关系',
  // —— 商品比价 ——
  compare_products: '商品比价',
  get_product_price_history: '查询价格走势',
  find_best_deal: '查找优惠促销',
  // —— 本地知识库 ——
  local_knowledge_query: '查询校园知识库',
}
function toolLabel(name) {
  return TOOL_LABELS[name] || name || '调用工具'
}
</script>
