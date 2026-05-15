import { createRouter, createWebHistory } from 'vue-router'
import { useSessionStore } from '@/stores/session'

// 后台管理端路由表：这里管理的是后台页面路径。
// 生产部署时建议通过 VITE_APP_BASE=/admin/ 把后台挂到 `/admin/` 页面路径下。
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

// 后台路由守卫：只有 ADMIN / OPERATOR 会进入管理页面，普通用户会被拦回登录页。
// 后端仍会对 `/api/admin/**` 做最终 RBAC 校验。
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
