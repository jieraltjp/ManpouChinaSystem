<script setup lang="ts">
/**
 * 货柜管理页面（v1.5.0，SPEC-B00 Issue #8）
 */
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { containerApi, type ContainerVO, type ContainerStatus, type ContainerType, type AssignShipRequest } from '@/api/logistics'
import { shipApi, type ShipVO } from '@/api/ship'
import { usePermission } from '@/composables/usePermission'

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
const tableData = ref<ContainerVO[]>([])
const pagination = ref({ page: 0, pageSize: 20, total: 0 })

const filterForm = ref({ status: '' as ContainerStatus | '', containerNo: '', shipId: '' as number | '' })
const dialogVisible = ref(false)
const dialogTitle = ref('')
const editingItem = ref<Partial<ContainerVO>>({})

const formData = ref({
  containerNo: '',
  containerType: 'GP20' as ContainerType,
  loadDate: '',
  departureDate: '',
  arrivalDate: '',
  timeSlot: '',
  arrivalLocation: '',
  remarks: '',
})

// ===== 分配船只 =====
const assignDialogVisible = ref(false)
const assignDialogTitle = ref('')
const assignEditingContainer = ref<Partial<ContainerVO>>({})
const shipOptions = ref<ShipVO[]>([])
const assignForm = ref<AssignShipRequest>({ shipId: 0, loadDate: '' })

const statusOptions: ContainerStatus[] = ['CREATED', 'LOADED', 'DEPARTED', 'ARRIVED']
const typeOptions: ContainerType[] = ['GP20', 'GP40', 'HC40', 'HC45']

function containerStatusTag(type: ContainerStatus) {
  return { CREATED: 'info', LOADED: 'success', DEPARTED: 'warning', ARRIVED: 'primary' }[type] ?? 'info'
}

function containerStatusLabel(type: ContainerStatus) {
  return t(`logistics.containerStatus.${type}`)
}

function containerTypeLabel(type: ContainerType) {
  return t(`logistics.container.type.${type}`)
}

async function loadData() {
  loading.value = true
  try {
    const res = await containerApi.list({
      page: pagination.value.page,
      pageSize: pagination.value.pageSize,
      status: filterForm.value.status || undefined,
      shipId: filterForm.value.shipId || undefined,
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
function onReset() { filterForm.value = { status: '', containerNo: '', shipId: '' }; pagination.value.page = 0; loadData() }

function onNew() {
  editingItem.value = {}
  dialogTitle.value = t('logistics.container.newButton')
  formData.value = {
    containerNo: '',
    containerType: 'GP20',
    loadDate: '',
    departureDate: '',
    arrivalDate: '',
    timeSlot: '',
    arrivalLocation: '',
    remarks: '',
  }
  dialogVisible.value = true
}

function onEdit(row: ContainerVO) {
  editingItem.value = row
  dialogTitle.value = t('logistics.container.editTitle')
  formData.value = {
    containerNo: row.containerNo,
    containerType: row.containerType,
    loadDate: row.loadDate ?? '',
    departureDate: row.departureDate ?? '',
    arrivalDate: row.arrivalDate ?? '',
    timeSlot: row.timeSlot ?? '',
    arrivalLocation: row.arrivalLocation ?? '',
    remarks: row.remarks ?? '',
  }
  dialogVisible.value = true
}

async function onSubmit() {
  if (!formData.value.containerNo) {
    ElMessage.warning(t('logistics.container.containerNoRequired'))
    return
  }
  try {
    if (editingItem.value.id) {
      await containerApi.update(editingItem.value.id, {
        containerNo: formData.value.containerNo,
        containerType: formData.value.containerType,
        loadDate: formData.value.loadDate || undefined,
        departureDate: formData.value.departureDate || undefined,
        arrivalDate: formData.value.arrivalDate || undefined,
        timeSlot: formData.value.timeSlot.trim() || undefined,
        arrivalLocation: formData.value.arrivalLocation.trim() || undefined,
        remarks: formData.value.remarks.trim() || undefined,
      })
    } else {
      await containerApi.create({
        containerNo: formData.value.containerNo,
        containerType: formData.value.containerType,
        loadDate: formData.value.loadDate || undefined,
        departureDate: formData.value.departureDate || undefined,
        arrivalDate: formData.value.arrivalDate || undefined,
        timeSlot: formData.value.timeSlot.trim() || undefined,
        arrivalLocation: formData.value.arrivalLocation.trim() || undefined,
        remarks: formData.value.remarks.trim() || undefined,
      })
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
    await ElMessageBox.confirm(t('logistics.container.unassignSuccess'), t('common.delete'), { type: 'warning' })
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
        <el-form-item :label="$t('logistics.container.filter.status')">
          <el-select v-model="filterForm.status" :placeholder="$t('logistics.filter.all')" clearable style="width:150px">
            <el-option v-for="s in statusOptions" :key="s" :value="s" :label="containerStatusLabel(s)" />
          </el-select>
        </el-form-item>
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
        <el-form-item :label="$t('logistics.container.filter.containerNo')">
          <el-input v-model="filterForm.containerNo" :placeholder="$t('logistics.container.filter.containerNoHint')" clearable style="width:180px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">{{ $t('logistics.filter.search') }}</el-button>
          <el-button @click="onReset">{{ $t('logistics.filter.reset') }}</el-button>
          <el-button v-if="hasPermission('container:create')" type="primary" @click="onNew">
            <el-icon><Plus /></el-icon>{{ $t('logistics.container.newButton') }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="tableData" stripe style="width:100%" min-height="200">
        <el-table-column prop="containerNo" :label="$t('logistics.container.column.containerNo')" min-width="160" />
        <el-table-column :label="$t('logistics.container.column.containerType')" min-width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ containerTypeLabel(row.containerType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('logistics.container.column.totalCbm')" min-width="110" align="right">
          <template #default="{ row }">
            {{ row.totalCbm != null ? row.totalCbm.toFixed(4) : '-' }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('logistics.container.column.totalWeightKg')" min-width="120" align="right">
          <template #default="{ row }">
            {{ row.totalWeightKg != null ? row.totalWeightKg.toFixed(2) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="planCount" :label="$t('logistics.container.column.planCount')" min-width="90" align="center" />
        <el-table-column :label="$t('logistics.container.column.status')" min-width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="containerStatusTag(row.status)" size="small">{{ containerStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="loadDate" :label="$t('logistics.container.column.loadDate')" min-width="110" />
        <el-table-column prop="departureDate" :label="$t('logistics.container.column.departureDate')" min-width="110" />
        <el-table-column prop="arrivalDate" :label="$t('logistics.container.column.arrivalDate')" min-width="110" />
        <el-table-column :label="$t('logistics.container.column.shipName')" min-width="130">
          <template #default="{ row }">
            {{ row.shipName ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('logistics.container.column.timeSlot')" min-width="110">
          <template #default="{ row }">
            {{ row.timeSlot ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('logistics.container.column.arrivalLocation')" min-width="130">
          <template #default="{ row }">
            {{ row.arrivalLocation ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('logistics.column.createTime')" min-width="160">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column :label="$t('logistics.column.actions')" min-width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="hasPermission('container:update')" size="small" @click="onEdit(row)">{{ $t('common.edit') }}</el-button>
            <el-button v-if="hasPermission('container:update')" size="small" type="success" plain @click="onAssignShip(row)">
              {{ row.shipId ? t('logistics.container.dialog.assignShip') : t('logistics.container.dialog.assignShip') }}
            </el-button>
            <el-button v-if="hasPermission('container:update') && row.shipId" size="small" type="warning" plain @click="onUnassignShip(row)">
              {{ $t('logistics.container.unassignSuccess') }}
            </el-button>
            <el-button v-if="hasPermission('container:delete')" size="small" type="danger" plain @click="onDelete(row)">{{ $t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>

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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" :close-on-click-modal="false">
      <el-form label-width="160px">
        <el-form-item :label="$t('logistics.container.column.containerNo')" required>
          <el-input v-model="formData.containerNo" :placeholder="$t('logistics.container.containerNoPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('logistics.container.column.containerType')" required>
          <el-select v-model="formData.containerType" style="width:100%">
            <el-option v-for="t in typeOptions" :key="t" :value="t" :label="containerTypeLabel(t)" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('logistics.container.column.loadDate')">
          <el-date-picker v-model="formData.loadDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item :label="$t('logistics.container.column.departureDate')">
          <el-date-picker v-model="formData.departureDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item :label="$t('logistics.container.column.arrivalDate')">
          <el-date-picker v-model="formData.arrivalDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item :label="$t('logistics.container.column.timeSlot')">
          <el-input v-model="formData.timeSlot" :placeholder="$t('logistics.container.dialog.timeSlotPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('logistics.container.column.arrivalLocation')">
          <el-input v-model="formData.arrivalLocation" :placeholder="$t('logistics.container.dialog.arrivalLocationPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('logistics.container.column.remarks')">
          <el-input v-model="formData.remarks" :placeholder="$t('logistics.container.dialog.remarksPlaceholder')" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="onSubmit">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>

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
