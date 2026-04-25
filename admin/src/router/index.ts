import { createRouter, createWebHistory } from 'vue-router'
import { useSessionStore } from '@/stores/session'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/login', name: 'Login', component: () => import('@/views/LoginView.vue') },
    {
      path: '/',
      component: () => import('@/views/AdminLayout.vue'),
      meta: { requiresAdmin: true },
      children: [
        { path: '', redirect: '/dashboard' },
        { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/DashboardView.vue') },
        { path: 'orders', name: 'Orders', component: () => import('@/views/OrdersView.vue') },
        { path: 'products', name: 'Products', component: () => import('@/views/ProductsView.vue') },
        { path: 'merchants', name: 'Merchants', component: () => import('@/views/MerchantsView.vue') },
        { path: 'users', name: 'Users', component: () => import('@/views/UsersView.vue') },
        { path: 'audit-logs', name: 'AuditLogs', component: () => import('@/views/AuditLogsView.vue') },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
  ],
})

router.beforeEach((to) => {
  const session = useSessionStore()
  if (to.name === 'Login') {
    return session.isAdmin ? '/dashboard' : true
  }
  if (to.meta.requiresAdmin && !session.isAdmin) {
    return '/login'
  }
  return true
})

export default router
