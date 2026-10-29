<template>
  <div class="page-container">
    <nav class="breadcrumb">
      <RouterLink to="/home">首页</RouterLink>
      <el-icon><ArrowRight /></el-icon>
      <RouterLink to="/secondhand">校园二手</RouterLink>
      <el-icon><ArrowRight /></el-icon>
      <span>{{ item?.name || '物品详情' }}</span>
    </nav>

    <StatePanel v-if="loading" type="loading" class="panel-offset" />
    <StatePanel
      v-else-if="error"
      type="error"
      title="没能打开这件物品"
      :description="error"
      action-text="返回列表"
      class="panel-offset"
      @action="router.push('/secondhand')"
    />

    <template v-else-if="item">
      <section class="detail-layout">
        <div class="detail-gallery">
          <div class="detail-cover" :class="tone">
            <img v-if="item.image" :src="item.image" :alt="item.name" />
            <el-icon v-else :size="96"><component :is="iconOfCategory(item.categoryName)" /></el-icon>
            <span class="condition-pill">{{ item.conditionDesc || '在售' }}</span>
          </div>
          <div class="detail-gallery-foot">
            <span><el-icon><Location /></el-icon>{{ item.tradeLocation || '校内面交' }}</span>
            <span><el-icon><View /></el-icon>同学发布 · 同校交易</span>
          </div>

          <div class="panel">
            <h2 class="panel-title">卖家描述</h2>
            <p class="panel-text">{{ item.description || '卖家还没有填写更多细节，可以直接联系询问。' }}</p>
            <dl class="meta-list">
              <div><dt>分类</dt><dd>{{ item.categoryName || '未分类' }}</dd></div>
              <div><dt>成色</dt><dd>{{ item.conditionDesc || '未填写' }}</dd></div>
              <div><dt>交易地点</dt><dd>{{ item.tradeLocation || '校内面交' }}</dd></div>
              <div><dt>商品编号</dt><dd>{{ item.id }}</dd></div>
            </dl>
          </div>
        </div>

        <aside class="detail-aside">
          <div class="detail-info-card">
            <div class="detail-tag-row">
              <span class="soft-tag">{{ item.categoryName || '校园二手' }}</span>
              <StatusTag :text="status.text" :tone="status.tone" />
            </div>
            <h1>{{ item.name }}</h1>
            <p class="detail-desc">{{ item.description || '卖家暂未填写描述' }}</p>

            <div class="price-hero">
              <span class="price-symbol">¥</span>
              <strong>{{ money(item.price) }}</strong>
              <small>校内面交价</small>
            </div>

            <div class="detail-meta-grid">
              <div><small>交易地点</small><b>{{ item.tradeLocation || '校内面交' }}</b></div>
              <div><small>成色</small><b>{{ item.conditionDesc || '未填写' }}</b></div>
              <div><small>卖家</small><b>同学 {{ String(item.sellerId || '').slice(-4) || '匿名' }}</b></div>
              <div><small>状态</small><b>{{ status.text }}</b></div>
            </div>

            <div class="buy-row">
              <span class="buy-label">数量</span>
              <el-input-number v-model="num" :min="1" :max="99" size="default" />
            </div>

            <div class="detail-actions">
              <el-button type="primary" size="large" round :disabled="item.status !== 1" :loading="adding" @click="add">
                <el-icon><ShoppingCart /></el-icon>加入购物车
              </el-button>
              <el-button size="large" round :disabled="item.status !== 1" @click="buyNow">立即购买</el-button>
            </div>
            <p class="detail-hint">
              <el-icon><InfoFilled /></el-icon>下单前请与卖家确认物品状态，建议在教学楼、宿舍楼下等公共区域面交。
            </p>
          </div>
        </aside>
      </section>

      <section class="content-section">
        <div class="section-title-row">
          <div><span class="eyebrow">MORE LIKE THIS</span><h2>同类好物</h2></div>
          <RouterLink to="/secondhand" class="text-link">查看更多 <el-icon><ArrowRight /></el-icon></RouterLink>
        </div>
        <div v-if="related.length" class="product-grid">
          <ItemCard v-for="entry in related" :key="entry.id" :item="entry" @open="openRelated" @add="addToCart" />
        </div>
        <StatePanel v-else type="empty" title="同类物品暂时只有这一件" />
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { itemApi } from '@/api/item'
import { useAddToCart } from '@/composables/useAddToCart'
import { ITEM_STATUS, iconOfCategory, money, statusMeta, toneOf } from '@/utils/format'
import ItemCard from '@/components/ItemCard.vue'
import StatePanel from '@/components/StatePanel.vue'
import StatusTag from '@/components/StatusTag.vue'

const route = useRoute()
const router = useRouter()
const { addToCart } = useAddToCart()

const item = ref(null)
const related = ref([])
const loading = ref(true)
const error = ref('')
const num = ref(1)
const adding = ref(false)

const tone = computed(() => toneOf(item.value?.categoryId))
const status = computed(() => statusMeta(ITEM_STATUS, item.value?.status))

async function load() {
  loading.value = true
  error.value = ''
  try {
    item.value = await itemApi.detail(route.params.id, { silent: true })
    if (item.value?.categoryId) {
      const data = await itemApi.page({ page: 1, pageSize: 4, categoryId: item.value.categoryId }, { silent: true })
      related.value = (data?.list || []).filter((entry) => entry.id !== item.value.id).slice(0, 3)
    } else {
      related.value = []
    }
  } catch (err) {
    error.value = err.message || '请稍后重试'
  } finally {
    loading.value = false
  }
}

async function add() {
  adding.value = true
  const success = await addToCart(item.value, num.value)
  adding.value = false
  return success
}

async function buyNow() {
  const success = await add()
  if (success) router.push({ path: '/checkout', query: { itemId: item.value.id } })
  else ElMessage.info('登录后即可下单')
}

function openRelated(entry) {
  router.push(`/secondhand/${entry.id}`)
}

watch(() => route.params.id, load)
onMounted(load)
</script>
