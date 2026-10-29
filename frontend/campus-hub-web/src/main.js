import { createApp, watch } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as Icons from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import { setUnauthorizedHandler } from './api/http'
import { useAuthStore } from './stores/auth'
import { useCartStore } from './stores/cart'
import './styles/index.css'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia).use(router).use(ElementPlus, { locale: zhCn })
for (const [name, component] of Object.entries(Icons)) app.component(name, component)

// token 失效：清空登录态并带回跳地址
const auth = useAuthStore(pinia)
const cart = useCartStore(pinia)
setUnauthorizedHandler(() => {
  auth.reset()
  cart.reset()
  const current = router.currentRoute.value
  if (current.meta?.auth) {
    router.push({ path: '/login', query: { redirect: current.fullPath } })
  }
})

// 登录态变化时同步购物车（服务端存储，需重新拉取）
watch(
  () => auth.isLoggedIn,
  (loggedIn) => {
    if (loggedIn) cart.load()
    else cart.reset()
  },
  { immediate: true }
)

if (auth.isLoggedIn) auth.loadMe()

app.mount('#app')
