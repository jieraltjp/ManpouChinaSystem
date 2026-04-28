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
      <div v-if="currentRow" class="drawer-content">
        <el-descriptions :column="1" border>
          <el-descriptions-item :label="$t('orderOverview.column.demandCode')">{{ currentRow.demandCode || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.column.productCode')">{{ currentRow.demandProductCode || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.column.subProductCode')">{{ currentRow.demandSubProductCode || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.column.demandType')">{{ currentRow.demandType || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.column.quantity')">{{ currentRow.demandQuantity ?? '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.column.destination')">{{ currentRow.demandDestination || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.column.japanLead')">{{ currentRow.demandJapanLead || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.column.factoryName')">{{ currentRow.snapshot?.factoryName || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.column.factoryProvince')">{{ currentRow.snapshot?.factoryProvince || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.column.factoryCity')">{{ currentRow.snapshot?.factoryCity || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.column.productNameZh')">{{ currentRow.snapshot?.productNameZh || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.column.productNameJa')">{{ currentRow.snapshot?.productNameJa || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.column.productCategory')">{{ currentRow.snapshot?.productCategory || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.column.demandStatus')">{{ currentRow.demandStatus || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.column.linkedProcurementId')">{{ currentRow.linkedProcurementId ?? '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('orderOverview.column.demandCreateTime')">{{ currentRow.demandCreateTime || '-' }}</el-descriptions-item>
        </el-descriptions>
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
import { orderChainApi, type OrderChainVO } from '@/api/orderChain'

const router = useRouter()

const loading = ref(false)
const chainData = ref<OrderChainVO[]>([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const filter = reactive({ status: '', keyword: '' })
const drawerVisible = ref(false)
const currentRow = ref<OrderChainVO | null>(null)

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

function onView(row: OrderChainVO) {
  currentRow.value = row
  drawerVisible.value = true
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
</style>
