<template>
  <div class="app-shell">
    <AppHeader v-if="!isBare" />
    <main class="app-main" :class="{ 'app-main-bare': isBare }">
      <RouterView />
    </main>
    <AppFooter v-if="!isBare" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'

const route = useRoute()
const isBare = computed(() => Boolean(route.meta.bare))
</script>
