import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { createCartLine } from '@/models'

const SESSION_KEY = 'campushub_session'
const CART_KEY = 'campushub_cart'

function readJson(key, fallback) {
  try { return JSON.parse(localStorage.getItem(key)) || fallback } catch { return fallback }
}

export const useAppStore = defineStore('app', () => {
  const session = ref(readJson(SESSION_KEY, null))
  const cart = ref(readJson(CART_KEY, []))
  const isAuthenticated = computed(() => Boolean(session.value?.token))
  const cartCount = computed(() => cart.value.reduce((sum, line) => sum + line.quantity, 0))
  const cartTotal = computed(() => cart.value.reduce((sum, line) => sum + line.price * line.quantity, 0))

  function persist() {
    localStorage.setItem(SESSION_KEY, JSON.stringify(session.value))
    localStorage.setItem(CART_KEY, JSON.stringify(cart.value))
  }
  function setSession(nextSession) { session.value = nextSession; persist(); if (nextSession?.token) localStorage.setItem('campushub_token', nextSession.token) }
  function logout() { session.value = null; localStorage.removeItem('campushub_token'); persist() }
  function addToCart(item) {
    const existing = cart.value.find((line) => line.itemId === item.id)
    if (existing) existing.quantity += 1
    else cart.value.push(createCartLine(item))
    persist()
  }
  function updateQuantity(itemId, quantity) {
    const line = cart.value.find((item) => item.itemId === itemId)
    if (!line) return
    line.quantity = Math.max(1, quantity)
    persist()
  }
  function removeFromCart(itemId) { cart.value = cart.value.filter((item) => item.itemId !== itemId); persist() }
  function clearCart() { cart.value = []; persist() }

  return { session, cart, isAuthenticated, cartCount, cartTotal, setSession, logout, addToCart, updateQuantity, removeFromCart, clearCart }
})
