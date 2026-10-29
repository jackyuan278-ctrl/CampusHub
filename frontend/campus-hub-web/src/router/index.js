import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  { path: '/', redirect: '/home' },
  { path: '/home', name: 'home', component: () => import('@/views/HomeView.vue'), meta: { title: '首页' } },
  { path: '/secondhand', name: 'secondhand', component: () => import('@/views/SecondhandView.vue'), meta: { title: '校园二手' } },
  { path: '/secondhand/:id', name: 'item-detail', component: () => import('@/views/ItemDetailView.vue'), meta: { title: '物品详情' } },
  { path: '/activities', name: 'activities', component: () => import('@/views/ActivitiesView.vue'), meta: { title: '校园活动' } },
  { path: '/activities/:id', name: 'activity-detail', component: () => import('@/views/ActivityDetailView.vue'), meta: { title: '活动详情' } },
  { path: '/community', name: 'community', component: () => import('@/views/CommunityView.vue'), meta: { title: '问答社区' } },
  { path: '/community/:id', name: 'question-detail', component: () => import('@/views/QuestionDetailView.vue'), meta: { title: '问题详情' } },
  { path: '/cart', name: 'cart', component: () => import('@/views/CartView.vue'), meta: { title: '购物车', auth: true } },
  { path: '/checkout', name: 'checkout', component: () => import('@/views/CheckoutView.vue'), meta: { title: '确认订单', auth: true } },
  { path: '/orders', name: 'orders', component: () => import('@/views/OrdersView.vue'), meta: { title: '我的订单', auth: true } },
  { path: '/orders/:id', name: 'order-detail', component: () => import('@/views/OrderDetailView.vue'), meta: { title: '订单详情', auth: true } },
  { path: '/profile', name: 'profile', component: () => import('@/views/ProfileView.vue'), meta: { title: '个人中心', auth: true } },
  { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { title: '登录', bare: true } },
  { path: '/:pathMatch(.*)*', redirect: '/home' }
]

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.auth && !auth.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.bare && auth.isLoggedIn) {
    return { path: '/home' }
  }
  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · CampusHub` : 'CampusHub 校园生活服务平台'
})

export default router
