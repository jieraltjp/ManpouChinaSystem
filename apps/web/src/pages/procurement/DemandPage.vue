<template>
  <div class="page">
    <div class="page-header">
      <h2 class="page-title">{{ $t('demand.title') }}</h2>
      <div class="header-actions">
        <el-button type="primary" @click="onNew">
          <el-icon><Plus /></el-icon> {{ $t('demand.newButton') }}
        </el-button>
      </div>
    </div>

    <!-- 统计 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#E8650A"><Clock /></el-icon></div>
            <div>
              <div class="stat-value">{{ pagination.total }}</div>
              <div class="stat-label">{{ $t('demand.stat.total') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#D97706"><Warning /></el-icon></div>
            <div>
              <div class="stat-value">{{ pendingCount }}</div>
              <div class="stat-label">{{ $t('demand.stat.pending') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#16A34A"><CircleCheck /></el-icon></div>
            <div>
              <div class="stat-value">{{ convertedCount }}</div>
              <div class="stat-label">{{ $t('demand.stat.converted') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filterForm">
        <el-form-item :label="$t('demand.filter.demandType')">
          <el-select v-model="filterForm.demandType" :placeholder="$t('demand.filter.all')" clearable style="width:140px">
            <el-option value="REPLENISHMENT" :label="$t('demand.type.replenishment')" />
            <el-option value="NEW_PURCHASE" :label="$t('demand.type.newPurchase')" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('demand.filter.productCode')">
          <el-input v-model="filterForm.productCode" :placeholder="$t('demand.dialog.productCodePlaceholder')" clearable style="width:140px" />
        </el-form-item>
        <el-form-item :label="$t('demand.filter.status')">
          <el-select v-model="filterForm.status" :placeholder="$t('demand.filter.all')" clearable style="width:140px">
            <el-option value="PENDING" :label="$t('demand.status.PENDING')" />
            <el-option value="CONVERTED" :label="$t('demand.status.CONVERTED')" />
            <el-option value="CANCELLED" :label="$t('demand.status.CANCELLED')" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">{{ $t('demand.filter.search') }}</el-button>
          <el-button @click="onReset">{{ $t('demand.filter.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="tableData" stripe style="width:100%">
        <el-table-column prop="demandCode" :label="$t('demand.column.demandCode')" width="160" />
        <el-table-column prop="demandType" :label="$t('demand.column.demandType')" width="130" align="center">
          <template #default="{ row }">
            <el-tag :type="row.demandType === 'NEW_PURCHASE' ? 'warning' : 'primary'" size="small">
              {{ row.demandType === 'NEW_PURCHASE' ? $t('demand.type.newPurchase') : $t('demand.type.replenishment') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="productCode" :label="$t('demand.column.productCode')" width="120">
          <template #default="{ row }">
            <span class="product-code">{{ row.productCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="subProductCode" :label="$t('demand.column.subProductCode')" width="100" />
        <el-table-column prop="quantity" :label="$t('demand.column.quantity')" width="80" align="right" />
        <el-table-column prop="destination" :label="$t('demand.column.destination')" min-width="100" show-overflow-tooltip />
        <el-table-column prop="japanLead" :label="$t('demand.column.japanLead')" width="100" />
        <el-table-column prop="status" :label="$t('demand.column.status')" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="demandStatusType(row.status)" size="small">
              {{ demandStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="$t('demand.column.createTime')" width="160">
          <template #default="{ row }">
            {{ row.createTime ? new Date(row.createTime).toLocaleString(currentLocale === 'ja' ? 'ja-JP' : 'zh-CN') : '-' }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('demand.column.action')" width="260" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" link type="primary" size="small" @click.stop="onConvert(row)">
              {{ $t('demand.action.convert') }}
            </el-button>
            <template v-if="row.status === 'CONVERTED'">
              <el-button link type="primary" size="small" @click.stop="onViewLinked(row)">
                {{ $t('demand.action.viewLinked') }}
              </el-button>
              <el-button link type="warning" size="small" @click.stop="onRevertConversion(row)">
                {{ $t('demand.action.revert') }}
              </el-button>
            </template>
            <el-button link type="primary" size="small" @click.stop="onEdit(row)">{{ $t('demand.action.edit') }}</el-button>
            <el-button v-if="row.status === 'PENDING'" link type="danger" size="small" @click.stop="onDelete(row)">
              {{ $t('demand.action.delete') }}
            </el-button>
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
    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? $t('demand.newDialogTitle') : $t('demand.editDialogTitle')" width="560px">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px">
        <el-form-item :label="$t('demand.dialog.demandType')" prop="demandType">
          <el-radio-group v-model="formData.demandType">
            <el-radio value="REPLENISHMENT">{{ $t('demand.type.replenishment') }}</el-radio>
            <el-radio value="NEW_PURCHASE">{{ $t('demand.type.newPurchase') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="$t('demand.dialog.productCode')" prop="productCode">
          <el-input v-model="formData.productCode" :placeholder="$t('demand.dialog.productCodePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('demand.dialog.subProductCode')">
          <el-input v-model="formData.subProductCode" :placeholder="$t('demand.dialog.subProductCodePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('demand.dialog.quantity')" prop="quantity">
          <el-input-number v-model="formData.quantity" :min="1" style="width:100%" />
        </el-form-item>
        <el-form-item :label="$t('demand.dialog.destination')">
          <el-input v-model="formData.destination" :placeholder="$t('demand.dialog.destinationPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('demand.dialog.japanLead')">
          <el-input v-model="formData.japanLead" :placeholder="$t('demand.dialog.japanLeadPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('demand.dialog.remarks')">
          <el-input v-model="formData.remarks" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('demand.dialog.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">{{ $t('demand.dialog.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, ElMessageBox } from 'element-plus'
import { Plus, Clock, Warning, CircleCheck } from '@element-plus/icons-vue'
import { demandApi, type DemandPageVO, type CreateDemandRequest, type UpdateDemandRequest } from '@/api/demand'
import { useI18n } from 'vue-i18n'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'update'>('create')
const currentRow = ref<DemandPageVO | null>(null)
const formRef = ref<FormInstance>()

const router = useRouter()
const { t, locale: localeRef } = useI18n()
const currentLocale = computed(() => localeRef.value)

const filterForm = reactive({ demandType: '', productCode: '', status: '' })
const pagination = reactive({ page: 1, pageSize: 20, total: 0 })
const tableData = ref<DemandPageVO[]>([])

const pendingCount = computed(() => tableData.value.filter(r => r.status === 'PENDING').length)
const convertedCount = computed(() => tableData.value.filter(r => r.status === 'CONVERTED').length)

const defaultFormData = (): CreateDemandRequest => ({
  demandType: 'REPLENISHMENT',
  productCode: '',
  subProductCode: '',
  quantity: 1,
  destination: '',
  japanLead: '',
  remarks: '',
})
const formData = reactive<CreateDemandRequest>(defaultFormData())

const formRules = {
  demandType: [{ required: true, message: () => t('demand.validation.demandTypeRequired'), trigger: 'change' }],
  productCode: [{ required: true, message: () => t('demand.validation.productCodeRequired'), trigger: 'blur' }],
  quantity: [{ required: true, message: () => t('demand.validation.quantityRequired'), trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    const res = await demandApi.list({
      page: pagination.page - 1,
      pageSize: pagination.pageSize,
      status: filterForm.status || undefined,
      demandType: filterForm.demandType || undefined,
      productCode: filterForm.productCode.trim() || undefined,
    })
    const payload = res.data.data as { content: DemandPageVO[]; totalElements: number }
    tableData.value = payload.content || []
    pagination.total = payload.totalElements || 0
  } catch { /* interceptor */ } finally {
    loading.value = false
  }
}

function onReset() {
  filterForm.demandType = ''
  filterForm.productCode = ''
  filterForm.status = ''
  pagination.page = 1
  loadData()
}

function onNew() {
  dialogMode.value = 'create'
  Object.assign(formData, defaultFormData())
  dialogVisible.value = true
}

function onEdit(row: DemandPageVO) {
  dialogMode.value = 'update'
  currentRow.value = row
  Object.assign(formData, {
    demandType: row.demandType,
    productCode: row.productCode,
    subProductCode: row.subProductCode || '',
    quantity: row.quantity,
    destination: row.destination || '',
    japanLead: row.japanLead || '',
    remarks: row.remarks || '',
  })
  dialogVisible.value = true
}

function onConvert(row: DemandPageVO) {
  router.push({
    path: '/procurement/order',
    query: {
      demandId: String(row.id),
      productCode: row.productCode,
      subProductCode: row.subProductCode || '',
      destination: row.destination || '',
      japanLead: row.japanLead || '',
      quantity: String(row.quantity),
    },
  })
}

function onViewLinked(row: DemandPageVO) {
  router.push({ path: '/procurement/order', query: { procurementId: String(row.linkedProcurementId) } })
}

async function onRevertConversion(row: DemandPageVO) {
  try {
    await ElMessageBox.confirm(
      t('demand.message.revertConfirm'),
      t('demand.message.revertConfirmTitle'),
      { confirmButtonText: t('common.confirm'), cancelButtonText: t('common.cancel'), type: 'warning' }
    )
  } catch { return }
  try {
    await demandApi.revertConversion(row.id)
    ElMessage.success(t('demand.message.revertSuccess'))
    loadData()
  } catch { /* interceptor */ }
}

async function onDelete(row: DemandPageVO) {
  try {
    await ElMessageBox.confirm(
      t('demand.message.deleteConfirm', { code: row.demandCode }),
      t('demand.message.deleteConfirmTitle'),
      { type: 'warning' },
    )
  } catch { return }
  try {
    await demandApi.delete(row.id)
    ElMessage.success(t('demand.message.deleteSuccess'))
    loadData()
  } catch { /* interceptor */ }
}

async function onSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    submitting.value = true
    try {
      if (dialogMode.value === 'create') {
        await demandApi.create(formData as CreateDemandRequest)
        ElMessage.success(t('demand.message.createSuccess'))
      } else if (currentRow.value) {
        await demandApi.update(currentRow.value.id, formData as UpdateDemandRequest)
        ElMessage.success(t('demand.message.updateSuccess'))
      }
      dialogVisible.value = false
      loadData()
    } finally {
      submitting.value = false
    }
  })
}

function demandStatusLabel(status: string): string {
  return { PENDING: t('demand.status.PENDING'), CONVERTED: t('demand.status.CONVERTED'), CANCELLED: t('demand.status.CANCELLED') }[status] ?? status
}

function demandStatusType(status: string): string {
  return { PENDING: 'warning', CONVERTED: 'success', CANCELLED: 'info' }[status] ?? 'info'
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
