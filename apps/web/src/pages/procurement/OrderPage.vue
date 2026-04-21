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

    <!-- 统计行 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#E8650A"><Document /></el-icon></div>
            <div>
              <div class="stat-value">{{ pagination.total }}</div>
              <div class="stat-label">发注单总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#E8650A"><Clock /></el-icon></div>
            <div>
              <div class="stat-value">{{ activeCount }}</div>
              <div class="stat-label">进行中</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#16A34A"><CircleCheck /></el-icon></div>
            <div>
              <div class="stat-value">{{ completedCount }}</div>
              <div class="stat-label">已完成</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#DC2626"><Warning /></el-icon></div>
            <div>
              <div class="stat-value">{{ returnedCount }}</div>
              <div class="stat-label">退货</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

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
        :data="tableRows"
        stripe
        style="width: 100%"
      >
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
        <el-table-column prop="createTime" label="创建时间" width="160">
          <template #default="{ row }">
            {{ row.createTime ? new Date(row.createTime).toLocaleString('zh-CN', {year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',second:'2-digit'}) : '-' }}
          </template>
        </el-table-column>
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
        <el-descriptions-item label="关联工厂">{{ currentRow.factoryId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="商品代码">{{ currentRow.productCode }}</el-descriptions-item>
        <el-descriptions-item label="子货号">{{ currentRow.subProductCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ currentRow.quantity }}</el-descriptions-item>
        <el-descriptions-item label="材质">{{ currentRow.material || '-' }}</el-descriptions-item>
        <el-descriptions-item label="需要检测">{{ currentRow.requiresQc ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="人民币单价">{{ currentRow.priceRmb }}</el-descriptions-item>
        <el-descriptions-item label="汇率">{{ currentRow.exchangeRate }}</el-descriptions-item>
        <el-descriptions-item label="票点">{{ currentRow.taxPoint }}</el-descriptions-item>
        <el-descriptions-item label="估算批发价(JPY)">{{ currentRow.estimatedPriceJpy?.toLocaleString() }}</el-descriptions-item>
        <el-descriptions-item label="报关类型">{{ billingTypeLabel(currentRow.billingType) }}</el-descriptions-item>
        <el-descriptions-item label="报关备注" :span="2">{{ currentRow.customsRemarks || '-' }}</el-descriptions-item>
        <el-descriptions-item label="说明书" :span="2">{{ currentRow.instructionManual || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType(currentRow.status)" size="small">
            {{ statusLabel(currentRow.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="客户公司">{{ currentRow.customerCompany || '-' }}</el-descriptions-item>
        <el-descriptions-item label="下单日">{{ currentRow.orderDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="厂家出货日">{{ currentRow.factoryShipDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="计划出货日">{{ currentRow.plannedShipDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="实际出货日">{{ currentRow.actualShipDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="商品担当">{{ currentRow.productLead || '-' }}</el-descriptions-item>
        <el-descriptions-item label="日本担当">{{ currentRow.japanLead || '-' }}</el-descriptions-item>
        <el-descriptions-item label="中国担当">{{ currentRow.chinaLead || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发送目的地" :span="2">{{ currentRow.destination || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ currentRow.createTime ? new Date(currentRow.createTime).toLocaleString('zh-CN', {year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',second:'2-digit'}) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="更新时间" :span="2">{{ currentRow.updateTime ? new Date(currentRow.updateTime).toLocaleString('zh-CN', {year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',second:'2-digit'}) : '-' }}</el-descriptions-item>
      </el-descriptions>

      <div class="drawer-actions">
        <el-button @click="drawerVisible = false">关闭</el-button>
        <el-button type="primary" @click="onEdit(currentRow)">编辑</el-button>
      </div>
    </el-drawer>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新规发注' : '编辑发注单'" width="800px">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
        <!-- 关联需求（创建时可选） -->
        <el-form-item v-if="dialogMode === 'create'" label="关联需求">
          <el-select v-model="selectedDemandId" placeholder="从补货需求带入商品信息（可选）" clearable filterable style="width:100%" @change="onDemandChange">
            <el-option v-for="d in demandOptions" :key="d.id" :label="`${d.demandCode} | ${d.productCode} | ${d.demandType === 'NEW_PURCHASE' ? '新品采购' : '补货'} | ${d.status}`" :value="d.id" />
          </el-select>
        </el-form-item>

        <!-- 选择工厂（必填） -->
        <el-form-item label="选择工厂" prop="factoryId">
          <div class="factory-select-row">
            <el-select v-model="formData.factoryId" placeholder="请选择工厂" filterable style="flex:1" :disabled="dialogMode === 'update'" @change="onFactorySelected">
              <el-option v-for="f in factoryOptions" :key="f.id" :label="`${f.factoryName}（${f.factoryCode}）`" :value="f.id" />
            </el-select>
            <el-button size="small" :disabled="dialogMode === 'update'" @click="onFactoryNew">
              <el-icon><Plus /></el-icon> 新建
            </el-button>
            <el-tooltip content="编辑选中工厂" placement="top">
              <el-button size="small" :disabled="!formData.factoryId || dialogMode === 'update'" @click="onFactoryEditCurrent">
                <el-icon><Edit /></el-icon>
              </el-button>
            </el-tooltip>
          </div>
        </el-form-item>

        <!-- 商品信息 -->
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="商品代码" prop="productCode">
              <el-input v-model="formData.productCode" placeholder="如 de077" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="子货号">
              <el-input v-model="formData.subProductCode" placeholder="如 re/wh/bk（颜色）" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="数量" prop="quantity">
              <el-input-number v-model="formData.quantity" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="材质">
              <el-input v-model="formData.material" placeholder="如 plastic/metal" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="需要检测">
              <el-switch v-model="formData.requiresQc" active-text="是" inactive-text="否" />
            </el-form-item>
          </el-col>
        </el-row>
        <!-- 价格 -->
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="人民币单价" prop="priceRmb">
              <el-input-number v-model="formData.priceRmb" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="汇率" prop="exchangeRate">
              <el-input-number v-model="formData.exchangeRate" :min="0.0001" :precision="4" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="票点" prop="taxPoint">
              <el-input-number v-model="formData.taxPoint" :min="0.0001" :precision="4" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="估算批发价">
          <div class="price-preview">
            <span class="price-value">{{ previewPriceJpy }}</span>
            <span class="price-unit">JPY</span>
            <span class="price-formula">= (RMB ÷ {{ formData.taxPoint }} × 1.02 × 1.2) × {{ formData.exchangeRate }} × 1.05</span>
          </div>
        </el-form-item>
        <!-- 报关类型 -->
        <el-form-item label="报关类型">
          <el-select v-model="formData.billingType" placeholder="选择报关类型" clearable style="width: 100%">
            <el-option v-for="opt in BILLING_TYPE_OPTIONS" :key="opt.value" :value="opt.value" :label="opt.label" />
          </el-select>
        </el-form-item>
        <!-- 第五行：报关备注 + 说明书 -->
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="报关备注">
              <el-input v-model="formData.customsRemarks" placeholder="报关备注" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="说明书">
              <el-input v-model="formData.instructionManual" placeholder="说明书备注" />
            </el-form-item>
          </el-col>
        </el-row>
        <!-- 第六行：客户公司 + 发送目的地 -->
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="客户公司">
              <el-input v-model="formData.customerCompany" placeholder="客户公司名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发送目的地">
              <el-input v-model="formData.destination" placeholder="如 名古屋倉庫" />
            </el-form-item>
          </el-col>
        </el-row>
        <!-- 第七行：担当 -->
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="商品担当">
              <el-input v-model="formData.productLead" placeholder="商品担当" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="日本担当">
              <el-input v-model="formData.japanLead" placeholder="日本担当" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="中国担当">
              <el-input v-model="formData.chinaLead" placeholder="中国担当" />
            </el-form-item>
          </el-col>
        </el-row>
        <!-- 第八行：日期 -->
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="下单日">
              <el-date-picker v-model="formData.orderDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="厂家出货日">
              <el-date-picker v-model="formData.factoryShipDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="计划出货日">
              <el-date-picker v-model="formData.plannedShipDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="实际出货日">
              <el-date-picker v-model="formData.actualShipDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <!-- 状态（仅更新模式） -->
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

    <!-- 工厂新建/编辑弹窗 -->
    <el-dialog v-model="factoryDialogVisible" :title="factoryDialogMode === 'create' ? '新增工厂' : '编辑工厂'" width="520px">
      <el-form ref="factoryFormRef" :model="factoryFormData" :rules="factoryFormRules" label-width="110px">
        <el-form-item label="工厂名称" prop="factoryName">
          <el-input v-model="factoryFormData.factoryName" placeholder="工厂全称" />
        </el-form-item>
        <el-form-item label="工厂位置">
          <el-input v-model="factoryFormData.location" placeholder="省/市，如 浙江省金华市" />
        </el-form-item>
        <el-form-item label="粗略位置">
          <el-input v-model="factoryFormData.roughLocation" placeholder="工业区/镇/园区" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="factoryFormData.contactName" placeholder="联系人姓名" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="factoryFormData.contactPhone" placeholder="手机或座机" />
        </el-form-item>
        <el-form-item v-if="factoryDialogMode === 'update'" label="状态">
          <el-select v-model="factoryFormData.status" style="width:100%">
            <el-option value="ACTIVE" label="合作中" />
            <el-option value="INACTIVE" label="已停止" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="factoryDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="factorySubmitting" @click="onFactorySubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, type FormInstance, ElMessageBox } from 'element-plus'
import { Plus, Clock, CircleCheck, Warning, Document, Edit } from '@element-plus/icons-vue'
import { procurementApi, type ProcurementPageVO, type CreateProcurementRequest, type UpdateProcurementRequest, BILLING_TYPE_OPTIONS } from '@/api/procurement'
import { factoryApi, type FactoryPageVO, type CreateFactoryRequest, type UpdateFactoryRequest, type FactoryStatus } from '@/api/factory'
import { demandApi, type DemandPageVO } from '@/api/demand'

const loading = ref(false)
const submitting = ref(false)
const drawerVisible = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'update'>('create')
const currentRow = ref<ProcurementPageVO | null>(null)
const formRef = ref<FormInstance>()

const deletableStatuses = ['未定', '発注待']

const statusOptions = [
  { value: '未定', label: '未定' },
  { value: '予定', label: '予定' },
  { value: 'OEM', label: 'OEM' },
  { value: '発注待', label: '発注待' },
  { value: '永康', label: '永康' },
  { value: '直送', label: '直送' },
  { value: '倉庫着', label: '倉庫着' },
  { value: '検品', label: '検品' },
  { value: '現地検品', label: '現地検品' },
  { value: 'エア便', label: 'エア便' },
  { value: 'メーカー直送', label: 'メーカー直送' },
  { value: '輸出', label: '輸出' },
  { value: '国内通関', label: '国内通関' },
  { value: '通関', label: '通関' },
  { value: '日本着', label: '日本着' },
  { value: '日本通関完了', label: '日本通関完了' },
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

const tableRows = ref<ProcurementPageVO[]>([])

// 工厂下拉
const factoryOptions = ref<FactoryPageVO[]>([])
async function loadFactories() {
  try {
    const res = await factoryApi.list({ page: 0, pageSize: 200 })
    factoryOptions.value = (res.data.data as { content: FactoryPageVO[] }).content || []
  } catch { /* handled by interceptor */ }
}

// 需求下拉（仅 PENDING）
const demandOptions = ref<DemandPageVO[]>([])
const selectedDemandId = ref<number | null>(null)
async function loadDemands() {
  try {
    const res = await demandApi.list({ page: 0, pageSize: 200, status: 'PENDING' })
    demandOptions.value = (res.data.data as { content: DemandPageVO[] }).content || []
  } catch { /* handled by interceptor */ }
}

/** 选中需求 → 自动带入 productCode / subProductCode / destination / japanLead / quantity */
function onDemandChange(demandId: number | null) {
  if (!demandId) return
  const d = demandOptions.value.find(x => x.id === demandId)
  if (!d) return
  formData.productCode = d.productCode
  formData.subProductCode = d.subProductCode || ''
  formData.destination = d.destination || ''
  formData.japanLead = d.japanLead || ''
  formData.quantity = d.quantity
}

// ===== 工厂管理 =====
const factoryDialogVisible = ref(false)
const factoryDialogMode = ref<'create' | 'update'>('create')
const factoryCurrentRow = ref<FactoryPageVO | null>(null)
const factoryFormRef = ref<FormInstance>()
const factorySubmitting = ref(false)

const defaultFactoryForm = (): CreateFactoryRequest & { status?: FactoryStatus } => ({
  factoryName: '',
  location: '',
  roughLocation: '',
  contactName: '',
  contactPhone: '',
})

const factoryFormData = reactive<CreateFactoryRequest & { status?: FactoryStatus }>(defaultFactoryForm())
const factoryFormRules = {
  factoryName: [{ required: true, message: '工厂名称不能为空', trigger: 'blur' }],
}

function onFactoryNew() {
  factoryDialogMode.value = 'create'
  Object.assign(factoryFormData, defaultFactoryForm())
  factoryDialogVisible.value = true
}

/** 选中工厂变更 — 清空需求关联（避免混淆） */
function onFactorySelected(_id: number | null) {
  selectedDemandId.value = null
}

/** 编辑当前选中的工厂 */
function onFactoryEditCurrent() {
  if (!formData.factoryId) return
  const factory = factoryOptions.value.find(f => f.id === formData.factoryId)
  if (!factory) return
  onFactoryEdit(factory)
}

function onFactoryEdit(row: FactoryPageVO) {
  factoryDialogMode.value = 'update'
  factoryCurrentRow.value = row
  Object.assign(factoryFormData, {
    factoryName: row.factoryName,
    location: row.location || '',
    roughLocation: row.roughLocation || '',
    contactName: row.contactName || '',
    contactPhone: row.contactPhone || '',
    status: row.status,
  })
  factoryDialogVisible.value = true
}

async function onFactorySubmit() {
  if (!factoryFormRef.value) return
  await factoryFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    factorySubmitting.value = true
    try {
      if (factoryDialogMode.value === 'create') {
        await factoryApi.create(factoryFormData as CreateFactoryRequest)
        ElMessage.success('工厂创建成功')
      } else if (factoryCurrentRow.value) {
        await factoryApi.update(factoryCurrentRow.value.id, factoryFormData as UpdateFactoryRequest)
        ElMessage.success('更新成功')
      }
      factoryDialogVisible.value = false
      await loadFactories()
    } finally {
      factorySubmitting.value = false
    }
  })
}

const activeCount = computed(() =>
  tableRows.value.filter(r => r.status !== '完了' && r.status !== '退货').length,
)
const completedCount = computed(() =>
  tableRows.value.filter(r => r.status === '完了').length,
)
const returnedCount = computed(() =>
  tableRows.value.filter(r => r.status === '退货').length,
)

const previewPriceJpy = computed(() => {
  const { priceRmb, taxPoint, exchangeRate } = formData
  if (!priceRmb || !taxPoint || !exchangeRate) return '—'
  const base = (priceRmb / taxPoint * 1.02 * 1.2) * exchangeRate * 1.05
  return Math.round(base * 100) / 100
})

const defaultFormData = (): CreateProcurementRequest & { status?: string } => ({
  factoryId: undefined,
  productCode: '',
  subProductCode: '',
  material: '',
  requiresQc: false,
  quantity: 1,
  priceRmb: 0,
  exchangeRate: 21.5,
  taxPoint: 1.1,
  billingType: undefined,
  customsRemarks: '',
  instructionManual: '',
  actualShipDate: '',
  orderDate: '',
  factoryShipDate: '',
  plannedShipDate: '',
  customerCompany: '',
  productLead: '',
  japanLead: '',
  chinaLead: '',
  destination: '',
  status: '未定',
})

const formData = reactive<CreateProcurementRequest & { status?: string }>(defaultFormData())

const formRules = {
  factoryId: [{ required: true, message: '请选择工厂', trigger: 'change' }],
  productCode: [
    { required: true, message: '商品代码不能为空', trigger: 'blur' },
    { max: 32, message: '商品代码最多 32 字符', trigger: 'blur' },
  ],
  quantity: [
    { required: true, message: '数量不能为空', trigger: 'blur' },
    { type: 'number', min: 1, message: '数量必须为正整数', trigger: 'blur' },
  ],
  priceRmb: [
    { required: true, message: '人民币单价不能为空', trigger: 'blur' },
    { type: 'number', min: 0, message: '人民币单价不能为负', trigger: 'blur' },
  ],
  exchangeRate: [
    { required: true, message: '汇率不能为空', trigger: 'blur' },
    { type: 'number', min: 0.0001, message: '汇率必须为正数', trigger: 'blur' },
  ],
  taxPoint: [
    { required: true, message: '票点不能为空', trigger: 'blur' },
    { type: 'number', min: 0.0001, message: '票点必须为正数', trigger: 'blur' },
  ],
  customerCompany: [{ max: 128, message: '客户公司最多 128 字符', trigger: 'blur' }],
  destination: [{ max: 128, message: '发送目的地最多 128 字符', trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    const res = await procurementApi.list({
      page: pagination.page - 1,
      pageSize: pagination.pageSize,
      status: filterForm.status || undefined,
      productCode: filterForm.productCode.trim() || undefined,
      customerCompany: filterForm.customerCompany.trim() || undefined,
    })
    const payload = res.data.data as { content: ProcurementPageVO[]; totalElements: number }
    tableRows.value = payload.content || []
    pagination.total = payload.totalElements || 0
    // 删除后若当前页越界，回退到第1页
    if (tableRows.value.length === 0 && pagination.total > 0 && pagination.page > 1) {
      pagination.page = 1
      loadData()
    }
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
  selectedDemandId.value = null
  Object.assign(formData, defaultFormData())
  dialogVisible.value = true
}

function onView(row: ProcurementPageVO) {
  currentRow.value = row
  drawerVisible.value = true
}

function onEdit(row: ProcurementPageVO | null) {
  dialogMode.value = 'update'
  currentRow.value = row
  selectedDemandId.value = null
  Object.assign(formData, {
    factoryId: row?.factoryId ?? undefined,
    productCode: row?.productCode ?? '',
    subProductCode: row?.subProductCode ?? '',
    material: row?.material ?? '',
    requiresQc: row?.requiresQc ?? false,
    quantity: row?.quantity ?? 1,
    priceRmb: row?.priceRmb ?? 0,
    exchangeRate: row?.exchangeRate ?? 21.5,
    taxPoint: row?.taxPoint ?? 1.1,
    billingType: row?.billingType ?? undefined,
    customsRemarks: row?.customsRemarks ?? '',
    instructionManual: row?.instructionManual ?? '',
    actualShipDate: row?.actualShipDate ?? '',
    orderDate: row?.orderDate ?? '',
    factoryShipDate: row?.factoryShipDate ?? '',
    plannedShipDate: row?.plannedShipDate ?? '',
    customerCompany: row?.customerCompany ?? '',
    productLead: row?.productLead ?? '',
    japanLead: row?.japanLead ?? '',
    chinaLead: row?.chinaLead ?? '',
    destination: row?.destination ?? '',
    status: row?.status ?? '未定',
  })
  dialogVisible.value = true
}

async function onDelete(row: ProcurementPageVO) {
  try {
    await ElMessageBox.confirm(
      `确认删除发注单「${row.productCode}」（${row.quantity}件）？`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await procurementApi.delete(row.id)
    ElMessage.success('删除成功')
    drawerVisible.value = false
    currentRow.value = null
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
          factoryId: formData.factoryId || undefined,
          productCode: formData.productCode,
          subProductCode: formData.subProductCode || undefined,
          material: formData.material || undefined,
          requiresQc: formData.requiresQc || undefined,
          quantity: formData.quantity,
          priceRmb: formData.priceRmb,
          exchangeRate: formData.exchangeRate,
          taxPoint: formData.taxPoint,
          billingType: formData.billingType || undefined,
          customsRemarks: formData.customsRemarks || undefined,
          instructionManual: formData.instructionManual || undefined,
          actualShipDate: formData.actualShipDate || undefined,
          orderDate: formData.orderDate || undefined,
          factoryShipDate: formData.factoryShipDate || undefined,
          plannedShipDate: formData.plannedShipDate || undefined,
          customerCompany: formData.customerCompany || undefined,
          productLead: formData.productLead || undefined,
          japanLead: formData.japanLead || undefined,
          chinaLead: formData.chinaLead || undefined,
          destination: formData.destination || undefined,
          status: formData.status || undefined,
        }
        await procurementApi.create(req)
        ElMessage.success('发注单创建成功')
      } else if (currentRow.value) {
        const req: UpdateProcurementRequest = {
          factoryId: formData.factoryId || undefined,
          productCode: formData.productCode,
          subProductCode: formData.subProductCode || undefined,
          material: formData.material || undefined,
          requiresQc: formData.requiresQc || undefined,
          quantity: formData.quantity,
          priceRmb: formData.priceRmb,
          exchangeRate: formData.exchangeRate,
          taxPoint: formData.taxPoint,
          billingType: formData.billingType || undefined,
          customsRemarks: formData.customsRemarks || undefined,
          instructionManual: formData.instructionManual || undefined,
          actualShipDate: formData.actualShipDate || undefined,
          orderDate: formData.orderDate || undefined,
          factoryShipDate: formData.factoryShipDate || undefined,
          plannedShipDate: formData.plannedShipDate || undefined,
          customerCompany: formData.customerCompany || undefined,
          productLead: formData.productLead || undefined,
          japanLead: formData.japanLead || undefined,
          chinaLead: formData.chinaLead || undefined,
          destination: formData.destination || undefined,
          status: formData.status || undefined,
        }
        const updatedId = currentRow.value.id
        await procurementApi.update(updatedId, req)
        ElMessage.success('发注单更新成功')
        // 同步更新表格当前行，避免整体刷新
        const idx = tableRows.value.findIndex(r => r.id === updatedId)
        if (idx !== -1) {
          const { data } = await procurementApi.get(updatedId)
          tableRows.value[idx] = data.data as ProcurementPageVO
        }
      }
      dialogVisible.value = false
      loadData()
    } finally {
      submitting.value = false
    }
  })
}

function statusLabel(status: string): string {
  return statusOptions.find(s => s.value === status)?.label ?? status
}

function statusType(status: string): string {
  const statusTypeMap: Record<string, string> = {
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
    '国内通関': 'success',
    '通関': 'success',
    '日本着': 'success',
    '日本通関完了': 'success',
    '会計': 'warning',
    '完了': 'info',
    '退货': 'danger',
  }
  return statusTypeMap[status] ?? 'info'
}

function billingTypeLabel(val: string | undefined): string {
  const map: Record<string, string> = {
    ZHE_LU_KAI_PIAO: '浙鲁开票',
    CHAO_HUI_TUI_SHUI: '超慧退税',
    NO_REFUND: '不退税',
    OTHER: '其他',
  }
  return val ? (map[val] ?? val) : '—'
}

onMounted(() => { loadData(); loadFactories(); loadDemands() })
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
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.3px;
}
.page-title::before {
  content: '';
  display: inline-block;
  width: 4px;
  height: 20px;
  background: var(--color-primary);
  border-radius: 2px;
  margin-right: 10px;
  vertical-align: middle;
}

.filter-card :deep(.el-card__body) {
  padding-bottom: 0;
}

.stats-row {
  margin-bottom: 4px;
}

/* ── 统计卡 ── */
.stat-card {
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-card);
  transition: all var(--transition-fast);
  position: relative;
  overflow: hidden;
}
.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--color-primary), var(--color-primary-light));
  border-radius: var(--radius-md) var(--radius-md) 0 0;
}
.stat-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
  border-color: var(--color-primary-pale);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 14px;
}

.stat-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--color-primary-pale);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-icon {
  font-size: 22px;
}

.stat-value {
  font-size: 26px;
  font-weight: 800;
  color: var(--text-primary);
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 4px;
  font-weight: 500;
}

.table-card :deep(.el-table__row) {
  cursor: pointer;
}

/* ── 商品代码：橙色 monospace 标签 ── */
.product-code {
  color: var(--color-primary);
  font-family: 'JetBrains Mono', 'Fira Code', 'Cascadia Code', monospace;
  font-size: 12px;
  font-weight: 700;
  background: var(--color-primary-pale);
  padding: 3px 9px;
  border-radius: 5px;
  border: 1px solid rgba(232,101,10,0.2);
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

/* ── 价格预览 ── */
.price-preview {
  display: flex;
  align-items: baseline;
  gap: 6px;
  color: var(--text-primary);
  line-height: 1;
  background: var(--color-primary-pale);
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  border: 1px solid rgba(232,101,10,0.15);
}

.price-value {
  font-size: 22px;
  font-weight: 800;
  color: var(--color-primary);
  font-variant-numeric: tabular-nums;
}

.price-unit {
  font-size: 13px;
  color: var(--color-primary-dark);
  font-weight: 600;
}

.price-formula {
  font-size: 11px;
  color: var(--text-muted);
  margin-left: 4px;
}

/* ── 抽屉底部按钮区 ── */
.drawer-actions {
  position: absolute;
  bottom: 20px;
  left: 0;
  right: 0;
  padding: 16px 24px;
  border-top: 1px solid var(--border-color);
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  background: #fff;
}

/* ── 工厂选择行 ── */
.factory-select-row {
  display: flex;
  gap: 8px;
  align-items: center;
  width: 100%;
}
</style>
