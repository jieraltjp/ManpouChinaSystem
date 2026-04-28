<template>
  <div class="page">
    <!-- 统计 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#E8650A"><Clock /></el-icon></div>
            <div>
              <div class="stat-value">{{ pagination.total }}</div>
              <div class="stat-label">{{ $t('demand.stat.total') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#D97706"><Warning /></el-icon></div>
            <div>
              <div class="stat-value">{{ pendingCount }}</div>
              <div class="stat-label">{{ $t('demand.stat.pending') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#16A34A"><CircleCheck /></el-icon></div>
            <div>
              <div class="stat-value">{{ confirmedCount }}</div>
              <div class="stat-label">{{ $t('demand.stat.confirmed') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选 -->
    <el-card class="filter-card" shadow="never">
      <template #header>
        <div class="filter-header">
          <el-form :inline="true" :model="filterForm" @keyup.enter="loadData">
            <el-form-item :label="$t('demand.filter.demandType')">
              <el-select v-model="filterForm.demandType" :placeholder="$t('demand.filter.all')" clearable style="width:140px">
                <el-option value="REPLENISHMENT" :label="$t('demand.type.replenishment')" />
                <el-option value="NEW_PURCHASE" :label="$t('demand.type.newPurchase')" />
              </el-select>
            </el-form-item>
            <el-form-item :label="$t('demand.filter.productCode')">
              <el-input v-model="filterForm.productCode" :placeholder="$t('demand.dialog.productCodePlaceholder')" clearable style="width:140px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadData">{{ $t('demand.filter.search') }}</el-button>
              <el-button @click="onReset">{{ $t('demand.filter.reset') }}</el-button>
            </el-form-item>
          </el-form>
          <el-button type="primary" @click="onNew">
            <el-icon><Plus /></el-icon> {{ $t('demand.newButton') }}
          </el-button>
        </div>
      </template>
    </el-card>

    <!-- 表格（v2.0.0：一行 = 一个子货号） -->
    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="tableData" stripe style="width:100%" min-height="200">
        <el-table-column prop="demandCode" :label="$t('demand.column.demandCode')" min-width="160" />
        <el-table-column prop="productCode" :label="$t('demand.column.productCode')" min-width="110">
          <template #default="{ row }">
            <span class="product-code">{{ row.productCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="subProductCode" :label="$t('demand.column.subProductCode')" min-width="140">
          <template #default="{ row }">
            <span class="product-code">{{ row.subProductCode }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('demand.column.imageUrl')" width="80" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.imageUrl"
              :src="row.imageUrl"
              fit="cover"
              style="width:48px;height:48px;border-radius:4px;cursor:pointer"
              :preview-src-list="[row.imageUrl]"
              preview-teleported
            />
            <span v-else style="color:#ccc;font-size:12px">—</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('demand.column.category')" min-width="100" align="center">
          <template #default="{ row }">
            {{ getCategoryLabel(row.productCode) }}
          </template>
        </el-table-column>
        <el-table-column prop="demandType" :label="$t('demand.column.demandType')" min-width="130" align="center">
          <template #default="{ row }">
            <el-tag :type="row.demandType === 'NEW_PURCHASE' ? 'warning' : 'primary'" size="small">
              {{ row.demandType === 'NEW_PURCHASE' ? $t('demand.type.newPurchase') : $t('demand.type.replenishment') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="quantity" :label="$t('demand.column.quantity')" min-width="90" align="right">
          <template #default="{ row }">
            <span class="qty-value">{{ row.quantity }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="destination" :label="$t('demand.column.destination')" min-width="110">
          <template #default="{ row }">
            <span class="dest-value">{{ row.destination || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="japanLead" :label="$t('demand.column.japanLead')" min-width="100" />
        <el-table-column prop="remarks" :label="$t('demand.column.remarks')" min-width="120" show-overflow-tooltip />
        <el-table-column prop="status" :label="$t('demand.column.status')" min-width="100" align="center">
          <template #default="{ row }">
            <el-tag
              :type="demandStatusType(row)"
              size="small"
              :disable-transitions="false"
              class="status-toggle"
              @click.stop="onToggleStatus(row)"
            >
              {{ demandStatusLabel(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('demand.column.action')" min-width="260" align="center">
          <template #default="{ row }">
            <el-button link class="btn-blue" size="small" @click.stop="onView(row)">{{ $t('common.view') }}</el-button>
            <el-button link type="warning" size="small" @click.stop="onEdit(row)">{{ $t('demand.action.edit') }}</el-button>
            <el-button link type="danger" size="small" @click.stop="onDelete(row)">{{ $t('common.delete') }}</el-button>
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
    <el-drawer v-model="drawerVisible" :title="$t('demand.drawerTitle')" size="560px" direction="rtl">
      <el-descriptions :column="2" border v-if="currentRow">
        <el-descriptions-item :label="$t('demand.column.demandCode')">{{ currentRow.demandCode }}</el-descriptions-item>
        <el-descriptions-item :label="$t('demand.column.demandType')">{{ demandTypeLabel(currentRow.demandType) }}</el-descriptions-item>
        <el-descriptions-item :label="$t('demand.column.productCode')">{{ currentRow.productCode }}</el-descriptions-item>
        <el-descriptions-item :label="$t('demand.column.subProductCode')">{{ currentRow.subProductCode || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('demand.column.imageUrl')" :span="2">
          <el-image
            v-if="currentRow.imageUrl"
            :src="currentRow.imageUrl"
            fit="contain"
            style="max-width:200px;max-height:200px;border-radius:4px"
            :preview-src-list="[currentRow.imageUrl]"
            preview-teleported
          />
          <span v-else style="color:#ccc">—</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('demand.column.category')">{{ getCategoryLabel(currentRow.productCode) }}</el-descriptions-item>
        <el-descriptions-item :label="$t('demand.column.quantity')">{{ currentRow.quantity }}</el-descriptions-item>
        <el-descriptions-item :label="$t('demand.column.destination')">{{ currentRow.destination || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('demand.column.japanLead')">{{ currentRow.japanLead || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('demand.column.status')">{{ demandStatusLabel(currentRow) }}</el-descriptions-item>
        <el-descriptions-item :label="$t('demand.dialog.remarks')" :span="2">{{ currentRow.remarks || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('demand.column.createTime')">{{ currentRow.createTime ? new Date(currentRow.createTime).toLocaleString(currentLocale === 'ja' ? 'ja-JP' : 'zh-CN') : '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('demand.column.createBy')">{{ currentRow.createBy || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div class="drawer-footer">
        <el-button @click="drawerVisible = false">{{ $t('common.close') }}</el-button>
        <el-button type="primary" @click="drawerVisible = false; currentRow && onEdit(currentRow)">{{ $t('demand.action.edit') }}</el-button>
      </div>
    </el-drawer>

    <!-- 新建/编辑弹窗（v2.1.0：单个录入 + 批量录入 两种模式） -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? $t('demand.newDialogTitle') : $t('demand.editDialogTitle')"
      width="680px"
      class="demand-dialog"
    >
      <!-- 模式切换（批量功能开发中） -->
      <div class="dialog-mode-tabs">
        <button
          class="mode-tab"
          :class="{ 'mode-tab--active': entryMode === 'single' }"
          @click="entryMode = 'single'"
          type="button"
        >{{ $t('demand.dialog.entryMode.single') }}</button>
        <button
          class="mode-tab"
          :class="{ 'mode-tab--active': entryMode === 'batch' }"
          @click="entryMode = 'batch'"
          type="button"
        >{{ $t('demand.dialog.entryMode.batch') }}</button>
      </div>

      <!-- ========== 单个录入模式 ========== -->
      <el-form
        v-if="entryMode === 'single'"
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="120px"
        class="demand-form"
      >
        <!-- 【基本信息】 -->
        <div class="form-section">
          <div class="form-section__title">{{ $t('demand.dialog.section.basic') }}</div>
          <el-form-item :label="$t('demand.dialog.demandType')" prop="demandType">
            <el-radio-group v-model="formData.demandType">
              <el-radio value="REPLENISHMENT">{{ $t('demand.type.replenishment') }}</el-radio>
              <el-radio value="NEW_PURCHASE">{{ $t('demand.type.newPurchase') }}</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item :label="$t('demand.dialog.productCode')" prop="productCode">
            <el-select
              v-model="formData.productCode"
              filterable
              remote
              reserve-keyword
              :remote-method="searchMasterCode"
              :loading="masterCodeLoading"
              :placeholder="$t('demand.dialog.productCodePlaceholder')"
              style="width:100%"
              @change="onMasterCodeChange"
            >
              <el-option
                v-for="item in masterCodeOptions"
                :key="item.masterCode"
                 :label="item.masterCode"
                :value="item.masterCode"
              >
                <span style="font-weight:600">{{ item.masterCode }}</span>
                <span v-if="item.nameZh" style="color:#999;font-size:12px;margin-left:8px">{{ item.nameZh }}</span>
                <span v-if="item.colorCount > 0" style="color:#E8650A;font-size:11px;float:right">{{ $t('demand.dialog.colorCount', { count: item.colorCount }) }}</span>
              </el-option>
            </el-select>
          </el-form-item>
        </div>

        <!-- 【需求明细】：Grid 布局，每个单元格 = label + control -->
        <div class="form-section">
          <div class="form-section__title">{{ $t('demand.dialog.section.detail') }}</div>
          <div class="detail-grid">
            <!-- 子货号（颜色） -->
            <div class="detail-cell">
              <label class="detail-label">{{ $t('demand.dialog.subProductCode') }}</label>
              <el-select
                v-model="formData.subProductCode"
                filterable
                :disabled="!formData.productCode"
                :placeholder="$t('demand.dialog.selectSubCode')"
                style="width:100%"
                @focus="loadSubCodeOptions"
              >
                <el-option
                  v-for="opt in subCodeOptions"
                  :key="opt.subCode"
                  :label="(opt.colorName || opt.subCode) + ' (' + opt.subCode + ')'"
                  :value="opt.subCode"
                />
              </el-select>
            </div>
            <!-- 需求量 -->
            <div class="detail-cell detail-cell--qty">
              <label class="detail-label">{{ $t('demand.dialog.quantity') }}</label>
              <el-input-number
                v-model="formData.quantity"
                :min="1"
                :max="999999"
                :step="100"
                controls-position="right"
                style="width:100%"
              />
            </div>
            <!-- 目的地 -->
            <div class="detail-cell">
              <label class="detail-label">{{ $t('demand.dialog.destination') }}</label>
              <el-select
                v-model="formData.destination"
                filterable
                remote
                reserve-keyword
                :remote-method="searchDestination"
                :loading="destLoading"
                :placeholder="$t('demand.dialog.destinationPlaceholder')"
                allow-create
                default-first-option
                style="width:100%"
                clearable
              >
                <el-option
                  v-for="d in destOptions"
                  :key="d"
                  :label="d"
                  :value="d"
                />
              </el-select>
            </div>
          </div>
        </div>

        <!-- 【补充信息】 -->
        <div class="form-section">
          <div class="form-section__title">{{ $t('demand.dialog.section.extra') }}</div>
          <div class="extra-grid">
            <div class="extra-cell">
              <label class="detail-label">{{ $t('demand.dialog.japanLead') }}</label>
              <el-select
                v-model="formData.japanLead"
                filterable
                remote
                reserve-keyword
                :remote-method="searchJapanLead"
                :loading="leadLoading"
                :placeholder="$t('demand.dialog.japanLeadPlaceholder')"
                allow-create
                default-first-option
                style="width:100%"
                clearable
              >
                <el-option
                  v-for="l in leadOptions"
                  :key="l"
                  :label="l"
                  :value="l"
                />
              </el-select>
            </div>
            <div class="extra-cell extra-cell--remarks">
              <label class="detail-label">{{ $t('demand.dialog.remarks') }}</label>
              <el-input v-model="formData.remarks" :placeholder="$t('demand.dialog.remarksPlaceholder')" />
            </div>
          </div>
        </div>
      </el-form>

      <!-- ========== 批量录入模式 ========== -->
      <div v-else class="batch-mode">
        <p class="batch-hint">{{ $t('demand.dialog.batchHint') }}</p>
        <p class="batch-hint batch-hint--muted">（批量录入功能开发中）</p>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('demand.dialog.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">{{ $t('demand.dialog.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 查看关联采购单弹窗（v2.0.0：单条） -->
    <el-dialog v-model="linkedDialogVisible" :title="$t('demand.dialog.linkedDialog.title')" width="600px">
      <el-table :data="linkedProcurement ? [linkedProcurement] : []" border size="small" v-loading="linkedLoading">
        <el-table-column prop="id" :label="$t('demand.linkedDialog.column.id')" width="70" />
        <el-table-column prop="factoryName" :label="$t('demand.linkedDialog.column.factoryName')" min-width="120" />
        <el-table-column prop="productCode" :label="$t('demand.linkedDialog.column.productCode')" width="100" />
        <el-table-column prop="subProductCode" :label="$t('demand.linkedDialog.column.subProductCode')" width="110" />
        <el-table-column prop="quantity" :label="$t('demand.linkedDialog.column.quantity')" width="70" align="right" />
        <el-table-column prop="destination" :label="$t('demand.linkedDialog.column.destination')" min-width="100" show-overflow-tooltip />
        <el-table-column prop="status" :label="$t('demand.linkedDialog.column.status')" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!linkedLoading && !linkedProcurement" style="text-align:center;color:#999;padding:20px">
        {{ $t('demand.linkedDialog.noData') }}
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, type FormInstance, ElMessageBox } from 'element-plus'
import { Plus, Clock, Warning, CircleCheck } from '@element-plus/icons-vue'
import { demandApi, type DemandPageVO, type DemandType } from '@/api/demand'
import { productApi, type MasterCodeSuggestVO, type SubCodeSuggestVO } from '@/api/product'
import { useI18n } from 'vue-i18n'

const loading = ref(false)
const submitting = ref(false)
const masterCodeLoading = ref(false)
const subCodeLoading = ref(false)
const linkedLoading = ref(false)
const masterCodeRaw = ref<MasterCodeSuggestVO[]>([])
const masterCodeOptions = computed<MasterCodeSuggestVO[]>(() =>
  Array.from(new Map(masterCodeRaw.value.map(i => [i.masterCode, i])).values())
)
const subCodeOptions = ref<SubCodeSuggestVO[]>([])
const destOptions = ref<string[]>([])
const destLoading = ref(false)
const leadOptions = ref<string[]>([])
const leadLoading = ref(false)
const linkedProcurement = ref<unknown | null>(null)
const loadedSubCodesFor = ref<string>('')
const dialogVisible = ref(false)
const drawerVisible = ref(false)
const linkedDialogVisible = ref(false)
const dialogMode = ref<'create' | 'update'>('create')
const entryMode = ref<'single' | 'batch'>('single')
const currentRow = ref<DemandPageVO | null>(null)
const formRef = ref<FormInstance>()

const { t, locale: localeRef } = useI18n()
const currentLocale = computed(() => localeRef.value)

const filterForm = reactive({ demandType: '', productCode: '' })
const pagination = reactive({ page: 1, pageSize: 20, total: 0 })
const tableData = ref<DemandPageVO[]>([])
const productCategoryMap = ref<Record<string, string>>({}) // productCode -> category

function getCategoryLabel(code: string): string {
  if (!code) return '-'
  const category = productCategoryMap.value[code]
  if (!category || category === '-') return '-'
  return t('product.category.' + category) ?? category
}

async function fetchProductCategories(rows: DemandPageVO[]) {
  const codes = [...new Set(rows.map(r => r.productCode).filter(Boolean))]
  if (!codes.length) return
  const results = await Promise.allSettled(
    codes.map(code => productApi.getByCode(code))
  )
  const map: Record<string, string> = {}
  results.forEach((r, i) => {
    if (r.status === 'fulfilled') {
      map[codes[i]] = r.value.data.data?.category || '-'
    } else {
      console.warn('[DemandPage] fetch category failed for', codes[i], r.reason)
    }
  })
  productCategoryMap.value = { ...productCategoryMap.value, ...map }
}

const pendingCount = computed(() => tableData.value.filter(r => !r.linkedProcurementId).length)
const confirmedCount = computed(() => tableData.value.filter(r => r.linkedProcurementId).length)

const formData = reactive<{
  demandType: DemandType
  productCode: string
  subProductCode: string
  quantity: number | undefined
  destination: string
  japanLead: string
  remarks: string
}>({
  demandType: 'REPLENISHMENT',
  productCode: '',
  subProductCode: '',
  quantity: 100,
  destination: '',
  japanLead: '',
  remarks: '',
})

const formRules = {
  demandType: [{ required: true, message: () => t('demand.validation.demandTypeRequired'), trigger: 'change' }],
  productCode: [{ required: true, message: () => t('demand.validation.productCodeRequired'), trigger: 'blur' }],
  subProductCode: [{ required: true, message: () => t('demand.validation.subProductCodeRequired'), trigger: 'blur' }],
  quantity: [{ required: true, message: () => t('demand.validation.quantityRequired'), trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    const res = await demandApi.list({
      page: pagination.page - 1,
      pageSize: pagination.pageSize,
      demandType: filterForm.demandType || undefined,
      productCode: filterForm.productCode.trim() || undefined,
    })
    const payload = res.data.data as { content: DemandPageVO[]; totalElements: number }
    tableData.value = payload.content || []
    pagination.total = payload.totalElements || 0
    fetchProductCategories(tableData.value)
  } catch { /* interceptor */ } finally {
    loading.value = false
  }
}

function onReset() {
  filterForm.demandType = ''
  filterForm.productCode = ''
  pagination.page = 1
  loadData()
}

function onNew() {
  dialogMode.value = 'create'
  entryMode.value = 'single'
  Object.assign(formData, {
    demandType: 'REPLENISHMENT',
    productCode: '',
    subProductCode: '',
    quantity: 100,
    destination: '',
    japanLead: '',
    remarks: '',
  })
  subCodeOptions.value = []
  loadedSubCodesFor.value = ''
  dialogVisible.value = true
}

function onEdit(row: DemandPageVO) {
  dialogMode.value = 'update'
  currentRow.value = row
  Object.assign(formData, {
    demandType: row.demandType,
    productCode: row.productCode,
    subProductCode: row.subProductCode || '',
    quantity: row.quantity ?? 1,
    destination: row.destination || '',
    japanLead: row.japanLead || '',
    remarks: row.remarks || '',
  })
  loadedSubCodesFor.value = row.productCode
  subCodeOptions.value = []
  dialogVisible.value = true
  loadSubCodeOptions()
}

function onView(row: DemandPageVO) {
  currentRow.value = row
  drawerVisible.value = true
}

let masterCodeTimer: ReturnType<typeof setTimeout>
async function searchMasterCode(query: string) {
  clearTimeout(masterCodeTimer)
  if (!query || query.length < 1) { masterCodeRaw.value = []; return }
  masterCodeTimer = setTimeout(async () => {
    masterCodeLoading.value = true
    try {
      const res = await productApi.suggestMasterCodes(query)
      masterCodeRaw.value = res.data.data || []
    } catch (e) { masterCodeRaw.value = [] } finally { masterCodeLoading.value = false }
  }, 300)
}

function onMasterCodeChange(val: string) {
  formData.productCode = val
  formData.subProductCode = ''
  subCodeOptions.value = []
  loadedSubCodesFor.value = ''
  if (val) loadSubCodeOptions()
}

async function loadSubCodeOptions() {
  const masterCode = formData.productCode
  if (!masterCode) return
  // 只有缓存命中（masterCode 没变且已有选项）才跳过
  if (loadedSubCodesFor.value === masterCode && subCodeOptions.value.length > 0) return
  loadedSubCodesFor.value = masterCode
  subCodeLoading.value = true
  try {
    const res = await productApi.suggestSubCodes(masterCode)
    subCodeOptions.value = res.data.data || []
    // 自动代入第一个子货号
    if (!formData.subProductCode && subCodeOptions.value.length > 0) {
      formData.subProductCode = subCodeOptions.value[0].subCode
    }
  } catch (e) { subCodeOptions.value = [] } finally { subCodeLoading.value = false }
}

async function searchDestination(query: string) {
  destLoading.value = true
  try {
    const res = await demandApi.suggestDestinations()
    const all = res.data.data || []
    destOptions.value = query
      ? all.filter((d: string) => d.toLowerCase().includes(query.toLowerCase()))
      : all
  } catch { destOptions.value = [] }
  finally { destLoading.value = false }
}

async function searchJapanLead(query: string) {
  leadLoading.value = true
  try {
    const res = await demandApi.suggestJapanLeads()
    const all = res.data.data || []
    leadOptions.value = query
      ? all.filter((l: string) => l.toLowerCase().includes(query.toLowerCase()))
      : all
  } catch { leadOptions.value = [] }
  finally { leadLoading.value = false }
}

async function onDelete(row: DemandPageVO) {
  try {
    await ElMessageBox.confirm(
      t('demand.message.deleteConfirm', { code: row.demandCode }),
      t('demand.message.deleteConfirmTitle'),
      { type: 'warning' },
    )
  } catch { return }
  try {
    await demandApi.delete(row.id)
    ElMessage.success(t('demand.message.deleteSuccess'))
    loadData()
  } catch { /* interceptor */ }
}

async function onSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    submitting.value = true
    try {
      const payload = {
        demandType: formData.demandType,
        productCode: formData.productCode,
        subProductCode: formData.subProductCode,
        quantity: formData.quantity ?? 1,
        destination: formData.destination || undefined,
        japanLead: formData.japanLead || undefined,
        remarks: formData.remarks || undefined,
      }
      if (dialogMode.value === 'create') {
        await demandApi.create(payload)
        ElMessage.success(t('demand.message.createSuccess'))
      } else if (currentRow.value) {
        await demandApi.update(currentRow.value.id, payload)
        ElMessage.success(t('demand.message.updateSuccess'))
      }
      dialogVisible.value = false
      loadData()
    } finally { submitting.value = false }
  })
}

function demandStatusLabel(row: DemandPageVO): string {
  // 有 linkedProcurementId 即显示"已确认"（v2.2.0）
  if (row.linkedProcurementId) return t('demand.status.CONFIRMED')
  return t('demand.status.PENDING')
}

function demandTypeLabel(type: string): string {
  return { REPLENISHMENT: t('demand.type.replenishment'), NEW_PURCHASE: t('demand.type.newPurchase') }[type] ?? type
}

function demandStatusType(row: DemandPageVO): string {
  // 有 linkedProcurementId → success（绿）；否则 warning（橙）（v2.2.0）
  return row.linkedProcurementId ? 'success' : 'warning'
}

async function onToggleStatus(row: DemandPageVO) {
  if (row.linkedProcurementId) {
    // 已确认 → 点击取消关联（v2.2.0）
    try {
      await demandApi.unlink(row.id)
      ElMessage.success(t('demand.message.unlinkedSuccess'))
      loadData()
    } catch { /* interceptor handles error */ }
  } else {
    // 待确认 → 提示去发注单页面关联（v2.2.0）
    ElMessage.info(t('demand.message.pleaseLinkInProcurement'))
  }
}

onMounted(() => loadData())
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.page-header { display: flex; align-items: center; justify-content: space-between; }
.filter-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.filter-card :deep(.el-card__body) { padding-bottom: 0; }
.table-card :deep(.el-card__body) { padding: 16px; }
.stats-row { margin-bottom: 4px; }
.stat-card { border-radius: var(--radius-md); border: 1px solid var(--border-color); box-shadow: var(--shadow-card); position: relative; overflow: hidden; transition: all var(--transition-fast); }
.stat-card::before { content: ''; position: absolute; top: 0; left: 0; right: 0; height: 3px; background: linear-gradient(90deg, var(--color-primary), var(--color-primary-light)); border-radius: var(--radius-md) var(--radius-md) 0 0; }
.stat-card:hover { box-shadow: var(--shadow-md); transform: translateY(-2px); }
.stat-content { display: flex; align-items: center; gap: 14px; }
.stat-icon-wrap { width: 48px; height: 48px; border-radius: 50%; background: var(--color-primary-pale); display: flex; align-items: center; justify-content: center; }
.stat-icon { font-size: 22px; }
.stat-value { font-size: 26px; font-weight: 800; color: var(--text-primary); line-height: 1; font-variant-numeric: tabular-nums; }
.stat-label { font-size: 13px; color: var(--text-secondary); margin-top: 4px; }
.product-code { color: var(--color-primary); font-family: monospace; font-size: 12px; font-weight: 700; }
.btn-blue { color: #409EFF !important; }
.qty-value { color: #D97706; font-weight: 600; }
.dest-value { color: #6B7280; }
.status-toggle { cursor: pointer; }
.status-toggle:hover { opacity: 0.8; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
.drawer-footer { padding: 16px 0 0; border-top: 1px solid var(--border-color); margin-top: 16px; display: flex; gap: 8px; }

/* v2.1.0 表单布局 */
:deep(.demand-dialog .el-dialog__body) { padding-top: 16px; }
:deep(.demand-form .el-form-item) { margin-bottom: 14px; }
:deep(.demand-form .el-form-item:last-child) { margin-bottom: 0; }

/* 模式切换标签 */
.dialog-mode-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 20px;
  background: #f5f7fa;
  border-radius: 8px;
  padding: 4px;
  width: fit-content;
}
.mode-tab {
  padding: 6px 20px;
  border: none;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  color: #606266;
  transition: all 0.2s;
}
.mode-tab:hover { color: #409eff; }
.mode-tab--active {
  background: #fff;
  color: #409eff;
  font-weight: 600;
  box-shadow: 0 1px 4px rgba(0,0,0,0.1);
}

/* 分组 */
.form-section { margin-bottom: 18px; }
.form-section:last-child { margin-bottom: 0; }
.form-section__title {
  font-size: 11px;
  font-weight: 700;
  color: #909399;
  text-transform: uppercase;
  letter-spacing: 0.8px;
  margin-bottom: 10px;
  padding-bottom: 6px;
  border-bottom: 1px solid #f0f0f0;
}

/* 需求明细：Grid 布局，不依赖 el-form-item 的 block/flex 行为 */
.detail-grid {
  display: grid;
  grid-template-columns: 2fr 140px 1.6fr;
  gap: 12px;
  align-items: start;
}
.detail-cell { display: flex; flex-direction: column; gap: 4px; }
.detail-cell--qty .el-input-number { width: 100%; }
.detail-label {
  font-size: 13px;
  color: #606266;
  line-height: 1.4;
  white-space: nowrap;
}

/* 补充信息：Grid 布局 */
.extra-grid {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 12px;
  align-items: start;
}
.extra-cell { display: flex; flex-direction: column; gap: 4px; }

/* 批量模式提示 */
.batch-mode { padding: 24px 0; text-align: center; }
.batch-hint { color: #606266; font-size: 14px; margin: 0 0 4px; }
.batch-hint--muted { color: #c0c4cc; font-size: 12px; }
</style>
