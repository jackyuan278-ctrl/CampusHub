<template>
  <footer class="app-footer">
    <div class="footer-inner">
      <div>
        <strong>CampusHub</strong>
        <p>让校园里的每一次相遇，都有迹可循。</p>
      </div>
      <div class="footer-links">
        <RouterLink to="/secondhand">二手交易</RouterLink>
        <RouterLink to="/activities">校园活动</RouterLink>
        <RouterLink to="/community">问答社区</RouterLink>
        <span>校园综合服务平台 · 第一版</span>
      </div>
    </div>
    <!-- 备案信息：管局要求备案号可见，且必须链接到工信部备案系统首页 -->
    <div class="footer-legal">
      <a :href="ICP_URL" target="_blank" rel="noopener noreferrer">{{ ICP_NO }}</a>
    </div>
  </footer>
</template>

<script setup>
/** 工信部备案系统首页——管局要求指向这里，不能指向具体查询页 */
const ICP_URL = 'https://beian.miit.gov.cn/'
const ICP_NO = '赣ICP备2026029696号'
</script>

<style scoped>
/* 页脚链接原本没有样式，几个链接和说明文字会连成一串（二手交易校园活动问答社区…） */
.footer-links {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 6px 20px;
  font-size: 13px;
}

.footer-legal {
  padding: 0 0 22px;
  text-align: center;
  font-size: 12px;
  line-height: 1.6;
}

.footer-legal a {
  color: var(--muted, #8a9a94);
  text-decoration: none;
}

.footer-legal a:hover {
  color: var(--primary-deep, #16755a);
  text-decoration: underline;
}
</style>
