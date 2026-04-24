<template>
  <div class="page">
    <div class="page-header">
      <h2 class="page-title">{{ $t('inspection.title') }}</h2>
      <div class="header-actions">
        <el-button type="primary" @click="onNew">
          <el-icon><Plus /></el-icon> {{ $t('inspection.newButton') }}
        </el-button>
      </div>
    </div>

    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#E8650A"><Document /></el-icon></div>
            <div><div class="stat-value">{{ pagination.total }}</div><div class="stat-label">{{ $t('inspection.stat.total') }}</div></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#16A34A"><CircleCheck /></el-icon></div>
            <div><div class="stat-value">{{ passCount }}</div><div class="stat-label">{{ $t('inspection.stat.pass') }}</div></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#DC2626"><Warning /></el-icon></div>
            <div><div class="stat-value">{{ failCount }}</div><div class="stat-label">{{ $t('inspection.stat.fail') }}</div></div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filterForm">
        <el-form-item :label="$t('inspection.filter.qcCode')">
          <el-input v-model="filterForm.qcCode" :placeholder="$t('inspection.filter.qcCodePlaceholder')" clearable style="width:180px" />
        </el-form-item>
        <el-form-item :label="$t('inspection.filter.result')">
          <el-select v-model="filterForm.result" :placeholder="$t('inspection.filter.all')" clearable style="width:120px">
            <el-option value="PASS" :label="$t('inspection.result.pass')" />
            <el-option value="FAIL" :label="$t('inspection.result.fail')" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearchFromButton">{{ $t('inspection.filter.search') }}</el-button>
          <el-button @click="onReset">{{ $t('inspection.filter.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="tableData" stripe style="width:100%" min-height="200">
        <el-table-column prop="qcCode" :label="$t('inspection.column.qcCode')" min-width="160" />
        <el-table-column prop="productCode" :label="$t('inspection.column.productCode')" min-width="120">
          <template #default="{ row }">
            <span class="product-code">{{ row.productCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sellerName" :label="$t('inspection.column.sellerName')" min-width="120" show-overflow-tooltip />
        <el-table-column prop="inspectionCount" :label="$t('inspection.column.inspectionCount')" min-width="90" align="right" />
        <el-table-column prop="passedCount" :label="$t('inspection.column.passedCount')" min-width="90" align="right">
          <template #default="{ row }">
            <span style="color:#16A34A;font-weight:600">{{ row.passedCount ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="defectiveCount" :label="$t('inspection.column.defectiveCount')" min-width="90" align="right">
          <template #default="{ row }">
            <span v-if="row.defectiveCount" style="color:#DC2626;font-weight:600">{{ row.defectiveCount }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="boxCount" :label="$t('inspection.column.boxCount')" min-width="70" align="right" />
        <el-table-column prop="qcDate" :label="$t('inspection.column.qcDate')" min-width="120" />
        <el-table-column prop="result" :label="$t('inspection.column.result')" min-width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.result === 'PASS' ? 'success' : 'danger'" size="small">
              {{ row.result === 'PASS' ? $t('inspection.result.pass') : $t('inspection.result.fail') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('inspection.column.action')" min-width="160" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="onView(row)">{{ $t('inspection.action.detail') }}</el-button>
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

    <!-- 新规验货弹窗 -->
    <el-dialog v-model="dialogVisible" :title="$t('inspection.newDialogTitle')" width="680px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item :label="$t('inspection.dialog.procurement')" prop="procurementId">
          <el-select
            v-model="form.procurementId"
            :placeholder="$t('inspection.dialog.procurementPlaceholder')"
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
        <el-divider content-position="left"><span class="divider-label">{{ $t('inspection.dialog.inspectionInfo') }}</span></el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('inspection.dialog.productCode')" prop="productCode">
              <el-input v-model="form.productCode" :placeholder="$t('inspection.dialog.productCodePlaceholder')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('inspection.dialog.subProductCode')">
              <el-input v-model="form.subProductCode" :placeholder="$t('inspection.dialog.subProductCodePlaceholder')" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('inspection.dialog.qcType')">
              <el-select v-model="form.qcType" :placeholder="$t('inspection.dialog.selectPlaceholder')" style="width:100%">
                <el-option value="ONSITE" :label="$t('inspection.qcType.onsite')" />
                <el-option value="REMOTE" :label="$t('inspection.qcType.remote')" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('inspection.dialog.qcDate')">
              <el-date-picker v-model="form.qcDate" type="date" value-format="YYYY-MM-DD" :placeholder="$t('inspection.dialog.datePlaceholder')" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item :label="$t('inspection.dialog.inspectionCount')" prop="inspectionCount">
              <el-input-number v-model="form.inspectionCount" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('inspection.dialog.passedCount')">
              <el-input-number v-model="form.passedCount" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('inspection.dialog.defectiveCount')">
              <el-input-number v-model="form.defectiveCount" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item :label="$t('inspection.dialog.result')">
              <el-select v-model="form.result" :placeholder="$t('inspection.dialog.selectPlaceholder')" style="width:100%">
                <el-option value="PASS" :label="$t('inspection.result.pass')" />
                <el-option value="FAIL" :label="$t('inspection.result.fail')" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('inspection.dialog.qcStatus')">
              <el-select v-model="form.status" :placeholder="$t('inspection.dialog.selectPlaceholder')" style="width:100%">
                <el-option value="PENDING" :label="$t('inspection.qcStatus.pending')" />
                <el-option value="COMPLETED" :label="$t('inspection.qcStatus.completed')" />
                <el-option value="RETURN_REQUESTED" :label="$t('inspection.qcStatus.returnRequested')" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('inspection.dialog.boxCount')">
              <el-input-number v-model="form.boxCount" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left"><span class="divider-label">{{ $t('inspection.dialog.cargoInfo') }}</span></el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('inspection.dialog.boxLengthCm')">
              <el-input-number v-model="form.boxLengthCm" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('inspection.dialog.boxWidthCm')">
              <el-input-number v-model="form.boxWidthCm" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('inspection.dialog.boxHeightCm')">
              <el-input-number v-model="form.boxHeightCm" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('inspection.dialog.netWeightPerUnit')">
              <el-input-number v-model="form.netWeightPerUnit" :min="0" :precision="3" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('inspection.dialog.grossWeight')">
              <el-input-number v-model="form.grossWeight" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('inspection.dialog.taxInclusivePrice')">
              <el-input-number v-model="form.taxInclusivePrice" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('inspection.dialog.quantity')">
              <el-input-number v-model="form.quantity" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('inspection.dialog.taxRefund')">
              <el-switch v-model="form.taxRefund" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="$t('inspection.dialog.material')">
          <el-input v-model="form.material" :placeholder="$t('inspection.dialog.materialPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('inspection.dialog.destination')">
          <el-input v-model="form.destination" :placeholder="$t('inspection.dialog.destinationPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('inspection.dialog.qcStandard')">
          <el-input v-model="form.qcStandard" type="textarea" :rows="2" :placeholder="$t('inspection.dialog.qcStandardPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('inspection.dialog.remarks')">
          <el-input v-model="form.remarks" type="textarea" :rows="2" :placeholder="$t('inspection.dialog.remarksPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('inspection.dialog.images')">
          <el-input v-model="form.images" type="textarea" :rows="2" :placeholder="$t('inspection.dialog.imagesPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" :title="$t('inspection.drawerTitle')" size="600px" direction="rtl">
      <el-descriptions :column="2" border v-if="currentRow">
        <el-descriptions-item :label="$t('inspection.column.qcCode')">{{ currentRow.qcCode }}</el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.column.result')">
          <el-tag :type="currentRow.result === 'PASS' ? 'success' : 'danger'" size="small">
            {{ currentRow.result === 'PASS' ? $t('inspection.result.pass') : $t('inspection.result.fail') }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.column.productCode')">
          <span class="product-code">{{ currentRow.productCode }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.dialog.qcStatus')">
          <el-tag :type="currentRow.status === 'COMPLETED' ? 'success' : currentRow.status === 'RETURN_REQUESTED' ? 'danger' : 'info'" size="small">
            {{ qcStatusLabel(currentRow.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.dialog.qcType')">{{ qcTypeLabel(currentRow.qcType) }}</el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.column.qcDate')">{{ currentRow.qcDate || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.column.sellerName')" :span="2">{{ currentRow.sellerName || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.dialog.destination')">{{ currentRow.destination || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.dialog.quantity')">{{ currentRow.quantity ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.column.inspectionCount')">{{ currentRow.inspectionCount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.column.passedCount')">{{ currentRow.passedCount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.column.defectiveCount')">{{ currentRow.defectiveCount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.column.boxCount')">{{ currentRow.boxCount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.dialog.netWeightPerUnit')">{{ currentRow.netWeightPerUnit ? currentRow.netWeightPerUnit + $t('common.units.kg') : '' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.dialog.grossWeight')">{{ currentRow.grossWeight ? currentRow.grossWeight + $t('common.units.kg') : '' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.dialog.taxInclusivePrice')">{{ currentRow.taxInclusivePrice ? $t('common.units.cny') + currentRow.taxInclusivePrice : '' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.dialog.boxDimension')" :span="2">{{ boxDimension }}</el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.dialog.material')">{{ currentRow.material || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.dialog.taxRefund')">{{ currentRow.taxRefund ? $t('inspection.yes') : $t('inspection.no') }}</el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.dialog.qcStandard')" :span="2">{{ currentRow.qcStandard || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.dialog.remarks')" :span="2">{{ currentRow.remarks || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.dialog.images')" :span="2">
          <span v-if="!currentRow.images">-</span>
          <template v-else>
            <span v-for="(url, i) in (currentRow.images || '').split('\n').filter(Boolean)" :key="i">
              <a :href="url" target="_blank" style="color:#409eff;word-break:break-all;">{{ url }}</a>
              <br />
            </span>
          </template>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.dialog.createBy')">{{ currentRow.createBy || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.dialog.createTime')">{{ currentRow.createTime || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Document, CircleCheck, Warning } from '@element-plus/icons-vue'
import { inspectionApi, type QcRecordVO, type QcResult, type QcStatus, type QcType } from '@/api/inspection'
import { procurementApi, type ProcurementPageVO } from '@/api/procurement'
import { useI18n } from 'vue-i18n'

const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const drawerVisible = ref(false)
const procurementLoading = ref(false)

const currentRow = ref<QcRecordVO | null>(null)
const filterForm = reactive({ qcCode: '', result: '' as QcResult | '' })
const pagination = reactive({ page: 1, pageSize: 20, total: 0 })
const tableData = ref<QcRecordVO[]>([])
const procurementList = ref<ProcurementPageVO[]>([])

const formRef = ref<FormInstance>()
const { t } = useI18n()

const form = reactive({
  procurementId: undefined as number | undefined,
  productCode: '',
  subProductCode: '',
  qcUserId: undefined as number | undefined,
  qcType: undefined as QcType | undefined,
  qcDate: '',
  result: undefined as QcResult | undefined,
  status: 'PENDING' as QcStatus,
  inspectionCount: undefined as number | undefined,
  passedCount: undefined as number | undefined,
  defectiveCount: undefined as number | undefined,
  boxCount: undefined as number | undefined,
  boxLengthCm: undefined as number | undefined,
  boxWidthCm: undefined as number | undefined,
  boxHeightCm: undefined as number | undefined,
  netWeightPerUnit: undefined as number | undefined,
  grossWeight: undefined as number | undefined,
  taxInclusivePrice: undefined as number | undefined,
  material: '',
  taxRefund: false,
  qcStandard: '',
  remarks: '',
  images: '',
  destination: '',
  quantity: undefined as number | undefined,
  orderDate: '',
  sellerName: '',
})

const formRules: FormRules = {
  procurementId: [{ required: true, message: () => t('inspection.validation.procurementRequired'), trigger: 'change' }],
  productCode: [{ required: true, message: () => t('inspection.validation.productCodeRequired'), trigger: 'blur' }],
  inspectionCount: [{ required: true, message: () => t('inspection.validation.inspectionCountRequired'), trigger: 'blur' }],
}

const passCount = computed(() => tableData.value.filter(r => r.result === 'PASS').length)
const failCount = computed(() => tableData.value.filter(r => r.result === 'FAIL').length)

const boxDimension = computed(() => {
  const r = currentRow.value
  if (!r) return '-'
  const parts = []
  if (r.boxLengthCm) parts.push(r.boxLengthCm)
  if (r.boxWidthCm) parts.push(r.boxWidthCm)
  if (r.boxHeightCm) parts.push(r.boxHeightCm)
  return parts.length ? parts.join(t('common.format.times')) + t('common.units.cm') : '-'
})

async function loadData() {
  loading.value = true
  try {
    const res = await inspectionApi.list({
      page: pagination.page - 1,
      pageSize: pagination.pageSize,
      qcCode: filterForm.qcCode || undefined,
      result: filterForm.result || undefined,
    })
    const data = res.data.data
    tableData.value = data?.content ?? []
    pagination.total = data?.totalElements ?? 0
  } catch (e: unknown) {
    console.error('[InspectionPage] loadData failed', e)
    ElMessage.error(t('inspection.message.loadFailed'))
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
  filterForm.qcCode = ''
  filterForm.result = ''
  pagination.page = 1
  loadData()
}

async function searchProcurement(query: string) {
  if (!query) { procurementList.value = []; return }
  procurementLoading.value = true
  try {
    const res = await procurementApi.list({ page: 0, pageSize: 20, productCode: query })
    procurementList.value = res.data.data?.content ?? []
  } catch (e) { console.error('[InspectionPage] searchProcurement failed', e); procurementList.value = [] }
  finally { procurementLoading.value = false }
}

function onProcurementSelected(id: number) {
  const p = procurementList.value.find(p => p.id === id)
  if (!p) return
  form.productCode = p.productCode
  form.subProductCode = p.subProductCode || ''
  form.quantity = p.quantity
  form.material = p.material || ''
  form.destination = p.destination || ''
}

function onNew() {
  formRef.value?.resetFields()
  Object.assign(form, {
    procurementId: undefined, productCode: '', subProductCode: '', qcUserId: undefined,
    qcType: undefined, qcDate: '', result: undefined, status: 'PENDING',
    inspectionCount: undefined, passedCount: undefined, defectiveCount: undefined,
    boxCount: undefined, boxLengthCm: undefined, boxWidthCm: undefined,
    boxHeightCm: undefined, netWeightPerUnit: undefined, grossWeight: undefined,
    taxInclusivePrice: undefined, material: '', taxRefund: false,
    qcStandard: '', remarks: '', images: '', destination: '', quantity: undefined, orderDate: '', sellerName: '',
  })
  procurementList.value = []
  dialogVisible.value = true
}

async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (form.inspectionCount !== undefined && form.passedCount !== undefined && form.passedCount > form.inspectionCount) {
      ElMessage.warning(t('inspection.validation.passedCountExceeds'))
      submitting.value = false
      return
    }
    await inspectionApi.create({
      procurementId: form.procurementId!,
      productCode: form.productCode,
      subProductCode: form.subProductCode || undefined,
      qcUserId: form.qcUserId,
      qcType: form.qcType,
      qcDate: form.qcDate || undefined,
      result: form.result,
      inspectionCount: form.inspectionCount,
      passedCount: form.passedCount,
      boxCount: form.boxCount,
      boxLengthCm: form.boxLengthCm,
      boxWidthCm: form.boxWidthCm,
      boxHeightCm: form.boxHeightCm,
      netWeightPerUnit: form.netWeightPerUnit,
      grossWeight: form.grossWeight,
      taxInclusivePrice: form.taxInclusivePrice,
      material: form.material || undefined,
      taxRefund: form.taxRefund,
      qcStandard: form.qcStandard || undefined,
      remarks: form.remarks || undefined,
      images: form.images || undefined,
      destination: form.destination || undefined,
      quantity: form.quantity,
      orderDate: form.orderDate || undefined,
      sellerName: form.sellerName || undefined,
    })
    ElMessage.success(t('inspection.message.createSuccess'))
    dialogVisible.value = false
    loadData()
  } catch (e) {
    console.error('[InspectionPage] onSubmit failed', e)
    ElMessage.error(t('inspection.message.createFailed'))
  } finally {
    submitting.value = false
  }
}

function onView(row: QcRecordVO) {
  currentRow.value = row
  drawerVisible.value = true
}

function onOverview(row: QcRecordVO) {
  router.push('/base/overview/' + row.procurementId)
}

function qcTypeLabel(qcType?: string): string {
  return { ONSITE: t('inspection.qcType.onsite'), REMOTE: t('inspection.qcType.remote') }[qcType ?? ''] ?? '-'
}

function qcStatusLabel(status?: string): string {
  return { PENDING: t('inspection.qcStatus.pending'), COMPLETED: t('inspection.qcStatus.completed'), RETURN_REQUESTED: t('inspection.qcStatus.returnRequested') }[status ?? ''] ?? status ?? '-'
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
.divider-label { font-size: 13px; font-weight: 600; color: var(--text-secondary); }
</style>
