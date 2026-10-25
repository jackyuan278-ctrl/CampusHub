import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import SecondhandView from '@/views/SecondhandView.vue'
import ActivitiesView from '@/views/ActivitiesView.vue'
import CommunityView from '@/views/CommunityView.vue'
import CartView from '@/views/CartView.vue'
import LoginView from '@/views/LoginView.vue'

export default createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/home' },
    { path: '/home', component: HomeView },
    { path: '/secondhand', component: SecondhandView },
    { path: '/activities', component: ActivitiesView },
    { path: '/community', component: CommunityView },
    { path: '/cart', component: CartView },
    { path: '/login', component: LoginView, meta: { bare: true } }
  ]
})
