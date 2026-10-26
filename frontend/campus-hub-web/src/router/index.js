import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import SecondhandView from '@/views/SecondhandView.vue'
import ActivitiesView from '@/views/ActivitiesView.vue'
import CommunityView from '@/views/CommunityView.vue'
import CartView from '@/views/CartView.vue'
import LoginView from '@/views/LoginView.vue'

export default createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    { path: '/', redirect: '/home' },
    { path: '/home', name: 'home', component: HomeView },
    { path: '/secondhand', name: 'secondhand', component: SecondhandView },
    { path: '/activities', name: 'activities', component: ActivitiesView },
    { path: '/community', name: 'community', component: CommunityView },
    { path: '/cart', name: 'cart', component: CartView },
    { path: '/login', name: 'login', component: LoginView, meta: { bare: true } }
  ]
})
