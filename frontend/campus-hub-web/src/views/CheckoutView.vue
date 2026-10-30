<template>
  <div class="page-container">
    <section class="page-heading">
      <div>
        <p class="eyebrow">CHECKOUT</p>
        <h1>确认订单</h1>
        <p>核对收货信息与商品明细，提交后即可支付。</p>
      </div>
      <div class="step-trail">
        <span class="is-active">1 确认订单</span>
        <el-icon><ArrowRight /></el-icon>
        <span>2 完成支付</span>
      </div>
    </section>

    <StatePanel v-if="loading" type="loading" class="panel-offset" />
    <StatePanel
      v-else-if="!lines.length"
      type="empty"
      title="没有待结算的商品"
      description="购物车里还没有选中商品，先去挑几件吧。"
      action-text="返回购物车"
      class="panel-offset"
      @action="router.push('/cart')"
    />

    <div v-else class="checkout-layout">
      <div class="checkout-main">
        <div class="panel">
          <div class="panel-head">
            <h2 class="panel-title">收货信息</h2>
            <el-button text type="primary" @click="openAddressDialog(null)">
              <el-icon><Plus /></el-icon>新增地址
            </el-button>
          </div>

          <div v-if="addresses.length" class="address-grid">
            <button
              v-for="address in addresses"
              :key="address.id"
              type="button"
              class="address-card"
              :class="{ 'is-active': selectedAddressId === address.id }"
              @click="selectedAddressId = address.id"
            >
              <span class="address-head">
                <b>{{ address.receiver }}</b>
                <small>{{ address.phone }}</small>
                <span v-if="address.isDefault" class="soft-tag">默认</span>
              </span>
              <span class="address-detail">{{ fullAddress(address) }}</span>
            </button>
          </div>
          <StatePanel v-else type="empty" title="还没有收货地址" description="添加一个地址，方便同学把东西交到你手上。" action-text="新增地址" @action="openAddressDialog(null)" />

          <div class="panel-divider"></div>

          <h2 class="panel-title">商品明细</h2>
          <ul class="checkout-goods">
            <li v-for="line in lines" :key="line.itemId">
              <div class="cart-thumb" :class="toneOf(line.itemId)">
                <img v-if="line.image" :src="line.image" :alt="line.name" />
                <el-icon v-else :size="22"><Goods /></el-icon>
              </div>
              <div class="checkout-goods-info">
                <b>{{ line.name }}</b>
                <small>{{ line.spec || '校内面交' }} · ×{{ line.num }}</small>
              </div>
              <span>¥{{ money(line.price * line.num) }}</span>
            </li>
          </ul>

          <div class="panel-divider"></div>

          <h2 class="panel-title">订单备注</h2>
          <el-input v-model="remark" type="textarea" :rows="2" maxlength="255" show-word-limit placeholder="例如：周三下午在图书馆门口面交" />
        </div>
      </div>

      <aside class="checkout-card">
        <span class="eyebrow">PAYMENT</span>
        <h2>应付金额</h2>
        <div class="checkout-row"><span>商品件数</span><b>{{ itemCount }} 件</b></div>
        <div class="checkout-row"><span>商品金额</span><b>¥{{ money(totalFee) }}</b></div>
        <div class="checkout-row muted"><span>交易方式</span><span>校内面交 · 免运费</span></div>
        <div class="checkout-total"><span>合计</span><strong>¥{{ money(totalFee) }}</strong></div>
        <el-button type="primary" round size="large" class="checkout-button" :loading="submitting" @click="submit">
          提交订单
        </el-button>
        <p class="checkout-note"><el-icon><InfoFilled /></el-icon>提交后请在订单详情页完成支付</p>
      </aside>
    </div>

    <AddressDialog v-model="addressVisible" :address="editingAddress" @saved="loadAddresses" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api/user'
import { orderApi } from '@/api/order'
import { useCartStore } from '@/stores/cart'
import { money, toneOf } from '@/utils/format'
import StatePanel from '@/components/StatePanel.vue'
import AddressDialog from '@/components/AddressDialog.vue'

const CHOSEN_KEY = 'campushub_checkout_items'

const route = useRoute()
const router = useRouter()
const cart = useCartStore()

const addresses = ref([])
const selectedAddressId = ref('')
const remark = ref('')
const loading = ref(true)
const submitting = ref(false)
const addressVisible = ref(false)
const editingAddress = ref(null)
const chosenIds = ref([])

const lines = computed(() => cart.items.filter((line) => chosenIds.value.includes(line.itemId)))
const totalFee = computed(() => lines.value.reduce((sum, line) => sum + line.price * line.num, 0))
const itemCount = computed(() => lines.value.reduce((sum, line) => sum + line.num, 0))
const currentAddress = computed(() => addresses.value.find((entry) => entry.id === selectedAddressId.value) || null)

function fullAddress(address) {
  return `${address.province || ''}${address.city || ''}${address.district || ''}${address.detail || ''}`
}

function readChosen() {
  if (route.query.itemId) return [String(route.query.itemId)]
  try {
    const list = JSON.parse(sessionStorage.getItem(CHOSEN_KEY) || '[]')
    return Array.isArray(list) ? list.map(String) : []
  } catch {
    return []
  }
}

async function loadAddresses() {
  try {
    const data = await userApi.addresses({ silent: true })
    addresses.value = Array.isArray(data) ? data : []
    if (!selectedAddressId.value || !addresses.value.some((entry) => entry.id === selectedAddressId.value)) {
      selectedAddressId.value = addresses.value.find((entry) => entry.isDefault)?.id || addresses.value[0]?.id || ''
    }
  } catch {
    addresses.value = []
  }
}

function openAddressDialog(address) {
  editingAddress.value = address
  addressVisible.value = true
}

async function submit() {
  if (!lines.value.length) return ElMessage.warning('没有可结算的商品')
  if (!currentAddress.value) return ElMessage.warning('请选择收货地址')
  submitting.value = true
  let order
  try {
    order = await orderApi.create({
      receiver: currentAddress.value.receiver,
      phone: currentAddress.value.phone,
      address: fullAddress(currentAddress.value),
      remark: remark.value,
      goods: lines.value.map((line) => ({
        itemId: line.itemId,
        name: line.name,
        price: line.price,
        num: 1,
        image: line.image,
        spec: line.spec
      }))
    })
  } catch {
    submitting.value = false
    return
  }
  // 订单已经创建，下面的收尾动作失败不能让用户以为「下单失败」
  sessionStorage.removeItem(CHOSEN_KEY)
  try {
    await Promise.all(chosenIds.value.map((id) => cart.remove(id)))
  } catch {
    // 购物车清理失败只影响下一次看到的列表，刷新即可自愈
  }
  submitting.value = false
  ElMessage.success('订单已提交，请完成支付')
  router.push(`/orders/${order.id}`)
}

onMounted(async () => {
  chosenIds.value = readChosen()
  await cart.load()
  await loadAddresses()
  loading.value = false
})
</script>
