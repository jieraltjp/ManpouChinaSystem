<template>
  <div class="page">
    <div class="page-header">
    </div>

    <!-- Tab 切换：需求单 / 发注单 -->
    <el-tabs v-model="activeTab" class="overview-tabs">
      <!-- 需求单 Tab -->
      <el-tab-pane :label="$t('orderOverview.tab.demands')" name="demands">
        <el-card class="filter-card" shadow="never">
          <el-form :inline="true" :model="demandFilter">
            <el-form-item :label="$t('orderOverview.filter.status')">
              <el-select v-model="demandFilter.status" clearable style="width:140px">
                <el-option :label="$t('orderOverview.status.all')" value="" />
                <el-option :label="$t('orderOverview.status.pending')" value="PENDING" />
                <el-option :label="$t('orderOverview.status.converted')" value="CONVERTED" />
                <el-option :label="$t('orderOverview.status.cancelled')" value="CANCELLED" />
              </el-select>
            </el-form-item>
            <el-form-item :label="$t('orderOverview.filter.keyword')">
              <el-input v-model="demandFilter.keyword" :placeholder="$t('orderOverview.filter.keywordPlaceholder')" clearable style="width:200px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadDemandSelector">{{ $t('orderOverview.filter.search') }}</el-button>
              <el-button @click="resetDemandFilter">{{ $t('orderOverview.filter.reset') }}</el-button>
            </el-form-item>
          </el-form>
        </el-card>
        <el-card class="table-card" shadow="never">
          <el-table v-loading="demandLoading" :data="demandSelectorData" stripe @row-click="onSelectDemand" min-height="200">
            <el-table-column prop="id" :label="$t('orderOverview.column.id')" min-width="80" />
            <el-table-column prop="demandCode" :label="$t('orderOverview.column.demandCode')" min-width="160" />
            <el-table-column prop="demandType" :label="$t('orderOverview.column.demandType')" min-width="120" />
            <el-table-column prop="productCode" :label="$t('orderOverview.column.productCode')" min-width="120" />
            <el-table-column prop="subProductCode" :label="$t('orderOverview.column.subProductCode')" min-width="140">
              <template #default="{ row }">
                <span class="product-code">{{ row.subProductCode }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="quantity" :label="$t('orderOverview.column.quantity')" min-width="80" align="right">
              <template #default="{ row }">
                <span class="qty-value">{{ row.quantity || 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="destination" :label="$t('orderOverview.column.destination')" min-width="110">
              <template #default="{ row }">
                {{ row.destination || '—' }}
              </template>
            </el-table-column>
            <el-table-column prop="japanLead" :label="$t('orderOverview.column.japanLead')" min-width="100" />
            <el-table-column prop="status" :label="$t('orderOverview.column.status')" min-width="120">
              <template #default="{ row }">
                <el-tag :type="demandStatusType(row.status)" size="small">{{ demandStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="$t('orderOverview.column.action')" min-width="100" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click.stop="router.push('/base/overview/demand/' + row.id)">
                  {{ $t('orderOverview.action.view') }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-wrap">
            <el-pagination
              background
              :current-page="demandPage"
              :page-size="demandPageSize"
              :total="demandTotal"
              layout="total, prev, pager, next"
              @current-change="onDemandPage"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 发注单 Tab -->
      <el-tab-pane :label="$t('orderOverview.tab.procurements')" name="procurements">
        <el-card class="filter-card" shadow="never">
          <el-form :inline="true" :model="procurementFilter">
            <el-form-item :label="$t('orderOverview.filter.keyword')">
              <el-input v-model="procurementFilter.keyword" :placeholder="$t('orderOverview.filter.keywordPlaceholder')" clearable style="width:200px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadProcurementSelector">{{ $t('orderOverview.filter.search') }}</el-button>
              <el-button @click="resetProcurementFilter">{{ $t('orderOverview.filter.reset') }}</el-button>
            </el-form-item>
          </el-form>
        </el-card>
        <el-card class="table-card" shadow="never">
          <el-table v-loading="procurementLoading" :data="procurementSelectorData" stripe @row-click="onSelectProcurement" min-height="200">
            <el-table-column prop="id" :label="$t('orderOverview.column.id')" min-width="80" />
            <el-table-column prop="productCode" :label="$t('orderOverview.column.productCode')" min-width="120" />
            <el-table-column prop="factoryName" :label="$t('orderOverview.column.factoryName')" min-width="160" show-overflow-tooltip />
            <el-table-column prop="status" :label="$t('orderOverview.column.status')" min-width="140" />
            <el-table-column prop="orderDate" :label="$t('orderOverview.column.orderDate')" min-width="120" />
            <el-table-column prop="destination" :label="$t('orderOverview.column.destination')" min-width="120" show-overflow-tooltip />
            <el-table-column :label="$t('orderOverview.column.action')" min-width="100" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click.stop="router.push('/base/overview/procurement/' + row.id)">
                  {{ $t('orderOverview.action.view') }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-wrap">
            <el-pagination
              background
              :current-page="procurementPage"
              :page-size="procurementPageSize"
              :total="procurementTotal"
              layout="total, prev, pager, next"
              @current-change="onProcurementPage"
            />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { orderOverviewApi, type DemandSelectorVO, type ProcurementPageVO } from '@/api/orderOverview'

const router = useRouter()
const { t } = useI18n()

// ===== Tab 状态 =====
const activeTab = ref<'demands' | 'procurements'>('demands')

// ===== Demand 列表 =====
const demandLoading = ref(false)
const demandSelectorData = ref<DemandSelectorVO[]>([])
const demandPage = ref(1)
const demandPageSize = ref(20)
const demandTotal = ref(0)
const demandFilter = reactive({ status: '', keyword: '' })

async function loadDemandSelector() {
  demandLoading.value = true
  try {
    const res = await orderOverviewApi.listDemandSelector({
      page: demandPage.value - 1,
      pageSize: demandPageSize.value,
      status: demandFilter.status || undefined,
      keyword: demandFilter.keyword || undefined,
    })
    demandSelectorData.value = res.data.data?.content ?? []
    demandTotal.value = res.data.data?.totalElements ?? 0
  } finally {
    demandLoading.value = false
  }
}

function resetDemandFilter() {
  demandFilter.status = ''
  demandFilter.keyword = ''
  demandPage.value = 1
  loadDemandSelector()
}

function onDemandPage(page: number) {
  demandPage.value = page
  loadDemandSelector()
}

function onSelectDemand(row: DemandSelectorVO) {
  router.push('/base/overview/demand/' + row.id)
}

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

// ===== Procurement 列表 =====
const procurementLoading = ref(false)
const procurementSelectorData = ref<ProcurementPageVO[]>([])
const procurementPage = ref(1)
const procurementPageSize = ref(20)
const procurementTotal = ref(0)
const procurementFilter = reactive({ keyword: '' })

async function loadProcurementSelector() {
  procurementLoading.value = true
  try {
    const res = await orderOverviewApi.listProcurementSelector({
      page: procurementPage.value - 1,
      pageSize: procurementPageSize.value,
      keyword: procurementFilter.keyword || undefined,
    })
    procurementSelectorData.value = res.data.data?.content ?? []
    procurementTotal.value = res.data.data?.totalElements ?? 0
  } finally {
    procurementLoading.value = false
  }
}

function resetProcurementFilter() {
  procurementFilter.keyword = ''
  procurementPage.value = 1
  loadProcurementSelector()
}

function onProcurementPage(page: number) {
  procurementPage.value = page
  loadProcurementSelector()
}

function onSelectProcurement(row: ProcurementPageVO) {
  router.push('/base/overview/procurement/' + row.id)
}

// 初始加载
onMounted(() => {
  loadDemandSelector()
  loadProcurementSelector()
})
</script>

<style scoped>
.page { padding: 16px; }
.page-header { margin-bottom: 16px; }
.page-title { margin: 0; font-size: 18px; font-weight: 600; }
.overview-tabs :deep(.el-tabs__header) { margin-bottom: 12px; }
.table-card { margin-top: 0; }
.filter-card { margin-bottom: 12px; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 12px; }
.product-code { color: var(--color-primary); font-family: monospace; font-size: 12px; font-weight: 700; background: var(--color-primary-pale); padding: 3px 9px; border-radius: 5px; }
.qty-value { color: #D97706; font-weight: 600; }
</style>
