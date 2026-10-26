<template>
  <section class="section-heading"><div><span v-if="eyebrow" class="eyebrow">{{ eyebrow }}</span><h1>{{ title }}</h1><p v-if="description">{{ description }}</p></div><div v-if="$slots.actions" class="heading-actions"><slot name="actions" /></div></section>
</template>
<script setup>
defineProps({ eyebrow: String, title: { type: String, required: true }, description: String })
</script>
