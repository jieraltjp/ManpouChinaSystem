<template>
  <div class="page">
    <!-- Filter Card -->
    <el-card class="filter-card" :body-style="{ padding: '12px 16px' }">
      <el-form :inline="true" :model="filter" @submit.prevent>
        <el-form-item :label="$t('offlineOrder.filter.code')">
          <el-input v-model="filter.code" :placeholder="$t('offlineOrder.filter.codeHint')" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item :label="$t('offlineOrder.filter.itemName')">
          <el-input v-model="filter.itemName" :placeholder="$t('offlineOrder.filter.itemNameHint')" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item :label="$t('offlineOrder.filter.factory')">
          <el-input v-model="filter.factory" :placeholder="$t('offlineOrder.filter.factoryHint')" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item :label="$t('offlineOrder.filter.arrival')">
          <el-select v-model="filter.arrival" :placeholder="$t('offlineOrder.filter.arrivalHint')" clearable style="width: 120px">
            <el-option :label="$t('offlineOrder.filter.all')" value="" />
            <el-option :label="$t('offlineOrder.arrivalStatus.out')" value="OUT" />
            <el-option :label="$t('offlineOrder.arrivalStatus.notOut')" value="NOT_OUT" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">{{ $t('offlineOrder.filter.search') }}</el-button>
          <el-button @click="onReset">{{ $t('offlineOrder.filter.reset') }}</el-button>
          <el-button type="primary" @click="onCreate" v-if="hasPermission('offline_order:create')">
            <el-icon><Plus /></el-icon>{{ $t('offlineOrder.newButton') }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Table Card -->
    <el-card class="table-card" :body-style="{ padding: '0' }">
      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%">
        <el-table-column type="selection" width="40" align="center" />
        <el-table-column :label="$t('offlineOrder.column.code')" prop="code" show-overflow-tooltip min-width="120" />
        <el-table-column :label="$t('offlineOrder.column.subCode')" prop="subCode" show-overflow-tooltip min-width="100" />
        <el-table-column :label="$t('offlineOrder.column.houkoku')" prop="houkoku" show-overflow-tooltip min-width="100" />
        <el-table-column :label="$t('offlineOrder.column.itemName')" prop="itemName" show-overflow-tooltip min-width="140" />
        <el-table-column :label="$t('offlineOrder.column.volumeCount')" prop="volumeCount" show-overflow-tooltip min-width="100" />
        <el-table-column :label="$t('offlineOrder.column.orderCount')" prop="orderCount" show-overflow-tooltip min-width="100" />
        <el-table-column :label="$t('offlineOrder.column.expectedDate')" prop="expectedDate" show-overflow-tooltip min-width="110" />
        <el-table-column :label="$t('offlineOrder.column.orderDate')" prop="orderDate" show-overflow-tooltip min-width="110" />
        <el-table-column :label="$t('offlineOrder.column.arrival')" prop="arrival" width="110">
          <template #default="{ row }">
            <el-tag :type="arrivalTagType(row.arrival)" size="small">{{ arrivalLabel(row.arrival) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('offlineOrder.column.unitCh')" prop="unitCh" show-overflow-tooltip min-width="80" />
        <el-table-column :label="$t('offlineOrder.column.rate')" prop="rate" show-overflow-tooltip min-width="80" />
        <el-table-column :label="$t('offlineOrder.column.souko')" prop="souko" show-overflow-tooltip min-width="100" />
        <el-table-column :label="$t('offlineOrder.column.factory')" prop="factory" show-overflow-tooltip min-width="120" />
        <el-table-column :label="$t('offlineOrder.column.contactor')" prop="contactor" show-overflow-tooltip min-width="100" />
        <el-table-column :label="$t('offlineOrder.column.contactorTel')" prop="contactorTel" show-overflow-tooltip min-width="120" />
        <el-table-column :label="$t('offlineOrder.column.principal')" prop="principal" show-overflow-tooltip min-width="100" />
        <el-table-column :label="$t('offlineOrder.column.updater')" prop="updater" show-overflow-tooltip min-width="100" />
        <el-table-column :label="$t('offlineOrder.column.actions')" width="160">
          <template #default="{ row }">
            <el-button link class="btn-blue" size="small" @click="onDetail(row)">{{ $t('offlineOrder.dialog.detail') }}</el-button>
            <el-button link type="warning" size="small" @click="onEdit(row)" v-if="hasPermission('offline_order:update')">{{ $t('offlineOrder.dialog.edit') }}</el-button>
            <el-button link type="danger" size="small" @click="onDelete(row)" v-if="hasPermission('offline_order:delete')">{{ $t('offlineOrder.dialog.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Pagination -->
    <div class="pagination-bar">
      <el-pagination
        v-model:current-page="filter.page"
        v-model:page-size="filter.pageSize"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="onPageChange"
        @size-change="loadData"
      />
    </div>

    <!-- Detail Drawer -->
    <el-drawer
      v-model="detailVisible"
      :title="$t('offlineOrder.drawerTitle')"
      direction="rtl"
      size="680px"
    >
      <div class="drawer-content">
        <!-- Basic Info -->
        <div class="drawer-section-title">{{ $t('offlineOrder.drawerSection.basic') }}</div>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.code') }}:</span>
            <span class="detail-value">{{ currentRow?.code || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.subCode') }}:</span>
            <span class="detail-value">{{ currentRow?.subCode || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.houkoku') }}:</span>
            <span class="detail-value">{{ currentRow?.houkoku || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.infoFile') }}:</span>
            <span class="detail-value">{{ currentRow?.infoFile || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.itemName') }}:</span>
            <span class="detail-value">{{ currentRow?.itemName || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.volumeCount') }}:</span>
            <span class="detail-value">{{ currentRow?.volumeCount ?? '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.orderCount') }}:</span>
            <span class="detail-value">{{ currentRow?.orderCount ?? '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.arrival') }}:</span>
            <span class="detail-value">{{ arrivalLabel(currentRow?.arrival) }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.unitCh') }}:</span>
            <span class="detail-value">{{ currentRow?.unitCh ?? '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.rate') }}:</span>
            <span class="detail-value">{{ currentRow?.rate ?? '-' }}</span>
          </div>
        </div>

        <!-- Date Info -->
        <div class="drawer-section-title">{{ $t('offlineOrder.drawerSection.date') }}</div>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.expectedDate') }}:</span>
            <span class="detail-value">{{ currentRow?.expectedDate || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.orderDate') }}:</span>
            <span class="detail-value">{{ currentRow?.orderDate || '-' }}</span>
          </div>
        </div>

        <!-- Factory Info -->
        <div class="drawer-section-title">{{ $t('offlineOrder.drawerSection.factory') }}</div>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.factory') }}:</span>
            <span class="detail-value">{{ currentRow?.factory || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.souko') }}:</span>
            <span class="detail-value">{{ currentRow?.souko || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.contactor') }}:</span>
            <span class="detail-value">{{ currentRow?.contactor || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.contactorTel') }}:</span>
            <span class="detail-value">{{ currentRow?.contactorTel || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.principal') }}:</span>
            <span class="detail-value">{{ currentRow?.principal || '-' }}</span>
          </div>
        </div>

        <!-- Memo -->
        <div class="drawer-section-title">{{ $t('offlineOrder.drawerSection.memo') }}</div>
        <div class="detail-grid">
          <div class="detail-item full-width">
            <span class="detail-label">{{ $t('offlineOrder.column.inventoryNote') }}:</span>
            <span class="detail-value">{{ currentRow?.inventoryNote || '-' }}</span>
          </div>
          <div class="detail-item full-width">
            <span class="detail-label">{{ $t('offlineOrder.column.memo') }}:</span>
            <span class="detail-value">{{ currentRow?.memo || '-' }}</span>
          </div>
          <div class="detail-item full-width">
            <span class="detail-label">{{ $t('offlineOrder.column.link') }}:</span>
            <span class="detail-value">{{ currentRow?.link || '-' }}</span>
          </div>
        </div>

        <!-- Audit Info -->
        <div class="drawer-section-title">{{ $t('offlineOrder.drawerSection.audit') }}</div>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.updater') }}:</span>
            <span class="detail-value">{{ currentRow?.updater || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ $t('offlineOrder.column.updatetime') }}:</span>
            <span class="detail-value">{{ formatDate(currentRow?.updatetime) }}</span>
          </div>
        </div>

        <div class="drawer-footer">
          <el-button @click="detailVisible = false">{{ $t('offlineOrder.drawerSection.close') }}</el-button>
        </div>
      </div>
    </el-drawer>

    <!-- Edit Dialog -->
    <el-dialog v-model="editVisible" :title="editTitle" width="680px" destroy-on-close>
      <el-form ref="formRef" :model="form" label-width="120px" style="padding: 0 8px">
        <!-- Basic Fields -->
        <div class="drawer-section-title">{{ $t('offlineOrder.drawerSection.basic') }}</div>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('offlineOrder.column.code')" prop="code">
              <el-input v-model="form.code" :placeholder="$t('offlineOrder.column.code')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('offlineOrder.column.subCode')" prop="subCode">
              <el-input v-model="form.subCode" :placeholder="$t('offlineOrder.column.subCode')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('offlineOrder.column.houkoku')" prop="houkoku">
              <el-input v-model="form.houkoku" :placeholder="$t('offlineOrder.column.houkoku')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('offlineOrder.column.infoFile')" prop="infoFile">
              <el-input v-model="form.infoFile" :placeholder="$t('offlineOrder.column.infoFile')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('offlineOrder.column.itemName')" prop="itemName">
              <el-input v-model="form.itemName" :placeholder="$t('offlineOrder.column.itemName')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('offlineOrder.column.volumeCount')" prop="volumeCount">
              <el-input-number v-model="form.volumeCount" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('offlineOrder.column.orderCount')" prop="orderCount">
              <el-input-number v-model="form.orderCount" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('offlineOrder.column.arrival')" prop="arrival">
              <el-select v-model="form.arrival" :placeholder="$t('offlineOrder.filter.arrival')" style="width: 100%">
                <el-option :label="$t('offlineOrder.filter.all')" value="" />
                <el-option :label="$t('offlineOrder.arrivalStatus.out')" value="OUT" />
                <el-option :label="$t('offlineOrder.arrivalStatus.notOut')" value="NOT_OUT" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('offlineOrder.column.unitCh')" prop="unitCh">
              <el-input v-model="form.unitCh" :placeholder="$t('offlineOrder.column.unitCh')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('offlineOrder.column.rate')" prop="rate">
              <el-input v-model="form.rate" :placeholder="$t('offlineOrder.column.rate')" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- Date Fields -->
        <div class="drawer-section-title">{{ $t('offlineOrder.drawerSection.date') }}</div>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('offlineOrder.column.expectedDate')" prop="expectedDate">
              <el-date-picker
                v-model="form.expectedDate"
                type="date"
                value-format="YYYY-MM-DD"
                :placeholder="$t('offlineOrder.column.expectedDate')"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('offlineOrder.column.orderDate')" prop="orderDate">
              <el-date-picker
                v-model="form.orderDate"
                type="date"
                value-format="YYYY-MM-DD"
                :placeholder="$t('offlineOrder.column.orderDate')"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- Factory Fields -->
        <div class="drawer-section-title">{{ $t('offlineOrder.drawerSection.factory') }}</div>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('offlineOrder.column.factory')" prop="factory">
              <el-input v-model="form.factory" :placeholder="$t('offlineOrder.column.factory')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('offlineOrder.column.souko')" prop="souko">
              <el-input v-model="form.souko" :placeholder="$t('offlineOrder.column.souko')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('offlineOrder.column.contactor')" prop="contactor">
              <el-input v-model="form.contactor" :placeholder="$t('offlineOrder.column.contactor')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('offlineOrder.column.contactorTel')" prop="contactorTel">
              <el-input v-model="form.contactorTel" :placeholder="$t('offlineOrder.column.contactorTel')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('offlineOrder.column.principal')" prop="principal">
              <el-input v-model="form.principal" :placeholder="$t('offlineOrder.column.principal')" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- Memo Fields -->
        <div class="drawer-section-title">{{ $t('offlineOrder.drawerSection.memo') }}</div>
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item :label="$t('offlineOrder.column.inventoryNote')" prop="inventoryNote">
              <el-input v-model="form.inventoryNote" type="textarea" :rows="2" :placeholder="$t('offlineOrder.column.inventoryNote')" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item :label="$t('offlineOrder.column.memo')" prop="memo">
              <el-input v-model="form.memo" type="textarea" :rows="2" :placeholder="$t('offlineOrder.column.memo')" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item :label="$t('offlineOrder.column.link')" prop="link">
              <el-input v-model="form.link" type="textarea" :rows="2" :placeholder="$t('offlineOrder.column.link')" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <el-button @click="editVisible = false">{{ $t('offlineOrder.dialog.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">{{ $t('offlineOrder.dialog.submit') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { offlineOrderApi } from '@/api/offline-order'
import type { OfflineOrderPageVO, CreateOfflineOrderRequest, UpdateOfflineOrderRequest } from '@/api/offline-order'
import { usePermission } from '@/composables/usePermission'

const { hasPermission } = usePermission()
const { t } = useI18n()

// Filter
const filter = reactive({
  code: '',
  itemName: '',
  factory: '',
  arrival: '' as '' | 'OUT' | 'NOT_OUT',
  page: 0,
  pageSize: 20,
})

// Table state
const loading = ref(false)
const tableData = ref<OfflineOrderPageVO[]>([])
const total = ref(0)

// Detail state
const detailVisible = ref(false)
const currentRow = ref<OfflineOrderPageVO | null>(null)

// Edit state
const editVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const isCreate = ref(true)
const editingId = ref<number | null>(null)

const blankForm = (): CreateOfflineOrderRequest => ({
  code: '',
  subCode: '',
  houkoku: '',
  infoFile: '',
  itemName: '',
  volumeCount: undefined,
  orderCount: undefined,
  arrival: undefined,
  expectedDate: '',
  orderDate: '',
  unitCh: undefined,
  rate: undefined,
  souko: '',
  factory: '',
  contactor: '',
  contactorTel: '',
  principal: '',
  memo: '',
  link: '',
  inventoryNote: undefined,
})

const form = reactive<CreateOfflineOrderRequest>(blankForm())

const editTitle = computed(() =>
  isCreate.value ? t('offlineOrder.newButton') : t('offlineOrder.editTitle')
)

const arrivalTagType = (arrival?: string): '' | 'success' | 'warning' | 'info' => {
  if (arrival === 'OUT') return 'success'
  if (arrival === 'NOT_OUT') return 'warning'
  return 'info'
}

const arrivalLabel = (arrival?: string): string => {
  if (arrival === 'OUT') return t('offlineOrder.arrivalStatus.out')
  if (arrival === 'NOT_OUT') return t('offlineOrder.arrivalStatus.notOut')
  return '-'
}

const formatDate = (d?: string): string => {
  if (!d) return '-'
  return d.replace('T', ' ').substring(0, 16)
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      code: filter.code || undefined,
      itemName: filter.itemName || undefined,
      factory: filter.factory || undefined,
      arrival: filter.arrival || undefined,
      page: filter.page,
      pageSize: filter.pageSize,
    }
    const res = await offlineOrderApi.list(params)
    tableData.value = res.data?.content ?? []
    total.value = res.data?.totalElements ?? 0
  } catch {
    ElMessage.error(t('offlineOrder.message.loadFailed'))
  } finally {
    loading.value = false
  }
}

const onReset = () => {
  filter.code = ''
  filter.itemName = ''
  filter.factory = ''
  filter.arrival = ''
  filter.page = 0
  loadData()
}

const onPageChange = (page: number) => {
  filter.page = page - 1
  loadData()
}

const onDetail = (row: OfflineOrderPageVO) => {
  currentRow.value = row
  detailVisible.value = true
}

const onCreate = () => {
  isCreate.value = true
  editingId.value = null
  Object.assign(form, blankForm())
  editVisible.value = true
}

const onEdit = (row: OfflineOrderPageVO) => {
  isCreate.value = false
  editingId.value = row.id
  Object.assign(form, {
    code: row.code ?? '',
    subCode: row.subCode ?? '',
    houkoku: row.houkoku ?? '',
    infoFile: row.infoFile ?? '',
    itemName: row.itemName ?? '',
    volumeCount: row.volumeCount,
    orderCount: row.orderCount,
    arrival: row.arrival as 'OUT' | 'NOT_OUT' | undefined,
    expectedDate: row.expectedDate ?? '',
    orderDate: row.orderDate ?? '',
    unitCh: row.unitCh ?? '',
    rate: row.rate ?? '',
    souko: row.souko ?? '',
    factory: row.factory ?? '',
    contactor: row.contactor ?? '',
    contactorTel: row.contactorTel ?? '',
    principal: row.principal ?? '',
    inventoryNote: row.inventoryNote ?? '',
    memo: row.memo ?? '',
    link: row.link ?? '',
  })
  editVisible.value = true
}

const onSubmit = async () => {
  submitting.value = true
  try {
    if (!editingId.value) {
      await offlineOrderApi.create(form as CreateOfflineOrderRequest)
      ElMessage.success(t('offlineOrder.message.createSuccess'))
    } else {
      await offlineOrderApi.update(editingId.value, form as UpdateOfflineOrderRequest)
      ElMessage.success(t('offlineOrder.message.updateSuccess'))
    }
    editVisible.value = false
    loadData()
  } catch {
    ElMessage.error(t('offlineOrder.message.saveFailed'))
  } finally {
    submitting.value = false
  }
}

const onDelete = (row: OfflineOrderPageVO) => {
  ElMessageBox.confirm(
    t('offlineOrder.deleteConfirm', { code: row.code, id: row.id }),
    t('common.delete'),
    { confirmButtonText: t('common.button.confirm'), cancelButtonText: t('common.button.cancel'), type: 'warning' }
  )
    .then(async () => {
      try {
        await offlineOrderApi.delete(row.id)
        ElMessage.success(t('offlineOrder.message.deleteSuccess'))
        loadData()
      } catch {
        ElMessage.error(t('offlineOrder.message.deleteFailed'))
      }
    })
    .catch(() => {})
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.page {
  padding: 16px;
}

.filter-card {
  margin-bottom: 12px;
}

.table-card {
  margin-bottom: 12px;
}

.pagination-bar {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.drawer-content {
  padding: 0 20px;
}

.drawer-section-title {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin: 16px 0 8px;
  padding-bottom: 6px;
  border-bottom: 1px solid #ebeef5;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.detail-item {
  display: flex;
  gap: 8px;
  font-size: 13px;
}

.detail-item.full-width {
  grid-column: 1 / -1;
}

.detail-label {
  color: #909399;
  flex-shrink: 0;
}

.detail-value {
  color: #303133;
  word-break: break-all;
}

.drawer-footer {
  margin-top: 24px;
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}
</style>
