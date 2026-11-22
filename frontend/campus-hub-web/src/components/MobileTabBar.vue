<template>
  <!-- 移动端底部导航：900px 以下才显示，桌面端完全由 CSS 隐藏 -->
  <nav class="mtb" aria-label="底部导航">
    <RouterLink
      v-for="item in items"
      :key="item.to"
      :to="item.to"
      class="mtb-item"
      :class="{ 'is-active': isActive(item.to) }"
    >
      <span class="mtb-icon-wrap">
        <el-icon class="mtb-icon"><component :is="item.icon" /></el-icon>
      </span>
      <span class="mtb-label">{{ item.label }}</span>
    </RouterLink>
  </nav>
</template>

<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()

/**
 * 五项对齐顶部导航，但把「智能助手」换成「我的」：
 * 助手有右下角悬浮球常驻入口，个人中心在手机上原本藏在头像下拉里，更需要一个直达入口。
 * 购物车不占 tab——顶部导航的购物车图标在手机上仍然可见。
 */
const items = [
  { to: '/home', label: '首页', icon: 'House' },
  { to: '/secondhand', label: '二手', icon: 'Goods' },
  { to: '/activities', label: '活动', icon: 'Calendar' },
  { to: '/community', label: '社区', icon: 'ChatDotRound' },
  { to: '/profile', label: '我的', icon: 'User' }
]

/** 详情页也算所属栏目选中：/activities/12 → 「活动」高亮 */
function isActive(to) {
  return route.path === to || route.path.startsWith(`${to}/`)
}
</script>

<style scoped>
/* 桌面端不渲染，确保不影响现有布局 */
.mtb {
  display: none;
}

@media (max-width: 900px) {
  .mtb {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 950;
    /* iOS/微信在滚动时会重排 fixed 元素，独立合成层能减少闪烁 */
    transform: translateZ(0);
    display: grid;
    /* minmax(0, 1fr)：1fr 的最小值是 min-content，五列会被内容撑宽并挤出屏幕 */
    grid-template-columns: repeat(5, minmax(0, 1fr));
    width: 100%;
    box-sizing: border-box;
    /* iPhone 底部横条区域留白，否则最后一排会被小横条压住 */
    padding-bottom: env(safe-area-inset-bottom, 0px);
    border-top: 1px solid var(--line, #e8eeeb);
    /* 用不透明底色而不是 backdrop-filter：
       实时背景模糊在滚动时要每帧重算，微信 X5 内核上掉帧很明显 */
    background: #fff;
    /* 把绘制范围限制在自身，滚动时不必让父级跟着重绘 */
    contain: layout paint;
  }

  .mtb-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 3px;
    height: 56px;
    color: var(--muted, #8a9a94);
    text-decoration: none;
    /* 手指点击时不要出现蓝色高亮块 */
    -webkit-tap-highlight-color: transparent;
    transition: color 0.15s;
  }

  .mtb-icon-wrap {
    display: grid;
    place-items: center;
    width: 30px;
    height: 24px;
    border-radius: 8px;
    transition: background 0.18s;
  }

  .mtb-icon {
    font-size: 20px;
  }

  .mtb-label {
    font-size: 10px;
    line-height: 1;
    letter-spacing: 0.02em;
  }

  /* 选中态：图标底色 + 文字变主题绿，不靠下划线（手机上太细看不见） */
  .mtb-item.is-active {
    color: var(--primary-deep, #16755a);
    font-weight: 700;
  }

  .mtb-item.is-active .mtb-icon-wrap {
    background: var(--mint, #e6f7ef);
  }

  .mtb-item:active .mtb-icon-wrap {
    transform: scale(0.94);
  }
}
</style>

<style>
/* 全局：给页面底部让出导航高度，避免最后一屏内容被遮。
   只对 .page-container 生效——助手页和登录页不用这个容器，且它们本身不显示导航。 */
@media (max-width: 900px) {
  .app-shell.has-tabbar .page-container {
    padding-bottom: calc(96px + env(safe-area-inset-bottom, 0px));
  }
  /* 悬浮球原本 bottom:26px，会被 56px 高的底部导航压住，整体上移到导航上方 */
  .app-shell.has-tabbar .asst-fab {
    bottom: calc(74px + env(safe-area-inset-bottom, 0px));
  }
  /* 页脚不在 .page-container 里，单独让位。
     注意是给整个 footer 加内距，而不是 footer-inner——备案号在 footer-inner 之后，
     只给 inner 加内距会让备案行正好落在底部导航下面 */
  .app-shell.has-tabbar .app-footer {
    padding-bottom: calc(64px + env(safe-area-inset-bottom, 0px));
  }
}
</style>
