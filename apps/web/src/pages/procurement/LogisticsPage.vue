<template>
  <div class="page">
    <div class="page-header">
      <h2 class="page-title">{{ $t('logistics.title') }}</h2>
      <div class="header-actions">
        <el-button type="primary" @click="onNew">
          <el-icon><Plus /></el-icon> {{ $t('logistics.newButton') }}
        </el-button>
      </div>
    </div>

    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#E8650A"><Van /></el-icon></div>
            <div><div class="stat-value">{{ pagination.total }}</div><div class="stat-label">{{ $t('logistics.stat.total') }}</div></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#1E40AF"><Top /></el-icon></div>
            <div><div class="stat-value">{{ bookedCount }}</div><div class="stat-label">{{ $t('logistics.stat.booked') }}</div></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="icon-transit" color="#7C3AED"><Loading /></el-icon></div>
            <div><div class="stat-value">{{ transitCount }}</div><div class="stat-label">{{ $t('logistics.stat.inTransit') }}</div></div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filterForm">
        <el-form-item :label="$t('logistics.filter.planType')">
          <el-select v-model="filterForm.planType" :placeholder="$t('logistics.filter.all')" clearable style="width:140px">
            <el-option value="SEA" :label="$t('logistics.planType.SEA')" />
            <el-option value="AIR" :label="$t('logistics.planType.AIR')" />
            <el-option value="CONSOLIDATION" :label="$t('logistics.planType.CONSOLIDATION')" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('logistics.filter.status')">
          <el-select v-model="filterForm.status" :placeholder="$t('logistics.filter.all')" clearable style="width:140px">
            <el-option value="PLANNED" :label="$t('logistics.status.PLANNED')" />
            <el-option value="BOOKED" :label="$t('logistics.status.BOOKED')" />
            <el-option value="IN_TRANSIT" :label="$t('logistics.status.IN_TRANSIT')" />
            <el-option value="DELIVERED" :label="$t('logistics.status.DELIVERED')" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearchFromButton">{{ $t('logistics.filter.search') }}</el-button>
          <el-button @click="onReset">{{ $t('logistics.filter.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="tableData" stripe style="width:100%" min-height="200">
        <el-table-column prop="planCode" :label="$t('logistics.column.planCode')" min-width="160" />
        <el-table-column prop="factoryName" :label="$t('logistics.column.factoryName')" min-width="140" show-overflow-tooltip />
        <el-table-column prop="productCode" :label="$t('logistics.column.productCode')" min-width="120">
          <template #default="{ row }">
            <span class="product-code">{{ row.productCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="planType" :label="$t('logistics.column.planType')" min-width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="planTypeTag(row.planType)" size="small">{{ planTypeLabel(row.planType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="cargoWeightKg" :label="$t('logistics.column.cargoWeightKg')" min-width="100" align="right">
          <template #default="{ row }">
            {{ row.cargoWeightKg ? row.cargoWeightKg + $t('common.units.kg') : '' }}
          </template>
        </el-table-column>
        <el-table-column prop="cargoVolumeCbm" :label="$t('logistics.column.cargoVolumeCbm')" min-width="90" align="right">
          <template #default="{ row }">
            {{ row.cargoVolumeCbm ? row.cargoVolumeCbm.toFixed(4) + $t('common.units.m3') : '' }}
          </template>
        </el-table-column>
        <el-table-column prop="requiresQc" :label="$t('logistics.column.requiresQc')" min-width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.requiresQc ? 'warning' : 'success'" size="small">
              {{ row.requiresQc ? $t('logistics.requiresQc.yes') : $t('logistics.requiresQc.no') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" :label="$t('logistics.column.status')" min-width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="logisticsStatusType(row.status)" size="small">
              {{ logisticsStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('logistics.column.action')" min-width="160" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="onView(row)">{{ $t('logistics.action.detail') }}</el-button>
            <el-button v-if="row.procurementId" link type="warning" size="small" @click.stop="onOverview(row)">{{ $t('orderOverview.action.view') }}</el-button>
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

    <!-- 新增调配弹窗 -->
    <el-dialog v-model="dialogVisible" :title="$t('logistics.dialog.newTitle')" width="640px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item :label="$t('logistics.dialog.procurement')" prop="procurementId">
          <el-select
            v-model="form.procurementId"
            :placeholder="$t('logistics.dialog.procurementPlaceholder')"
            filterable
            remote
            :remote-method="searchProcurement"
            :loading="procurementLoading"
            style="width:100%"
            @change="onProcurementSelected"
          >
            <el-option
              v-for="p in procurementList"
              :key="p.id"
              :label="`${p.productCode}${p.subProductCode ? '-' + p.subProductCode : ''} / ${p.customerCompany || ''} / ${p.orderDate || ''}`"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-divider content-position="left"><span class="divider-label">{{ $t('logistics.dialog.cargoInfo') }}</span></el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('logistics.dialog.productCode')" prop="productCode">
              <el-input v-model="form.productCode" :placeholder="$t('logistics.dialog.productCodePlaceholder')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('logistics.dialog.subProductCode')">
              <el-input v-model="form.subProductCode" :placeholder="$t('logistics.dialog.subProductCodePlaceholder')" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('logistics.dialog.planType')" prop="planType">
              <el-select v-model="form.planType" :placeholder="$t('logistics.dialog.selectPlaceholder')" style="width:100%">
                <el-option value="SEA" :label="$t('logistics.planType.SEA')" />
                <el-option value="AIR" :label="$t('logistics.planType.AIR')" />
                <el-option value="CONSOLIDATION" :label="$t('logistics.planType.CONSOLIDATION')" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('logistics.dialog.requiresQc')">
              <el-switch v-model="form.requiresQc" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item :label="$t('logistics.dialog.cargoLengthCm')">
              <el-input-number v-model="form.cargoLengthCm" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('logistics.dialog.cargoWidthCm')">
              <el-input-number v-model="form.cargoWidthCm" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('logistics.dialog.cargoHeightCm')">
              <el-input-number v-model="form.cargoHeightCm" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('logistics.dialog.cargoWeightKg')">
              <el-input-number v-model="form.cargoWeightKg" :min="0" :precision="3" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('logistics.dialog.quantity')">
              <el-input-number v-model="form.quantity" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left"><span class="divider-label">{{ $t('logistics.dialog.shipPlan') }}</span></el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('logistics.dialog.estimatedShipDate')">
              <el-date-picker v-model="form.estimatedShipDate" type="date" value-format="YYYY-MM-DD" :placeholder="$t('logistics.dialog.datePlaceholder')" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('logistics.dialog.actualShipDate')">
              <el-date-picker v-model="form.actualShipDate" type="date" value-format="YYYY-MM-DD" :placeholder="$t('logistics.dialog.datePlaceholder')" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="$t('logistics.dialog.remarks')">
          <el-input v-model="form.remarks" type="textarea" :rows="2" :placeholder="$t('logistics.dialog.remarksPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('logistics.dialog.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">{{ $t('logistics.dialog.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" :title="$t('logistics.drawerTitle')" size="560px" direction="rtl">
      <el-descriptions :column="2" border v-if="currentRow">
        <el-descriptions-item :label="$t('logistics.column.planCode')">{{ currentRow.planCode }}</el-descriptions-item>
        <el-descriptions-item :label="$t('logistics.column.planType')">
          <el-tag :type="planTypeTag(currentRow.planType)" size="small">{{ planTypeLabel(currentRow.planType) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('logistics.column.productCode')">
          <span class="product-code">{{ currentRow.productCode }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('logistics.column.factoryName')">{{ currentRow.factoryName || (currentRow.factoryId ? `ID:${currentRow.factoryId}` : '-') }}</el-descriptions-item>
        <el-descriptions-item :label="$t('logistics.column.status')">
          <el-tag :type="logisticsStatusType(currentRow.status)" size="small">{{ logisticsStatusLabel(currentRow.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('logistics.dialog.cargoDimension')" :span="2">{{ cargoDimension }}</el-descriptions-item>
        <el-descriptions-item :label="$t('logistics.column.cargoWeightKg')">{{ currentRow.cargoWeightKg ? currentRow.cargoWeightKg + $t('common.units.kg') : '' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('logistics.column.cargoVolumeCbm')">{{ currentRow.cargoVolumeCbm ? currentRow.cargoVolumeCbm.toFixed(4) + $t('common.units.m3') : '' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('logistics.dialog.quantity')">{{ currentRow.quantity ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('logistics.column.requiresQc')">
          <el-tag :type="currentRow.requiresQc ? 'warning' : 'success'" size="small">
            {{ currentRow.requiresQc ? $t('logistics.requiresQc.yes') : $t('logistics.requiresQc.no') }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('logistics.dialog.estimatedShipDate')">{{ currentRow.estimatedShipDate || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('logistics.dialog.actualShipDate')">{{ currentRow.actualShipDate || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('logistics.dialog.remarks')" :span="2">{{ currentRow.remarks || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('logistics.dialog.createTime')">{{ currentRow.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('logistics.dialog.updateTime')">{{ currentRow.updateTime || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Van, Top, Loading } from '@element-plus/icons-vue'
import { logisticsApi, type LogisticsPlanVO, type LogisticsStatus, type PlanType } from '@/api/logistics'
import { procurementApi, type ProcurementPageVO } from '@/api/procurement'
import { useI18n } from 'vue-i18n'

const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const drawerVisible = ref(false)
const procurementLoading = ref(false)

const currentRow = ref<LogisticsPlanVO | null>(null)
const filterForm = reactive({
  planType: '' as PlanType | '',
  status: '' as LogisticsStatus | '',
})
const pagination = reactive({ page: 1, pageSize: 20, total: 0 })
const tableData = ref<LogisticsPlanVO[]>([])
const procurementList = ref<ProcurementPageVO[]>([])

const formRef = ref<FormInstance>()
const { t } = useI18n()

const form = reactive({
  procurementId: undefined as number | undefined,
  factoryId: undefined as number | undefined,
  productCode: '',
  subProductCode: '',
  planType: undefined as PlanType | undefined,
  cargoLengthCm: undefined as number | undefined,
  cargoWidthCm: undefined as number | undefined,
  cargoHeightCm: undefined as number | undefined,
  cargoWeightKg: undefined as number | undefined,
  quantity: undefined as number | undefined,
  requiresQc: false,
  estimatedShipDate: '',
  actualShipDate: '',
  remarks: '',
})

const formRules: FormRules = {
  procurementId: [{ required: true, message: () => t('logistics.validation.procurementRequired'), trigger: 'change' }],
  productCode: [{ required: true, message: () => t('logistics.validation.productCodeRequired'), trigger: 'blur' }],
  planType: [{ required: true, message: () => t('logistics.validation.planTypeRequired'), trigger: 'change' }],
}

const bookedCount = computed(() => tableData.value.filter(r => r.status === 'BOOKED').length)
const transitCount = computed(() => tableData.value.filter(r => r.status === 'IN_TRANSIT').length)

const cargoDimension = computed(() => {
  const r = currentRow.value
  if (!r) return '-'
  const parts: string[] = []
  if (r.cargoLengthCm) parts.push(String(r.cargoLengthCm))
  if (r.cargoWidthCm) parts.push(String(r.cargoWidthCm))
  if (r.cargoHeightCm) parts.push(String(r.cargoHeightCm))
  return parts.length ? parts.join(t('common.format.times')) + t('common.units.cm') : '-'
})

async function loadData() {
  loading.value = true
  try {
    const res = await logisticsApi.list({
      page: pagination.page - 1,
      pageSize: pagination.pageSize,
      planType: filterForm.planType || undefined,
      status: filterForm.status || undefined,
    })
    const data = res.data.data
    tableData.value = data?.content ?? []
    pagination.total = data?.totalElements ?? 0
  } catch (e) {
    console.error('[LogisticsPage] loadData failed', e)
    ElMessage.error(t('logistics.message.loadFailed'))
  } finally {
    loading.value = false
  }
}

async function onSearch() {
  await loadData()
}

function onSearchFromButton() {
  pagination.page = 1
  loadData()
}

function onReset() {
  filterForm.planType = ''
  filterForm.status = ''
  pagination.page = 1
  loadData()
}

async function searchProcurement(query: string) {
  if (!query) { procurementList.value = []; return }
  procurementLoading.value = true
  try {
    const res = await procurementApi.list({ page: 0, pageSize: 20, productCode: query })
    procurementList.value = res.data.data?.content ?? []
  } catch (e) { console.error('[LogisticsPage] searchProcurement failed', e); procurementList.value = [] }
  finally { procurementLoading.value = false }
}

function onProcurementSelected(id: number) {
  const p = procurementList.value.find(p => p.id === id)
  if (!p) return
  form.productCode = p.productCode
  form.subProductCode = p.subProductCode || ''
  form.factoryId = p.factoryId
  form.quantity = p.quantity
  form.requiresQc = p.requiresQc ?? false
}

function onNew() {
  formRef.value?.resetFields()
  Object.assign(form, {
    procurementId: undefined, factoryId: undefined,
    productCode: '', subProductCode: '', planType: undefined,
    cargoLengthCm: undefined, cargoWidthCm: undefined, cargoHeightCm: undefined,
    cargoWeightKg: undefined, quantity: undefined, requiresQc: false,
    estimatedShipDate: '', actualShipDate: '', remarks: '',
  })
  procurementList.value = []
  dialogVisible.value = true
}

async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await logisticsApi.create({
      procurementId: form.procurementId,
      factoryId: form.factoryId,
      productCode: form.productCode,
      subProductCode: form.subProductCode || undefined,
      planType: form.planType!,
      cargoLengthCm: form.cargoLengthCm,
      cargoWidthCm: form.cargoWidthCm,
      cargoHeightCm: form.cargoHeightCm,
      cargoWeightKg: form.cargoWeightKg,
      quantity: form.quantity,
      requiresQc: form.requiresQc,
      estimatedShipDate: form.estimatedShipDate || undefined,
      actualShipDate: form.actualShipDate || undefined,
      remarks: form.remarks || undefined,
    })
    ElMessage.success(t('logistics.message.createSuccess'))
    dialogVisible.value = false
    loadData()
  } catch (e) {
    console.error('[LogisticsPage] onSubmit failed', e)
    ElMessage.error(t('logistics.message.createFailed'))
  } finally {
    submitting.value = false
  }
}

function onView(row: LogisticsPlanVO) {
  currentRow.value = row
  drawerVisible.value = true
}

function onOverview(row: LogisticsPlanVO) {
  router.push('/base/overview/' + row.procurementId)
}

function planTypeLabel(type?: string): string {
  return { SEA: t('logistics.planType.SEA'), AIR: t('logistics.planType.AIR'), CONSOLIDATION: t('logistics.planType.CONSOLIDATION') }[type ?? ''] ?? type ?? '-'
}

function planTypeTag(type?: string): string {
  return { SEA: 'primary', AIR: 'warning', CONSOLIDATION: 'success' }[type ?? ''] ?? 'info'
}

function logisticsStatusLabel(status?: string): string {
  return { PLANNED: t('logistics.status.PLANNED'), BOOKED: t('logistics.status.BOOKED'), IN_TRANSIT: t('logistics.status.IN_TRANSIT'), DELIVERED: t('logistics.status.DELIVERED') }[status ?? ''] ?? status ?? '-'
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
.divider-label { font-size: 13px; font-weight: 600; color: var(--text-secondary); }
</style>
