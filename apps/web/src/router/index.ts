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
        redirect: '/procurement/procurement',
        meta: { titleKey: 'menu.procurement', requiresAuth: true },
        children: [
          {
            path: 'demand',
            name: 'Demand',
            component: () => import('@/pages/procurement/DemandPage.vue'),
            meta: { titleKey: 'menu.demand', requiresAuth: true },
          },
          {
            path: 'procurement',
            name: 'ProcurementOrder',
            component: () => import('@/pages/procurement/ProcurementPage.vue'),
            meta: { titleKey: 'menu.procurement', requiresAuth: true },
          },
          {
            path: 'qc-record',
            name: 'QcRecord',
            component: () => import('@/pages/procurement/QcRecordPage.vue'),
            meta: { titleKey: 'menu.qcRecord', requiresAuth: true },
          },
          {
            path: 'logistics-plan',
            name: 'LogisticsPlan',
            component: () => import('@/pages/procurement/LogisticsPlanPage.vue'),
            meta: { titleKey: 'menu.logisticsPlan', requiresAuth: true },
          },
          {
            path: 'domestic-customs',
            name: 'DomesticCustoms',
            component: () => import('@/pages/customs/DomesticCustomsPage.vue'),
            meta: { titleKey: 'menu.domesticCustoms', requiresAuth: true },
          },
          {
            path: 'japan-customs',
            name: 'JapanCustomsRecord',
            component: () => import('@/pages/customs/JapanCustomsRecordPage.vue'),
            meta: { titleKey: 'menu.japanCustomsRecord', requiresAuth: true },
          },
        ],
      },
      {
        path: 'finance',
        name: 'Finance',
        redirect: '/finance/tax-refund-record',
        meta: { titleKey: 'menu.finance', requiresAuth: true },
        children: [
          {
            path: 'tax-refund-record',
            name: 'TaxRefundRecord',
            component: () => import('@/pages/finance/TaxRefundRecordPage.vue'),
            meta: { titleKey: 'menu.taxRefundRecord', requiresAuth: true },
          },
        ],
      },
      {
        path: 'sales',
        name: 'Sales',
        redirect: '/sales/sales-record',
        meta: { titleKey: 'menu.sales', requiresAuth: true },
        children: [
          {
            path: 'sales-record',
            name: 'SalesRecord',
            component: () => import('@/pages/sales/SalesRecordPage.vue'),
            meta: { titleKey: 'menu.salesRecord', requiresAuth: true },
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
          {
            path: 'overview/demand/:demandId',
            name: 'DemandOverview',
            component: () => import('@/pages/procurement/DemandOverviewPage.vue'),
            meta: { titleKey: 'menu.orderOverview', requiresAuth: true },
          },
          {
            path: 'overview/procurement/:procurementId',
            name: 'ProcurementOverview',
            component: () => import('@/pages/procurement/ProcurementOverviewPage.vue'),
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
