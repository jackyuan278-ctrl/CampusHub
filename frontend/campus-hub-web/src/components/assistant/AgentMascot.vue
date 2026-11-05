<template>
  <svg
    class="asst-mascot-svg"
    :class="[`is-${variant}`, { 'is-busy': busy }]"
    :viewBox="viewBox"
    role="img"
    aria-label="CampusHub 智能助手"
  >
    <defs>
      <linearGradient id="wbHead" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stop-color="#ffffff" />
        <stop offset="55%" stop-color="#eef1f4" />
        <stop offset="100%" stop-color="#cfd6dc" />
      </linearGradient>
      <radialGradient id="wbScreen" cx="50%" cy="40%" r="70%">
        <stop offset="0%" stop-color="#25282e" />
        <stop offset="100%" stop-color="#0b0d10" />
      </radialGradient>
      <linearGradient id="wbEarCup" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stop-color="#50565e" />
        <stop offset="100%" stop-color="#2c3038" />
      </linearGradient>
      <linearGradient id="wbEye" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stop-color="#7affdc" />
        <stop offset="100%" stop-color="#2ee6a8" />
      </linearGradient>
      <filter id="wbGlow" x="-60%" y="-60%" width="220%" height="220%">
        <feGaussianBlur stdDeviation="3" />
      </filter>
    </defs>

    <!-- 阴影：探头模式 -->
    <ellipse v-if="variant === 'peek'" cx="84" cy="140" rx="40" ry="4" fill="#333" opacity=".1" />

    <!-- 猫耳朵：尖三角，更挺更窄 -->
    <path d="M 46 36 L 36 6 L 64 20 Z" fill="url(#wbHead)" />
    <path d="M 122 36 L 132 6 L 104 20 Z" fill="url(#wbHead)" />
    <!-- 内耳 -->
    <path d="M 48 32 L 42 14 L 58 22 Z" fill="#3a4048" />
    <path d="M 120 32 L 126 14 L 110 22 Z" fill="#3a4048" />

    <!-- 两侧耳机：圆形扁片 -->
    <ellipse cx="32" cy="66" rx="8" ry="11" fill="url(#wbEarCup)" />
    <ellipse cx="136" cy="66" rx="8" ry="11" fill="url(#wbEarCup)" />
    <ellipse cx="32" cy="66" rx="4" ry="6" fill="#6a7078" />
    <ellipse cx="136" cy="66" rx="4" ry="6" fill="#6a7078" />

    <!-- 头：稍椭圆，不是正圆 -->
    <ellipse cx="84" cy="64" rx="46" ry="42" fill="url(#wbHead)" />
    <!-- 高光 -->
    <ellipse cx="66" cy="40" rx="14" ry="7" fill="#fff" opacity=".5" />

    <!-- 黑色屏幕脸：横向圆角矩形 -->
    <rect x="50" y="42" width="68" height="42" rx="18" fill="url(#wbScreen)" />

    <!-- 眼睛：两个青绿色竖条 -->
    <g filter="url(#wbGlow)" opacity=".5">
      <rect class="asst-eye" x="66" y="54" width="8" height="18" rx="4" fill="#2ee6a8" />
      <rect class="asst-eye" x="94" y="54" width="8" height="18" rx="4" fill="#2ee6a8" />
    </g>
    <g class="asst-eyes">
      <rect class="asst-eye" x="66" y="54" width="8" height="18" rx="4" fill="url(#wbEye)" />
      <rect class="asst-eye" x="94" y="54" width="8" height="18" rx="4" fill="url(#wbEye)" />
    </g>

    <!-- 小天线 -->
    <line x1="84" y1="24" x2="84" y2="14" stroke="#cfd6dc" stroke-width="2.5" stroke-linecap="round" />
    <circle cx="84" cy="12" r="3" fill="#2ee6a8" />

    <!-- 身体：窄一点，不要圆肚子 -->
    <g v-if="variant === 'peek'">
      <rect x="56" y="100" width="56" height="38" rx="16" fill="url(#wbHead)" />
      <!-- 小手扒边 -->
      <ellipse cx="44" cy="120" rx="7" ry="6" fill="url(#wbHead)" />
      <ellipse cx="124" cy="120" rx="7" ry="6" fill="url(#wbHead)" />
    </g>
  </svg>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  busy: { type: Boolean, default: false },
  /** peek = 扒着边缘探头；face = 只有脑袋，用于右下角悬浮球 */
  variant: { type: String, default: 'peek' }
})

const viewBox = computed(() => (props.variant === 'face' ? '24 5 120 95' : '24 5 120 140'))
</script>
