<template>
  <div class="page">
    <!-- 筛选 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filter">
        <el-form-item :label="$t('cargoSize.filter.status')">
          <el-select v-model="filter.status" clearable style="width:140px">
            <el-option :label="$t('common.all')" value="" />
            <el-option :label="$t('cargoSize.status.PENDING')" value="PENDING" />
            <el-option :label="$t('cargoSize.status.PROMOTED')" value="PROMOTED" />
            <el-option :label="$t('cargoSize.status.DISCARDED')" value="DISCARDED" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('cargoSize.filter.keyword')">
          <el-input v-model="filter.keyword" :placeholder="$t('cargoSize.filter.keywordPlaceholder')" clearable style="width:180px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">{{ $t('common.button.search') }}</el-button>
          <el-button @click="onReset">{{ $t('common.button.reset') }}</el-button>
          <el-button type="success" @click="onCreate" v-if="hasPermission('cargo_size:create')">
            {{ $t('cargoSize.action.create') }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card class="table-card" shadow="never">
      <el-table :data="tableData" v-loading="loading" stripe style="width:100%">
        <el-table-column :label="$t('cargoSize.column.code')" prop="code" min-width="140">
          <template #default="{ row }">
            <el-tag type="info" size="small">{{ row.code }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('cargoSize.column.heightCm')" prop="heightCm" min-width="80" align="right" />
        <el-table-column :label="$t('cargoSize.column.widthCm')" prop="widthCm" min-width="80" align="right" />
        <el-table-column :label="$t('cargoSize.column.lengthCm')" prop="lengthCm" min-width="80" align="right" />
        <el-table-column :label="$t('cargoSize.column.dimensionTotal')" min-width="100" align="right">
          <template #default="{ row }">{{ calcDimensionTotal(row) }}</template>
        </el-table-column>
        <el-table-column :label="$t('cargoSize.column.netWeightKg')" prop="netWeightKg" min-width="90" align="right" />
        <el-table-column :label="$t('cargoSize.column.volumeCbm')" min-width="100" align="right">
          <template #default="{ row }">{{ calcVolume(row) }}</template>
        </el-table-column>
        <el-table-column :label="$t('cargoSize.column.unitsPerPackage')" prop="unitsPerPackage" min-width="90" align="right" />
        <el-table-column :label="$t('cargoSize.column.packHeightCm')" prop="packHeightCm" min-width="100" align="right" />
        <el-table-column :label="$t('cargoSize.column.packWidthCm')" prop="packWidthCm" min-width="100" align="right" />
        <el-table-column :label="$t('cargoSize.column.packDepthCm')" prop="packDepthCm" min-width="100" align="right" />
        <el-table-column :label="$t('cargoSize.column.packageDimTotal')" min-width="110" align="right">
          <template #default="{ row }">{{ calcPackageDimTotal(row) }}</template>
        </el-table-column>
        <el-table-column :label="$t('cargoSize.column.packageWeightKg')" prop="packageWeightKg" min-width="110" align="right" />
        <el-table-column :label="$t('cargoSize.column.remarks')" prop="remarks" min-width="140">
          <template #default="{ row }">{{ row.remarks || '-' }}</template>
        </el-table-column>
        <el-table-column :label="$t('cargoSize.column.action')" min-width="200">
          <template #default="{ row }">
            <el-button link class="btn-blue" size="small" @click="onDetail(row)">{{ $t('cargoSize.action.detail') }}</el-button>
            <el-button v-if="hasPermission('cargo_size:update')" text size="small" type="warning" @click="onEdit(row)">
              {{ $t('cargoSize.action.edit') }}
            </el-button>
            <el-button v-if="hasPermission('cargo_size:delete')" text size="small" type="danger" @click="onDelete(row)">
              {{ $t('common.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination-bar"
        background
        layout="total, prev, pager, next"
        :total="total"
        :page-size="filter.size"
        :current-page="filter.page + 1"
        @current-change="onPageChange"
      />
    </el-card>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" :title="$t('cargoSize.drawer.title')" size="680px" direction="rtl" :before-close="() => detailVisible = false">
      <div class="drawer-content" v-if="currentRow">
        <div class="drawer-section-title">{{ $t('cargoSize.drawer.section.basic') }}</div>
        <div class="detail-grid">
          <div class="detail-item"><span class="detail-label">{{ $t('cargoSize.column.code') }}</span><span class="detail-value"><el-tag type="info" size="small">{{ currentRow.code }}</el-tag></span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('cargoSize.column.subCode') }}</span><span class="detail-value">{{ currentRow.subCode || '-' }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('cargoSize.masterCode') }}</span><span class="detail-value">{{ currentRow.masterCode }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('cargoSize.column.updateTime') }}</span><span class="detail-value">{{ formatDate(currentRow.updateTime) }}</span></div>
        </div>
        <div class="drawer-section-title">{{ $t('cargoSize.drawer.section.dims') }}</div>
        <div class="detail-grid">
          <div class="detail-item"><span class="detail-label">{{ $t('cargoSize.lengthCm') }}</span><span class="detail-value">{{ currentRow.lengthCm ?? '-' }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('cargoSize.widthCm') }}</span><span class="detail-value">{{ currentRow.widthCm ?? '-' }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('cargoSize.heightCm') }}</span><span class="detail-value">{{ currentRow.heightCm ?? '-' }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('cargoSize.netWeightKg') }}</span><span class="detail-value">{{ currentRow.netWeightKg != null ? currentRow.netWeightKg + ' kg' : '-' }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('cargoSize.dimensionTotal') }}</span><span class="detail-value">{{ calcDimensionTotal(currentRow) || '-' }}</span></div>
          <div class="detail-item full-width"><span class="detail-label">{{ $t('cargoSize.volumeCbm') }}</span><span class="detail-value">{{ calcVolume(currentRow) || '-' }}</span></div>
        </div>
        <div class="drawer-section-title">{{ $t('cargoSize.drawer.section.package') }}</div>
        <div class="detail-grid">
          <div class="detail-item"><span class="detail-label">{{ $t('cargoSize.packHeightCm') }}</span><span class="detail-value">{{ currentRow.packHeightCm ?? '-' }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('cargoSize.packWidthCm') }}</span><span class="detail-value">{{ currentRow.packWidthCm ?? '-' }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('cargoSize.packDepthCm') }}</span><span class="detail-value">{{ currentRow.packDepthCm ?? '-' }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('cargoSize.packageDimTotal') }}</span><span class="detail-value">{{ calcPackageDimTotal(currentRow) || '-' }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('cargoSize.packageWeightKg') }}</span><span class="detail-value">{{ currentRow.packageWeightKg ?? '-' }}</span></div>
          <div class="detail-item"><span class="detail-label">{{ $t('cargoSize.unitsPerPackage') }}</span><span class="detail-value">{{ currentRow.unitsPerPackage ?? '-' }}</span></div>
        </div>
        <div class="drawer-section-title">{{ $t('cargoSize.drawer.section.remarks') }}</div>
        <div class="detail-remarks">{{ currentRow.remarks || '-' }}</div>

        <div class="drawer-footer">
          <el-button @click="detailVisible = false">{{ $t('common.button.close') }}</el-button>
          <el-button v-if="currentRow.status === 'PENDING' && hasPermission('cargo_size:promote')" type="primary" @click="onPromote(currentRow)">
            {{ $t('cargoSize.action.promote') }}
          </el-button>
          <el-button v-if="currentRow.status === 'PENDING' && hasPermission('cargo_size:discard')" type="danger" @click="onDiscard(currentRow)">
            {{ $t('cargoSize.action.discard') }}
          </el-button>
        </div>
      </div>
    </el-drawer>

    <!-- 升格弹窗 -->
    <el-dialog v-model="promoteVisible" :title="$t('cargoSize.dialog.promoteTitle')" width="840px" :close-on-click-modal="false">
      <div class="dialog-info-box" v-if="currentRow">
        {{ $t('cargoSize.code') }}: {{ currentRow.code }} |
        {{ $t('cargoSize.lengthCm') }}: {{ currentRow.lengthCm }} × {{ $t('cargoSize.widthCm') }}: {{ currentRow.widthCm }} × {{ $t('cargoSize.heightCm') }}: {{ currentRow.heightCm }} {{ $t('common.units.cm') }} /
        {{ $t('cargoSize.netWeightKg') }}: {{ currentRow.netWeightKg }} kg
      </div>
      <el-form ref="promoteFormRef" :model="promoteForm" :rules="promoteRules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item :label="$t('cargoSize.dialog.nameZh')" prop="nameZh">
              <el-input v-model="promoteForm.nameZh" maxlength="255" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('cargoSize.dialog.nameEn')">
              <el-input v-model="promoteForm.nameEn" maxlength="255" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('cargoSize.dialog.category')">
              <el-select v-model="promoteForm.category" style="width:100%">
                <el-option v-for="c in categoryOptions" :key="c.value" :label="c.label" :value="c.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('cargoSize.dialog.unit')">
              <el-input v-model="promoteForm.unit" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('cargoSize.dialog.unitPriceRmb')">
              <el-input-number v-model="promoteForm.unitPriceRmb" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('cargoSize.dialog.origin')">
              <el-input v-model="promoteForm.origin" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('cargoSize.dialog.hsCode')">
              <el-input v-model="promoteForm.hsCode" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item :label="$t('cargoSize.dialog.factoryIds')">
              <el-select v-model="promoteForm.factoryIds" multiple remote filterable :placeholder="$t('cargoSize.dialog.factorySearchPlaceholder')" style="width:100%" remote-show-suffix>
                <el-option v-for="f in factoryOptions" :key="f.id" :label="f.factoryName" :value="f.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item :label="$t('cargoSize.dialog.remarks')">
              <el-input v-model="promoteForm.remarks" type="textarea" :rows="2" maxlength="512" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="promoteVisible = false">{{ $t('common.button.cancel') }}</el-button>
        <el-button type="primary" :loading="promoteSubmitting" @click="onPromoteSubmit">
          {{ $t('cargoSize.action.promote') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 编辑货物尺寸弹窗 -->
    <el-dialog v-model="editVisible" :title="$t('cargoSize.dialog.editCargoSizeTitle')" width="640px" :close-on-click-modal="false">
      <el-form ref="editFormRef" :model="editForm" label-width="130px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('cargoSize.lengthCm')">
              <el-input-number v-model="editForm.lengthCm" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('cargoSize.widthCm')">
              <el-input-number v-model="editForm.widthCm" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('cargoSize.heightCm')">
              <el-input-number v-model="editForm.heightCm" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('cargoSize.netWeightKg')">
              <el-input-number v-model="editForm.netWeightKg" :min="0" :precision="4" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('cargoSize.packHeightCm')">
              <el-input-number v-model="editForm.packHeightCm" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('cargoSize.packWidthCm')">
              <el-input-number v-model="editForm.packWidthCm" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('cargoSize.packDepthCm')">
              <el-input-number v-model="editForm.packDepthCm" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('cargoSize.packageWeightKg')">
              <el-input-number v-model="editForm.packageWeightKg" :min="0" :precision="4" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('cargoSize.unitsPerPackage')">
              <el-input-number v-model="editForm.unitsPerPackage" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item :label="$t('cargoSize.dialog.remarks')">
              <el-input v-model="editForm.remarks" type="textarea" :rows="2" maxlength="512" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">{{ $t('common.button.cancel') }}</el-button>
        <el-button type="primary" :loading="editSubmitting" @click="onEditSubmit">
          {{ $t('common.button.save') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 新增货物尺寸弹窗 -->
    <el-dialog v-model="createVisible" :title="$t('cargoSize.dialog.createTitle')" width="640px" :close-on-click-modal="false">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="130px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('cargoSize.masterCode')" prop="masterCode">
              <el-input v-model="createForm.masterCode" maxlength="32" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('cargoSize.column.code')" prop="code">
              <el-input v-model="createForm.code" maxlength="96" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('cargoSize.column.subCode')">
              <el-input v-model="createForm.subCode" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('cargoSize.lengthCm')">
              <el-input-number v-model="createForm.lengthCm" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('cargoSize.widthCm')">
              <el-input-number v-model="createForm.widthCm" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('cargoSize.heightCm')">
              <el-input-number v-model="createForm.heightCm" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('cargoSize.netWeightKg')">
              <el-input-number v-model="createForm.netWeightKg" :min="0" :precision="4" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('cargoSize.packHeightCm')">
              <el-input-number v-model="createForm.packHeightCm" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('cargoSize.packWidthCm')">
              <el-input-number v-model="createForm.packWidthCm" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('cargoSize.packDepthCm')">
              <el-input-number v-model="createForm.packDepthCm" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('cargoSize.packageWeightKg')">
              <el-input-number v-model="createForm.packageWeightKg" :min="0" :precision="4" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('cargoSize.unitsPerPackage')">
              <el-input-number v-model="createForm.unitsPerPackage" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item :label="$t('cargoSize.dialog.remarks')">
              <el-input v-model="createForm.remarks" type="textarea" :rows="2" maxlength="512" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">{{ $t('common.button.cancel') }}</el-button>
        <el-button type="primary" :loading="createSubmitting" @click="onCreateSubmit">
          {{ $t('common.button.save') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { usePermission } from '@/composables/usePermission'
import {
  getCargoSizes,
  promoteCargoSize,
  discardCargoSize,
  updateCargoSize,
  deleteCargoSize,
  createCargoSize,
  type CargoSizeVO,
  type CargoSizeUpdateCmd,
  type CargoSizeCreateCmd,
} from '@/api/cargoSize'
import { factoryApi } from '@/api/factory'

const { t } = useI18n()
const { hasPermission } = usePermission()

// ---- Table ----
const filter = reactive({ keyword: '', status: '', page: 0, size: 20 })
const loading = ref(false)
const tableData = ref<CargoSizeVO[]>([])
const total = ref(0)

// ---- Detail drawer ----
const detailVisible = ref(false)
const currentRow = ref<any>(null)

// ---- Promote dialog ----
const promoteVisible = ref(false)
const promoteSubmitting = ref(false)
const promoteFormRef = ref()
const promoteForm = reactive({
  nameZh: '',
  nameEn: '',
  category: 'ORDINARY',
  unit: '个',
  unitPriceRmb: undefined as number | undefined,
  origin: '',
  factoryIds: [] as number[],
  hsCode: '',
  remarks: '',
})
const promoteRules = {
  nameZh: [{ required: true, message: t('cargoSize.validation.nameZhRequired'), trigger: 'blur' }],
}

// ---- Edit dialog ----
const editVisible = ref(false)
const editSubmitting = ref(false)
const editFormRef = ref()
const editForm = reactive<CargoSizeUpdateCmd>({
  lengthCm: undefined,
  widthCm: undefined,
  heightCm: undefined,
  netWeightKg: undefined,
  packHeightCm: undefined,
  packWidthCm: undefined,
  packDepthCm: undefined,
  packageWeightKg: undefined,
  unitsPerPackage: undefined,
  remarks: '',
})

// ---- Factory options ----
const factoryOptions = ref<{ id: number; factoryName: string }[]>([])

// ---- Create dialog ----
const createVisible = ref(false)
const createSubmitting = ref(false)
const createFormRef = ref()
const createForm = reactive<CargoSizeCreateCmd>({
  masterCode: '',
  subCode: '',
  code: '',
  lengthCm: undefined,
  widthCm: undefined,
  heightCm: undefined,
  netWeightKg: undefined,
  packHeightCm: undefined,
  packWidthCm: undefined,
  packDepthCm: undefined,
  packageWeightKg: undefined,
  unitsPerPackage: undefined,
  remarks: '',
})
const createRules = {
  masterCode: [{ required: true, message: t('cargoSize.validation.masterCodeRequired'), trigger: 'blur' }],
  code: [{ required: true, message: t('cargoSize.validation.codeRequired'), trigger: 'blur' }],
}

// ---- Category options ----
const categoryOptions = [
  { value: 'ORDINARY', label: t('product.category.ORDINARY') },
  { value: 'OEM', label: t('product.category.OEM') },
  { value: 'FACTORY_DIRECT', label: t('product.category.FACTORY_DIRECT') },
  { value: 'NORMAL', label: t('product.category.NORMAL') },
  { value: 'SAMPLE', label: t('product.category.SAMPLE') },
  { value: 'SELF_USE', label: t('product.category.SELF_USE') },
  { value: 'PARTS', label: t('product.category.PARTS') },
  { value: 'INDEPENDENT', label: t('product.category.INDEPENDENT') },
]

// ---- Helpers ----
function calcVolume(row: any) {
  if (row.lengthCm && row.widthCm && row.heightCm) {
    const v = (row.lengthCm * row.widthCm * row.heightCm) / 1000000
    return v.toFixed(6)
  }
  return null
}

function calcDimensionTotal(row: any) {
  if (row.lengthCm && row.widthCm && row.heightCm) {
    return (row.lengthCm + row.widthCm + row.heightCm).toFixed(2)
  }
  return null
}

function calcPackageDimTotal(row: any) {
  if (row.packHeightCm && row.packWidthCm && row.packDepthCm) {
    return (row.packHeightCm + row.packWidthCm + row.packDepthCm).toFixed(2)
  }
  return null
}

function formatDate(d: string | undefined) {
  if (!d) return '-'
  return d.replace('T', ' ').slice(0, 16)
}

// ---- Load data ----
async function loadData() {
  loading.value = true
  try {
    const res = await getCargoSizes(filter)
    tableData.value = res.data?.content ?? []
    total.value = res.data?.totalElements ?? 0
  } catch {
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function loadFactoryOptions() {
  try {
    const res = await factoryApi.list({ pageSize: 9999 })
    factoryOptions.value = (res.data?.content ?? []).map((f: any) => ({ id: f.id, factoryName: f.factoryName }))
  } catch {
    factoryOptions.value = []
  }
}

// ---- Actions ----
function onReset() {
  filter.keyword = ''
  filter.status = ''
  filter.page = 0
  loadData()
}

function onPageChange(page: number) {
  filter.page = page - 1
  loadData()
}

function onDetail(row: any) {
  currentRow.value = row
  detailVisible.value = true
}

function onPromote(row: any) {
  currentRow.value = row
  promoteForm.nameZh = ''
  promoteForm.nameEn = ''
  promoteForm.category = 'ORDINARY'
  promoteForm.unit = '个'
  promoteForm.unitPriceRmb = undefined
  promoteForm.origin = ''
  promoteForm.factoryIds = []
  promoteForm.hsCode = ''
  promoteForm.remarks = ''
  promoteVisible.value = true
}

async function onPromoteSubmit() {
  try {
    await promoteFormRef.value.validate()
  } catch {
    return
  }
  if (!currentRow.value) return
  promoteSubmitting.value = true
  try {
    await promoteCargoSize(currentRow.value.id, { ...promoteForm })
    ElMessage.success(t('cargoSize.confirm.promoteSuccess'))
    promoteVisible.value = false
    detailVisible.value = false
    await loadData()
  } finally {
    promoteSubmitting.value = false
  }
}

async function onDiscard(row: any) {
  try {
    await ElMessageBox.confirm(t('cargoSize.confirm.discard'), t('cargoSize.action.discard'), {
      confirmButtonText: t('common.button.confirm'),
      cancelButtonText: t('common.button.cancel'),
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await discardCargoSize(row.id)
    ElMessage.success(t('cargoSize.confirm.discardSuccess'))
    detailVisible.value = false
    await loadData()
  } catch {}
}

function onEdit(row: any) {
  currentRow.value = row
  editForm.lengthCm = row.lengthCm
  editForm.widthCm = row.widthCm
  editForm.heightCm = row.heightCm
  editForm.netWeightKg = row.netWeightKg
  editForm.packHeightCm = row.packHeightCm
  editForm.packWidthCm = row.packWidthCm
  editForm.packDepthCm = row.packDepthCm
  editForm.packageWeightKg = row.packageWeightKg
  editForm.unitsPerPackage = row.unitsPerPackage
  editForm.remarks = row.remarks || ''
  editVisible.value = true
}

async function onEditSubmit() {
  if (!currentRow.value) return
  editSubmitting.value = true
  try {
    await updateCargoSize(currentRow.value.id, { ...editForm })
    ElMessage.success(t('cargoSize.confirm.editSuccess'))
    editVisible.value = false
    detailVisible.value = false
    await loadData()
  } finally {
    editSubmitting.value = false
  }
}

async function onDelete(row: any) {
  try {
    await ElMessageBox.confirm(t('cargoSize.confirm.delete'), t('common.delete'), {
      confirmButtonText: t('common.button.confirm'),
      cancelButtonText: t('common.button.cancel'),
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await deleteCargoSize(row.id)
    ElMessage.success(t('cargoSize.confirm.deleteSuccess'))
    detailVisible.value = false
    await loadData()
  } catch {}
}

function onCreate() {
  createForm.masterCode = ''
  createForm.subCode = ''
  createForm.code = ''
  createForm.lengthCm = undefined
  createForm.widthCm = undefined
  createForm.heightCm = undefined
  createForm.netWeightKg = undefined
  createForm.packHeightCm = undefined
  createForm.packWidthCm = undefined
  createForm.packDepthCm = undefined
  createForm.packageWeightKg = undefined
  createForm.unitsPerPackage = undefined
  createForm.remarks = ''
  createVisible.value = true
}

async function onCreateSubmit() {
  try {
    await createFormRef.value.validate()
  } catch {
    return
  }
  createSubmitting.value = true
  try {
    await createCargoSize({ ...createForm })
    ElMessage.success(t('cargoSize.confirm.createSuccess'))
    createVisible.value = false
    await loadData()
  } finally {
    createSubmitting.value = false
  }
}

onMounted(async () => {
  await loadData()
  await loadFactoryOptions()
})
</script>

<style scoped>
.page {
  padding: 16px;
}
.filter-card {
  margin-bottom: 12px;
}
.table-card {
  margin-bottom: 12px;
}
.pagination-bar {
  margin-top: 12px;
  justify-content: flex-end;
}
.drawer-content {
  padding: 0 20px;
}
.drawer-section-title {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin: 16px 0 8px;
  padding-bottom: 6px;
  border-bottom: 1px solid #ebeef5;
}
.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.detail-item {
  display: flex;
  gap: 8px;
  font-size: 13px;
}
.detail-item.full-width {
  grid-column: 1 / -1;
}
.detail-label {
  color: #909399;
  flex-shrink: 0;
}
.detail-value {
  color: #303133;
  word-break: break-all;
}
.detail-remarks {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  white-space: pre-wrap;
}
.drawer-footer {
  margin-top: 24px;
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}
.dialog-info-box {
  background: #f5f7fa;
  border-radius: 4px;
  padding: 8px 12px;
  font-size: 13px;
  color: #606266;
  margin-bottom: 16px;
}
:deep(.el-table__header-wrapper),
:deep(.el-table__header th.el-table__cell) {
  position: sticky !important;
  top: 0 !important;
  z-index: 10 !important;
  background: inherit;
}
</style>