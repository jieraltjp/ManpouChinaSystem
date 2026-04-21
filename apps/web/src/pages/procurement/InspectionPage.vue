<template>
  <div class="page">
    <div class="page-header">
      <h2 class="page-title">验货记录</h2>
      <div class="header-actions">
        <el-button type="primary" @click="onNew">
          <el-icon><Plus /></el-icon> 新规验货
        </el-button>
      </div>
    </div>

    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#E8650A"><Document /></el-icon></div>
            <div><div class="stat-value">{{ pagination.total }}</div><div class="stat-label">全部记录</div></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#16A34A"><CircleCheck /></el-icon></div>
            <div><div class="stat-value">{{ passCount }}</div><div class="stat-label">合格</div></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#DC2626"><Warning /></el-icon></div>
            <div><div class="stat-value">{{ failCount }}</div><div class="stat-label">不合格</div></div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="验货编号">
          <el-input v-model="filterForm.qcCode" placeholder="如 Q-20260421-001" clearable style="width:180px" />
        </el-form-item>
        <el-form-item label="验货结果">
          <el-select v-model="filterForm.result" placeholder="全部" clearable style="width:120px">
            <el-option value="PASS" label="合格" />
            <el-option value="FAIL" label="不合格" />
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
        <el-table-column prop="qcCode" label="验货编号" width="160" />
        <el-table-column prop="productCode" label="货号" width="120">
          <template #default="{ row }">
            <span class="product-code">{{ row.productCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="inspectionCount" label="检品数" width="90" align="right" />
        <el-table-column prop="passedCount" label="合格数" width="90" align="right">
          <template #default="{ row }">
            <span style="color:#16A34A;font-weight:600">{{ row.passedCount ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="defectiveCount" label="不良数" width="90" align="right">
          <template #default="{ row }">
            <span v-if="row.defectiveCount" style="color:#DC2626;font-weight:600">{{ row.defectiveCount }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="boxCount" label="箱数" width="70" align="right" />
        <el-table-column prop="qcDate" label="验货日期" width="120" />
        <el-table-column prop="result" label="结果" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.result === 'PASS' ? 'success' : 'danger'" size="small">
              {{ row.result === 'PASS' ? '合格' : '不合格' }}
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

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" title="验货详情" size="560px" direction="rtl">
      <el-descriptions :column="2" border v-if="currentRow">
        <el-descriptions-item label="验货编号">{{ currentRow.qcCode }}</el-descriptions-item>
        <el-descriptions-item label="验货结果">
          <el-tag :type="currentRow.result === 'PASS' ? 'success' : 'danger'" size="small">
            {{ currentRow.result === 'PASS' ? '合格' : '不合格' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="货号">{{ currentRow.productCode }}</el-descriptions-item>
        <el-descriptions-item label="验货日期">{{ currentRow.qcDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="检品数">{{ currentRow.inspectionCount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="合格数">{{ currentRow.passedCount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="不良数">{{ currentRow.defectiveCount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="箱数">{{ currentRow.boxCount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="卖家名称" :span="2">{{ currentRow.sellerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="验货标准" :span="2">{{ currentRow.qcStandard || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentRow.remarks || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Document, CircleCheck, Warning } from '@element-plus/icons-vue'

interface QcRecordVO {
  id: number
  qcCode: string
  productCode: string
  sellerName?: string
  qcDate?: string
  result?: string
  inspectionCount?: number
  passedCount?: number
  defectiveCount?: number
  boxCount?: number
  qcStandard?: string
  remarks?: string
}

const loading = ref(false)
const drawerVisible = ref(false)
const currentRow = ref<QcRecordVO | null>(null)
const filterForm = reactive({ qcCode: '', result: '' })
const pagination = reactive({ page: 1, pageSize: 20, total: 0 })
const tableData = ref<QcRecordVO[]>([])

const passCount = computed(() => tableData.value.filter(r => r.result === 'PASS').length)
const failCount = computed(() => tableData.value.filter(r => r.result === 'FAIL').length)

async function loadData() {
  loading.value = true
  try {
    // 验货记录 API 暂用占位，字段确认后接入真实 API
    ElMessage.info('验货记录 API 字段确认中，当前显示占位数据')
    pagination.total = 0
    tableData.value = []
  } catch { /* interceptor */ } finally {
    loading.value = false
  }
}

function onReset() {
  filterForm.qcCode = ''
  filterForm.result = ''
  pagination.page = 1
  loadData()
}

function onNew() {
  ElMessage.info('验货记录 API 字段确认中，表单开发待完成')
}

function onView(row: QcRecordVO) {
  currentRow.value = row
  drawerVisible.value = true
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
.stat-icon { font-size: 22px; }
.stat-value { font-size: 26px; font-weight: 800; color: var(--text-primary); line-height: 1; font-variant-numeric: tabular-nums; }
.stat-label { font-size: 13px; color: var(--text-secondary); margin-top: 4px; }
.product-code { color: var(--color-primary); font-family: monospace; font-size: 12px; font-weight: 700; background: var(--color-primary-pale); padding: 3px 9px; border-radius: 5px; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
