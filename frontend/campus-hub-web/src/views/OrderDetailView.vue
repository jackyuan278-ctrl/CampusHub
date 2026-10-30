<template>
  <div class="page-container">
    <nav class="breadcrumb">
      <RouterLink to="/home">首页</RouterLink>
      <el-icon><ArrowRight /></el-icon>
      <RouterLink to="/orders">我的订单</RouterLink>
      <el-icon><ArrowRight /></el-icon>
      <span>{{ route.params.id }}</span>
    </nav>

    <StatePanel v-if="loading" type="loading" class="panel-offset" />
    <StatePanel
      v-else-if="error"
      type="error"
      title="没能打开这笔订单"
      :description="error"
      action-text="返回订单列表"
      class="panel-offset"
      @action="router.push('/orders')"
    />

    <template v-else-if="order">
      <section class="order-hero" :class="{ 'is-paid': order.status !== 1 }">
        <div>
          <span class="eyebrow">ORDER STATUS</span>
          <h1>{{ status.text }}</h1>
          <p>{{ heroTip }}</p>
        </div>
        <div class="order-hero-fee">
          <small>订单金额</small>
          <strong>¥{{ money(order.totalFee) }}</strong>
        </div>
      </section>

      <section class="order-detail-layout">
        <div class="order-detail-main">
          <div class="panel">
            <h2 class="panel-title">商品明细</h2>
            <ul class="checkout-goods">
              <li v-for="goods in order.goods" :key="goods.itemId">
                <div class="cart-thumb" :class="toneOf(goods.itemId)">
                  <img v-if="goods.image" :src="goods.image" :alt="goods.name" />
                  <el-icon v-else :size="22"><Goods /></el-icon>
                </div>
                <div class="checkout-goods-info">
                  <b>{{ goods.name }}</b>
                  <small>{{ goods.spec || '校内面交' }} · ×{{ goods.num }}</small>
                </div>
                <span>¥{{ money(goods.price * goods.num) }}</span>
              </li>
            </ul>
          </div>

          <div class="panel">
            <h2 class="panel-title">收货与交易</h2>
            <dl class="meta-list">
              <div><dt>收货人</dt><dd>{{ order.receiver }}</dd></div>
              <div><dt>联系电话</dt><dd>{{ order.phone }}</dd></div>
              <div><dt>收货地址</dt><dd>{{ order.address }}</dd></div>
              <div><dt>订单备注</dt><dd>{{ order.remark || '无' }}</dd></div>
              <div><dt>下单时间</dt><dd>{{ formatDate(order.createTime, true) }}</dd></div>
              <div><dt>支付时间</dt><dd>{{ formatDate(order.payTime, true) || '未支付' }}</dd></div>
            </dl>
          </div>
        </div>

        <aside class="order-detail-aside">
          <div class="checkout-card">
            <span class="eyebrow">PAYMENT</span>
            <h2>{{ order.status === 1 ? '选择支付方式' : '支付信息' }}</h2>

            <template v-if="order.status === 1">
              <div class="pay-options">
                <button type="button" class="pay-option" :class="{ 'is-active': payType === 'balance' }" @click="payType = 'balance'">
                  <span class="pay-icon"><el-icon><Wallet /></el-icon></span>
                  <span>
                    <b>余额支付</b>
                    <small>当前余额 ¥{{ money(auth.balance) }}</small>
                  </span>
                  <el-icon v-if="payType === 'balance'" class="pay-check"><CircleCheckFilled /></el-icon>
                </button>
                <button type="button" class="pay-option" :class="{ 'is-active': payType === 'mock' }" @click="payType = 'mock'">
                  <span class="pay-icon"><el-icon><Iphone /></el-icon></span>
                  <span>
                    <b>模拟扫码支付</b>
                    <small>演示环境使用，立即完成支付</small>
                  </span>
                  <el-icon v-if="payType === 'mock'" class="pay-check"><CircleCheckFilled /></el-icon>
                </button>
              </div>

              <div class="checkout-total"><span>应付</span><strong>¥{{ money(order.totalFee) }}</strong></div>
              <el-button type="primary" round size="large" class="checkout-button" :loading="paying" @click="pay">立即支付</el-button>
              <el-button text class="cancel-order" :loading="cancelling" @click="cancelOrder">取消订单</el-button>
              <p class="checkout-note"><el-icon><Lock /></el-icon>支付完成后请线下与卖家完成面交</p>
            </template>

            <template v-else>
              <div class="checkout-row"><span>支付方式</span><b>{{ PAYMENT_TYPE[order.paymentType] || '未记录' }}</b></div>
              <div class="checkout-row"><span>支付时间</span><b>{{ formatDate(order.payTime, true) || '—' }}</b></div>
              <div class="checkout-total"><span>已支付</span><strong>¥{{ money(order.totalFee) }}</strong></div>
              <el-button round size="large" class="checkout-button" @click="router.push('/orders')">返回订单列表</el-button>
            </template>
          </div>
        </aside>
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderApi, payApi } from '@/api/order'
import { useAuthStore } from '@/stores/auth'
import { ORDER_STATUS, PAYMENT_TYPE, formatDate, money, statusMeta, toneOf } from '@/utils/format'
import StatePanel from '@/components/StatePanel.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const order = ref(null)
const loading = ref(true)
const error = ref('')
const payType = ref('balance')
const paying = ref(false)
const cancelling = ref(false)

const status = computed(() => statusMeta(ORDER_STATUS, order.value?.status))
const heroTip = computed(() => {
  const map = {
    1: '订单已创建，完成支付后即可和卖家约面交。',
    2: '支付已完成，记得和卖家确认取货时间。',
    3: '卖家已发货，注意查看消息。',
    4: '交易已完成，感谢你让闲置继续流动。',
    5: '订单已关闭，商品会回到卖家手中。'
  }
  return map[order.value?.status] || '订单信息如下。'
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    order.value = await orderApi.detail(route.params.id, { silent: true })
  } catch (err) {
    error.value = err.message || '请稍后重试'
  } finally {
    loading.value = false
  }
}

async function cancelOrder() {
  try {
    await ElMessageBox.confirm('取消后商品会回到货架，确定要取消吗？', '取消订单', {
      confirmButtonText: '取消订单',
      cancelButtonText: '再想想',
      type: 'warning'
    })
  } catch {
    return
  }
  cancelling.value = true
  try {
    await orderApi.cancel(order.value.id)
    ElMessage.success('订单已取消')
    await load()
  } catch {
    // 请求层已统一提示
  } finally {
    cancelling.value = false
  }
}

async function pay() {
  paying.value = true
  try {
    await payApi.pay(order.value.id, payType.value)
    ElMessage.success('支付成功')
    await auth.loadMe()
    await waitForPaid()
  } catch {
    // 统一错误提示
  } finally {
    paying.value = false
  }
}

/**
 * 支付单是同步落库的，但订单状态要等 outbox 事件经 MQ 传到交易服务才变「已支付」。
 * 支付接口一返回就刷新必然读到旧的 status=1，所以按固定间隔追几次，拿到新状态立刻停。
 */
async function waitForPaid(maxTries = 5, interval = 700) {
  for (let i = 0; i < maxTries; i += 1) {
    await new Promise((resolve) => setTimeout(resolve, interval))
    try {
      const data = await orderApi.detail(route.params.id, { silent: true })
      order.value = data
      if (data.status !== 1) return
    } catch {
      // 单次查询失败不中断轮询，下一轮继续
    }
  }
  ElMessage.info('支付已完成，订单状态同步中，稍后刷新即可')
}

onMounted(load)
</script>
