<template>
  <div class="page">
    <!-- Filter Card -->
    <el-card class="filter-card" :body-style="{ padding: '12px 16px' }">
      <el-form :inline="true" :model="filter" @submit.prevent>
        <el-form-item :label="$t('offlineOrder.filter.code')">
          <el-input v-model="filter.code" :placeholder="$t('offlineOrder.filter.codeHint')" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item :label="$t('offlineOrder.filter.location')">
          <el-input v-model="filter.location" :placeholder="$t('offlineOrder.filter.locationHint')" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item :label="$t('offlineOrder.filter.souko')">
          <el-input v-model="filter.souko" :placeholder="$t('offlineOrder.filter.soukoHint')" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item :label="$t('offlineOrder.filter.destination')">
          <el-input v-model="filter.destination" :placeholder="$t('offlineOrder.filter.destinationHint')" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">{{ $t('offlineOrder.filter.search') }}</el-button>
          <el-button @click="onReset">{{ $t('offlineOrder.filter.reset') }}</el-button>
          <el-button type="primary" @click="onCreate" v-if="hasPermission('offline_order:create')">
            <el-icon><Plus /></el-icon>{{ $t('offlineOrder.newButton') }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Table Card -->
    <el-card class="table-card" :body-style="{ padding: '0' }">
      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%">
        <el-table-column type="selection" width="40" align="center" />
        <el-table-column :label="$t('offlineOrder.column.code')" prop="code" show-overflow-tooltip min-width="120" />
        <el-table-column :label="$t('offlineOrder.column.manager')" prop="manager" show-overflow-tooltip min-width="100" />
        <el-table-column :label="$t('offlineOrder.column.destination')" prop="destination" show-overflow-tooltip min-width="100" />
        <el-table-column :label="$t('offlineOrder.column.tax')" prop="tax" show-overflow-tooltip min-width="80" />
        <el-table-column :label="$t('offlineOrder.column.material')" prop="material" show-overflow-tooltip min-width="100" />
        <el-table-column :label="$t('offlineOrder.column.kensa')" prop="kensa" show-overflow-tooltip min-width="80" />
        <el-table-column :label="$t('offlineOrder.column.pieces')" prop="pieces" show-overflow-tooltip min-width="80" />
        <el-table-column :label="$t('offlineOrder.column.num')" prop="num" show-overflow-tooltip min-width="80" />
        <el-table-column :label="$t('offlineOrder.column.date1')" prop="date1" show-overflow-tooltip min-width="110" />
        <el-table-column :label="$t('offlineOrder.column.status')" prop="status" show-overflow-tooltip min-width="100" />
        <el-table-column :label="$t('offlineOrder.column.location')" prop="location" show-overflow-tooltip min-width="120" />
        <el-table-column :label="$t('offlineOrder.column.unitCh')" prop="unitCh" show-overflow-tooltip min-width="80" />
        <el-table-column :label="$t('offlineOrder.column.rate')" prop="rate" show-overflow-tooltip min-width="80" />
        <el-table-column :label="$t('offlineOrder.column.souko')" prop="souko" show-overflow-tooltip min-width="120" />
        <el-table-column :label="$t('offlineOrder.column.factory')" prop="factoryAddr" show-overflow-tooltip min-width="120" />
        <el-table-column :label="$t('offlineOrder.column.updateuser')" prop="updateuser" show-overflow-tooltip min-width="100" />
        <el-table-column :label="$t('offlineOrder.column.updatetimelegacy')" prop="updatetime" show-overflow-tooltip min-width="150" />
        <el-table-column :label="$t('offlineOrder.column.actions')" width="160">
          <template #default="{ row }">
            <el-button link class="btn-blue" size="small" @click="onDetail(row)">{{ $t('offlineOrder.dialog.detail') }}</el-button>
            <el-button link type="warning" size="small" @click="onEdit(row)" v-if="hasPermission('offline_order:update')">{{ $t('offlineOrder.dialog.edit') }}</el-button>
            <el-button link type="danger" size="small" @click="onDelete(row)" v-if="hasPermission('offline_order:delete')">{{ $t('offlineOrder.dialog.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Pagination -->
    <div class="pagination-bar">
      <el-pagination
        v-model:current-page="filter.page"
        v-model:page-size="filter.pageSize"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="onPageChange"
        @size-change="loadData"
      />
    </div>

    <!-- Detail Drawer -->
    <el-drawer
      v-model="detailVisible"
      :title="$t('offlineOrder.drawerTitle')"
      direction="rtl"
      size="720px"
    >
      <div class="drawer-content">
        <!-- Basic Info -->
        <div class="drawer-section-title">{{ $t('offlineOrder.drawerSection.basic') }}</div>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.code') }}:</span>
            <span class="detail-value">{{ currentRow?.code || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.manager') }}:</span>
            <span class="detail-value">{{ currentRow?.manager || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.destination') }}:</span>
            <span class="detail-value">{{ currentRow?.destination || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.tax') }}:</span>
            <span class="detail-value">{{ currentRow?.tax || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.material') }}:</span>
            <span class="detail-value">{{ currentRow?.material || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.kensa') }}:</span>
            <span class="detail-value">{{ currentRow?.kensa || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.pieces') }}:</span>
            <span class="detail-value">{{ currentRow?.pieces ?? '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.num') }}:</span>
            <span class="detail-value">{{ currentRow?.num ?? '-' }}</span>
          </div>
        </div>

        <!-- Weight & Size -->
        <div class="drawer-section-title">{{ $t('offlineOrder.drawerSection.weightSize') }}</div>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.weight') }}:</span>
            <span class="detail-value">{{ currentRow?.weight ?? '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.weight2') }}:</span>
            <span class="detail-value">{{ currentRow?.weight2 ?? '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.length') }}:</span>
            <span class="detail-value">{{ currentRow?.length ?? '-' }}</span>
          </div>
        </div>

        <!-- Date & Status -->
        <div class="drawer-section-title">{{ $t('offlineOrder.drawerSection.dateStatus') }}</div>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.date1') }}:</span>
            <span class="detail-value">{{ currentRow?.date1 || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.status') }}:</span>
            <span class="detail-value">{{ currentRow?.status || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.unitCh') }}:</span>
            <span class="detail-value">{{ currentRow?.unitCh ?? '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.rate') }}:</span>
            <span class="detail-value">{{ currentRow?.rate ?? '-' }}</span>
          </div>
        </div>

        <!-- Factory Info -->
        <div class="drawer-section-title">{{ $t('offlineOrder.drawerSection.factory') }}</div>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.location') }}:</span>
            <span class="detail-value">{{ currentRow?.location || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.souko') }}:</span>
            <span class="detail-value">{{ currentRow?.souko || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.factory') }}:</span>
            <span class="detail-value">{{ currentRow?.factoryAddr || '-' }}</span>
          </div>
        </div>

        <!-- Memo -->
        <div class="drawer-section-title">{{ $t('offlineOrder.drawerSection.memo') }}</div>
        <div class="detail-grid">
          <div class="detail-item full-width">
            <span class="detail-label">{{ $t('offlineOrder.column.other') }}:</span>
            <span class="detail-value">{{ currentRow?.other || '-' }}</span>
          </div>
          <div class="detail-item full-width">
            <span class="detail-label">{{ $t('offlineOrder.column.rireki') }}:</span>
            <span class="detail-value">{{ currentRow?.rireki || '-' }}</span>
          </div>
        </div>

        <!-- Audit Info -->
        <div class="drawer-section-title">{{ $t('offlineOrder.drawerSection.audit') }}</div>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.updateuser') }}:</span>
            <span class="detail-value">{{ currentRow?.updateuser || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.updatetimelegacy') }}:</span>
            <span class="detail-value">{{ formatDate(currentRow?.updatetime) }}</span>
          </div>
        </div>

        <div class="drawer-footer">
          <el-button @click="detailVisible = false">{{ $t('offlineOrder.drawerSection.close') }}</el-button>
        </div>
      </div>
    </el-drawer>

    <!-- Edit Dialog -->
    <el-dialog v-model="editVisible" :title="editTitle" width="760px" destroy-on-close>
      <el-form ref="formRef" :model="form" label-width="130px" style="padding: 0 8px">
        <!-- Basic Fields -->
        <div class="drawer-section-title">{{ $t('offlineOrder.drawerSection.basic') }}</div>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('offlineOrder.column.code')" prop="code">
              <el-input v-model="form.code" :placeholder="$t('offlineOrder.column.code')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('offlineOrder.column.manager')" prop="manager">
              <el-input v-model="form.manager" :placeholder="$t('offlineOrder.column.manager')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('offlineOrder.column.destination')" prop="destination">
              <el-input v-model="form.destination" :placeholder="$t('offlineOrder.column.destination')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('offlineOrder.column.tax')" prop="tax">
              <el-input v-model="form.tax" :placeholder="$t('offlineOrder.column.tax')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('offlineOrder.column.material')" prop="material">
              <el-input v-model="form.material" :placeholder="$t('offlineOrder.column.material')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('offlineOrder.column.kensa')" prop="kensa">
              <el-input v-model="form.kensa" :placeholder="$t('offlineOrder.column.kensa')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('offlineOrder.column.pieces')" prop="pieces">
              <el-input-number v-model="form.pieces" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('offlineOrder.column.num')" prop="num">
              <el-input-number v-model="form.num" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('offlineOrder.column.status')" prop="status">
              <el-input v-model="form.status" :placeholder="$t('offlineOrder.column.status')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('offlineOrder.column.showFlag')" prop="showFlag">
              <el-input-number v-model="form.showFlag" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- Weight & Size Fields -->
        <div class="drawer-section-title">{{ $t('offlineOrder.drawerSection.weightSize') }}</div>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('offlineOrder.column.weight')" prop="weight">
              <el-input-number v-model="form.weight" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('offlineOrder.column.weight2')" prop="weight2">
              <el-input-number v-model="form.weight2" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('offlineOrder.column.length')" prop="length">
              <el-input-number v-model="form.length" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- Date & Unit Fields -->
        <div class="drawer-section-title">{{ $t('offlineOrder.drawerSection.dateStatus') }}</div>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('offlineOrder.column.date1')" prop="date1">
              <el-date-picker
                v-model="form.date1"
                type="date"
                value-format="YYYY-MM-DD"
                :placeholder="$t('offlineOrder.column.date1')"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('offlineOrder.column.unitCh')" prop="unitCh">
              <el-input-number v-model="form.unitCh" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('offlineOrder.column.rate')" prop="rate">
              <el-input-number v-model="form.rate" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- Factory Fields -->
        <div class="drawer-section-title">{{ $t('offlineOrder.drawerSection.factory') }}</div>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('offlineOrder.column.location')" prop="location">
              <el-input v-model="form.location" :placeholder="$t('offlineOrder.column.location')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('offlineOrder.column.souko')" prop="souko">
              <el-input v-model="form.souko" :placeholder="$t('offlineOrder.column.souko')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('offlineOrder.column.factory')" prop="factoryAddr">
              <el-input v-model="form.factoryAddr" :placeholder="$t('offlineOrder.column.factory')" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- Memo Fields -->
        <div class="drawer-section-title">{{ $t('offlineOrder.drawerSection.memo') }}</div>
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item :label="$t('offlineOrder.column.other')" prop="other">
              <el-input v-model="form.other" type="textarea" :rows="2" :placeholder="$t('offlineOrder.column.other')" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item :label="$t('offlineOrder.column.rireki')" prop="rireki">
              <el-input v-model="form.rireki" type="textarea" :rows="2" :placeholder="$t('offlineOrder.column.rireki')" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <el-button @click="editVisible = false">{{ $t('offlineOrder.dialog.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">{{ $t('offlineOrder.dialog.submit') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { legacyImportList8Api } from '@/api/legacy-import-list8'
import type { LegacyImportList8VO, LegacyImportList8UpdateCmd } from '@/api/legacy-import-list8'
import { usePermission } from '@/composables/usePermission'

const { hasPermission } = usePermission()
const { t } = useI18n()

// Filter
const filter = reactive({
  code: '',
  location: '',
  souko: '',
  destination: '',
  page: 0,
  pageSize: 20,
})

// Table state
const loading = ref(false)
const tableData = ref<LegacyImportList8VO[]>([])
const total = ref(0)

// Detail state
const detailVisible = ref(false)
const currentRow = ref<LegacyImportList8VO | null>(null)

// Edit state
const editVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const isCreate = ref(true)
const editingId = ref<number | null>(null)

const blankForm = (): LegacyImportList8UpdateCmd => ({
  code: '',
  manager: '',
  destination: '',
  tax: '',
  material: '',
  kensa: '',
  pieces: undefined,
  num: undefined,
  weight: undefined,
  weight2: undefined,
  length: undefined,
  date1: '',
  status: '',
  other: '',
  unitCh: undefined,
  rate: undefined,
  souko: '',
  location: '',
  factoryAddr: '',
  showFlag: undefined,
  rireki: '',
})

const form = reactive<LegacyImportList8UpdateCmd>(blankForm())

const editTitle = computed(() =>
  isCreate.value ? t('offlineOrder.newButton') : t('offlineOrder.editTitle')
)

const formatDate = (d?: string): string => {
  if (!d) return '-'
  return d.replace('T', ' ').substring(0, 16)
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      code: filter.code || undefined,
      location: filter.location || undefined,
      souko: filter.souko || undefined,
      destination: filter.destination || undefined,
      page: filter.page,
      pageSize: filter.pageSize,
    }
    const res = await legacyImportList8Api.list(params)
    tableData.value = res.data?.content ?? []
    total.value = res.data?.totalElements ?? 0
  } catch {
    ElMessage.error(t('offlineOrder.message.loadFailed'))
  } finally {
    loading.value = false
  }
}

const onReset = () => {
  filter.code = ''
  filter.location = ''
  filter.souko = ''
  filter.destination = ''
  filter.page = 0
  loadData()
}

const onPageChange = (page: number) => {
  filter.page = page - 1
  loadData()
}

const onDetail = (row: LegacyImportList8VO) => {
  currentRow.value = row
  detailVisible.value = true
}

const onCreate = () => {
  isCreate.value = true
  editingId.value = null
  Object.assign(form, blankForm())
  editVisible.value = true
}

const onEdit = (row: LegacyImportList8VO) => {
  isCreate.value = false
  editingId.value = row.id
  Object.assign(form, {
    code: row.code ?? '',
    manager: row.manager ?? '',
    destination: row.destination ?? '',
    tax: row.tax ?? '',
    material: row.material ?? '',
    kensa: row.kensa ?? '',
    pieces: row.pieces,
    num: row.num,
    weight: row.weight,
    weight2: row.weight2,
    length: row.length,
    date1: row.date1 ?? '',
    status: row.status ?? '',
    other: row.other ?? '',
    unitCh: row.unitCh,
    rate: row.rate,
    souko: row.souko ?? '',
    location: row.location ?? '',
    factoryAddr: row.factoryAddr ?? '',
    showFlag: row.showFlag,
    rireki: row.rireki ?? '',
  })
  editVisible.value = true
}

const onSubmit = async () => {
  submitting.value = true
  try {
    if (!editingId.value) {
      await legacyImportList8Api.create(form)
      ElMessage.success(t('offlineOrder.message.createSuccess'))
    } else {
      await legacyImportList8Api.update(editingId.value, form)
      ElMessage.success(t('offlineOrder.message.updateSuccess'))
    }
    editVisible.value = false
    loadData()
  } catch {
    ElMessage.error(t('offlineOrder.message.saveFailed'))
  } finally {
    submitting.value = false
  }
}

const onDelete = (row: LegacyImportList8VO) => {
  ElMessageBox.confirm(
    t('offlineOrder.deleteConfirm', { code: row.code, id: row.id }),
    t('common.delete'),
    { confirmButtonText: t('common.button.confirm'), cancelButtonText: t('common.button.cancel'), type: 'warning' }
  )
    .then(async () => {
      try {
        await legacyImportList8Api.delete(row.id)
        ElMessage.success(t('offlineOrder.message.deleteSuccess'))
        loadData()
      } catch {
        ElMessage.error(t('offlineOrder.message.deleteFailed'))
      }
    })
    .catch(() => {})
}

onMounted(() => {
  loadData()
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
  display: flex;
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

.drawer-footer {
  margin-top: 24px;
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}
:deep(.el-table__header-wrapper),
:deep(.el-table__header th.el-table__cell) {
  position: sticky !important;
  top: 0 !important;
  z-index: 10 !important;
  background: inherit;
}
</style>
