<template>
  <button type="button" class="asst-fab" aria-label="打开 CampusHub 智能助手" @click="open">
    <span class="asst-fab-halo" aria-hidden="true" />
    <AgentMascot variant="face" class="asst-fab-mascot" />
    <span class="asst-fab-bubble">有什么想问的？</span>
  </button>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import AgentMascot from './AgentMascot.vue'

const router = useRouter()
const route = useRoute()

function open() {
  if (route.name === 'assistant') return
  router.push('/assistant')
}
</script>
