<script setup lang="ts">
/**
 * 货柜管理页面（v1.5.0，SPEC-B00 Issue #8）
 */
import { ref, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Plus } from '@element-plus/icons-vue'
import { containerApi, type ContainerVO, type AssignShipRequest } from '@/api/logistics'
import { shipApi, type ShipVO } from '@/api/ship'
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

const loading = ref(false)
const excelViewMode = ref<'table' | 'copy'>('table')
const tableData = ref<ContainerVO[]>([])
const pagination = ref({ page: 0, pageSize: 20, total: 0 })

const tableRef = ref()
const selectedRows = ref<ContainerVO[]>([])

const filterForm = ref({ containerNo: '', shipId: '' as number | '', showFlag: null as boolean | null, legacyStatus: '', cabinetNo: '' })
const dialogVisible = ref(false)
const dialogTitle = ref('')
const editingItem = ref<Partial<ContainerVO>>({})

// ===== 详情抽屉 =====
const drawerVisible = ref(false)
const currentRow = ref<ContainerVO | null>(null)

const formData = ref({
  containerNo: '',
  loadDate: '',
  departureDate: '',
  timeSlot: '',
  arrivalLocation: '',
  remarks: '',
  cabinetNo: '',
  period: '',
  legacyStatus: '',
  showFlag: true as boolean,
})

// ===== 分配船只 =====
const assignDialogVisible = ref(false)
const assignDialogTitle = ref('')
const assignEditingContainer = ref<Partial<ContainerVO>>({})
const shipOptions = ref<ShipVO[]>([])
const assignForm = ref<AssignShipRequest>({ shipId: 0, loadDate: '' })

const periodOptions = computed(() => [
  t('logistics.container.periodOptions.earlyMorning'),
  t('logistics.container.periodOptions.morning'),
  t('logistics.container.periodOptions.afternoon'),
  t('logistics.container.periodOptions.evening'),
])
const legacyStatusOptions = computed(() => [
  t('logistics.container.legacyStatusOptions.notOut'),
  t('logistics.container.legacyStatusOptions.out'),
  t('logistics.container.legacyStatusOptions.pending'),
])

const copyColumns: ExcelColDef[] = [
  { prop: 'containerNo', label: t('logistics.container.column.containerNo') },
  { prop: 'shipName', label: t('logistics.container.column.shipName'), formatter: (row) => row.shipName || '' },
  { prop: 'shipNumber', label: t('logistics.container.column.shipNumber'), formatter: (row) => row.shipNumber || '' },
  { prop: 'cabinetNo', label: t('logistics.container.column.cabinetNo'), formatter: (row) => row.cabinetNo || '' },
  { prop: 'loadDate', label: t('logistics.container.column.loadDate'), formatter: (row) => row.loadDate || '' },
  { prop: 'departureDate', label: t('logistics.container.column.departureDate'), formatter: (row) => row.departureDate || '' },
  { prop: 'period', label: t('logistics.container.column.period'), formatter: (row) => row.period || '' },
  { prop: 'legacyStatus', label: t('logistics.container.column.status'), formatter: (row) => row.legacyStatus || '-' },
  { prop: 'arrivalLocation', label: t('logistics.container.column.arrivalLocation'), formatter: (row) => row.arrivalLocation || '' },
  { prop: 'remarks', label: t('logistics.container.column.remarks'), formatter: (row) => row.remarks || '' },
  { prop: 'action', label: t('logistics.column.actions'), excluded: true },
]

async function loadData() {
  loading.value = true
  try {
    const res = await containerApi.list({
      page: pagination.value.page,
      pageSize: pagination.value.pageSize,
      shipId: filterForm.value.shipId || undefined,
      showFlag: filterForm.value.showFlag ?? undefined,
      legacyStatus: filterForm.value.legacyStatus || undefined,
      cabinetNo: filterForm.value.cabinetNo || undefined,
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
function onReset() { filterForm.value = { containerNo: '', shipId: '', showFlag: null, legacyStatus: '', cabinetNo: '' }; pagination.value.page = 0; loadData() }

function onView(row: ContainerVO) {
  currentRow.value = row
  drawerVisible.value = true
}

function onNew() {
  editingItem.value = {}
  dialogTitle.value = t('logistics.container.newButton')
  formData.value = {
    containerNo: '',
    loadDate: '',
    departureDate: '',
    timeSlot: '',
    arrivalLocation: '',
    remarks: '',
    cabinetNo: '',
    period: '',
    legacyStatus: '',
    showFlag: true,
  }
  dialogVisible.value = true
}

function onEdit(row: ContainerVO) {
  editingItem.value = row
  dialogTitle.value = t('logistics.container.editTitle')
  formData.value = {
    containerNo: row.containerNo,
    loadDate: row.loadDate ?? '',
    departureDate: row.departureDate ?? '',
    timeSlot: row.timeSlot ?? '',
    arrivalLocation: row.arrivalLocation ?? '',
    remarks: row.remarks ?? '',
    cabinetNo: row.cabinetNo ?? '',
    period: row.period ?? '',
    legacyStatus: row.legacyStatus ?? '',
    showFlag: row.showFlag ?? true,
  }
  dialogVisible.value = true
}

async function onSubmit() {
  if (!formData.value.containerNo) {
    ElMessage.warning(t('logistics.container.containerNoRequired'))
    return
  }
  try {
    const commonFields = {
      containerNo: formData.value.containerNo,
      loadDate: formData.value.loadDate || undefined,
      departureDate: formData.value.departureDate || undefined,
      timeSlot: formData.value.timeSlot.trim() || undefined,
      arrivalLocation: formData.value.arrivalLocation.trim() || undefined,
      remarks: formData.value.remarks.trim() || undefined,
      cabinetNo: formData.value.cabinetNo.trim() || undefined,
      period: formData.value.period || undefined,
      legacyStatus: formData.value.legacyStatus || undefined,
      showFlag: formData.value.showFlag,
    }
    if (editingItem.value.id) {
      await containerApi.update(editingItem.value.id, commonFields)
    } else {
      await containerApi.create(commonFields)
    }
    ElMessage.success(t('common.message.saveSuccess'))
    dialogVisible.value = false
    loadData()
  } catch {
    ElMessage.error(t('common.message.saveFailed'))
  }
}

async function onDelete(row: ContainerVO) {
  try {
    await ElMessageBox.confirm(t('logistics.container.deleteConfirm'), t('common.delete'), { type: 'warning' })
    await containerApi.delete(row.id)
    ElMessage.success(t('common.message.deleteSuccess'))
    loadData()
  } catch {
    // cancelled
  }
}

function onSelectionChange(selection: ContainerVO[]) {
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
    await Promise.all(selectedRows.value.map(r => containerApi.delete(r.id)))
    ElMessage.success(t('common.batch.deleteSuccess', { n: selectedRows.value.length }))
    selectedRows.value = []
    loadData()
  } catch {
    ElMessage.error(t('common.batch.deleteFailed'))
  } finally {
    loading.value = false
  }
}

async function loadShipOptions() {
  try {
    const res = await shipApi.list({ pageSize: 100 })
    shipOptions.value = res.data?.content ?? []
  } catch {
    // ignore
  }
}

function onAssignShip(row: ContainerVO) {
  assignEditingContainer.value = row
  assignDialogTitle.value = t('logistics.container.dialog.assignShip')
  assignForm.value = {
    shipId: row.shipId ?? 0,
    loadDate: row.loadDate ?? '',
  }
  loadShipOptions()
  assignDialogVisible.value = true
}

async function onAssignShipSubmit() {
  if (!assignForm.value.shipId) {
    ElMessage.warning(t('logistics.container.dialog.shipPlaceholder'))
    return
  }
  try {
    if (assignEditingContainer.value.shipId) {
      // 重新分配
      await containerApi.assignShip(assignEditingContainer.value.id!, assignForm.value)
    } else {
      await containerApi.assignShip(assignEditingContainer.value.id!, assignForm.value)
    }
    ElMessage.success(t('logistics.container.assignSuccess'))
    assignDialogVisible.value = false
    loadData()
  } catch {
    ElMessage.error(t('common.message.saveFailed'))
  }
}

async function onUnassignShip(row: ContainerVO) {
  try {
    await ElMessageBox.confirm(t('logistics.container.unassignConfirm'), t('common.delete'), { type: 'warning' })
    await containerApi.unassignShip(row.id)
    ElMessage.success(t('logistics.container.unassignSuccess'))
    loadData()
  } catch {
    // cancelled
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
        <el-form-item :label="$t('logistics.container.filter.containerNo')">
          <el-input v-model="filterForm.containerNo" :placeholder="$t('logistics.container.filter.containerNoHint')" clearable style="width:180px" />
        </el-form-item>
        <el-form-item :label="$t('logistics.container.filter.ship')">
          <el-select
            v-model="filterForm.shipId"
            :placeholder="$t('logistics.container.filter.shipHint')"
            clearable
            filterable
            style="width:160px"
            @focus="loadShipOptions"
          >
            <el-option v-for="s in shipOptions" :key="s.id" :value="s.id" :label="`${s.shipName} (${s.shipNumber})`" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('logistics.container.filter.showFlag')">
          <el-select v-model="filterForm.showFlag" :placeholder="$t('logistics.filter.all')" clearable style="width:120px">
            <el-option :value="true" :label="$t('logistics.container.showFlag.active')" />
            <el-option :value="false" :label="$t('logistics.container.showFlag.archived')" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('logistics.container.filter.legacyStatus')">
          <el-select v-model="filterForm.legacyStatus" :placeholder="$t('logistics.filter.all')" clearable style="width:110px">
            <el-option v-for="s in legacyStatusOptions" :key="s" :value="s" :label="s" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('logistics.container.filter.cabinetNo')">
          <el-input v-model="filterForm.cabinetNo" :placeholder="$t('logistics.container.filter.cabinetNoHint')" clearable style="width:150px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">{{ $t('logistics.filter.search') }}</el-button>
          <el-button @click="onReset">{{ $t('logistics.filter.reset') }}</el-button>
          <el-button type="primary" @click="onNew" v-if="hasPermission('container:create')">
            <el-icon><Plus /></el-icon>{{ $t('logistics.container.newButton') }}
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
              v-if="selectedRows.length && hasPermission('container:delete')"
              type="danger"
              size="small"
              @click="onBatchDelete"
            >
              <el-icon><Delete /></el-icon>{{ $t('common.batch.delete', { n: selectedRows.length }) }}
            </el-button>
          </div>
        </div>
      </template>
      <el-table v-if="excelViewMode === 'table'" v-loading="loading" :data="tableData" stripe style="width:100%" ref="tableRef" row-key="id" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="50" align="center" :reserve-selection="true" />
        <el-table-column prop="containerNo" :label="$t('logistics.container.column.containerNo')" min-width="120" />
        <el-table-column :label="$t('logistics.container.column.shipName')" min-width="110">
          <template #default="{ row }">
            {{ row.shipName ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('logistics.container.column.shipNumber')" min-width="100">
          <template #default="{ row }">
            {{ row.shipNumber ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="cabinetNo" :label="$t('logistics.container.column.cabinetNo')" min-width="130" />
        <el-table-column prop="loadDate" :label="$t('logistics.container.column.loadDate')" min-width="100" />
        <el-table-column prop="departureDate" :label="$t('logistics.container.column.departureDate')" min-width="100" />
        <el-table-column prop="period" :label="$t('logistics.container.column.period')" min-width="90" />
        <el-table-column :label="$t('logistics.container.column.status')" min-width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="row.legacyStatus === '出完' ? 'success' : row.legacyStatus === '未出' ? 'info' : 'warning'" size="small">{{ row.legacyStatus || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('logistics.container.column.arrivalLocation')" min-width="100">
          <template #default="{ row }">
            {{ row.arrivalLocation ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('logistics.container.column.remarks')" min-width="110" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="cell-single-line">{{ row.remarks || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('logistics.column.actions')" min-width="150">
          <template #default="{ row }">
            <el-button link class="btn-blue" size="small" @click.stop="onView(row)">{{ $t('common.view') }}</el-button>
            <el-button v-if="hasPermission('container:update')" link type="warning" size="small" @click="onEdit(row)">{{ $t('common.edit') }}</el-button>
            <el-button v-if="hasPermission('container:delete')" link type="danger" size="small" @click="onDelete(row)">{{ $t('common.delete') }}</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="580px" :close-on-click-modal="false">
      <el-form label-width="140px">
        <el-form-item :label="$t('logistics.container.column.containerNo')" required>
          <el-input v-model="formData.containerNo" :placeholder="$t('logistics.container.containerNoPlaceholder')" />
        </el-form-item>
        <!-- list7 原始字段（SPEC-B14）-->
        <el-form-item :label="$t('logistics.container.column.cabinetNo')">
          <el-input v-model="formData.cabinetNo" :placeholder="$t('logistics.container.filter.cabinetNoPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('logistics.container.column.loadDate')">
          <el-date-picker v-model="formData.loadDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item :label="$t('logistics.container.column.departureDate')">
          <el-date-picker v-model="formData.departureDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item :label="$t('logistics.container.column.period')">
          <el-select v-model="formData.period" clearable style="width:100%">
            <el-option v-for="p in periodOptions" :key="p" :value="p" :label="p" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('logistics.container.column.status')">
          <el-select v-model="formData.legacyStatus" clearable style="width:100%">
            <el-option v-for="s in legacyStatusOptions" :key="s" :value="s" :label="s" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('logistics.container.column.arrivalLocation')">
          <el-input v-model="formData.arrivalLocation" :placeholder="$t('logistics.container.dialog.arrivalLocationPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('logistics.container.column.remarks')">
          <el-input v-model="formData.remarks" :placeholder="$t('logistics.container.dialog.remarksPlaceholder')" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item :label="$t('logistics.container.column.showFlag')">
          <el-switch v-model="formData.showFlag" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="onSubmit">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" :title="$t('logistics.container.drawerTitle')" size="560px" direction="rtl">
      <el-descriptions :column="2" border v-if="currentRow">
        <el-descriptions-item :label="$t('logistics.container.column.containerNo')">{{ currentRow.containerNo }}</el-descriptions-item>
        <el-descriptions-item :label="$t('logistics.container.column.shipName')">{{ currentRow.shipName || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('logistics.container.column.shipNumber')">{{ currentRow.shipNumber || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('logistics.container.column.cabinetNo')">{{ currentRow.cabinetNo || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('logistics.container.column.loadDate')">{{ currentRow.loadDate || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('logistics.container.column.departureDate')">{{ currentRow.departureDate || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('logistics.container.column.period')">{{ currentRow.period || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('logistics.container.column.status')">
          <el-tag :type="currentRow.legacyStatus === '出完' ? 'success' : currentRow.legacyStatus === '未出' ? 'info' : 'warning'" size="small">{{ currentRow.legacyStatus || '-' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('logistics.container.column.arrivalLocation')">{{ currentRow.arrivalLocation || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('logistics.container.column.showFlag')">
          <el-tag v-if="currentRow.showFlag === false" type="info" size="small">{{ $t('logistics.container.showFlag.archived') }}</el-tag>
          <el-tag v-else type="success" size="small">{{ $t('logistics.container.showFlag.active') }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('logistics.container.column.remarks')" :span="2">{{ currentRow.remarks || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('logistics.container.column.legacyUpdater')">{{ currentRow.legacyUpdater || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('logistics.container.column.legacyUpdatetime')">{{ currentRow.legacyUpdatetime ? formatTime(currentRow.legacyUpdatetime) : '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button v-if="hasPermission('container:update')" type="warning" @click="() => { drawerVisible = false; onEdit(currentRow!) }">{{ $t('common.edit') }}</el-button>
        <el-button v-if="hasPermission('container:update')" type="primary" @click="() => { drawerVisible = false; onAssignShip(currentRow!) }">{{ $t('logistics.container.dialog.assignShip') }}</el-button>
        <el-button v-if="hasPermission('container:update') && currentRow?.shipId" type="danger" @click="() => { drawerVisible = false; onUnassignShip(currentRow!) }">{{ $t('logistics.container.dialog.unassignShip') }}</el-button>
        <el-button @click="drawerVisible = false">{{ $t('common.close') }}</el-button>
      </template>
    </el-drawer>

    <!-- 分配船只弹窗 -->
    <el-dialog v-model="assignDialogVisible" :title="assignDialogTitle" width="480px" :close-on-click-modal="false">
      <el-form label-width="160px">
        <el-form-item :label="$t('logistics.container.dialog.shipName')" required>
          <el-select
            v-model="assignForm.shipId"
            :placeholder="$t('logistics.container.dialog.shipPlaceholder')"
            filterable
            style="width:100%"
          >
            <el-option v-for="s in shipOptions" :key="s.id" :value="s.id" :label="`${s.shipName} (${s.shipNumber})`" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('logistics.container.column.loadDate')">
          <el-date-picker v-model="assignForm.loadDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="onAssignShipSubmit">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.filter-card :deep(.el-card__body) { padding-bottom: 0; }
.table-card :deep(.el-card__body) { padding: 16px; }
.table-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
.batch-actions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.selection-count { margin-left: 4px; }
:deep(.el-drawer__body) { overflow-y: auto !important; overflow-x: hidden; }
:deep(.cell-single-line) { display: block; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.btn-blue { color: #E8650A !important; }
:deep(.el-table__header-wrapper),
:deep(.el-table__header th.el-table__cell) {
  position: sticky !important;
  top: 0 !important;
  z-index: 10 !important;
  background: inherit;
}
</style>
