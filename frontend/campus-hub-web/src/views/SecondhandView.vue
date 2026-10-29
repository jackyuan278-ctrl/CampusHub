<template>
  <div class="page-container">
    <section class="page-heading">
      <div>
        <p class="eyebrow">SECOND-HAND MARKET</p>
        <h1>校园二手交易</h1>
        <p>让闲置物品在校园里流动起来。</p>
      </div>
      <el-tooltip content="后端暂未开放商品发布接口" placement="bottom-end">
        <el-button round @click="explainPublish">
          <el-icon><Plus /></el-icon>发布闲置
        </el-button>
      </el-tooltip>
    </section>

    <div class="market-toolbar">
      <label class="search-shell">
        <el-icon><Search /></el-icon>
        <input v-model="keyword" type="search" placeholder="搜索物品名称，比如「键盘」「高数」" />
        <button v-if="keyword" type="button" aria-label="清空搜索" @click="keyword = ''">
          <el-icon><CircleClose /></el-icon>
        </button>
      </label>
      <div class="category-tabs">
        <button :class="{ active: !categoryId }" @click="selectCategory('')">全部</button>
        <button v-for="category in categories" :key="category.id" :class="{ active: categoryId === category.id }" @click="selectCategory(category.id)">
          {{ category.name }}
        </button>
      </div>
      <el-select v-model="sort" class="sort-select" size="default">
        <el-option label="综合排序" value="" />
        <el-option label="价格从低到高" value="priceAsc" />
        <el-option label="价格从高到低" value="priceDesc" />
      </el-select>
    </div>

    <div class="market-summary">
      <span>共 <b>{{ total }}</b> 件在售好物<template v-if="keyword"> · 关键词「{{ keyword }}」</template></span>
      <span class="summary-tip"><el-icon><Location /></el-icon>同校面交，当天可取</span>
    </div>

    <StatePanel v-if="loading" type="loading" class="panel-offset" />
    <StatePanel
      v-else-if="error"
      type="error"
      title="物品列表加载失败"
      :description="error"
      action-text="重新加载"
      class="panel-offset"
      @action="reload"
    />
    <div v-else-if="items.length" class="product-grid">
      <ItemCard v-for="item in items" :key="item.id" :item="item" @open="openItem" @add="addToCart" />
    </div>
    <StatePanel
      v-else
      type="empty"
      title="没有找到符合条件的物品"
      description="换个关键词或分类试试，也可以去活动与社区逛逛。"
      class="panel-offset"
    />

    <div v-if="!loading && items.length" class="load-more-row">
      <el-button v-if="hasMore" round :loading="loadingMore" @click="loadMore">加载更多好物</el-button>
      <span v-else>已经到底啦，共 {{ items.length }} 件</span>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { itemApi } from '@/api/item'
import { useAddToCart } from '@/composables/useAddToCart'
import ItemCard from '@/components/ItemCard.vue'
import StatePanel from '@/components/StatePanel.vue'

const PAGE_SIZE = 12

const router = useRouter()
const { addToCart } = useAddToCart()

const categories = ref([])
const items = ref([])
const keyword = ref('')
const categoryId = ref('')
const sort = ref('')
const total = ref(0)
const loading = ref(false)
const loadingMore = ref(false)
const error = ref('')
const hasMore = ref(false)
const cursor = ref(null)
const cursorSortValue = ref(null)
let searchTimer = null

async function loadCategories() {
  try {
    const data = await itemApi.categories({ silent: true })
    categories.value = Array.isArray(data) ? data : []
  } catch {
    categories.value = []
  }
}

/** 首次/筛选变化：走 page 分页（后端前 5 页支持 offset） */
async function reload() {
  loading.value = true
  error.value = ''
  cursor.value = null
  cursorSortValue.value = null
  try {
    const data = await itemApi.page(
      {
        page: 1,
        pageSize: PAGE_SIZE,
        keyword: keyword.value.trim() || undefined,
        categoryId: categoryId.value || undefined,
        sort: sort.value || undefined
      },
      { silent: true }
    )
    items.value = data?.list || []
    total.value = Number(data?.total || items.value.length)
    // page 模式不返回 hasMore，用「已加载数 < 总数」推断是否还能继续拉取
    hasMore.value = items.value.length < total.value
  } catch (err) {
    error.value = err.message || '请稍后重试'
    items.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/** 滚动加载：第 2 页起改用游标分页，避免后端深分页限制 */
async function loadMore() {
  if (!items.value.length) return
  loadingMore.value = true
  try {
    const last = items.value[items.value.length - 1]
    const params = {
      pageSize: PAGE_SIZE,
      keyword: keyword.value.trim() || undefined,
      categoryId: categoryId.value || undefined,
      sort: sort.value || undefined,
      cursorId: cursor.value || last.id
    }
    if (sort.value) params.cursorSortValue = cursorSortValue.value ?? last.price
    const data = await itemApi.page(params, { silent: true })
    const list = data?.list || []
    items.value = [...items.value, ...list]
    hasMore.value = Boolean(data?.hasMore)
    cursor.value = data?.nextCursor || null
    cursorSortValue.value = data?.nextCursorSortValue ?? null
  } catch (err) {
    ElMessage.error(err.message || '加载更多失败')
  } finally {
    loadingMore.value = false
  }
}

function selectCategory(id) {
  categoryId.value = id
}

function openItem(item) {
  router.push(`/secondhand/${item.id}`)
}

function explainPublish() {
  ElMessage.info('campus-secondhand-service 目前只开放查询与库存接口，发布功能待后端补齐')
}

watch([categoryId, sort], () => {
  hasMore.value = false
  reload()
})

watch(keyword, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    hasMore.value = false
    reload()
  }, 350)
})

onMounted(() => {
  loadCategories()
  reload()
})

onBeforeUnmount(() => clearTimeout(searchTimer))
</script>
