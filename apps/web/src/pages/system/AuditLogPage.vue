<template>
  <div v-if="hasPermission('audit:read')" class="page">
    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filterForm">
        <el-form-item :label="$t('auditLog.filter.module')">
          <el-select v-model="filterForm.module" clearable style="width:140px">
            <el-option v-for="m in MODULE_OPTIONS" :key="m" :label="$t(`auditLog.module.${m}`)" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('auditLog.filter.action')">
          <el-select v-model="filterForm.action" clearable style="width:120px">
            <el-option v-for="a in ACTION_OPTIONS" :key="a" :label="$t(`auditLog.actionTag.${a}`)" :value="a" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('auditLog.filter.resourceType')">
          <el-input v-model="filterForm.resourceType" :placeholder="$t('auditLog.filter.resourceType')" clearable style="width:140px" />
        </el-form-item>
        <el-form-item :label="$t('auditLog.filter.userId')">
          <el-input v-model="filterForm.userId" :placeholder="$t('auditLog.filter.userId')" clearable style="width:140px" />
        </el-form-item>
        <el-form-item :label="$t('auditLog.filter.startTime')">
          <el-date-picker v-model="filterForm.startTime" type="datetime" :placeholder="$t('auditLog.filter.startTime')"
            value-format="YYYY-MM-DDTHH:mm:ss" style="width:170px" />
        </el-form-item>
        <el-form-item :label="$t('auditLog.filter.endTime')">
          <el-date-picker v-model="filterForm.endTime" type="datetime" :placeholder="$t('auditLog.filter.endTime')"
            value-format="YYYY-MM-DDTHH:mm:ss" style="width:170px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">{{ $t('auditLog.filter.search') }}</el-button>
          <el-button @click="onReset">{{ $t('auditLog.filter.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="table-header">
          <el-radio-group v-model="excelViewMode" size="small">
            <el-radio-button value="table">{{ $t('common.viewMode.table') }}</el-radio-button>
            <el-radio-button value="copy">{{ $t('common.viewMode.excel') }}</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <el-table v-if="excelViewMode === 'table'" v-loading="loading" :data="tableData" stripe style="width:100%" min-height="300">
        <el-table-column :label="$t('auditLog.column.createTime')" min-width="160">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="username" :label="$t('auditLog.column.username')" min-width="110" />
        <el-table-column :label="$t('auditLog.column.module')" min-width="100">
          <template #default="{ row }">
            {{ $t(`auditLog.module.${row.module}`, row.module) }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('auditLog.column.action')" min-width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="actionTagType(row.action)" size="small">{{ $t(`auditLog.actionTag.${row.action}`, row.action) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('auditLog.column.httpMethod')" min-width="70" align="center">
          <template #default="{ row }">
            <span class="http-method" :class="`http-${row.httpMethod?.toLowerCase()}`">{{ row.httpMethod }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="httpUrl" :label="$t('auditLog.column.httpUrl')" min-width="200" show-overflow-tooltip />
        <el-table-column prop="resourceType" :label="$t('auditLog.column.resourceType')" min-width="100" />
        <el-table-column prop="ipAddress" :label="$t('auditLog.column.ipAddress')" min-width="130" />
        <el-table-column :label="$t('auditLog.column.detail')" width="90" align="center">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">{{ $t('auditLog.action.viewDetail') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <ExcelTable v-else :columns="copyColumns" :data="tableData" />

      <!-- 分页 -->
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" :title="$t('auditLog.detail.title')" size="560px">
      <el-descriptions :column="1" border>
        <el-descriptions-item :label="$t('auditLog.column.createTime')">{{ formatTime(currentLog?.createTime) }}</el-descriptions-item>
        <el-descriptions-item :label="$t('auditLog.column.username')">{{ currentLog?.username }}</el-descriptions-item>
        <el-descriptions-item :label="$t('auditLog.column.operatorName')">{{ currentLog?.operatorName || '—' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('auditLog.column.module')">{{ $t(`auditLog.module.${currentLog?.module ?? ''}`) }}</el-descriptions-item>
        <el-descriptions-item :label="$t('auditLog.column.action')">
          <el-tag :type="actionTagType(currentLog?.action)" size="small">{{ $t(`auditLog.actionTag.${currentLog?.action ?? ''}`) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('auditLog.column.httpMethod')">{{ currentLog?.httpMethod }}</el-descriptions-item>
        <el-descriptions-item :label="$t('auditLog.column.httpUrl')">{{ currentLog?.httpUrl }}</el-descriptions-item>
        <el-descriptions-item :label="$t('auditLog.column.resourceType')">{{ currentLog?.resourceType || '—' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('auditLog.column.resourceId')">{{ currentLog?.resourceId || '—' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('auditLog.column.ipAddress')">{{ currentLog?.ipAddress }}</el-descriptions-item>
        <el-descriptions-item :label="$t('auditLog.column.traceId')">{{ currentLog?.traceId || '—' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('auditLog.column.detail')">
          <pre v-if="formattedDetail" class="detail-json">{{ formattedDetail }}</pre>
          <span v-else>—</span>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">{{ $t('auditLog.detail.close') }}</el-button>
      </template>
    </el-drawer>
  </div>
  <el-empty v-else :description="$t('auditLog.noPermission')" />
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { usePermission } from '@/composables/usePermission'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { pageAuditLogs } from '@/api/auditLog'
import type { AuditLogVO } from '@/api/auditLog'
import ExcelTable, { type ExcelColDef } from '@/components/ExcelTable.vue'

const { locale: localeRef, t } = useI18n()
const { hasPermission } = usePermission()

function formatTime(ts: string | undefined | null): string {
  if (!ts) return '-'
  return new Date(ts).toLocaleString(localeRef.value === 'ja' ? 'ja-JP' : 'zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit',
  })
}

const MODULE_OPTIONS = ['auth', 'procurement', 'factory', 'product', 'user', 'role']
const ACTION_OPTIONS = ['LOGIN', 'CREATE', 'UPDATE', 'DELETE', 'APPROVE', 'REJECT']

// ===== 状态 =====
const loading = ref(false)
const tableData = ref<AuditLogVO[]>([])
const excelViewMode = ref<'table' | 'copy'>('table')

const copyColumns: ExcelColDef[] = [
  { prop: 'createTime', label: t('auditLog.column.createTime'), formatter: (row: AuditLogVO) => formatTime(row.createTime) },
  { prop: 'username', label: t('auditLog.column.username') },
  { prop: 'module', label: t('auditLog.column.module'), formatter: (row: AuditLogVO) => t(`auditLog.module.${row.module}`, row.module) },
  { prop: 'action', label: t('auditLog.column.action'), formatter: (row: AuditLogVO) => t(`auditLog.actionTag.${row.action}`, row.action) },
  { prop: 'httpMethod', label: t('auditLog.column.httpMethod') },
  { prop: 'httpUrl', label: t('auditLog.column.httpUrl') },
  { prop: 'resourceType', label: t('auditLog.column.resourceType') },
  { prop: 'ipAddress', label: t('auditLog.column.ipAddress') },
  { prop: 'detail', label: t('auditLog.column.detail'), excluded: true },
]

const pagination = reactive({ page: 1, size: 20, total: 0 })
const filterForm = reactive({
  module: '',
  action: '',
  resourceType: '',
  userId: '',
  startTime: '',
  endTime: '',
})

const detailVisible = ref(false)
const currentLog = ref<AuditLogVO | null>(null)

// ===== 计算属性 =====
const formattedDetail = computed(() => {
  if (!currentLog.value?.detail) return null
  try {
    return JSON.stringify(JSON.parse(currentLog.value.detail), null, 2)
  } catch {
    return currentLog.value.detail
  }
})

// ===== 方法 =====
async function loadData() {
  loading.value = true
  try {
    const res = await pageAuditLogs({
      module: filterForm.module || undefined,
      action: filterForm.action || undefined,
      resourceType: filterForm.resourceType || undefined,
      userId: filterForm.userId || undefined,
      startTime: filterForm.startTime || undefined,
      endTime: filterForm.endTime || undefined,
      page: pagination.page - 1,
      size: pagination.size,
    })
    tableData.value = res.content ?? []
    pagination.total = res.totalElements ?? 0
  } catch {
    ElMessage.error(t('auditLog.message.loadFailed'))
  } finally {
    loading.value = false
  }
}

async function openDetail(row: AuditLogVO) {
  currentLog.value = row
  detailVisible.value = true
}

function onSearch() {
  pagination.page = 1
  loadData()
}

function onReset() {
  filterForm.module = ''
  filterForm.action = ''
  filterForm.resourceType = ''
  filterForm.userId = ''
  filterForm.startTime = ''
  filterForm.endTime = ''
  pagination.page = 1
  loadData()
}

function actionTagType(action: string | undefined) {
  switch (action) {
    case 'LOGIN': return 'info'
    case 'CREATE': return 'success'
    case 'UPDATE': return 'warning'
    case 'DELETE': return 'danger'
    case 'APPROVE': return 'success'
    case 'REJECT': return 'danger'
    default: return 'info'
  }
}

// 初始加载
loadData()
</script>

<style scoped>
.filter-card {
  margin-bottom: 12px;
}
.table-card {
  margin-bottom: 0;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.http-method {
  font-family: monospace;
  font-size: 12px;
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 4px;
}
.http-get { color: #67C23A; background: #f0f9eb; }
.http-post { color: #409EFF; background: #ecf5ff; }
.http-put, .http-patch { color: #E6A23C; background: #fdf6ec; }
.http-delete { color: #F56C6C; background: #fef0f0; }
.detail-json {
  background: #f5f7fa;
  padding: 8px;
  border-radius: 4px;
  font-size: 12px;
  max-height: 300px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
}
</style>
