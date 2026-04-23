<template>
  <div class="page">
    <!-- 选择器模式 -->
    <template v-if="!routeProcurementId">
      <div class="page-header">
        <h2 class="page-title">{{ $t('orderOverview.title') }}</h2>
      </div>
      <el-card class="filter-card" shadow="never">
        <el-form :inline="true" :model="selectorFilter">
          <el-form-item :label="$t('orderOverview.filter.keyword')">
            <el-input v-model="selectorFilter.keyword" :placeholder="$t('orderOverview.filter.keywordPlaceholder')" clearable style="width:200px" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="loadSelector">{{ $t('orderOverview.filter.search') }}</el-button>
            <el-button @click="resetSelector">{{ $t('orderOverview.filter.reset') }}</el-button>
          </el-form-item>
        </el-form>
      </el-card>
      <el-card class="table-card" shadow="never">
        <el-table v-loading="selectorLoading" :data="selectorData" stripe @row-click="onSelectRow">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="productCode" :label="$t('orderOverview.column.productCode')" width="120" />
          <el-table-column prop="factoryName" :label="$t('orderOverview.column.factoryName')" width="160" show-overflow-tooltip />
          <el-table-column prop="status" :label="$t('orderOverview.column.status')" width="140" />
          <el-table-column prop="orderDate" :label="$t('orderOverview.column.orderDate')" width="120" />
          <el-table-column prop="destination" :label="$t('orderOverview.column.destination')" min-width="120" show-overflow-tooltip />
          <el-table-column :label="$t('orderOverview.column.action')" width="120" align="center">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click.stop="router.push('/base/overview/' + row.id)">
                {{ $t('orderOverview.action.view') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-wrap">
          <el-pagination
            background
            :current-page="selectorPage"
            :page-size="selectorPageSize"
            :total="selectorTotal"
            layout="total, prev, pager, next"
            @current-change="onSelectorPage"
          />
        </div>
      </el-card>
    </template>

    <!-- 详情模式 -->
    <template v-else>
      <div class="page-header">
        <div class="header-left">
          <el-button text @click="router.push('/base/overview')">
            <el-icon><ArrowLeft /></el-icon> {{ $t('orderOverview.back') }}
          </el-button>
          <h2 class="page-title">{{ $t('orderOverview.title') }}</h2>
          <el-tag v-if="overview?.procurement">{{ overview.procurement.procurementCode }}</el-tag>
        </div>
        <div class="header-actions">
          <el-button @click="onPrint">{{ $t('orderOverview.print') }}</el-button>
        </div>
      </div>

      <!-- 状态进度条 -->
      <StatusProgressBar
        v-if="overview"
        :step-statuses="overview.stepStatuses"
        :current-step="currentStep"
      />

      <!-- 加载态 -->
      <el-card v-if="loading" class="table-card" shadow="never">
        <el-skeleton :rows="6" animated />
      </el-card>

      <!-- 错误态 -->
      <el-card v-else-if="error" shadow="never">
        <el-result icon="error" :title="$t('orderOverview.loadFailed')" :sub-title="error">
          <template #extra>
            <el-button type="primary" @click="fetch">{{ $t('orderOverview.retry') }}</el-button>
          </template>
        </el-result>
      </el-card>

      <!-- 步骤卡片组 -->
      <div v-else-if="overview" class="step-cards">
        <!-- 步骤1：补货需求 -->
        <StepCard :step-number="1" :title="$t('orderOverview.step1.title')" :status="overview.stepStatuses[0]">
          <template v-if="overview.demand">
            <div class="step-grid">
              <div class="step-item"><span class="label">{{ $t('orderOverview.step1.demandCode') }}</span><span class="value">{{ overview.demand.demandCode }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step1.demandType') }}</span><span class="value">{{ overview.demand.demandType }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step1.productCode') }}</span><span class="value">{{ overview.demand.productCode }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step1.quantity') }}</span><span class="value">{{ overview.demand.quantity }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step1.destination') }}</span><span class="value">{{ overview.demand.destination ?? $t('common.format.dash') }}</span></div>
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
            <div class="step-item"><span class="label">{{ $t('orderOverview.step2.priceRmb') }}</span><span class="value">{{ overview.procurement.priceRmb != null ? `¥${Number(overview.procurement.priceRmb).toFixed(2)}` : $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step2.taxPoint') }}</span><span class="value">{{ overview.procurement.taxPoint ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step2.billingType') }}</span><span class="value">{{ overview.procurement.billingType ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step2.estimatedPriceJpy') }}</span><span class="value">{{ overview.procurement.estimatedPriceJpy != null ? `${Number(overview.procurement.estimatedPriceJpy).toFixed(0)} JPY` : $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step2.orderDate') }}</span><span class="value">{{ overview.procurement.orderDate ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step2.plannedShipDate') }}</span><span class="value">{{ overview.procurement.plannedShipDate ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step2.actualShipDate') }}</span><span class="value">{{ overview.procurement.actualShipDate ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step2.status') }}</span><span class="value">{{ overview.procurement.status ?? $t('common.format.dash') }}</span></div>
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
              <div class="step-item"><span class="label">{{ $t('orderOverview.step4.planType') }}</span><span class="value">{{ overview.logisticsPlan.planType ?? $t('common.format.dash') }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step4.cargoVolume') }}</span><span class="value">{{ overview.logisticsPlan.cargoVolumeCbm != null ? `${Number(overview.logisticsPlan.cargoVolumeCbm).toFixed(4)} CBM` : $t('common.format.dash') }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step4.cargoWeight') }}</span><span class="value">{{ overview.logisticsPlan.cargoWeightKg != null ? `${Number(overview.logisticsPlan.cargoWeightKg).toFixed(2)} kg` : $t('common.format.dash') }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step4.estimatedShipDate') }}</span><span class="value">{{ overview.logisticsPlan.estimatedShipDate ?? $t('common.format.dash') }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step4.actualShipDate') }}</span><span class="value">{{ overview.logisticsPlan.actualShipDate ?? $t('common.format.dash') }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step4.status') }}</span><span class="value">{{ overview.logisticsPlan.status ?? $t('common.format.dash') }}</span></div>
            </div>
          </template>
          <template v-else>
            <div class="step-empty">{{ $t('orderOverview.step.notStarted') }}</div>
          </template>
        </StepCard>

        <!-- 步骤5-8 占位 -->
        <StepCard :step-number="5" :title="$t('orderOverview.step5.title')" :status="overview.stepStatuses[4]">
          <template v-if="overview.domesticCustoms">
            <div class="step-grid">
              <div class="step-item"><span class="label">{{ $t('orderOverview.step5.customsCode') }}</span><span class="value">{{ overview.domesticCustoms.customsCode }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step5.productCode') }}</span><span class="value">{{ overview.domesticCustoms.productCode }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step5.status') }}</span><span class="value">{{ overview.domesticCustoms.status ?? $t('common.format.dash') }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step5.estimatedValue') }}</span><span class="value">{{ overview.domesticCustoms.estimatedValueCny != null ? `¥${Number(overview.domesticCustoms.estimatedValueCny).toFixed(2)}` : $t('common.format.dash') }}</span></div>
            </div>
          </template>
          <template v-else>
            <div class="step-empty">{{ $t('orderOverview.step.notStarted') }}</div>
          </template>
        </StepCard>

        <StepCard :step-number="6" :title="$t('orderOverview.step6.title')" :status="overview.stepStatuses[5]">
          <template v-if="overview.japanCustoms">
            <div class="step-grid">
              <div class="step-item"><span class="label">{{ $t('orderOverview.step6.customsEntryNo') }}</span><span class="value">{{ overview.japanCustoms.customsEntryNo ?? $t('common.format.dash') }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step6.arrivalDate') }}</span><span class="value">{{ overview.japanCustoms.arrivalDate ?? $t('common.format.dash') }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step6.clearanceDate') }}</span><span class="value">{{ overview.japanCustoms.clearanceDate ?? $t('common.format.dash') }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step6.customsBroker') }}</span><span class="value">{{ overview.japanCustoms.customsBroker ?? $t('common.format.dash') }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step6.importDutyPaid') }}</span><span class="value">{{ overview.japanCustoms.importDutyPaid != null ? `${Number(overview.japanCustoms.importDutyPaid).toFixed(2)} JPY` : $t('common.format.dash') }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step6.consumptionTaxPaid') }}</span><span class="value">{{ overview.japanCustoms.consumptionTaxPaid != null ? `${Number(overview.japanCustoms.consumptionTaxPaid).toFixed(2)} JPY` : $t('common.format.dash') }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step6.arrivalPort') }}</span><span class="value">{{ overview.japanCustoms.arrivalPort ?? $t('common.format.dash') }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step6.status') }}</span><span class="value">{{ overview.japanCustoms.status ?? $t('common.format.dash') }}</span></div>
            </div>
          </template>
          <template v-else>
            <div class="step-empty">{{ $t('orderOverview.step.notStarted') }}</div>
          </template>
        </StepCard>

        <StepCard :step-number="7" :title="$t('orderOverview.step7.title')" :status="overview.stepStatuses[6]">
          <template v-if="overview.taxRefund">
            <div class="step-grid">
              <div class="step-item"><span class="label">{{ $t('orderOverview.step7.refundCode') }}</span><span class="value">{{ overview.taxRefund.refundCode ?? $t('common.format.dash') }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step7.estimatedRefundRmb') }}</span><span class="value">{{ overview.taxRefund.estimatedRefundRmb != null ? `¥${Number(overview.taxRefund.estimatedRefundRmb).toFixed(2)}` : $t('common.format.dash') }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step7.actualRefundRmb') }}</span><span class="value">{{ overview.taxRefund.actualRefundRmb != null ? `¥${Number(overview.taxRefund.actualRefundRmb).toFixed(2)}` : $t('common.format.dash') }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step7.refundDate') }}</span><span class="value">{{ overview.taxRefund.refundDate ?? $t('common.format.dash') }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step7.status') }}</span><span class="value">{{ overview.taxRefund.status ?? $t('common.format.dash') }}</span></div>
            </div>
          </template>
          <template v-else>
            <div class="step-empty">{{ $t('orderOverview.step.notStarted') }}</div>
          </template>
        </StepCard>
        <StepCard :step-number="8" :title="$t('orderOverview.step8.title')" :status="overview.stepStatuses[7]">
          <template v-if="overview.salesRecord">
            <div class="step-grid">
              <div class="step-item"><span class="label">{{ $t('orderOverview.step8.recordCode') }}</span><span class="value">{{ overview.salesRecord.recordCode ?? $t('common.format.dash') }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step8.salesChannel') }}</span><span class="value">{{ overview.salesRecord.salesChannel ?? $t('common.format.dash') }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step8.listingDate') }}</span><span class="value">{{ overview.salesRecord.listingDate ?? $t('common.format.dash') }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step8.currentStock') }}</span><span class="value">{{ overview.salesRecord.currentStock ?? $t('common.format.dash') }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step8.salesQuantity') }}</span><span class="value">{{ overview.salesRecord.salesQuantity ?? $t('common.format.dash') }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step8.returnRate') }}</span><span class="value">{{ overview.salesRecord.returnRate != null ? `${(Number(overview.salesRecord.returnRate) * 100).toFixed(2)}%` : $t('common.format.dash') }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step8.sellingPriceJpy') }}</span><span class="value">{{ overview.salesRecord.sellingPriceJpy != null ? `${Number(overview.salesRecord.sellingPriceJpy).toFixed(0)} JPY` : $t('common.format.dash') }}</span></div>
              <div class="step-item"><span class="label">{{ $t('orderOverview.step8.status') }}</span><span class="value">{{ overview.salesRecord.status ?? $t('common.format.dash') }}</span></div>
            </div>
          </template>
          <template v-else>
            <div class="step-empty">{{ $t('orderOverview.step.notStarted') }}</div>
          </template>
        </StepCard>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { useOrderOverview } from '@/composables/useOrderOverview'
import { orderOverviewApi } from '@/api/orderOverview'
import type { ProcurementPageVO } from '@/api/orderOverview'
import StatusProgressBar from './components/StatusProgressBar.vue'
import StepCard from './components/StepCard.vue'

const route = useRoute()
const router = useRouter()

const routeProcurementId = computed(() => {
  const id = route.params.procurementId
  return id ? Number(id) : null
})

// ===== 选择器模式 =====
const selectorLoading = ref(false)
loadSelector() // 初始加载
const selectorData = ref<ProcurementPageVO[]>([])
const selectorPage = ref(1)
const selectorPageSize = ref(20)
const selectorTotal = ref(0)
const selectorFilter = reactive({ keyword: '' })

async function loadSelector() {
  selectorLoading.value = true
  try {
    const res = await orderOverviewApi.listSelector({
      page: selectorPage.value - 1,
      pageSize: selectorPageSize.value,
      keyword: selectorFilter.keyword || undefined,
    })
    selectorData.value = res.data.data.content
    selectorTotal.value = res.data.data.totalElements
  } finally {
    selectorLoading.value = false
  }
}

function resetSelector() {
  selectorFilter.keyword = ''
  selectorPage.value = 1
  loadSelector()
}

function onSelectorPage() {
  loadSelector()
}

function onSelectRow(row: ProcurementPageVO) {
  router.push('/base/overview/' + row.id)
}

// ===== 详情模式 =====
const procurementId = computed(() => routeProcurementId.value ?? 0)
const { overview, loading, error, currentStep, fetch } = useOrderOverview(procurementId)

function onPrint() {
  window.print()
}

function qcResultType(result?: string) {
  if (result === 'PASS') return 'success'
  if (result === 'FAIL') return 'danger'
  return 'info'
}
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}
.header-actions {
  display: flex;
  gap: 8px;
}
.stats-row {
  margin-bottom: 16px;
}
.filter-card {
  margin-bottom: 16px;
}
.table-card {
  margin-bottom: 16px;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.step-cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.step-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}
.step-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.step-item .label {
  font-size: 12px;
  color: #909399;
}
.step-item .value {
  font-size: 14px;
  color: #303133;
}
.step-empty {
  color: #909399;
  font-size: 14px;
  text-align: center;
  padding: 16px 0;
}
</style>
