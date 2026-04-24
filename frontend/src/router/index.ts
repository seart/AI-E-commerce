import { createRouter, createWebHistory } from 'vue-router'
import { hasStoredSession } from '@/services/session'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'Home',
      component: () => import('@/views/home/Home.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/Login.vue'),
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/register/Register.vue'),
    },
    {
      path: '/search',
      name: 'Search',
      component: () => import('@/views/search/SearchView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/search-list',
      name: 'SearchList',
      component: () => import('@/views/search/SearchList.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/merchant/:id',
      name: 'MerchantDetail',
      component: () => import('@/views/merchant/MerchantDetail.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/cart',
      name: 'CartView',
      component: () => import('@/views/cart/CartView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/checkout',
      name: 'CheckoutView',
      component: () => import('@/views/checkout/CheckoutView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/orders',
      name: 'OrdersView',
      component: () => import('@/views/orders/OrdersView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/profile',
      name: 'ProfileView',
      component: () => import('@/views/profile/ProfileView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/address',
      name: 'AddressList',
      component: () => import('@/views/address/AddressList.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/address/edit',
      name: 'AddressEdit',
      component: () => import('@/views/address/AddressEdit.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
})

router.beforeEach((to) => {
  const hasSession = hasStoredSession()
  const { name } = to
  const isLoginOrRegister = name === 'Login' || name === 'Register'

  if (isLoginOrRegister && hasSession) {
    return { name: 'Home' }
  }
  if (!to.meta.requiresAuth || hasSession || isLoginOrRegister) {
    return true
  }
  return { name: 'Login' }
})

export default router
