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
          <el-button type="primary" @click="onSearchFromButton">查询</el-button>
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
        <el-table-column prop="sellerName" label="卖家名称" min-width="120" show-overflow-tooltip />
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
          @size-change="onSearch"
          @current-change="onSearch"
        />
      </div>
    </el-card>

    <!-- 新规验货弹窗 -->
    <el-dialog v-model="dialogVisible" title="新规验货" width="680px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="关联采购单" prop="procurementId">
          <el-select
            v-model="form.procurementId"
            placeholder="请选择采购单"
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
        <el-divider content-position="left"><span class="divider-label">验货信息</span></el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="货号" prop="productCode">
              <el-input v-model="form.productCode" placeholder="主货号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="子货号">
              <el-input v-model="form.subProductCode" placeholder="子货号/枝番" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="验货类型">
              <el-select v-model="form.qcType" placeholder="请选择" style="width:100%">
                <el-option value="ONSITE" label="现场验货" />
                <el-option value="REMOTE" label="远程验货" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="验货日期">
              <el-date-picker v-model="form.qcDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="检品数" prop="inspectionCount">
              <el-input-number v-model="form.inspectionCount" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="合格数">
              <el-input-number v-model="form.passedCount" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="不良数">
              <el-input-number v-model="form.defectiveCount" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="验货结果">
              <el-select v-model="form.result" placeholder="请选择" style="width:100%">
                <el-option value="PASS" label="合格" />
                <el-option value="FAIL" label="不合格" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="验货状态">
              <el-select v-model="form.status" placeholder="请选择" style="width:100%">
                <el-option value="PENDING" label="待验货" />
                <el-option value="COMPLETED" label="已完成" />
                <el-option value="RETURN_REQUESTED" label="退货" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="箱数">
              <el-input-number v-model="form.boxCount" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left"><span class="divider-label">货物信息</span></el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="箱子长(cm)">
              <el-input-number v-model="form.boxLengthCm" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="箱子宽(cm)">
              <el-input-number v-model="form.boxWidthCm" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="箱子高(cm)">
              <el-input-number v-model="form.boxHeightCm" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单件净重">
              <el-input-number v-model="form.netWeightPerUnit" :min="0" :precision="3" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="毛重">
              <el-input-number v-model="form.grossWeight" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="含税价">
              <el-input-number v-model="form.taxInclusivePrice" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="数量">
              <el-input-number v-model="form.quantity" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否退税">
              <el-switch v-model="form.taxRefund" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="材质">
          <el-input v-model="form.material" placeholder="如 棉/涤纶/牛皮纸" />
        </el-form-item>
        <el-form-item label="目的地">
          <el-input v-model="form.destination" placeholder="如 东京/大阪" />
        </el-form-item>
        <el-form-item label="验货标准">
          <el-input v-model="form.qcStandard" type="textarea" :rows="2" placeholder="验收标准描述" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remarks" type="textarea" :rows="2" placeholder="备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" title="验货详情" size="600px" direction="rtl">
      <el-descriptions :column="2" border v-if="currentRow">
        <el-descriptions-item label="验货编号">{{ currentRow.qcCode }}</el-descriptions-item>
        <el-descriptions-item label="验货结果">
          <el-tag :type="currentRow.result === 'PASS' ? 'success' : 'danger'" size="small">
            {{ currentRow.result === 'PASS' ? '合格' : '不合格' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="货号">
          <span class="product-code">{{ currentRow.productCode }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="验货状态">
          <el-tag :type="currentRow.status === 'COMPLETED' ? 'success' : currentRow.status === 'RETURN_REQUESTED' ? 'danger' : 'info'" size="small">
            {{ { PENDING: '待验货', COMPLETED: '已完成', RETURN_REQUESTED: '退货' }[currentRow.status] ?? currentRow.status }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="验货类型">{{ { ONSITE: '现场验货', REMOTE: '远程验货' }[currentRow.qcType ?? ''] || '-' }}</el-descriptions-item>
        <el-descriptions-item label="验货日期">{{ currentRow.qcDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="卖家名称" :span="2">{{ currentRow.sellerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="目的地">{{ currentRow.destination || '-' }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ currentRow.quantity ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="检品数">{{ currentRow.inspectionCount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="合格数">{{ currentRow.passedCount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="不良数">{{ currentRow.defectiveCount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="箱数">{{ currentRow.boxCount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="单件净重">{{ currentRow.netWeightPerUnit ? currentRow.netWeightPerUnit + 'kg' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="毛重">{{ currentRow.grossWeight ? currentRow.grossWeight + 'kg' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="含税价">{{ currentRow.taxInclusivePrice ? '¥' + currentRow.taxInclusivePrice : '-' }}</el-descriptions-item>
        <el-descriptions-item label="箱子尺寸" :span="2">{{ boxDimension }}</el-descriptions-item>
        <el-descriptions-item label="材质">{{ currentRow.material || '-' }}</el-descriptions-item>
        <el-descriptions-item label="是否退税">{{ currentRow.taxRefund ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="验货标准" :span="2">{{ currentRow.qcStandard || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentRow.remarks || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建人">{{ currentRow.createBy || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentRow.createTime || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Document, CircleCheck, Warning } from '@element-plus/icons-vue'
import { inspectionApi, type QcRecordVO, type QcResult, type QcStatus, type QcType } from '@/api/inspection'
import { procurementApi, type ProcurementPageVO } from '@/api/procurement'

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
  destination: '',
  quantity: undefined as number | undefined,
  orderDate: '',
  sellerName: '',
})

const formRules: FormRules = {
  procurementId: [{ required: true, message: '请选择关联采购单', trigger: 'change' }],
  productCode: [{ required: true, message: '请输入货号', trigger: 'blur' }],
  inspectionCount: [{ required: true, message: '请输入检品数', trigger: 'blur' }],
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
  return parts.length ? parts.join('×') + 'cm' : '-'
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
    tableData.value = data.content
    pagination.total = data.totalElements
  } catch (e: unknown) {
    console.error('[InspectionPage] loadData failed', e)
    ElMessage.error('加载验货记录失败')
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
    procurementList.value = res.data.data.content
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
    qcStandard: '', remarks: '', destination: '', quantity: undefined, orderDate: '', sellerName: '',
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
      ElMessage.warning('合格数不能大于检品数')
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
      status: form.status,
      inspectionCount: form.inspectionCount,
      passedCount: form.passedCount,
      defectiveCount: form.defectiveCount,
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
      destination: form.destination || undefined,
      quantity: form.quantity,
      orderDate: form.orderDate || undefined,
      sellerName: form.sellerName || undefined,
    })
    ElMessage.success('验货记录创建成功')
    dialogVisible.value = false
    loadData()
  } catch (e) {
    console.error('[InspectionPage] onSubmit failed', e)
    ElMessage.error('创建验货记录失败')
  } finally {
    submitting.value = false
  }
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
.divider-label { font-size: 13px; font-weight: 600; color: var(--text-secondary); }
</style>
