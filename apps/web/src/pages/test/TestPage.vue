<template>
  <div class="test-page">
    <div class="page-header">
      <h2 class="page-title">发注单管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="onNew">
          <el-icon><Plus /></el-icon>
          新规发注
        </el-button>
      </div>
    </div>

    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="商品代码">
          <el-input v-model="filterForm.productCode" placeholder="如 de077" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="客户公司">
          <el-input v-model="filterForm.customerCompany" placeholder="客户公司" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card class="table-card" shadow="never">
      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
        style="width: 100%"
        @row-click="onRowClick"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="productCode" label="商品代码" width="120">
          <template #default="{ row }">
            <span class="product-code">{{ row.productCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="80" align="right" />
        <el-table-column prop="estimatedPriceJpy" label="估算批发价(JPY)" width="150" align="right">
          <template #default="{ row }">
            {{ row.estimatedPriceJpy ? row.estimatedPriceJpy.toLocaleString() : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="customerCompany" label="客户公司" min-width="140" show-overflow-tooltip />
        <el-table-column prop="productLead" label="商品担当" width="100" />
        <el-table-column prop="plannedShipDate" label="计划出货日" width="130" />
        <el-table-column prop="status" label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="onView(row)">详情</el-button>
            <el-button link type="primary" size="small" @click.stop="onEdit(row)"
              :disabled="row.status === '完了'">编辑</el-button>
            <el-button link type="danger" size="small" @click.stop="onDelete(row)"
              :disabled="!deletableStatuses.includes(row.status)">删除</el-button>
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

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" title="发注单详情" size="600px" direction="rtl">
      <el-descriptions :column="2" border v-if="currentRow">
        <el-descriptions-item label="商品代码">{{ currentRow.productCode }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ currentRow.quantity }}</el-descriptions-item>
        <el-descriptions-item label="人民币单价">{{ currentRow.priceRmb }}</el-descriptions-item>
        <el-descriptions-item label="汇率">{{ currentRow.exchangeRate }}</el-descriptions-item>
        <el-descriptions-item label="票点">{{ currentRow.taxPoint }}</el-descriptions-item>
        <el-descriptions-item label="估算批发价(JPY)">{{ currentRow.estimatedPriceJpy?.toLocaleString() }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType(currentRow.status)" size="small">
            {{ statusLabel(currentRow.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="客户公司">{{ currentRow.customerCompany || '-' }}</el-descriptions-item>
        <el-descriptions-item label="下单日">{{ currentRow.orderDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="厂家出货日">{{ currentRow.factoryShipDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="计划出货日">{{ currentRow.plannedShipDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="商品担当">{{ currentRow.productLead || '-' }}</el-descriptions-item>
        <el-descriptions-item label="日本担当">{{ currentRow.japanLead || '-' }}</el-descriptions-item>
        <el-descriptions-item label="中国担当">{{ currentRow.chinaLead || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发送目的地" :span="2">{{ currentRow.destination || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ currentRow.createTime }}</el-descriptions-item>
      </el-descriptions>

      <div class="drawer-actions">
        <el-button @click="drawerVisible = false">关闭</el-button>
        <el-button type="primary" @click="onEdit(currentRow)">编辑</el-button>
      </div>
    </el-drawer>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新规发注' : '编辑发注单'" width="640px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
        <el-form-item label="商品代码" prop="productCode">
          <el-input v-model="formData.productCode" placeholder="如 de077" />
        </el-form-item>
        <el-form-item label="数量" prop="quantity">
          <el-input-number v-model="formData.quantity" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="人民币单价" prop="priceRmb">
          <el-input-number v-model="formData.priceRmb" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="汇率" prop="exchangeRate">
          <el-input-number v-model="formData.exchangeRate" :min="0.0001" :precision="4" style="width: 100%" />
        </el-form-item>
        <el-form-item label="票点" prop="taxPoint">
          <el-input-number v-model="formData.taxPoint" :min="0.0001" :precision="4" style="width: 100%" />
        </el-form-item>
        <el-form-item label="客户公司">
          <el-input v-model="formData.customerCompany" placeholder="客户公司名称" />
        </el-form-item>
        <el-form-item label="商品担当">
          <el-input v-model="formData.productLead" placeholder="商品担当姓名" />
        </el-form-item>
        <el-form-item label="日本担当">
          <el-input v-model="formData.japanLead" placeholder="日本担当姓名" />
        </el-form-item>
        <el-form-item label="中国担当">
          <el-input v-model="formData.chinaLead" placeholder="中国担当姓名" />
        </el-form-item>
        <el-form-item label="计划出货日">
          <el-date-picker v-model="formData.plannedShipDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="发送目的地">
          <el-input v-model="formData.destination" placeholder="如 名古屋倉庫" />
        </el-form-item>
        <el-form-item v-if="dialogMode === 'update'" label="状态" prop="status">
          <el-select v-model="formData.status" style="width: 100%">
            <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { procurementApi, type ProcurementPageVO, type CreateProcurementRequest, type UpdateProcurementRequest } from '@/api/procurement'

const loading = ref(false)
const submitting = ref(false)
const drawerVisible = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'update'>('create')
const currentRow = ref<ProcurementPageVO | null>(null)
const formRef = ref()

const deletableStatuses = ['未定', '発注待']

const statusOptions = [
  { value: '未定', label: '未定' },
  { value: '発注待', label: '発注待' },
  { value: '永康', label: '永康' },
  { value: '直送', label: '直送' },
  { value: '倉庫着', label: '倉庫着' },
  { value: '検品', label: '検品' },
  { value: '現地検品', label: '現地検品' },
  { value: 'エア便', label: 'エア便' },
  { value: 'メーカー直送', label: 'メーカー直送' },
  { value: '輸出', label: '輸出' },
  { value: '通関', label: '通関' },
  { value: '日本着', label: '日本着' },
  { value: '会計', label: '会計' },
  { value: '完了', label: '完了' },
  { value: '退货', label: '退货' },
]

const filterForm = reactive({
  productCode: '',
  status: '',
  customerCompany: '',
})

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0,
})

const tableData = ref<ProcurementPageVO[]>([])

const defaultFormData = (): CreateProcurementRequest & { status?: string } => ({
  productCode: '',
  quantity: 1,
  priceRmb: 0,
  exchangeRate: 21.5,
  taxPoint: 1.1,
  customerCompany: '',
  productLead: '',
  japanLead: '',
  chinaLead: '',
  plannedShipDate: '',
  destination: '',
  status: '未定',
})

const formData = reactive<CreateProcurementRequest & { status?: string }>(defaultFormData())

const formRules = {
  productCode: [{ required: true, message: '商品代码不能为空', trigger: 'blur' }],
  quantity: [{ required: true, message: '数量不能为空', trigger: 'blur' }],
  priceRmb: [{ required: true, message: '人民币单价不能为空', trigger: 'blur' }],
  exchangeRate: [{ required: true, message: '汇率不能为空', trigger: 'blur' }],
  taxPoint: [{ required: true, message: '票点不能为空', trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    const res = await procurementApi.list({
      page: pagination.page - 1,
      pageSize: pagination.pageSize,
      status: filterForm.status || undefined,
      productCode: filterForm.productCode || undefined,
      customerCompany: filterForm.customerCompany || undefined,
    })
    const payload = res.data.data as { content: ProcurementPageVO[]; totalElements: number }
    tableData.value = payload.content || []
    pagination.total = payload.totalElements || 0
  } catch {
    // error handled by axios interceptor
  } finally {
    loading.value = false
  }
}

function onReset() {
  filterForm.productCode = ''
  filterForm.status = ''
  filterForm.customerCompany = ''
  pagination.page = 1
  loadData()
}

function onNew() {
  dialogMode.value = 'create'
  Object.assign(formData, defaultFormData())
  dialogVisible.value = true
}

function onView(row: ProcurementPageVO) {
  currentRow.value = row
  drawerVisible.value = true
}

function onEdit(row: ProcurementPageVO | null) {
  dialogMode.value = 'update'
  Object.assign(formData, {
    productCode: row?.productCode ?? '',
    quantity: row?.quantity ?? 1,
    priceRmb: row?.priceRmb ?? 0,
    exchangeRate: row?.exchangeRate ?? 21.5,
    taxPoint: row?.taxPoint ?? 1.1,
    customerCompany: row?.customerCompany ?? '',
    productLead: row?.productLead ?? '',
    japanLead: row?.japanLead ?? '',
    chinaLead: row?.chinaLead ?? '',
    plannedShipDate: row?.plannedShipDate ?? '',
    destination: row?.destination ?? '',
    status: row?.status ?? '未定',
  })
  dialogVisible.value = true
}

async function onDelete(row: ProcurementPageVO) {
  try {
    await procurementApi.delete(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    // error handled by axios interceptor
  }
}

async function onSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    submitting.value = true
    try {
      if (dialogMode.value === 'create') {
        const req: CreateProcurementRequest = {
          productCode: formData.productCode,
          quantity: formData.quantity,
          priceRmb: formData.priceRmb,
          exchangeRate: formData.exchangeRate,
          taxPoint: formData.taxPoint,
          customerCompany: formData.customerCompany || undefined,
          productLead: formData.productLead || undefined,
          japanLead: formData.japanLead || undefined,
          chinaLead: formData.chinaLead || undefined,
          plannedShipDate: formData.plannedShipDate || undefined,
          destination: formData.destination || undefined,
        }
        await procurementApi.create(req)
        ElMessage.success('发注单创建成功')
      } else if (currentRow.value) {
        const req: UpdateProcurementRequest = {
          productCode: formData.productCode,
          quantity: formData.quantity,
          priceRmb: formData.priceRmb,
          exchangeRate: formData.exchangeRate,
          taxPoint: formData.taxPoint,
          customerCompany: formData.customerCompany || undefined,
          productLead: formData.productLead || undefined,
          japanLead: formData.japanLead || undefined,
          chinaLead: formData.chinaLead || undefined,
          plannedShipDate: formData.plannedShipDate || undefined,
          destination: formData.destination || undefined,
          status: formData.status !== '未定' ? formData.status : undefined,
        }
        await procurementApi.update(currentRow.value.id, req)
        ElMessage.success('发注单更新成功')
      }
      dialogVisible.value = false
      loadData()
    } finally {
      submitting.value = false
    }
  })
}

function onRowClick(row: ProcurementPageVO) {
  onView(row)
}

function statusLabel(status: string): string {
  return statusOptions.find(s => s.value === status)?.label ?? status
}

function statusType(status: string): string {
  const map: Record<string, string> = {
    '未定': 'info',
    '発注待': 'warning',
    '永康': 'warning',
    '直送': 'warning',
    '倉庫着': 'primary',
    '現地検品': 'primary',
    '検品': 'primary',
    'エア便': 'success',
    'メーカー直送': 'success',
    '輸出': 'success',
    '通関': 'success',
    '日本着': 'success',
    '会計': 'warning',
    '完了': 'info',
    '退货': 'danger',
  }
  return map[status] ?? 'info'
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.test-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.filter-card :deep(.el-card__body) {
  padding-bottom: 0;
}

.table-card :deep(.el-table__row) {
  cursor: pointer;
}

.product-code {
  color: #409eff;
  font-family: monospace;
  font-size: 13px;
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.drawer-actions {
  position: absolute;
  bottom: 20px;
  left: 0;
  right: 0;
  padding: 16px 24px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}
</style>
