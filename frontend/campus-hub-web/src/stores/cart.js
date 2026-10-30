import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { cartApi } from '@/api/cart'
import { useAuthStore } from './auth'

/**
 * 购物车以服务端为准（Redis 按用户隔离）。
 * 本地不再保存副本，避免多端不同步。
 */
export const useCartStore = defineStore('cart', () => {
  const items = ref([])
  const loading = ref(false)
  const ready = ref(false)
  const loadError = ref('')
  const auth = useAuthStore()

  const count = computed(() => items.value.reduce((sum, line) => sum + (line.num || 0), 0))
  const totalFee = computed(() => items.value.reduce((sum, line) => sum + (line.price || 0) * (line.num || 0), 0))
  const isEmpty = computed(() => items.value.length === 0)

  function reset() {
    items.value = []
    ready.value = false
    loadError.value = ''
  }

  async function load(options = {}) {
    if (!auth.isLoggedIn) {
      reset()
      return []
    }
    loading.value = true
    try {
      const data = await cartApi.list({ silent: true, ...options })
      items.value = Array.isArray(data) ? data : []
      ready.value = true
      loadError.value = ''
      return items.value
    } catch (error) {
      // 失败时保留上一次的结果，并把错误交给页面渲染：
      // 直接清空会让用户以为购物车被清光了
      loadError.value = error?.message || '购物车加载失败'
      return items.value
    } finally {
      loading.value = false
    }
  }

  async function add(itemId, num = 1) {
    await cartApi.add(itemId, num)
    await load()
  }

  async function updateNum(itemId, num) {
    const target = items.value.find((line) => line.itemId === itemId)
    if (target) target.num = num
    await cartApi.updateNum(itemId, num)
    await load()
  }

  async function remove(itemId) {
    await cartApi.remove(itemId)
    await load()
  }

  async function clear() {
    await cartApi.clear()
    reset()
  }

  return { items, loading, ready, loadError, count, totalFee, isEmpty, load, add, updateNum, remove, clear, reset }
})
