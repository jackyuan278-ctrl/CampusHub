<template>
  <div class="page-container">
    <section class="page-heading">
      <div>
        <p class="eyebrow">MY ORDERS</p>
        <h1>我的订单</h1>
        <p>从下单到支付，交易进度都在这里。</p>
      </div>
      <el-button round @click="router.push('/secondhand')">继续逛逛</el-button>
    </section>

    <div class="filter-bar">
      <div class="filter-tabs">
        <button v-for="tab in tabs" :key="tab.key" :class="{ active: activeTab === tab.key }" @click="activeTab = tab.key">
          {{ tab.label }}
        </button>
      </div>
      <span class="filter-count">共 {{ total }} 笔</span>
    </div>

    <StatePanel v-if="loading" type="loading" class="panel-offset" />
    <StatePanel
      v-else-if="error"
      type="error"
      title="订单加载失败"
      :description="error"
      action-text="重新加载"
      class="panel-offset"
      @action="load"
    />
    <div v-else-if="orders.length" class="order-list">
      <article v-for="order in orders" :key="order.id" class="order-card">
        <header class="order-head">
          <div>
            <b>订单号 {{ order.id }}</b>
            <small>{{ formatDate(order.createTime, true) }}</small>
          </div>
          <StatusTag :text="statusMeta(ORDER_STATUS, order.status).text" :tone="statusMeta(ORDER_STATUS, order.status).tone" />
        </header>

        <div class="order-goods">
          <div v-for="goods in order.goods" :key="goods.itemId" class="order-goods-item">
            <div class="cart-thumb" :class="toneOf(goods.itemId)">
              <img v-if="goods.image" :src="goods.image" :alt="goods.name" />
              <el-icon v-else :size="22"><Goods /></el-icon>
            </div>
            <div>
              <b>{{ goods.name }}</b>
              <small>¥{{ money(goods.price) }} × {{ goods.num }}</small>
            </div>
          </div>
        </div>

        <footer class="order-foot">
          <span class="order-address"><el-icon><Location /></el-icon>{{ order.address }}</span>
          <div class="order-actions">
            <span class="order-total">合计 <strong>¥{{ money(order.totalFee) }}</strong></span>
            <el-button size="small" round @click="router.push(`/orders/${order.id}`)">订单详情</el-button>
            <el-button
              v-if="order.status === 1"
              size="small"
              round
              :loading="cancelling === order.id"
              @click="cancelOrder(order)"
            >取消订单</el-button>
            <el-button v-if="order.status === 1" size="small" round type="primary" @click="router.push(`/orders/${order.id}`)">去支付</el-button>
          </div>
        </footer>
      </article>
    </div>
    <StatePanel v-else type="empty" title="还没有相关订单" description="挑一件好物，开始你的第一笔校园交易。" action-text="去逛逛" class="panel-offset" @action="router.push('/secondhand')" />

    <div v-if="orders.length && hasMore" class="load-more-row">
      <el-button round :loading="loadingMore" @click="loadMore">加载更多订单</el-button>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderApi } from '@/api/order'
import { ORDER_STATUS, formatDate, money, statusMeta, toneOf } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'
import StatePanel from '@/components/StatePanel.vue'

const PAGE_SIZE = 10

const router = useRouter()

const orders = ref([])
const loading = ref(true)
const loadingMore = ref(false)
const error = ref('')
const activeTab = ref('')
const total = ref(0)
const page = ref(1)
const hasMore = ref(false)
const cancelling = ref(null)

const tabs = [
  { key: '', label: '全部' },
  { key: '1', label: '待付款' },
  { key: '2', label: '已付款' },
  { key: '3', label: '已发货' },
  { key: '4', label: '已完成' },
  { key: '5', label: '已关闭' }
]

/** 状态筛选交给后端：前端分页后不能再靠内存过滤，否则只能过滤当前这一页 */
function queryParams(target) {
  return {
    page: target,
    pageSize: PAGE_SIZE,
    status: activeTab.value || undefined
  }
}

async function load() {
  loading.value = true
  error.value = ''
  page.value = 1
  try {
    const data = await orderApi.mine(queryParams(1), { silent: true })
    orders.value = Array.isArray(data?.list) ? data.list : []
    total.value = Number(data?.total || orders.value.length)
    hasMore.value = Boolean(data?.hasMore)
  } catch (err) {
    error.value = err.message || '请稍后重试'
    orders.value = []
    total.value = 0
    hasMore.value = false
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  if (loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  const next = page.value + 1
  try {
    const data = await orderApi.mine(queryParams(next), { silent: true })
    const list = Array.isArray(data?.list) ? data.list : []
    // 翻页期间可能有新订单插到第一页，用 id 去重
    const seen = new Set(orders.value.map((order) => order.id))
    orders.value = [...orders.value, ...list.filter((order) => !seen.has(order.id))]
    page.value = next
    hasMore.value = Boolean(data?.hasMore)
  } catch (err) {
    ElMessage.error(err.message || '加载更多失败')
  } finally {
    loadingMore.value = false
  }
}

async function cancelOrder(order) {
  try {
    await ElMessageBox.confirm(`确定取消订单 ${order.id} 吗？取消后商品会回到货架。`, '取消订单', {
      confirmButtonText: '取消订单',
      cancelButtonText: '再想想',
      type: 'warning'
    })
  } catch {
    return
  }
  cancelling.value = order.id
  try {
    await orderApi.cancel(order.id)
    ElMessage.success('订单已取消')
    await load()
  } catch {
    // 请求层已统一提示
  } finally {
    cancelling.value = null
  }
}

watch(activeTab, load)
onMounted(load)
</script>
