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
    meta: { requiresAuth: false, titleKey: 'auth.login' },
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
        meta: { titleKey: 'menu.dashboard', requiresAuth: true },
      },
      {
        path: 'procurement',
        name: 'Procurement',
        redirect: '/procurement/order',
        meta: { titleKey: 'menu.procurement', requiresAuth: true },
        children: [
          {
            path: 'demand',
            name: 'Demand',
            component: () => import('@/pages/procurement/DemandPage.vue'),
            meta: { titleKey: 'menu.demand', requiresAuth: true },
          },
          {
            path: 'order',
            name: 'Order',
            component: () => import('@/pages/procurement/OrderPage.vue'),
            meta: { titleKey: 'menu.order', requiresAuth: true },
          },
          {
            path: 'inspection',
            name: 'Inspection',
            component: () => import('@/pages/procurement/InspectionPage.vue'),
            meta: { titleKey: 'menu.inspection', requiresAuth: true },
          },
          {
            path: 'logistics',
            name: 'Logistics',
            component: () => import('@/pages/procurement/LogisticsPage.vue'),
            meta: { titleKey: 'menu.logistics', requiresAuth: true },
          },
          {
            path: 'customs',
            name: 'Customs',
            component: () => import('@/pages/customs/CustomsPage.vue'),
            meta: { titleKey: 'menu.customs', requiresAuth: true },
          },
          {
            path: 'japan-customs',
            name: 'JapanCustoms',
            component: () => import('@/pages/customs/JapanCustomsPage.vue'),
            meta: { titleKey: 'menu.japanCustoms', requiresAuth: true },
          },
        ],
      },
      {
        path: 'finance',
        name: 'Finance',
        redirect: '/finance/tax-refund',
        meta: { titleKey: 'menu.finance', requiresAuth: true },
        children: [
          {
            path: 'tax-refund',
            name: 'TaxRefund',
            component: () => import('@/pages/finance/TaxRefundPage.vue'),
            meta: { titleKey: 'menu.taxRefund', requiresAuth: true },
          },
        ],
      },
      {
        path: 'sales',
        name: 'Sales',
        redirect: '/sales/operations',
        meta: { titleKey: 'menu.sales', requiresAuth: true },
        children: [
          {
            path: 'operations',
            name: 'SalesOperations',
            component: () => import('@/pages/sales/SalesOperationsPage.vue'),
            meta: { titleKey: 'menu.salesOperations', requiresAuth: true },
          },
        ],
      },
      {
        path: 'base',
        name: 'Base',
        redirect: '/base/factory',
        meta: { titleKey: 'menu.base', requiresAuth: true },
        children: [
          {
            path: 'factory',
            name: 'Factory',
            component: () => import('@/pages/factory/FactoryPage.vue'),
            meta: { titleKey: 'menu.factory', requiresAuth: true },
          },
          {
            path: 'product',
            name: 'Product',
            component: () => import('@/pages/product/ProductPage.vue'),
            meta: { titleKey: 'menu.product', requiresAuth: true },
          },
          {
            path: 'overview',
            name: 'OrderOverview',
            component: () => import('@/pages/procurement/OrderOverviewPage.vue'),
            meta: { titleKey: 'menu.orderOverview', requiresAuth: true },
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
