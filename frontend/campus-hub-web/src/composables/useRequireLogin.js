import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

/** 需要登录的操作统一走这里：未登录先提示，再带 redirect 跳登录页 */
export function useRequireLogin() {
  const auth = useAuthStore()
  const router = useRouter()
  const route = useRoute()

  return function requireLogin(message = '登录后才能继续操作') {
    if (auth.isLoggedIn) return true
    ElMessage.warning(message)
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return false
  }
}
