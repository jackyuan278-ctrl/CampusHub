<template>
  <div v-if="mockState.active" class="mock-banner">
    <el-icon><WarningFilled /></el-icon>
    <span>后端未连接，当前展示的是本地演示数据（接口路径与字段结构与真实服务一致）</span>
    <button type="button" @click="reload">重试连接</button>
  </div>
</template>

<script setup>
import { mockState } from '@/api/mock'

function reload() {
  window.location.reload()
}
</script>
