<template>
  <el-container class="app-layout">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapsed ? '64px' : '220px'" class="sidebar">
      <div class="logo">
        <span v-if="!isCollapsed">{{ $t('app.logo') }}</span>
        <span v-else>{{ $t('app.logoShort') }}</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapsed"
        :collapse-transition="false"
        router
        class="sidebar-menu"
      >
        <el-menu-item v-if="auth.isAdmin" index="/dashboard">
          <el-icon><DataBoard /></el-icon>
          <template #title>{{ $t('menu.dashboard') }}</template>
        </el-menu-item>

        <!-- 发注管理 -->
        <el-sub-menu v-if="hasPermission('page:demand:access') || hasPermission('page:procurement:access') || hasPermission('page:shipment:access') || hasPermission('page:qc:access') || hasPermission('page:logistics:access') || hasPermission('page:consolidation:access') || hasPermission('page:container:access') || hasPermission('page:customs:access') || hasPermission('page:japan_customs:access')" index="procurement" :popper-class="'sidebar-popper'">
          <template #title>
            <el-icon><ShoppingCart /></el-icon>
            <span v-if="!isCollapsed">{{ $t('menu.procurement-all') }}</span>
          </template>
          <el-menu-item v-if="hasPermission('page:demand:access')" index="/procurement/demand">
            <el-icon><FolderOpened /></el-icon>
            <template #title>{{ $t('menu.demand') }}</template>
          </el-menu-item>
          <el-menu-item v-if="hasPermission('page:procurement:access')" index="/procurement/procurement">
            <el-icon><ShoppingCart /></el-icon>
            <template #title>{{ $t('menu.procurement') }}</template>
          </el-menu-item>
          <el-menu-item v-if="hasPermission('page:shipment:access')" index="/procurement/shipment-batch">
            <el-icon><Goods /></el-icon>
            <template #title>{{ $t('menu.shipmentBatch') }}</template>
          </el-menu-item>
          <el-menu-item v-if="hasPermission('page:qc:access')" index="/procurement/qc-record">
            <el-icon><CircleCheck /></el-icon>
            <template #title>{{ $t('menu.qcRecord') }}</template>
          </el-menu-item>
          <el-menu-item v-if="hasPermission('page:logistics:access')" index="/procurement/logistics-plan">
            <el-icon><Van /></el-icon>
            <template #title>{{ $t('menu.logisticsPlan') }}</template>
          </el-menu-item>
          <el-menu-item v-if="hasPermission('page:consolidation:access')" index="/procurement/consolidation-pool">
            <el-icon><Connection /></el-icon>
            <template #title>{{ $t('menu.consolidationPool') }}</template>
          </el-menu-item>
          <el-menu-item v-if="hasPermission('page:customs:access')" index="/procurement/domestic-customs">
            <el-icon><DocumentCopy /></el-icon>
            <template #title>{{ $t('menu.domesticCustoms') }}</template>
          </el-menu-item>
          <el-menu-item v-if="hasPermission('page:japan_customs:access')" index="/procurement/japan-customs">
            <el-icon><Box /></el-icon>
            <template #title>{{ $t('menu.japanCustomsRecord') }}</template>
          </el-menu-item>
        </el-sub-menu>

        <!-- 财务管理 -->
        <el-sub-menu v-if="hasPermission('page:tax_refund:access')" index="finance" :popper-class="'sidebar-popper'">
          <template #title>
            <el-icon><Money /></el-icon>
            <span v-if="!isCollapsed">{{ $t('menu.finance') }}</span>
          </template>
          <el-menu-item v-if="hasPermission('page:tax_refund:access')" index="/finance/tax-refund-record">
            <el-icon><Tickets /></el-icon>
            <template #title>{{ $t('menu.taxRefundRecord') }}</template>
          </el-menu-item>
        </el-sub-menu>

        <!-- 运营销售 -->
        <el-sub-menu v-if="hasPermission('page:sales:access')" index="sales" :popper-class="'sidebar-popper'">
          <template #title>
            <el-icon><TrendCharts /></el-icon>
            <span v-if="!isCollapsed">{{ $t('menu.sales') }}</span>
          </template>
          <el-menu-item v-if="hasPermission('page:sales:access')" index="/sales/sales-record">
            <el-icon><Goods /></el-icon>
            <template #title>{{ $t('menu.salesRecord') }}</template>
          </el-menu-item>
        </el-sub-menu>

        <!-- 基础数据 -->
        <el-sub-menu v-if="hasPermission('page:factory:access') || hasPermission('page:product:access') || hasPermission('page:order:access') || hasPermission('page:container:access') || hasPermission('cargo_size:read') || hasPermission('legacy_procurement:read') || hasPermission('dispatch:read') || hasPermission('offline_order:read')" index="base" :popper-class="'sidebar-popper'">
          <template #title>
            <el-icon><Menu /></el-icon>
            <span v-if="!isCollapsed">{{ $t('menu.base') }}</span>
          </template>
          <el-menu-item v-if="hasPermission('page:factory:access')" index="/base/factory">
            <el-icon><OfficeBuilding /></el-icon>
            <template #title>{{ $t('menu.factory') }}</template>
          </el-menu-item>
          <el-menu-item v-if="hasPermission('page:product:access')" index="/base/product">
            <el-icon><Goods /></el-icon>
            <template #title>{{ $t('menu.product') }}</template>
          </el-menu-item>
          <el-menu-item v-if="hasPermission('cargo_size:read')" index="/base/cargo-size">
            <el-icon><Box /></el-icon>
            <template #title>{{ $t('menu.cargoSize') }}</template>
          </el-menu-item>
          <el-menu-item v-if="hasPermission('page:container:access')" index="/base/container">
            <el-icon><Ship /></el-icon>
            <template #title>{{ $t('menu.container') }}</template>
          </el-menu-item>
          <el-menu-item v-if="hasPermission('page:order:access')" index="/base/overview">
            <el-icon><Document /></el-icon>
            <template #title>{{ $t('menu.orderOverview') }}</template>
          </el-menu-item>
          <el-menu-item v-if="hasPermission('dispatch:read')" index="/base/dispatch">
            <el-icon><List /></el-icon>
            <template #title>{{ $t('menu.dispatch') }}</template>
          </el-menu-item>
          <el-menu-item v-if="hasPermission('legacy_procurement:read')" index="/base/legacy-procurement">
            <el-icon><Clock /></el-icon>
            <template #title>{{ $t('menu.legacyProcurement') }}</template>
          </el-menu-item>
          <el-menu-item v-if="hasPermission('offline_order:read')" index="/base/offline-order">
            <el-icon><List /></el-icon>
            <template #title>{{ $t('menu.offlineOrder') }}</template>
          </el-menu-item>
        </el-sub-menu>

        <!-- 系统管理 -->
        <el-sub-menu v-if="hasPermission('page:user:access') || hasPermission('page:role:access') || hasPermission('page:audit:access')" index="system" :popper-class="'sidebar-popper'">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span v-if="!isCollapsed">{{ $t('menu.system') }}</span>
          </template>
          <el-menu-item v-if="hasPermission('page:user:access')" index="/system/user">
            <el-icon><User /></el-icon>
            <template #title>{{ $t('menu.user') }}</template>
          </el-menu-item>
          <el-menu-item v-if="hasPermission('page:role:access')" index="/system/role">
            <el-icon><Key /></el-icon>
            <template #title>{{ $t('menu.role') }}</template>
          </el-menu-item>
          <el-menu-item v-if="hasPermission('page:audit:access')" index="/system/audit-log">
            <el-icon><Document /></el-icon>
            <template #title>{{ $t('menu.auditLog') }}</template>
          </el-menu-item>
          <el-menu-item index="/system/profile">
            <el-icon><User /></el-icon>
            <template #title>{{ $t('menu.profileLabel') }}</template>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶栏 -->
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="isCollapsed = !isCollapsed">
            <Fold v-if="!isCollapsed" />
            <Expand v-else />
          </el-icon>
          <span class="header-title">{{ currentPageTitle }}</span>
        </div>

        <div class="header-right">
          <!-- 用户设置面板 -->
          <el-dropdown trigger="click" placement="bottom-end">
            <span class="user-info">
              <el-avatar
                :size="32"
                :src="avatarDataUrl"
                icon="UserFilled"
              />
              <span class="username">
                {{ currentLocale === 'ja'
                    ? (userInfo?.nameJp || userInfo?.nameCn || auth.claims?.username)
                    : (userInfo?.nameCn || userInfo?.nameJp || auth.claims?.username)
                    || $t('common.user') }}
              </span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <div class="user-panel">
                <!-- 用户信息区 -->
                <div class="panel-user-info">
                  <el-avatar
                    :size="40"
                    :src="avatarDataUrl"
                    icon="UserFilled"
                  />
                  <div class="panel-user-text">
                    <div class="panel-username">
                      {{ currentLocale === 'ja'
                          ? (userInfo?.nameJp || userInfo?.nameCn || auth.claims?.username)
                          : (userInfo?.nameCn || userInfo?.nameJp || auth.claims?.username)
                          || $t('common.user') }}
                    </div>
                    <div class="panel-role">
                      {{ currentLocale === 'ja'
                          ? (userInfo?.roles?.[0]?.roleNameJp || userInfo?.roles?.[0]?.roleNameCn)
                          : (userInfo?.roles?.[0]?.roleNameCn || userInfo?.roles?.[0]?.roleNameJp)
                          || '—' }}
                    </div>
                  </div>
                </div>

                <el-divider style="margin: 8px 0" />

                <!-- 语言设置 -->
                <div class="panel-setting-row">
                  <div class="panel-setting-label">{{ $t('common.locale.zh') }} / {{ $t('common.locale.ja') }}</div>
                  <el-radio-group v-model="currentLocale" size="small" @change="onLocaleChange">
                    <el-radio-button value="zh">{{ $t('common.locale.zh') }}</el-radio-button>
                    <el-radio-button value="ja">{{ $t('common.locale.ja') }}</el-radio-button>
                  </el-radio-group>
                </div>

                <!-- 时区设置 -->
                <div class="panel-setting-row">
                  <div class="panel-setting-label">{{ $t('common.timezone.label') }}</div>
                  <el-select v-model="currentTimezone" size="small" style="width: 140px;" @change="onTimezoneChange">
                    <el-option value="CST" :label="$t('common.timezone.CST')" />
                    <el-option value="JST" :label="$t('common.timezone.JST')" />
                  </el-select>
                </div>

                <el-divider style="margin: 8px 0" />

                <!-- 个人中心 -->
                <div class="panel-logout" @click="router.push('/system/profile')">
                  <el-icon><User /></el-icon>
                  {{ $t('menu.profileLabel') }}
                </div>

                <el-divider style="margin: 8px 0" />

                <!-- 退出登录 -->
                <div class="panel-logout" @click="onLogout">
                  <el-icon><SwitchButton /></el-icon>
                  {{ $t('app.logout') }}
                </div>
              </div>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="main-content">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Fold, Expand, ArrowDown, SwitchButton, DataBoard, ShoppingCart, FolderOpened, CircleCheck, Van, DocumentCopy, Box, Goods, OfficeBuilding, Menu, Document, Money, Tickets, TrendCharts, Setting, User, Key, Ship, List, Connection } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useI18n } from 'vue-i18n'
import { usePermission } from '@/composables/usePermission'
import { getCurrentUser } from '@/api/user'
import type { Locale } from '@/locales'
import type { UserVO } from '@/api/user'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const { locale, t } = useI18n()
const { hasPermission } = usePermission()

const isCollapsed = ref(false)
const activeMenu = computed(() => route.path)
const currentLocale = ref<Locale>((localStorage.getItem('locale') as Locale) || 'zh')
const currentTimezone = ref(localStorage.getItem('timezone') || 'CST')
const userInfo = ref<UserVO | null>(null)

/** 头像 data URL（computed 避免内联拼接触发全局重渲染） */
const avatarDataUrl = computed(() =>
  userInfo.value?.avatarUrl ? `data:image/jpeg;base64,${userInfo.value.avatarUrl}` : undefined
)

onMounted(async () => {
  if (auth.token) {
    try {
      userInfo.value = await getCurrentUser()
    } catch {
      // ignore: header falls back to JWT claims
    }
  }
})

/** 当前页面标题（根据路由匹配 menu i18n key） */
const routeTitleMap: Record<string, string> = {
  '/dashboard': 'dashboard.title',
  '/procurement/demand': 'demand.title',
  '/procurement/procurement': 'order.title',
  '/procurement/qc-record': 'inspection.title',
  '/procurement/logistics-plan': 'logistics.title',
  '/procurement/domestic-customs': 'customs.title',
  '/procurement/japan-customs': 'japanCustoms.title',
  '/finance/tax-refund-record': 'taxRefund.title',
  '/sales/sales-record': 'sales.title',
  '/base/factory': 'factory.title',
  '/base/product': 'product.title',
  '/base/overview': 'orderOverview.title',
  '/base/legacy-procurement': 'legacyProcurement.title',
  '/base/dispatch': 'dispatch.title',
  '/base/offline-order': 'offlineOrder.title',
  '/procurement/shipment-batch': 'menu.shipmentBatch',
  '/system/user': 'menu.user',
  '/system/role': 'menu.role',
  '/system/cos-test': 'menu.cosTest',
}
const currentPageTitle = computed(() => {
  const key = routeTitleMap[route.path]
  return key ? t(key) : ''
})

function onLocaleChange(newLocale: Locale) {
  locale.value = newLocale
  localStorage.setItem('locale', newLocale)
}

function onTimezoneChange(tz: string) {
  localStorage.setItem('timezone', tz)
}

function onLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.app-layout {
  height: 100vh;
}

/* ── 侧边栏：限制视口高度，超出自动滚动 ── */
.sidebar {
  background: linear-gradient(180deg, #1E2533 0%, #252D3D 100%);
  transition: width 0.3s;
  overflow-y: auto;        /* 菜单项超出视口时滚动 */
  overflow-x: hidden;      /* 水平溢出截断 */
  box-shadow: 2px 0 16px rgba(0,0,0,0.18);
  border-right: none;
}

/* ── Logo 区：橙色渐变 ── */
.logo {
  height: 64px;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 1px;
  white-space: nowrap;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(232,101,10,0.35);
}

/* ── 菜单容器：负责水平截断 ── */
.sidebar-menu {
  border-right: none;
  background: transparent;
}

/* 子菜单标题行 */
.sidebar-menu :deep(.el-sub-menu__title) {
  color: #C0C4CC;
  border-radius: 0 8px 8px 0;
  margin: 2px 8px;
  width: calc(100% - 16px);
  transition: all var(--transition-fast);
  min-width: 0;
  overflow: hidden;     /* 截断文字，绝对定位的 arrow 不受影响 */
}

/* 强制显示被 Element Plus 隐藏的 arrow */
.sidebar-menu :deep(.el-sub-menu__icon-arrow) {
  display: flex !important;  /* 覆盖 Element Plus 的 display: none !important */
  visibility: visible !important;
  opacity: 1 !important;
  position: absolute;        /* 确保相对定位正确 */
  right: 8px;               /* 与标题右侧保持间距 */
  z-index: 10;              /* 提升层级，避免被文字覆盖 */
}

.sidebar-menu :deep(.el-sub-menu__title:hover) {
  background: rgba(255,255,255,0.06);
  color: #fff;
}

/* 标题文字截断：保证 arrow 不被文字覆盖 */
.sidebar-menu :deep(.el-sub-menu__title > span) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 激活父级菜单（发注管理）时橙色 */
.sidebar-menu :deep(.el-sub-menu.is-active .el-sub-menu__title) {
  background: rgba(232,101,10,0.12) !important;
  border-left: 3px solid var(--color-primary);
  color: var(--color-primary-light) !important;
}

/* 菜单项 */
.sidebar-menu :deep(.el-menu-item) {
  color: #C0C4CC;
  border-radius: 0 8px 8px 0;
  margin: 2px 8px;
  width: calc(100% - 16px);
  transition: all var(--transition-fast);
  padding-left: 48px !important;
}
.sidebar-menu :deep(.el-menu-item:hover) {
  background: rgba(255,255,255,0.06);
  color: #fff;
}

/* 激活菜单项：橙色左边框 + 淡橙背景 */
.sidebar-menu :deep(.el-menu-item.is-active) {
  background: rgba(232,101,10,0.15) !important;
  border-left: 3px solid var(--color-primary);
  color: var(--color-primary-light) !important;
}
.sidebar-menu :deep(.el-menu-item.is-active .el-icon) {
  color: var(--color-primary-light) !important;
}

/* inline 子菜单（二级展开项）：覆盖白底 + 灰字 */
.sidebar-menu :deep(.el-menu--inline) {
  background: transparent !important;
}
.sidebar-menu :deep(.el-menu--inline .el-menu-item) {
  color: #C0C4CC;
  background: transparent;
  border-radius: 0 8px 8px 0;
  margin: 2px 8px;
  width: calc(100% - 16px);
  padding-left: 48px !important;
}
.sidebar-menu :deep(.el-menu--inline .el-menu-item:hover) {
  background: rgba(255,255,255,0.06);
  color: #fff;
}
.sidebar-menu :deep(.el-menu--inline .el-menu-item.is-active) {
  background: rgba(232,101,10,0.15) !important;
  border-left: 3px solid var(--color-primary);
  color: var(--color-primary-light) !important;
}

/* ── 顶栏 ── */
.header {
  background: var(--bg-header);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  border-bottom: 1px solid var(--border-color);
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: var(--text-secondary);
  padding: 6px;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}
.collapse-btn:hover {
  color: var(--color-primary);
  background: var(--color-primary-pale);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  transition: background var(--transition-fast);
}
.user-info:hover {
  background: var(--color-primary-pale);
}

.username {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}

.main-content {
  background: var(--bg-page);
  padding: 20px;
}

/* ── 用户设置面板 ── */
.user-panel {
  width: 240px;
  padding: 12px;
  background: var(--bg-header);
}

.panel-user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 0 8px;
}

.panel-user-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.panel-username {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.panel-role {
  font-size: 12px;
  color: var(--text-secondary);
}

.panel-setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 0;
  gap: 8px;
}

.panel-setting-label {
  font-size: 12px;
  color: var(--text-secondary);
  flex-shrink: 0;
}

.panel-logout {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 4px;
  cursor: pointer;
  color: var(--color-danger, #dc2626);
  font-size: 13px;
  border-radius: var(--radius-sm);
  transition: background var(--transition-fast);
}
.panel-logout:hover {
  background: rgba(220, 38, 38, 0.08);
}
</style>
