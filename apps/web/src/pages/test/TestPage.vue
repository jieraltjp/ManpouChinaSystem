<template>
  <div class="test-page">
    <div class="page-header">
      <h2 class="page-title">采购单管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="onNew">
          <el-icon><Plus /></el-icon>
          新建采购单
        </el-button>
      </div>
    </div>

    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="采购单号">
          <el-input v-model="filterForm.orderNo" placeholder="PO+yyyyMMdd+序号" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="待审核" value="PENDING" />
            <el-option label="已批准" value="APPROVED" />
            <el-option label="已拒绝" value="REJECTED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="filterForm.priority" placeholder="全部" clearable style="width: 120px">
            <el-option label="紧急" value="URGENT" />
            <el-option label="高" value="HIGH" />
            <el-option label="普通" value="NORMAL" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card class="table-card" shadow="never">
      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
        style="width: 100%"
        @row-click="onRowClick"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="orderNo" label="采购单号" width="180">
          <template #default="{ row }">
            <span class="order-no">{{ row.orderNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="contactName" label="联系人" width="100" />
        <el-table-column prop="shippingAddress" label="发货地址" min-width="160" show-overflow-tooltip />
        <el-table-column prop="priority" label="优先级" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="priorityType(row.priority)" size="small">
              {{ priorityLabel(row.priority) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isExport" label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isExport" type="warning" size="small">出口</el-tag>
            <el-tag v-else type="info" size="small">内贸</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="onView(row)">详情</el-button>
            <el-button link type="primary" size="small" @click.stop="onEdit(row)" :disabled="row.status !== 'DRAFT'">编辑</el-button>
            <el-button link type="danger" size="small" @click.stop="onDelete(row)" :disabled="row.status !== 'DRAFT'">删除</el-button>
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
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" title="采购单详情" size="600px" direction="rtl">
      <el-descriptions :column="2" border v-if="currentRow">
        <el-descriptions-item label="采购单号">{{ currentRow.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType(currentRow.status)" size="small">
            {{ statusLabel(currentRow.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="联系人">{{ currentRow.contactName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ currentRow.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="发货地址" :span="2">{{ currentRow.shippingAddress }}</el-descriptions-item>
        <el-descriptions-item label="优先级">
          <el-tag :type="priorityType(currentRow.priority)" size="small">
            {{ priorityLabel(currentRow.priority) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="类型">
          {{ currentRow.isExport ? '出口' : '内贸' }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ currentRow.createTime }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentRow.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div class="drawer-actions">
        <el-button @click="drawerVisible = false">关闭</el-button>
        <el-button type="primary" v-if="currentRow?.status === 'DRAFT'" @click="onSubmit(currentRow)">
          提交审核
        </el-button>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

interface PurchaseOrder {
  id: number
  orderNo: string
  contactName: string
  contactPhone: string
  shippingAddress: string
  isExport: boolean
  status: string
  priority: string
  remark: string
  createTime: string
}

const loading = ref(false)
const drawerVisible = ref(false)
const currentRow = ref<PurchaseOrder | null>(null)

const filterForm = reactive({
  orderNo: '',
  status: '',
  priority: '',
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
})

// 模拟数据
const tableData = ref<PurchaseOrder[]>([
  {
    id: 1,
    orderNo: 'PO20260419001',
    contactName: '张三',
    contactPhone: '13800138000',
    shippingAddress: '浙江省金华市永康市五金城A区',
    isExport: true,
    status: 'DRAFT',
    priority: 'NORMAL',
    remark: '新品发注，货号待确认',
    createTime: '2026-04-19 10:30:00',
  },
  {
    id: 2,
    orderNo: 'PO20260418003',
    contactName: '李四',
    contactPhone: '13900139000',
    shippingAddress: '广东省深圳市宝安区福永镇',
    isExport: false,
    status: 'PENDING',
    priority: 'URGENT',
    remark: '加急订单',
    createTime: '2026-04-18 14:22:00',
  },
  {
    id: 3,
    orderNo: 'PO20260417005',
    contactName: '王五',
    contactPhone: '13700137000',
    shippingAddress: '浙江省杭州市余杭区阿里巴巴园区',
    isExport: true,
    status: 'APPROVED',
    priority: 'HIGH',
    remark: '',
    createTime: '2026-04-17 09:15:00',
  },
  {
    id: 4,
    orderNo: 'PO20260416002',
    contactName: '赵六',
    contactPhone: '13600136000',
    shippingAddress: '上海市浦东新区外高桥保税区',
    isExport: true,
    status: 'REJECTED',
    priority: 'NORMAL',
    remark: '货号有误，请重新提交',
    createTime: '2026-04-16 16:45:00',
  },
  {
    id: 5,
    orderNo: 'PO20260415008',
    contactName: '孙七',
    contactPhone: '13500135000',
    shippingAddress: '江苏省苏州市工业园区星湖街',
    isExport: false,
    status: 'CANCELLED',
    priority: 'NORMAL',
    remark: '客户取消订单',
    createTime: '2026-04-15 11:20:00',
  },
])

function loadData() {
  loading.value = true
  setTimeout(() => {
    pagination.total = 5
    loading.value = false
  }, 600)
}

function onReset() {
  filterForm.orderNo = ''
  filterForm.status = ''
  filterForm.priority = ''
  loadData()
}

function onNew() {
  ElMessage.info('新建采购单页面（待实现）')
}

function onView(row: PurchaseOrder) {
  currentRow.value = row
  drawerVisible.value = true
}

function onEdit(row: PurchaseOrder) {
  ElMessage.info(`编辑采购单 ${row.orderNo}（待实现）`)
}

function onDelete(row: PurchaseOrder) {
  ElMessage.warning(`删除采购单 ${row.orderNo}（待实现）`)
}

function onRowClick(row: PurchaseOrder) {
  onView(row)
}

function onSubmit(row: PurchaseOrder) {
  ElMessage.success(`提交采购单 ${row.orderNo} 审核（待实现）`)
}

function statusLabel(status: string): string {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    PENDING: '待审核',
    APPROVED: '已批准',
    REJECTED: '已拒绝',
    CANCELLED: '已取消',
  }
  return map[status] ?? status
}

function statusType(status: string): string {
  const map: Record<string, string> = {
    DRAFT: 'info',
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
    CANCELLED: 'info',
  }
  return map[status] ?? 'info'
}

function priorityLabel(priority: string): string {
  const map: Record<string, string> = {
    URGENT: '紧急',
    HIGH: '高',
    NORMAL: '普通',
  }
  return map[priority] ?? priority
}

function priorityType(priority: string): string {
  const map: Record<string, string> = {
    URGENT: 'danger',
    HIGH: 'warning',
    NORMAL: 'info',
  }
  return map[priority] ?? 'info'
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.test-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.filter-card :deep(.el-card__body) {
  padding-bottom: 0;
}

.table-card :deep(.el-table__row) {
  cursor: pointer;
}

.order-no {
  color: #409eff;
  font-family: monospace;
  font-size: 13px;
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.drawer-actions {
  position: absolute;
  bottom: 20px;
  left: 0;
  right: 0;
  padding: 16px 24px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}
</style>
