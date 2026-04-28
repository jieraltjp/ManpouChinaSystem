<template>
  <div class="page">
    <!-- 统计行 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#409EFF"><OfficeBuilding /></el-icon></div>
            <div>
              <div class="stat-value">{{ factoryStats.total }}</div>
              <div class="stat-label">{{ $t('factory.stat.total') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#67C23A"><CircleCheck /></el-icon></div>
            <div>
              <div class="stat-value">{{ factoryStats.active }}</div>
              <div class="stat-label">{{ $t('factory.stat.active') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#E6A23C"><Warning /></el-icon></div>
            <div>
              <div class="stat-value">{{ factoryStats.potential }}</div>
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
          <el-input v-model="filterForm.factoryName" :placeholder="$t('factory.filter.factoryNamePlaceholder')" clearable style="width:180px" />
        </el-form-item>
        <el-form-item :label="$t('factory.filter.cooperationStatus')">
          <el-select v-model="filterForm.cooperationStatus" :placeholder="$t('factory.filter.cooperationStatusPlaceholder')" clearable style="width:160px">
            <el-option :label="$t('factory.cooperationStatus.ACTIVE')" value="ACTIVE" />
            <el-option :label="$t('factory.cooperationStatus.SUSPENDED')" value="SUSPENDED" />
            <el-option :label="$t('factory.cooperationStatus.ELIMINATED')" value="ELIMINATED" />
            <el-option :label="$t('factory.cooperationStatus.POTENTIAL')" value="POTENTIAL" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('factory.filter.province')">
          <el-input v-model="filterForm.province" :placeholder="$t('factory.filter.provincePlaceholder')" clearable style="width:120px" />
        </el-form-item>
        <el-form-item :label="$t('factory.filter.city')">
          <el-input v-model="filterForm.city" :placeholder="$t('factory.filter.cityPlaceholder')" clearable style="width:120px" />
        </el-form-item>
        <el-form-item :label="$t('factory.filter.county')">
          <el-input v-model="filterForm.county" :placeholder="$t('factory.filter.countyPlaceholder')" clearable style="width:120px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">{{ $t('factory.filter.search') }}</el-button>
          <el-button @click="onReset">{{ $t('factory.filter.reset') }}</el-button>
          <el-button type="primary" @click="onNew">
            <el-icon><Plus /></el-icon>{{ $t('factory.newButton') }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="tableData" stripe style="width:100%" min-height="200">
        <el-table-column prop="factoryCode" :label="$t('factory.column.factoryCode')" min-width="140" />
        <el-table-column prop="factoryName" :label="$t('factory.column.factoryName')" min-width="160" show-overflow-tooltip />
        <el-table-column prop="province" :label="$t('factory.column.province')" min-width="100" />
        <el-table-column prop="city" :label="$t('factory.column.city')" min-width="100" />
        <el-table-column prop="county" :label="$t('factory.column.county')" min-width="100" />
        <el-table-column prop="roughLocation" :label="$t('factory.column.roughLocation')" min-width="160" show-overflow-tooltip />
        <el-table-column prop="contactName" :label="$t('factory.column.contactName')" min-width="100" />
        <el-table-column prop="contactPhone" :label="$t('factory.column.contactPhone')" min-width="130" />
        <el-table-column prop="cooperationStatus" :label="$t('factory.column.cooperationStatus')" min-width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.cooperationStatus)" size="small">
              {{ statusLabel(row.cooperationStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('factory.column.action')" min-width="150" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="onView(row)">{{ $t('factory.action.detail') }}</el-button>
            <el-button link type="primary" size="small" @click.stop="onEdit(row)">{{ $t('factory.action.edit') }}</el-button>
            <el-button link type="danger" size="small" @click.stop="onDelete(row)">{{ $t('factory.action.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <el-pagination
          background
          :current-page="pagination.page"
          :page-size="pagination.pageSize"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @current-change="onPageChange"
        />
      </div>
    </el-card>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" :title="$t('factory.drawer.title')" size="600px" direction="rtl">
      <div v-if="currentRow" class="detail-grid">
        <div class="detail-section-title">{{ $t('factory.drawer.section.basic') }}</div>
        <div class="detail-item"><span class="detail-label">{{ $t('factory.drawer.factoryCode') }}</span><span class="detail-value">{{ currentRow.factoryCode }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('factory.drawer.factoryName') }}</span><span class="detail-value">{{ currentRow.factoryName }}</span></div>
        <div class="detail-section-title">{{ $t('factory.drawer.section.location') }}</div>
        <div class="detail-item"><span class="detail-label">{{ $t('factory.drawer.province') }}</span><span class="detail-value">{{ currentRow.province ?? $t('common.format.dash') }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('factory.drawer.city') }}</span><span class="detail-value">{{ currentRow.city ?? $t('common.format.dash') }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('factory.drawer.county') }}</span><span class="detail-value">{{ currentRow.county ?? $t('common.format.dash') }}</span></div>
        <div class="detail-item full-width"><span class="detail-label">{{ $t('factory.drawer.roughLocation') }}</span><span class="detail-value">{{ currentRow.roughLocation ?? $t('common.format.dash') }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('factory.drawer.longitude') }}</span><span class="detail-value">{{ currentRow.longitude ?? $t('common.format.dash') }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('factory.drawer.latitude') }}</span><span class="detail-value">{{ currentRow.latitude ?? $t('common.format.dash') }}</span></div>

        <div class="detail-section-title">{{ $t('factory.drawer.section.contact') }}</div>
        <div class="detail-item"><span class="detail-label">{{ $t('factory.drawer.contactName') }}</span><span class="detail-value">{{ currentRow.contactName ?? $t('common.format.dash') }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('factory.drawer.contactPhone') }}</span><span class="detail-value">{{ currentRow.contactPhone ?? $t('common.format.dash') }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('factory.drawer.contactWechat') }}</span><span class="detail-value">{{ currentRow.contactWechat ?? $t('common.format.dash') }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('factory.drawer.contactQq') }}</span><span class="detail-value">{{ currentRow.contactQq ?? $t('common.format.dash') }}</span></div>

        <div class="detail-section-title">{{ $t('factory.drawer.section.cooperation') }}</div>
        <div class="detail-item"><span class="detail-label">{{ $t('factory.drawer.cooperationStatus') }}</span><span class="detail-value">
          <el-tag :type="statusTagType(currentRow.cooperationStatus)" size="small">{{ statusLabel(currentRow.cooperationStatus) }}</el-tag>
        </span></div>
        <div class="detail-section-title">{{ $t('factory.drawer.section.audit') }}</div>
        <div class="detail-item"><span class="detail-label">{{ $t('factory.drawer.createBy') }}</span><span class="detail-value">{{ currentRow.createBy ?? $t('common.format.dash') }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('factory.drawer.createTime') }}</span><span class="detail-value">{{ currentRow.createTime ?? $t('common.format.dash') }}</span></div>
        <div class="detail-item"><span class="detail-label">{{ $t('factory.drawer.updateTime') }}</span><span class="detail-value">{{ currentRow.updateTime ?? $t('common.format.dash') }}</span></div>

        <div v-if="currentRow.notes" class="detail-item full-width">
          <span class="detail-label">{{ $t('factory.drawer.notes') }}</span>
          <span class="detail-value">{{ currentRow.notes }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">{{ $t('factory.drawer.close') }}</el-button>
        <el-button type="primary" @click="onEditFromDrawer">{{ $t('factory.drawer.edit') }}</el-button>
      </template>
    </el-drawer>

    <!-- 新规/编辑弹窗 -->
    <el-dialog v-model="formVisible" :title="formTitle" width="760px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="120px">
        <!-- 基础信息 -->
        <div class="dialog-section-title">{{ $t('factory.dialog.section.basic') }}</div>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('factory.dialog.factoryName')" prop="factoryName">
              <el-input v-model="form.factoryName" :placeholder="$t('factory.dialog.factoryNamePlaceholder')" maxlength="128" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 地理位置 -->
        <div class="dialog-section-title">{{ $t('factory.dialog.section.location') }}</div>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item :label="$t('factory.dialog.province')">
              <el-input v-model="form.province" :placeholder="$t('factory.dialog.provincePlaceholder')" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('factory.dialog.city')">
              <el-input v-model="form.city" :placeholder="$t('factory.dialog.cityPlaceholder')" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('factory.dialog.county')">
              <el-input v-model="form.county" :placeholder="$t('factory.dialog.countyPlaceholder')" maxlength="64" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="$t('factory.dialog.roughLocation')">
          <el-input v-model="form.roughLocation" :placeholder="$t('factory.dialog.roughLocationPlaceholder')" maxlength="500" />
        </el-form-item>

        <!-- 联系方式 -->
        <div class="dialog-section-title">{{ $t('factory.dialog.section.contact') }}</div>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('factory.dialog.contactName')">
              <el-input v-model="form.contactName" :placeholder="$t('factory.dialog.contactNamePlaceholder')" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('factory.dialog.contactPhone')">
              <el-input v-model="form.contactPhone" :placeholder="$t('factory.dialog.contactPhonePlaceholder')" maxlength="32" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('factory.dialog.contactWechat')">
              <el-input v-model="form.contactWechat" :placeholder="$t('factory.dialog.contactWechatPlaceholder')" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('factory.dialog.contactQq')">
              <el-input v-model="form.contactQq" :placeholder="$t('factory.dialog.contactQqPlaceholder')" maxlength="32" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 合作信息 -->
        <div class="dialog-section-title">{{ $t('factory.dialog.section.cooperation') }}</div>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('factory.dialog.cooperationStatus')">
              <el-select v-model="form.cooperationStatus" :placeholder="$t('factory.dialog.cooperationStatusPlaceholder')" clearable style="width:100%">
                <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 备注 -->
        <el-form-item :label="$t('factory.dialog.notes')">
          <el-input v-model="form.notes" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">{{ $t('factory.dialog.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">{{ $t('factory.dialog.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, OfficeBuilding, CircleCheck, Warning } from '@element-plus/icons-vue'
import { factoryApi } from '@/api/factory'
import type { FactoryPageVO, CreateFactoryRequest, UpdateFactoryRequest, CooperationStatus, FactoryStatsDTO } from '@/api/factory'

const { t } = useI18n()

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<FactoryPageVO[]>([])
const detailVisible = ref(false)
const formVisible = ref(false)
const currentRow = ref<FactoryPageVO | null>(null)
const isEdit = ref(false)
const formRef = ref()

const pagination = reactive({ page: 1, pageSize: 20, total: 0 })
const filterForm = reactive({ factoryName: '', cooperationStatus: '' as CooperationStatus | '', province: '', city: '', county: '' })

// 统计值（从 stats API 获取）
const factoryStats = ref<FactoryStatsDTO>({ total: 0, active: 0, potential: 0, suspended: 0, eliminated: 0 })

// 枚举选项
const statusOptions: { value: CooperationStatus; label: string }[] = [
  { value: 'ACTIVE', label: t('factory.cooperationStatus.ACTIVE') },
  { value: 'SUSPENDED', label: t('factory.cooperationStatus.SUSPENDED') },
  { value: 'ELIMINATED', label: t('factory.cooperationStatus.ELIMINATED') },
  { value: 'POTENTIAL', label: t('factory.cooperationStatus.POTENTIAL') },
]

const defaultForm = (): CreateFactoryRequest => ({
  factoryName: '',
  province: '',
  city: '',
  county: '',
  roughLocation: '',
  longitude: undefined,
  latitude: undefined,
  contactName: '',
  contactPhone: '',
  contactWechat: '',
  contactQq: '',
  cooperationStatus: undefined,
  notes: '',
})

const form = reactive<CreateFactoryRequest>(defaultForm())

const formRules = {
  factoryName: [{ required: true, message: () => t('factory.validation.factoryNameRequired'), trigger: 'blur' }],
}

const formTitle = computed(() => isEdit.value ? t('factory.dialog.editTitle') : t('factory.dialog.newTitle'))

async function loadData() {
  loading.value = true
  try {
    const res = await factoryApi.list({
      page: pagination.page - 1,
      pageSize: pagination.pageSize,
      factoryName: filterForm.factoryName || undefined,
      cooperationStatus: filterForm.cooperationStatus || undefined,
      province: filterForm.province || undefined,
      city: filterForm.city || undefined,
      county: filterForm.county || undefined,
    })
    const data = res.data.data
    tableData.value = data.content
    pagination.total = data.totalElements
  } finally {
    loading.value = false
  }
  loadStats()
}

async function loadStats() {
  try {
    const res = await factoryApi.stats()
    factoryStats.value = res.data.data
  } catch { /* ignore */ }
}

function onSearch() {
  pagination.page = 1
  loadData()
}

function onReset() {
  filterForm.factoryName = ''
  filterForm.cooperationStatus = ''
  filterForm.province = ''
  filterForm.city = ''
  filterForm.county = ''
  pagination.page = 1
  loadData()
  loadStats()
}

function onNew() {
  isEdit.value = false
  Object.assign(form, defaultForm())
  formVisible.value = true
}

function onView(row: FactoryPageVO) {
  currentRow.value = row
  detailVisible.value = true
}

function onEdit(row: FactoryPageVO) {
  isEdit.value = true
  currentRow.value = row
  detailVisible.value = false
  Object.assign(form, {
    factoryName: row.factoryName ?? '',
    province: row.province ?? '',
    city: row.city ?? '',
    county: row.county ?? '',
    roughLocation: row.roughLocation ?? '',
    longitude: row.longitude,
    latitude: row.latitude,
    contactName: row.contactName ?? '',
    contactPhone: row.contactPhone ?? '',
    contactWechat: row.contactWechat ?? '',
    contactQq: row.contactQq ?? '',
    cooperationStatus: row.cooperationStatus,
    notes: row.notes ?? '',
  })
  formVisible.value = true
}

function onEditFromDrawer() {
  if (currentRow.value) onEdit(currentRow.value)
}

async function onDelete(row: FactoryPageVO) {
  await ElMessageBox.confirm(
    t('factory.message.deleteConfirm', { name: row.factoryName, code: row.factoryCode }),
    t('factory.message.deleteConfirmTitle'),
  )
  await factoryApi.delete(row.id)
  ElMessage.success(t('factory.message.deleteSuccess'))
  loadData()
}

async function onSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value && currentRow.value) {
      await factoryApi.update(currentRow.value.id, form as UpdateFactoryRequest)
      ElMessage.success(t('factory.message.updateSuccess'))
    } else {
      await factoryApi.create(form)
      ElMessage.success(t('factory.message.createSuccess'))
    }
    formVisible.value = false
    loadData()
    loadStats()
  } finally {
    submitting.value = false
  }
}

function onPageChange(page: number) {
  pagination.page = page
  loadData()
}

// 辅助函数
function statusLabel(status?: CooperationStatus) {
  if (!status) return t('common.format.dash')
  return t(`factory.cooperationStatus.${status}`)
}

function statusTagType(status?: CooperationStatus) {
  switch (status) {
    case 'ACTIVE': return 'success'
    case 'SUSPENDED': return 'warning'
    case 'ELIMINATED': return 'danger'
    case 'POTENTIAL': return 'info'
    default: return 'info'
  }
}

loadData()
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.filter-card :deep(.el-card__body) { padding-bottom: 0; }
.table-card :deep(.el-card__body) { padding: 16px; }
.stats-row { margin-bottom: 4px; }
.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 16px;
  padding: 8px 16px;
}
.detail-section-title {
  grid-column: 1 / -1;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  border-bottom: 1px solid #EBEEF5;
  padding-bottom: 4px;
  margin-bottom: 4px;
}
.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.detail-item.full-width {
  grid-column: 1 / -1;
}
.detail-label {
  font-size: 12px;
  color: #909399;
}
.detail-value {
  font-size: 14px;
  color: #303133;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.dialog-section-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin: 8px 0 4px;
  padding-left: 4px;
  border-left: 3px solid #409EFF;
}
</style>
