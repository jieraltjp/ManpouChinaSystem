<template>
  <el-container class="app-layout">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapsed ? '64px' : '220px'" class="sidebar">
      <div class="logo">
        <span v-if="!isCollapsed">漫普中国</span>
        <span v-else>MC</span>
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
          <template #title>仪表盘</template>
        </el-menu-item>

        <el-menu-item index="/examples">
          <el-icon><Document /></el-icon>
          <template #title>示例列表</template>
        </el-menu-item>

        <el-menu-item index="/test">
          <el-icon><ShoppingCart /></el-icon>
          <template #title>采购单管理</template>
        </el-menu-item>
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
          <el-dropdown @command="onCommand">
            <span class="user-info">
              <el-avatar :size="32" icon="UserFilled" />
              <span class="username">{{ auth.claims?.username || 'User' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
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
import { Fold, Expand, ArrowDown, SwitchButton, DataBoard, Document, ShoppingCart } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const isCollapsed = ref(false)
const activeMenu = computed(() => route.path)

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

/* 菜单项 */
.sidebar-menu :deep(.el-menu-item) {
  color: #C0C4CC;
  border-radius: 0 8px 8px 0;
  margin: 2px 8px;
  width: calc(100% - 16px);
  transition: all var(--transition-fast);
}
.sidebar-menu :deep(.el-menu-item:hover) {
  background: rgba(255,255,255,0.06);
  color: #fff;
}

/* 激活项：橙色左边框 + 淡橙背景 */
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
