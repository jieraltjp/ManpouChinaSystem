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

    <!-- 进度条：step1=COMPLETED, step2-8=NOT_STARTED -->
    <StatusProgressBar v-if="overview" :step-statuses="overview.stepStatuses" />

    <div v-if="overview" class="step-cards">
      <!-- 步骤1：补货需求（唯一有数据的步骤）-->
      <StepCard :step-number="1" :title="$t('orderOverview.step1.title')" :status="overview.stepStatuses[0]">
        <template v-if="overview.demand">
          <div class="step-grid">
            <div class="step-item"><span class="label">{{ $t('orderOverview.step1.demandCode') }}</span><span class="value">{{ overview.demand.demandCode }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step1.demandType') }}</span><span class="value">{{ demandTypeLabel(overview.demand.demandType) }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step1.productCode') }}</span><span class="value">{{ overview.demand.productCode }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step1.subProductItems') }}</span><span class="value highlight">{{ subProductSummary(overview.demand) }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step1.japanLead') }}</span><span class="value">{{ overview.demand.japanLead ?? $t('common.format.dash') }}</span></div>
            <div class="step-item"><span class="label">{{ $t('orderOverview.step1.status') }}</span>
              <el-tag :type="demandStatusType(overview.demand.status)" size="small">{{ demandStatusLabel(overview.demand.status) }}</el-tag>
            </div>
          </div>
          <!-- 转采购按钮（v2.2.0 已移除） -->
        </template>
        <template v-else>
          <div class="step-empty">{{ $t('orderOverview.step.notStarted') }}</div>
        </template>
      </StepCard>

      <!-- 步骤2-8：全部未开始 -->
      <StepCard v-for="step in remainingSteps" :key="step.num" :step-number="step.num" :title="step.title" :status="'NOT_STARTED'">
        <div class="step-empty">{{ $t('orderOverview.step.notStarted') }}</div>
      </StepCard>
    </div>

  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Loading } from '@element-plus/icons-vue'
import { orderOverviewApi, type DemandOverviewVO } from '@/api/orderOverview'
import StatusProgressBar from './components/StatusProgressBar.vue'
import StepCard from './components/StepCard.vue'

const route = useRoute()
const { t } = useI18n()

const demandId = computed(() => Number(route.params.demandId))

const overview = ref<DemandOverviewVO | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)
async function fetch() {
  loading.value = true
  error.value = null
  try {
    const res = await orderOverviewApi.getDemandOverview(demandId.value)
    overview.value = res.data.data
  } catch (e: unknown) {
    error.value = (e as Error).message ?? t('demand.message.loadFailed')
  } finally {
    loading.value = false
  }
}

fetch()

const remainingSteps = computed(() => {
  const steps = [
    { num: 2, key: 'orderOverview.step2.title' },
    { num: 3, key: 'orderOverview.step3.title' },
    { num: 4, key: 'orderOverview.step4.title' },
    { num: 5, key: 'orderOverview.step5.title' },
    { num: 6, key: 'orderOverview.step6.title' },
    { num: 7, key: 'orderOverview.step7.title' },
    { num: 8, key: 'orderOverview.step8.title' },
  ]
  return steps.map(s => ({ num: s.num, title: s.num + ' ' + t(s.key) }))
})

function demandStatusType(status?: string) {
  if (status === 'PENDING') return 'warning'
  if (status === 'CONVERTED') return 'success'
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
.loading-wrap, .error-wrap { display: flex; justify-content: center; align-items: center; min-height: 200px; }
.step-cards { display: flex; flex-direction: column; gap: 12px; margin-top: 16px; }
.step-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 8px; }
.step-item { display: flex; gap: 8px; align-items: center; }
.step-item .label { color: #909399; flex-shrink: 0; min-width: 80px; }
.step-item .value { color: #303133; }
.step-item .value.highlight { color: #409eff; font-weight: 500; }
.step-empty { color: #c0c4cc; font-size: 13px; }
.action-bar { margin-top: 12px; display: flex; gap: 8px; }
</style>
