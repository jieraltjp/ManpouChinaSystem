<template>
  <div class="page">
    <div class="page-header">
      <h2 class="page-title">工厂管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="onNew">
          <el-icon><Plus /></el-icon> 新增工厂
        </el-button>
      </div>
    </div>

    <!-- 统计行 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#E8650A"><OfficeBuilding /></el-icon></div>
            <div>
              <div class="stat-value">{{ pagination.total }}</div>
              <div class="stat-label">工厂总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#16A34A"><CircleCheck /></el-icon></div>
            <div>
              <div class="stat-value">{{ activeCount }}</div>
              <div class="stat-label">合作中</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#D97706"><Clock /></el-icon></div>
            <div>
              <div class="stat-value">{{ potentialCount }}</div>
              <div class="stat-label">潜在合作</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="工厂名称">
          <el-input v-model="filterForm.factoryName" placeholder="工厂名称" clearable style="width:160px" />
        </el-form-item>
        <el-form-item label="合作状态">
          <el-select v-model="filterForm.cooperationStatus" placeholder="全部" clearable style="width:140px">
            <el-option value="ACTIVE" label="合作中" />
            <el-option value="SUSPENDED" label="已暂停" />
            <el-option value="ELIMINATED" label="已淘汰" />
            <el-option value="POTENTIAL" label="潜在合作" />
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
        <el-table-column prop="factoryCode" label="工厂编号" width="160" />
        <el-table-column prop="factoryName" label="工厂名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ categoryLabel(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="province" label="省份" width="90" />
        <el-table-column prop="city" label="城市" width="90" />
        <el-table-column prop="roughLocation" label="详细地址" min-width="140" show-overflow-tooltip />
        <el-table-column prop="contactName" label="联系人" width="100" />
        <el-table-column prop="contactPhone" label="联系电话" width="130" />
        <el-table-column prop="cooperationStatus" label="合作状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="cooperationStatusTag(row.cooperationStatus)" size="small">
              {{ cooperationStatusLabel(row.cooperationStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="onView(row)">详情</el-button>
            <el-button link type="primary" size="small" @click.stop="onEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click.stop="onDelete(row)">删除</el-button>
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
    <el-drawer v-model="drawerVisible" title="工厂详情" size="600px" direction="rtl">
      <el-descriptions :column="2" border v-if="currentRow">
        <el-descriptions-item label="工厂编号">{{ currentRow.factoryCode }}</el-descriptions-item>
        <el-descriptions-item label="合作状态">
          <el-tag :type="cooperationStatusTag(currentRow.cooperationStatus)" size="small">
            {{ cooperationStatusLabel(currentRow.cooperationStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="工厂名称" :span="2">{{ currentRow.factoryName }}</el-descriptions-item>
        <el-descriptions-item label="分类">
          <el-tag size="small" type="info">{{ categoryLabel(currentRow.category) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="账期">{{ paymentTermsLabel(currentRow.paymentTerms) }}</el-descriptions-item>
        <el-descriptions-item label="地址" :span="2">
          {{ [currentRow.province, currentRow.city, currentRow.county, currentRow.roughLocation].filter(Boolean).join('') || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="联系人">{{ currentRow.contactName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ currentRow.contactPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="微信号">{{ currentRow.contactWechat || '-' }}</el-descriptions-item>
        <el-descriptions-item label="QQ号">{{ currentRow.contactQq || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentRow.notes || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ currentRow.createTime ? new Date(currentRow.createTime).toLocaleString('zh-CN') : '-' }}</el-descriptions-item>
        <el-descriptions-item label="更新时间" :span="2">{{ currentRow.updateTime ? new Date(currentRow.updateTime).toLocaleString('zh-CN') : '-' }}</el-descriptions-item>
      </el-descriptions>
      <div class="drawer-actions">
        <el-button @click="drawerVisible = false">关闭</el-button>
        <el-button type="primary" @click="onEdit(currentRow)">编辑</el-button>
      </div>
    </el-drawer>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增工厂' : '编辑工厂'" width="680px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
        <el-form-item label="工厂名称" prop="factoryName">
          <el-input v-model="formData.factoryName" placeholder="工厂全称" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="分类">
              <el-select v-model="formData.category" placeholder="选择分类" clearable style="width:100%">
                <el-option v-for="c in CATEGORY_OPTIONS" :key="c.value" :value="c.value" :label="c.label" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="合作状态">
              <el-select v-model="formData.cooperationStatus" placeholder="选择状态" clearable style="width:100%">
                <el-option v-for="s in COOPERATION_OPTIONS" :key="s.value" :value="s.value" :label="s.label" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="账期">
              <el-select v-model="formData.paymentTerms" placeholder="选择账期" clearable style="width:100%">
                <el-option v-for="p in PAYMENT_OPTIONS" :key="p.value" :value="p.value" :label="p.label" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="省份">
              <el-input v-model="formData.province" placeholder="省/自治区" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="城市">
              <el-input v-model="formData.city" placeholder="市" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="区县">
              <el-input v-model="formData.county" placeholder="区/县" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="详细地址">
          <el-input v-model="formData.roughLocation" placeholder="工业区/镇/园区/街道门牌号" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="联系人">
              <el-input v-model="formData.contactName" placeholder="联系人姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="formData.contactPhone" placeholder="手机或座机" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="微信号">
              <el-input v-model="formData.contactWechat" placeholder="微信号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="QQ号">
              <el-input v-model="formData.contactQq" placeholder="QQ号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="formData.notes" type="textarea" :rows="2" placeholder="备注信息" />
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
import { Plus, OfficeBuilding, CircleCheck, Clock } from '@element-plus/icons-vue'
import { factoryApi, type FactoryPageVO, type CreateFactoryRequest, type UpdateFactoryRequest, type CooperationStatus, type FactoryCategory, type PaymentTerms } from '@/api/factory'

const CATEGORY_OPTIONS: { value: FactoryCategory; label: string }[] = [
  { value: 'TOOLS', label: '五金工具' },
  { value: 'TEXTILE', label: '纺织服装' },
  { value: 'PLASTIC', label: '塑料制品' },
  { value: 'ELECTRONICS', label: '电子电器' },
  { value: 'FURNITURE', label: '家具家居' },
  { value: 'AUTO_PARTS', label: '汽车配件' },
  { value: 'SPORTS', label: '运动户外' },
  { value: 'PET', label: '宠物用品' },
  { value: 'MEDICAL', label: '医疗器械' },
  { value: 'CRAFTS', label: '工艺礼品' },
  { value: 'CHEMICAL', label: '化工材料' },
  { value: 'OTHER', label: '其他' },
]

const COOPERATION_OPTIONS: { value: CooperationStatus; label: string }[] = [
  { value: 'ACTIVE', label: '合作中' },
  { value: 'SUSPENDED', label: '已暂停' },
  { value: 'ELIMINATED', label: '已淘汰' },
  { value: 'POTENTIAL', label: '潜在合作' },
]

const PAYMENT_OPTIONS: { value: PaymentTerms; label: string }[] = [
  { value: 'CASH', label: '现结' },
  { value: 'NET_30', label: '月结30天' },
  { value: 'NET_60', label: '月结60天' },
  { value: 'NET_90', label: '月结90天' },
  { value: 'CREDIT', label: '信用账期' },
]

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const drawerVisible = ref(false)
const dialogMode = ref<'create' | 'update'>('create')
const currentRow = ref<FactoryPageVO | null>(null)
const formRef = ref<FormInstance>()

const filterForm = reactive({ factoryName: '', cooperationStatus: '' })
const pagination = reactive({ page: 1, pageSize: 20, total: 0 })
const tableData = ref<FactoryPageVO[]>([])

const activeCount = computed(() => tableData.value.filter(r => r.cooperationStatus === 'ACTIVE').length)
const potentialCount = computed(() => tableData.value.filter(r => r.cooperationStatus === 'POTENTIAL').length)

const defaultFormData = (): CreateFactoryRequest => ({
  factoryName: '',
  category: undefined,
  province: '',
  city: '',
  county: '',
  roughLocation: '',
  contactName: '',
  contactPhone: '',
  contactWechat: '',
  contactQq: '',
  cooperationStatus: undefined,
  paymentTerms: undefined,
  notes: '',
})

const formData = reactive<CreateFactoryRequest>(defaultFormData())

const formRules = {
  factoryName: [{ required: true, message: '工厂名称不能为空', trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    const res = await factoryApi.list({
      page: pagination.page - 1,
      pageSize: pagination.pageSize,
      factoryName: filterForm.factoryName.trim() || undefined,
      cooperationStatus: filterForm.cooperationStatus || undefined,
    })
    const payload = res.data.data as { content: FactoryPageVO[]; totalElements: number }
    tableData.value = payload.content || []
    pagination.total = payload.totalElements || 0
    if (tableData.value.length === 0 && pagination.total > 0 && pagination.page > 1) {
      pagination.page = 1
      loadData()
    }
  } catch { /* interceptor */ } finally {
    loading.value = false
  }
}

function onReset() {
  filterForm.factoryName = ''
  filterForm.cooperationStatus = ''
  pagination.page = 1
  loadData()
}

function onNew() {
  dialogMode.value = 'create'
  Object.assign(formData, defaultFormData())
  dialogVisible.value = true
}

function onView(row: FactoryPageVO) {
  currentRow.value = row
  drawerVisible.value = true
}

function onEdit(row: FactoryPageVO | null) {
  if (row) drawerVisible.value = false
  dialogMode.value = 'update'
  currentRow.value = row
  Object.assign(formData, {
    factoryName: row?.factoryName ?? '',
    category: row?.category ?? undefined,
    province: row?.province ?? '',
    city: row?.city ?? '',
    county: row?.county ?? '',
    roughLocation: row?.roughLocation ?? '',
    contactName: row?.contactName ?? '',
    contactPhone: row?.contactPhone ?? '',
    contactWechat: row?.contactWechat ?? '',
    contactQq: row?.contactQq ?? '',
    cooperationStatus: row?.cooperationStatus ?? undefined,
    paymentTerms: row?.paymentTerms ?? undefined,
    notes: row?.notes ?? '',
  })
  dialogVisible.value = true
}

async function onDelete(row: FactoryPageVO) {
  try {
    await ElMessageBox.confirm(`确认删除工厂「${row.factoryName}」？`, '删除确认', { type: 'warning' })
  } catch { return }
  try {
    await factoryApi.delete(row.id)
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
        await factoryApi.create(formData as CreateFactoryRequest)
        ElMessage.success('工厂创建成功')
      } else if (currentRow.value) {
        await factoryApi.update(currentRow.value.id, formData as UpdateFactoryRequest)
        ElMessage.success('更新成功')
      }
      dialogVisible.value = false
      loadData()
    } finally {
      submitting.value = false
    }
  })
}

function cooperationStatusLabel(s?: string): string {
  return COOPERATION_OPTIONS.find(o => o.value === s)?.label ?? s ?? '-'
}

function cooperationStatusTag(s?: string): string {
  return { ACTIVE: 'success', SUSPENDED: 'warning', ELIMINATED: 'danger', POTENTIAL: 'info' }[s ?? ''] ?? 'info'
}

function categoryLabel(c?: string): string {
  return CATEGORY_OPTIONS.find(o => o.value === c)?.label ?? c ?? '-'
}

function paymentTermsLabel(p?: string): string {
  return PAYMENT_OPTIONS.find(o => o.value === p)?.label ?? p ?? '-'
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
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
.drawer-actions { position: absolute; bottom: 20px; left: 0; right: 0; padding: 16px 24px; border-top: 1px solid var(--border-color); display: flex; gap: 12px; justify-content: flex-end; background: #fff; }
</style>
