<template>
  <div class="page">
    <div class="page-header">
      <h2 class="page-title">{{ $t('product.title') }}</h2>
      <div class="header-actions">
        <el-button type="primary" @click="onNew">
          <el-icon><Plus /></el-icon> {{ $t('product.newButton') }}
        </el-button>
      </div>
    </div>

    <!-- 统计行 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#E8650A"><Goods /></el-icon></div>
            <div>
              <div class="stat-value">{{ pagination.total }}</div>
              <div class="stat-label">{{ $t('product.stat.total') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filterForm">
        <el-form-item :label="$t('product.filter.masterCode')">
          <el-input v-model="filterForm.masterCode" :placeholder="$t('product.filter.masterCodePlaceholder')" clearable style="width:160px" />
        </el-form-item>
        <el-form-item :label="$t('product.filter.keyword')">
          <el-input v-model="filterForm.keyword" :placeholder="$t('product.filter.keywordPlaceholder')" clearable style="width:160px" />
        </el-form-item>
        <el-form-item :label="$t('product.filter.hsCode')">
          <el-input v-model="filterForm.hsCode" :placeholder="$t('product.filter.hsCode')" clearable style="width:140px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">{{ $t('product.filter.search') }}</el-button>
          <el-button @click="onReset">{{ $t('product.filter.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="tableData" stripe style="width:100%">
        <el-table-column prop="masterCode" :label="$t('product.column.masterCode')" width="120" />
        <el-table-column prop="subCode" :label="$t('product.column.subCode')" width="100" />
        <el-table-column prop="nameZh" :label="$t('product.column.nameZh')" min-width="140" show-overflow-tooltip />
        <el-table-column prop="nameEn" :label="$t('product.column.nameEn')" min-width="140" show-overflow-tooltip />
        <el-table-column prop="hsCode" :label="$t('product.column.hsCode')" width="120" />
        <el-table-column prop="unitPriceRmb" :label="$t('product.column.unitPriceRmb')" width="120" align="right">
          <template #default="{ row }">{{ row.unitPriceRmb != null ? `¥${Number(row.unitPriceRmb).toFixed(2)}` : $t('common.format.dash') }}</template>
        </el-table-column>
        <el-table-column prop="origin" :label="$t('product.column.origin')" width="90" />
        <el-table-column prop="material" :label="$t('product.column.material')" width="100" show-overflow-tooltip />
        <el-table-column prop="unit" :label="$t('product.column.unit')" width="70" align="center" />
        <el-table-column :label="$t('product.column.action')" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="onView(row)">{{ $t('product.action.detail') }}</el-button>
            <el-button link type="primary" size="small" @click.stop="onEdit(row)">{{ $t('product.action.edit') }}</el-button>
            <el-button link type="danger" size="small" @click.stop="onDelete(row)">{{ $t('product.action.delete') }}</el-button>
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
    <el-drawer v-model="detailVisible" :title="$t('product.drawer.title')" size="600px" direction="rtl">
      <div v-if="currentRow" class="drawer-content">
        <!-- 基本信息区 -->
        <div class="drawer-section-title">{{ $t('product.drawer.section.basicInfo') }}</div>
        <div class="detail-grid">
          <div class="detail-item"><span class="detail-label">{{ $t('product.drawer.masterCode') }}</span><span class="detail-value">{{ currentRow.masterCode }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('product.drawer.subCode') }}</span><span class="detail-value">{{ currentRow.subCode || $t('common.format.dash') }}</span></div>
          <div class="detail-item full-width"><span class="detail-label">{{ $t('product.drawer.nameZh') }}</span><span class="detail-value">{{ currentRow.nameZh || $t('common.format.dash') }}</span></div>
          <div class="detail-item full-width"><span class="detail-label">{{ $t('product.drawer.nameEn') }}</span><span class="detail-value">{{ currentRow.nameEn || $t('common.format.dash') }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('product.drawer.hsCode') }}</span><span class="detail-value">{{ currentRow.hsCode || $t('common.format.dash') }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('product.drawer.nameJa') }}</span><span class="detail-value">{{ currentRow.nameJa || $t('common.format.dash') }}</span></div>
        </div>

        <!-- 规格信息区 -->
        <div class="drawer-section-title">{{ $t('product.drawer.section.specInfo') }}</div>
        <div class="detail-grid">
          <div class="detail-item"><span class="detail-label">{{ $t('product.drawer.origin') }}</span><span class="detail-value">{{ currentRow.origin || $t('common.format.dash') }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('product.drawer.material') }}</span><span class="detail-value">{{ currentRow.material || $t('common.format.dash') }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('product.drawer.unit') }}</span><span class="detail-value">{{ currentRow.unit || $t('common.format.dash') }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('product.drawer.unitPriceRmb') }}</span><span class="detail-value">{{ currentRow.unitPriceRmb != null ? `¥${Number(currentRow.unitPriceRmb).toFixed(2)}` : $t('common.format.dash') }}</span></div>
          <div class="detail-item full-width"><span class="detail-label">{{ $t('product.drawer.lengthCm') }} × {{ $t('product.drawer.widthCm') }} × {{ $t('product.drawer.heightCm') }}</span><span class="detail-value">{{ dims(currentRow) }}</span></div>
          <div class="detail-item full-width"><span class="detail-label">{{ $t('product.drawer.netWeight') }} / {{ $t('product.drawer.grossWeight') }}</span><span class="detail-value">{{ weightStr(currentRow) }}</span></div>
        </div>

        <!-- 关联工厂区 -->
        <div class="drawer-section-title">
          {{ $t('product.drawer.section.factories') }}
          <span class="factory-count-badge" v-if="productFactories.length > 0">{{ productFactories.length }}</span>
        </div>
        <div v-if="factoriesLoading" class="factories-loading">
          <el-icon class="is-loading"><Loading /></el-icon> {{ $t('common.loading') }}
        </div>
        <div v-else-if="productFactories.length === 0" class="factories-empty">
          {{ $t('product.drawer.noFactories') }}
        </div>
        <div v-else class="factories-list">
          <div v-for="factory in productFactories" :key="factory.factoryId" class="factory-card">
            <div class="factory-header">
              <span class="factory-name">{{ factory.factoryName || $t('common.format.dash') }}</span>
              <el-tag v-if="factory.isPreferred" type="warning" size="small">{{ $t('product.drawer.preferred') }}</el-tag>
              <el-tag v-if="factory.cooperationStatus" size="small">{{ factory.cooperationStatus }}</el-tag>
            </div>
            <div class="factory-detail">
              <span>{{ $t('product.drawer.factoryCode') }}: {{ factory.factoryCode || '-' }}</span>
              <span>{{ $t('product.drawer.factoryLocation') }}: {{ [factory.province, factory.city].filter(Boolean).join('') || '-' }}</span>
              <span>{{ $t('product.drawer.contact') }}: {{ factory.contactName || '-' }}{{ factory.contactPhone ? ` (${factory.contactPhone})` : '' }}</span>
              <span>{{ $t('product.drawer.moq') }}: {{ factory.moq ?? '-' }}</span>
              <span>{{ $t('product.drawer.leadTimeDays') }}: {{ factory.leadTimeDays != null ? `${factory.leadTimeDays}${ $t('product.drawer.days') }` : '-' }}</span>
              <span>{{ $t('product.drawer.unitPrice') }}: {{ factory.unitPriceRmb != null ? `¥${Number(factory.unitPriceRmb).toFixed(2)}` : '-' }}</span>
            </div>
          </div>
        </div>

        <!-- 备注 -->
        <div class="drawer-section-title">{{ $t('product.drawer.remarks') }}</div>
        <div class="detail-remarks">
          {{ currentRow.remarks || $t('common.format.dash') }}
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">{{ $t('product.drawer.close') }}</el-button>
        <el-button type="primary" @click="onEditFromDrawer">{{ $t('product.drawer.edit') }}</el-button>
      </template>
    </el-drawer>

    <!-- 新规/编辑弹窗 -->
    <el-dialog v-model="formVisible" :title="formTitle" width="720px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('product.dialog.masterCode')" prop="masterCode">
              <el-input v-model="form.masterCode" :placeholder="$t('product.dialog.masterCodePlaceholder')" maxlength="32" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('product.dialog.subCode')">
              <el-input v-model="form.subCode" :placeholder="$t('product.dialog.subCodePlaceholder')" maxlength="64" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('product.dialog.nameZh')" prop="nameZh">
              <el-input v-model="form.nameZh" :placeholder="$t('product.dialog.nameZhPlaceholder')" maxlength="255" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('product.dialog.nameEn')">
              <el-input v-model="form.nameEn" :placeholder="$t('product.dialog.nameEnPlaceholder')" maxlength="255" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('product.dialog.nameJa')">
              <el-input v-model="form.nameJa" :placeholder="$t('product.dialog.nameJaPlaceholder')" maxlength="128" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('product.dialog.hsCode')">
              <el-input v-model="form.hsCode" :placeholder="$t('product.dialog.hsCodePlaceholder')" maxlength="20" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item :label="$t('product.dialog.unitPriceRmb')">
              <el-input-number v-model="form.unitPriceRmb" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('product.dialog.taxPoint')">
              <el-input-number v-model="form.taxPoint" :min="0" :max="10" :precision="4" style="width:100%" :placeholder="$t('product.dialog.taxPointPlaceholder')" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('product.dialog.taxRate')">
              <el-input-number v-model="form.taxRate" :min="0" :max="1" :precision="4" style="width:100%" :placeholder="$t('product.dialog.taxRatePlaceholder')" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item :label="$t('product.dialog.lengthCm')">
              <el-input-number v-model="form.lengthCm" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('product.dialog.widthCm')">
              <el-input-number v-model="form.widthCm" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('product.dialog.heightCm')">
              <el-input-number v-model="form.heightCm" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item :label="$t('product.dialog.netWeight')">
              <el-input-number v-model="form.netWeightKg" :min="0" :precision="4" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('product.dialog.grossWeight')">
              <el-input-number v-model="form.grossWeightKg" :min="0" :precision="4" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('product.dialog.unit')">
              <el-input v-model="form.unit" :placeholder="$t('product.dialog.unitPlaceholder')" maxlength="50" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item :label="$t('product.dialog.origin')">
          <el-input v-model="form.origin" :placeholder="$t('product.dialog.originPlaceholder')" maxlength="100" />
        </el-form-item>

        <el-form-item :label="$t('product.dialog.declarationElements')">
          <el-input v-model="form.declarationElements" type="textarea" :rows="2" :placeholder="$t('product.dialog.declarationElementsPlaceholder')" />
        </el-form-item>

        <el-form-item :label="$t('product.dialog.remarks')">
          <el-input v-model="form.remarks" type="textarea" :rows="2" maxlength="512" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">{{ $t('product.dialog.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">{{ $t('product.dialog.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Goods, Loading } from '@element-plus/icons-vue'
import { productApi } from '@/api/product'
import type { ProductPageVO, CreateProductRequest, UpdateProductRequest, ProductFactoryVO } from '@/api/product'

const { t } = useI18n()

const loading = ref(false)
const submitting = ref(false)
const factoriesLoading = ref(false)
const tableData = ref<ProductPageVO[]>([])
const productFactories = ref<ProductFactoryVO[]>([])
const detailVisible = ref(false)
const formVisible = ref(false)
const currentRow = ref<ProductPageVO | null>(null)
const isEdit = ref(false)
const formRef = ref()

const pagination = reactive({ page: 1, pageSize: 20, total: 0 })

const filterForm = reactive({ masterCode: '', keyword: '', hsCode: '' })

const defaultForm = (): CreateProductRequest => ({
  masterCode: '',
  subCode: '',
  nameJa: '',
  nameEn: '',
  nameZh: '',
  origin: '',
  hsCode: '',
  unitPriceRmb: undefined,
  taxPoint: 1.1,
  taxRate: 0.1,
  lengthCm: undefined,
  widthCm: undefined,
  heightCm: undefined,
  netWeightKg: undefined,
  grossWeightKg: undefined,
  unit: '',
  declarationElements: '',
  remarks: '',
})

const form = reactive<CreateProductRequest>(defaultForm())

const formRules = {
  masterCode: [{ required: true, message: () => t('product.validation.masterCodeRequired'), trigger: 'blur' }],
  nameZh: [{ required: true, message: () => t('product.validation.nameZhRequired'), trigger: 'blur' }],
}

const formTitle = computed(() => isEdit.value ? t('product.dialog.editTitle') : t('product.dialog.newTitle'))

async function loadData() {
  loading.value = true
  try {
    const res = await productApi.list({
      page: pagination.page - 1,
      pageSize: pagination.pageSize,
      masterCode: filterForm.masterCode || undefined,
      keyword: filterForm.keyword || undefined,
      hsCode: filterForm.hsCode || undefined,
    })
    const data = res.data.data
    tableData.value = data.content
    pagination.total = data.totalElements
  } finally {
    loading.value = false
  }
}

function onReset() {
  filterForm.masterCode = ''
  filterForm.keyword = ''
  filterForm.hsCode = ''
  pagination.page = 1
  loadData()
}

function onNew() {
  isEdit.value = false
  Object.assign(form, defaultForm())
  formVisible.value = true
}

async function onView(row: ProductPageVO) {
  currentRow.value = row
  detailVisible.value = true
  productFactories.value = []
  factoriesLoading.value = true
  try {
    const res = await productApi.getProductFactories(row.id)
    productFactories.value = res.data.data || []
  } catch {
    productFactories.value = []
  } finally {
    factoriesLoading.value = false
  }
}

function onEdit(row: ProductPageVO) {
  isEdit.value = true
  currentRow.value = row
  detailVisible.value = false
  Object.assign(form, {
    masterCode: row.masterCode,
    subCode: row.subCode || '',
    nameJa: row.nameJa || '',
    nameEn: row.nameEn || '',
    nameZh: row.nameZh || '',
    origin: row.origin || '',
    hsCode: row.hsCode || '',
    unitPriceRmb: row.unitPriceRmb,
    taxPoint: row.taxPoint ?? 1.1,
    taxRate: row.taxRate ?? 0.1,
    lengthCm: row.lengthCm,
    widthCm: row.widthCm,
    heightCm: row.heightCm,
    netWeightKg: row.netWeightKg,
    grossWeightKg: row.grossWeightKg,
    unit: row.unit || '',
    declarationElements: row.declarationElements || '',
    remarks: row.remarks || '',
  })
  formVisible.value = true
}

function onEditFromDrawer() {
  if (currentRow.value) onEdit(currentRow.value)
}

async function onDelete(row: ProductPageVO) {
  await ElMessageBox.confirm(
    t('product.message.deleteConfirm', { code: row.masterCode }),
    t('product.message.deleteConfirmTitle'),
  )
  await productApi.delete(row.id)
  ElMessage.success(t('product.message.deleteSuccess'))
  loadData()
}

async function onSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value && currentRow.value) {
      await productApi.update(currentRow.value.id, form as UpdateProductRequest)
      ElMessage.success(t('product.message.updateSuccess'))
    } else {
      await productApi.create(form)
      ElMessage.success(t('product.message.createSuccess'))
    }
    formVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

function onPageChange(page: number) {
  pagination.page = page
  loadData()
}

function dims(row: ProductPageVO) {
  const l = row.lengthCm, w = row.widthCm, h = row.heightCm
  if (l || w || h) return `${l ?? t('common.format.dash')} × ${w ?? t('common.format.dash')} × ${h ?? t('common.format.dash')} cm`
  return t('common.format.dash')
}

function weightStr(row: ProductPageVO) {
  const n = row.netWeightKg, g = row.grossWeightKg
  if (n || g) return `${n ?? t('common.format.dash')}kg / ${g ?? t('common.format.dash')}kg`
  return t('common.format.dash')
}

loadData()
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.drawer-content {
  padding: 0 16px;
}
.drawer-section-title {
  font-size: 13px;
  font-weight: 600;
  color: #1A1A2E;
  margin: 16px 0 10px;
  padding-bottom: 6px;
  border-bottom: 1px solid #E8ECF1;
  display: flex;
  align-items: center;
  gap: 8px;
}
.factory-count-badge {
  background: var(--color-primary);
  color: #fff;
  border-radius: 10px;
  padding: 0 6px;
  font-size: 11px;
  font-weight: 600;
  line-height: 18px;
}
.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  padding: 0;
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
.factories-loading,
.factories-empty {
  text-align: center;
  padding: 20px;
  color: #909399;
  font-size: 13px;
}
.factories-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.factory-card {
  border: 1px solid #E8ECF1;
  border-radius: 8px;
  padding: 12px;
  background: #FAFBFC;
}
.factory-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.factory-name {
  font-weight: 600;
  font-size: 14px;
  color: #1A1A2E;
  flex: 1;
}
.factory-detail {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px 16px;
  font-size: 12px;
  color: #606266;
}
.detail-remarks {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  padding: 4px 0;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
