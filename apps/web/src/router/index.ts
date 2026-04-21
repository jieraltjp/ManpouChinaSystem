/**
 * 路由定义 + 路由守卫。
 */
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/pages/auth/LoginPage.vue'),
    meta: { requiresAuth: false, title: '登录' },
  },
  {
    path: '/',
    component: () => import('@/layouts/AppLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/dashboard',
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/pages/dashboard/DashboardPage.vue'),
        meta: { title: '仪表盘', requiresAuth: true },
      },
      {
        path: 'procurement',
        name: 'Procurement',
        redirect: '/procurement/order',
        meta: { title: '发注管理', requiresAuth: true },
        children: [
          {
            path: 'demand',
            name: 'Demand',
            component: () => import('@/pages/procurement/DemandPage.vue'),
            meta: { title: '补货需求', requiresAuth: true },
          },
          {
            path: 'order',
            name: 'Order',
            component: () => import('@/pages/procurement/OrderPage.vue'),
            meta: { title: '发注单', requiresAuth: true },
          },
          {
            path: 'inspection',
            name: 'Inspection',
            component: () => import('@/pages/procurement/InspectionPage.vue'),
            meta: { title: '验货记录', requiresAuth: true },
          },
          {
            path: 'logistics',
            name: 'Logistics',
            component: () => import('@/pages/procurement/LogisticsPage.vue'),
            meta: { title: '调配计划', requiresAuth: true },
          },
        ],
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

/** 路由守卫 */
router.beforeEach((to, _from, next) => {
  const auth = useAuthStore()

  if (to.meta.requiresAuth !== false && !auth.isAuthenticated) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }

  if (to.name === 'Login' && auth.isAuthenticated) {
    next({ name: 'Dashboard' })
    return
  }

  next()
})

export default router
