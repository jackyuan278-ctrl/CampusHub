<template>
  <div class="page-container">
    <section class="page-heading">
      <div>
        <p class="eyebrow">SHOPPING CART</p>
        <h1>我的购物车</h1>
        <p>购物车按账号保存在服务端，换设备也能接着结算。</p>
      </div>
      <el-button v-if="cart.items.length" text type="danger" @click="clearAll">清空购物车</el-button>
    </section>

    <StatePanel v-if="cart.loading && !cart.ready" type="loading" class="panel-offset" />
    <StatePanel
      v-else-if="cart.loadError"
      type="error"
      title="购物车没能加载出来"
      :description="cart.loadError"
      action-text="重新加载"
      class="panel-offset"
      @action="reload"
    />
    <StatePanel
      v-else-if="cart.isEmpty"
      type="empty"
      title="购物车还是空的"
      description="去二手市场转转，把心仪的好物先放进来。"
      action-text="去逛校园二手"
      class="panel-offset"
      @action="router.push('/secondhand')"
    />

    <div v-else class="cart-layout">
      <div class="cart-lines">
        <div class="cart-section-head">
          <el-checkbox :model-value="allSelected" :indeterminate="partSelected" @change="toggleAll">全选</el-checkbox>
          <span>共 {{ cart.count }} 件</span>
        </div>

        <div v-for="line in cart.items" :key="line.itemId" class="cart-line" :class="{ 'is-unbuyable': !isBuyable(line) }">
          <el-checkbox
            :model-value="selected.includes(line.itemId)"
            :disabled="!isBuyable(line)"
            @change="(checked) => toggleOne(line.itemId, checked)"
          />
          <div class="cart-thumb" :class="toneOf(line.itemId)">
            <img v-if="line.image" :src="line.image" :alt="line.name" />
            <el-icon v-else :size="26"><Goods /></el-icon>
          </div>
          <div class="cart-line-info">
            <h3 @click="router.push(`/secondhand/${line.itemId}`)">{{ line.name }}</h3>
            <p>
              <span v-if="!isBuyable(line)" class="soft-tag is-warn">{{ statusText(line) }}</span>
              <span v-else>{{ line.spec || '校内面交 · 当面验货' }}</span>
            </p>
          </div>
          <span class="line-price">¥{{ money(line.price) }}</span>
          <span class="line-qty">×{{ line.num }}</span>
          <span class="line-subtotal">¥{{ money(line.price * line.num) }}</span>
          <button type="button" class="icon-button danger" aria-label="移除" @click="removeLine(line)">
            <el-icon><Delete /></el-icon>
          </button>
        </div>
      </div>

      <aside class="checkout-card">
        <span class="eyebrow">SUMMARY</span>
        <h2>结算</h2>
        <div class="checkout-row"><span>已选商品</span><b>{{ selectedCount }} 件</b></div>
        <div class="checkout-row"><span>商品金额</span><b>¥{{ money(selectedTotal) }}</b></div>
        <div class="checkout-row muted"><span>交易方式</span><span>校内面交 · 免运费</span></div>
        <div class="checkout-total"><span>合计</span><strong>¥{{ money(selectedTotal) }}</strong></div>
        <el-button type="primary" round size="large" class="checkout-button" :disabled="!selected.length" @click="goCheckout">
          去结算
        </el-button>
        <p class="checkout-note"><el-icon><Lock /></el-icon>下单后可选择余额支付或模拟扫码支付</p>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useCartStore } from '@/stores/cart'
import { money, toneOf } from '@/utils/format'
import StatePanel from '@/components/StatePanel.vue'

const CHOSEN_KEY = 'campushub_checkout_items'

const router = useRouter()
const cart = useCartStore()
const selected = ref([])

const buyableIds = computed(() => cart.items.filter((line) => isBuyable(line)).map((line) => line.itemId))
const allSelected = computed(() => buyableIds.value.length > 0 && selected.value.length === buyableIds.value.length)
const partSelected = computed(() => selected.value.length > 0 && selected.value.length < buyableIds.value.length)
const chosenLines = computed(() => cart.items.filter((line) => selected.value.includes(line.itemId)))
const selectedCount = computed(() => chosenLines.value.reduce((sum, line) => sum + line.num, 0))
const selectedTotal = computed(() => chosenLines.value.reduce((sum, line) => sum + line.price * line.num, 0))

/** status 为 null 表示后端没查成（降级），按可购买处理，结算时后端还会再拦一次 */
function isBuyable(line) {
  return line.status === null || line.status === undefined || line.status === 1
}

function statusText(line) {
  if (line.status === 3) return '已售出'
  if (line.status === 0) return '已下架'
  if (line.status === 2) return '已被预订'
  return '暂不可购买'
}

function syncSelection() {
  // 只默认勾选还能买的，已售/下架的不参与结算
  const ids = cart.items.filter(isBuyable).map((line) => line.itemId)
  selected.value = selected.value.filter((id) => ids.includes(id))
  if (!selected.value.length && ids.length) selected.value = ids
}

function toggleAll(checked) {
  selected.value = checked ? cart.items.filter(isBuyable).map((line) => line.itemId) : []
}

function toggleOne(itemId, checked) {
  selected.value = checked
    ? [...selected.value, itemId]
    : selected.value.filter((id) => id !== itemId)
}

async function removeLine(line) {
  try {
    await ElMessageBox.confirm(`确定把「${line.name}」移出购物车吗？`, '移除商品', {
      confirmButtonText: '移除',
      cancelButtonText: '保留',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await cart.remove(line.itemId)
    ElMessage.success('已移除')
  } catch {
    // 统一错误提示
  }
}

async function clearAll() {
  try {
    await ElMessageBox.confirm('清空后需要重新加购，确定清空购物车吗？', '清空购物车', {
      confirmButtonText: '清空',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await cart.clear()
    ElMessage.success('购物车已清空')
  } catch {
    // 统一错误提示
  }
}

function goCheckout() {
  if (!selected.value.length) return ElMessage.warning('请先选择要结算的商品')
  sessionStorage.setItem(CHOSEN_KEY, JSON.stringify(selected.value))
  router.push('/checkout')
}

async function reload() {
  await cart.load()
  syncSelection()
}

watch(() => cart.items, syncSelection, { deep: true })
onMounted(async () => {
  await cart.load()
  syncSelection()
})
</script>
