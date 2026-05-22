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
      <template #header>
        <div class="table-header">
          <el-radio-group v-model="excelViewMode" size="small">
            <el-radio-button value="table">{{ $t('common.viewMode.table') }}</el-radio-button>
            <el-radio-button value="copy">{{ $t('common.viewMode.excel') }}</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <el-table v-if="excelViewMode === 'table'" v-loading="loading" :data="chainData" stripe style="width: 100%">
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
        <!-- 商品图片 -->
        <el-table-column :label="$t('orderOverview.column.imageUrl')" width="70" align="center">
          <template #default="{ row }">
            <el-image
              v-if="productImageMap[row.demandProductCode]"
              :src="productImageMap[row.demandProductCode]"
              fit="cover"
              style="width:40px;height:40px;border-radius:6px;border:1px solid #e0e6ed;cursor:pointer"
              :preview-src-list="[productImageMap[row.demandProductCode]]"
              preview-teleported
            />
            <span v-else class="no-image">—</span>
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
        <!-- 单价 -->
        <el-table-column :label="$t('orderOverview.column.priceRmb')" min-width="90" align="right">
          <template #default="{ row }">
            <span v-if="row.procurementPriceRmb" class="money">¥{{ Number(row.procurementPriceRmb).toFixed(2) }}</span>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <!-- 票点 -->
        <el-table-column :label="$t('orderOverview.column.taxPoint')" min-width="70" align="center">
          <template #default="{ row }">
            <span v-if="row.procurementTaxPoint">{{ Number(row.procurementTaxPoint).toFixed(1) }}%</span>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <!-- 报关类型 -->
        <el-table-column :label="$t('orderOverview.column.customsType')" min-width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.procurementBillingType === 'CHAO_HUI_TUI_SHUI' ? 'success' : 'warning'" size="small">
              {{ customsTypeLabel(row.procurementBillingType) }}
            </el-tag>
          </template>
        </el-table-column>
        <!-- 采购人 -->
        <el-table-column :label="$t('orderOverview.column.procurementCreateBy')" min-width="90" align="center">
          <template #default="{ row }">
            <span v-if="row.procurementCreateBy" class="text-secondary">{{ row.procurementCreateBy }}</span>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <!-- 状态 -->
        <el-table-column :label="$t('orderOverview.column.status')" min-width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="chainStatusType(row)" size="small">
              {{ chainStatusLabel(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <!-- 操作 -->
        <el-table-column :label="$t('orderOverview.column.action')" min-width="80" align="center">
          <template #default="{ row }">
            <el-button link class="btn-blue" size="small" @click.stop="onView(row)">{{ $t('orderOverview.action.view') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <ExcelTable v-else :columns="copyColumns" :data="chainData" />

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
    <el-drawer v-model="drawerVisible" :title="$t('orderOverview.drawerTitle')" direction="rtl" size="720px" bodyStyle="overflow-y: auto">
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
          <el-descriptions-item :label="$t('orderOverview.step2.imageUrl')">
            <el-image
              v-if="detailImageUrl"
              :src="detailImageUrl"
              fit="contain"
              style="max-width:200px;max-height:200px;border-radius:8px;border:1px solid #e0e6ed;cursor:pointer"
              :preview-src-list="[detailImageUrl]"
              preview-teleported
            />
            <span v-else>—</span>
          </el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step2.quantity')">{{ detailData.procurement.quantity ?? '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step2.priceRmb')">{{ detailData.procurement.priceRmb ? $t('common.currency.cny') + Number(detailData.procurement.priceRmb).toFixed(2) : '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step2.taxPoint')">{{ detailData.procurement.taxPoint ?? '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step2.billingType')">{{ billingTypeLabel(detailData.procurement.billingType) }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step2.orderDate')">{{ formatDate(detailData.procurement.orderDate) }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step2.plannedShipDate')">{{ formatDate(detailData.procurement.plannedShipDate) }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step2.actualShipDate')">{{ formatDate(detailData.procurement.actualShipDate) }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step2.status')">{{ statusLabelByValue(detailData.procurement.status) }}</el-descriptions-item>
          <el-descriptions-item v-if="detailData.procurement.status === '退货'" :label="$t('orderOverview.step2.returnReason')" :span="2">
            <span class="return-reason-text">{{ detailData.procurement.returnReason || '-' }}</span>
          </el-descriptions-item>
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

        <!-- 步骤5：国内报关 -->
        <div class="drawer-section-title">{{ $t('orderOverview.step5.title') }}</div>
        <el-descriptions v-if="detailData.domesticCustoms" :column="2" border size="small">
          <el-descriptions-item :label="$t('orderOverview.step5.customsCode')">{{ detailData.domesticCustoms.customsCode || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step5.containerNo')">{{ detailData.domesticCustoms.containerNo || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step5.productCode')">{{ detailData.domesticCustoms.productCode || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step5.estimatedValueCny')">{{ detailData.domesticCustoms.estimatedValueCny ? $t('common.currency.cny') + Number(detailData.domesticCustoms.estimatedValueCny).toFixed(2) : '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step5.status')">{{ domesticCustomsStatusLabel(detailData.domesticCustoms.status) }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step5.createTime')">{{ formatDate(detailData.domesticCustoms.createTime) }}</el-descriptions-item>
        </el-descriptions>
        <div v-else class="step-empty">{{ $t('orderOverview.stepStatusUI.notStarted') }}</div>

        <!-- 步骤6：日本清关 -->
        <div class="drawer-section-title">{{ $t('orderOverview.step6.title') }}</div>
        <el-descriptions v-if="detailData.japanCustoms" :column="2" border size="small">
          <el-descriptions-item :label="$t('orderOverview.step6.containerNo')">{{ detailData.japanCustoms.containerNo || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step6.importDutyPaid')">{{ detailData.japanCustoms.importDutyPaid ? $t('common.currency.jpy') + Number(detailData.japanCustoms.importDutyPaid).toLocaleString() : '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step6.consumptionTaxPaid')">{{ detailData.japanCustoms.consumptionTaxPaid ? $t('common.currency.jpy') + Number(detailData.japanCustoms.consumptionTaxPaid).toLocaleString() : '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step6.arrivalDate')">{{ formatDate(detailData.japanCustoms.arrivalDate) }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step6.clearanceDate')">{{ formatDate(detailData.japanCustoms.clearanceDate) }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step6.status')">{{ japanCustomsStatusLabel(detailData.japanCustoms.status) }}</el-descriptions-item>
        </el-descriptions>
        <div v-else class="step-empty">{{ $t('orderOverview.stepStatusUI.notStarted') }}</div>

        <!-- 步骤7：退税 -->
        <div class="drawer-section-title">{{ $t('orderOverview.step7.title') }}</div>
        <el-descriptions v-if="detailData.taxRefund" :column="2" border size="small">
          <el-descriptions-item :label="$t('orderOverview.step7.refundCode')">{{ detailData.taxRefund.refundCode || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step7.billingType')">{{ billingTypeLabel(detailData.taxRefund.billingType) }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step7.estimatedRefundRmb')">{{ detailData.taxRefund.estimatedRefundRmb ? $t('common.currency.cny') + Number(detailData.taxRefund.estimatedRefundRmb).toFixed(2) : '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step7.refundDate')">{{ formatDate(detailData.taxRefund.refundDate) }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step7.status')">{{ taxRefundStatusLabel(detailData.taxRefund.status) }}</el-descriptions-item>
        </el-descriptions>
        <div v-else class="step-empty">{{ $t('orderOverview.stepStatusUI.notStarted') }}</div>

        <!-- 步骤8：运营销售 -->
        <div class="drawer-section-title">{{ $t('orderOverview.step8.title') }}</div>
        <el-descriptions v-if="detailData.salesRecord" :column="2" border size="small">
          <el-descriptions-item :label="$t('orderOverview.step8.recordCode')">{{ detailData.salesRecord.recordCode || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step8.salesChannel')">{{ salesChannelLabel(detailData.salesRecord.salesChannel) }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step8.initialStock')">{{ detailData.salesRecord.initialStock ?? '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step8.currentStock')">{{ detailData.salesRecord.currentStock ?? '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step8.salesQuantity')">{{ detailData.salesRecord.salesQuantity ?? '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.step8.status')">{{ salesStatusLabel(detailData.salesRecord.status) }}</el-descriptions-item>
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
import { productApi } from '@/api/product'
import ExcelTable, { type ExcelColDef } from '@/components/ExcelTable.vue'

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
const detailImageUrl = ref<string>('')
const productImageMap = ref<Record<string, string>>({})
const detailData = ref<OrderChainDetailVO | null>(null)
const excelViewMode = ref<'table' | 'copy'>('table')

const copyColumns: ExcelColDef[] = [
  { prop: 'demandProductCode', label: t('orderOverview.column.productCode') },
  { prop: 'demandSubProductCode', label: t('orderOverview.column.subProductCode') },
  { prop: 'snapshot.factoryName', label: t('orderOverview.column.factoryName'), formatter: (row: OrderChainVO) => (row as any).snapshot?.factoryName ?? '' },
  { prop: 'demandQuantity', label: t('orderOverview.column.quantity') },
  { prop: 'demandDestination', label: t('orderOverview.column.destination') },
  { prop: 'procurementPriceRmb', label: t('orderOverview.column.priceRmb'), formatter: (row: OrderChainVO) => row.procurementPriceRmb ? `¥${Number(row.procurementPriceRmb).toFixed(2)}` : '' },
  { prop: 'procurementTaxPoint', label: t('orderOverview.column.taxPoint'), formatter: (row: OrderChainVO) => row.procurementTaxPoint ? `${Number(row.procurementTaxPoint).toFixed(1)}%` : '' },
  { prop: 'procurementBillingType', label: t('orderOverview.column.customsType'), formatter: (row: OrderChainVO) => customsTypeLabel(row.procurementBillingType) },
  { prop: 'procurementCreateBy', label: t('orderOverview.column.procurementCreateBy') },
  { prop: 'status', label: t('orderOverview.column.status'), formatter: (row: OrderChainVO) => chainStatusLabel(row) },
  { prop: 'action', label: t('orderOverview.column.action'), excluded: true },
]

function formatDate(val: string | undefined | null): string {
  if (!val) return '-'
  return new Date(val).toLocaleString(locale.value === 'ja' ? 'ja-JP' : 'zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit',
  })
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

function customsTypeLabel(val: string | undefined): string {
  if (!val) return '-'
  return val === 'CHAO_HUI_TUI_SHUI' ? '超慧退税' : '杂货'
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

function domesticCustomsStatusLabel(val: string | undefined): string {
  if (!val) return '-'
  return t(`orderOverview.enum.domesticCustoms.${val}` as any, { default: val })
}

function japanCustomsStatusLabel(val: string | undefined): string {
  if (!val) return '-'
  return t(`orderOverview.enum.japanCustoms.${val}` as any, { default: val })
}

function taxRefundStatusLabel(val: string | undefined): string {
  if (!val) return '-'
  return t(`orderOverview.enum.taxRefund.${val}` as any, { default: val })
}

function salesChannelLabel(val: string | undefined): string {
  if (!val) return '-'
  return t(`sales.channel.${val.toLowerCase()}` as any, { default: val })
}

function salesStatusLabel(val: string | undefined): string {
  if (!val) return '-'
  const map: Record<string, string> = {
    LISTED: 'listed', LOW_STOCK: 'lowStock', OUT_OF_STOCK: 'outOfStock', DISCONTINUED: 'discontinued',
  }
  return t(`sales.status.${map[val] ?? val.toLowerCase()}` as any, { default: val })
}

function chainStatusType(row: OrderChainVO): string {
  if (row.step4Status === 'COMPLETED') return 'success'
  if (row.step4Status === 'IN_PROGRESS') return 'warning'
  if (row.step3Status === 'COMPLETED') return 'success'
  if (row.step3Status === 'IN_PROGRESS') return 'warning'
  if (row.step2Status === 'COMPLETED') return 'success'
  if (row.step2Status === 'IN_PROGRESS') return 'warning'
  if (row.step1Status === 'COMPLETED') return 'success'
  return 'info'
}

function chainStatusLabel(row: OrderChainVO): string {
  const steps = [
    { status: row.step4Status, label: t('orderOverview.step.4') },
    { status: row.step3Status, label: t('orderOverview.step.3') },
    { status: row.step2Status, label: t('orderOverview.step.2') },
    { status: row.step1Status, label: t('orderOverview.step.1') },
  ]
  for (const s of steps) {
    if (s.status === 'COMPLETED') return s.label + ' ' + t('orderOverview.stepStatusUI.status.completed')
    if (s.status === 'IN_PROGRESS') return s.label + ' ' + t('orderOverview.stepStatusUI.status.inProgress')
  }
  return t('orderOverview.stepStatusUI.notStarted')
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
    chainData.value = res.data?.content ?? []
    total.value = res.data?.totalElements ?? 0
    // 批量获取商品图片
    const codes = [...new Set(chainData.value.map(r => r.demandProductCode).filter(Boolean))]
    if (codes.length) {
      try {
        const r = await productApi.batchGetCategories(codes)
        const imgMap: Record<string, string> = {}
        for (const item of (r.data ?? [])) {
          if (item.imageUrl) imgMap[item.masterCode] = item.imageUrl
        }
        productImageMap.value = { ...productImageMap.value, ...imgMap }
      } catch { /* ignore */ }
    }
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

async function onView(row: OrderChainVO) {
  currentRow.value = row
  drawerVisible.value = true
  detailData.value = null
  detailLoading.value = true
  detailImageUrl.value = ''
  try {
    const res = await orderChainApi.getChainDetail(row.demandId)
    detailData.value = res.data
    // 获取商品图片
    const productCode = res.data?.procurement?.productCode || res.data?.demand?.productCode
    if (productCode) {
      try {
        const p = await productApi.getByCode(productCode)
        if (p.data?.imageUrl) detailImageUrl.value = p.data.imageUrl
      } catch { /* ignore */ }
    }
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
.table-card :deep(.el-table__row) { cursor: pointer; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 12px; }
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

/* 商品图片缩略图 */
.return-reason-text { color: #F56C6C; font-size: 13px; }
.btn-blue { color: #409EFF !important; }
.product-thumb {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid #e0e6ed;
  cursor: pointer;
}
.no-image {
  color: #c0c4cc;
  font-size: 18px;
}
.drawer-product-thumb {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #e0e6ed;
  cursor: pointer;
}
:deep(.el-drawer__body) { overflow-y: auto !important; overflow-x: hidden; }
</style>
