import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { userApi } from '@/api/user'
import { getToken, setToken } from '@/api/http'

const USER_KEY = 'campushub_user'

function readCachedUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY)) || null
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(getToken())
  const user = ref(readCachedUser())
  const loading = ref(false)

  const isLoggedIn = computed(() => Boolean(token.value))
  const displayName = computed(() => user.value?.nickname || user.value?.username || '校园用户')
  const balance = computed(() => user.value?.balance ?? 0)

  function cacheUser(value) {
    user.value = value
    if (value) localStorage.setItem(USER_KEY, JSON.stringify(value))
    else localStorage.removeItem(USER_KEY)
  }

  async function loadMe() {
    if (!token.value) return null
    try {
      const data = await userApi.me({ silent: true })
      cacheUser(data)
      return data
    } catch (error) {
      // token 失效时清空本地登录态，避免页面反复 401
      if (Number(error.code) === 401) reset()
      return null
    }
  }

  async function login(payload) {
    loading.value = true
    try {
      const data = await userApi.login(payload)
      token.value = data.token
      setToken(data.token)
      await loadMe()
      return data
    } finally {
      loading.value = false
    }
  }

  async function register(payload) {
    await userApi.register(payload)
    return login({ username: payload.username, password: payload.password })
  }

  function reset() {
    token.value = ''
    setToken('')
    cacheUser(null)
  }

  return { token, user, loading, isLoggedIn, displayName, balance, loadMe, login, register, reset, cacheUser }
})
