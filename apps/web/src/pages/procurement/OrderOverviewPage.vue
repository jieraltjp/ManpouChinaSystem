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
      <el-table v-loading="loading" :data="chainData" stripe @row-click="onRowClick" min-height="200">
        <!-- 需求编号 -->
        <el-table-column prop="demandCode" :label="$t('orderOverview.column.demandCode')" min-width="160" />
        <!-- 商品名称（快照） -->
        <el-table-column :label="$t('orderOverview.column.productCode')" min-width="140">
          <template #default="{ row }">
            <span v-if="row.snapshot?.productNameZh">{{ row.snapshot.productNameZh }}</span>
            <span v-else class="text-muted">{{ row.demandProductCode || '—' }}</span>
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
        <!-- 4步状态进度 -->
        <el-table-column :label="$t('orderOverview.column.stepStatus')" min-width="200" align="center">
          <template #default="{ row }">
            <div class="step-status">
              <span class="step-badge" :class="row.step1Status">{{ $t('orderOverview.step.1') }}</span>
              <span class="step-arrow">→</span>
              <span class="step-badge" :class="row.step2Status">{{ $t('orderOverview.step.2') }}</span>
              <span class="step-arrow">→</span>
              <span class="step-badge" :class="row.step3Status">{{ $t('orderOverview.step.3') }}</span>
              <span class="step-arrow">→</span>
              <span class="step-badge" :class="row.step4Status">{{ $t('orderOverview.step.4') }}</span>
            </div>
          </template>
        </el-table-column>
        <!-- 操作 -->
        <el-table-column :label="$t('orderOverview.column.action')" min-width="80" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="router.push('/base/overview/demand/' + row.demandId)">
              {{ $t('orderOverview.action.view') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { orderChainApi, type OrderChainVO } from '@/api/orderChain'

const router = useRouter()

const loading = ref(false)
const chainData = ref<OrderChainVO[]>([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const filter = reactive({ status: '', keyword: '' })

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
.product-code { color: var(--color-primary); font-family: monospace; font-size: 12px; font-weight: 700; background: var(--color-primary-pale); padding: 3px 9px; border-radius: 5px; }
.qty-value { color: #D97706; font-weight: 600; }
.text-muted { color: #999; }
.step-status { display: flex; align-items: center; justify-content: center; gap: 2px; font-size: 12px; }
.step-badge {
  display: inline-block;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 11px;
  font-weight: 600;
}
.step-badge.NOT_STARTED { background: #f5f5f5; color: #999; }
.step-badge.COMPLETED { background: #d4edda; color: #155724; }
.step-badge.IN_PROGRESS { background: #fff3cd; color: #856404; }
.step-arrow { color: #ccc; margin: 0 1px; }
</style>
