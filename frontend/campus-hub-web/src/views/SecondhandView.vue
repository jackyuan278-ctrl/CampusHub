<template><section class="page-heading"><div><p class="eyebrow">SECOND-HAND MARKET</p><h1>校园二手交易</h1><p>让闲置物品在校园里流动起来。</p></div><el-button type="primary" round @click="ElMessage.info('发布接口将在后端联调阶段接入')">发布闲置</el-button></section><div class="toolbar"><el-input v-model="keyword" placeholder="搜索商品名称" :prefix-icon="Search" clearable class="search-input"/><el-select v-model="category" placeholder="全部分类" class="category-select"><el-option label="全部分类" value="all"/><el-option label="数码产品" value="digital"/><el-option label="教材书籍" value="books"/><el-option label="生活用品" value="life"/></el-select></div><div class="product-grid"><article v-for="item in filtered" :key="item.id" class="product-card"><div class="product-cover" :class="item.color"><span class="condition-pill">{{item.condition}}</span><el-icon :size="42"><component :is="item.icon"/></el-icon></div><div class="product-body"><div class="tag">{{item.category}}</div><h3>{{item.title}}</h3><p>{{item.desc}}</p><div class="product-meta"><span><el-icon><Location/></el-icon>{{item.location}}</span><span>{{item.seller}}</span></div><div class="product-footer"><div class="price-line"><strong>¥{{item.price}}</strong><del v-if="item.originalPrice">¥{{item.originalPrice}}</del></div><el-button type="primary" link @click="campus.addToCart(item)">加入购物车</el-button></div></div></article></div></template>
<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { useCampusStore } from '../stores/campus'
import { campusHubApi } from '@/api/campusHubApi'
const campus = useCampusStore()
const keyword = ref(''); const category = ref('all'); const items = ref([])
onMounted(async () => { items.value = await campusHubApi.listSecondhand() })
const filtered = computed(() => items.value.filter(x => (category.value === 'all' || x.category === ({ digital: '数码产品', books: '教材书籍', life: '生活用品' }[category.value])) && x.title.includes(keyword.value)))
</script>
