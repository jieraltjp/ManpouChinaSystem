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
          <el-button type="primary" @click="onNew" :disabled="!procurementId">
            <el-icon><Plus /></el-icon>{{ $t('shipmentBatch.action.create') }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="tableRows" stripe style="width: 100%">
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
        <el-table-column :label="$t('order.column.action')" min-width="180" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="onEdit(row)">{{ $t('demand.action.edit') }}</el-button>
            <el-button link type="danger" size="small" @click="onDelete(row)"
              :disabled="!deletableStatuses.includes(row.status)">{{ $t('common.delete') }}</el-button>
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
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, ElMessageBox } from 'element-plus'
import { Plus, ArrowLeft } from '@element-plus/icons-vue'
import { shipmentBatchApi, type ShipmentBatchVO, type ShipmentBatchStatus } from '@/api/procurement'
import { useI18n } from 'vue-i18n'

const route = useRoute()
const router = useRouter()
const { t, locale: localeRef } = useI18n()

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
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
.filter-card :deep(.el-card__body) { padding-bottom: 0; }
.page-header { display: flex; align-items: center; gap: 12px; }
.page-title { font-size: 18px; font-weight: 700; color: var(--text-primary); }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
