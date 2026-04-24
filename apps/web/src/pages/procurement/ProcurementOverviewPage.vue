<template>
  <div class="page">
    <div class="page-header">
      <div class="header-left">
        <el-button :icon="ArrowLeft" text @click="router.push('/base/overview')">
          {{ $t('common.action.back') }}
        </el-button>
        <h2 class="page-title">{{ $t('orderOverview.title') }}</h2>
      </div>
    </div>

    <div v-if="loading" class="loading-wrap">
      <el-icon class="is-loading" :size="24"><Loading /></el-icon>
    </div>
    <div v-else-if="error" class="error-wrap">
      <el-result icon="error" :title="$t('common.error.loadFailed')" :sub-title="error">
        <template #extra>
          <el-button type="primary" @click="fetch">{{ $t('common.action.retry') }}</el-button>
        </template>
      </el-result>
    </div>

    <!-- 进度条 -->
    <StatusProgressBar v-if="overview" :step-statuses="overview.stepStatuses" />

    <!-- 步骤卡片组 -->
    <div v-if="overview" class="step-cards">
      <!-- 步骤1：补货需求 -->
      <StepCard :step-number="1" :title="$t('orderOverview.step1.title')" :status="overview.stepStatuses[0]">
        <template v-if="overview.demand">
          <div class="step-grid">
            <div class="step-item"><span class="label">{{ $t('orderOverview.step1.demandCode') }}</span><span class="value">{{ overview.demand.demandCode }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step1.demandType') }}</span><span class="value">{{ overview.demand.demandType }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step1.productCode') }}</span><span class="value">{{ overview.demand.productCode }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step1.subProductItems') }}</span><span class="value highlight">{{ subProductSummary(overview.demand) }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step1.japanLead') }}</span><span class="value">{{ overview.demand.japanLead ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step1.status') }}</span><span class="value">{{ overview.demand.status }}</span></div>
          </div>
        </template>
        <template v-else>
          <div class="step-empty">{{ $t('orderOverview.step.notStarted') }}</div>
        </template>
      </StepCard>

      <!-- 步骤2：发注单信息 -->
      <StepCard :step-number="2" :title="$t('orderOverview.step2.title')" :status="overview.stepStatuses[1]">
        <div v-if="overview.procurement" class="step-grid">
          <div class="step-item"><span class="label">{{ $t('orderOverview.step2.procurementCode') }}</span><span class="value">{{ overview.procurement.procurementCode }}</span></div>
          <div class="step-item"><span class="label">{{ $t('orderOverview.step2.factoryName') }}</span><span class="value">{{ overview.procurement.factoryName ?? $t('common.format.dash') }}</span></div>
          <div class="step-item"><span class="label">{{ $t('orderOverview.step2.productCode') }}</span><span class="value">{{ overview.procurement.productCode }}</span></div>
          <div class="step-item"><span class="label">{{ $t('orderOverview.step2.quantity') }}</span><span class="value">{{ overview.procurement.quantity ?? $t('common.format.dash') }}</span></div>
          <div class="step-item"><span class="label">{{ $t('orderOverview.step2.priceRmb') }}</span><span class="value">{{ overview.procurement.priceRmb != null ? $t('common.currency.cny') + Number(overview.procurement.priceRmb).toFixed(2) : $t('common.format.dash') }}</span></div>
          <div class="step-item"><span class="label">{{ $t('orderOverview.step2.taxPoint') }}</span><span class="value">{{ overview.procurement.taxPoint ?? $t('common.format.dash') }}</span></div>
          <div class="step-item"><span class="label">{{ $t('orderOverview.step2.billingType') }}</span><span class="value">{{ billingTypeLabel(overview.procurement.billingType) }}</span></div>
          <div class="step-item"><span class="label">{{ $t('orderOverview.step2.estimatedPriceJpy') }}</span><span class="value">{{ overview.procurement.estimatedPriceJpy != null ? `${Number(overview.procurement.estimatedPriceJpy).toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 0 })} ${$t('common.units.jpy')}` : $t('common.format.dash') }}</span></div>
          <div class="step-item"><span class="label">{{ $t('orderOverview.step2.orderDate') }}</span><span class="value">{{ overview.procurement.orderDate ?? $t('common.format.dash') }}</span></div>
          <div class="step-item"><span class="label">{{ $t('orderOverview.step2.plannedShipDate') }}</span><span class="value">{{ overview.procurement.plannedShipDate ?? $t('common.format.dash') }}</span></div>
          <div class="step-item"><span class="label">{{ $t('orderOverview.step2.actualShipDate') }}</span><span class="value">{{ overview.procurement.actualShipDate ?? $t('common.format.dash') }}</span></div>
          <div class="step-item"><span class="label">{{ $t('orderOverview.step2.status') }}</span><span class="value">{{ procurementStatusLabel(overview.procurement.status) }}</span></div>
        </div>
      </StepCard>

      <!-- 步骤3：验货记录 -->
      <StepCard :step-number="3" :title="$t('orderOverview.step3.title')" :status="overview.stepStatuses[2]">
        <template v-if="overview.qcRecord">
          <div class="step-grid">
            <div class="step-item"><span class="label">{{ $t('orderOverview.step3.qcCode') }}</span><span class="value">{{ overview.qcRecord.qcCode }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step3.result') }}</span><span class="value">
              <el-tag :type="qcResultType(overview.qcRecord.result)" size="small">{{ overview.qcRecord.result }}</el-tag>
            </span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step3.inspectionCount') }}</span><span class="value">{{ overview.qcRecord.inspectionCount ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step3.passedCount') }}</span><span class="value">{{ overview.qcRecord.passedCount ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step3.defectiveCount') }}</span><span class="value">{{ overview.qcRecord.defectiveCount ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step3.qcDate') }}</span><span class="value">{{ overview.qcRecord.qcDate ?? $t('common.format.dash') }}</span></div>
          </div>
        </template>
        <template v-else>
          <div class="step-empty">{{ $t('orderOverview.step.notStarted') }}</div>
        </template>
      </StepCard>

      <!-- 步骤4：调配计划 -->
      <StepCard :step-number="4" :title="$t('orderOverview.step4.title')" :status="overview.stepStatuses[3]">
        <template v-if="overview.logisticsPlan">
          <div class="step-grid">
            <div class="step-item"><span class="label">{{ $t('orderOverview.step4.planCode') }}</span><span class="value">{{ overview.logisticsPlan.planCode }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step4.planType') }}</span><span class="value">{{ logisticsPlanTypeLabel(overview.logisticsPlan.planType) }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step4.cargoVolume') }}</span><span class="value">{{ overview.logisticsPlan.cargoVolumeCbm != null ? `${Number(overview.logisticsPlan.cargoVolumeCbm).toFixed(4)} CBM` : $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step4.cargoWeight') }}</span><span class="value">{{ overview.logisticsPlan.cargoWeightKg != null ? `${Number(overview.logisticsPlan.cargoWeightKg).toFixed(2)} kg` : $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step4.estimatedShipDate') }}</span><span class="value">{{ overview.logisticsPlan.estimatedShipDate ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step4.actualShipDate') }}</span><span class="value">{{ overview.logisticsPlan.actualShipDate ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step4.status') }}</span><span class="value">{{ logisticsStatusLabel(overview.logisticsPlan.status) }}</span></div>
          </div>
        </template>
        <template v-else>
          <div class="step-empty">{{ $t('orderOverview.step.notStarted') }}</div>
        </template>
      </StepCard>

      <!-- 步骤5：国内报关 -->
      <StepCard :step-number="5" :title="$t('orderOverview.step5.title')" :status="overview.stepStatuses[4]">
        <template v-if="overview.domesticCustoms">
          <div class="step-grid">
            <div class="step-item"><span class="label">{{ $t('orderOverview.step5.customsCode') }}</span><span class="value">{{ overview.domesticCustoms.customsCode }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step5.productCode') }}</span><span class="value">{{ overview.domesticCustoms.productCode }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step5.status') }}</span><span class="value">{{ domesticCustomsStatusLabel(overview.domesticCustoms.status) }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step5.estimatedValue') }}</span><span class="value">{{ overview.domesticCustoms.estimatedValueCny != null ? $t('common.currency.cny') + Number(overview.domesticCustoms.estimatedValueCny).toFixed(2) : $t('common.format.dash') }}</span></div>
          </div>
        </template>
        <template v-else>
          <div class="step-empty">{{ $t('orderOverview.step.notStarted') }}</div>
        </template>
      </StepCard>

      <!-- 步骤6：日本清关 -->
      <StepCard :step-number="6" :title="$t('orderOverview.step6.title')" :status="overview.stepStatuses[5]">
        <template v-if="overview.japanCustoms">
          <div class="step-grid">
            <div class="step-item"><span class="label">{{ $t('orderOverview.step6.customsEntryNo') }}</span><span class="value">{{ overview.japanCustoms.customsEntryNo ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step6.arrivalDate') }}</span><span class="value">{{ overview.japanCustoms.arrivalDate ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step6.clearanceDate') }}</span><span class="value">{{ overview.japanCustoms.clearanceDate ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step6.customsBroker') }}</span><span class="value">{{ overview.japanCustoms.customsBroker ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step6.importDutyPaid') }}</span><span class="value">{{ overview.japanCustoms.importDutyPaid != null ? `${Number(overview.japanCustoms.importDutyPaid).toLocaleString(undefined, { minimumFractionDigits: 2 })} ${$t('common.units.jpy')}` : $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step6.consumptionTaxPaid') }}</span><span class="value">{{ overview.japanCustoms.consumptionTaxPaid != null ? `${Number(overview.japanCustoms.consumptionTaxPaid).toLocaleString(undefined, { minimumFractionDigits: 2 })} ${$t('common.units.jpy')}` : $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step6.arrivalPort') }}</span><span class="value">{{ overview.japanCustoms.arrivalPort ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step6.status') }}</span><span class="value">{{ japanCustomsStatusLabel(overview.japanCustoms.status) }}</span></div>
          </div>
        </template>
        <template v-else>
          <div class="step-empty">{{ $t('orderOverview.step.notStarted') }}</div>
        </template>
      </StepCard>

      <!-- 步骤7：退税 -->
      <StepCard :step-number="7" :title="$t('orderOverview.step7.title')" :status="overview.stepStatuses[6]">
        <template v-if="overview.taxRefund">
          <div class="step-grid">
            <div class="step-item"><span class="label">{{ $t('orderOverview.step7.refundCode') }}</span><span class="value">{{ overview.taxRefund.refundCode ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step7.estimatedRefundRmb') }}</span><span class="value">{{ overview.taxRefund.estimatedRefundRmb != null ? $t('common.currency.cny') + Number(overview.taxRefund.estimatedRefundRmb).toFixed(2) : $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step7.actualRefundRmb') }}</span><span class="value">{{ overview.taxRefund.actualRefundRmb != null ? $t('common.currency.cny') + Number(overview.taxRefund.actualRefundRmb).toFixed(2) : $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step7.refundDate') }}</span><span class="value">{{ overview.taxRefund.refundDate ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step7.status') }}</span><span class="value">{{ taxRefundStatusLabel(overview.taxRefund.status) }}</span></div>
          </div>
        </template>
        <template v-else>
          <div class="step-empty">{{ $t('orderOverview.step.notStarted') }}</div>
        </template>
      </StepCard>

      <!-- 步骤8：运营销售 -->
      <StepCard :step-number="8" :title="$t('orderOverview.step8.title')" :status="overview.stepStatuses[7]">
        <template v-if="overview.salesRecord">
          <div class="step-grid">
            <div class="step-item"><span class="label">{{ $t('orderOverview.step8.recordCode') }}</span><span class="value">{{ overview.salesRecord.recordCode ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step8.salesChannel') }}</span><span class="value">{{ salesChannelLabel(overview.salesRecord.salesChannel) }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step8.listingDate') }}</span><span class="value">{{ overview.salesRecord.listingDate ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step8.currentStock') }}</span><span class="value">{{ overview.salesRecord.currentStock ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step8.salesQuantity') }}</span><span class="value">{{ overview.salesRecord.salesQuantity ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step8.returnRate') }}</span><span class="value">{{ overview.salesRecord.returnRate != null ? `${(Number(overview.salesRecord.returnRate) * 100).toFixed(2)}%` : $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step8.sellingPriceJpy') }}</span><span class="value">{{ overview.salesRecord.sellingPriceJpy != null ? `${Number(overview.salesRecord.sellingPriceJpy).toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 0 })} ${$t('common.units.jpy')}` : $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step8.status') }}</span><span class="value">{{ salesStatusLabel(overview.salesRecord.status) }}</span></div>
          </div>
        </template>
        <template v-else>
          <div class="step-empty">{{ $t('orderOverview.step.notStarted') }}</div>
        </template>
      </StepCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ArrowLeft, Loading } from '@element-plus/icons-vue'
import { orderOverviewApi, type OrderOverviewVO } from '@/api/orderOverview'
import StatusProgressBar from './components/StatusProgressBar.vue'
import StepCard from './components/StepCard.vue'

const { t } = useI18n()

const route = useRoute()
const router = useRouter()

const procurementId = computed(() => Number(route.params.procurementId))

const overview = ref<OrderOverviewVO | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)

async function fetch() {
  loading.value = true
  error.value = null
  try {
    const res = await orderOverviewApi.getOverview(procurementId.value)
    overview.value = res.data.data
  } catch (e: unknown) {
    error.value = (e as Error).message ?? '加载订单总览失败'
  } finally {
    loading.value = false
  }
}

fetch()

function qcResultType(result?: string) {
  if (result === 'PASS') return 'success'
  if (result === 'FAIL') return 'danger'
  return 'info'
}

function procurementStatusLabel(s?: string) {
  return s ? t('orderOverview.enum.procurement.' + s, s) : t('common.format.dash')
}

function logisticsStatusLabel(s?: string) {
  return s ? t('orderOverview.enum.logistics.' + s, s) : t('common.format.dash')
}

function logisticsPlanTypeLabel(s?: string) {
  return s ? t('orderOverview.enum.planType.' + s, s) : t('common.format.dash')
}

function domesticCustomsStatusLabel(s?: string) {
  return s ? t('orderOverview.enum.domesticCustoms.' + s, s) : t('common.format.dash')
}

function japanCustomsStatusLabel(s?: string) {
  return s ? t('orderOverview.enum.japanCustoms.' + s, s) : t('common.format.dash')
}

function taxRefundStatusLabel(s?: string) {
  return s ? t('orderOverview.enum.taxRefund.' + s, s) : t('common.format.dash')
}

function billingTypeLabel(s?: string) {
  return s ? t('orderOverview.enum.billingType.' + s, s) : t('common.format.dash')
}

function salesStatusLabel(s?: string) {
  return s ? t('orderOverview.enum.sales.' + s, s) : t('common.format.dash')
}

function salesChannelLabel(s?: string) {
  return s ? t('orderOverview.enum.salesChannel.' + s, s) : t('common.format.dash')
}

function subProductSummary(demand: { subProductCode?: string; quantity?: number; destination?: string }): string {
  if (!demand.subProductCode) return t('common.format.dash')
  const parts = [demand.subProductCode]
  if (demand.quantity != null) parts.push(demand.quantity + t('demand.dialog.unitTai'))
  if (demand.destination) parts.push(demand.destination)
  return parts.join(' ')
}
</script>

<style scoped>
.page { padding: 16px; }
.page-header {
  display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px;
}
.header-left { display: flex; align-items: center; gap: 8px; }
.page-title { margin: 0; font-size: 18px; font-weight: 600; }
.loading-wrap, .error-wrap { display: flex; justify-content: center; align-items: center; min-height: 200px; }
.step-cards { display: flex; flex-direction: column; gap: 12px; margin-top: 16px; }
.step-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 8px; }
.step-item { display: flex; gap: 8px; }
.step-item .label { color: #909399; flex-shrink: 0; min-width: 80px; }
.step-item .value { color: #303133; }
.step-empty { color: #c0c4cc; font-size: 13px; }
</style>
