<template>
  <div class="page">
    <div class="page-header">
      <div class="header-actions">
        <el-button type="primary" @click="onNew">
          <el-icon><Plus /></el-icon> {{ $t('taxRefund.newButton') }}
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#6366F1"><Document /></el-icon></div>
            <div><div class="stat-value">{{ pagination.total }}</div><div class="stat-label">{{ $t('taxRefund.stat.total') }}</div></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#F59E0B"><Clock /></el-icon></div>
            <div><div class="stat-value">{{ statusCount.APPLYING }}</div><div class="stat-label">{{ $t('taxRefund.stat.applying') }}</div></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#16A34A"><CircleCheck /></el-icon></div>
            <div><div class="stat-value">{{ statusCount.COMPLETED }}</div><div class="stat-label">{{ $t('taxRefund.stat.completed') }}</div></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#DC2626"><Close /></el-icon></div>
            <div><div class="stat-value">{{ statusCount.NO_REFUND }}</div><div class="stat-label">{{ $t('taxRefund.stat.noRefund') }}</div></div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filterForm">
        <el-form-item :label="$t('taxRefund.filter.procurementId')">
          <el-input-number v-model="filterForm.procurementId" :min="1" style="width:130px" clearable />
        </el-form-item>
        <el-form-item :label="$t('taxRefund.filter.status')">
          <el-select v-model="filterForm.status" :placeholder="$t('common.all')" clearable style="width:140px">
            <el-option value="APPLYING" :label="$t('taxRefund.status.applying')" />
            <el-option value="COMPLETED" :label="$t('taxRefund.status.completed')" />
            <el-option value="NO_REFUND" :label="$t('taxRefund.status.noRefund')" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearchFromButton">{{ $t('common.search') }}</el-button>
          <el-button @click="onReset">{{ $t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表 -->
    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="tableData" stripe style="width:100%" min-height="200">
        <el-table-column prop="refundCode" :label="$t('taxRefund.column.refundCode')" min-width="180">
          <template #default="{ row }">
            <span class="code-badge">{{ row.refundCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="procurementId" :label="$t('taxRefund.column.procurementId')" min-width="100" align="center">
          <template #default="{ row }">
            <span v-if="row.procurementId">{{ row.procurementId }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('taxRefund.column.billingType')" min-width="130" align="center">
          <template #default="{ row }">
            {{ row.billingType ? $t('taxRefund.enum.billingType.' + row.billingType) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="taxPoint" :label="$t('taxRefund.column.taxPoint')" min-width="90" align="right">
          <template #default="{ row }">
            <span v-if="row.taxPoint !== null">{{ (Number(row.taxPoint) * 100).toFixed(1) }}%</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="estimatedRefundRmb" :label="$t('taxRefund.column.estimatedRefundRmb')" min-width="140" align="right">
          <template #default="{ row }">
            <span v-if="row.estimatedRefundRmb !== null" class="money">{{ Number(row.estimatedRefundRmb).toLocaleString() }} {{ $t('common.currency.cny') }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="actualRefundRmb" :label="$t('taxRefund.column.actualRefundRmb')" min-width="140" align="right">
          <template #default="{ row }">
            <span v-if="row.actualRefundRmb !== null" class="money-success">{{ Number(row.actualRefundRmb).toLocaleString() }} {{ $t('common.currency.cny') }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="refundDate" :label="$t('taxRefund.column.refundDate')" min-width="120" />
        <el-table-column prop="refundBank" :label="$t('taxRefund.column.refundBank')" min-width="140" show-overflow-tooltip />
        <el-table-column prop="status" :label="$t('taxRefund.column.status')" min-width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('taxRefund.column.action')" min-width="240" align="center">
          <template #default="{ row }">
            <el-button link class="btn-blue" size="small" @click.stop="onView(row)">{{ $t('taxRefund.action.detail') }}</el-button>
            <el-button link type="warning" size="small" @click.stop="onEdit(row)">{{ $t('taxRefund.action.edit') }}</el-button>
            <el-button link type="danger" size="small" @click.stop="onDelete(row)">{{ $t('taxRefund.action.delete') }}</el-button>
            <template v-if="row.status === 'APPLYING'">
              <el-button link type="success" size="small" :loading="actionLoading === row.id + '-complete'" @click.stop="onComplete(row)">{{ $t('taxRefund.action.complete') }}</el-button>
              <el-button link type="warning" size="small" :loading="actionLoading === row.id + '-no-refund'" @click.stop="onNoRefund(row)">{{ $t('taxRefund.action.noRefund') }}</el-button>
            </template>
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
          @size-change="onSearch"
          @current-change="onSearch"
        />
      </div>
    </el-card>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" :title="$t('taxRefund.drawerTitle')" size="600px" direction="rtl">
      <el-descriptions :column="2" border v-if="currentRow">
        <el-descriptions-item :label="$t('taxRefund.column.refundCode')">
          <span class="code-badge">{{ currentRow.refundCode }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('taxRefund.column.status')">
          <el-tag :type="statusTagType(currentRow.status)" size="small">{{ statusLabel(currentRow.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('taxRefund.column.procurementId')">{{ currentRow.procurementId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('taxRefund.column.japanCustomsId')">{{ currentRow.japanCustomsId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('taxRefund.column.billingType')">{{ currentRow.billingType ? $t('taxRefund.enum.billingType.' + currentRow.billingType) : '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('taxRefund.column.taxPoint')">
          <span v-if="currentRow.taxPoint !== null">{{ (Number(currentRow.taxPoint) * 100).toFixed(1) }}%</span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('taxRefund.column.priceRmb')">
          <span v-if="currentRow.priceRmb !== null">{{ Number(currentRow.priceRmb).toLocaleString() }} {{ $t('common.currency.cny') }}</span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('taxRefund.column.quantity')">{{ currentRow.quantity ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('taxRefund.column.estimatedRefundRmb')">
          <span v-if="currentRow.estimatedRefundRmb !== null" class="money">{{ Number(currentRow.estimatedRefundRmb).toLocaleString() }} {{ $t('common.currency.cny') }}</span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('taxRefund.column.actualRefundRmb')">
          <span v-if="currentRow.actualRefundRmb !== null" class="money-success">{{ Number(currentRow.actualRefundRmb).toLocaleString() }} {{ $t('common.currency.cny') }}</span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('taxRefund.column.refundDate')">{{ currentRow.refundDate ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('taxRefund.column.refundBank')">{{ currentRow.refundBank ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('taxRefund.column.createBy')">{{ currentRow.createBy || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('taxRefund.column.createTime')">{{ currentRow.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('taxRefund.column.remarks')" :span="2">{{ currentRow.remarks || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <!-- 新建/编辑弹窗 -->
    <el-dialog
      v-model="formDialogVisible"
      :title="dialogMode === 'create' ? $t('taxRefund.newDialogTitle') : $t('taxRefund.editDialogTitle')"
      width="600px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="dialogForm" :rules="dialogRules" label-width="130px">
        <!-- 关联采购单 -->
        <el-form-item :label="$t('taxRefund.dialog.procurement')" prop="procurementId">
          <el-select
            v-model="dialogForm.procurementId"
            filterable
            remote
            reserve-keyword
            :remote-method="searchProcurement"
            :loading="procurementLoading"
            :placeholder="$t('taxRefund.dialog.procurementPlaceholder')"
            style="width:100%"
            @change="onProcurementSelected"
          >
            <el-option
              v-for="p in procurementOptions"
              :key="p.id"
              :label="`${p.productCode}${p.subProductCode ? '-' + p.subProductCode : ''} / ${p.factoryName || ''} / ${p.orderDate || ''}`"
              :value="p.id"
            />
          </el-select>
        </el-form-item>

        <!-- 日本清关记录（选填） -->
        <el-form-item :label="$t('taxRefund.dialog.japanCustomsId')">
          <el-input-number
            v-model="dialogForm.japanCustomsId"
            :min="1"
            :placeholder="$t('taxRefund.dialog.japanCustomsIdPlaceholder')"
            style="width:100%"
            clearable
          />
        </el-form-item>

        <el-divider />

        <!-- 采购信息（自动代入，只读展示） -->
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item :label="$t('taxRefund.dialog.billingType')">
              <el-input :model-value="billingTypeLabel(dialogForm.billingType)" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('taxRefund.dialog.priceRmb')">
              <el-input :model-value="dialogForm.priceRmb != null ? dialogForm.priceRmb : '-'" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('taxRefund.dialog.taxPoint')">
              <el-input :model-value="dialogForm.taxPoint != null ? (Number(dialogForm.taxPoint) * 100).toFixed(1) + '%' : '-'" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item :label="$t('taxRefund.dialog.quantity')">
              <el-input :model-value="dialogForm.quantity != null ? dialogForm.quantity : '-'" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('taxRefund.dialog.exchangeRate')">
              <el-input :model-value="dialogForm.exchangeRate != null ? dialogForm.exchangeRate : '-'" disabled />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 理论退税额预览 -->
        <el-form-item :label="$t('taxRefund.dialog.estimatedRefundPreview')">
          <div class="refund-preview">
            <span class="refund-value">{{ estimatedRefundPreview }}</span>
            <span class="refund-unit">{{ $t('common.currency.cny') }}</span>
            <span class="refund-formula">{{ $t('taxRefund.dialog.estimatedRefundHint') }}</span>
          </div>
        </el-form-item>

        <!-- 备注 -->
        <el-form-item :label="$t('taxRefund.dialog.remarks')">
          <el-input
            v-model="dialogForm.remarks"
            type="textarea"
            :rows="2"
            :placeholder="$t('taxRefund.dialog.remarksPlaceholder')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="formSubmitting" @click="onFormSubmit">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 完成退税弹窗 -->
    <el-dialog v-model="completeDialogVisible" :title="$t('taxRefund.completeDialogTitle')" width="480px">
      <el-form :model="completeForm" label-width="150px">
        <el-form-item :label="$t('taxRefund.completeDialog.actualRefundRmb')" required>
          <el-input-number v-model="completeForm.actualRefundRmb" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item :label="$t('taxRefund.completeDialog.refundDate')" required>
          <el-date-picker v-model="completeForm.refundDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item :label="$t('taxRefund.completeDialog.refundBank')">
          <el-input v-model="completeForm.refundBank" :placeholder="$t('taxRefund.completeDialog.refundBankPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeDialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="success" :loading="actionLoading.startsWith('complete-')" @click="onCompleteConfirm">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { ElMessage, type FormInstance, ElMessageBox } from 'element-plus'
import { Document, Clock, CircleCheck, Close, Plus } from '@element-plus/icons-vue'
import { taxRefundApi, type TaxRefundVO, type TaxRefundStatus, type TaxRefundCreateRequest } from '@/api/taxRefund'
import { procurementApi, type ProcurementPageVO } from '@/api/procurement'
import { useI18n } from 'vue-i18n'

const loading = ref(false)
const drawerVisible = ref(false)
const completeDialogVisible = ref(false)
const actionLoading = ref('')

const currentRow = ref<TaxRefundVO | null>(null)
const filterForm = reactive({
  procurementId: undefined as number | undefined,
  status: '' as TaxRefundStatus | '',
})
const pagination = reactive({ page: 1, pageSize: 20, total: 0 })
const tableData = ref<TaxRefundVO[]>([])
const completeForm = reactive({
  actualRefundRmb: 0,
  refundDate: '',
  refundBank: '',
})
const completingRowId = ref<number | null>(null)

// 新建/编辑弹窗
const formDialogVisible = ref(false)
const dialogMode = ref<'create' | 'update'>('create')
const formRef = ref<FormInstance>()
const formSubmitting = ref(false)
const currentFormRow = ref<TaxRefundVO | null>(null)

const dialogForm = reactive({
  procurementId: undefined as number | undefined,
  japanCustomsId: undefined as number | undefined,
  billingType: undefined as string | undefined,
  priceRmb: undefined as number | undefined,
  quantity: undefined as number | undefined,
  taxPoint: undefined as number | undefined,
  exchangeRate: undefined as number | undefined,
  remarks: '',
})

const dialogRules = {
  procurementId: [{ required: true, message: () => t('taxRefund.validation.procurementRequired'), trigger: 'change' }],
}

const procurementOptions = ref<ProcurementPageVO[]>([])
const procurementLoading = ref(false)

const estimatedRefundPreview = computed(() => {
  const { priceRmb, quantity, taxPoint } = dialogForm
  if (priceRmb == null || quantity == null || taxPoint == null) return '—'
  const refund = priceRmb * quantity * (taxPoint - 1)
  return isNaN(refund) || !isFinite(refund) ? '—' : Math.round(refund * 100) / 100
})

async function searchProcurement(query: string) {
  if (!query) { procurementOptions.value = []; return }
  procurementLoading.value = true
  try {
    const res = await procurementApi.list({ page: 0, pageSize: 20, productCode: query.trim() || undefined })
    procurementOptions.value = res.data.data?.content ?? []
  } catch { procurementOptions.value = [] }
  finally { procurementLoading.value = false }
}

function onProcurementSelected(id: number) {
  const p = procurementOptions.value.find(p => p.id === id)
  if (!p) return
  dialogForm.billingType = p.billingType ?? undefined
  dialogForm.priceRmb = p.priceRmb ?? undefined
  dialogForm.quantity = p.quantity ?? undefined
  dialogForm.taxPoint = p.taxPoint ?? undefined
  dialogForm.exchangeRate = p.exchangeRate ?? undefined
}

function onNew() {
  dialogMode.value = 'create'
  currentFormRow.value = null
  Object.assign(dialogForm, {
    procurementId: undefined,
    japanCustomsId: undefined,
    billingType: undefined,
    priceRmb: undefined,
    quantity: undefined,
    taxPoint: undefined,
    exchangeRate: undefined,
    remarks: '',
  })
  procurementOptions.value = []
  formDialogVisible.value = true
}

function onEdit(row: TaxRefundVO) {
  dialogMode.value = 'update'
  currentFormRow.value = row
  Object.assign(dialogForm, {
    procurementId: row.procurementId ?? undefined,
    japanCustomsId: row.japanCustomsId ?? undefined,
    billingType: row.billingType ?? undefined,
    priceRmb: row.priceRmb ?? undefined,
    quantity: row.quantity ?? undefined,
    taxPoint: row.taxPoint ?? undefined,
    exchangeRate: row.exchangeRate ?? undefined,
    remarks: row.remarks || '',
  })
  // 如果有 procurementId，加载其信息用于显示
  if (row.procurementId) {
    procurementOptions.value = [{
      id: row.procurementId,
      productCode: '',
      subProductCode: '',
      factoryName: '',
      orderDate: '',
      billingType: row.billingType ?? undefined,
      priceRmb: row.priceRmb ?? undefined,
      quantity: row.quantity ?? undefined,
      taxPoint: row.taxPoint ?? undefined,
      exchangeRate: row.exchangeRate ?? undefined,
    }] as ProcurementPageVO[]
  }
  formDialogVisible.value = true
}

async function onFormSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  formSubmitting.value = true
  try {
    const payload: TaxRefundCreateRequest = {
      procurementId: dialogForm.procurementId ?? undefined,
      japanCustomsId: dialogForm.japanCustomsId,
      billingType: dialogForm.billingType,
      priceRmb: dialogForm.priceRmb,
      quantity: dialogForm.quantity,
      taxPoint: dialogForm.taxPoint,
      exchangeRate: dialogForm.exchangeRate,
      remarks: dialogForm.remarks || undefined,
    }
    if (dialogMode.value === 'create') {
      await taxRefundApi.create(payload)
      ElMessage.success(t('taxRefund.message.createSuccess'))
    } else if (currentFormRow.value) {
      // 使用 update 接口（或复用 create 逻辑，视后端实现而定）
      await taxRefundApi.create({ ...payload, procurementId: currentFormRow.value.procurementId ?? undefined })
      ElMessage.success(t('taxRefund.message.updateSuccess'))
    }
    formDialogVisible.value = false
    loadData()
  } catch (e) {
    console.error('[TaxRefundRecordPage] submit failed', e)
    ElMessage.error(t('taxRefund.message.actionFailed'))
  } finally {
    formSubmitting.value = false
  }
}

async function onDelete(row: TaxRefundVO) {
  try {
    await ElMessageBox.confirm(
      t('taxRefund.message.deleteConfirm', { code: row.refundCode }),
      t('taxRefund.message.deleteConfirmTitle'),
      { confirmButtonText: t('common.delete'), cancelButtonText: t('common.cancel'), type: 'warning' },
    )
  } catch { return }
  try {
    await taxRefundApi.delete(row.id)
    ElMessage.success(t('taxRefund.message.deleteSuccess'))
    loadData()
  } catch {
    ElMessage.error(t('taxRefund.message.actionFailed'))
  }
}

function billingTypeLabel(val?: string | null): string {
  if (!val) return '-'
  return t('taxRefund.enum.billingType.' + val) ?? val
}

const { t } = useI18n()

const statusCount = computed(() => {
  const counts: Record<string, number> = { APPLYING: 0, COMPLETED: 0, NO_REFUND: 0 }
  tableData.value.forEach(r => { if (r.status in counts) counts[r.status]++ })
  return counts
})

function statusLabel(status?: string): string {
  const map: Record<string, string> = {
    APPLYING: t('taxRefund.status.applying'),
    COMPLETED: t('taxRefund.status.completed'),
    NO_REFUND: t('taxRefund.status.noRefund'),
  }
  return map[status ?? ''] ?? status ?? '-'
}

function statusTagType(status?: string): string {
  const map: Record<string, string> = {
    APPLYING: 'warning',
    COMPLETED: 'success',
    NO_REFUND: 'info',
  }
  return map[status ?? ''] ?? 'info'
}

async function loadData() {
  loading.value = true
  try {
    const res = await taxRefundApi.list({
      page: pagination.page - 1,
      pageSize: pagination.pageSize,
      status: filterForm.status || undefined,
      procurementId: filterForm.procurementId,
    })
    const data = res.data.data
    tableData.value = data?.content ?? []
    pagination.total = data?.totalElements ?? 0
  } catch (e: unknown) {
    console.error('[TaxRefundRecordPage] loadData failed', e)
    ElMessage.error(t('taxRefund.message.loadFailed'))
  } finally {
    loading.value = false
  }
}

function onSearch() { loadData() }
function onSearchFromButton() { pagination.page = 1; loadData() }

function onReset() {
  filterForm.procurementId = undefined
  filterForm.status = ''
  pagination.page = 1
  loadData()
}

function onView(row: TaxRefundVO) {
  currentRow.value = row
  drawerVisible.value = true
}

function onComplete(row: TaxRefundVO) {
  completingRowId.value = row.id
  completeForm.actualRefundRmb = Number(row.estimatedRefundRmb) || 0
  completeForm.refundDate = ''
  completeForm.refundBank = ''
  completeDialogVisible.value = true
}

async function onCompleteConfirm() {
  if (!completingRowId.value) return
  if (!completeForm.refundDate) {
    ElMessage.error(t('taxRefund.message.refundDateRequired'))
    return
  }
  actionLoading.value = `complete-${completingRowId.value}`
  try {
    await taxRefundApi.complete(completingRowId.value, {
      actualRefundRmb: completeForm.actualRefundRmb,
      refundDate: completeForm.refundDate,
      refundBank: completeForm.refundBank || undefined,
    })
    ElMessage.success(t('taxRefund.message.completeSuccess'))
    completeDialogVisible.value = false
    loadData()
  } catch (e) {
    console.error('[TaxRefundRecordPage] complete failed', e)
    ElMessage.error(t('taxRefund.message.actionFailed'))
  } finally {
    actionLoading.value = ''
    completingRowId.value = null
  }
}

async function onNoRefund(row: TaxRefundVO) {
  actionLoading.value = `${row.id}-no-refund`
  try {
    await taxRefundApi.markNoRefund(row.id)
    ElMessage.success(t('taxRefund.message.noRefundSuccess'))
    loadData()
  } catch (e) {
    console.error('[TaxRefundRecordPage] noRefund failed', e)
    ElMessage.error(t('taxRefund.message.actionFailed'))
  } finally {
    actionLoading.value = ''
  }
}

onMounted(() => loadData())

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
.page-header { display: flex; align-items: center; justify-content: space-between; }
.page-title { margin: 0; font-size: 20px; font-weight: 700; color: var(--text-primary); }
.page-title::before { content: ''; display: inline-block; width: 4px; height: 20px; background: var(--color-primary); border-radius: 2px; margin-right: 10px; vertical-align: middle; }
.header-actions { display: flex; gap: 8px; }
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
.code-badge { color: var(--color-primary); font-family: monospace; font-size: 12px; font-weight: 700; background: var(--color-primary-pale); padding: 3px 9px; border-radius: 5px; }
.money { color: #F59E0B; font-weight: 600; }
.money-success { color: #16A34A; font-weight: 600; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
.refund-preview {
  display: flex;
  align-items: baseline;
  gap: 6px;
  background: #fef3c7;
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  border: 1px solid rgba(245,158,11,0.2);
}
.refund-value { font-size: 22px; font-weight: 800; color: #D97706; font-variant-numeric: tabular-nums; }
.refund-unit { font-size: 13px; color: #92400e; font-weight: 600; }
.refund-formula { font-size: 11px; color: #92400e; margin-left: 4px; }
</style>
