<template>
  <div class="page">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="5">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#6366F1"><Goods /></el-icon></div>
            <div><div class="stat-value">{{ pagination.total }}</div><div class="stat-label">{{ $t('sales.stat.total') }}</div></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="5">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#16A34A"><CircleCheck /></el-icon></div>
            <div><div class="stat-value">{{ statusCount.LISTED }}</div><div class="stat-label">{{ $t('sales.stat.listed') }}</div></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="5">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#F59E0B"><Warning /></el-icon></div>
            <div><div class="stat-value">{{ statusCount.LOW_STOCK }}</div><div class="stat-label">{{ $t('sales.stat.lowStock') }}</div></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="5">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#DC2626"><CloseBold /></el-icon></div>
            <div><div class="stat-value">{{ statusCount.OUT_OF_STOCK }}</div><div class="stat-label">{{ $t('sales.stat.outOfStock') }}</div></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#6B7280"><Remove /></el-icon></div>
            <div><div class="stat-value">{{ statusCount.DISCONTINUED }}</div><div class="stat-label">{{ $t('sales.stat.discontinued') }}</div></div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filterForm">
        <el-form-item :label="$t('sales.filter.productCode')">
          <el-input v-model="filterForm.productCode" :placeholder="$t('sales.filter.productCodePlaceholder')" clearable style="width:150px" />
        </el-form-item>
        <el-form-item :label="$t('sales.filter.salesChannel')">
          <el-select v-model="filterForm.salesChannel" :placeholder="$t('common.all')" clearable style="width:140px">
            <el-option value="AMAZON" :label="$t('sales.channel.amazon')" />
            <el-option value="MERCALI" :label="$t('sales.channel.mercali')" />
            <el-option value="SELF_SITE" :label="$t('sales.channel.selfSite')" />
            <el-option value="OTHER" :label="$t('sales.channel.other')" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('sales.filter.status')">
          <el-select v-model="filterForm.status" :placeholder="$t('common.all')" clearable style="width:140px">
            <el-option value="LISTED" :label="$t('sales.status.listed')" />
            <el-option value="LOW_STOCK" :label="$t('sales.status.lowStock')" />
            <el-option value="OUT_OF_STOCK" :label="$t('sales.status.outOfStock')" />
            <el-option value="DISCONTINUED" :label="$t('sales.status.discontinued')" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearchFromButton">{{ $t('common.search') }}</el-button>
          <el-button @click="onReset">{{ $t('common.reset') }}</el-button>
          <el-button type="primary" @click="onCreate" v-if="hasPermission('sales:create')">
            <el-icon><Plus /></el-icon>{{ $t('sales.newButton') }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表 -->
    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="tableData" stripe style="width:100%" min-height="200">
        <el-table-column prop="recordCode" :label="$t('sales.column.recordCode')" min-width="170">
          <template #default="{ row }">
            <span class="code-badge">{{ row.recordCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="productCode" :label="$t('sales.column.productCode')" min-width="110">
          <template #default="{ row }">
            <span v-if="row.productCode">{{ row.productCode }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="subProductCode" :label="$t('sales.column.subProductCode')" min-width="100" />
        <el-table-column prop="salesChannel" :label="$t('sales.column.salesChannel')" min-width="110" align="center">
          <template #default="{ row }">
            <span>{{ channelLabel(row.salesChannel) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="currentStock" :label="$t('sales.column.currentStock')" min-width="90" align="center">
          <template #default="{ row }">
            <span v-if="row.currentStock !== null" :class="stockClass(row)">{{ row.currentStock }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="salesQuantity" :label="$t('sales.column.salesQuantity')" min-width="90" align="center" />
        <el-table-column prop="returnedQuantity" :label="$t('sales.column.returnedQuantity')" min-width="90" align="center">
          <template #default="{ row }">
            <span v-if="row.returnedQuantity !== null" :class="row.returnedQuantity > 0 ? 'text-danger' : ''">{{ row.returnedQuantity }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="returnRate" :label="$t('sales.column.returnRate')" min-width="80" align="right">
          <template #default="{ row }">
            <span v-if="row.returnRate !== null">{{ (Number(row.returnRate) * 100).toFixed(1) }}%</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="sellingPriceJpy" :label="$t('sales.column.sellingPriceJpy')" min-width="120" align="right">
          <template #default="{ row }">
            <span v-if="row.sellingPriceJpy !== null" class="money">{{ Number(row.sellingPriceJpy).toLocaleString('ja-JP') }} {{ $t('common.units.jpy') }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="listingDate" :label="$t('sales.column.listingDate')" min-width="110" />
        <el-table-column prop="status" :label="$t('sales.column.status')" min-width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('sales.column.action')" min-width="220" align="center">
          <template #default="{ row }">
            <el-button link class="btn-blue" size="small" @click.stop="onView(row)">{{ $t('sales.action.detail') }}</el-button>
            <template v-if="row.status !== 'DISCONTINUED'">
              <el-button v-if="hasPermission('sales:update')" link type="info" size="small" @click.stop="onUpdateStock(row)">{{ $t('sales.action.updateStock') }}</el-button>
              <el-button v-if="hasPermission('sales:update')" link type="danger" size="small" :loading="actionLoading === row.id + '-discontinue'" @click.stop="onDiscontinue(row)">{{ $t('sales.action.discontinue') }}</el-button>
            </template>
            <template v-else>
              <el-button v-if="hasPermission('sales:update')" link type="success" size="small" :loading="actionLoading === row.id + '-relist'" @click.stop="onRelist(row)">{{ $t('sales.action.relist') }}</el-button>
              <el-button link type="danger" size="small" :loading="actionLoading === row.id + '-delete'" @click.stop="onDelete(row)" v-if="hasPermission('sales:delete')">{{ $t('common.delete') }}</el-button>
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
    <el-drawer v-model="drawerVisible" :title="$t('sales.drawerTitle')" size="600px" direction="rtl">
      <el-descriptions :column="2" border v-if="currentRow">
        <el-descriptions-item :label="$t('sales.column.recordCode')">
          <span class="code-badge">{{ currentRow.recordCode }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('sales.column.status')">
          <el-tag :type="statusTagType(currentRow.status)" size="small">{{ statusLabel(currentRow.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('sales.column.productCode')">{{ currentRow.productCode ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('sales.column.subProductCode')">{{ currentRow.subProductCode ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('sales.column.procurementId')">{{ currentRow.procurementId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('sales.column.salesChannel')">{{ channelLabel(currentRow.salesChannel) }}</el-descriptions-item>
        <el-descriptions-item :label="$t('sales.column.listingDate')">{{ currentRow.listingDate ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('sales.column.sellingPriceJpy')">
          <span v-if="currentRow.sellingPriceJpy !== null" class="money">{{ Number(currentRow.sellingPriceJpy).toLocaleString('ja-JP') }} {{ $t('common.units.jpy') }}</span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('sales.column.initialStock')">{{ currentRow.initialStock ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('sales.column.currentStock')">
          <span :class="stockClass(currentRow)">{{ currentRow.currentStock ?? '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('sales.column.safetyStock')">{{ currentRow.safetyStock ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('sales.column.salesQuantity')">{{ currentRow.salesQuantity ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('sales.column.returnedQuantity')">{{ currentRow.returnedQuantity ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('sales.column.returnRate')">
          <span v-if="currentRow.returnRate !== null">{{ (Number(currentRow.returnRate) * 100).toFixed(1) }}%</span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('sales.column.createBy')">{{ currentRow.createBy || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('sales.column.createTime')">{{ currentRow.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('sales.column.remarks')" :span="2">{{ currentRow.remarks || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <!-- 新规上架弹窗 -->
    <el-dialog v-model="createDialogVisible" :title="$t('sales.newDialogTitle')" width="520px">
      <el-form :model="createForm" label-width="140px">
        <el-form-item :label="$t('sales.form.procurementId')">
          <el-input-number v-model="createForm.procurementId" :min="1" style="width:100%" />
        </el-form-item>
        <el-form-item :label="$t('sales.form.productCode')">
          <el-input v-model="createForm.productCode" :placeholder="$t('sales.form.productCodePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('sales.form.subProductCode')">
          <el-input v-model="createForm.subProductCode" placeholder="re / wh / bk" />
        </el-form-item>
        <el-form-item :label="$t('sales.form.salesChannel')" required>
          <el-select v-model="createForm.salesChannel" style="width:100%">
            <el-option value="AMAZON" :label="$t('sales.channel.amazon')" />
            <el-option value="MERCALI" :label="$t('sales.channel.mercali')" />
            <el-option value="SELF_SITE" :label="$t('sales.channel.selfSite')" />
            <el-option value="OTHER" :label="$t('sales.channel.other')" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('sales.form.listingDate')">
          <el-date-picker v-model="createForm.listingDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item :label="$t('sales.form.initialStock')">
          <el-input-number v-model="createForm.initialStock" :min="0" style="width:100%" />
        </el-form-item>
        <el-form-item :label="$t('sales.form.safetyStock')">
          <el-input-number v-model="createForm.safetyStock" :min="0" style="width:100%" />
        </el-form-item>
        <el-form-item :label="$t('sales.form.sellingPriceJpy')">
          <el-input-number v-model="createForm.sellingPriceJpy" :min="0" :precision="0" style="width:100%" />
        </el-form-item>
        <el-form-item :label="$t('sales.form.remarks')">
          <el-input v-model="createForm.remarks" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saveLoading" @click="onCreateConfirm">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- 库存更新弹窗 -->
    <el-dialog v-model="stockDialogVisible" :title="$t('sales.stockDialogTitle')" width="420px">
      <el-form :model="stockForm" label-width="130px">
        <el-form-item :label="$t('sales.stockDialog.sold')">
          <el-input-number v-model="stockForm.sold" :min="0" style="width:100%" />
        </el-form-item>
        <el-form-item :label="$t('sales.stockDialog.returned')">
          <el-input-number v-model="stockForm.returned" :min="0" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stockDialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="actionLoading.startsWith('stock-')" @click="onStockConfirm">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- 库存预警弹窗 -->
    <el-dialog v-model="alertDialogVisible" :title="$t('sales.alertDialogTitle')" width="700px">
      <el-table :data="alertData" stripe>
        <el-table-column prop="productCode" :label="$t('sales.column.productCode')" min-width="130" />
        <el-table-column prop="subProductCode" :label="$t('sales.column.subProductCode')" min-width="100" />
        <el-table-column prop="salesChannel" :label="$t('sales.column.salesChannel')" min-width="110" align="center">
          <template #default="{ row }">{{ channelLabel(row.salesChannel) }}</template>
        </el-table-column>
        <el-table-column prop="currentStock" :label="$t('sales.column.currentStock')" min-width="90" align="center">
          <template #default="{ row }">
            <span v-if="row.currentStock !== null" :class="stockClass(row)">{{ row.currentStock }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="safetyStock" :label="$t('sales.column.safetyStock')" min-width="90" align="center" />
        <el-table-column prop="status" :label="$t('sales.column.status')" min-width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('sales.column.action')" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="onView(row); alertDialogVisible = false">{{ $t('sales.action.detail') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, CircleCheck, Warning, CloseBold, Remove, Goods } from '@element-plus/icons-vue'
import { salesOperationsApi, type SalesRecordVO, type SalesStatus, type SalesChannel } from '@/api/salesOperations'
import { useI18n } from 'vue-i18n'
import { usePermission } from '@/composables/usePermission'

const { t } = useI18n()
const { hasPermission } = usePermission()

const loading = ref(false)
const saveLoading = ref(false)
const drawerVisible = ref(false)
const createDialogVisible = ref(false)
const stockDialogVisible = ref(false)
const alertDialogVisible = ref(false)
const actionLoading = ref('')

const currentRow = ref<SalesRecordVO | null>(null)
const alertData = ref<SalesRecordVO[]>([])
const stockRowId = ref<number | null>(null)

const filterForm = reactive({
  productCode: '',
  salesChannel: '' as SalesChannel | '',
  status: '' as SalesStatus | '',
})
const pagination = reactive({ page: 1, pageSize: 20, total: 0 })
const tableData = ref<SalesRecordVO[]>([])

const createForm = reactive({
  procurementId: undefined as number | undefined,
  productCode: '',
  subProductCode: '',
  salesChannel: '' as SalesChannel | '',
  listingDate: '',
  initialStock: 0,
  safetyStock: 0,
  sellingPriceJpy: 0,
  remarks: '',
})

const stockForm = reactive({ sold: 0, returned: 0 })

const statusCount = computed(() => {
  const counts: Record<string, number> = { LISTED: 0, LOW_STOCK: 0, OUT_OF_STOCK: 0, DISCONTINUED: 0 }
  tableData.value.forEach(r => { if (r.status in counts) counts[r.status]++ })
  return counts
})

function channelLabel(channel?: string | null): string {
  const map: Record<string, string> = {
    AMAZON: t('sales.channel.amazon'),
    MERCALI: t('sales.channel.mercali'),
    SELF_SITE: t('sales.channel.selfSite'),
    OTHER: t('sales.channel.other'),
  }
  return map[channel ?? ''] ?? channel ?? '-'
}

function statusLabel(status?: string): string {
  const map: Record<string, string> = {
    LISTED: t('sales.status.listed'),
    LOW_STOCK: t('sales.status.lowStock'),
    OUT_OF_STOCK: t('sales.status.outOfStock'),
    DISCONTINUED: t('sales.status.discontinued'),
  }
  return map[status ?? ''] ?? status ?? '-'
}

function statusTagType(status?: string): string {
  const map: Record<string, string> = {
    LISTED: 'success',
    LOW_STOCK: 'warning',
    OUT_OF_STOCK: 'danger',
    DISCONTINUED: 'info',
  }
  return map[status ?? ''] ?? 'info'
}

function stockClass(row: SalesRecordVO): string {
  if (row.status === 'OUT_OF_STOCK') return 'stock-oos'
  if (row.status === 'LOW_STOCK') return 'stock-low'
  return ''
}

async function loadData() {
  loading.value = true
  try {
    const res = await salesOperationsApi.list({
      page: pagination.page - 1,
      pageSize: pagination.pageSize,
      productCode: filterForm.productCode || undefined,
      salesChannel: filterForm.salesChannel || undefined,
      status: filterForm.status || undefined,
    })
    const data = res.data
    tableData.value = data?.content ?? []
    pagination.total = data?.totalElements ?? 0
  } catch (e: unknown) {
    console.error('[SalesRecordPage] loadData failed', e)
    ElMessage.error(t('sales.message.loadFailed'))
  } finally {
    loading.value = false
  }
}

function onSearch() { loadData() }
function onSearchFromButton() { pagination.page = 1; loadData() }

function onReset() {
  filterForm.productCode = ''
  filterForm.salesChannel = ''
  filterForm.status = ''
  pagination.page = 1
  loadData()
}

function onView(row: SalesRecordVO) {
  currentRow.value = row
  drawerVisible.value = true
}

function onCreate() {
  Object.assign(createForm, {
    procurementId: undefined,
    productCode: '',
    subProductCode: '',
    salesChannel: 'AMAZON',
    listingDate: '',
    initialStock: 0,
    safetyStock: 0,
    sellingPriceJpy: 0,
    remarks: '',
  })
  createDialogVisible.value = true
}

async function onCreateConfirm() {
  if (!createForm.salesChannel) {
    ElMessage.error(t('sales.validation.salesChannelRequired'))
    return
  }
  saveLoading.value = true
  try {
    await salesOperationsApi.create({
      procurementId: createForm.procurementId,
      productCode: createForm.productCode || undefined,
      subProductCode: createForm.subProductCode || undefined,
      salesChannel: createForm.salesChannel || undefined,
      listingDate: createForm.listingDate || undefined,
      initialStock: createForm.initialStock || undefined,
      safetyStock: createForm.safetyStock || undefined,
      sellingPriceJpy: createForm.sellingPriceJpy || undefined,
      remarks: createForm.remarks || undefined,
    })
    ElMessage.success(t('sales.message.createSuccess'))
    createDialogVisible.value = false
    loadData()
  } catch (e) {
    console.error('[SalesRecordPage] create failed', e)
    ElMessage.error(t('sales.message.createFailed'))
  } finally {
    saveLoading.value = false
  }
}

function onUpdateStock(row: SalesRecordVO) {
  stockRowId.value = row.id
  stockForm.sold = 0
  stockForm.returned = 0
  stockDialogVisible.value = true
}

async function onStockConfirm() {
  if (!stockRowId.value) return
  actionLoading.value = `stock-${stockRowId.value}`
  try {
    await salesOperationsApi.updateStock(stockRowId.value, {
      sold: stockForm.sold,
      returned: stockForm.returned,
    })
    ElMessage.success(t('sales.message.updateSuccess'))
    stockDialogVisible.value = false
    loadData()
  } catch (e) {
    console.error('[SalesRecordPage] updateStock failed', e)
    ElMessage.error(t('sales.message.actionFailed'))
  } finally {
    actionLoading.value = ''
    stockRowId.value = null
  }
}

async function onDiscontinue(row: SalesRecordVO) {
  actionLoading.value = `${row.id}-discontinue`
  try {
    await salesOperationsApi.discontinue(row.id)
    ElMessage.success(t('sales.message.discontinueSuccess'))
    loadData()
  } catch (e) {
    console.error('[SalesRecordPage] discontinue failed', e)
    ElMessage.error(t('sales.message.actionFailed'))
  } finally {
    actionLoading.value = ''
  }
}

async function onRelist(row: SalesRecordVO) {
  actionLoading.value = `${row.id}-relist`
  try {
    await salesOperationsApi.relist(row.id)
    ElMessage.success(t('sales.message.relistSuccess'))
    loadData()
  } catch (e) {
    console.error('[SalesRecordPage] relist failed', e)
    ElMessage.error(t('sales.message.actionFailed'))
  } finally {
    actionLoading.value = ''
  }
}

async function onDelete(row: SalesRecordVO) {
  try {
    await ElMessageBox.confirm(t('sales.message.deleteConfirm'), t('common.warning'), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      type: 'warning',
    })
  } catch { return }
  actionLoading.value = `${row.id}-delete`
  try {
    await salesOperationsApi.delete(row.id)
    ElMessage.success(t('sales.message.deleteSuccess'))
    loadData()
  } catch (e) {
    console.error('[SalesRecordPage] delete failed', e)
    ElMessage.error(t('sales.message.actionFailed'))
  } finally {
    actionLoading.value = ''
  }
}

onMounted(() => { loadData() })

// 修正 el-table 空状态时 empty-block 宽度超出列宽
watch(tableData, () => {
  nextTick(() => {
    const headerTable = document.querySelector('.el-table__header') as HTMLElement
    const scrollView = document.querySelector('.el-scrollbar__view') as HTMLElement
    const emptyBlock = document.querySelector('.el-table__empty-block') as HTMLElement
    if (headerTable) {
      const headerW = headerTable.offsetWidth
      if (scrollView) scrollView.style.width = headerW + 'px'
      if (emptyBlock) emptyBlock.style.width = headerW + 'px'
    }
  })
})
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.filter-card :deep(.el-card__body) { padding-bottom: 0; }
.stats-row { margin-bottom: 0; }
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
.stock-low { color: #F59E0B; font-weight: 700; }
.stock-oos { color: #DC2626; font-weight: 700; }
.text-danger { color: #DC2626; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
