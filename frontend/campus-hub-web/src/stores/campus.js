import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

const initialItems = [
  { id: 1, title: '九成新机械键盘', desc: '自用半年，功能正常，校内面交。', price: 129, category: '数码产品', icon: 'Monitor', color: 'blue' },
  { id: 2, title: '高等数学教材', desc: '配套习题册，适合大一课程复习。', price: 25, category: '教材书籍', icon: 'Reading', color: 'green' },
  { id: 3, title: '宿舍小风扇', desc: '静音三档风力，毕业出。', price: 35, category: '生活用品', icon: 'WindPower', color: 'orange' },
  { id: 4, title: '便携显示器', desc: '轻薄便携，适合宿舍和图书馆使用。', price: 260, category: '数码产品', icon: 'Cellphone', color: 'purple' }
]

export const useCampusStore = defineStore('campus', () => {
  const items = ref(initialItems)
  const cart = ref(JSON.parse(localStorage.getItem('campushub-cart') || '[]'))
  const currentUser = ref(JSON.parse(localStorage.getItem('campushub-user') || 'null'))

  const cartCount = computed(() => cart.value.reduce((sum, item) => sum + item.quantity, 0))
  const cartTotal = computed(() => cart.value.reduce((sum, item) => sum + item.price * item.quantity, 0))

  function persistCart() { localStorage.setItem('campushub-cart', JSON.stringify(cart.value)) }
  function addToCart(item) {
    const existing = cart.value.find(entry => entry.id === item.id)
    if (existing) existing.quantity += 1
    else cart.value.push({ ...item, quantity: 1 })
    persistCart()
  }
  function removeFromCart(id) {
    cart.value = cart.value.filter(item => item.id !== id)
    persistCart()
  }
  function setUser(user) {
    currentUser.value = user
    if (user) localStorage.setItem('campushub-user', JSON.stringify(user))
    else localStorage.removeItem('campushub-user')
  }
  function logout() { setUser(null) }

  return { items, cart, currentUser, cartCount, cartTotal, addToCart, removeFromCart, setUser, logout }
})
