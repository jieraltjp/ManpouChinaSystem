<template>
  <div class="page">
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

    <div v-if="chain" class="step-cards">
      <!-- 进度条 -->
      <StatusProgressBar :step-statuses="stepStatuses" />

      <!-- 步骤1：补货需求 -->
      <StepCard :step-number="1" :title="$t('orderOverview.step1.title')" :status="step1Status">
        <template v-if="chain.demand">
          <div class="step-grid">
            <div class="step-item"><span class="label">{{ $t('orderOverview.step1.demandCode') }}</span><span class="value">{{ chain.demand.demandCode }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step1.demandType') }}</span><span class="value">{{ demandTypeLabel(chain.demand.demandType) }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step1.productCode') }}</span><span class="value">{{ chain.demand.productCode }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step1.subProductItems') }}</span><span class="value highlight">{{ subProductSummary(chain.demand) }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step1.japanLead') }}</span><span class="value">{{ chain.demand.japanLead ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step1.status') }}</span>
              <el-tag :type="demandStatusType(chain.demand.status)" size="small">{{ demandStatusLabel(chain.demand.status) }}</el-tag>
            </div>
          </div>
        </template>
        <template v-else>
          <div class="step-empty">{{ $t('orderOverview.stepStatusUI.notStarted') }}</div>
        </template>
      </StepCard>

      <!-- 步骤2：发注单 -->
      <StepCard :step-number="2" :title="$t('orderOverview.step2.title')" :status="step2Status">
        <template v-if="chain.procurement">
          <div class="step-grid">
            <div class="step-item"><span class="label">{{ $t('procurement.field.procurementCode') }}</span><span class="value">{{ chain.procurement.procurementCode }}</span></div>
            <div class="step-item" v-if="chain.factory"><span class="label">{{ $t('procurement.field.factoryName') }}</span><span class="value">{{ chain.factory.factoryName }}</span></div>
            <div class="step-item"><span class="label">{{ $t('procurement.field.productCode') }}</span><span class="value">{{ chain.procurement.productCode }}</span></div>
            <div class="step-item"><span class="label">{{ $t('procurement.field.quantity') }}</span><span class="value">{{ chain.procurement.quantity ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('procurement.field.priceRmb') }}</span><span class="value">{{ chain.procurement.priceRmb ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('procurement.field.billingType') }}</span><span class="value">{{ billingTypeLabel(chain.procurement.billingType) }}</span></div>
            <div class="step-item"><span class="label">{{ $t('procurement.field.orderDate') }}</span><span class="value">{{ chain.procurement.orderDate ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('procurement.field.factoryShipDate') }}</span><span class="value">{{ chain.procurement.factoryShipDate ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('procurement.field.plannedShipDate') }}</span><span class="value">{{ chain.procurement.plannedShipDate ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('procurement.field.status') }}</span>
              <el-tag :type="procurementStatusType(chain.procurement.status)" size="small">{{ procurementStatusLabel(chain.procurement.status) }}</el-tag>
            </div>
          </div>
        </template>
        <template v-else>
          <div class="step-empty">{{ $t('orderOverview.stepStatusUI.notStarted') }}</div>
        </template>
      </StepCard>

      <!-- 步骤3：厂家出货 -->
      <StepCard :step-number="3" :title="$t('orderOverview.step3.title')" :status="step3Status">
        <template v-if="chain.shipmentBatches && chain.shipmentBatches.length">
          <div v-for="batch in chain.shipmentBatches" :key="batch.id" class="batch-card">
            <div class="step-grid">
              <div class="step-item"><span class="label">{{ $t('orderOverview.step3.batchCode') }}</span><span class="value">{{ batch.batchCode }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step3.shipmentQuantity') }}</span><span class="value">{{ batch.shipmentQuantity ?? $t('common.format.dash') }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step3.factoryShipDate') }}</span><span class="value">{{ batch.factoryShipDate ?? $t('common.format.dash') }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step3.actualShipDate') }}</span><span class="value">{{ batch.actualShipDate ?? $t('common.format.dash') }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step3.qcRecordCount') }}</span><span class="value">{{ batch.qcRecordCount ?? $t('common.format.dash') }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step3.totalPassedCount') }}</span><span class="value">{{ batch.totalPassedCount ?? $t('common.format.dash') }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step3.status') }}</span>
                <el-tag :type="shipmentStatusType(batch.status)" size="small">{{ shipmentStatusLabel(batch.status) }}</el-tag>
              </div>
            </div>
          </div>
        </template>
        <template v-else>
          <div class="step-empty">{{ $t('orderOverview.stepStatusUI.notStarted') }}</div>
        </template>
      </StepCard>

      <!-- 步骤4：QC验货 -->
      <StepCard :step-number="4" :title="$t('orderOverview.step4.title')" :status="step4Status">
        <template v-if="chain.qcRecords && chain.qcRecords.length">
          <div v-for="qc in chain.qcRecords" :key="qc.id" class="batch-card">
            <div class="step-grid">
              <div class="step-item"><span class="label">{{ $t('orderOverview.step4.qcCode') }}</span><span class="value">{{ qc.qcCode }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step4.result') }}</span>
                <el-tag :type="qcResultType(qc.result)" size="small">{{ qcResultLabel(qc.result) }}</el-tag>
              </div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step4.inspectionCount') }}</span><span class="value">{{ qc.inspectionCount ?? $t('common.format.dash') }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step4.passedCount') }}</span><span class="value">{{ qc.passedCount ?? $t('common.format.dash') }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step4.defectiveCount') }}</span><span class="value">{{ qc.defectiveCount ?? $t('common.format.dash') }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step4.boxCount') }}</span><span class="value">{{ qc.boxCount ?? $t('common.format.dash') }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step4.qcDate') }}</span><span class="value">{{ qc.qcDate ?? $t('common.format.dash') }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step4.status') }}</span>
                <el-tag :type="qcStatusType(qc.status)" size="small">{{ qcStatusLabel(qc.status) }}</el-tag>
              </div>
            </div>
          </div>
        </template>
        <template v-else>
          <div class="step-empty">{{ $t('orderOverview.stepStatusUI.notStarted') }}</div>
        </template>
      </StepCard>

      <!-- 步骤5：调配计划 -->
      <StepCard :step-number="5" :title="$t('orderOverview.step5.title')" :status="step5Status">
        <template v-if="chain.logisticsPlan">
          <div class="step-grid">
            <div class="step-item"><span class="label">{{ $t('orderOverview.step5.planCode') }}</span><span class="value">{{ chain.logisticsPlan.planCode }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step5.cargoVolume') }}</span><span class="value">{{ formatCbm(chain.logisticsPlan.cargoVolumeCbm) }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step5.cargoWeight') }}</span><span class="value">{{ chain.logisticsPlan.cargoWeightKg ? chain.logisticsPlan.cargoWeightKg + ' kg' : $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step5.estimatedShipDate') }}</span><span class="value">{{ chain.logisticsPlan.estimatedShipDate ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step5.actualShipDate') }}</span><span class="value">{{ chain.logisticsPlan.actualShipDate ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step5.status') }}</span>
              <el-tag :type="logisticsStatusType(chain.logisticsPlan.status)" size="small">{{ logisticsStatusLabel(chain.logisticsPlan.status) }}</el-tag>
            </div>
          </div>
        </template>
        <template v-else>
          <div class="step-empty">{{ $t('orderOverview.stepStatusUI.notStarted') }}</div>
        </template>
      </StepCard>

      <!-- 步骤6：国内报关 -->
      <StepCard :step-number="6" :title="$t('orderOverview.step6.title')" :status="step6Status">
        <template v-if="chain.domesticCustoms">
          <div class="step-grid">
            <div class="step-item"><span class="label">{{ $t('orderOverview.step6.customsCode') }}</span><span class="value">{{ chain.domesticCustoms.customsCode }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step6.containerNo') }}</span><span class="value">{{ chain.domesticCustoms.containerNo ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step6.estimatedValueCny') }}</span><span class="value">{{ chain.domesticCustoms.estimatedValueCny ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step6.status') }}</span>
              <el-tag :type="customsStatusType(chain.domesticCustoms.status)" size="small">{{ customsStatusLabel(chain.domesticCustoms.status) }}</el-tag>
            </div>
          </div>
        </template>
        <template v-else>
          <div class="step-empty">{{ $t('orderOverview.stepStatusUI.notStarted') }}</div>
        </template>
      </StepCard>

      <!-- 步骤7：日本清关 -->
      <StepCard :step-number="7" :title="$t('orderOverview.step7.title')" :status="step7Status">
        <template v-if="chain.japanCustoms">
          <div class="step-grid">
            <div class="step-item"><span class="label">{{ $t('orderOverview.step7.customsEntryNo') }}</span><span class="value">{{ chain.japanCustoms.customsEntryNo ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step7.containerNo') }}</span><span class="value">{{ chain.japanCustoms.containerNo ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step7.arrivalDate') }}</span><span class="value">{{ chain.japanCustoms.arrivalDate ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step7.clearanceDate') }}</span><span class="value">{{ chain.japanCustoms.clearanceDate ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step7.customsBroker') }}</span><span class="value">{{ chain.japanCustoms.customsBroker ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step7.importDutyPaid') }}</span><span class="value">{{ chain.japanCustoms.importDutyPaid != null ? '¥' + chain.japanCustoms.importDutyPaid : $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step7.consumptionTaxPaid') }}</span><span class="value">{{ chain.japanCustoms.consumptionTaxPaid != null ? '¥' + chain.japanCustoms.consumptionTaxPaid : $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step7.arrivalPort') }}</span><span class="value">{{ chain.japanCustoms.arrivalPort ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step7.status') }}</span>
              <el-tag :type="customsStatusType(chain.japanCustoms.status)" size="small">{{ customsStatusLabel(chain.japanCustoms.status) }}</el-tag>
            </div>
          </div>
        </template>
        <template v-else>
          <div class="step-empty">{{ $t('orderOverview.stepStatusUI.notStarted') }}</div>
        </template>
      </StepCard>

      <!-- 步骤8：出口退税 -->
      <StepCard :step-number="8" :title="$t('orderOverview.step8.title')" :status="step8Status">
        <template v-if="chain.taxRefund">
          <div class="step-grid">
            <div class="step-item"><span class="label">{{ $t('orderOverview.step8.refundCode') }}</span><span class="value">{{ chain.taxRefund.refundCode ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step8.billingType') }}</span><span class="value">{{ billingTypeLabel(chain.taxRefund.billingType) }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step8.priceRmb') }}</span><span class="value">{{ chain.taxRefund.priceRmb ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step8.quantity') }}</span><span class="value">{{ chain.taxRefund.quantity ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step8.taxPoint') }}</span><span class="value">{{ chain.taxRefund.taxPoint != null ? chain.taxRefund.taxPoint + '%' : $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step8.estimatedRefundRmb') }}</span><span class="value">{{ chain.taxRefund.estimatedRefundRmb ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step8.actualRefundRmb') }}</span><span class="value">{{ chain.taxRefund.actualRefundRmb ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step8.refundDate') }}</span><span class="value">{{ chain.taxRefund.refundDate ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step8.status') }}</span>
              <el-tag :type="taxRefundStatusType(chain.taxRefund.status)" size="small">{{ taxRefundStatusLabel(chain.taxRefund.status) }}</el-tag>
            </div>
          </div>
        </template>
        <template v-else>
          <div class="step-empty">{{ $t('orderOverview.stepStatusUI.notStarted') }}</div>
        </template>
      </StepCard>

      <!-- 步骤9：运营销售 -->
      <StepCard :step-number="9" :title="$t('orderOverview.step9.title')" :status="step9Status">
        <template v-if="chain.salesRecord">
          <div class="step-grid">
            <div class="step-item"><span class="label">{{ $t('orderOverview.step9.recordCode') }}</span><span class="value">{{ chain.salesRecord.recordCode ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step9.salesChannel') }}</span><span class="value">{{ salesChannelLabel(chain.salesRecord.salesChannel) }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step9.listingDate') }}</span><span class="value">{{ chain.salesRecord.listingDate ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step9.currentStock') }}</span><span class="value">{{ chain.salesRecord.currentStock ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step9.salesQuantity') }}</span><span class="value">{{ chain.salesRecord.salesQuantity ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step9.returnedQuantity') }}</span><span class="value">{{ chain.salesRecord.returnedQuantity ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step9.sellingPriceJpy') }}</span><span class="value">{{ chain.salesRecord.sellingPriceJpy != null ? '¥' + chain.salesRecord.sellingPriceJpy : $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step9.status') }}</span>
              <el-tag :type="salesStatusType(chain.salesRecord.status)" size="small">{{ salesStatusLabel(chain.salesRecord.status) }}</el-tag>
            </div>
          </div>
        </template>
        <template v-else>
          <div class="step-empty">{{ $t('orderOverview.stepStatusUI.notStarted') }}</div>
        </template>
      </StepCard>

      <!-- 操作栏 -->
      <div class="action-bar">
        <el-button
          v-if="chain.demand && hasPermission('procurement:delete')"
          type="danger"
          size="small"
          @click="onDelete"
        >
          {{ $t('common.delete') }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { orderChainApi, type OrderChainDetailVO, type ShipmentBatchVO, type QcRecordVO } from '@/api/orderChain'
import { usePermission } from '@/composables/usePermission'
import StatusProgressBar from './components/StatusProgressBar.vue'
import StepCard from './components/StepCard.vue'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const { hasPermission } = usePermission()

const demandId = computed(() => Number(route.params.demandId))

const chain = ref<OrderChainDetailVO | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)

async function fetch() {
  loading.value = true
  error.value = null
  try {
    const res = await orderChainApi.getChainDetail(demandId.value)
    chain.value = res.data
  } catch (e: unknown) {
    error.value = (e as Error).message ?? t('demand.message.loadFailed')
  } finally {
    loading.value = false
  }
}

fetch()

// ====== 步骤状态计算 ======

type StepStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED'

const step1Status = computed<StepStatus>(() =>
  chain.value?.demand ? 'COMPLETED' : 'NOT_STARTED')

const step2Status = computed<StepStatus>(() =>
  chain.value?.procurement ? 'COMPLETED' : 'NOT_STARTED')

const step3Status = computed<StepStatus>(() => {
  if (!chain.value?.shipmentBatches?.length) return 'NOT_STARTED'
  return chain.value.shipmentBatches.some((b: ShipmentBatchVO) => b.status === 'SHIPPED' || b.status === 'IN_TRANSIT')
    ? 'IN_PROGRESS' : 'COMPLETED'
})

const step4Status = computed<StepStatus>(() => {
  if (!chain.value?.qcRecords?.length) return 'NOT_STARTED'
  return chain.value.qcRecords.some((q: QcRecordVO) => q.status === 'COMPLETED' || q.status === 'RETURN_REQUESTED')
    ? 'COMPLETED' : 'IN_PROGRESS'
})

const step5Status = computed<StepStatus>(() => {
  if (!chain.value?.logisticsPlan) return 'NOT_STARTED'
  const s = chain.value.logisticsPlan.status
  return s === 'DELIVERED' ? 'COMPLETED' : 'IN_PROGRESS'
})

const step6Status = computed<StepStatus>(() => {
  if (!chain.value?.domesticCustoms) return 'NOT_STARTED'
  return chain.value.domesticCustoms.status === 'CLEARED' ? 'COMPLETED' : 'IN_PROGRESS'
})

const step7Status = computed<StepStatus>(() => {
  if (!chain.value?.japanCustoms) return 'NOT_STARTED'
  return chain.value.japanCustoms.status === 'CLEARED' ? 'COMPLETED' : 'IN_PROGRESS'
})

const step8Status = computed<StepStatus>(() => {
  if (!chain.value?.taxRefund) return 'NOT_STARTED'
  const s = chain.value.taxRefund.status
  return s === 'COMPLETED' || s === 'NO_REFUND' ? 'COMPLETED' : 'IN_PROGRESS'
})

const step9Status = computed<StepStatus>(() => {
  if (!chain.value?.salesRecord) return 'NOT_STARTED'
  return chain.value.salesRecord.status === 'DISCONTINUED' ? 'COMPLETED' : 'IN_PROGRESS'
})

const stepStatuses = computed<StepStatus[]>(() => [
  step1Status.value,
  step2Status.value,
  step3Status.value,
  step4Status.value,
  step5Status.value,
  step6Status.value,
  step7Status.value,
  step8Status.value,
  step9Status.value,
])

// ====== 删除 ======

async function onDelete() {
  if (!chain.value?.demandId) return
  try {
    await ElMessageBox.confirm(
      t('common.deleteConfirm'),
      t('common.deleteConfirmTitle'),
      { confirmButtonText: t('common.delete'), cancelButtonText: t('common.cancel'), type: 'warning' }
    )
  } catch { return }
  try {
    await orderChainApi.deleteChain(chain.value.demandId)
    ElMessage.success(t('common.deleteSuccess'))
    router.push('/base/overview')
  } catch {
    ElMessage.error(t('common.deleteFailed'))
  }
}

// ====== 标签函数 ======

function demandStatusType(status?: string) {
  if (status === 'PENDING') return 'warning'
  if (status === 'CONFIRMED') return 'success'
  if (status === 'CANCELLED') return 'info'
  return 'info'
}
function demandStatusLabel(status?: string) {
  if (!status) return t('common.format.dash')
  return t(`demand.status.${status}`)
}
function demandTypeLabel(type?: string) {
  if (!type) return t('common.format.dash')
  return t(`demand.type.${type}`)
}
function procurementStatusType(status?: string) {
  if (status === 'PENDING') return 'info'
  if (status === 'IN_PROGRESS') return 'warning'
  if (status === 'COMPLETED') return 'success'
  if (status === 'RETURNED') return 'danger'
  return 'info'
}
function procurementStatusLabel(status?: string) {
  if (!status) return t('common.format.dash')
  return t(`procurement.status.${status}`)
}
function billingTypeLabel(type?: string) {
  if (!type) return t('common.format.dash')
  return t(`procurement.billingType.${type}`)
}
function shipmentStatusType(status?: string) {
  if (status === 'PENDING') return 'info'
  if (status === 'SHIPPED' || status === 'IN_TRANSIT') return 'warning'
  if (status === 'DELIVERED') return 'success'
  return 'info'
}
function shipmentStatusLabel(status?: string) {
  if (!status) return t('common.format.dash')
  return t(`procurement.shipment.status.${status}`)
}
function qcResultType(result?: string) {
  if (result === 'PASSED') return 'success'
  if (result === 'FAILED') return 'danger'
  return 'info'
}
function qcResultLabel(result?: string) {
  if (!result) return t('common.format.dash')
  // 映射 backend PASSED/FAILED → i18n PASS/FAIL
  const key = result === 'PASSED' ? 'PASS' : result === 'FAILED' ? 'FAIL' : result
  return t(`orderOverview.step4.qcResult.${key}`)
}
function qcStatusType(status?: string) {
  if (status === 'COMPLETED') return 'success'
  if (status === 'RETURN_REQUESTED') return 'warning'
  return 'info'
}
function qcStatusLabel(status?: string) {
  if (!status) return t('common.format.dash')
  // 映射 backend COMPLETED/RETURN_REQUESTED → inspection.qcStatus lowercase keys
  const key = status === 'COMPLETED' ? 'completed' : status === 'RETURN_REQUESTED' ? 'returnRequested' : status.toLowerCase()
  return t(`inspection.qcStatus.${key}`)
}
function logisticsStatusType(status?: string) {
  if (status === 'PENDING') return 'info'
  if (status === 'IN_TRANSIT') return 'warning'
  if (status === 'DELIVERED') return 'success'
  return 'info'
}
function logisticsStatusLabel(status?: string) {
  if (!status) return t('common.format.dash')
  return t(`logistics.plan.status.${status}`)
}
function customsStatusType(status?: string) {
  if (status === 'CLEARED') return 'success'
  if (status === 'IN_PROGRESS') return 'warning'
  return 'info'
}
function customsStatusLabel(status?: string) {
  if (!status) return t('common.format.dash')
  return t(`customs.status.${status}`)
}
function taxRefundStatusType(status?: string) {
  if (status === 'COMPLETED' || status === 'NO_REFUND') return 'success'
  if (status === 'PENDING') return 'info'
  return 'warning'
}
function taxRefundStatusLabel(status?: string) {
  if (!status) return t('common.format.dash')
  return t(`finance.taxRefund.status.${status}`)
}
function salesChannelLabel(channel?: string) {
  if (!channel) return t('common.format.dash')
  return t(`sales.channel.${channel}`)
}
function salesStatusType(status?: string) {
  if (status === 'DISCONTINUED') return 'info'
  if (status === 'ACTIVE') return 'success'
  return 'warning'
}
function salesStatusLabel(status?: string) {
  if (!status) return t('common.format.dash')
  return t(`sales.status.${status}`)
}
function subProductSummary(demand: { subProductCode?: string; quantity?: number; destination?: string }): string {
  if (!demand.subProductCode) return t('common.format.dash')
  const parts = [demand.subProductCode]
  if (demand.quantity != null) parts.push(demand.quantity + t('demand.dialog.unitTai'))
  if (demand.destination) parts.push(demand.destination)
  return parts.join(' ')
}
function formatCbm(cbm?: number): string {
  if (cbm == null) return t('common.format.dash')
  return cbm.toFixed(3) + ' CBM'
}
</script>

<style scoped>
.page { padding: 16px; }
.loading-wrap, .error-wrap { display: flex; justify-content: center; align-items: center; min-height: 200px; }
.step-cards { display: flex; flex-direction: column; gap: 12px; }
.step-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 8px; }
.step-item { display: flex; gap: 8px; align-items: center; }
.step-item .label { color: #909399; flex-shrink: 0; min-width: 80px; }
.step-item .value { color: #303133; }
.step-item .value.highlight { color: #409eff; font-weight: 500; }
.step-empty { color: #c0c4cc; font-size: 13px; }
.batch-card { padding: 8px 0; border-top: 1px solid #f0f0f0; }
.batch-card:first-child { border-top: none; padding-top: 0; }
.action-bar { margin-top: 12px; display: flex; gap: 8px; }
</style>
