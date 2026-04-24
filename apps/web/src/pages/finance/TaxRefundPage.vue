<template>
  <div class="page">
    <div class="page-header">
      <h2 class="page-title">{{ $t('taxRefund.title') }}</h2>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#6366F1"><Document /></el-icon></div>
            <div><div class="stat-value">{{ pagination.total }}</div><div class="stat-label">{{ $t('taxRefund.stat.total') }}</div></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#F59E0B"><Clock /></el-icon></div>
            <div><div class="stat-value">{{ statusCount.APPLYING }}</div><div class="stat-label">{{ $t('taxRefund.stat.applying') }}</div></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#16A34A"><CircleCheck /></el-icon></div>
            <div><div class="stat-value">{{ statusCount.COMPLETED }}</div><div class="stat-label">{{ $t('taxRefund.stat.completed') }}</div></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#DC2626"><Close /></el-icon></div>
            <div><div class="stat-value">{{ statusCount.NO_REFUND }}</div><div class="stat-label">{{ $t('taxRefund.stat.noRefund') }}</div></div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filterForm">
        <el-form-item :label="$t('taxRefund.filter.procurementId')">
          <el-input-number v-model="filterForm.procurementId" :min="1" style="width:130px" clearable />
        </el-form-item>
        <el-form-item :label="$t('taxRefund.filter.status')">
          <el-select v-model="filterForm.status" :placeholder="$t('common.all')" clearable style="width:140px">
            <el-option value="APPLYING" :label="$t('taxRefund.status.applying')" />
            <el-option value="COMPLETED" :label="$t('taxRefund.status.completed')" />
            <el-option value="NO_REFUND" :label="$t('taxRefund.status.noRefund')" />
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
      <el-table v-loading="loading" :data="tableData" stripe style="width:100%" min-height="200">
        <el-table-column prop="refundCode" :label="$t('taxRefund.column.refundCode')" min-width="180">
          <template #default="{ row }">
            <span class="code-badge">{{ row.refundCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="procurementId" :label="$t('taxRefund.column.procurementId')" min-width="100" align="center">
          <template #default="{ row }">
            <span v-if="row.procurementId">{{ row.procurementId }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="billingType" :label="$t('taxRefund.column.billingType')" min-width="130" align="center" />
        <el-table-column prop="taxPoint" :label="$t('taxRefund.column.taxPoint')" min-width="90" align="right">
          <template #default="{ row }">
            <span v-if="row.taxPoint !== null">{{ (Number(row.taxPoint) * 100).toFixed(1) }}%</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="estimatedRefundRmb" :label="$t('taxRefund.column.estimatedRefundRmb')" min-width="140" align="right">
          <template #default="{ row }">
            <span v-if="row.estimatedRefundRmb !== null" class="money">{{ Number(row.estimatedRefundRmb).toLocaleString() }} RMB</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="actualRefundRmb" :label="$t('taxRefund.column.actualRefundRmb')" min-width="140" align="right">
          <template #default="{ row }">
            <span v-if="row.actualRefundRmb !== null" class="money-success">{{ Number(row.actualRefundRmb).toLocaleString() }} RMB</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="refundDate" :label="$t('taxRefund.column.refundDate')" min-width="120" />
        <el-table-column prop="refundBank" :label="$t('taxRefund.column.refundBank')" min-width="140" show-overflow-tooltip />
        <el-table-column prop="status" :label="$t('taxRefund.column.status')" min-width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('taxRefund.column.action')" min-width="180" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="onView(row)">{{ $t('taxRefund.action.detail') }}</el-button>
            <template v-if="row.status === 'APPLYING'">
              <el-button link type="success" size="small" :loading="actionLoading === row.id + '-complete'" @click.stop="onComplete(row)">{{ $t('taxRefund.action.complete') }}</el-button>
              <el-button link type="warning" size="small" :loading="actionLoading === row.id + '-no-refund'" @click.stop="onNoRefund(row)">{{ $t('taxRefund.action.noRefund') }}</el-button>
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
    <el-drawer v-model="drawerVisible" :title="$t('taxRefund.drawerTitle')" size="600px" direction="rtl">
      <el-descriptions :column="2" border v-if="currentRow">
        <el-descriptions-item :label="$t('taxRefund.column.refundCode')">
          <span class="code-badge">{{ currentRow.refundCode }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('taxRefund.column.status')">
          <el-tag :type="statusTagType(currentRow.status)" size="small">{{ statusLabel(currentRow.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('taxRefund.column.procurementId')">{{ currentRow.procurementId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('taxRefund.column.japanCustomsId')">{{ currentRow.japanCustomsId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('taxRefund.column.billingType')">{{ currentRow.billingType ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('taxRefund.column.taxPoint')">
          <span v-if="currentRow.taxPoint !== null">{{ (Number(currentRow.taxPoint) * 100).toFixed(1) }}%</span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('taxRefund.column.priceRmb')">
          <span v-if="currentRow.priceRmb !== null">{{ Number(currentRow.priceRmb).toLocaleString() }} RMB</span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('taxRefund.column.quantity')">{{ currentRow.quantity ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('taxRefund.column.estimatedRefundRmb')">
          <span v-if="currentRow.estimatedRefundRmb !== null" class="money">{{ Number(currentRow.estimatedRefundRmb).toLocaleString() }} RMB</span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('taxRefund.column.actualRefundRmb')">
          <span v-if="currentRow.actualRefundRmb !== null" class="money-success">{{ Number(currentRow.actualRefundRmb).toLocaleString() }} RMB</span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('taxRefund.column.refundDate')">{{ currentRow.refundDate ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('taxRefund.column.refundBank')">{{ currentRow.refundBank ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('taxRefund.column.createBy')">{{ currentRow.createBy || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('taxRefund.column.createTime')">{{ currentRow.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('taxRefund.column.remarks')" :span="2">{{ currentRow.remarks || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <!-- 完成退税弹窗 -->
    <el-dialog v-model="completeDialogVisible" :title="$t('taxRefund.completeDialogTitle')" width="480px">
      <el-form :model="completeForm" label-width="150px">
        <el-form-item :label="$t('taxRefund.completeDialog.actualRefundRmb')" required>
          <el-input-number v-model="completeForm.actualRefundRmb" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item :label="$t('taxRefund.completeDialog.refundDate')" required>
          <el-date-picker v-model="completeForm.refundDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item :label="$t('taxRefund.completeDialog.refundBank')">
          <el-input v-model="completeForm.refundBank" :placeholder="$t('taxRefund.completeDialog.refundBankPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeDialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="success" :loading="actionLoading.startsWith('complete-')" @click="onCompleteConfirm">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, Clock, CircleCheck, Close } from '@element-plus/icons-vue'
import { taxRefundApi, type TaxRefundVO, type TaxRefundStatus } from '@/api/taxRefund'
import { useI18n } from 'vue-i18n'

const loading = ref(false)
const drawerVisible = ref(false)
const completeDialogVisible = ref(false)
const actionLoading = ref('')

const currentRow = ref<TaxRefundVO | null>(null)
const filterForm = reactive({
  procurementId: undefined as number | undefined,
  status: '' as TaxRefundStatus | '',
})
const pagination = reactive({ page: 1, pageSize: 20, total: 0 })
const tableData = ref<TaxRefundVO[]>([])
const completeForm = reactive({
  actualRefundRmb: 0,
  refundDate: '',
  refundBank: '',
})
const completingRowId = ref<number | null>(null)

const { t } = useI18n()

const statusCount = computed(() => {
  const counts: Record<string, number> = { APPLYING: 0, COMPLETED: 0, NO_REFUND: 0 }
  tableData.value.forEach(r => { if (r.status in counts) counts[r.status]++ })
  return counts
})

function statusLabel(status?: string): string {
  const map: Record<string, string> = {
    APPLYING: t('taxRefund.status.applying'),
    COMPLETED: t('taxRefund.status.completed'),
    NO_REFUND: t('taxRefund.status.noRefund'),
  }
  return map[status ?? ''] ?? status ?? '-'
}

function statusTagType(status?: string): string {
  const map: Record<string, string> = {
    APPLYING: 'warning',
    COMPLETED: 'success',
    NO_REFUND: 'info',
  }
  return map[status ?? ''] ?? 'info'
}

async function loadData() {
  loading.value = true
  try {
    const res = await taxRefundApi.list({
      page: pagination.page - 1,
      pageSize: pagination.pageSize,
      status: filterForm.status || undefined,
      procurementId: filterForm.procurementId,
    })
    const data = res.data.data
    tableData.value = data?.content ?? []
    pagination.total = data?.totalElements ?? 0
  } catch (e: unknown) {
    console.error('[TaxRefundPage] loadData failed', e)
    ElMessage.error(t('taxRefund.message.loadFailed'))
  } finally {
    loading.value = false
  }
}

function onSearch() { loadData() }
function onSearchFromButton() { pagination.page = 1; loadData() }

function onReset() {
  filterForm.procurementId = undefined
  filterForm.status = ''
  pagination.page = 1
  loadData()
}

function onView(row: TaxRefundVO) {
  currentRow.value = row
  drawerVisible.value = true
}

function onComplete(row: TaxRefundVO) {
  completingRowId.value = row.id
  completeForm.actualRefundRmb = Number(row.estimatedRefundRmb) || 0
  completeForm.refundDate = ''
  completeForm.refundBank = ''
  completeDialogVisible.value = true
}

async function onCompleteConfirm() {
  if (!completingRowId.value) return
  if (!completeForm.refundDate) {
    ElMessage.error(t('taxRefund.message.refundDateRequired'))
    return
  }
  actionLoading.value = `complete-${completingRowId.value}`
  try {
    await taxRefundApi.complete(completingRowId.value, {
      actualRefundRmb: completeForm.actualRefundRmb,
      refundDate: completeForm.refundDate,
      refundBank: completeForm.refundBank || undefined,
    })
    ElMessage.success(t('taxRefund.message.completeSuccess'))
    completeDialogVisible.value = false
    loadData()
  } catch (e) {
    console.error('[TaxRefundPage] complete failed', e)
    ElMessage.error(t('taxRefund.message.actionFailed'))
  } finally {
    actionLoading.value = ''
    completingRowId.value = null
  }
}

async function onNoRefund(row: TaxRefundVO) {
  actionLoading.value = `${row.id}-no-refund`
  try {
    await taxRefundApi.markNoRefund(row.id)
    ElMessage.success(t('taxRefund.message.noRefundSuccess'))
    loadData()
  } catch (e) {
    console.error('[TaxRefundPage] noRefund failed', e)
    ElMessage.error(t('taxRefund.message.actionFailed'))
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
.money { color: #F59E0B; font-weight: 600; }
.money-success { color: #16A34A; font-weight: 600; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
