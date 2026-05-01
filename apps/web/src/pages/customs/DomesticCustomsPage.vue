<template>
  <div class="page">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#6366F1"><Document /></el-icon></div>
            <div><div class="stat-value">{{ pagination.total }}</div><div class="stat-label">{{ $t('customs.stat.total') }}</div></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#F59E0B"><Clock /></el-icon></div>
            <div><div class="stat-value">{{ statusCount.PENDING }}</div><div class="stat-label">{{ $t('customs.stat.pending') }}</div></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#3B82F6"><Top /></el-icon></div>
            <div><div class="stat-value">{{ statusCount.SUBMITTED }}</div><div class="stat-label">{{ $t('customs.stat.submitted') }}</div></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#16A34A"><CircleCheck /></el-icon></div>
            <div><div class="stat-value">{{ statusCount.CLEARED }}</div><div class="stat-label">{{ $t('customs.stat.cleared') }}</div></div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filterForm">
        <el-form-item :label="$t('customs.filter.customsCode')">
          <el-input v-model="filterForm.keyword" :placeholder="$t('customs.filter.customsCodePlaceholder')" clearable style="width:160px" />
        </el-form-item>
        <el-form-item :label="$t('customs.filter.containerNo')">
          <el-input v-model="filterForm.containerNo" :placeholder="$t('customs.filter.containerNoPlaceholder')" clearable style="width:150px" />
        </el-form-item>
        <el-form-item :label="$t('customs.filter.procurementId')">
          <el-input-number v-model="filterForm.procurementId" :placeholder="$t('customs.filter.procurementIdPlaceholder')" :min="1" style="width:130px; height:32px" clearable />
        </el-form-item>
        <el-form-item :label="$t('customs.filter.status')">
          <el-select v-model="filterForm.status" :placeholder="$t('common.all')" clearable style="width:130px">
            <el-option value="PENDING" :label="$t('customs.status.pending')" />
            <el-option value="SUBMITTED" :label="$t('customs.status.submitted')" />
            <el-option value="CLEARED" :label="$t('customs.status.cleared')" />
            <el-option value="REJECTED" :label="$t('customs.status.rejected')" />
          </el-select>
        </el-form-item>
        <el-form-item style="flex-shrink:0">
          <el-button type="primary" @click="onSearchFromButton">{{ $t('common.search') }}</el-button>
          <el-button @click="onReset">{{ $t('common.reset') }}</el-button>
          <el-button type="primary" @click="onNew">
            <el-icon><Plus /></el-icon><span>{{ $t('customs.batchButton') }}</span>
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表 -->
    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="table-header">
          <el-radio-group v-model="viewMode" size="small">
            <el-radio-button value="list">{{ $t('customs.viewMode.list') }}</el-radio-button>
            <el-radio-button value="group">{{ $t('customs.viewMode.byContainer') }}</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <!-- 列表视图 -->
      <el-table v-loading="loading" v-if="viewMode === 'list'" :data="tableData" stripe style="width:100%" min-height="200">
        <el-table-column prop="customsCode" :label="$t('customs.column.customsCode')" min-width="180">
          <template #default="{ row }">
            <span class="code-badge">{{ row.customsCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="containerNo" :label="$t('customs.column.containerNo')" min-width="140" show-overflow-tooltip />
        <el-table-column prop="procurementId" :label="$t('customs.column.procurementId')" min-width="110" align="center">
          <template #default="{ row }">
            <span v-if="row.procurementId">{{ row.procurementId }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="productCode" :label="$t('customs.column.productCode')" min-width="140">
          <template #default="{ row }">
            <span class="product-code">{{ row.productCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="subProductCode" :label="$t('customs.column.subProductCode')" min-width="100" />
        <el-table-column prop="quantity" :label="$t('customs.column.quantity')" min-width="80" align="right">
          <template #default="{ row }">
            <span v-if="row.quantity !== null">{{ row.quantity }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="estimatedValueCny" :label="$t('customs.column.estimatedValueCny')" min-width="130" align="right">
          <template #default="{ row }">
            <span v-if="row.estimatedValueCny != null" class="money">{{ $t('common.currency.cny') }}{{ row.estimatedValueCny?.toLocaleString(undefined, { minimumFractionDigits: 2 }) }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" :label="$t('customs.column.status')" min-width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="$t('customs.column.createTime')" min-width="160" />
        <el-table-column :label="$t('customs.column.action')" min-width="200" align="center">
          <template #default="{ row }">
            <el-button link class="btn-blue" size="small" @click.stop="onView(row)">{{ $t('customs.action.detail') }}</el-button>
            <template v-if="row.status === 'PENDING'">
              <el-button link type="success" size="small" :loading="actionLoading === row.id + '-submit'" @click.stop="onSubmit(row)">{{ $t('customs.action.submit') }}</el-button>
              <el-button link type="danger" size="small" :loading="actionLoading === row.id + '-delete'" @click.stop="onDelete(row)">{{ $t('common.delete') }}</el-button>
            </template>
            <template v-else-if="row.status === 'SUBMITTED'">
              <el-button link type="success" size="small" :loading="actionLoading === row.id + '-clear'" @click.stop="onClear(row)">{{ $t('customs.action.clear') }}</el-button>
              <el-button link type="danger" size="small" :loading="actionLoading === row.id + '-reject'" @click.stop="onReject(row)">{{ $t('customs.action.reject') }}</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分组视图 -->
      <el-collapse v-loading="loading" v-if="viewMode === 'group'" class="container-group-collapse">
        <el-collapse-item v-for="group in groupedData" :key="group.containerNo">
          <template #title>
            <div class="group-header">
              <el-icon class="group-icon"><Box /></el-icon>
              <span class="group-title">
                {{ group.containerNo || $t('customs.group.ungrouped') }}
              </span>
              <el-tag size="small" type="info" class="group-count">{{ group.records.length }} {{ $t('customs.group.records') }}</el-tag>
              <el-tag v-if="group.statusCount.PENDING" size="small" type="warning" class="group-badge">{{ group.statusCount.PENDING }} {{ $t('customs.status.pending') }}</el-tag>
              <el-tag v-if="group.statusCount.SUBMITTED" size="small" type="primary" class="group-badge">{{ group.statusCount.SUBMITTED }} {{ $t('customs.status.submitted') }}</el-tag>
              <el-tag v-if="group.statusCount.CLEARED" size="small" type="success" class="group-badge">{{ group.statusCount.CLEARED }} {{ $t('customs.status.cleared') }}</el-tag>
              <el-tag v-if="group.statusCount.REJECTED" size="small" type="danger" class="group-badge">{{ group.statusCount.REJECTED }} {{ $t('customs.status.rejected') }}</el-tag>
            </div>
          </template>
          <el-table :data="group.records" stripe>
            <el-table-column prop="customsCode" :label="$t('customs.column.customsCode')" min-width="180">
              <template #default="{ row }">
                <span class="code-badge">{{ row.customsCode }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="productCode" :label="$t('customs.column.productCode')" min-width="120">
              <template #default="{ row }">
                <span class="product-code">{{ row.productCode }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="subProductCode" :label="$t('customs.column.subProductCode')" min-width="100" />
            <el-table-column prop="quantity" :label="$t('customs.column.quantity')" min-width="80" align="right">
              <template #default="{ row }">
                <span v-if="row.quantity !== null">{{ row.quantity }}</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="estimatedValueCny" :label="$t('customs.column.estimatedValueCny')" min-width="130" align="right">
              <template #default="{ row }">
                <span v-if="row.estimatedValueCny != null" class="money">{{ $t('common.currency.cny') }}{{ row.estimatedValueCny?.toLocaleString(undefined, { minimumFractionDigits: 2 }) }}</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" :label="$t('customs.column.status')" min-width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" :label="$t('customs.column.createTime')" min-width="160" />
            <el-table-column :label="$t('customs.column.action')" min-width="180" align="center">
              <template #default="{ row }">
                <el-button link class="btn-blue" size="small" @click.stop="onView(row)">{{ $t('customs.action.detail') }}</el-button>
                <template v-if="row.status === 'PENDING'">
                  <el-button link type="success" size="small" :loading="actionLoading === row.id + '-submit'" @click.stop="onSubmit(row)">{{ $t('customs.action.submit') }}</el-button>
                  <el-button link type="danger" size="small" :loading="actionLoading === row.id + '-delete'" @click.stop="onDelete(row)">{{ $t('common.delete') }}</el-button>
                </template>
                <template v-else-if="row.status === 'SUBMITTED'">
                  <el-button link type="success" size="small" :loading="actionLoading === row.id + '-clear'" @click.stop="onClear(row)">{{ $t('customs.action.clear') }}</el-button>
                  <el-button link type="danger" size="small" :loading="actionLoading === row.id + '-reject'" @click.stop="onReject(row)">{{ $t('customs.action.reject') }}</el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>
        </el-collapse-item>
      </el-collapse>

      <div v-if="viewMode === 'list'" class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="onSearch"
          @current-change="onSearch"
        />
      </div>
    </el-card>

    <!-- 新规创建弹窗（v1.4.0 批量模式） -->
    <el-dialog v-model="dialogVisible" :title="$t('customs.newDialogTitle')" width="720px" destroy-on-close>
      <!-- 第一行：货柜号搜索 -->
      <div class="batch-header">
        <span class="batch-label">{{ $t('customs.dialog.containerNo') }}：</span>
        <el-select
          v-model="batchForm.containerNo"
          filterable
          remote
          reserve-keyword
          :placeholder="$t('customs.dialog.containerNoPlaceholder')"
          :remote-method="searchContainers"
          :loading="containerLoading"
          style="flex:1; min-width:200px"
          @change="onBatchContainerSelect"
          clearable
        >
          <el-option
            v-for="item in containerOptions"
            :key="item.containerNo"
            :label="item.containerNo"
            :value="item.containerNo"
          >
            <span style="font-weight:600">{{ item.containerNo }}</span>
            <span style="color:#999;font-size:12px;margin-left:8px">{{ item.productCode }} / {{ item.factoryName || ('ID:' + item.factoryId) }}</span>
          </el-option>
        </el-select>
        <span v-if="batchForm.containerNo" class="batch-hint">
          {{ $t('customs.batch.selectedCount', { n: selectedPlanIds.length }) }}
        </span>
      </div>

      <!-- 第二行：调配计划表格（选中要创建报关的记录） -->
      <div v-if="batchForm.containerNo" class="batch-plan-table">
        <el-table
          :data="batchPlanList"
          max-height="280"
          stripe
          @selection-change="onBatchSelectionChange"
        >
          <el-table-column type="selection" width="40" />
          <el-table-column :label="$t('customs.column.productCode')" min-width="130">
            <template #default="{ row }">
              <span class="product-code">{{ row.productCode }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('customs.column.subProductCode')" prop="subProductCode" min-width="110" />
          <el-table-column :label="$t('customs.column.factoryId')" prop="factoryId" min-width="80" align="center" />
          <el-table-column :label="$t('customs.column.quantity')" prop="quantity" min-width="70" align="right" />
          <el-table-column :label="$t('customs.column.status')" width="80" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.customsCreated" type="success" size="small">{{ $t('customs.status.created') }}</el-tag>
              <el-tag v-else type="info" size="small">{{ $t('customs.status.notCreated') }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 第三行：批量字段（预估货值 + 备注） -->
      <div v-if="selectedPlanIds.length > 0" class="batch-footer">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item :label="$t('customs.dialog.estimatedValueCny')" style="margin-bottom:0">
              <el-input-number v-model="batchForm.estimatedValueCny" :min="0" :precision="2" :placeholder="$t('customs.dialog.estimatedValueCnyPlaceholder')" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('customs.batch.selectedPlans')" style="margin-bottom:0">
              <div class="batch-count-display">{{ selectedPlanIds.length }} {{ $t('customs.batch.items') }}</div>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('customs.dialog.remarks')" style="margin-bottom:0">
              <el-input v-model="batchForm.remarks" type="textarea" :rows="2" :placeholder="$t('customs.dialog.remarksPlaceholder')" />
            </el-form-item>
          </el-col>
        </el-row>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button
          type="primary"
          :loading="submitting"
          :disabled="selectedPlanIds.length === 0"
          @click="onBatchSubmit"
        >
          {{ $t('customs.batch.createButton', { n: selectedPlanIds.length }) }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" :title="$t('customs.drawerTitle')" size="560px" direction="rtl">
      <el-descriptions :column="2" border v-if="currentRow">
        <el-descriptions-item :label="$t('customs.column.customsCode')">
          <span class="code-badge">{{ currentRow.customsCode }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('customs.column.containerNo')">{{ currentRow.containerNo ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('customs.column.status')">
          <el-tag :type="statusTagType(currentRow.status)" size="small">{{ statusLabel(currentRow.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('customs.column.procurementId')">{{ currentRow.procurementId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('customs.column.factoryId')">{{ currentRow.factoryId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('customs.column.productCode')">
          <span class="product-code">{{ currentRow.productCode }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('customs.column.subProductCode')">{{ currentRow.subProductCode ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('customs.column.quantity')">{{ currentRow.quantity ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('customs.column.estimatedValueCny')">
          <span v-if="currentRow.estimatedValueCny !== null" class="money">{{ $t('common.currency.cny') }}{{ currentRow.estimatedValueCny.toLocaleString(undefined, { minimumFractionDigits: 2 }) }}</span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('customs.dialog.remarks')" :span="2">{{ currentRow.remarks || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('customs.dialog.createBy')">{{ currentRow.createBy || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('customs.dialog.createTime')">{{ currentRow.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('customs.dialog.updateTime')">{{ currentRow.updateTime || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <!-- 驳回原因弹窗 -->
    <el-dialog v-model="rejectDialogVisible" :title="$t('customs.rejectDialogTitle')" width="420px">
      <el-form>
        <el-form-item :label="$t('customs.rejectDialog.reasonLabel')">
          <el-input v-model="rejectReason" type="textarea" :rows="3" :placeholder="$t('customs.rejectDialog.reasonPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="danger" :loading="actionLoading.startsWith('reject-')" @click="onRejectConfirm">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Document, Clock, Top, CircleCheck, Box } from '@element-plus/icons-vue'
import { customsApi, type CustomsVO, type DomesticCustomsStatus, type CustomsCreateRequest, type CustomsBatchCreateRequest } from '@/api/customs'
import { logisticsApi, type LogisticsPlanVO } from '@/api/logistics'
import { useI18n } from 'vue-i18n'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const drawerVisible = ref(false)
const rejectDialogVisible = ref(false)
const actionLoading = ref('')
const rejectReason = ref('')
const rejectingRowId = ref<number | null>(null)
const viewMode = ref<'list' | 'group'>('list')

const containerOptions = ref<LogisticsPlanVO[]>([])
const containerLoading = ref(false)

const currentRow = ref<CustomsVO | null>(null)
const filterForm = reactive({
  keyword: '',
  containerNo: '',
  procurementId: undefined as number | undefined,
  status: '' as DomesticCustomsStatus | '',
})
const pagination = reactive({ page: 1, pageSize: 20, total: 0 })
const tableData = ref<CustomsVO[]>([])

const route = useRoute()
const { t } = useI18n()

// 单记录表单（保留备用；当前主模式为批量）
const form = reactive<CustomsCreateRequest>({
  containerNo: '',
  procurementId: undefined,
  factoryId: undefined,
  productCode: '',
  subProductCode: undefined,
  quantity: undefined,
  estimatedValueCny: undefined,
  remarks: undefined,
})

// 批量创建表单（v1.4.0）
const batchForm = reactive({
  containerNo: '',
  estimatedValueCny: undefined as number | undefined,
  remarks: '',
})
const batchPlanList = ref<(LogisticsPlanVO & { customsCreated?: boolean })[]>([])
const batchSelectedRows = ref<(LogisticsPlanVO & { customsCreated?: boolean })[]>([])
const selectedPlanIds = computed(() => batchSelectedRows.value.map(r => r.id!))

const statusCount = computed(() => {
  const counts: Record<string, number> = { PENDING: 0, SUBMITTED: 0, CLEARED: 0, REJECTED: 0 }
  tableData.value.forEach(r => { if (r.status in counts) counts[r.status]++ })
  return counts
})

const groupedData = computed(() => {
  const groups = new Map<string, { containerNo: string | null; records: CustomsVO[]; statusCount: Record<string, number> }>()
  tableData.value.forEach(record => {
    const key = record.containerNo ?? '__UNGROUPED__'
    if (!groups.has(key)) {
      groups.set(key, { containerNo: record.containerNo ?? null, records: [], statusCount: { PENDING: 0, SUBMITTED: 0, CLEARED: 0, REJECTED: 0 } })
    }
    const group = groups.get(key)!
    group.records.push(record)
    if (record.status in group.statusCount) group.statusCount[record.status]++
  })
  return Array.from(groups.values()).sort((a, b) => {
    if (a.containerNo === null) return 1
    if (b.containerNo === null) return -1
    return a.containerNo.localeCompare(b.containerNo)
  })
})

function statusLabel(status?: string): string {
  const map: Record<string, string> = {
    PENDING: t('customs.status.pending'),
    SUBMITTED: t('customs.status.submitted'),
    CLEARED: t('customs.status.cleared'),
    REJECTED: t('customs.status.rejected'),
  }
  return map[status ?? ''] ?? status ?? '-'
}

function statusTagType(status?: string): string {
  const map: Record<string, string> = {
    PENDING: 'warning',
    SUBMITTED: 'primary',
    CLEARED: 'success',
    REJECTED: 'danger',
  }
  return map[status ?? ''] ?? 'info'
}

async function loadData() {
  loading.value = true
  try {
    const res = await customsApi.list({
      page: pagination.page - 1,
      pageSize: pagination.pageSize,
      keyword: filterForm.keyword || undefined,
      containerNo: filterForm.containerNo || undefined,
      procurementId: filterForm.procurementId,
      status: filterForm.status || undefined,
    })
    const data = res.data
    tableData.value = data?.content ?? []
    pagination.total = data?.totalElements ?? 0
  } catch (e: unknown) {
    console.error('[DomesticCustomsPage] loadData failed', e)
    ElMessage.error(t('customs.message.loadFailed'))
  } finally {
    loading.value = false
  }
}

function onSearch() { loadData() }
function onSearchFromButton() { pagination.page = 1; loadData() }

function onReset() {
  filterForm.keyword = ''
  filterForm.containerNo = ''
  filterForm.procurementId = undefined
  filterForm.status = ''
  pagination.page = 1
  loadData()
}

async function searchContainers(query: string) {
  if (!query || query.length < 2) {
    containerOptions.value = []
    return
  }
  containerLoading.value = true
  try {
    const res = await logisticsApi.list({ containerNo: query, pageSize: 20 })
    containerOptions.value = res.data?.content ?? []
  } catch {
    containerOptions.value = []
  } finally {
    containerLoading.value = false
  }
}

async function onBatchContainerSelect(containerNo: string) {
  if (!containerNo) {
    batchPlanList.value = []
    batchSelectedRows.value = []
    return
  }
  containerLoading.value = true
  try {
    const res = await logisticsApi.list({ containerNo, pageSize: 100 })
    const plans: LogisticsPlanVO[] = res.data?.content ?? []
    // 检查每个 plan 是否已有报关记录（通过表格数据中的 customsCode 推断）
    const existingContainerNos = new Set(
      tableData.value.filter(r => r.containerNo === containerNo).map(r => r.productCode + '-' + r.subProductCode)
    )
    batchPlanList.value = plans.map(p => ({
      ...p,
      customsCreated: existingContainerNos.has(p.productCode + '-' + (p.subProductCode ?? '')),
    }))
    batchSelectedRows.value = []
  } catch {
    batchPlanList.value = []
  } finally {
    containerLoading.value = false
  }
}

function onBatchSelectionChange(rows: (LogisticsPlanVO & { customsCreated?: boolean })[]) {
  batchSelectedRows.value = rows
}

function onNew() {
  containerOptions.value = []
  batchPlanList.value = []
  batchSelectedRows.value = []
  // v1.4.0: URL 参数货柜号直接代入批量表单
  const urlContainerNo = route.query.containerNo as string | undefined
  Object.assign(batchForm, {
    containerNo: urlContainerNo ?? '',
    estimatedValueCny: undefined,
    remarks: '',
  })
  Object.assign(form, {
    containerNo: '',
    procurementId: undefined,
    factoryId: undefined,
    productCode: '',
    subProductCode: undefined,
    quantity: undefined,
    estimatedValueCny: undefined,
    remarks: undefined,
  })
  // URL 参数有货柜号时自动加载调配计划列表
  if (urlContainerNo) {
    searchContainers(urlContainerNo)
    containerOptions.value = [{ containerNo: urlContainerNo } as LogisticsPlanVO]
    // 延迟加载 batch 计划列表
    setTimeout(() => onBatchContainerSelect(urlContainerNo!), 100)
  }
  dialogVisible.value = true
}

async function onBatchSubmit() {
  if (selectedPlanIds.value.length === 0) {
    ElMessage.warning(t('customs.validation.selectAtLeastOne'))
    return
  }
  submitting.value = true
  try {
    const req: CustomsBatchCreateRequest = {
      containerNo: batchForm.containerNo,
      logisticsPlanIds: selectedPlanIds.value,
      estimatedValueCny: batchForm.estimatedValueCny,
      remarks: batchForm.remarks || undefined,
    }
    const res = await customsApi.batchCreate(req)
    const ids: number[] = res.data ?? []
    ElMessage.success(t('customs.message.batchCreateSuccess', { n: ids.length }))
    dialogVisible.value = false
    loadData()
  } catch (e) {
    console.error('[DomesticCustomsPage] onBatchSubmit failed', e)
    ElMessage.error(t('customs.message.createFailed'))
  } finally {
    submitting.value = false
  }
}

function onView(row: CustomsVO) {
  currentRow.value = row
  drawerVisible.value = true
}

async function onSubmit(row: CustomsVO) {
  actionLoading.value = `${row.id}-submit`
  try {
    await customsApi.submit(row.id)
    ElMessage.success(t('customs.message.submitSuccess'))
    loadData()
  } catch (e) {
    console.error('[DomesticCustomsPage] submit failed', e)
    ElMessage.error(t('customs.message.actionFailed'))
  } finally {
    actionLoading.value = ''
  }
}

async function onClear(row: CustomsVO) {
  actionLoading.value = `${row.id}-clear`
  try {
    await customsApi.clear(row.id)
    ElMessage.success(t('customs.message.clearSuccess'))
    loadData()
  } catch (e) {
    console.error('[DomesticCustomsPage] clear failed', e)
    ElMessage.error(t('customs.message.actionFailed'))
  } finally {
    actionLoading.value = ''
  }
}

function onReject(row: CustomsVO) {
  rejectingRowId.value = row.id
  rejectReason.value = ''
  rejectDialogVisible.value = true
}

async function onRejectConfirm() {
  if (!rejectingRowId.value) return
  actionLoading.value = `reject-${rejectingRowId.value}`
  try {
    await customsApi.reject(rejectingRowId.value, { reason: rejectReason.value })
    ElMessage.success(t('customs.message.rejectSuccess'))
    rejectDialogVisible.value = false
    loadData()
  } catch (e) {
    console.error('[DomesticCustomsPage] reject failed', e)
    ElMessage.error(t('customs.message.actionFailed'))
  } finally {
    actionLoading.value = ''
    rejectingRowId.value = null
  }
}

async function onDelete(row: CustomsVO) {
  try {
    await ElMessageBox.confirm(t('customs.message.deleteConfirm'), t('common.warning'), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      type: 'warning',
    })
  } catch { return }
  actionLoading.value = `${row.id}-delete`
  try {
    await customsApi.delete(row.id)
    ElMessage.success(t('customs.message.deleteSuccess'))
    loadData()
  } catch (e) {
    console.error('[DomesticCustomsPage] delete failed', e)
    ElMessage.error(t('customs.message.actionFailed'))
  } finally {
    actionLoading.value = ''
  }
}

onMounted(() => {
  // v1.3.0: URL 参数货柜号自动填入筛选栏
  const urlContainerNo = route.query.containerNo as string | undefined
  if (urlContainerNo) {
    filterForm.containerNo = urlContainerNo
  }
  loadData()
})

// 修正 el-table 空状态时 empty-block 宽度超出列宽
watch(tableData, () => {
  nextTick(() => {
    const headerTable = document.querySelector('.el-table__header') as HTMLElement
    const scrollView = document.querySelector('.el-scrollbar__view') as HTMLElement
    const emptyBlock = document.querySelector('.el-table__empty-block') as HTMLElement
    if (headerTable) {
      const headerW = headerTable.offsetWidth
      if (scrollView) scrollView.style.width = headerW + 'px'
      if (emptyBlock) emptyBlock.style.width = headerW + 'px'
    }
  })
})
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.stats-row { margin-bottom: 0; }
.stat-card { border-radius: var(--radius-md); border: 1px solid var(--border-color); box-shadow: var(--shadow-card); position: relative; overflow: hidden; transition: all var(--transition-fast); }
.stat-card::before { content: ''; position: absolute; top: 0; left: 0; right: 0; height: 3px; background: linear-gradient(90deg, var(--color-primary), var(--color-primary-light)); border-radius: var(--radius-md) var(--radius-md) 0 0; }
.stat-card:hover { box-shadow: var(--shadow-md); transform: translateY(-2px); }
.stat-content { display: flex; align-items: center; gap: 14px; }
.stat-icon-wrap { width: 48px; height: 48px; border-radius: 50%; background: var(--color-primary-pale); display: flex; align-items: center; justify-content: center; }
.stat-icon { font-size: 22px; }
.stat-value { font-size: 26px; font-weight: 800; color: var(--text-primary); line-height: 1; font-variant-numeric: tabular-nums; }
.stat-label { font-size: 13px; color: var(--text-secondary); margin-top: 4px; }
.code-badge { color: var(--color-primary); font-family: monospace; font-size: 12px; font-weight: 700; background: var(--color-primary-pale); padding: 3px 9px; border-radius: 5px; }
.product-code { color: var(--color-primary); font-family: monospace; font-size: 12px; font-weight: 700; background: var(--color-primary-pale); padding: 3px 9px; border-radius: 5px; }
.money { color: #16A34A; font-weight: 600; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
.table-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.container-group-collapse { margin-top: 8px; }
.group-header { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.group-icon { font-size: 18px; color: var(--color-primary); }
.group-title { font-weight: 600; font-size: 14px; color: var(--text-primary); }
.group-count { margin-left: 4px; }
.group-badge { margin-left: 4px; }
/* 批量创建样式 */
.batch-header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.batch-label { font-weight: 600; color: var(--text-primary); white-space: nowrap; }
.batch-hint { font-size: 13px; color: var(--text-secondary); white-space: nowrap; }
.batch-plan-table { margin-bottom: 12px; border: 1px solid var(--border-color); border-radius: var(--radius-sm); overflow: hidden; }
.batch-footer { background: var(--bg-page); border-radius: var(--radius-sm); padding: 12px 4px; }
.batch-count-display { font-size: 22px; font-weight: 800; color: var(--text-primary); font-variant-numeric: tabular-nums; line-height: 32px; }
.filter-card :deep(.el-card__body) { padding-bottom: 0; }
</style>
