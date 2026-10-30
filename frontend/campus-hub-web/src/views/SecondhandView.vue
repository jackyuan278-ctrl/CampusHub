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

    <div v-if="!error && (items.length || page > 1)" class="pager-row">
      <el-button round :disabled="page <= 1 || loading" @click="previous">上一页</el-button>
      <template v-if="page <= MAX_OFFSET_PAGE">
        <button
          v-for="pageNumber in visiblePages"
          :key="pageNumber"
          type="button"
          class="pager-number"
          :class="{ 'is-active': pageNumber === page }"
          :disabled="loading"
          @click="goToPage(pageNumber)"
        >{{ pageNumber }}</button>
      </template>
      <span v-else class="pager-current">第 {{ page }} 页</span>
      <el-button round :disabled="!hasMore || loading" @click="next">下一页</el-button>
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
const error = ref('')
const hasMore = ref(false)
const page = ref(1)
/** 后端前 5 页支持 offset，第 6 页起必须走游标，与后端 MAX_OFFSET_PAGE 保持一致 */
const MAX_OFFSET_PAGE = 5
const visiblePages = ref([1])
/** 游标栈：cursorStack[i] 是「第 MAX_OFFSET_PAGE+1+i 页」继续取数时用的游标（上一页末条） */
const cursorStack = ref([])
let searchTimer = null

async function loadCategories() {
  try {
    const data = await itemApi.categories({ silent: true })
    categories.value = Array.isArray(data) ? data : []
  } catch {
    categories.value = []
  }
}

/** 游标里要带的排序值：只有价格排序需要，综合/默认排序只靠 id 递进 */
function sortValueOf(item) {
  return sort.value === 'priceAsc' || sort.value === 'priceDesc' ? item.price : null
}

/** 按当前页码取数：前 5 页走 offset，第 6 页起改走游标，避免深分页 LIMIT offset 全表扫 */
// 快速翻页/切分类时会有多个请求在飞，只认最后一次的结果，避免旧响应覆盖新列表
let reloadSeq = 0

async function load() {
  const seq = ++reloadSeq
  loading.value = true
  error.value = ''
  try {
    const params = {
      pageSize: PAGE_SIZE,
      keyword: keyword.value.trim() || undefined,
      categoryId: categoryId.value || undefined,
      sort: sort.value || undefined
    }
    if (page.value <= MAX_OFFSET_PAGE) {
      params.page = page.value
    } else {
      const cursor = cursorStack.value[page.value - MAX_OFFSET_PAGE - 1] || {}
      params.cursorId = cursor.id
      if (cursor.sortValue != null) {
        params.cursorSortValue = cursor.sortValue
      }
    }
    const data = await itemApi.page(params, { silent: true })
    if (seq !== reloadSeq) return
    const list = data?.list || []
    items.value = list
    if (data?.total != null) {
      total.value = Number(data.total)
    }
    // offset 阶段后端不给 hasMore，用「本页末条序号 < 总数」推算；游标阶段直接用后端结果
    hasMore.value = page.value <= MAX_OFFSET_PAGE
      ? page.value * PAGE_SIZE < total.value
      : Boolean(data?.hasMore)
    // 把「下一页从哪继续」记下来，深页的上一页/下一页都靠它重放，不用重新算
    if (page.value === MAX_OFFSET_PAGE && hasMore.value) {
      const last = list[list.length - 1]
      cursorStack.value[0] = last ? { id: last.id, sortValue: sortValueOf(last) } : null
    } else if (page.value > MAX_OFFSET_PAGE && data?.nextCursor != null) {
      cursorStack.value[page.value - MAX_OFFSET_PAGE] = {
        id: data.nextCursor,
        sortValue: data.nextCursorSortValue ?? null
      }
    }
    if (page.value <= MAX_OFFSET_PAGE) {
      const pageCount = Math.min(MAX_OFFSET_PAGE, Math.max(1, Math.ceil(total.value / PAGE_SIZE)))
      visiblePages.value = Array.from({ length: pageCount }, (_, index) => index + 1)
    }
  } catch (err) {
    if (seq !== reloadSeq) return
    error.value = err.message || '请稍后重试'
    items.value = []
  } finally {
    if (seq === reloadSeq) loading.value = false
  }
}

function goToPage(pageNumber) {
  if (loading.value || pageNumber === page.value) return
  if (pageNumber < 1 || pageNumber > MAX_OFFSET_PAGE) return
  page.value = pageNumber
  load()
}

function next() {
  if (!hasMore.value || loading.value) return
  page.value += 1
  load()
}

function previous() {
  if (page.value <= 1 || loading.value) return
  page.value -= 1
  load()
}

/** 筛选条件变化：回到第 1 页，游标栈整体作废 */
function reload() {
  page.value = 1
  cursorStack.value = []
  visiblePages.value = [1]
  total.value = 0
  hasMore.value = false
  load()
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

/* 分页器：前 5 页给页码，第 6 页起只保留上一页/下一页 + 当前页码 */
.pager-row {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 8px;
  margin: 28px 0 8px;
}

.pager-row .pager-number {
  min-width: 34px;
  height: 34px;
  padding: 0 10px;
  border: 1px solid var(--el-border-color);
  border-radius: 10px;
  background: var(--el-bg-color);
  color: var(--el-text-color-regular);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.18s ease;
}

.pager-row .pager-number:hover {
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
}

.pager-row .pager-number.is-active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary);
  color: #fff;
  cursor: default;
}

/* 翻页请求在飞时禁止连点，但当前页不要跟着变灰 */
.pager-row .pager-number:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.pager-row .pager-number.is-active:disabled {
  opacity: 1;
}

.pager-row .pager-current {
  min-width: 68px;
  text-align: center;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}
</style>
