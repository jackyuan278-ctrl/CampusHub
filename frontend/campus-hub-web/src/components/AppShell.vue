<template>
  <div class="shell" :class="{ 'shell-auth': isAuthRoute }">
    <header v-if="!isAuthRoute" class="app-header">
      <div class="header-inner">
        <RouterLink to="/home" class="brand-lockup">
          <span class="brand-mark">C</span>
          <span><strong>CampusHub</strong><small>校园生活服务平台</small></span>
        </RouterLink>
        <nav class="main-nav" aria-label="主导航">
          <RouterLink v-for="item in navItems" :key="item.to" :to="item.to" class="nav-link" active-class="is-active">
            <el-icon><component :is="item.icon" /></el-icon>{{ item.label }}
          </RouterLink>
        </nav>
        <div class="header-actions">
          <RouterLink v-if="isShoppingRoute" to="/cart" class="cart-link" :class="{ 'has-items': appStore.cartCount }">
            <el-icon><ShoppingCart /></el-icon><span>购物车</span><b v-if="appStore.cartCount">{{ appStore.cartCount }}</b>
          </RouterLink>
          <el-dropdown v-if="appStore.isAuthenticated" trigger="click" @command="handleCommand">
            <button class="profile-button"><span class="avatar avatar-sm">{{ appStore.session.user?.name?.slice(0, 1) || '校' }}</span><span class="profile-name">{{ appStore.session.user?.name || '校园用户' }}</span><el-icon><ArrowDown /></el-icon></button>
            <template #dropdown><el-dropdown-menu><el-dropdown-item command="profile">个人中心</el-dropdown-item><el-dropdown-item divided command="logout">退出登录</el-dropdown-item></el-dropdown-menu></template>
          </el-dropdown>
          <RouterLink v-else :to="{ path: '/login', query: { redirect: route.fullPath } }" class="login-link">登录 / 注册</RouterLink>
        </div>
      </div>
    </header>
    <main class="app-main" :class="{ 'app-main-auth': isAuthRoute }"><RouterView /></main>
    <footer v-if="!isAuthRoute" class="app-footer"><div class="footer-inner"><div><strong>CampusHub</strong><p>让校园里的每一次相遇，都有迹可循。</p></div><span>校园综合服务平台 · 第一版</span></div></footer>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const isAuthRoute = computed(() => Boolean(route.meta.bare))
const isShoppingRoute = computed(() => ['/secondhand', '/cart'].includes(route.path))
const navItems = [
  { to: '/home', label: '首页', icon: 'House' },
  { to: '/secondhand', label: '校园二手', icon: 'Goods' },
  { to: '/activities', label: '校园活动', icon: 'Calendar' },
  { to: '/community', label: '问答社区', icon: 'ChatDotRound' }
]
function handleCommand(command) {
  if (command === 'logout') { appStore.logout(); router.push('/home') }
  if (command === 'profile') router.push('/home#profile')
}
</script>
