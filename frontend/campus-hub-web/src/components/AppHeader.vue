<template>
  <header class="topbar">
    <div class="topbar-inner">
      <RouterLink to="/home" class="brand">
        <span class="brand-mark">C</span>
        <span class="brand-text">
          <strong>CampusHub</strong>
          <small>校园生活服务平台</small>
        </span>
      </RouterLink>

      <nav class="main-nav" aria-label="主导航">
        <RouterLink v-for="entry in navItems" :key="entry.to" :to="entry.to" class="nav-link" active-class="is-active">
          <el-icon><component :is="entry.icon" /></el-icon>{{ entry.label }}
        </RouterLink>
      </nav>

      <div class="header-actions">
        <el-dropdown trigger="click" placement="bottom-end" @command="handlePublish">
          <button class="publish-button" type="button">
            <el-icon><Plus /></el-icon><span>发布</span>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="activity">发起校园活动</el-dropdown-item>
              <el-dropdown-item command="question">提一个问题</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <RouterLink to="/cart" class="cart-link" :class="{ 'has-items': cart.count > 0 }">
          <el-icon><ShoppingCart /></el-icon>
          <span>购物车</span>
          <b v-if="cart.count">{{ cart.count > 99 ? '99+' : cart.count }}</b>
        </RouterLink>

        <el-dropdown v-if="auth.isLoggedIn" trigger="click" placement="bottom-end" @command="handleCommand">
          <button class="profile-button" type="button">
            <el-avatar :size="30" :src="auth.user?.avatar || defaultAvatar" class="avatar-sm" />
            <span class="profile-name">{{ auth.displayName }}</span>
            <el-icon><ArrowDown /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="orders">我的订单</el-dropdown-item>
              <el-dropdown-item command="signups">我的报名</el-dropdown-item>
              <el-dropdown-item command="addresses" divided>收货地址</el-dropdown-item>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <RouterLink v-else :to="{ path: '/login', query: { redirect: route.fullPath } }" class="login-link">
          登录 / 注册
        </RouterLink>
      </div>
    </div>
  </header>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import defaultAvatar from '@/assets/avatar-default.svg'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const cart = useCartStore()

const navItems = [
  { to: '/home', label: '首页', icon: 'House' },
  { to: '/secondhand', label: '校园二手', icon: 'Goods' },
  { to: '/activities', label: '校园活动', icon: 'Calendar' },
  { to: '/community', label: '问答社区', icon: 'ChatDotRound' }
]

function requireLogin(redirect) {
  ElMessage.warning('请先登录后再操作')
  router.push({ path: '/login', query: { redirect } })
}

function handlePublish(command) {
  const target = command === 'activity' ? '/activities' : '/community'
  if (!auth.isLoggedIn) return requireLogin(`${target}?action=publish`)
  router.push({ path: target, query: { action: 'publish' } })
}

function handleCommand(command) {
  if (command === 'logout') {
    auth.reset()
    cart.reset()
    ElMessage.success('已退出登录')
    router.push('/home')
    return
  }
  if (command === 'profile') router.push('/profile')
  if (command === 'orders') router.push('/orders')
  if (command === 'signups') router.push({ path: '/profile', query: { tab: 'signups' } })
  if (command === 'addresses') router.push({ path: '/profile', query: { tab: 'addresses' } })
}
</script>
