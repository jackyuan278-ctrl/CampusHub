<template><section class="page-heading"><div><p class="eyebrow">SHOPPING CART</p><h1>我的购物车</h1><p>确认你的校园好物，准备提交订单。</p></div><el-button v-if="cart.length" type="primary" round @click="submitOrder">提交订单</el-button></section><el-empty v-if="!cart.length" description="购物车还是空的" /><div v-else class="cart-panel"><div v-for="item in cart" :key="item.id" class="cart-row"><div class="product-cover mini" :class="item.color"><el-icon :size="28"><component :is="item.icon" /></el-icon></div><div class="cart-info"><h3>{{ item.title }}</h3><p>{{ item.desc }}</p><span>数量：{{ item.quantity }}</span></div><strong>¥{{ item.price * item.quantity }}</strong><el-button text type="danger" @click="campus.removeFromCart(item.id)">移除</el-button></div><div class="cart-summary"><span>合计</span><strong>¥{{ campus.cartTotal }}</strong></div></div></template>
<script setup>
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useCampusStore } from '../stores/campus'
const campus = useCampusStore()
const cart = computed(() => campus.cart)
function submitOrder() { ElMessage.info('订单接口待后端联调，当前已保留购物车状态。') }
</script>
