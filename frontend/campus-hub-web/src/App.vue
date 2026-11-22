<template>
  <div class="app-shell" :class="{ 'has-tabbar': showTabbar }">
    <AppHeader v-if="!isBare" />
    <main class="app-main" :class="{ 'app-main-bare': isBare, 'app-main-full': isFull }">
      <RouterView />
    </main>
    <AppFooter v-if="!isBare && !isFull" />
    <!-- 右下角常驻助手入口：登录页、购物车/订单/个人中心这类个人事务页不出现 -->
    <AgentFab v-if="showFab" />
    <!-- 移动端底部导航：登录页（bare）和智能助手页（full，全屏对话）不显示 -->
    <MobileTabBar v-if="showTabbar" />
  </div>
</template>

<script setup>
import { computed, watchEffect } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
import MobileTabBar from '@/components/MobileTabBar.vue'
import AgentFab from '@/components/assistant/AgentFab.vue'
// 悬浮球在所有页面都要渲染，样式必须随主包加载，不能跟着助手页懒加载
import '@/styles/assistant.css'

const route = useRoute()
const isBare = computed(() => Boolean(route.meta.bare))
/** full：占满剩余视口高度、不渲染页脚（智能助手页用） */
const isFull = computed(() => Boolean(route.meta.full))
const showFab = computed(() => !isBare.value && !isFull.value && !route.meta.auth)
/** 底部导航：桌面端由 CSS 隐藏，这里只排除登录页与助手页 */
const showTabbar = computed(() => !isBare.value && !isFull.value)

/**
 * 同步一个 body 上的标记：弹窗遮罩（el-overlay）被 teleport 到 body 下，
 * 只有靠这个 class 才能让「遮罩止于底部导航上沿」的样式只在该生效的页面生效。
 */
watchEffect(() => {
  document.body.classList.toggle('has-tabbar', showTabbar.value)
})
</script>
