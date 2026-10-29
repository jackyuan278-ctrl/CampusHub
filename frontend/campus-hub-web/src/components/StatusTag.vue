<template>
  <span class="status-tag" :class="`is-${tone}`">{{ text }}</span>
</template>

<script setup>
defineProps({
  text: { type: String, default: '' },
  tone: { type: String, default: 'muted' }
})
</script>
