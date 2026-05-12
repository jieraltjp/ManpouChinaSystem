<script setup lang="ts">
/**
 * 船只管理页面（SPEC-B12 Phase 2）。
 */
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
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
const tableData = ref<ShipVO[]>([])
const pagination = ref({ page: 0, pageSize: 20, total: 0 })

const filterForm = ref({
  shipName: '',
  shipNumber: '',
  arrivalPort: '',
})
const dialogVisible = ref(false)
const dialogTitle = ref('')
const editingItem = ref<Partial<ShipVO>>({})

const formData = ref({
  shipName: '',
  shipNumber: '',
  carrier: '',
  departurePort: '',
  arrivalPort: '',
})

async function loadData() {
  loading.value = true
  try {
    const res = await shipApi.list({
      page: pagination.value.page,
      pageSize: pagination.value.pageSize,
      shipName: filterForm.value.shipName || undefined,
      shipNumber: filterForm.value.shipNumber || undefined,
      arrivalPort: filterForm.value.arrivalPort || undefined,
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
function onReset() {
  filterForm.value = { shipName: '', shipNumber: '', arrivalPort: '' }
  pagination.value.page = 0
  loadData()
}

function onNew() {
  editingItem.value = {}
  dialogTitle.value = t('logistics.ship.newButton')
  formData.value = { shipName: '', shipNumber: '', carrier: '', departurePort: '', arrivalPort: '' }
  dialogVisible.value = true
}

function onEdit(row: ShipVO) {
  editingItem.value = row
  dialogTitle.value = t('logistics.ship.editTitle')
  formData.value = {
    shipName: row.shipName,
    shipNumber: row.shipNumber,
    carrier: row.carrier ?? '',
    departurePort: row.departurePort ?? '',
    arrivalPort: row.arrivalPort ?? '',
  }
  dialogVisible.value = true
}

async function onSubmit() {
  if (!formData.value.shipName.trim()) {
    ElMessage.warning(t('logistics.ship.nameRequired'))
    return
  }
  if (!editingItem.value.id && !formData.value.shipNumber.trim()) {
    ElMessage.warning(t('logistics.ship.numberRequired'))
    return
  }
  try {
    if (editingItem.value.id) {
      await shipApi.update(editingItem.value.id, {
        shipName: formData.value.shipName.trim(),
        carrier: formData.value.carrier.trim() || undefined,
        departurePort: formData.value.departurePort.trim() || undefined,
        arrivalPort: formData.value.arrivalPort.trim() || undefined,
      })
    } else {
      await shipApi.create({
        shipName: formData.value.shipName.trim(),
        shipNumber: formData.value.shipNumber.trim(),
        carrier: formData.value.carrier.trim() || undefined,
        departurePort: formData.value.departurePort.trim() || undefined,
        arrivalPort: formData.value.arrivalPort.trim() || undefined,
      })
    }
    ElMessage.success(t('common.message.saveSuccess'))
    dialogVisible.value = false
    loadData()
  } catch (e: any) {
    const msg = e?.response?.data?.message ?? e?.message ?? ''
    if (msg.includes('船号已存在') || msg.includes('ship_number_exists')) {
      ElMessage.error(t('logistics.ship.numberExists'))
    } else {
      ElMessage.error(t('common.message.saveFailed'))
    }
  }
}

async function onDelete(row: ShipVO) {
  const count = row.containerCount ?? 0
  const msg = count > 0
    ? t('logistics.ship.hasContainers', { count })
    : t('logistics.ship.deleteConfirm', { name: row.shipName, number: row.shipNumber, count })
  try {
    await ElMessageBox.confirm(msg, t('common.delete'), { type: 'warning' })
    await shipApi.delete(row.id)
    ElMessage.success(t('logistics.ship.deleteSuccess'))
    loadData()
  } catch (e: any) {
    if (e !== 'cancel') {
      const msg = e?.response?.data?.message ?? e?.message ?? ''
      if (msg.includes('has_containers') || msg.includes('有')) {
        ElMessage.warning(t('logistics.ship.hasContainers', { count }))
      }
    }
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
        <el-form-item :label="$t('logistics.ship.filter.shipName')">
          <el-input v-model="filterForm.shipName" :placeholder="$t('logistics.ship.filter.shipNameHint')" clearable style="width:160px" />
        </el-form-item>
        <el-form-item :label="$t('logistics.ship.filter.shipNumber')">
          <el-input v-model="filterForm.shipNumber" :placeholder="$t('logistics.ship.filter.shipNumberHint')" clearable style="width:140px" />
        </el-form-item>
        <el-form-item :label="$t('logistics.ship.filter.arrivalPort')">
          <el-input v-model="filterForm.arrivalPort" :placeholder="$t('logistics.ship.filter.arrivalPortHint')" clearable style="width:140px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">{{ $t('logistics.filter.search') }}</el-button>
          <el-button @click="onReset">{{ $t('logistics.filter.reset') }}</el-button>
          <el-button v-if="hasPermission('ship:create')" type="primary" @click="onNew">
            <el-icon><Plus /></el-icon>{{ $t('logistics.ship.newButton') }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="tableData" stripe style="width:100%" min-height="200">
        <el-table-column prop="shipName" :label="$t('logistics.ship.column.shipName')" min-width="140" />
        <el-table-column prop="shipNumber" :label="$t('logistics.ship.column.shipNumber')" min-width="120" />
        <el-table-column prop="carrier" :label="$t('logistics.ship.column.carrier')" min-width="130" />
        <el-table-column prop="departurePort" :label="$t('logistics.ship.column.departurePort')" min-width="120" />
        <el-table-column prop="arrivalPort" :label="$t('logistics.ship.column.arrivalPort')" min-width="120" />
        <el-table-column :label="$t('logistics.ship.column.containerCount')" min-width="110" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.containerCount" type="info" size="small">{{ row.containerCount }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('logistics.ship.column.createTime')" min-width="160">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column :label="$t('logistics.column.actions')" min-width="140" fixed="right">
          <template #default="{ row }">
            <el-button v-if="hasPermission('ship:update')" size="small" @click="onEdit(row)">{{ $t('common.edit') }}</el-button>
            <el-button v-if="hasPermission('ship:delete') && !row.containerCount" size="small" type="danger" plain @click="onDelete(row)">{{ $t('common.delete') }}</el-button>
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
        <el-form-item :label="$t('logistics.ship.dialog.shipName')" required>
          <el-input v-model="formData.shipName" :placeholder="$t('logistics.ship.dialog.shipNamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('logistics.ship.dialog.shipNumber')" :required="!editingItem.id">
          <el-input v-model="formData.shipNumber" :placeholder="$t('logistics.ship.dialog.shipNumberPlaceholder')" :disabled="!!editingItem.id" />
        </el-form-item>
        <el-form-item :label="$t('logistics.ship.dialog.carrier')">
          <el-input v-model="formData.carrier" :placeholder="$t('logistics.ship.dialog.carrierPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('logistics.ship.dialog.departurePort')">
          <el-input v-model="formData.departurePort" :placeholder="$t('logistics.ship.dialog.departurePortPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('logistics.ship.dialog.arrivalPort')">
          <el-input v-model="formData.arrivalPort" :placeholder="$t('logistics.ship.dialog.arrivalPortPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="onSubmit">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>
