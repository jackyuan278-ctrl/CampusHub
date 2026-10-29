import { ElMessage } from 'element-plus'
import { useCartStore } from '@/stores/cart'
import { useRequireLogin } from './useRequireLogin'

/** 加入购物车：统一登录校验 + 成功提示，商品来源可能是 ItemVO 或列表项 */
export function useAddToCart() {
  const cart = useCartStore()
  const requireLogin = useRequireLogin()

  return {
    async addToCart(item, num = 1) {
      if (!requireLogin('登录后才能加入购物车')) return false
      try {
        await cart.add(item.id, num)
        ElMessage.success(`已加入购物车：${item.name || '商品'}`)
        return true
      } catch {
        return false
      }
    }
  }
}
