<template>
  <div class="page-container">
    <section class="page-heading">
      <div>
        <p class="eyebrow">SECOND-HAND MARKET</p>
        <h1>校园二手交易</h1>
        <p>让闲置物品在校园里流动起来。</p>
      </div>
      <el-button round type="primary" @click="openPublish">
        <el-icon><Plus /></el-icon>发布闲置
      </el-button>
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

    <el-dialog v-model="publishVisible" title="发布闲置" width="620px" destroy-on-close>
      <el-form ref="publishFormRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="物品名称" prop="name">
          <el-input v-model="form.name" maxlength="255" show-word-limit placeholder="比如：罗技 G304 无线鼠标" />
        </el-form-item>
        <div class="publish-row">
          <el-form-item label="价格（元）" prop="price">
            <el-input-number v-model="form.price" :min="0.01" :max="100000" :precision="2" :step="1" class="full-width" />
          </el-form-item>
          <el-form-item label="分类" prop="categoryId">
            <el-select v-model="form.categoryId" placeholder="选择分类" class="full-width">
              <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" />
            </el-select>
          </el-form-item>
        </div>
        <div class="publish-row">
          <el-form-item label="成色" prop="conditionDesc">
            <el-input v-model="form.conditionDesc" maxlength="100" show-word-limit placeholder="比如：9成新，无明显磕碰" />
          </el-form-item>
          <el-form-item label="交易地点" prop="tradeLocation">
            <el-input v-model="form.tradeLocation" maxlength="255" show-word-limit placeholder="比如：图书馆东门" />
          </el-form-item>
        </div>
        <el-form-item label="商品图片（可选）" prop="image">
          <ImageUploader v-model="form.image" :upload="itemApi.uploadImage" tip="上传商品图" />
        </el-form-item>
        <el-form-item label="物品描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            maxlength="1000"
            show-word-limit
            placeholder="使用时长、配件是否齐全、有无瑕疵"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitPublish">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { itemApi } from '@/api/item'
import { useAddToCart } from '@/composables/useAddToCart'
import { useRequireLogin } from '@/composables/useRequireLogin'
import ItemCard from '@/components/ItemCard.vue'
import ImageUploader from '@/components/ImageUploader.vue'
import StatePanel from '@/components/StatePanel.vue'

const PAGE_SIZE = 12

const router = useRouter()
const { addToCart } = useAddToCart()
const requireLogin = useRequireLogin()

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
// 快速切换分类时会有多个请求在飞，只认最后一次的结果，避免旧响应覆盖新列表
let reloadSeq = 0

async function reload() {
  const seq = ++reloadSeq
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
    if (seq !== reloadSeq) return
    items.value = data?.list || []
    total.value = Number(data?.total || items.value.length)
    // page 模式不返回 hasMore，用「已加载数 < 总数」推断是否还能继续拉取
    hasMore.value = items.value.length < total.value
  } catch (err) {
    if (seq !== reloadSeq) return
    error.value = err.message || '请稍后重试'
    items.value = []
    total.value = 0
  } finally {
    if (seq === reloadSeq) loading.value = false
  }
}

/** 滚动加载：第 2 页起改用游标分页，避免后端深分页限制 */
async function loadMore() {
  if (!items.value.length) return
  const seq = reloadSeq
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
    // 期间用户换了筛选条件，这批数据已经不属于当前列表
    if (seq !== reloadSeq) return
    const list = data?.list || []
    items.value = [...items.value, ...list]
    hasMore.value = Boolean(data?.hasMore)
    cursor.value = data?.nextCursor || null
    cursorSortValue.value = data?.nextCursorSortValue ?? null
  } catch (err) {
    if (seq === reloadSeq) ElMessage.error(err.message || '加载更多失败')
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

/** 发布闲置：与社区提问保持一致，用一个弹窗承载，不单开路由 */
const publishVisible = ref(false)
const submitting = ref(false)
const publishFormRef = ref(null)
const form = reactive({
  name: '',
  price: 0,
  categoryId: '',
  conditionDesc: '',
  tradeLocation: '',
  image: '',
  description: ''
})
const rules = {
  name: [{ required: true, message: '请填写物品名称', trigger: 'blur' }],
  price: [{ required: true, message: '请填写价格', trigger: 'change' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

function openPublish() {
  if (!requireLogin('登录后才能发布闲置')) return
  publishVisible.value = true
}

async function submitPublish() {
  const valid = await publishFormRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await itemApi.publish({
      name: form.name.trim(),
      // 页面按「元」输入，后端按「分」存储，四舍五入避免浮点误差
      price: Math.round(Number(form.price) * 100),
      categoryId: form.categoryId,
      conditionDesc: form.conditionDesc.trim(),
      tradeLocation: form.tradeLocation.trim(),
      image: form.image.trim(),
      description: form.description.trim()
    })
    ElMessage.success('发布成功')
    publishVisible.value = false
    Object.assign(form, {
      name: '',
      price: 0,
      categoryId: '',
      conditionDesc: '',
      tradeLocation: '',
      image: '',
      description: ''
    })
    await reload()
  } catch {
    // 错误提示由 request 统一弹出
  } finally {
    submitting.value = false
  }
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

<style scoped>
/* 发布弹窗里的两列布局：价格+分类、成色+地点 */
.publish-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
@media (max-width: 560px) {
  .publish-row {
    grid-template-columns: 1fr;
  }
}
</style>
