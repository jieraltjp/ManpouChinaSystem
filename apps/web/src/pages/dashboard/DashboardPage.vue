<template>
  <div class="dashboard">
    <h2 class="page-title">仪表盘</h2>

    <!-- 欢迎卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#409eff"><User /></el-icon>
            <div>
              <div class="stat-value">{{ auth.claims?.username || '—' }}</div>
              <div class="stat-label">当前用户</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#67c23a"><Key /></el-icon>
            <div>
              <div class="stat-value">{{ auth.claims?.roles?.join(', ') || '—' }}</div>
              <div class="stat-label">角色</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#e6a23c"><OfficeBuilding /></el-icon>
            <div>
              <div class="stat-value">{{ auth.claims?.tenantId || '—' }}</div>
              <div class="stat-label">租户</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#f56c6c"><Timer /></el-icon>
            <div>
              <div class="stat-value">{{ tokenExpiry }}</div>
              <div class="stat-label">Token 剩余</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- JWT 信息 -->
    <el-card shadow="hover" class="info-card">
      <template #header>
        <span>JWT Claims</span>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="User ID (sub)">
          {{ auth.claims?.sub || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="Tenant ID">
          {{ auth.claims?.tenantId || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="Roles">
          <el-tag
            v-for="role in auth.claims?.roles"
            :key="role"
            size="small"
            class="mr-4"
          >
            {{ role }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="Permissions">
          <el-tag
            v-for="perm in auth.claims?.permissions"
            :key="perm"
            type="info"
            size="small"
            class="mr-4"
          >
            {{ perm }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="Issued At">
          {{ issuedAt }}
        </el-descriptions-item>
        <el-descriptions-item label="Expires At">
          {{ expiresAt }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 快捷入口 -->
    <el-card shadow="hover" class="info-card mt-16">
      <template #header>
        <span>快捷入口</span>
      </template>
      <el-space wrap>
        <el-button type="primary" @click="$router.push('/procurement/order')">
          <el-icon><Document /></el-icon>
          发注单管理
        </el-button>
        <el-button type="primary" @click="$router.push('/procurement/factory')">
          <el-icon><OfficeBuilding /></el-icon>
          工厂管理
        </el-button>
        <el-button @click="handleRelogin">
          <el-icon><SwitchButton /></el-icon>
          重新登录
        </el-button>
      </el-space>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { User, Key, OfficeBuilding, Timer, SwitchButton } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import dayjs from 'dayjs'

const auth = useAuthStore()

function handleRelogin() {
  auth.logout()
  window.location.href = '/login'
}

const issuedAt = computed(() =>
  auth.claims?.iat ? dayjs.unix(auth.claims.iat).format('YYYY-MM-DD HH:mm:ss') : '—',
)
const expiresAt = computed(() =>
  auth.claims?.exp ? dayjs.unix(auth.claims.exp).format('YYYY-MM-DD HH:mm:ss') : '—',
)
const tokenExpiry = computed(() => {
  if (!auth.claims?.exp) return '—'
  const remaining = dayjs.unix(auth.claims.exp).diff(dayjs(), 'minute')
  return remaining > 0 ? `${remaining} min` : '已过期'
})
</script>

<style scoped>
.dashboard {
  max-width: 1200px;
}

.page-title {
  margin: 0 0 20px;
  font-size: 22px;
  color: #303133;
}

.stat-row {
  margin-bottom: 16px;
}

.stat-card {
  border-radius: 8px;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  font-size: 36px;
}

.stat-value {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.info-card {
  border-radius: 8px;
  margin-bottom: 16px;
}

.mr-4 {
  margin-right: 8px;
}

.mt-16 {
  margin-top: 16px;
}
</style>
