<template>
  <div v-if="Number(total) > Number(pageSize)" class="pager-bar">
    <span class="pager-total">共 {{ total }} 条</span>
    <el-pagination
      background
      layout="prev, pager, next"
      :current-page="Number(page)"
      :page-size="Number(pageSize)"
      :total="Number(total) || 0"
      hide-on-single-page
      @current-change="(next) => $emit('change', next)"
    />
  </div>
</template>

<script setup>
defineProps({
  total: { type: [Number, String], default: 0 },
  page: { type: [Number, String], default: 1 },
  pageSize: { type: [Number, String], default: 10 }
})
defineEmits(['change'])
</script>
