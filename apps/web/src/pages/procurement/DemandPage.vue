<template>
  <div class="page">
    <div class="page-header">
      <h2 class="page-title">补货需求</h2>
      <div class="header-actions">
        <el-button type="primary" @click="onNew">
          <el-icon><Plus /></el-icon> 新规录入
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
              <div class="stat-label">全部需求</div>
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
              <div class="stat-label">待确认</div>
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
              <div class="stat-label">已转采购</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="需求类型">
          <el-select v-model="filterForm.demandType" placeholder="全部" clearable style="width:140px">
            <el-option value="REPLENISHMENT" label="补货（非新品）" />
            <el-option value="NEW_PURCHASE" label="新品采购" />
          </el-select>
        </el-form-item>
        <el-form-item label="货号">
          <el-input v-model="filterForm.productCode" placeholder="主货号" clearable style="width:140px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="全部" clearable style="width:140px">
            <el-option value="PENDING" label="待确认" />
            <el-option value="CONVERTED" label="已转采购" />
            <el-option value="CANCELLED" label="已取消" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="tableData" stripe style="width:100%">
        <el-table-column prop="demandCode" label="需求编号" width="160" />
        <el-table-column prop="demandType" label="类型" width="130" align="center">
          <template #default="{ row }">
            <el-tag :type="row.demandType === 'NEW_PURCHASE' ? 'warning' : 'primary'" size="small">
              {{ row.demandType === 'NEW_PURCHASE' ? '新品采购' : '补货（非新品）' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="productCode" label="主货号" width="120">
          <template #default="{ row }">
            <span class="product-code">{{ row.productCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="subProductCode" label="子货号" width="100" />
        <el-table-column prop="quantity" label="数量" width="80" align="right" />
        <el-table-column prop="destination" label="目的地" min-width="100" show-overflow-tooltip />
        <el-table-column prop="japanLead" label="日本担当" width="100" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="demandStatusType(row.status)" size="small">
              {{ demandStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="录入时间" width="160">
          <template #default="{ row }">
            {{ row.createTime ? new Date(row.createTime).toLocaleString('zh-CN') : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" link type="primary" size="small" @click.stop="onConvert(row)">
              转采购
            </el-button>
            <el-button v-if="row.status === 'CONVERTED'" link type="primary" size="small" @click.stop="onViewLinked(row)">
              查看采购单
            </el-button>
            <el-button link type="primary" size="small" @click.stop="onEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 'PENDING'" link type="danger" size="small" @click.stop="onDelete(row)">
              删除
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
    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新规录入 — 补货需求' : '编辑需求'" width="560px">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px">
        <el-form-item label="需求类型" prop="demandType">
          <el-radio-group v-model="formData.demandType">
            <el-radio value="REPLENISHMENT">补货（非新品）</el-radio>
            <el-radio value="NEW_PURCHASE">新品采购</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="主货号" prop="productCode">
          <el-input v-model="formData.productCode" placeholder="如 odn012" />
        </el-form-item>
        <el-form-item label="子货号（颜色）">
          <el-input v-model="formData.subProductCode" placeholder="如 re / wh / bk" />
        </el-form-item>
        <el-form-item label="需求量" prop="quantity">
          <el-input-number v-model="formData.quantity" :min="1" style="width:100%" />
        </el-form-item>
        <el-form-item label="目的地">
          <el-input v-model="formData.destination" placeholder="如 名古屋 / 久留米" />
        </el-form-item>
        <el-form-item label="日本担当">
          <el-input v-model="formData.japanLead" placeholder="日本担当姓名" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.remarks" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, type FormInstance, ElMessageBox } from 'element-plus'
import { Plus, Clock, Warning, CircleCheck } from '@element-plus/icons-vue'
import { demandApi, type DemandPageVO, type CreateDemandRequest, type UpdateDemandRequest } from '@/api/demand'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'update'>('create')
const currentRow = ref<DemandPageVO | null>(null)
const formRef = ref<FormInstance>()

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
  demandType: [{ required: true, message: '请选择需求类型', trigger: 'change' }],
  productCode: [{ required: true, message: '主货号不能为空', trigger: 'blur' }],
  quantity: [{ required: true, message: '需求量不能为空', trigger: 'blur' }],
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

function onConvert(_row: DemandPageVO) {
  ElMessage.info('请先在「发注单」页面创建采购单，然后在需求列表中关联。')
}

function onViewLinked(row: DemandPageVO) {
  if (row.linkedProcurementId) {
    ElMessage.info(`关联采购单ID: ${row.linkedProcurementId}`)
  }
}

async function onDelete(row: DemandPageVO) {
  try {
    await ElMessageBox.confirm(`确认删除需求「${row.demandCode}」？`, '删除确认', { type: 'warning' })
  } catch { return }
  try {
    await demandApi.delete(row.id)
    ElMessage.success('删除成功')
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
        ElMessage.success('需求录入成功')
      } else if (currentRow.value) {
        await demandApi.update(currentRow.value.id, formData as UpdateDemandRequest)
        ElMessage.success('更新成功')
      }
      dialogVisible.value = false
      loadData()
    } finally {
      submitting.value = false
    }
  })
}

function demandStatusLabel(status: string): string {
  return { PENDING: '待确认', CONVERTED: '已转采购', CANCELLED: '已取消' }[status] ?? status
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
