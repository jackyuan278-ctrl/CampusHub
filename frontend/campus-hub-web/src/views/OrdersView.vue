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
      <span class="filter-count">共 {{ filteredOrders.length }} 笔</span>
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
    <div v-else-if="filteredOrders.length" class="order-list">
      <article v-for="order in filteredOrders" :key="order.id" class="order-card">
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
            <el-button v-if="order.status === 1" size="small" round type="primary" @click="router.push(`/orders/${order.id}`)">去支付</el-button>
          </div>
        </footer>
      </article>
    </div>
    <StatePanel v-else type="empty" title="还没有相关订单" description="挑一件好物，开始你的第一笔校园交易。" action-text="去逛逛" class="panel-offset" @action="router.push('/secondhand')" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { orderApi } from '@/api/order'
import { ORDER_STATUS, formatDate, money, statusMeta, toneOf } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'
import StatePanel from '@/components/StatePanel.vue'

const router = useRouter()

const orders = ref([])
const loading = ref(true)
const error = ref('')
const activeTab = ref('')

const tabs = [
  { key: '', label: '全部' },
  { key: '1', label: '待付款' },
  { key: '2', label: '已付款' },
  { key: '3', label: '已发货' },
  { key: '4', label: '已完成' },
  { key: '5', label: '已关闭' }
]

const filteredOrders = computed(() =>
  activeTab.value ? orders.value.filter((order) => String(order.status) === activeTab.value) : orders.value
)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const data = await orderApi.mine({ silent: true })
    orders.value = Array.isArray(data) ? data : []
  } catch (err) {
    error.value = err.message || '请稍后重试'
    orders.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
