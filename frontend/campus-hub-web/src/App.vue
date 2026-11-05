<template>
  <div class="app-shell">
    <AppHeader v-if="!isBare" />
    <main class="app-main" :class="{ 'app-main-bare': isBare, 'app-main-full': isFull }">
      <RouterView />
    </main>
    <AppFooter v-if="!isBare && !isFull" />
    <!-- 右下角常驻助手入口：登录页、购物车/订单/个人中心这类个人事务页不出现 -->
    <AgentFab v-if="showFab" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
import AgentFab from '@/components/assistant/AgentFab.vue'
// 悬浮球在所有页面都要渲染，样式必须随主包加载，不能跟着助手页懒加载
import '@/styles/assistant.css'

const route = useRoute()
const isBare = computed(() => Boolean(route.meta.bare))
/** full：占满剩余视口高度、不渲染页脚（智能助手页用） */
const isFull = computed(() => Boolean(route.meta.full))
const showFab = computed(() => !isBare.value && !isFull.value && !route.meta.auth)
</script>
