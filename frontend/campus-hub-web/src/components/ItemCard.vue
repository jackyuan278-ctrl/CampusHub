<template>
  <article class="product-card" :class="{ 'is-sold-out': soldOut }">
    <div class="product-cover" :class="tone" role="button" tabindex="0" @click="$emit('open', item)" @keyup.enter="$emit('open', item)" @keyup.space.prevent="$emit('open', item)">
      <img v-if="item.image" :src="item.image" :alt="item.name" loading="lazy" />
      <el-icon v-else :size="42"><component :is="iconOfCategory(item.categoryName)" /></el-icon>
      <span class="condition-pill">{{ soldOut ? '已售出' : (item.conditionDesc || '在售') }}</span>
      <div v-if="soldOut" class="sold-out-mask"><span>已售出</span></div>
    </div>
    <div class="product-body">
      <div class="item-meta">
        <span class="soft-tag">{{ item.categoryName || '未分类' }}</span>
        <span>{{ item.tradeLocation || '校内面交' }}</span>
      </div>
      <h3 @click="$emit('open', item)">{{ item.name }}</h3>
      <p>{{ item.description || '卖家暂未填写描述' }}</p>
      <div class="item-bottom">
        <strong>¥{{ money(item.price) }}</strong>
        <el-button type="primary" plain size="small" round :disabled="item.status !== 1" @click.stop="$emit('add', item)">
          <el-icon><ShoppingCart /></el-icon>{{ soldOut ? '已售出' : '加入购物车' }}
        </el-button>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import { iconOfCategory, money, toneOf } from '@/utils/format'

const props = defineProps({
  item: { type: Object, required: true }
})
defineEmits(['open', 'add'])

const tone = computed(() => toneOf(props.item.categoryId || props.item.categoryName))
/** 二手一物一件：status=3 表示这件货已经被买走 */
const soldOut = computed(() => props.item.status === 3)
</script>
