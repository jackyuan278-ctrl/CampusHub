<template>
  <div class="app-shell">
    <header class="topbar"><div class="topbar-inner">
      <div class="brand" @click="$router.push('/home')"><strong>CampusHub</strong><small>校园生活服务平台</small></div>
      <el-menu :default-active="$route.path" mode="horizontal" router :ellipsis="false" class="nav-menu">
        <el-menu-item index="/home">首页</el-menu-item><el-menu-item index="/secondhand">二手交易</el-menu-item><el-menu-item index="/activities">校园活动</el-menu-item><el-menu-item index="/community">问答社区</el-menu-item>
      </el-menu>
      <div class="user-area"><el-button v-if="['/secondhand','/cart'].includes($route.path)" text @click="$router.push('/cart')"><el-icon><ShoppingCart /></el-icon>购物车 <el-badge v-if="campus.cartCount" :value="campus.cartCount" class="cart-badge" /></el-button><el-button v-if="campus.currentUser" text @click="campus.logout">{{ campus.currentUser.nickname }}</el-button><el-button v-else type="primary" plain round @click="$router.push({ path: '/login', query: { redirect: $route.fullPath } })">登录</el-button></div>
    </div></header>
    <main class="page-wrap"><router-view /></main>
  </div>
</template>
<script setup>
import { useCampusStore } from './stores/campus'
const campus = useCampusStore()
</script>
