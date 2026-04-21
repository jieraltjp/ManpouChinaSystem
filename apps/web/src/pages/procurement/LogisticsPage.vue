<template>
  <div class="page">
    <div class="page-header">
      <h2 class="page-title">调配计划</h2>
      <div class="header-actions">
        <el-button type="primary" @click="onNew">
          <el-icon><Plus /></el-icon> 新增调配
        </el-button>
      </div>
    </div>

    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#E8650A"><Van /></el-icon></div>
            <div><div class="stat-value">{{ pagination.total }}</div><div class="stat-label">全部调配</div></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#1E40AF"><Top /></el-icon></div>
            <div><div class="stat-value">{{ bookedCount }}</div><div class="stat-label">已订舱</div></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="icon-transit" color="#7C3AED"><Loading /></el-icon></div>
            <div><div class="stat-value">{{ transitCount }}</div><div class="stat-label">运输中</div></div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="调配类型">
          <el-select v-model="filterForm.planType" placeholder="全部" clearable style="width:140px">
            <el-option value="SEA" label="海运" />
            <el-option value="AIR" label="空运" />
            <el-option value="CONSOLIDATION" label="拼柜" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="全部" clearable style="width:140px">
            <el-option value="PLANNED" label="调配中" />
            <el-option value="BOOKED" label="已订舱" />
            <el-option value="IN_TRANSIT" label="运输中" />
            <el-option value="DELIVERED" label="已送达" />
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
        <el-table-column prop="planCode" label="调配编号" width="160" />
        <el-table-column prop="productCode" label="货号" width="120">
          <template #default="{ row }">
            <span class="product-code">{{ row.productCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="planType" label="调配类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="planTypeTag(row.planType)" size="small">{{ planTypeLabel(row.planType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="cargoWeightKg" label="货物重量" width="100" align="right">
          <template #default="{ row }">
            {{ row.cargoWeightKg ? row.cargoWeightKg + 'kg' : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="cargoVolumeCbm" label="体积" width="90" align="right">
          <template #default="{ row }">
            {{ row.cargoVolumeCbm ? row.cargoVolumeCbm.toFixed(4) + 'm³' : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="requiresQc" label="需要检测" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.requiresQc ? 'warning' : 'success'" size="small">
              {{ row.requiresQc ? '需检测' : '无需' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="logisticsStatusType(row.status)" size="small">
              {{ logisticsStatusLabel(row.status) }}
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

    <!-- 新增调配弹窗 -->
    <el-dialog v-model="dialogVisible" title="新增调配" width="640px" destroy-on-close>
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
        <el-divider content-position="left"><span class="divider-label">货物信息</span></el-divider>
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
            <el-form-item label="调配类型" prop="planType">
              <el-select v-model="form.planType" placeholder="请选择" style="width:100%">
                <el-option value="SEA" label="海运" />
                <el-option value="AIR" label="空运" />
                <el-option value="CONSOLIDATION" label="拼柜" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="需要检测">
              <el-switch v-model="form.requiresQc" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="货物长(cm)">
              <el-input-number v-model="form.cargoLengthCm" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="货物宽(cm)">
              <el-input-number v-model="form.cargoWidthCm" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="货物高(cm)">
              <el-input-number v-model="form.cargoHeightCm" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="货物重量(kg)">
              <el-input-number v-model="form.cargoWeightKg" :min="0" :precision="3" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="数量">
              <el-input-number v-model="form.quantity" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left"><span class="divider-label">发货计划</span></el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="预计发货日">
              <el-date-picker v-model="form.estimatedShipDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="实际发货日">
              <el-date-picker v-model="form.actualShipDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
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
    <el-drawer v-model="drawerVisible" title="调配详情" size="560px" direction="rtl">
      <el-descriptions :column="2" border v-if="currentRow">
        <el-descriptions-item label="调配编号">{{ currentRow.planCode }}</el-descriptions-item>
        <el-descriptions-item label="调配类型">
          <el-tag :type="planTypeTag(currentRow.planType)" size="small">{{ planTypeLabel(currentRow.planType) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="货号">
          <span class="product-code">{{ currentRow.productCode }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="logisticsStatusType(currentRow.status)" size="small">{{ logisticsStatusLabel(currentRow.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="货物尺寸" :span="2">{{ cargoDimension }}</el-descriptions-item>
        <el-descriptions-item label="货物重量">{{ currentRow.cargoWeightKg ? currentRow.cargoWeightKg + 'kg' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="体积">{{ currentRow.cargoVolumeCbm ? currentRow.cargoVolumeCbm.toFixed(4) + 'm³' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ currentRow.quantity ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="需要检测">
          <el-tag :type="currentRow.requiresQc ? 'warning' : 'success'" size="small">
            {{ currentRow.requiresQc ? '需检测' : '无需' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="预计发货日">{{ currentRow.estimatedShipDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="实际发货日">{{ currentRow.actualShipDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentRow.remarks || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentRow.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ currentRow.updateTime || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Van, Top, Loading } from '@element-plus/icons-vue'
import { logisticsApi, type LogisticsPlanVO, type LogisticsStatus, type PlanType } from '@/api/logistics'
import { procurementApi, type ProcurementPageVO } from '@/api/procurement'

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
  procurementId: [{ required: true, message: '请选择关联采购单', trigger: 'change' }],
  productCode: [{ required: true, message: '请输入货号', trigger: 'blur' }],
  planType: [{ required: true, message: '请选择调配类型', trigger: 'change' }],
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
  return parts.length ? parts.join('×') + 'cm' : '-'
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
    tableData.value = data.content
    pagination.total = data.totalElements
  } catch {
    ElMessage.error('加载调配计划失败')
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
    procurementList.value = res.data.data.content
  } catch { procurementList.value = [] }
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
    ElMessage.success('调配计划创建成功')
    dialogVisible.value = false
    loadData()
  } catch {
    ElMessage.error('创建调配计划失败')
  } finally {
    submitting.value = false
  }
}

function onView(row: LogisticsPlanVO) {
  currentRow.value = row
  drawerVisible.value = true
}

function planTypeLabel(type?: string): string {
  return { SEA: '海运', AIR: '空运', CONSOLIDATION: '拼柜' }[type ?? ''] ?? type ?? '-'
}

function planTypeTag(type?: string): string {
  return { SEA: 'primary', AIR: 'warning', CONSOLIDATION: 'success' }[type ?? ''] ?? 'info'
}

function logisticsStatusLabel(status?: string): string {
  return { PLANNED: '调配中', BOOKED: '已订舱', IN_TRANSIT: '运输中', DELIVERED: '已送达' }[status ?? ''] ?? status ?? '-'
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
