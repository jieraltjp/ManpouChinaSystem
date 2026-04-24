<template>
  <div class="page">
    <div class="page-header">
      <h2 class="page-title">{{ $t('customs.title') }}</h2>
      <div class="header-actions">
        <el-button type="primary" @click="onNew">
          <el-icon><Plus /></el-icon> {{ $t('customs.newButton') }}
        </el-button>
      </div>
    </div>

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
        <el-form-item :label="$t('customs.filter.procurementId')">
          <el-input-number v-model="filterForm.procurementId" :placeholder="$t('customs.filter.procurementIdPlaceholder')" :min="1" style="width:130px" clearable />
        </el-form-item>
        <el-form-item :label="$t('customs.filter.status')">
          <el-select v-model="filterForm.status" :placeholder="$t('common.all')" clearable style="width:130px">
            <el-option value="PENDING" :label="$t('customs.status.pending')" />
            <el-option value="SUBMITTED" :label="$t('customs.status.submitted')" />
            <el-option value="CLEARED" :label="$t('customs.status.cleared')" />
            <el-option value="REJECTED" :label="$t('customs.status.rejected')" />
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
      <el-table v-loading="loading" :data="tableData" stripe style="width:100%">
        <el-table-column prop="customsCode" :label="$t('customs.column.customsCode')" width="180">
          <template #default="{ row }">
            <span class="code-badge">{{ row.customsCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="procurementId" :label="$t('customs.column.procurementId')" width="110" align="center">
          <template #default="{ row }">
            <span v-if="row.procurementId">{{ row.procurementId }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="productCode" :label="$t('customs.column.productCode')" width="140">
          <template #default="{ row }">
            <span class="product-code">{{ row.productCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="subProductCode" :label="$t('customs.column.subProductCode')" width="100" />
        <el-table-column prop="quantity" :label="$t('customs.column.quantity')" width="80" align="right">
          <template #default="{ row }">
            <span v-if="row.quantity !== null">{{ row.quantity }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="estimatedValueCny" :label="$t('customs.column.estimatedValueCny')" width="130" align="right">
          <template #default="{ row }">
            <span v-if="row.estimatedValueCny !== null" class="money">{{ $t('common.currency.cny') }}{{ row.estimatedValueCny.toLocaleString(undefined, { minimumFractionDigits: 2 }) }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" :label="$t('customs.column.status')" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="$t('customs.column.createTime')" width="160" />
        <el-table-column :label="$t('customs.column.action')" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="onView(row)">{{ $t('customs.action.detail') }}</el-button>
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

    <!-- 新规创建弹窗 -->
    <el-dialog v-model="dialogVisible" :title="$t('customs.newDialogTitle')" width="640px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="110px">
        <el-form-item :label="$t('customs.dialog.procurementId')" prop="procurementId">
          <el-input-number v-model="form.procurementId" :min="1" :placeholder="$t('customs.dialog.procurementIdPlaceholder')" style="width:100%" />
        </el-form-item>
        <el-form-item :label="$t('customs.dialog.productCode')" prop="productCode">
          <el-input v-model="form.productCode" :placeholder="$t('customs.dialog.productCodePlaceholder')" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('customs.dialog.subProductCode')">
              <el-input v-model="form.subProductCode" :placeholder="$t('customs.dialog.subProductCodePlaceholder')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('customs.dialog.quantity')">
              <el-input-number v-model="form.quantity" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('customs.dialog.estimatedValueCny')">
              <el-input-number v-model="form.estimatedValueCny" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('customs.dialog.factoryId')">
              <el-input-number v-model="form.factoryId" :min="1" :placeholder="$t('customs.dialog.factoryIdPlaceholder')" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="$t('customs.dialog.remarks')">
          <el-input v-model="form.remarks" type="textarea" :rows="3" :placeholder="$t('customs.dialog.remarksPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmitForm">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" :title="$t('customs.drawerTitle')" size="560px" direction="rtl">
      <el-descriptions :column="2" border v-if="currentRow">
        <el-descriptions-item :label="$t('customs.column.customsCode')">
          <span class="code-badge">{{ currentRow.customsCode }}</span>
        </el-descriptions-item>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Document, Clock, Top, CircleCheck } from '@element-plus/icons-vue'
import { customsApi, type CustomsVO, type DomesticCustomsStatus, type CustomsCreateRequest } from '@/api/customs'
import { useI18n } from 'vue-i18n'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const drawerVisible = ref(false)
const rejectDialogVisible = ref(false)
const actionLoading = ref('')
const rejectReason = ref('')
const rejectingRowId = ref<number | null>(null)

const currentRow = ref<CustomsVO | null>(null)
const filterForm = reactive({
  keyword: '',
  procurementId: undefined as number | undefined,
  status: '' as DomesticCustomsStatus | '',
})
const pagination = reactive({ page: 1, pageSize: 20, total: 0 })
const tableData = ref<CustomsVO[]>([])

const formRef = ref<FormInstance>()
const { t } = useI18n()

const form = reactive<CustomsCreateRequest>({
  procurementId: undefined,
  factoryId: undefined,
  productCode: '',
  subProductCode: undefined,
  quantity: undefined,
  estimatedValueCny: undefined,
  remarks: undefined,
})

const formRules: FormRules = {
  productCode: [{ required: true, message: () => t('customs.validation.productCodeRequired'), trigger: 'blur' }],
}

const statusCount = computed(() => {
  const counts: Record<string, number> = { PENDING: 0, SUBMITTED: 0, CLEARED: 0, REJECTED: 0 }
  tableData.value.forEach(r => { if (r.status in counts) counts[r.status]++ })
  return counts
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
      procurementId: filterForm.procurementId,
      status: filterForm.status || undefined,
    })
    const data = res.data.data
    tableData.value = data.content
    pagination.total = data.totalElements
  } catch (e: unknown) {
    console.error('[CustomsPage] loadData failed', e)
    ElMessage.error(t('customs.message.loadFailed'))
  } finally {
    loading.value = false
  }
}

function onSearch() { loadData() }
function onSearchFromButton() { pagination.page = 1; loadData() }

function onReset() {
  filterForm.keyword = ''
  filterForm.procurementId = undefined
  filterForm.status = ''
  pagination.page = 1
  loadData()
}

function onNew() {
  formRef.value?.resetFields()
  Object.assign(form, {
    procurementId: undefined,
    factoryId: undefined,
    productCode: '',
    subProductCode: undefined,
    quantity: undefined,
    estimatedValueCny: undefined,
    remarks: undefined,
  })
  dialogVisible.value = true
}

async function onSubmitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await customsApi.create(form)
    ElMessage.success(t('customs.message.createSuccess'))
    dialogVisible.value = false
    loadData()
  } catch (e) {
    console.error('[CustomsPage] onSubmitForm failed', e)
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
    console.error('[CustomsPage] submit failed', e)
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
    console.error('[CustomsPage] clear failed', e)
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
    console.error('[CustomsPage] reject failed', e)
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
    console.error('[CustomsPage] delete failed', e)
    ElMessage.error(t('customs.message.actionFailed'))
  } finally {
    actionLoading.value = ''
  }
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
.code-badge { color: var(--color-primary); font-family: monospace; font-size: 12px; font-weight: 700; background: var(--color-primary-pale); padding: 3px 9px; border-radius: 5px; }
.product-code { color: var(--color-primary); font-family: monospace; font-size: 12px; font-weight: 700; background: var(--color-primary-pale); padding: 3px 9px; border-radius: 5px; }
.money { color: #16A34A; font-weight: 600; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
