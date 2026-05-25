<template>
  <div class="test-page">
    <!-- 页面标题 + 返回 -->
    <div class="page-header">
      <el-button text @click="router.back()">
        <el-icon><ArrowLeft /></el-icon>{{ $t('common.back') }}
      </el-button>
      <span class="page-title">{{ $t('shipmentBatch.title') }}</span>
    </div>

    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filterForm">
        <el-form-item :label="$t('shipmentBatch.filter.status')">
          <el-select v-model="filterForm.status" :placeholder="$t('order.filter.all')" clearable style="width: 140px">
            <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">{{ $t('order.filter.search') }}</el-button>
          <el-button @click="onReset">{{ $t('order.filter.reset') }}</el-button>
          <el-button type="primary" @click="onNew" :disabled="!procurementId" v-if="hasPermission('shipment:create')">
            <el-icon><Plus /></el-icon>{{ $t('shipmentBatch.action.create') }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="table-header">
          <el-radio-group v-model="excelViewMode" size="small">
            <el-radio-button value="table">{{ $t('common.viewMode.table') }}</el-radio-button>
            <el-radio-button value="copy">{{ $t('common.viewMode.excel') }}</el-radio-button>
          </el-radio-group>
          <div class="batch-actions">
            <span v-if="selectedRows.length" class="selection-count">
              <el-tag type="info" size="small">{{ $t('common.batch.selectedCount', { n: selectedRows.length }) }}</el-tag>
            </span>
            <el-button
              v-if="selectedRows.length"
              type="danger"
              size="small"
              @click="onBatchDelete"
            >
              <el-icon><Delete /></el-icon>{{ $t('common.batch.delete', { n: selectedRows.length }) }}
            </el-button>
          </div>
        </div>
      </template>
      <el-table v-if="excelViewMode === 'table'" ref="tableRef" v-loading="loading" :data="tableRows" stripe style="width: 100%" row-key="id" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="50" align="center" :reserve-selection="true" />
        <el-table-column prop="productMasterCode" :label="$t('product.column.masterCode')" min-width="100" align="center" show-overflow-tooltip />
        <el-table-column prop="productSubCode" :label="$t('product.column.subCode')" min-width="100" align="center" show-overflow-tooltip />
        <el-table-column prop="productImageUrl" :label="$t('product.column.image')" width="80" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.productImageUrl"
              :src="row.productImageUrl"
              fit="contain"
              style="width: 48px; height: 48px;"
              :preview-src-list="[row.productImageUrl]"
              preview-teleported
            />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="batchCode" :label="$t('shipmentBatch.column.batchCode')" min-width="160" show-overflow-tooltip />
        <el-table-column prop="shipmentQuantity" :label="$t('shipmentBatch.column.shipmentQuantity')" min-width="120" align="right">
          <template #default="{ row }">{{ row.shipmentQuantity?.toLocaleString() ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="factoryShipDate" :label="$t('shipmentBatch.column.factoryShipDate')" min-width="120" align="center">
          <template #default="{ row }">{{ row.factoryShipDate || '-' }}</template>
        </el-table-column>
        <el-table-column prop="actualShipDate" :label="$t('shipmentBatch.column.actualShipDate')" min-width="120" align="center">
          <template #default="{ row }">{{ row.actualShipDate || '-' }}</template>
        </el-table-column>
        <el-table-column prop="status" :label="$t('shipmentBatch.column.status')" min-width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remarks" :label="$t('shipmentBatch.column.remarks')" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remarks || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createBy" :label="$t('shipmentBatch.column.createBy')" min-width="100" align="center" />
        <el-table-column prop="createTime" :label="$t('shipmentBatch.column.createTime')" min-width="160" align="center">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column :label="$t('order.column.action')" min-width="200" align="center">
          <template #default="{ row }">
            <el-button link class="btn-blue" size="small" @click="onDetail(row)">{{ $t('demand.action.detail') }}</el-button>
            <el-button link class="btn-blue" size="small" @click="onEdit(row)" v-if="hasPermission('shipment:update')">{{ $t('demand.action.edit') }}</el-button>
            <el-button link type="danger" size="small" @click="onDelete(row)"
              :disabled="!deletableStatuses.includes(row.status)" v-if="hasPermission('shipment:delete')">{{ $t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <ExcelTable v-else :columns="copyColumns" :data="tableRows" />

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
    <el-drawer v-model="detailVisible" :title="$t('shipmentBatch.drawer.title')" size="560px" direction="rtl" bodyStyle="overflow-y: auto">
      <el-descriptions :column="2" border v-if="currentRow">
        <el-descriptions-item :label="$t('shipmentBatch.column.batchCode')">
          <span class="code-badge">{{ currentRow.batchCode }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('shipmentBatch.column.status')">
          <el-tag :type="statusType(currentRow.status)" size="small">{{ statusLabel(currentRow.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('product.column.masterCode')">{{ currentRow.productMasterCode || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('product.column.subCode')">{{ currentRow.productSubCode || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('shipmentBatch.column.shipmentQuantity')">{{ currentRow.shipmentQuantity?.toLocaleString() ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('shipmentBatch.column.factoryShipDate')">{{ currentRow.factoryShipDate || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('shipmentBatch.column.actualShipDate')">{{ currentRow.actualShipDate || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('shipmentBatch.column.qcRecordCount')">{{ currentRow.qcRecordCount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('shipmentBatch.column.totalPassedCount')">{{ currentRow.totalPassedCount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('shipmentBatch.column.remarks')" :span="2">{{ currentRow.remarks || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('shipmentBatch.column.createBy')">{{ currentRow.createBy || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('shipmentBatch.column.createTime')">{{ formatTime(currentRow.createTime) }}</el-descriptions-item>
        <el-descriptions-item :label="$t('shipmentBatch.column.updateBy')">{{ currentRow.updateBy || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('shipmentBatch.column.updateTime')">{{ formatTime(currentRow.updateTime) }}</el-descriptions-item>
        <el-descriptions-item :label="$t('product.column.image')" :span="2">
          <el-image
            v-if="currentRow.productImageUrl"
            :src="currentRow.productImageUrl"
            fit="contain"
            style="width: 80px; height: 80px;"
            :preview-src-list="[currentRow.productImageUrl]"
            preview-teleported
          />
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? $t('shipmentBatch.dialog.createTitle') : $t('shipmentBatch.dialog.editTitle')" width="560px">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item :label="$t('shipmentBatch.dialog.procurementId')" v-if="dialogMode === 'create'">
          <el-input-number v-model="formData.procurementId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="$t('shipmentBatch.dialog.shipmentQuantity')" prop="shipmentQuantity">
          <el-input-number v-model="formData.shipmentQuantity" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="$t('shipmentBatch.dialog.factoryShipDate')">
          <el-date-picker v-model="formData.factoryShipDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="dialogMode === 'update'" :label="$t('shipmentBatch.dialog.actualShipDate')">
          <el-date-picker v-model="formData.actualShipDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="dialogMode === 'update'" :label="$t('shipmentBatch.dialog.status')">
          <el-select v-model="formData.status" style="width: 100%">
            <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('shipmentBatch.dialog.remarks')">
          <el-input v-model="formData.remarks" type="textarea" :rows="2" maxlength="512" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('order.dialog.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">{{ $t('order.dialog.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { usePermission } from '@/composables/usePermission'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, ElMessageBox } from 'element-plus'
import { Plus, ArrowLeft, Delete } from '@element-plus/icons-vue'
import { shipmentBatchApi, type ShipmentBatchVO, type ShipmentBatchStatus } from '@/api/procurement'
import { useI18n } from 'vue-i18n'
import ExcelTable, { type ExcelColDef } from '@/components/ExcelTable.vue'

const route = useRoute()
const router = useRouter()
const { t, locale: localeRef } = useI18n()
const { hasPermission } = usePermission()

const loading = ref(false)
const tableRef = ref()
const selectedRows = ref<ShipmentBatchVO[]>([])
const submitting = ref(false)
const excelViewMode = ref<'table' | 'copy'>('table')
const dialogVisible = ref(false)
const detailVisible = ref(false)
const dialogMode = ref<'create' | 'update'>('create')
const currentRow = ref<ShipmentBatchVO | null>(null)
const formRef = ref<FormInstance>()
const procurementId = computed(() => route.query.procurementId ? Number(route.query.procurementId) : null)

// 后端 ShipmentBatchStatus 枚举值（同时作为 i18n key）
const BATCH_STATUS_PENDING = '待验货'
const BATCH_STATUS_INSPECTING = '验货中'
const BATCH_STATUS_INSPECTED = '已验货'
const BATCH_STATUS_CANCELLED = '已取消'

const deletableStatuses: ShipmentBatchStatus[] = [BATCH_STATUS_PENDING]

const ALL_BATCH_STATUSES: ShipmentBatchStatus[] = [
  BATCH_STATUS_PENDING,
  BATCH_STATUS_INSPECTING,
  BATCH_STATUS_INSPECTED,
  BATCH_STATUS_CANCELLED,
]

const statusOptions = computed(() =>
  ALL_BATCH_STATUSES.map(v => ({
    value: v,
    label: t(`shipmentBatch.status.${v}` as any, { default: v }),
  }))
)

const filterForm = reactive({ status: '' as ShipmentBatchStatus | '' })
const pagination = reactive({ page: 1, pageSize: 20, total: 0 })
const tableRows = ref<ShipmentBatchVO[]>([])

function formatTime(ts: string | undefined): string {
  if (!ts) return '-'
  return new Date(ts).toLocaleString(localeRef.value === 'ja' ? 'ja-JP' : 'zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit',
  })
}

function statusLabel(status: ShipmentBatchStatus | undefined): string {
  if (!status) return '-'
  return t(`shipmentBatch.status.${status}` as any, { default: status })
}

function statusType(status: ShipmentBatchStatus | undefined): string {
  if (!status) return ''
  const map: Record<ShipmentBatchStatus, string> = {
    [BATCH_STATUS_PENDING]: 'info',
    [BATCH_STATUS_INSPECTING]: 'warning',
    [BATCH_STATUS_INSPECTED]: 'success',
    [BATCH_STATUS_CANCELLED]: 'danger',
  }
  return map[status] ?? ''
}

async function loadData() {
  loading.value = true
  try {
    const res = await shipmentBatchApi.list({
      page: pagination.page - 1,
      pageSize: pagination.pageSize,
      procurementId: procurementId.value ?? undefined,
      status: filterForm.status || undefined,
    })
    const payload = res.data as { content: ShipmentBatchVO[]; totalElements: number }
    tableRows.value = payload?.content ?? []
    pagination.total = payload?.totalElements ?? 0
    if (tableRows.value.length === 0 && pagination.total > 0 && pagination.page > 1) {
      pagination.page = 1
      loadData()
    }
  } catch { /* handled by interceptor */ } finally {
    loading.value = false
  }
}

function onReset() {
  filterForm.status = ''
  pagination.page = 1
  loadData()
}

function onNew() {
  dialogMode.value = 'create'
  currentRow.value = null
  Object.assign(formData, defaultFormData())
  if (procurementId.value) formData.procurementId = procurementId.value
  dialogVisible.value = true
}

function onDetail(row: ShipmentBatchVO) {
  currentRow.value = row
  detailVisible.value = true
}

function onEdit(row: ShipmentBatchVO) {
  dialogMode.value = 'update'
  currentRow.value = row
  Object.assign(formData, {
    procurementId: row.procurementId,
    shipmentQuantity: row.shipmentQuantity,
    factoryShipDate: row.factoryShipDate || '',
    actualShipDate: row.actualShipDate || '',
    status: row.status,
    remarks: row.remarks || '',
  })
  dialogVisible.value = true
}

async function onDelete(row: ShipmentBatchVO) {
  try {
    await ElMessageBox.confirm(
      t('shipmentBatch.message.deleteConfirm', { batchCode: row.batchCode }),
      t('shipmentBatch.message.deleteConfirmTitle'),
      { confirmButtonText: t('common.delete'), cancelButtonText: t('common.cancel'), type: 'warning' },
    )
  } catch { return }
  try {
    await shipmentBatchApi.delete(row.id)
    ElMessage.success(t('shipmentBatch.message.deleteSuccess'))
    loadData()
  } catch { /* handled by interceptor */ }
}

function onSelectionChange(selection: ShipmentBatchVO[]) {
  selectedRows.value = selection
}

async function onBatchDelete() {
  if (!selectedRows.value.length) return
  try {
    await ElMessageBox.confirm(
      t('common.batch.deleteConfirm', { n: selectedRows.value.length }),
      t('common.batch.deleteConfirmTitle'),
      { confirmButtonText: t('common.delete'), cancelButtonText: t('common.cancel'), type: 'warning' },
    )
  } catch { return }
  loading.value = true
  try {
    await Promise.all(selectedRows.value.map(r => shipmentBatchApi.delete(r.id)))
    ElMessage.success(t('common.batch.deleteSuccess', { n: selectedRows.value.length }))
    selectedRows.value = []
    loadData()
  } catch {
    ElMessage.error(t('common.batch.deleteFailed'))
  } finally {
    loading.value = false
  }
}

const defaultFormData = () => ({
  procurementId: procurementId.value ?? 0,
  shipmentQuantity: 1,
  factoryShipDate: '',
  actualShipDate: '',
  status: BATCH_STATUS_PENDING as ShipmentBatchStatus,
  remarks: '',
})

const formData = reactive(defaultFormData())

const formRules = {
  shipmentQuantity: [{ required: true, message: () => t('shipmentBatch.validation.quantityRequired'), trigger: 'blur' }],
}

const copyColumns: ExcelColDef[] = [
  { prop: 'productMasterCode', label: t('product.column.masterCode'), formatter: (row) => row.productMasterCode || '' },
  { prop: 'productSubCode', label: t('product.column.subCode'), formatter: (row) => row.productSubCode || '' },
  { prop: 'productImageUrl', label: t('product.column.image'), formatter: (row) => row.productImageUrl || '' },
  { prop: 'batchCode', label: t('shipmentBatch.column.batchCode') },
  { prop: 'shipmentQuantity', label: t('shipmentBatch.column.shipmentQuantity'), formatter: (row) => row.shipmentQuantity != null ? row.shipmentQuantity.toLocaleString() : '' },
  { prop: 'factoryShipDate', label: t('shipmentBatch.column.factoryShipDate'), formatter: (row) => row.factoryShipDate || '' },
  { prop: 'actualShipDate', label: t('shipmentBatch.column.actualShipDate'), formatter: (row) => row.actualShipDate || '' },
  { prop: 'status', label: t('shipmentBatch.column.status'), formatter: (row) => statusLabel(row.status) },
  { prop: 'remarks', label: t('shipmentBatch.column.remarks'), formatter: (row) => row.remarks || '' },
  { prop: 'createBy', label: t('shipmentBatch.column.createBy'), formatter: (row) => row.createBy || '' },
  { prop: 'createTime', label: t('shipmentBatch.column.createTime'), formatter: (row) => formatTime(row.createTime) },
  { prop: 'action', label: t('order.column.action'), excluded: true },
]

async function onSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    submitting.value = true
    try {
      if (dialogMode.value === 'create') {
        await shipmentBatchApi.create({
          procurementId: formData.procurementId,
          shipmentQuantity: formData.shipmentQuantity,
          factoryShipDate: formData.factoryShipDate || undefined,
          remarks: formData.remarks || undefined,
        })
        ElMessage.success(t('shipmentBatch.message.createSuccess'))
      } else if (currentRow.value) {
        await shipmentBatchApi.update(currentRow.value.id, {
          shipmentQuantity: formData.shipmentQuantity,
          factoryShipDate: formData.factoryShipDate || undefined,
          actualShipDate: formData.actualShipDate || undefined,
          status: formData.status,
          remarks: formData.remarks || undefined,
        })
        ElMessage.success(t('order.message.updateSuccess'))
      }
      dialogVisible.value = false
      loadData()
    } finally {
      submitting.value = false
    }
  })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.test-page { display: flex; flex-direction: column; gap: 16px; }
.table-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.filter-card :deep(.el-card__body) { padding-bottom: 0; }
.page-header { display: flex; align-items: center; gap: 12px; }
.page-title { font-size: 18px; font-weight: 700; color: var(--text-primary); }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
.batch-actions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.selection-count { margin-left: 4px; }
:deep(.el-table__header-wrapper),
:deep(.el-table__header th.el-table__cell) {
  position: sticky !important;
  top: 0 !important;
  z-index: 10 !important;
  background: inherit;
}
</style>
