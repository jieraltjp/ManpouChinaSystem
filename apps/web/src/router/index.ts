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
        redirect: '/procurement/procurement',
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/pages/dashboard/DashboardPage.vue'),
        meta: { titleKey: 'menu.dashboard', requiresAuth: true, roles: ['ADMIN'] },
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
            path: 'shipment-batch',
            name: 'ShipmentBatch',
            component: () => import('@/pages/procurement/ShipmentBatchPage.vue'),
            meta: { titleKey: 'menu.shipmentBatch', requiresAuth: true },
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
            path: 'consolidation-pool',
            name: 'ConsolidationPool',
            component: () => import('@/pages/procurement/ConsolidationPoolPage.vue'),
            meta: { titleKey: 'menu.consolidationPool', requiresAuth: true },
          },
          {
            path: 'container',
            name: 'Container',
            component: () => import('@/pages/procurement/ContainerPage.vue'),
            meta: { titleKey: 'menu.container', requiresAuth: true },
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
            path: 'ship',
            name: 'Ship',
            component: () => import('@/pages/logistics/ShipPage.vue'),
            meta: { titleKey: 'menu.ship', requiresAuth: true },
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
      {
        path: 'system',
        name: 'System',
        redirect: '/system/user',
        meta: { titleKey: 'menu.system', requiresAuth: true },
        children: [
          {
            path: 'user',
            name: 'SystemUser',
            component: () => import('@/pages/system/UserPage.vue'),
            meta: { titleKey: 'menu.user', requiresAuth: true, roles: ['ADMIN', 'MANAGER'] },
          },
          {
            path: 'role',
            name: 'SystemRole',
            component: () => import('@/pages/system/RolePage.vue'),
            meta: { titleKey: 'menu.role', requiresAuth: true, roles: ['ADMIN'] },
          },
          {
            path: 'cos-test',
            name: 'CosTest',
            component: () => import('@/pages/system/CosTestPage.vue'),
            meta: { titleKey: 'menu.cosTest', requiresAuth: true },
          },
          {
            path: 'audit-log',
            name: 'AuditLog',
            component: () => import('@/pages/system/AuditLogPage.vue'),
            meta: { titleKey: 'menu.auditLog', requiresAuth: true, roles: ['ADMIN', 'MANAGER'] },
          },
        ],
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/procurement/procurement',
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

  // 检查角色限制（meta.roles）
  const allowedRoles = (to.meta.roles as string[] | undefined)
  if (allowedRoles && allowedRoles.length > 0) {
    const userRoles = auth.claims?.roles ?? []
    const hasRole = allowedRoles.some(r => userRoles.includes(r))
    if (!hasRole) {
      next({ name: 'ProcurementOrder' })
      return
    }
  }

  if (to.name === 'Login' && auth.isAuthenticated) {
    next(auth.isAdmin ? { name: 'Dashboard' } : { name: 'ProcurementOrder' })
    return
  }

  if (to.name === 'Dashboard' && !auth.isAdmin) {
    next({ name: 'ProcurementOrder' })
    return
  }

  next()
})

export default router
