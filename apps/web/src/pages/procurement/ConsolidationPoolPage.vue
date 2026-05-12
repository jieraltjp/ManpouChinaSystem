<script setup lang="ts">
/**
 * 拼柜池管理页面（v1.5.0，SPEC-B00 Issue #8）
 */
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { consolidationPoolApi, type ConsolidationPoolVO, type ConsolidationPoolStatus } from '@/api/logistics'
import { containerApi } from '@/api/logistics'
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
const tableData = ref<ConsolidationPoolVO[]>([])
const pagination = ref({ page: 0, pageSize: 20, total: 0 })

const filterForm = ref({ status: '' as ConsolidationPoolStatus | '', destinationPort: '' })
const dialogVisible = ref(false)
const dialogTitle = ref('')
const editingItem = ref<Partial<ConsolidationPoolVO>>({})

const formData = ref({ destinationPort: '', containerThresholdCbm: 70 })

const statusOptions: ConsolidationPoolStatus[] = ['OPEN', 'PENDING', 'LOADED', 'SHIPPED']

function poolStatusTag(type: ConsolidationPoolStatus) {
  return { OPEN: 'success', PENDING: 'warning', LOADED: 'primary', SHIPPED: 'info' }[type] ?? 'info'
}

function poolStatusLabel(type: ConsolidationPoolStatus) {
  return t(`logistics.poolStatus.${type}`)
}

async function loadData() {
  loading.value = true
  try {
    const res = await consolidationPoolApi.list({
      page: pagination.value.page,
      pageSize: pagination.value.pageSize,
      status: filterForm.value.status || undefined,
      destinationPort: filterForm.value.destinationPort || undefined,
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
function onReset() { filterForm.value = { status: '', destinationPort: '' }; pagination.value.page = 0; loadData() }

function onNew() {
  editingItem.value = {}
  dialogTitle.value = t('logistics.pool.newButton')
  formData.value = { destinationPort: '', containerThresholdCbm: 70 }
  dialogVisible.value = true
}

function onEdit(row: ConsolidationPoolVO) {
  editingItem.value = row
  dialogTitle.value = t('logistics.pool.editTitle')
  formData.value = { destinationPort: row.destinationPort, containerThresholdCbm: row.containerThresholdCbm ?? 70 }
  dialogVisible.value = true
}

async function onSubmit() {
  if (!formData.value.destinationPort) {
    ElMessage.warning(t('logistics.pool.destinationRequired'))
    return
  }
  try {
    if (editingItem.value.id) {
      await consolidationPoolApi.update(editingItem.value.id, {
        destinationPort: formData.value.destinationPort,
        containerThresholdCbm: formData.value.containerThresholdCbm,
      })
    } else {
      await consolidationPoolApi.create({
        destinationPort: formData.value.destinationPort,
        containerThresholdCbm: formData.value.containerThresholdCbm,
      })
    }
    ElMessage.success(t('common.message.saveSuccess'))
    dialogVisible.value = false
    loadData()
  } catch {
    ElMessage.error(t('common.message.saveFailed'))
  }
}

async function onDelete(row: ConsolidationPoolVO) {
  try {
    await ElMessageBox.confirm(t('logistics.pool.deleteConfirm'), t('common.delete'), { type: 'warning' })
    await consolidationPoolApi.delete(row.id)
    ElMessage.success(t('common.message.deleteSuccess'))
    loadData()
  } catch {
    // cancelled
  }
}

async function onCreateContainer(row: ConsolidationPoolVO) {
  try {
    await ElMessageBox.confirm(
      t('logistics.pool.createContainerConfirm', { poolCode: row.poolCode }),
      t('logistics.pool.createContainer'), { type: 'info' })
    const res = await containerApi.create({ containerNo: '', containerType: 'GP20', poolId: row.id })
    ElMessage.success(t('logistics.pool.containerCreated') + ' ID: ' + res.data)
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
        <el-form-item :label="$t('logistics.pool.filter.status')">
          <el-select v-model="filterForm.status" :placeholder="$t('logistics.filter.all')" clearable style="width:140px">
            <el-option v-for="s in statusOptions" :key="s" :value="s" :label="poolStatusLabel(s)" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('logistics.pool.filter.destinationPort')">
          <el-input v-model="filterForm.destinationPort" :placeholder="$t('logistics.pool.filter.destinationHint')" clearable style="width:180px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">{{ $t('logistics.filter.search') }}</el-button>
          <el-button @click="onReset">{{ $t('logistics.filter.reset') }}</el-button>
          <el-button type="primary" @click="onNew" v-if="hasPermission('consolidation:create')">
            <el-icon><Plus /></el-icon>{{ $t('logistics.pool.newButton') }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="tableData" stripe style="width:100%" min-height="200">
        <el-table-column prop="poolCode" :label="$t('logistics.pool.column.poolCode')" min-width="160" />
        <el-table-column prop="destinationPort" :label="$t('logistics.pool.column.destinationPort')" min-width="140" />
        <el-table-column :label="$t('logistics.pool.column.totalCbm')" min-width="120" align="right">
          <template #default="{ row }">
            {{ row.totalCbm != null ? row.totalCbm.toFixed(4) : '-' }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('logistics.pool.column.totalWeightKg')" min-width="120" align="right">
          <template #default="{ row }">
            {{ row.totalWeightKg != null ? row.totalWeightKg.toFixed(2) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="planCount" :label="$t('logistics.pool.column.planCount')" min-width="90" align="center" />
        <el-table-column :label="$t('logistics.pool.column.threshold')" min-width="110" align="right">
          <template #default="{ row }">
            {{ row.containerThresholdCbm ?? 70 }} m³
          </template>
        </el-table-column>
        <el-table-column :label="$t('logistics.pool.column.status')" min-width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="poolStatusTag(row.status)" size="small">{{ poolStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('logistics.column.createTime')" min-width="160">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column :label="$t('logistics.column.actions')" min-width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="onEdit(row)" v-if="hasPermission('consolidation:update')">{{ $t('common.edit') }}</el-button>
            <el-button v-if="hasPermission('container:create')" size="small" type="primary" plain @click="onCreateContainer(row)">{{ $t('logistics.pool.action.createContainer') }}</el-button>
            <el-button size="small" type="danger" plain @click="onDelete(row)" v-if="hasPermission('consolidation:delete')">{{ $t('common.delete') }}</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px" :close-on-click-modal="false">
      <el-form label-width="160px">
        <el-form-item :label="$t('logistics.pool.column.destinationPort')" required>
          <el-input v-model="formData.destinationPort" :placeholder="$t('logistics.pool.destinationPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('logistics.pool.column.threshold')">
          <el-input-number v-model="formData.containerThresholdCbm" :min="1" :max="200" :precision="2" />
          <span style="margin-left:8px">{{ $t('common.units.m3') }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="onSubmit">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>
