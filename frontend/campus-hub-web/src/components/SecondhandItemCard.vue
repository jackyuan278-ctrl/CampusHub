<template>
  <article class="item-card">
    <button class="item-visual" :class="item.accent" @click="$emit('open', item)">
      <el-icon :size="44"><component :is="coverIcon" /></el-icon>
      <span class="condition-pill">{{ item.condition }}</span>
    </button>
    <div class="item-content">
      <div class="item-meta"><span class="soft-tag">{{ item.category }}</span><span>{{ item.location }}</span></div>
      <h3 @click="$emit('open', item)">{{ item.title }}</h3>
      <p>{{ item.description }}</p>
      <div class="item-bottom"><div><strong>¥{{ item.price }}</strong><del>¥{{ item.originalPrice }}</del></div><el-button type="primary" plain size="small" round @click="$emit('add', item)"><el-icon><ShoppingCart /></el-icon>加入购物车</el-button></div>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({ item: { type: Object, required: true } })
defineEmits(['add', 'open'])
const coverIcons = { keyboard: 'Keyboard', book: 'Reading', fan: 'WindPower', monitor: 'Monitor', chair: 'Goods', notes: 'Notebook' }
const coverIcon = computed(() => coverIcons[props.item.cover] || 'Goods')
</script>

