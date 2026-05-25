<script setup lang="ts">
/**
 * 货物发送整理页面（v1.0.0）
 */
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Plus } from '@element-plus/icons-vue'
import { dispatchApi, type DispatchVO } from '@/api/dispatch'
import { usePermission } from '@/composables/usePermission'
import ExcelTable, { type ExcelColDef } from '@/components/ExcelTable.vue'

const { hasPermission } = usePermission()
const { t, locale: localeRef } = useI18n()

function formatTime(ts: string | undefined | null): string {
  if (!ts) return '-'
  return new Date(ts).toLocaleString(localeRef.value === 'ja' ? 'ja-JP' : 'zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit',
  })
}

function formatDate(d: string | undefined | null): string {
  if (!d || d === '0000-00-00') return '-'
  return d
}

const loading = ref(false)
const excelViewMode = ref<'table' | 'copy'>('table')
const tableData = ref<DispatchVO[]>([])
const pagination = ref({ page: 0, pageSize: 20, total: 0 })

const tableRef = ref()
const selectedRows = ref<DispatchVO[]>([])

const filterForm = ref({ code: '', destination: '', manager: '', showFlag: '' as string })

const dialogVisible = ref(false)
const dialogTitle = ref('')
const editingItem = ref<Partial<DispatchVO>>({})

// ===== 详情抽屉 =====
const drawerVisible = ref(false)
const currentRow = ref<DispatchVO | null>(null)

const formData = ref({
  code: '',
  manager: '',
  destination: '',
  tax: '',
  material: '',
  kensa: '',
  quantity: 0,
  pieces: 0,
  weight: 0,
  weight2: 0,
  length: 0,
  location: '',
  dispatchDate: '',
  status: '',
  other: '',
  unitPrice: 0,
  rate: 0,
  warehouse: '',
  factoryAddr: '',
  showFlag: 0,
  rireki: '',
})

const copyColumns: ExcelColDef[] = [
  { prop: 'code', label: t('dispatch.column.code') },
  { prop: 'manager', label: t('dispatch.column.manager') },
  { prop: 'destination', label: t('dispatch.column.destination') },
  { prop: 'material', label: t('dispatch.column.material') },
  { prop: 'quantity', label: t('dispatch.column.quantity') },
  { prop: 'pieces', label: t('dispatch.column.pieces') },
  { prop: 'weight', label: t('dispatch.column.weight') },
  { prop: 'weight2', label: t('dispatch.column.weight2') },
  { prop: 'length', label: t('dispatch.column.length') },
  { prop: 'location', label: t('dispatch.column.location') },
  { prop: 'dispatchDate', label: t('dispatch.column.dispatchDate'), formatter: (row) => formatDate(row.dispatchDate) },
  { prop: 'status', label: t('dispatch.column.status') },
  { prop: 'warehouse', label: t('dispatch.column.warehouse') },
  { prop: 'other', label: t('dispatch.column.other') },
  { prop: 'action', label: t('dispatch.column.actions'), excluded: true },
]

async function loadData() {
  loading.value = true
  try {
    const res = await dispatchApi.list({
      page: pagination.value.page,
      pageSize: pagination.value.pageSize,
      code: filterForm.value.code || undefined,
      destination: filterForm.value.destination || undefined,
      manager: filterForm.value.manager || undefined,
      showFlag: filterForm.value.showFlag || undefined,
    })
    const data = res.data
    tableData.value = data?.content ?? []
    pagination.value.total = data?.totalElements ?? 0
  } catch {
    ElMessage.error(t('common.message.loadFailed'))
  } finally {
    loading.value = false
  }
}

function onSearch() { pagination.value.page = 0; loadData() }
function onReset() { filterForm.value = { code: '', destination: '', manager: '', showFlag: '' }; pagination.value.page = 0; loadData() }

function onView(row: DispatchVO) {
  currentRow.value = row
  drawerVisible.value = true
}

function onNew() {
  editingItem.value = {}
  dialogTitle.value = t('dispatch.newButton')
  formData.value = {
    code: '', manager: '', destination: '', tax: '', material: '', kensa: '',
    quantity: 0, pieces: 0, weight: 0, weight2: 0, length: 0,
    location: '', dispatchDate: '', status: '', other: '',
    unitPrice: 0, rate: 0, warehouse: '', factoryAddr: '',
    showFlag: 0, rireki: '',
  }
  dialogVisible.value = true
}

function onEdit(row: DispatchVO) {
  editingItem.value = row
  dialogTitle.value = t('dispatch.editTitle')
  formData.value = {
    code: row.code, manager: row.manager, destination: row.destination,
    tax: row.tax ?? '', material: row.material ?? '', kensa: row.kensa ?? '',
    quantity: row.quantity, pieces: row.pieces, weight: row.weight,
    weight2: row.weight2 ?? 0, length: row.length ?? 0,
    location: row.location ?? '', dispatchDate: row.dispatchDate ?? '',
    status: row.status ?? '', other: row.other ?? '',
    unitPrice: row.unitPrice ?? 0, rate: row.rate ?? 0,
    warehouse: row.warehouse ?? '', factoryAddr: row.factoryAddr ?? '',
    showFlag: row.showFlag ?? 0, rireki: row.rireki ?? '',
  }
  dialogVisible.value = true
}

async function onSubmit() {
  if (!formData.value.code) {
    ElMessage.warning(t('dispatch.validation.codeRequired'))
    return
  }
  try {
    if (editingItem.value.id) {
      await dispatchApi.update(editingItem.value.id, {
        ...formData.value,
        dispatchDate: formData.value.dispatchDate || undefined,
      })
    } else {
      await dispatchApi.create({
        ...formData.value,
        dispatchDate: formData.value.dispatchDate || undefined,
      })
    }
    ElMessage.success(t('common.message.saveSuccess'))
    dialogVisible.value = false
    loadData()
  } catch {
    ElMessage.error(t('common.message.saveFailed'))
  }
}

async function onDelete(row: DispatchVO) {
  try {
    await ElMessageBox.confirm(
      t('dispatch.deleteConfirm', { code: row.code }),
      t('common.delete'),
      { type: 'warning' }
    )
    await dispatchApi.delete(row.id)
    ElMessage.success(t('common.message.deleteSuccess'))
    loadData()
  } catch {
    // cancelled
  }
}

function onSelectionChange(selection: DispatchVO[]) {
  selectedRows.value = selection
}

async function onBatchDelete() {
  if (!selectedRows.value.length) return
  try {
    await ElMessageBox.confirm(
      t('common.batch.deleteConfirm', { n: selectedRows.value.length }),
      t('common.batch.deleteConfirmTitle'),
      { confirmButtonText: t('common.delete'), cancelButtonText: t('common.cancel'), type: 'warning' }
    )
  } catch { return }
  loading.value = true
  try {
    await Promise.all(selectedRows.value.map(r => dispatchApi.delete(r.id)))
    ElMessage.success(t('common.batch.deleteSuccess', { n: selectedRows.value.length }))
    selectedRows.value = []
    loadData()
  } catch {
    ElMessage.error(t('common.batch.deleteFailed'))
  } finally {
    loading.value = false
  }
}

function onPageChange(p: number) { pagination.value.page = p - 1; loadData() }
function onPageSizeChange(s: number) { pagination.value.pageSize = s; pagination.value.page = 0; loadData() }

onMounted(loadData)
</script>

<template>
  <div class="page">
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filterForm">
        <el-form-item :label="$t('dispatch.filter.code')">
          <el-input v-model="filterForm.code" :placeholder="$t('dispatch.filter.codeHint')" clearable style="width:160px" />
        </el-form-item>
        <el-form-item :label="$t('dispatch.filter.destination')">
          <el-input v-model="filterForm.destination" :placeholder="$t('dispatch.filter.destinationHint')" clearable style="width:140px" />
        </el-form-item>
        <el-form-item :label="$t('dispatch.filter.manager')">
          <el-input v-model="filterForm.manager" :placeholder="$t('dispatch.filter.managerHint')" clearable style="width:120px" />
        </el-form-item>
        <el-form-item :label="$t('dispatch.filter.showFlag')">
          <el-select v-model="filterForm.showFlag" :placeholder="$t('dispatch.filter.all')" clearable style="width:120px">
            <el-option value="0" :label="$t('dispatch.showFlag.active')" />
            <el-option value="1" :label="$t('dispatch.showFlag.archived')" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">{{ $t('dispatch.filter.search') }}</el-button>
          <el-button @click="onReset">{{ $t('dispatch.filter.reset') }}</el-button>
          <el-button type="primary" @click="onNew" v-if="hasPermission('dispatch:create')">
            <el-icon><Plus /></el-icon>{{ $t('dispatch.newButton') }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="table-header">
          <el-radio-group v-model="excelViewMode" size="small">
            <el-radio-button value="table">{{ $t('common.viewMode.table') }}</el-radio-button>
            <el-radio-button value="copy">{{ $t('common.viewMode.excel') }}</el-radio-button>
          </el-radio-group>
          <div class="batch-actions">
            <span v-if="selectedRows.length" class="selection-count">
              <el-tag type="info" size="small">{{ $t('common.batch.selectedCount', { n: selectedRows.length }) }}</el-tag>
            </span>
            <el-button
              v-if="selectedRows.length && hasPermission('dispatch:delete')"
              type="danger"
              size="small"
              @click="onBatchDelete"
            >
              <el-icon><Delete /></el-icon>{{ $t('common.batch.delete', { n: selectedRows.length }) }}
            </el-button>
          </div>
        </div>
      </template>
      <el-table v-if="excelViewMode === 'table'" v-loading="loading" :data="tableData" stripe style="width:100%" min-height="200" ref="tableRef" row-key="id" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="50" align="center" :reserve-selection="true" />
        <el-table-column prop="code" :label="$t('dispatch.column.code')" min-width="110" />
        <el-table-column prop="manager" :label="$t('dispatch.column.manager')" min-width="80" />
        <el-table-column prop="destination" :label="$t('dispatch.column.destination')" min-width="90" />
        <el-table-column prop="material" :label="$t('dispatch.column.material')" min-width="110" show-overflow-tooltip />
        <el-table-column prop="quantity" :label="$t('dispatch.column.quantity')" min-width="70" align="right" />
        <el-table-column prop="pieces" :label="$t('dispatch.column.pieces')" min-width="70" align="right" />
        <el-table-column prop="weight" :label="$t('dispatch.column.weight')" min-width="80" align="right" />
        <el-table-column prop="weight2" :label="$t('dispatch.column.weight2')" min-width="80" align="right" />
        <el-table-column prop="length" :label="$t('dispatch.column.length')" min-width="70" align="right" />
        <el-table-column prop="location" :label="$t('dispatch.column.location')" min-width="90" />
        <el-table-column :label="$t('dispatch.column.dispatchDate')" min-width="100">
          <template #default="{ row }">
            {{ formatDate(row.dispatchDate) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" :label="$t('dispatch.column.status')" min-width="70" />
        <el-table-column :label="$t('dispatch.column.actions')" min-width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="hasPermission('dispatch:read')" link class="btn-blue" size="small" @click.stop="onView(row)">{{ $t('common.view') }}</el-button>
            <el-button v-if="hasPermission('dispatch:update')" link type="warning" size="small" @click="onEdit(row)">{{ $t('common.edit') }}</el-button>
            <el-button v-if="hasPermission('dispatch:delete')" link type="danger" size="small" @click="onDelete(row)">{{ $t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <ExcelTable v-else :columns="copyColumns" :data="tableData" />

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        @current-change="onPageChange"
        @size-change="onPageSizeChange"
        style="margin-top:16px;justify-content:flex-end"
      />
    </el-card>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="620px" :close-on-click-modal="false">
      <el-form label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('dispatch.column.code')" required>
              <el-input v-model="formData.code" :placeholder="$t('dispatch.dialog.codePlaceholder')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('dispatch.column.manager')">
              <el-input v-model="formData.manager" :placeholder="$t('dispatch.dialog.managerPlaceholder')" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('dispatch.column.destination')">
              <el-input v-model="formData.destination" :placeholder="$t('dispatch.dialog.destinationPlaceholder')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('dispatch.column.dispatchDate')">
              <el-date-picker v-model="formData.dispatchDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('dispatch.column.material')">
              <el-input v-model="formData.material" :placeholder="$t('dispatch.dialog.materialPlaceholder')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('dispatch.column.kensa')">
              <el-input v-model="formData.kensa" :placeholder="$t('dispatch.dialog.kensaPlaceholder')" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item :label="$t('dispatch.column.quantity')">
              <el-input-number v-model="formData.quantity" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('dispatch.column.pieces')">
              <el-input-number v-model="formData.pieces" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('dispatch.column.length')">
              <el-input-number v-model="formData.length" :min="0" :precision="1" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item :label="$t('dispatch.column.weight')">
              <el-input-number v-model="formData.weight" :min="0" :precision="1" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('dispatch.column.weight2')">
              <el-input-number v-model="formData.weight2" :min="0" :precision="1" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('dispatch.column.unitPrice')">
              <el-input-number v-model="formData.unitPrice" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('dispatch.column.location')">
              <el-input v-model="formData.location" :placeholder="$t('dispatch.dialog.locationPlaceholder')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('dispatch.column.warehouse')">
              <el-input v-model="formData.warehouse" :placeholder="$t('dispatch.dialog.warehousePlaceholder')" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('dispatch.column.factoryAddr')">
              <el-input v-model="formData.factoryAddr" :placeholder="$t('dispatch.dialog.factoryAddrPlaceholder')" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item :label="$t('dispatch.column.rate')">
              <el-input-number v-model="formData.rate" :min="0" :precision="4" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item :label="$t('dispatch.column.tax')">
              <el-input v-model="formData.tax" :placeholder="$t('dispatch.dialog.taxPlaceholder')" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('dispatch.column.status')">
              <el-input v-model="formData.status" :placeholder="$t('dispatch.dialog.statusPlaceholder')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('dispatch.column.showFlag')">
              <el-select v-model="formData.showFlag" style="width:100%">
                <el-option :value="0" :label="$t('dispatch.showFlag.active')" />
                <el-option :value="1" :label="$t('dispatch.showFlag.archived')" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="$t('dispatch.column.other')">
          <el-input v-model="formData.other" :placeholder="$t('dispatch.dialog.otherPlaceholder')" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item :label="$t('dispatch.column.rireki')">
          <el-input v-model="formData.rireki" :placeholder="$t('dispatch.dialog.rirekiPlaceholder')" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="onSubmit">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" :title="$t('dispatch.drawerTitle')" size="560px" direction="rtl">
      <el-descriptions :column="2" border v-if="currentRow">
        <el-descriptions-item :label="$t('dispatch.column.code')" :span="2">{{ currentRow.code }}</el-descriptions-item>
        <el-descriptions-item :label="$t('dispatch.column.manager')">{{ currentRow.manager }}</el-descriptions-item>
        <el-descriptions-item :label="$t('dispatch.column.destination')">{{ currentRow.destination }}</el-descriptions-item>
        <el-descriptions-item :label="$t('dispatch.column.dispatchDate')">{{ formatDate(currentRow.dispatchDate) }}</el-descriptions-item>
        <el-descriptions-item :label="$t('dispatch.column.status')">{{ currentRow.status || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('dispatch.column.material')">{{ currentRow.material || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('dispatch.column.kensa')">{{ currentRow.kensa || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('dispatch.column.quantity')">{{ currentRow.quantity }}</el-descriptions-item>
        <el-descriptions-item :label="$t('dispatch.column.pieces')">{{ currentRow.pieces }}</el-descriptions-item>
        <el-descriptions-item :label="$t('dispatch.column.weight')">{{ currentRow.weight }}</el-descriptions-item>
        <el-descriptions-item :label="$t('dispatch.column.weight2')">{{ currentRow.weight2 }}</el-descriptions-item>
        <el-descriptions-item :label="$t('dispatch.column.length')">{{ currentRow.length }}</el-descriptions-item>
        <el-descriptions-item :label="$t('dispatch.column.unitPrice')">{{ currentRow.unitPrice }}</el-descriptions-item>
        <el-descriptions-item :label="$t('dispatch.column.rate')">{{ currentRow.rate }}</el-descriptions-item>
        <el-descriptions-item :label="$t('dispatch.column.location')">{{ currentRow.location || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('dispatch.column.warehouse')">{{ currentRow.warehouse || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('dispatch.column.factoryAddr')">{{ currentRow.factoryAddr || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('dispatch.column.tax')">{{ currentRow.tax || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('dispatch.column.showFlag')">
          <el-tag v-if="currentRow.showFlag === 1" type="info" size="small">{{ $t('dispatch.showFlag.archived') }}</el-tag>
          <el-tag v-else type="success" size="small">{{ $t('dispatch.showFlag.active') }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('dispatch.column.other')" :span="2">{{ currentRow.other || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('dispatch.column.rireki')" :span="2">{{ currentRow.rireki || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('dispatch.column.createTime')">{{ formatTime(currentRow.createTime) }}</el-descriptions-item>
        <el-descriptions-item :label="$t('dispatch.column.updateTime')">{{ formatTime(currentRow.updateTime) }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button v-if="hasPermission('dispatch:update')" type="warning" @click="() => { drawerVisible = false; onEdit(currentRow!) }">{{ $t('common.edit') }}</el-button>
        <el-button @click="drawerVisible = false">{{ $t('common.close') }}</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.filter-card :deep(.el-card__body) { padding-bottom: 0; }
.table-card :deep(.el-card__body) { padding: 16px; }
.table-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
.batch-actions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.selection-count { margin-left: 4px; }
:deep(.el-drawer__body) { overflow-y: auto !important; overflow-x: hidden; padding: 20px; }
.btn-blue { color: #E8650A !important; }
:deep(.el-table__header-wrapper),
:deep(.el-table__header th.el-table__cell) {
  position: sticky !important;
  top: 0 !important;
  z-index: 10 !important;
  background: inherit;
}
</style>