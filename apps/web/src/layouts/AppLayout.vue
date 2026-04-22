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
        <el-menu-item index="/dashboard">
          <el-icon><DataBoard /></el-icon>
          <template #title>{{ $t('menu.dashboard') }}</template>
        </el-menu-item>

        <!-- 发注管理 -->
        <el-sub-menu index="procurement" :popper-class="'sidebar-popper'">
          <template #title>
            <el-icon><ShoppingCart /></el-icon>
            <span v-if="!isCollapsed">{{ $t('menu.procurement') }}</span>
          </template>
          <el-menu-item index="/procurement/demand">
            <el-icon><FolderOpened /></el-icon>
            <template #title>{{ $t('menu.demand') }}</template>
          </el-menu-item>
          <el-menu-item index="/procurement/order">
            <el-icon><ShoppingCart /></el-icon>
            <template #title>{{ $t('menu.order') }}</template>
          </el-menu-item>
          <el-menu-item index="/procurement/inspection">
            <el-icon><CircleCheck /></el-icon>
            <template #title>{{ $t('menu.inspection') }}</template>
          </el-menu-item>
          <el-menu-item index="/procurement/logistics">
            <el-icon><Van /></el-icon>
            <template #title>{{ $t('menu.logistics') }}</template>
          </el-menu-item>
          <el-menu-item index="/procurement/product">
            <el-icon><Goods /></el-icon>
            <template #title>{{ $t('menu.product') }}</template>
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
        </div>

        <div class="header-right">
          <!-- 语言切换 -->
          <el-select v-model="currentLocale" size="small" style="margin-right: 12px; width: 80px;" @change="onLocaleChange">
            <el-option value="zh" :label="$t('common.locale.zh')" />
            <el-option value="ja" :label="$t('common.locale.ja')" />
          </el-select>

          <el-dropdown @command="onCommand">
            <span class="user-info">
              <el-avatar :size="32" icon="UserFilled" />
              <span class="username">{{ auth.claims?.username || $t('common.user') }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  {{ $t('app.logout') }}
                </el-dropdown-item>
              </el-dropdown-menu>
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
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Fold, Expand, ArrowDown, SwitchButton, DataBoard, ShoppingCart, FolderOpened, CircleCheck, Van, Goods } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useI18n } from 'vue-i18n'
import type { Locale } from '@/locales'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const { locale } = useI18n()

const isCollapsed = ref(false)
const activeMenu = computed(() => route.path)
const currentLocale = computed(() => locale.value as Locale)

function onLocaleChange(newLocale: Locale) {
  locale.value = newLocale
  localStorage.setItem('locale', newLocale)
}

function onCommand(cmd: string) {
  if (cmd === 'logout') {
    auth.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.app-layout {
  height: 100vh;
}

/* ── 侧边栏 ── */
.sidebar {
  background: linear-gradient(180deg, #1E2533 0%, #252D3D 100%);
  transition: width 0.3s;
  overflow: hidden;
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

/* ── 菜单通用 ── */
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
}
.sidebar-menu :deep(.el-sub-menu__title:hover) {
  background: rgba(255,255,255,0.06);
  color: #fff;
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
</style>
