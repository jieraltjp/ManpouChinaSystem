<template>
  <div class="page">
    <div class="page-header">
      <h2 class="page-title">{{ $t('factory.title') }}</h2>
      <div class="header-actions">
        <el-button type="primary" @click="onNew">
          <el-icon><Plus /></el-icon> {{ $t('factory.newButton') }}
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
              <div class="stat-label">{{ $t('factory.stat.total') }}</div>
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
              <div class="stat-label">{{ $t('factory.stat.active') }}</div>
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
              <div class="stat-label">{{ $t('factory.stat.potential') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filterForm">
        <el-form-item :label="$t('factory.filter.factoryName')">
          <el-input v-model="filterForm.factoryName" :placeholder="$t('factory.filter.factoryName')" clearable style="width:160px" />
        </el-form-item>
        <el-form-item :label="$t('factory.filter.cooperationStatus')">
          <el-select v-model="filterForm.cooperationStatus" :placeholder="$t('factory.filter.all')" clearable style="width:140px">
            <el-option v-for="s in cooperationOptions" :key="s.value" :value="s.value" :label="s.label" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">{{ $t('factory.filter.search') }}</el-button>
          <el-button @click="onReset">{{ $t('factory.filter.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="tableData" stripe style="width:100%">
        <el-table-column prop="factoryCode" :label="$t('factory.column.factoryCode')" width="160" />
        <el-table-column prop="factoryName" :label="$t('factory.column.factoryName')" min-width="160" show-overflow-tooltip />
        <el-table-column prop="category" :label="$t('factory.column.category')" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ categoryLabel(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="province" :label="$t('factory.column.province')" width="90" />
        <el-table-column prop="city" :label="$t('factory.column.city')" width="90" />
        <el-table-column prop="roughLocation" :label="$t('factory.column.roughLocation')" min-width="140" show-overflow-tooltip />
        <el-table-column prop="contactName" :label="$t('factory.column.contactName')" width="100" />
        <el-table-column prop="contactPhone" :label="$t('factory.column.contactPhone')" width="130" />
        <el-table-column prop="cooperationStatus" :label="$t('factory.column.cooperationStatus')" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="cooperationStatusTag(row.cooperationStatus)" size="small">
              {{ cooperationStatusLabel(row.cooperationStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('factory.column.action')" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="onView(row)">{{ $t('factory.action.detail') }}</el-button>
            <el-button link type="primary" size="small" @click.stop="onEdit(row)">{{ $t('factory.action.edit') }}</el-button>
            <el-button link type="danger" size="small" @click.stop="onDelete(row)">{{ $t('factory.action.delete') }}</el-button>
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
    <el-drawer v-model="drawerVisible" :title="$t('factory.drawer.title')" size="600px" direction="rtl">
      <el-descriptions :column="2" border v-if="currentRow">
        <el-descriptions-item :label="$t('factory.drawer.factoryCode')">{{ currentRow.factoryCode }}</el-descriptions-item>
        <el-descriptions-item :label="$t('factory.drawer.cooperationStatus')">
          <el-tag :type="cooperationStatusTag(currentRow.cooperationStatus)" size="small">
            {{ cooperationStatusLabel(currentRow.cooperationStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('factory.drawer.factoryName')" :span="2">{{ currentRow.factoryName }}</el-descriptions-item>
        <el-descriptions-item :label="$t('factory.drawer.category')">
          <el-tag size="small" type="info">{{ categoryLabel(currentRow.category) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('factory.drawer.paymentTerms')">{{ paymentTermsLabel(currentRow.paymentTerms) }}</el-descriptions-item>
        <el-descriptions-item :label="$t('factory.drawer.address')" :span="2">
          {{ [currentRow.province, currentRow.city, currentRow.county, currentRow.roughLocation].filter(Boolean).join('') || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('factory.drawer.contactName')">{{ currentRow.contactName || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('factory.drawer.contactPhone')">{{ currentRow.contactPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('factory.drawer.contactWechat')">{{ currentRow.contactWechat || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('factory.drawer.contactQq')">{{ currentRow.contactQq || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('factory.drawer.notes')" :span="2">{{ currentRow.notes || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('factory.drawer.createTime')" :span="2">{{ currentRow.createTime ? new Date(currentRow.createTime).toLocaleString(locale === 'ja' ? 'ja-JP' : 'zh-CN') : '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('factory.drawer.updateTime')" :span="2">{{ currentRow.updateTime ? new Date(currentRow.updateTime).toLocaleString(locale === 'ja' ? 'ja-JP' : 'zh-CN') : '-' }}</el-descriptions-item>
      </el-descriptions>
      <div class="drawer-actions">
        <el-button @click="drawerVisible = false">{{ $t('factory.drawer.close') }}</el-button>
        <el-button type="primary" @click="onEdit(currentRow)">{{ $t('factory.drawer.edit') }}</el-button>
      </div>
    </el-drawer>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? $t('factory.dialog.newTitle') : $t('factory.dialog.editTitle')" width="680px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
        <el-form-item :label="$t('factory.dialog.factoryName')" prop="factoryName">
          <el-input v-model="formData.factoryName" :placeholder="$t('factory.dialog.factoryNamePlaceholder')" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item :label="$t('factory.dialog.category')">
              <el-select v-model="formData.category" :placeholder="$t('factory.dialog.categoryPlaceholder')" clearable style="width:100%">
                <el-option v-for="c in categoryOptions" :key="c.value" :value="c.value" :label="c.label" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('factory.dialog.cooperationStatus')">
              <el-select v-model="formData.cooperationStatus" :placeholder="$t('factory.dialog.cooperationStatusPlaceholder')" clearable style="width:100%">
                <el-option v-for="s in cooperationOptions" :key="s.value" :value="s.value" :label="s.label" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('factory.dialog.paymentTerms')">
              <el-select v-model="formData.paymentTerms" :placeholder="$t('factory.dialog.paymentTermsPlaceholder')" clearable style="width:100%">
                <el-option v-for="p in paymentTermsOptions" :key="p.value" :value="p.value" :label="p.label" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item :label="$t('factory.dialog.province')">
              <el-input v-model="formData.province" :placeholder="$t('factory.dialog.provincePlaceholder')" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('factory.dialog.city')">
              <el-input v-model="formData.city" :placeholder="$t('factory.dialog.cityPlaceholder')" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('factory.dialog.county')">
              <el-input v-model="formData.county" :placeholder="$t('factory.dialog.countyPlaceholder')" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="$t('factory.dialog.roughLocation')">
          <el-input v-model="formData.roughLocation" :placeholder="$t('factory.dialog.roughLocationPlaceholder')" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('factory.dialog.contactName')">
              <el-input v-model="formData.contactName" :placeholder="$t('factory.dialog.contactNamePlaceholder')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('factory.dialog.contactPhone')">
              <el-input v-model="formData.contactPhone" :placeholder="$t('factory.dialog.contactPhonePlaceholder')" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('factory.dialog.contactWechat')">
              <el-input v-model="formData.contactWechat" :placeholder="$t('factory.dialog.contactWechatPlaceholder')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('factory.dialog.contactQq')">
              <el-input v-model="formData.contactQq" :placeholder="$t('factory.dialog.contactQqPlaceholder')" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="$t('factory.dialog.notes')">
          <el-input v-model="formData.notes" type="textarea" :rows="2" :placeholder="$t('factory.dialog.notesPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('factory.dialog.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">{{ $t('factory.dialog.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, type FormInstance, ElMessageBox } from 'element-plus'
import { Plus, OfficeBuilding, CircleCheck, Clock } from '@element-plus/icons-vue'
import { factoryApi, type FactoryPageVO, type CreateFactoryRequest, type UpdateFactoryRequest, type CooperationStatus, type FactoryCategory, type PaymentTerms } from '@/api/factory'
import { useI18n } from 'vue-i18n'

const { t, locale } = useI18n()

const CATEGORY_KEYS: FactoryCategory[] = ['TOOLS', 'TEXTILE', 'PLASTIC', 'ELECTRONICS', 'FURNITURE', 'AUTO_PARTS', 'SPORTS', 'PET', 'MEDICAL', 'CRAFTS', 'CHEMICAL', 'OTHER']
const COOPERATION_KEYS: CooperationStatus[] = ['ACTIVE', 'SUSPENDED', 'ELIMINATED', 'POTENTIAL']
const PAYMENT_KEYS: PaymentTerms[] = ['CASH', 'NET_30', 'NET_60', 'NET_90', 'CREDIT']

const categoryOptions = computed(() => CATEGORY_KEYS.map(k => ({ value: k, label: t(`factory.category.${k}`) })))
const cooperationOptions = computed(() => COOPERATION_KEYS.map(k => ({ value: k, label: t(`factory.cooperationStatus.${k}`) })))
const paymentTermsOptions = computed(() => PAYMENT_KEYS.map(k => ({ value: k, label: t(`factory.paymentTerms.${k}`) })))

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
  factoryName: [{ required: true, message: () => t('factory.validation.factoryNameRequired'), trigger: 'blur' }],
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
    await ElMessageBox.confirm(
      t('factory.message.deleteConfirm', { name: row.factoryName }),
      t('factory.message.deleteConfirmTitle'),
      { type: 'warning' }
    )
  } catch { return }
  try {
    await factoryApi.delete(row.id)
    ElMessage.success(t('factory.message.deleteSuccess'))
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
        ElMessage.success(t('factory.message.createSuccess'))
      } else if (currentRow.value) {
        await factoryApi.update(currentRow.value.id, formData as UpdateFactoryRequest)
        ElMessage.success(t('factory.message.updateSuccess'))
      }
      dialogVisible.value = false
      loadData()
    } finally {
      submitting.value = false
    }
  })
}

function cooperationStatusLabel(s?: string): string {
  return t(`factory.cooperationStatus.${s}` as any, { default: s ?? '-' })
}

function cooperationStatusTag(s?: string): string {
  return { ACTIVE: 'success', SUSPENDED: 'warning', ELIMINATED: 'danger', POTENTIAL: 'info' }[s ?? ''] ?? 'info'
}

function categoryLabel(c?: string): string {
  return t(`factory.category.${c}` as any, { default: c ?? '-' })
}

function paymentTermsLabel(p?: string): string {
  return t(`factory.paymentTerms.${p}` as any, { default: p ?? '-' })
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
