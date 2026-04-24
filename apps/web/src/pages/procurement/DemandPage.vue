<template>
  <div class="page">
    <div class="page-header">
      <h2 class="page-title">{{ $t('demand.title') }}</h2>
      <div class="header-actions">
        <el-button type="primary" @click="onNew">
          <el-icon><Plus /></el-icon> {{ $t('demand.newButton') }}
        </el-button>
      </div>
    </div>

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
              <div class="stat-value">{{ convertedCount }}</div>
              <div class="stat-label">{{ $t('demand.stat.converted') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filterForm">
        <el-form-item :label="$t('demand.filter.demandType')">
          <el-select v-model="filterForm.demandType" :placeholder="$t('demand.filter.all')" clearable style="width:140px">
            <el-option value="REPLENISHMENT" :label="$t('demand.type.replenishment')" />
            <el-option value="NEW_PURCHASE" :label="$t('demand.type.newPurchase')" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('demand.filter.productCode')">
          <el-input v-model="filterForm.productCode" :placeholder="$t('demand.dialog.productCodePlaceholder')" clearable style="width:140px" />
        </el-form-item>
        <el-form-item :label="$t('demand.filter.status')">
          <el-select v-model="filterForm.status" :placeholder="$t('demand.filter.all')" clearable style="width:140px">
            <el-option value="PENDING" :label="$t('demand.status.PENDING')" />
            <el-option value="CONVERTED" :label="$t('demand.status.CONVERTED')" />
            <el-option value="CANCELLED" :label="$t('demand.status.CANCELLED')" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">{{ $t('demand.filter.search') }}</el-button>
          <el-button @click="onReset">{{ $t('demand.filter.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="tableData" stripe style="width:100%">
        <el-table-column prop="demandCode" :label="$t('demand.column.demandCode')" width="160" />
        <el-table-column prop="demandType" :label="$t('demand.column.demandType')" width="130" align="center">
          <template #default="{ row }">
            <el-tag :type="row.demandType === 'NEW_PURCHASE' ? 'warning' : 'primary'" size="small">
              {{ row.demandType === 'NEW_PURCHASE' ? $t('demand.type.newPurchase') : $t('demand.type.replenishment') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="productCode" :label="$t('demand.column.productCode')" width="120">
          <template #default="{ row }">
            <span class="product-code">{{ row.productCode }}</span>
          </template>
        </el-table-column>
        <!-- v1.6.0: 子货号明细列 -->
        <el-table-column :label="$t('demand.column.subProductItems')" min-width="220">
          <template #default="{ row }">
            <span v-if="!row.subProductItems || row.subProductItems.length === 0">—</span>
            <span v-else v-for="(item, idx) in row.subProductItems" :key="idx" class="sub-item-chip">
              <el-tag size="small" type="info" style="margin-right:4px">{{ item.subCode }}</el-tag>
              <span class="sub-item-detail">{{ item.quantity }}台</span>
              <span v-if="item.destination" class="sub-item-dest"> → {{ item.destination }}</span>
              <span v-if="idx < row.subProductItems.length - 1" style="margin-right:6px">,</span>
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="japanLead" :label="$t('demand.column.japanLead')" width="100" />
        <el-table-column prop="status" :label="$t('demand.column.status')" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="demandStatusType(row.status)" size="small">
              {{ demandStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="$t('demand.column.createTime')" width="160">
          <template #default="{ row }">
            {{ row.createTime ? new Date(row.createTime).toLocaleString(currentLocale === 'ja' ? 'ja-JP' : 'zh-CN') : '-' }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('demand.column.action')" width="260" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" link type="primary" size="small" @click.stop="onConvert(row)">
              {{ $t('demand.action.convert') }}
            </el-button>
            <template v-if="row.status === 'CONVERTED'">
              <el-button link type="primary" size="small" @click.stop="onViewLinked(row)">
                {{ $t('demand.action.viewLinked') }}
              </el-button>
              <el-button link type="warning" size="small" @click.stop="onRevertConversion(row)">
                {{ $t('demand.action.revert') }}
              </el-button>
            </template>
            <el-button link type="primary" size="small" @click.stop="onEdit(row)">{{ $t('demand.action.edit') }}</el-button>
            <el-button v-if="row.status === 'PENDING'" link type="danger" size="small" @click.stop="onDelete(row)">
              {{ $t('demand.action.delete') }}
            </el-button>
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

    <!-- 新建/编辑弹窗（v1.6.0：子货号明细 2列卡片网格） -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? $t('demand.newDialogTitle') : $t('demand.editDialogTitle')"
      width="660px"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px" :class="'demand-form'">
        <!-- 第一行：类型 + 主货号 -->
        <div class="form-row form-row--2col">
          <el-form-item :label="$t('demand.dialog.demandType')" prop="demandType" class="form-item--shrink">
            <el-radio-group v-model="formData.demandType">
              <el-radio value="REPLENISHMENT">{{ $t('demand.type.replenishment') }}</el-radio>
              <el-radio value="NEW_PURCHASE">{{ $t('demand.type.newPurchase') }}</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item :label="$t('demand.dialog.productCode')" prop="productCode" class="form-item--fill">
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
                :label="item.masterCode + (item.nameZh ? ' — ' + item.nameZh : '')"
                :value="item.masterCode"
              >
                <span style="font-weight:600">{{ item.masterCode }}</span>
                <span v-if="item.nameZh" style="color:#999;font-size:12px;margin-left:8px">{{ item.nameZh }}</span>
                <span v-if="item.colorCount > 0" style="color:#E8650A;font-size:11px;float:right">{{ $t('demand.dialog.colorCount', { count: item.colorCount }) }}</span>
              </el-option>
            </el-select>
          </el-form-item>
        </div>

        <!-- 子货号明细区 -->
        <div class="sic-section">
          <div class="sic-header">
            <span class="sic-title">{{ $t('demand.dialog.subProductItems') }}</span>
            <span class="sic-count" v-if="formData.subProductItems.length > 0">
              {{ formData.subProductItems.length }} 个子货号
            </span>
          </div>

          <!-- 2列卡片网格 -->
          <div class="sic-grid" v-if="formData.subProductItems.length > 0">
            <div
              v-for="(item, $index) in formData.subProductItems"
              :key="$index"
              class="sic-card"
            >
              <!-- 卡片头部：子货号选择 + 删除 -->
              <div class="sic-card-head">
                <el-select
                  v-model="item.subCode"
                  filterable
                  allow-create
                  default-first-option
                  :disabled="!formData.productCode && dialogMode === 'create'"
                  :placeholder="$t('demand.dialog.selectColorVariant')"
                  size="small"
                  style="width:100%;flex:1"
                  @focus="loadSubCodeOptions($index)"
                >
                  <el-option
                    v-for="opt in subCodeOptions[$index] || []"
                    :key="opt.subCode"
                    :label="(opt.colorName || opt.subCode) + ' (' + opt.subCode + ')'"
                    :value="opt.subCode"
                  />
                </el-select>
                <el-button
                  link
                  type="danger"
                  size="small"
                  :disabled="formData.subProductItems.length <= 1"
                  @click="removeSubProductItem($index)"
                  style="flex-shrink:0;padding:2px 4px;margin-left:4px"
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>

              <!-- 卡片体：数量 + 目的地 -->
              <div class="sic-card-body">
                <div class="sic-field">
                  <label class="sic-field__label">{{ $t('demand.dialog.col.quantity') }}</label>
                  <el-input-number
                    v-model="item.quantity"
                    :min="0"
                    :max="99999"
                    size="small"
                    controls-position="right"
                    style="width:100%"
                  />
                </div>
                <div class="sic-field">
                  <label class="sic-field__label">{{ $t('demand.dialog.col.destination') }}</label>
                  <el-input
                    v-model="item.destination"
                    :placeholder="$t('demand.dialog.destinationPlaceholder')"
                    size="small"
                    style="width:100%"
                  />
                </div>
              </div>
            </div>
          </div>

          <div v-if="formData.subProductItems.length === 0" class="sic-empty">
            {{ $t('demand.validation.subProductItemsRequired') }}
          </div>

          <el-button size="small" class="sic-add-btn" @click="addSubProductItem">
            <el-icon><Plus /></el-icon> {{ $t('demand.dialog.addSubItem') }}
          </el-button>
        </div>

        <!-- 第二行：日本担当 + 备注 -->
        <div class="form-row form-row--2col">
          <el-form-item :label="$t('demand.dialog.japanLead')" class="form-item--fill">
            <el-input v-model="formData.japanLead" :placeholder="$t('demand.dialog.japanLeadPlaceholder')" />
          </el-form-item>
          <el-form-item :label="$t('demand.dialog.remarks')" class="form-item--fill">
            <el-input v-model="formData.remarks" :placeholder="$t('demand.dialog.remarksPlaceholder')" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('demand.dialog.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">{{ $t('demand.dialog.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 转采购弹窗（v1.6.0：选择工厂） -->
    <el-dialog v-model="convertDialogVisible" :title="$t('demand.dialog.convertDialog.title')" width="500px">
      <el-form label-width="100px">
        <el-form-item :label="$t('demand.dialog.convertDialog.factory')" required>
          <el-select
            v-model="convertForm.factoryId"
            filterable
            remote
            reserve-keyword
            :remote-method="searchFactory"
            :loading="factoryLoading"
            :placeholder="$t('demand.dialog.convertDialog.factoryPlaceholder')"
            style="width:100%"
          >
            <el-option
              v-for="f in factoryOptions"
              :key="f.id"
              :label="f.factoryName + ' (' + f.factoryCode + ')'"
              :value="f.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('demand.dialog.convertDialog.preview')">
          <div v-if="convertForm.demand" style="font-size:13px;color:#666">
            <div><b>{{ convertForm.demand.productCode }}</b></div>
            <div v-for="(item, i) in (convertForm.demand.subProductItems || [])" :key="i" style="margin-top:2px">
              {{ item.subCode }} × {{ item.quantity }}
              <span v-if="item.destination"> → {{ item.destination }}</span>
            </div>
            <div style="margin-top:4px;color:#D97706">
              {{ $t('demand.dialog.convertDialog.willCreate', { n: (convertForm.demand.subProductItems || []).length }) }}
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="convertDialogVisible = false">{{ $t('demand.dialog.cancel') }}</el-button>
        <el-button type="primary" :loading="converting" @click="doConvert">
          {{ $t('demand.dialog.convertDialog.confirm') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 查看关联采购单弹窗（v1.6.0） -->
    <el-dialog v-model="linkedDialogVisible" :title="$t('demand.dialog.linkedDialog.title')" width="600px">
      <el-table :data="linkedProcurements" border size="small" v-loading="linkedLoading">
        <el-table-column prop="id" :label="$t('demand.linkedDialog.column.id')" width="70" />
        <el-table-column prop="factoryName" :label="$t('demand.linkedDialog.column.factoryName')" min-width="120" />
        <el-table-column prop="productCode" :label="$t('demand.linkedDialog.column.productCode')" width="100" />
        <el-table-column prop="subProductCode" :label="$t('demand.linkedDialog.column.subProductCode')" width="90" />
        <el-table-column prop="quantity" :label="$t('demand.linkedDialog.column.quantity')" width="70" align="right" />
        <el-table-column prop="destination" :label="$t('demand.linkedDialog.column.destination')" min-width="100" show-overflow-tooltip />
        <el-table-column prop="status" :label="$t('demand.linkedDialog.column.status')" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, type FormInstance, ElMessageBox } from 'element-plus'
import { Plus, Clock, Warning, CircleCheck, Delete } from '@element-plus/icons-vue'
import {
  demandApi,
  type DemandPageVO,
  type CreateDemandRequest,
  type UpdateDemandRequest,
  type SubProductItem,
} from '@/api/demand'
import { productApi, type MasterCodeSuggestVO, type SubCodeSuggestVO } from '@/api/product'
import { factoryApi, type FactoryPageVO } from '@/api/factory'
import { useI18n } from 'vue-i18n'

const loading = ref(false)
const submitting = ref(false)
const masterCodeLoading = ref(false)
const subCodeLoading = ref(false)
const factoryLoading = ref(false)
const converting = ref(false)
const linkedLoading = ref(false)
const masterCodeOptions = ref<MasterCodeSuggestVO[]>([])
const subCodeOptions = ref<SubCodeSuggestVO[][]>([[]])
const factoryOptions = ref<FactoryPageVO[]>([])
const linkedProcurements = ref<unknown[]>([])
const loadedSubCodesFor = ref<string>('')
const dialogVisible = ref(false)
const convertDialogVisible = ref(false)
const linkedDialogVisible = ref(false)
const dialogMode = ref<'create' | 'update'>('create')
const currentRow = ref<DemandPageVO | null>(null)
const formRef = ref<FormInstance>()

const { t, locale: localeRef } = useI18n()
const currentLocale = computed(() => localeRef.value)

const filterForm = reactive({ demandType: '', productCode: '', status: '' })
const pagination = reactive({ page: 1, pageSize: 20, total: 0 })
const tableData = ref<DemandPageVO[]>([])

const pendingCount = computed(() => tableData.value.filter(r => r.status === 'PENDING').length)
const convertedCount = computed(() => tableData.value.filter(r => r.status === 'CONVERTED').length)

// 转采购弹窗数据
const convertForm = reactive<{ factoryId: number | null; demand: DemandPageVO | null }>({
  factoryId: null,
  demand: null,
})

function emptyItem(): SubProductItem {
  return { subCode: '', quantity: 0, destination: '' }
}

function defaultFormData(): CreateDemandRequest & { subProductItems: SubProductItem[] } {
  return {
    demandType: 'REPLENISHMENT',
    productCode: '',
    subProductItems: [emptyItem()],
    japanLead: '',
    remarks: '',
  }
}

// 强类型化 formData
const formData = reactive<{ demandType: string; productCode: string; subProductItems: SubProductItem[]; japanLead: string; remarks: string }>(
  defaultFormData() as { demandType: string; productCode: string; subProductItems: SubProductItem[]; japanLead: string; remarks: string }
)

const formRules = {
  demandType: [{ required: true, message: () => t('demand.validation.demandTypeRequired'), trigger: 'change' }],
  productCode: [{ required: true, message: () => t('demand.validation.productCodeRequired'), trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    const res = await demandApi.list({
      page: pagination.page - 1,
      pageSize: pagination.pageSize,
      status: filterForm.status || undefined,
      demandType: filterForm.demandType || undefined,
      productCode: filterForm.productCode.trim() || undefined,
    })
    const payload = res.data.data as { content: DemandPageVO[]; totalElements: number }
    tableData.value = payload.content || []
    pagination.total = payload.totalElements || 0
  } catch { /* interceptor */ } finally {
    loading.value = false
  }
}

function onReset() {
  filterForm.demandType = ''
  filterForm.productCode = ''
  filterForm.status = ''
  pagination.page = 1
  loadData()
}

function onNew() {
  dialogMode.value = 'create'
  const d = defaultFormData()
  Object.assign(formData, d)
  subCodeOptions.value = [[]]
  loadedSubCodesFor.value = ''
  dialogVisible.value = true
}

function onEdit(row: DemandPageVO) {
  dialogMode.value = 'update'
  currentRow.value = row
  Object.assign(formData, {
    demandType: row.demandType,
    productCode: row.productCode,
    subProductItems: (row.subProductItems && row.subProductItems.length > 0)
      ? row.subProductItems.map(i => ({ subCode: i.subCode, quantity: i.quantity, destination: i.destination || '' }))
      : [emptyItem()],
    japanLead: row.japanLead || '',
    remarks: row.remarks || '',
  })
  loadedSubCodesFor.value = row.productCode
  subCodeOptions.value = [[]]
  dialogVisible.value = true
}

function addSubProductItem() {
  formData.subProductItems.push(emptyItem())
  subCodeOptions.value.push([])
}

function removeSubProductItem(index: number) {
  formData.subProductItems.splice(index, 1)
  subCodeOptions.value.splice(index, 1)
}


// 转采购（v1.6.0：弹窗选工厂）
function onConvert(row: DemandPageVO) {
  convertForm.demand = row
  convertForm.factoryId = null
  factoryOptions.value = []
  convertDialogVisible.value = true
}

async function searchFactory(query: string) {
  if (!query || query.length < 1) {
    factoryOptions.value = []
    return
  }
  factoryLoading.value = true
  try {
    const res = await factoryApi.list({ factoryName: query, pageSize: 20 })
    factoryOptions.value = res.data.data?.content || []
  } catch {
    factoryOptions.value = []
  } finally {
    factoryLoading.value = false
  }
}

async function doConvert() {
  if (!convertForm.factoryId) {
    ElMessage.warning(t('demand.dialog.convertDialog.factoryRequired'))
    return
  }
  if (!convertForm.demand) return
  converting.value = true
  try {
    const res = await demandApi.convertToProcurement(convertForm.demand.id, { factoryId: convertForm.factoryId! })
    const data = res.data.data!
    convertDialogVisible.value = false
    ElMessage.success(
      t('demand.message.convertSuccess', {
        n: data.linkedProcurementIds.length,
        ids: data.linkedProcurementIds.join(', '),
      })
    )
    loadData()
  } catch { /* interceptor */ } finally {
    converting.value = false
  }
}

async function onViewLinked(row: DemandPageVO) {
  linkedDialogVisible.value = true
  linkedProcurements.value = []
  linkedLoading.value = true
  try {
    const res = await demandApi.getLinkedProcurements(row.id)
    linkedProcurements.value = res.data.data || []
  } catch {
    ElMessage.error(t('demand.message.loadLinkedFailed'))
  } finally {
    linkedLoading.value = false
  }
}

let masterCodeTimer: ReturnType<typeof setTimeout>
async function searchMasterCode(query: string) {
  clearTimeout(masterCodeTimer)
  if (!query || query.length < 1) {
    masterCodeOptions.value = []
    return
  }
  masterCodeTimer = setTimeout(async () => {
    masterCodeLoading.value = true
    try {
      const res = await productApi.suggestMasterCodes(query)
      masterCodeOptions.value = res.data.data || []
    } catch (e) {
      console.error('[DemandPage] searchMasterCode failed', e)
      masterCodeOptions.value = []
    } finally {
      masterCodeLoading.value = false
    }
  }, 300)
}

function onMasterCodeChange(val: string) {
  formData.productCode = val
  formData.subProductItems = [emptyItem()]
  subCodeOptions.value = [[]]
  loadedSubCodesFor.value = ''
  if (val) {
    loadSubCodeOptions(0)
  }
}

async function loadSubCodeOptions(_index: number) {
  const masterCode = formData.productCode
  if (!masterCode || loadedSubCodesFor.value === masterCode) return
  loadedSubCodesFor.value = masterCode
  subCodeLoading.value = true
  try {
    const res = await productApi.suggestSubCodes(masterCode)
    const opts = res.data.data || []
    // 填充所有空槽位
    for (let i = 0; i < subCodeOptions.value.length; i++) {
      subCodeOptions.value[i] = opts
    }
    // 如果只有一行，自动代入全部子货号
    if (formData.subProductItems.length === 1 && !formData.subProductItems[0].subCode && opts.length > 0) {
      formData.subProductItems = opts.map(o => ({
        subCode: o.subCode,
        quantity: 0,
        destination: '',
      }))
      subCodeOptions.value = opts.map(() => opts)
    }
  } catch (e) {
    console.error('[DemandPage] loadSubCodeOptions failed', e)
  } finally {
    subCodeLoading.value = false
  }
}

async function onRevertConversion(row: DemandPageVO) {
  try {
    await ElMessageBox.confirm(
      t('demand.message.revertConfirm'),
      t('demand.message.revertConfirmTitle'),
      { confirmButtonText: t('common.confirm'), cancelButtonText: t('common.cancel'), type: 'warning' }
    )
  } catch { return }
  try {
    await demandApi.revertConversion(row.id)
    ElMessage.success(t('demand.message.revertSuccess'))
    loadData()
  } catch { /* interceptor */ }
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
    // 过滤空行
    const items = formData.subProductItems.filter(i => i.subCode.trim())
    if (items.length === 0) {
      ElMessage.warning(t('demand.validation.subProductItemsRequired'))
      return
    }
    submitting.value = true
    try {
      const payload = {
        demandType: formData.demandType,
        productCode: formData.productCode,
        subProductItems: items,
        japanLead: formData.japanLead || undefined,
        remarks: formData.remarks || undefined,
      }
      if (dialogMode.value === 'create') {
        await demandApi.create(payload as CreateDemandRequest)
        ElMessage.success(t('demand.message.createSuccess'))
      } else if (currentRow.value) {
        await demandApi.update(currentRow.value.id, payload as UpdateDemandRequest)
        ElMessage.success(t('demand.message.updateSuccess'))
      }
      dialogVisible.value = false
      loadData()
    } finally {
      submitting.value = false
    }
  })
}

function demandStatusLabel(status: string): string {
  return { PENDING: t('demand.status.PENDING'), CONVERTED: t('demand.status.CONVERTED'), CANCELLED: t('demand.status.CANCELLED') }[status] ?? status
}

function demandStatusType(status: string): string {
  return { PENDING: 'warning', CONVERTED: 'success', CANCELLED: 'info' }[status] ?? 'info'
}

onMounted(() => loadData())
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.page-header { display: flex; align-items: center; justify-content: space-between; }
.page-title { margin: 0; font-size: 20px; font-weight: 700; color: var(--text-primary); }
.page-title::before { content: ''; display: inline-block; width: 4px; height: 20px; background: var(--color-primary); border-radius: 2px; margin-right: 10px; vertical-align: middle; }
.filter-card :deep(.el-card__body) { padding-bottom: 0; }
.stats-row { margin-bottom: 4px; }
.stat-card { border-radius: var(--radius-md); border: 1px solid var(--border-color); box-shadow: var(--shadow-card); position: relative; overflow: hidden; transition: all var(--transition-fast); }
.stat-card::before { content: ''; position: absolute; top: 0; left: 0; right: 0; height: 3px; background: linear-gradient(90deg, var(--color-primary), var(--color-primary-light)); border-radius: var(--radius-md) var(--radius-md) 0 0; }
.stat-card:hover { box-shadow: var(--shadow-md); transform: translateY(-2px); }
.stat-content { display: flex; align-items: center; gap: 14px; }
.stat-icon-wrap { width: 48px; height: 48px; border-radius: 50%; background: var(--color-primary-pale); display: flex; align-items: center; justify-content: center; }
.stat-icon { font-size: 22px; }
.stat-value { font-size: 26px; font-weight: 800; color: var(--text-primary); line-height: 1; font-variant-numeric: tabular-nums; }
.stat-label { font-size: 13px; color: var(--text-secondary); margin-top: 4px; }
.product-code { color: var(--color-primary); font-family: monospace; font-size: 12px; font-weight: 700; background: var(--color-primary-pale); padding: 3px 9px; border-radius: 5px; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
.sub-item-chip { font-size: 12px; }
.sub-item-detail { color: #D97706; font-weight: 600; }
.sub-item-dest { color: #6B7280; }

/* v1.6.0: 弹窗表单布局 */
:deep(.demand-form .el-form-item) { margin-bottom: 16px; }
:deep(.demand-form .el-form-item:last-child) { margin-bottom: 0; }
.form-row--2col {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 16px;
  align-items: flex-start;
}
.form-item--shrink { flex-shrink: 0; }
.form-item--fill { flex: 1; }

/* 子货号明细区 — 2列卡片网格 */
.sic-section {
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  padding: 14px 16px;
  margin-bottom: 16px;
}
.sic-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.sic-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-regular);
}
.sic-count {
  font-size: 11px;
  color: var(--el-color-primary);
  background: color-mix(in srgb, var(--el-color-primary) 10%, transparent);
  border-radius: 20px;
  padding: 2px 10px;
  font-weight: 500;
}
.sic-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-bottom: 10px;
}
.sic-card {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 10px 12px;
  transition: box-shadow 0.2s, border-color 0.2s;
}
.sic-card:hover {
  border-color: var(--el-border-color);
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.sic-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.sic-card-body {
  display: grid;
  grid-template-columns: 90px 1fr;
  gap: 8px;
  align-items: center;
}
.sic-field__label {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  display: block;
  margin-bottom: 2px;
}
.sic-empty {
  text-align: center;
  color: var(--el-text-color-placeholder);
  font-size: 13px;
  padding: 8px 0 4px;
}
.sic-add-btn {
  width: 100%;
  border-style: dashed !important;
  color: var(--el-color-primary);
  font-size: 13px;
  margin-top: 4px;
}
</style>
