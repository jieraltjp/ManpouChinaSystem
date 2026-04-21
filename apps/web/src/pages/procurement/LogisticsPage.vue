<template>
  <div class="page">
    <div class="page-header">
      <h2 class="page-title">调配计划</h2>
      <div class="header-actions">
        <el-button type="primary" @click="onNew">
          <el-icon><Plus /></el-icon> 新增调配
        </el-button>
      </div>
    </div>

    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#E8650A"><Van /></el-icon></div>
            <div><div class="stat-value">{{ pagination.total }}</div><div class="stat-label">全部调配</div></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#1E40AF"><Top /></el-icon></div>
            <div><div class="stat-value">{{ bookedCount }}</div><div class="stat-label">已订舱</div></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="icon-transit" color="#7C3AED"><Loading /></el-icon></div>
            <div><div class="stat-value">{{ transitCount }}</div><div class="stat-label">运输中</div></div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="调配类型">
          <el-select v-model="filterForm.planType" placeholder="全部" clearable style="width:140px">
            <el-option value="SEA" label="海运" />
            <el-option value="AIR" label="空运" />
            <el-option value="CONSOLIDATION" label="拼柜" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="全部" clearable style="width:140px">
            <el-option value="PLANNED" label="调配中" />
            <el-option value="BOOKED" label="已订舱" />
            <el-option value="IN_TRANSIT" label="运输中" />
            <el-option value="DELIVERED" label="已送达" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="tableData" stripe style="width:100%">
        <el-table-column prop="planCode" label="调配编号" width="160" />
        <el-table-column prop="productCode" label="货号" width="120">
          <template #default="{ row }">
            <span class="product-code">{{ row.productCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="factoryName" label="工厂名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="planType" label="调配类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="planTypeTag(row.planType)" size="small">{{ planTypeLabel(row.planType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="cargoWeight" label="货物重量" width="100" align="right">
          <template #default="{ row }">
            {{ row.cargoWeight ? row.cargoWeight + 'kg' : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="requiresQc" label="需要检测" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.requiresQc ? 'warning' : 'success'" size="small">
              {{ row.requiresQc ? '需检测' : '无需' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="logisticsStatusType(row.status)" size="small">
              {{ logisticsStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="onView(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <el-drawer v-model="drawerVisible" title="调配详情" size="500px" direction="rtl">
      <el-descriptions :column="2" border v-if="currentRow">
        <el-descriptions-item label="调配编号">{{ currentRow.planCode }}</el-descriptions-item>
        <el-descriptions-item label="调配类型">{{ planTypeLabel(currentRow.planType) }}</el-descriptions-item>
        <el-descriptions-item label="货号">{{ currentRow.productCode }}</el-descriptions-item>
        <el-descriptions-item label="货物重量">{{ currentRow.cargoWeight ? currentRow.cargoWeight + 'kg' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="logisticsStatusType(currentRow.status)" size="small">{{ logisticsStatusLabel(currentRow.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentRow.remarks || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Van, Top, Loading } from '@element-plus/icons-vue'

interface LogisticsPlanVO {
  id: number
  planCode: string
  productCode: string
  factoryName?: string
  planType?: string
  cargoWeight?: number
  requiresQc?: boolean
  status?: string
  remarks?: string
}

const loading = ref(false)
const drawerVisible = ref(false)
const currentRow = ref<LogisticsPlanVO | null>(null)
const filterForm = reactive({ planType: '', status: '' })
const pagination = reactive({ page: 1, pageSize: 20, total: 0 })
const tableData = ref<LogisticsPlanVO[]>([])

const bookedCount = computed(() => tableData.value.filter(r => r.status === 'BOOKED').length)
const transitCount = computed(() => tableData.value.filter(r => r.status === 'IN_TRANSIT').length)

async function loadData() {
  loading.value = true
  try {
    ElMessage.info('调配计划 API 字段确认中，当前显示占位数据')
    pagination.total = 0
    tableData.value = []
  } catch { /* interceptor */ } finally {
    loading.value = false
  }
}

function onReset() {
  filterForm.planType = ''
  filterForm.status = ''
  pagination.page = 1
  loadData()
}

function onNew() {
  ElMessage.info('调配计划 API 字段确认中，表单开发待完成')
}

function onView(row: LogisticsPlanVO) {
  currentRow.value = row
  drawerVisible.value = true
}

function planTypeLabel(type?: string): string {
  return { SEA: '海运', AIR: '空运', CONSOLIDATION: '拼柜' }[type ?? ''] ?? type ?? '-'
}

function planTypeTag(type?: string): string {
  return { SEA: 'primary', AIR: 'warning', CONSOLIDATION: 'success' }[type ?? ''] ?? 'info'
}

function logisticsStatusLabel(status?: string): string {
  return { PLANNED: '调配中', BOOKED: '已订舱', IN_TRANSIT: '运输中', DELIVERED: '已送达' }[status ?? ''] ?? status ?? '-'
}

function logisticsStatusType(status?: string): string {
  return { PLANNED: 'info', BOOKED: 'warning', IN_TRANSIT: 'primary', DELIVERED: 'success' }[status ?? ''] ?? 'info'
}

onMounted(() => loadData())
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.page-header { display: flex; align-items: center; justify-content: space-between; }
.page-title { margin: 0; font-size: 20px; font-weight: 700; color: var(--text-primary); }
.page-title::before { content: ''; display: inline-block; width: 4px; height: 20px; background: var(--color-primary); border-radius: 2px; margin-right: 10px; vertical-align: middle; }
.filter-card :deep(.el-card__body) { padding-bottom: 0; }
.stats-row { margin-bottom: 4px; }
.stat-card { border-radius: var(--radius-md); border: 1px solid var(--border-color); box-shadow: var(--shadow-card); position: relative; overflow: hidden; transition: all var(--transition-fast); }
.stat-card::before { content: ''; position: absolute; top: 0; left: 0; right: 0; height: 3px; background: linear-gradient(90deg, var(--color-primary), var(--color-primary-light)); border-radius: var(--radius-md) var(--radius-md) 0 0; }
.stat-card:hover { box-shadow: var(--shadow-md); transform: translateY(-2px); }
.stat-content { display: flex; align-items: center; gap: 14px; }
.stat-icon-wrap { width: 48px; height: 48px; border-radius: 50%; background: var(--color-primary-pale); display: flex; align-items: center; justify-content: center; }
.icon-transit { animation: spin 1.5s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.stat-icon { font-size: 22px; }
.stat-value { font-size: 26px; font-weight: 800; color: var(--text-primary); line-height: 1; font-variant-numeric: tabular-nums; }
.stat-label { font-size: 13px; color: var(--text-secondary); margin-top: 4px; }
.product-code { color: var(--color-primary); font-family: monospace; font-size: 12px; font-weight: 700; background: var(--color-primary-pale); padding: 3px 9px; border-radius: 5px; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
