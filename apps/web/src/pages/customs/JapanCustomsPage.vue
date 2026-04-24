<template>
  <div class="page">
    <div class="page-header">
      <h2 class="page-title">{{ $t('japanCustoms.title') }}</h2>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#6366F1"><Document /></el-icon></div>
            <div><div class="stat-value">{{ pagination.total }}</div><div class="stat-label">{{ $t('japanCustoms.stat.total') }}</div></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#F59E0B"><Clock /></el-icon></div>
            <div><div class="stat-value">{{ statusCount.PENDING }}</div><div class="stat-label">{{ $t('japanCustoms.stat.pending') }}</div></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#3B82F6"><Loading /></el-icon></div>
            <div><div class="stat-value">{{ statusCount.IN_PROGRESS }}</div><div class="stat-label">{{ $t('japanCustoms.stat.inProgress') }}</div></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#16A34A"><CircleCheck /></el-icon></div>
            <div><div class="stat-value">{{ statusCount.CLEARED }}</div><div class="stat-label">{{ $t('japanCustoms.stat.cleared') }}</div></div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filterForm">
        <el-form-item :label="$t('japanCustoms.filter.entryNo')">
          <el-input v-model="filterForm.keyword" :placeholder="$t('japanCustoms.filter.entryNoPlaceholder')" clearable style="width:180px" />
        </el-form-item>
        <el-form-item :label="$t('japanCustoms.filter.procurementId')">
          <el-input-number v-model="filterForm.procurementId" :min="1" style="width:130px" clearable />
        </el-form-item>
        <el-form-item :label="$t('japanCustoms.filter.status')">
          <el-select v-model="filterForm.status" :placeholder="$t('common.all')" clearable style="width:140px">
            <el-option value="PENDING" :label="$t('japanCustoms.status.pending')" />
            <el-option value="IN_PROGRESS" :label="$t('japanCustoms.status.inProgress')" />
            <el-option value="CLEARED" :label="$t('japanCustoms.status.cleared')" />
            <el-option value="FAILED" :label="$t('japanCustoms.status.failed')" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearchFromButton">{{ $t('common.search') }}</el-button>
          <el-button @click="onReset">{{ $t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表 -->
    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="tableData" stripe style="width:100%">
        <el-table-column prop="customsEntryNo" :label="$t('japanCustoms.column.entryNo')" width="180">
          <template #default="{ row }">
            <span class="code-badge">{{ row.customsEntryNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="procurementId" :label="$t('japanCustoms.column.procurementId')" width="80" align="center">
          <template #default="{ row }">
            <span v-if="row.procurementId">{{ row.procurementId }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="subProductCode" :label="$t('japanCustoms.column.subProductCode')" width="80" align="center">
          <template #default="{ row }">
            <span v-if="row.subProductCode">{{ row.subProductCode }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="arrivalPort" :label="$t('japanCustoms.column.arrivalPort')" width="90" />
        <el-table-column prop="arrivalDate" :label="$t('japanCustoms.column.arrivalDate')" width="120" />
        <el-table-column prop="customsBroker" :label="$t('japanCustoms.column.broker')" width="140" show-overflow-tooltip />
        <el-table-column prop="importDutyPaid" :label="$t('japanCustoms.column.importDuty')" width="120" align="right">
          <template #default="{ row }">
            <span v-if="row.importDutyPaid !== null" class="money">{{ row.importDutyPaid.toLocaleString('ja-JP') }} JPY</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="consumptionTaxPaid" :label="$t('japanCustoms.column.consumptionTax')" width="120" align="right">
          <template #default="{ row }">
            <span v-if="row.consumptionTaxPaid !== null" class="money">{{ row.consumptionTaxPaid.toLocaleString('ja-JP') }} JPY</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="clearanceDate" :label="$t('japanCustoms.column.clearanceDate')" width="120" />
        <el-table-column prop="status" :label="$t('japanCustoms.column.status')" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('japanCustoms.column.action')" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="onView(row)">{{ $t('japanCustoms.action.detail') }}</el-button>
            <template v-if="row.status === 'PENDING'">
              <el-button link type="success" size="small" :loading="actionLoading === row.id + '-start'" @click.stop="onStart(row)">{{ $t('japanCustoms.action.start') }}</el-button>
              <el-button link type="danger" size="small" :loading="actionLoading === row.id + '-delete'" @click.stop="onDelete(row)">{{ $t('common.delete') }}</el-button>
            </template>
            <template v-else-if="row.status === 'IN_PROGRESS'">
              <el-button link type="success" size="small" :loading="actionLoading === row.id + '-complete'" @click.stop="onComplete(row)">{{ $t('japanCustoms.action.complete') }}</el-button>
              <el-button link type="danger" size="small" :loading="actionLoading === row.id + '-fail'" @click.stop="onFail(row)">{{ $t('japanCustoms.action.fail') }}</el-button>
            </template>
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
          @size-change="onSearch"
          @current-change="onSearch"
        />
      </div>
    </el-card>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" :title="$t('japanCustoms.drawerTitle')" size="600px" direction="rtl">
      <el-descriptions :column="2" border v-if="currentRow">
        <el-descriptions-item :label="$t('japanCustoms.column.entryNo')">
          <span class="code-badge">{{ currentRow.customsEntryNo }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('japanCustoms.column.status')">
          <el-tag :type="statusTagType(currentRow.status)" size="small">{{ statusLabel(currentRow.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('japanCustoms.column.procurementId')">{{ currentRow.procurementId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('japanCustoms.column.subProductCode')">{{ currentRow.subProductCode ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('japanCustoms.column.domesticCustomsId')">{{ currentRow.domesticCustomsId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('japanCustoms.column.arrivalPort')">{{ currentRow.arrivalPort ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('japanCustoms.column.arrivalDate')">{{ currentRow.arrivalDate ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('japanCustoms.column.broker')">{{ currentRow.customsBroker ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('japanCustoms.column.brokerPhone')">{{ currentRow.brokerPhone ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('japanCustoms.column.brokerContact')">{{ currentRow.brokerContact ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('japanCustoms.column.weight')">
          <span v-if="currentRow.declaredWeightKg !== null">{{ currentRow.declaredWeightKg }} kg</span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('japanCustoms.column.volume')">
          <span v-if="currentRow.declaredVolumeCbm !== null">{{ currentRow.declaredVolumeCbm }} m³</span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('japanCustoms.column.importDuty')">
          <span v-if="currentRow.importDutyPaid !== null" class="money">{{ currentRow.importDutyPaid.toLocaleString('ja-JP') }} JPY</span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('japanCustoms.column.consumptionTax')">
          <span v-if="currentRow.consumptionTaxPaid !== null" class="money">{{ currentRow.consumptionTaxPaid.toLocaleString('ja-JP') }} JPY</span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('japanCustoms.column.clearanceDate')">{{ currentRow.clearanceDate ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('japanCustoms.column.remarks')" :span="2">{{ currentRow.remarks || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('japanCustoms.column.createBy')">{{ currentRow.createBy || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('japanCustoms.column.createTime')">{{ currentRow.createTime || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <!-- 完成清关弹窗 -->
    <el-dialog v-model="completeDialogVisible" :title="$t('japanCustoms.completeDialogTitle')" width="480px">
      <el-form :model="completeForm" label-width="140px">
        <el-form-item :label="$t('japanCustoms.completeDialog.importDuty')">
          <el-input-number v-model="completeForm.importDutyPaid" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item :label="$t('japanCustoms.completeDialog.consumptionTax')">
          <el-input-number v-model="completeForm.consumptionTaxPaid" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item :label="$t('japanCustoms.completeDialog.clearanceDate')">
          <el-date-picker v-model="completeForm.clearanceDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeDialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="success" :loading="actionLoading.startsWith('complete-')" @click="onCompleteConfirm">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- 失败原因弹窗 -->
    <el-dialog v-model="failDialogVisible" :title="$t('japanCustoms.failDialogTitle')" width="420px">
      <el-form>
        <el-form-item :label="$t('japanCustoms.failDialog.reasonLabel')">
          <el-input v-model="failReason" type="textarea" :rows="3" :placeholder="$t('japanCustoms.failDialog.reasonPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="failDialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="danger" :loading="actionLoading.startsWith('fail-')" @click="onFailConfirm">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Clock, Loading, CircleCheck } from '@element-plus/icons-vue'
import { japanCustomsApi, type JapanCustomsVO, type JapanCustomsStatus } from '@/api/japanCustoms'
import { useI18n } from 'vue-i18n'

const loading = ref(false)
const drawerVisible = ref(false)
const completeDialogVisible = ref(false)
const failDialogVisible = ref(false)
const actionLoading = ref('')
const failReason = ref('')
const completingRowId = ref<number | null>(null)
const failingRowId = ref<number | null>(null)

const currentRow = ref<JapanCustomsVO | null>(null)
const filterForm = reactive({
  keyword: '',
  procurementId: undefined as number | undefined,
  status: '' as JapanCustomsStatus | '',
})
const pagination = reactive({ page: 1, pageSize: 20, total: 0 })
const tableData = ref<JapanCustomsVO[]>([])
const completeForm = reactive({
  importDutyPaid: 0,
  consumptionTaxPaid: 0,
  clearanceDate: '',
})

const { t } = useI18n()

const statusCount = computed(() => {
  const counts: Record<string, number> = { PENDING: 0, IN_PROGRESS: 0, CLEARED: 0, FAILED: 0 }
  tableData.value.forEach(r => { if (r.status in counts) counts[r.status]++ })
  return counts
})

function statusLabel(status?: string): string {
  const map: Record<string, string> = {
    PENDING: t('japanCustoms.status.pending'),
    IN_PROGRESS: t('japanCustoms.status.inProgress'),
    CLEARED: t('japanCustoms.status.cleared'),
    FAILED: t('japanCustoms.status.failed'),
  }
  return map[status ?? ''] ?? status ?? '-'
}

function statusTagType(status?: string): string {
  const map: Record<string, string> = {
    PENDING: 'warning',
    IN_PROGRESS: 'primary',
    CLEARED: 'success',
    FAILED: 'danger',
  }
  return map[status ?? ''] ?? 'info'
}

async function loadData() {
  loading.value = true
  try {
    const res = await japanCustomsApi.list({
      page: pagination.page - 1,
      pageSize: pagination.pageSize,
      status: filterForm.status || undefined,
      procurementId: filterForm.procurementId,
    })
    const data = res.data.data
    tableData.value = data.content
    pagination.total = data.totalElements
  } catch (e: unknown) {
    console.error('[JapanCustomsPage] loadData failed', e)
    ElMessage.error(t('japanCustoms.message.loadFailed'))
  } finally {
    loading.value = false
  }
}

function onSearch() { loadData() }
function onSearchFromButton() { pagination.page = 1; loadData() }

function onReset() {
  filterForm.keyword = ''
  filterForm.procurementId = undefined
  filterForm.status = ''
  pagination.page = 1
  loadData()
}

function onView(row: JapanCustomsVO) {
  currentRow.value = row
  drawerVisible.value = true
}

async function onStart(row: JapanCustomsVO) {
  actionLoading.value = `${row.id}-start`
  try {
    await japanCustomsApi.start(row.id)
    ElMessage.success(t('japanCustoms.message.startSuccess'))
    loadData()
  } catch (e) {
    console.error('[JapanCustomsPage] start failed', e)
    ElMessage.error(t('japanCustoms.message.actionFailed'))
  } finally {
    actionLoading.value = ''
  }
}

function onComplete(row: JapanCustomsVO) {
  completingRowId.value = row.id
  completeForm.importDutyPaid = 0
  completeForm.consumptionTaxPaid = 0
  completeForm.clearanceDate = ''
  completeDialogVisible.value = true
}

async function onCompleteConfirm() {
  if (!completingRowId.value) return
  actionLoading.value = `complete-${completingRowId.value}`
  try {
    await japanCustomsApi.complete(completingRowId.value, {
      importDutyPaid: completeForm.importDutyPaid,
      consumptionTaxPaid: completeForm.consumptionTaxPaid,
      clearanceDate: completeForm.clearanceDate,
    })
    ElMessage.success(t('japanCustoms.message.completeSuccess'))
    completeDialogVisible.value = false
    loadData()
  } catch (e) {
    console.error('[JapanCustomsPage] complete failed', e)
    ElMessage.error(t('japanCustoms.message.actionFailed'))
  } finally {
    actionLoading.value = ''
    completingRowId.value = null
  }
}

function onFail(row: JapanCustomsVO) {
  failingRowId.value = row.id
  failReason.value = ''
  failDialogVisible.value = true
}

async function onFailConfirm() {
  if (!failingRowId.value) return
  actionLoading.value = `fail-${failingRowId.value}`
  try {
    await japanCustomsApi.fail(failingRowId.value, { reason: failReason.value })
    ElMessage.success(t('japanCustoms.message.failSuccess'))
    failDialogVisible.value = false
    loadData()
  } catch (e) {
    console.error('[JapanCustomsPage] fail failed', e)
    ElMessage.error(t('japanCustoms.message.actionFailed'))
  } finally {
    actionLoading.value = ''
    failingRowId.value = null
  }
}

async function onDelete(row: JapanCustomsVO) {
  try {
    await ElMessageBox.confirm(t('japanCustoms.message.deleteConfirm'), t('common.warning'), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      type: 'warning',
    })
  } catch { return }
  actionLoading.value = `${row.id}-delete`
  try {
    await japanCustomsApi.delete(row.id)
    ElMessage.success(t('japanCustoms.message.deleteSuccess'))
    loadData()
  } catch (e) {
    console.error('[JapanCustomsPage] delete failed', e)
    ElMessage.error(t('japanCustoms.message.actionFailed'))
  } finally {
    actionLoading.value = ''
  }
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
.code-badge { color: var(--color-primary); font-family: monospace; font-size: 12px; font-weight: 700; background: var(--color-primary-pale); padding: 3px 9px; border-radius: 5px; }
.money { color: #16A34A; font-weight: 600; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
