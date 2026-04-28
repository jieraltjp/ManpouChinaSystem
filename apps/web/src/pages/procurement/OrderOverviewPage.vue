<template>
  <div class="page">
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filter">
        <el-form-item :label="$t('orderOverview.filter.status')">
          <el-select v-model="filter.status" clearable style="width:140px">
            <el-option :label="$t('orderOverview.status.all')" value="" />
            <el-option :label="$t('orderOverview.status.pending')" value="PENDING" />
            <el-option :label="$t('orderOverview.status.converted')" value="CONVERTED" />
            <el-option :label="$t('orderOverview.status.cancelled')" value="CANCELLED" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('orderOverview.filter.keyword')">
          <el-input v-model="filter.keyword" :placeholder="$t('orderOverview.filter.keywordPlaceholder')" clearable style="width:200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadChainList">{{ $t('orderOverview.filter.search') }}</el-button>
          <el-button @click="resetFilter">{{ $t('orderOverview.filter.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <div class="table-scroll-wrap">
        <el-table v-loading="loading" :data="chainData" stripe @row-click="onRowClick" table-layout="fixed" min-height="200">
        <!-- 主货号 -->
        <el-table-column prop="demandProductCode" :label="$t('orderOverview.column.productCode')" min-width="140">
          <template #default="{ row }">
            <span class="product-code">{{ row.demandProductCode || '—' }}</span>
          </template>
        </el-table-column>
        <!-- 子货号 -->
        <el-table-column prop="demandSubProductCode" :label="$t('orderOverview.column.subProductCode')" min-width="130">
          <template #default="{ row }">
            <span v-if="row.demandSubProductCode" class="product-code">{{ row.demandSubProductCode }}</span>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <!-- 工厂（快照） -->
        <el-table-column :label="$t('orderOverview.column.factoryName')" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.snapshot?.factoryName">{{ row.snapshot.factoryName }}</span>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <!-- 数量 -->
        <el-table-column prop="demandQuantity" :label="$t('orderOverview.column.quantity')" min-width="80" align="right">
          <template #default="{ row }">
            <span v-if="row.demandQuantity" class="qty-value">{{ row.demandQuantity }}</span>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <!-- 目的地 -->
        <el-table-column prop="demandDestination" :label="$t('orderOverview.column.destination')" min-width="110">
          <template #default="{ row }">
            {{ row.demandDestination || '—' }}
          </template>
        </el-table-column>
        <!-- 操作 -->
        <el-table-column :label="$t('orderOverview.column.action')" min-width="80" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="onView(row)">{{ $t('orderOverview.action.view') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      </div>
      <div class="pagination-wrap">
        <el-pagination
          background
          :current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="onPage"
        />
      </div>
    </el-card>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" :title="$t('orderOverview.drawerTitle')" direction="rtl" size="auto">
      <div v-if="detailLoading" class="detail-loading">
        <el-icon class="is-loading" :size="24"><Loading /></el-icon>
      </div>
      <div v-else-if="detailData" class="drawer-content">
        <!-- 步骤1：补货需求 -->
        <div class="drawer-section-title">{{ $t('orderOverview.step1.title') }}</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item :label="$t('orderOverview.column.demandCode')">{{ detailData.demand?.demandCode || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.column.demandType')">{{ demandTypeLabel(detailData.demand?.demandType) }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.column.productCode')">{{ detailData.demand?.productCode || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.column.subProductCode')">{{ detailData.demand?.subProductCode || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.column.quantity')">{{ detailData.demand?.quantity ?? '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.column.destination')">{{ detailData.demand?.destination || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.column.japanLead')">{{ detailData.demand?.japanLead || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.column.demandStatus')">{{ demandStatusLabel(detailData.demand?.status) }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.column.createTime')">{{ formatDate(detailData.demand?.createTime) }}</el-descriptions-item>
        </el-descriptions>

        <!-- 步骤2：发注单 -->
        <div class="drawer-section-title">{{ $t('orderOverview.step2.title') }}</div>
        <el-descriptions v-if="detailData.procurement" :column="2" border size="small">
          <el-descriptions-item :label="$t('orderOverview.step2.procurementCode')">{{ detailData.procurement.procurementCode || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step2.factoryName')">{{ detailData.factory?.factoryName || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step2.productCode')">{{ detailData.procurement.productCode || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step2.subProductCode')">{{ detailData.procurement.subProductCode || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step2.quantity')">{{ detailData.procurement.quantity ?? '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step2.priceRmb')">{{ detailData.procurement.priceRmb ? $t('common.currency.cny') + Number(detailData.procurement.priceRmb).toFixed(2) : '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step2.taxPoint')">{{ detailData.procurement.taxPoint ?? '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step2.billingType')">{{ billingTypeLabel(detailData.procurement.billingType) }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step2.orderDate')">{{ formatDate(detailData.procurement.orderDate) }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step2.plannedShipDate')">{{ formatDate(detailData.procurement.plannedShipDate) }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step2.actualShipDate')">{{ formatDate(detailData.procurement.actualShipDate) }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step2.status')">{{ statusLabelByValue(detailData.procurement.status) }}</el-descriptions-item>
        </el-descriptions>
        <div v-else class="step-empty">{{ $t('orderOverview.stepStatusUI.notStarted') }}</div>

        <!-- 步骤3：验货记录 -->
        <div class="drawer-section-title">{{ $t('orderOverview.step3.title') }}</div>
        <el-descriptions v-if="detailData.qcRecord" :column="2" border size="small">
          <el-descriptions-item :label="$t('orderOverview.step3.qcCode')">{{ detailData.qcRecord.qcCode || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step3.result')">{{ qcResultLabel(detailData.qcRecord.result) }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step3.inspectionCount')">{{ detailData.qcRecord.inspectionCount ?? '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step3.passedCount')">{{ detailData.qcRecord.passedCount ?? '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step3.defectiveCount')">{{ detailData.qcRecord.defectiveCount ?? '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step3.qcDate')">{{ formatDate(detailData.qcRecord.qcDate) }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step3.status')">{{ qcStatusLabel(detailData.qcRecord.status) }}</el-descriptions-item>
        </el-descriptions>
        <div v-else class="step-empty">{{ $t('orderOverview.stepStatusUI.notStarted') }}</div>

        <!-- 步骤4：调配计划 -->
        <div class="drawer-section-title">{{ $t('orderOverview.step4.title') }}</div>
        <el-descriptions v-if="detailData.logisticsPlan" :column="2" border size="small">
          <el-descriptions-item :label="$t('orderOverview.step4.planCode')">{{ detailData.logisticsPlan.planCode || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step4.planType')">{{ planTypeLabel(detailData.logisticsPlan.planType) }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step4.cargoVolume')">{{ detailData.logisticsPlan.cargoVolumeCbm ? detailData.logisticsPlan.cargoVolumeCbm + ' CBM' : '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step4.cargoWeight')">{{ detailData.logisticsPlan.cargoWeightKg ? detailData.logisticsPlan.cargoWeightKg + ' kg' : '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step4.estimatedShipDate')">{{ formatDate(detailData.logisticsPlan.estimatedShipDate) }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step4.actualShipDate')">{{ formatDate(detailData.logisticsPlan.actualShipDate) }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step4.status')">{{ logisticsStatusLabel(detailData.logisticsPlan.status) }}</el-descriptions-item>
        </el-descriptions>
        <div v-else class="step-empty">{{ $t('orderOverview.stepStatusUI.notStarted') }}</div>

        <div class="drawer-footer">
          <el-button type="primary" @click="drawerVisible = false; router.push('/base/overview/demand/' + currentRow!.demandId)">{{ $t('orderOverview.action.viewDetail') }}</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Loading } from '@element-plus/icons-vue'
import { orderChainApi, type OrderChainVO, type OrderChainDetailVO } from '@/api/orderChain'

const router = useRouter()
const { locale, t } = useI18n()

const loading = ref(false)
const chainData = ref<OrderChainVO[]>([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const filter = reactive({ status: '', keyword: '' })
const drawerVisible = ref(false)
const currentRow = ref<OrderChainVO | null>(null)
const detailLoading = ref(false)
const detailData = ref<OrderChainDetailVO | null>(null)

function formatDate(val: string | undefined | null): string {
  if (!val) return '-'
  return new Date(val).toLocaleString(locale.value === 'ja' ? 'ja-JP' : 'zh-CN')
}

function demandStatusLabel(val: string | undefined): string {
  if (!val) return '-'
  return t(`demand.status.${val}` as any, { default: val })
}

function demandTypeLabel(val: string | undefined): string {
  if (!val) return '-'
  return t(`demand.type.${val}` as any, { default: val })
}

function billingTypeLabel(val: string | undefined): string {
  if (!val) return '-'
  return t(`order.billingType.${val}` as any, { default: val })
}

function statusLabelByValue(val: string | undefined): string {
  if (!val) return '-'
  return t(`order.status.${val}` as any, { default: val })
}

function qcResultLabel(val: string | undefined): string {
  if (!val) return '-'
  return t(`orderOverview.step3.qcResult.${val}` as any, { default: val })
}

function qcStatusLabel(val: string | undefined): string {
  return { PENDING: t('inspection.qcStatus.pending'), COMPLETED: t('inspection.qcStatus.completed'), RETURN_REQUESTED: t('inspection.qcStatus.returnRequested') }[val ?? ''] ?? val ?? '-'
}

function planTypeLabel(val: string | undefined): string {
  if (!val) return '-'
  return t(`logistics.planType.${val}` as any, { default: val })
}

function logisticsStatusLabel(val: string | undefined): string {
  if (!val) return '-'
  return t(`logistics.status.${val}` as any, { default: val })
}

async function loadChainList() {
  loading.value = true
  try {
    const res = await orderChainApi.listChain({
      page: page.value - 1,
      pageSize: pageSize.value,
      demandStatus: filter.status || undefined,
      keyword: filter.keyword || undefined,
    })
    chainData.value = res.data.data?.content ?? []
    total.value = res.data.data?.totalElements ?? 0
  } finally {
    loading.value = false
  }
}

function resetFilter() {
  filter.status = ''
  filter.keyword = ''
  page.value = 1
  loadChainList()
}

function onPage(p: number) {
  page.value = p
  loadChainList()
}

function onRowClick(row: OrderChainVO) {
  router.push('/base/overview/demand/' + row.demandId)
}

async function onView(row: OrderChainVO) {
  currentRow.value = row
  drawerVisible.value = true
  detailData.value = null
  detailLoading.value = true
  try {
    const res = await orderChainApi.getChainDetail(row.demandId)
    detailData.value = res.data.data
  } catch {
    detailData.value = null
  } finally {
    detailLoading.value = false
  }
}

onMounted(() => {
  loadChainList()
})
</script>

<style scoped>
.page { padding: 16px; }
.filter-card :deep(.el-card__body) { padding-bottom: 0; }
.filter-card { margin-bottom: 12px; }
.table-card { margin-top: 0; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 12px; }
.table-scroll-wrap { overflow-x: auto; }
.product-code { color: var(--color-primary); font-family: monospace; font-size: 12px; font-weight: 700; background: var(--color-primary-pale); padding: 3px 9px; border-radius: 5px; }
.qty-value { color: #D97706; font-weight: 600; }
.text-muted { color: #999; }
.drawer-content { padding: 0 20px; }
.drawer-footer { padding: 20px 0 0; border-top: 1px solid var(--border-color); margin-top: 20px; display: flex; gap: 8px; }
.drawer-section-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin: 20px 0 10px;
  padding-bottom: 6px;
  border-bottom: 1px solid #EBEEF5;
}
.step-empty { color: #c0c4cc; font-size: 13px; padding: 8px 0; }
.detail-loading { display: flex; justify-content: center; align-items: center; min-height: 200px; }
</style>
