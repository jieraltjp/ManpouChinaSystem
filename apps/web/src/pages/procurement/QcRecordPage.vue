<template>
  <div class="page">
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#E8650A"><Document /></el-icon></div>
            <div><div class="stat-value">{{ pagination.total }}</div><div class="stat-label">{{ $t('inspection.stat.total') }}</div></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#16A34A"><CircleCheck /></el-icon></div>
            <div><div class="stat-value">{{ passCount }}</div><div class="stat-label">{{ $t('inspection.stat.pass') }}</div></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#DC2626"><Warning /></el-icon></div>
            <div><div class="stat-value">{{ failCount }}</div><div class="stat-label">{{ $t('inspection.stat.fail') }}</div></div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filterForm">
        <el-form-item :label="$t('inspection.filter.qcCode')">
          <el-input v-model="filterForm.qcCode" :placeholder="$t('inspection.filter.qcCodePlaceholder')" clearable style="width:180px" />
        </el-form-item>
        <el-form-item :label="$t('inspection.filter.shipmentBatchId')">
          <el-select
            v-model="filterForm.shipmentBatchId"
            :placeholder="$t('inspection.filter.shipmentBatchIdPlaceholder')"
            clearable
            filterable
            style="width:180px"
          >
            <el-option
              v-for="b in shipmentBatchList"
              :key="b.id"
              :label="`${b.batchCode} / ${b.status}`"
              :value="b.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('inspection.filter.result')">
          <el-select v-model="filterForm.result" :placeholder="$t('inspection.filter.all')" clearable style="width:120px">
            <el-option value="PASS" :label="$t('inspection.result.pass')" />
            <el-option value="FAIL" :label="$t('inspection.result.fail')" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('inspection.filter.status')">
          <el-select v-model="filterForm.status" :placeholder="$t('inspection.filter.all')" clearable style="width:130px">
            <el-option value="PENDING" :label="$t('inspection.qcStatus.pending')" />
            <el-option value="COMPLETED" :label="$t('inspection.qcStatus.completed')" />
            <el-option value="RETURN_REQUESTED" :label="$t('inspection.qcStatus.returnRequested')" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearchFromButton">{{ $t('inspection.filter.search') }}</el-button>
          <el-button @click="onReset">{{ $t('inspection.filter.reset') }}</el-button>
          <el-button type="primary" @click="onNew" v-if="hasPermission('qc:create')">
            <el-icon><Plus /></el-icon>{{ $t('inspection.newButton') }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="tableData" stripe style="width:100%" min-height="200">
        <el-table-column prop="qcCode" :label="$t('inspection.column.qcCode')" min-width="160" />
        <el-table-column :label="$t('inspection.column.shipmentBatchId')" min-width="150" align="center">
          <template #default="{ row }">
            <span v-if="row.batchCode" class="product-code">{{ row.batchCode }}</span>
            <span v-else-if="row.shipmentBatchId" class="product-code">{{ row.shipmentBatchId }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="productCode" :label="$t('inspection.column.productCode')" min-width="120">
          <template #default="{ row }">
            <span class="product-code">{{ row.productCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="subProductCode" :label="$t('inspection.column.subProductCode')" min-width="110">
          <template #default="{ row }">
            {{ row.subProductCode || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="sellerName" :label="$t('inspection.column.sellerName')" min-width="120" show-overflow-tooltip />
        <el-table-column prop="shipmentQuantity" :label="$t('inspection.column.quantity')" min-width="80" align="right">
          <template #default="{ row }">
            <span v-if="row.shipmentQuantity" class="product-code">{{ row.shipmentQuantity }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="quantity" :label="$t('inspection.column.orderQuantity')" min-width="80" align="right">
          <template #default="{ row }">
            <span v-if="row.quantity" style="color:#909399">{{ row.quantity }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="inspectionCount" :label="$t('inspection.column.inspectionCount')" min-width="90" align="right" />
        <el-table-column prop="passedCount" :label="$t('inspection.column.passedCount')" min-width="90" align="right">
          <template #default="{ row }">
            <span style="color:#16A34A;font-weight:600">{{ row.passedCount ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="defectiveCount" :label="$t('inspection.column.defectiveCount')" min-width="90" align="right">
          <template #default="{ row }">
            <span v-if="row.defectiveCount" style="color:#DC2626;font-weight:600">{{ row.defectiveCount }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="qcDate" :label="$t('inspection.column.qcDate')" min-width="120" />
        <el-table-column prop="result" :label="$t('inspection.column.result')" min-width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.result === 'PASS' ? 'success' : 'danger'" size="small">
              {{ row.result === 'PASS' ? $t('inspection.result.pass') : $t('inspection.result.fail') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" :label="$t('inspection.column.status')" min-width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'COMPLETED' ? 'success' : row.status === 'RETURN_REQUESTED' ? 'danger' : 'info'" size="small">
              {{ qcStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('inspection.column.action')" min-width="180" align="center">
          <template #default="{ row }">
            <el-button link class="btn-blue" size="small" @click.stop="onView(row)">{{ $t('inspection.action.detail') }}</el-button>
            <el-button link type="warning" size="small" @click.stop="onEdit(row)" v-if="hasPermission('qc:update')">{{ $t('inspection.action.edit') }}</el-button>
            <el-button link type="danger" size="small" @click.stop="onDelete(row)" v-if="hasPermission('qc:delete')">{{ $t('inspection.action.delete') }}</el-button>
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
          @size-change="onSearch"
          @current-change="onSearch"
        />
      </div>
    </el-card>

    <!-- 新规验货弹窗 -->
    <el-dialog v-model="dialogVisible" :title="$t('inspection.newDialogTitle')" width="920px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="86px">
        <!-- Row 1: 出货批次 -->
        <el-row :gutter="10">
          <el-col :span="16">
            <el-form-item :label="$t('inspection.dialog.shipmentBatch')" prop="shipmentBatchId">
              <el-select
                v-model="form.shipmentBatchId"
                :placeholder="$t('inspection.dialog.shipmentBatchPlaceholder')"
                filterable
                :loading="shipmentBatchLoading"
                style="width:100%"
                @change="onShipmentBatchSelected"
              >
                <el-option
                  v-for="b in shipmentBatchList"
                  :key="b.id"
                  :label="`${b.batchCode} / ${b.status}`"
                  :value="b.id"
                />
              </el-select>
              <div v-if="currentRow?.batchCode" class="batch-info-tag">
                <el-tag size="small" type="info">{{ currentRow.batchCode }}</el-tag>
                <el-tag v-if="currentRow.batchStatus" size="small" type="warning" style="margin-left:4px">{{ currentRow.batchStatus }}</el-tag>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('inspection.column.sellerName')">
              <el-input v-model="form.sellerName" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <!-- Row 2: 货号 + 子货号 -->
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item :label="$t('inspection.dialog.productCode')" prop="productCode">
              <el-input v-model="form.productCode" :placeholder="$t('inspection.dialog.productCodePlaceholder')" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('inspection.dialog.subProductCode')">
              <el-input v-model="form.subProductCode" :placeholder="$t('inspection.dialog.subProductCodePlaceholder')" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <!-- Row 3: 验货类型 + 日期 + 数量 + 状态 -->
        <el-row :gutter="10">
          <el-col :span="6">
            <el-form-item :label="$t('inspection.dialog.qcType')">
              <el-select v-model="form.qcType" style="width:100%">
                <el-option value="ONSITE" :label="$t('inspection.qcType.onsite')" />
                <el-option value="REMOTE" :label="$t('inspection.qcType.remote')" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item :label="$t('inspection.dialog.qcDate')">
              <el-date-picker v-model="form.qcDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item :label="$t('inspection.dialog.quantity')">
              <el-input-number v-model="form.quantity" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item :label="$t('inspection.dialog.qcStatus')">
              <el-select v-model="form.status" style="width:100%">
                <el-option value="PENDING" :label="$t('inspection.qcStatus.pending')" />
                <el-option value="COMPLETED" :label="$t('inspection.qcStatus.completed')" />
                <el-option value="RETURN_REQUESTED" :label="$t('inspection.qcStatus.returnRequested')" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <!-- Row 4: 检品数 + 合格数 + 结果 + 箱数 -->
        <el-row :gutter="10">
          <el-col :span="6">
            <el-form-item :label="$t('inspection.dialog.inspectionCount')" prop="inspectionCount">
              <el-input-number v-model="form.inspectionCount" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item :label="$t('inspection.dialog.passedCount')">
              <el-input-number v-model="form.passedCount" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item :label="$t('inspection.dialog.result')">
              <el-select v-model="form.result" style="width:100%">
                <el-option value="PASS" :label="$t('inspection.result.pass')" />
                <el-option value="FAIL" :label="$t('inspection.result.fail')" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item :label="$t('inspection.dialog.boxCount')">
              <el-input-number v-model="form.boxCount" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <!-- Row 5: 材质 + 目的地 -->
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item :label="$t('inspection.dialog.material')">
              <el-input v-model="form.material" :placeholder="$t('inspection.dialog.materialPlaceholder')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('inspection.dialog.destination')">
              <el-input v-model="form.destination" :placeholder="$t('inspection.dialog.destinationPlaceholder')" />
            </el-form-item>
          </el-col>
        </el-row>
        <!-- Row 6: 体积（长+宽+高） -->
        <el-row :gutter="10">
          <el-col :span="8">
            <el-form-item :label="$t('inspection.dialog.boxLengthCm')">
              <el-input-number v-model="form.boxLengthCm" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('inspection.dialog.boxWidthCm')">
              <el-input-number v-model="form.boxWidthCm" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('inspection.dialog.boxHeightCm')">
              <el-input-number v-model="form.boxHeightCm" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <!-- Row 7: 重量 + 价格 -->
        <el-row :gutter="10">
          <el-col :span="8">
            <el-form-item :label="$t('inspection.dialog.netWeightPerUnit')">
              <el-input-number v-model="form.netWeightPerUnit" :min="0" :precision="3" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('inspection.dialog.grossWeight')">
              <el-input-number v-model="form.grossWeight" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('inspection.dialog.taxInclusivePrice')">
              <el-input-number v-model="form.taxInclusivePrice" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <!-- 备注 -->
        <el-form-item :label="$t('inspection.dialog.qcStandard')">
          <el-input v-model="form.qcStandard" type="textarea" :rows="1" :placeholder="$t('inspection.dialog.qcStandardPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('inspection.dialog.remarks')">
          <el-input v-model="form.remarks" type="textarea" :rows="1" :placeholder="$t('inspection.dialog.remarksPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('inspection.dialog.images')">
          <el-upload
            ref="uploadRef"
            action="#"
            :auto-upload="false"
            :limit="9"
            :file-list="uploadFileList"
            :on-change="onImageChange"
            :on-remove="onImageRemove"
            accept="image/jpeg,image/png,image/webp"
            list-type="picture-card"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">{{ $t('inspection.dialog.imagesTip') }}</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" :title="$t('inspection.drawerTitle')" size="720px" direction="rtl">
      <el-descriptions :column="2" border v-if="currentRow" label-class-name="drawer-label">
        <!-- 基本信息 -->
        <el-descriptions-item :label="$t('inspection.column.qcCode')">{{ currentRow.qcCode }}</el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.column.result')">
          <el-tag :type="currentRow.result === 'PASS' ? 'success' : 'danger'" size="small">
            {{ currentRow.result === 'PASS' ? $t('inspection.result.pass') : $t('inspection.result.fail') }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.dialog.qcStatus')">
          <el-tag :type="currentRow.status === 'COMPLETED' ? 'success' : currentRow.status === 'RETURN_REQUESTED' ? 'danger' : 'info'" size="small">
            {{ qcStatusLabel(currentRow.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.dialog.qcType')">{{ qcTypeLabel(currentRow.qcType) }}</el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.column.qcDate')">{{ currentRow.qcDate || '-' }}</el-descriptions-item>
        <!-- 出货批次与货柜信息 -->
        <el-descriptions-item :label="$t('inspection.column.shipmentBatchId')" :span="2">
          <span v-if="currentRow.batchCode" class="product-code">{{ currentRow.batchCode }}</span>
          <el-tag v-else-if="currentRow.shipmentBatchId" size="small" type="info">{{ currentRow.shipmentBatchId }}</el-tag>
          <span v-else>-</span>
          <el-tag v-if="currentRow.batchStatus" size="small" type="warning" style="margin-left:8px">{{ currentRow.batchStatus }}</el-tag>
        </el-descriptions-item>
        <!-- 商品信息 -->
        <el-descriptions-item :label="$t('inspection.column.productCode')">
          <span class="product-code">{{ currentRow.productCode }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.column.subProductCode')">{{ currentRow.subProductCode || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.column.sellerName')" :span="2">{{ currentRow.sellerName || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.dialog.material')">{{ currentRow.material || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.dialog.destination')">{{ currentRow.destination || '-' }}</el-descriptions-item>
        <!-- 验货数据 -->
        <el-descriptions-item :label="$t('inspection.column.quantity')">
          <span v-if="currentRow.shipmentQuantity" class="product-code">{{ currentRow.shipmentQuantity }}</span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.column.orderQuantity')">
          <span v-if="currentRow.quantity">{{ currentRow.quantity }}</span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.column.inspectionCount')">{{ currentRow.inspectionCount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.column.passedCount')">{{ currentRow.passedCount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.column.defectiveCount')">{{ currentRow.defectiveCount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.column.boxCount')">{{ currentRow.boxCount ?? '-' }}</el-descriptions-item>
        <!-- 体积重量 -->
        <el-descriptions-item :label="$t('inspection.dialog.boxDimension')" :span="2">{{ boxDimension }}</el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.dialog.netWeightPerUnit')">{{ currentRow.netWeightPerUnit ? currentRow.netWeightPerUnit + ' kg' : '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.dialog.grossWeight')">{{ currentRow.grossWeight ? currentRow.grossWeight + ' kg' : '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.dialog.taxInclusivePrice')">{{ currentRow.taxInclusivePrice ? '¥' + currentRow.taxInclusivePrice : '-' }}</el-descriptions-item>
        <!-- 备注 -->
        <el-descriptions-item :label="$t('inspection.dialog.qcStandard')" :span="2">{{ currentRow.qcStandard || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.dialog.remarks')" :span="2">{{ currentRow.remarks || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.dialog.images')" :span="2">
          <div v-if="!drawerImages.length">-</div>
          <template v-else>
            <div class="drawer-images">
              <el-image
                v-for="img in drawerImages"
                :key="img.id"
                :src="img.url"
                :preview-src-list="drawerImages.map(i => i.url)"
                fit="cover"
                class="drawer-image-thumb"
                :lazy="false"
              />
            </div>
          </template>
        </el-descriptions-item>
        <!-- 元数据 -->
        <el-descriptions-item :label="$t('inspection.dialog.createBy')">{{ currentRow.createBy || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('inspection.dialog.createTime')">
          {{ currentRow.createTime ? new Date(currentRow.createTime).toLocaleString(localeRef === 'ja' ? 'ja-JP' : 'zh-CN', {year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',second:'2-digit'}) : '-' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules, ElMessageBox } from 'element-plus'
import { Plus, Document, CircleCheck, Warning } from '@element-plus/icons-vue'
import { inspectionApi, type QcRecordVO, type QcResult, type QcStatus, type QcType, type QcImageVO } from '@/api/inspection'
import { procurementApi, type ProcurementPageVO } from '@/api/procurement'
import { shipmentBatchApi, type ShipmentBatchVO } from '@/api/procurement'
import { useI18n } from 'vue-i18n'
import { usePermission } from '@/composables/usePermission'

const { hasPermission } = usePermission()

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const drawerVisible = ref(false)
const shipmentBatchLoading = ref(false)

const currentRow = ref<QcRecordVO | null>(null)
const filterForm = reactive({ qcCode: '', result: '' as QcResult | '', status: '' as string | '', procurementId: undefined as number | undefined, shipmentBatchId: undefined as number | undefined })
const pagination = reactive({ page: 1, pageSize: 20, total: 0 })
const tableData = ref<QcRecordVO[]>([])
const shipmentBatchList = ref<ShipmentBatchVO[]>([])

const formRef = ref<FormInstance>()
const uploadRef = ref()

/** 上传文件项：id 有值=已有图片，raw 有值=本地待上传文件 */
interface UploadFileItem {
  id?: number        // 数据库ID，有则代表已有图片
  name: string
  url: string        // COS URL 或本地 blob URL
  raw?: File         // 本地文件，上传后清空
  status: 'ready' | 'uploaded'  // ready=待上传，uploaded=已上传到COS
}
const uploadFileList = ref<UploadFileItem[]>([])

/** 详情抽屉图片列表（从 qc_image 表加载） */
const drawerImages = ref<QcImageVO[]>([])
const { t, locale: localeRef } = useI18n()

const form = reactive({
  shipmentBatchId: undefined as number | undefined,  // V43新增：关联出货批次（必填）
  procurementId: undefined as number | undefined,    // 保留用于审计追溯
  productCode: '',
  subProductCode: '',
  qcUserId: undefined as number | undefined,
  qcType: undefined as QcType | undefined,
  qcDate: '',
  result: undefined as QcResult | undefined,
  status: 'PENDING' as QcStatus,
  inspectionCount: undefined as number | undefined,
  passedCount: undefined as number | undefined,
  boxCount: undefined as number | undefined,
  boxLengthCm: undefined as number | undefined,
  boxWidthCm: undefined as number | undefined,
  boxHeightCm: undefined as number | undefined,
  netWeightPerUnit: undefined as number | undefined,
  grossWeight: undefined as number | undefined,
  taxInclusivePrice: undefined as number | undefined,
  material: '',
  qcStandard: '',
  remarks: '',
  images: '',
  destination: '',
  quantity: undefined as number | undefined,
  orderDate: '',
  sellerName: '',
})

// 弹窗打开时预加载出货批次（下拉默认有选项）
watch(dialogVisible, async (val) => {
  if (val) {
    shipmentBatchLoading.value = true
    try {
      const res = await shipmentBatchApi.list({ page: 0, pageSize: 200 })
      shipmentBatchList.value = res.data?.content ?? []
    } catch {
      shipmentBatchList.value = []
    } finally {
      shipmentBatchLoading.value = false
    }
  }
})

const formRules: FormRules = {
  shipmentBatchId: [{ required: true, message: () => t('inspection.validation.shipmentBatchRequired'), trigger: 'change' }],
  productCode: [{ required: true, message: () => t('inspection.validation.productCodeRequired'), trigger: 'blur' }],
  inspectionCount: [{ required: true, message: () => t('inspection.validation.inspectionCountRequired'), trigger: 'blur' }],
}

const passCount = computed(() => tableData.value.filter(r => r.result === 'PASS').length)
const failCount = computed(() => tableData.value.filter(r => r.result === 'FAIL').length)

const boxDimension = computed(() => {
  const r = currentRow.value
  if (!r) return '-'
  const parts = []
  if (r.boxLengthCm) parts.push(r.boxLengthCm)
  if (r.boxWidthCm) parts.push(r.boxWidthCm)
  if (r.boxHeightCm) parts.push(r.boxHeightCm)
  return parts.length ? parts.join(t('common.format.times')) + t('common.units.cm') : '-'
})

// --- 图片上传处理（SPEC-C12） ---
function onImageChange(file: { name: string; raw: File; uid: number }, fileList: { name: string; raw: File; uid: number }[]) {
  if (file.raw.size > 5 * 1024 * 1024) {
    ElMessage.warning(t('inspection.dialog.imageSizeWarning'))
    uploadRef.value?.handleRemove(file)
    return
  }
  if (fileList.length > 9) {
    ElMessage.warning(t('inspection.dialog.imageCountWarning'))
    uploadRef.value?.handleRemove(file)
    return
  }
  const alreadyIn = uploadFileList.value.some(f => f.name === file.name && f.raw === file.raw)
  if (!alreadyIn) {
    uploadFileList.value = [...uploadFileList.value, {
      name: file.name,
      url: URL.createObjectURL(file.raw),
      raw: file.raw,
      status: 'ready',
    }]
  }
}

function onImageRemove(file: { id?: number; name: string; raw?: File; uid: number }) {
  // 已有图片（有 id）：按 id 匹配
  if (file.id != null) {
    uploadFileList.value = uploadFileList.value.filter(f => f.id !== file.id)
  } else {
    // 本地新图片（无 id）：按 name + raw 匹配
    uploadFileList.value = uploadFileList.value.filter(
      f => !(f.name === file.name && f.raw === file.raw)
    )
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await inspectionApi.list({
      page: pagination.page - 1,
      pageSize: pagination.pageSize,
      qcCode: filterForm.qcCode || undefined,
      result: filterForm.result || undefined,
      status: filterForm.status || undefined,
      procurementId: filterForm.procurementId,
      shipmentBatchId: filterForm.shipmentBatchId,
    })
    const data = res.data
    tableData.value = data?.content ?? []
    pagination.total = data?.totalElements ?? 0
  } catch {
    ElMessage.error(t('inspection.message.loadFailed'))
  } finally {
    loading.value = false
  }
}

async function onSearch() {
  await loadData()
}

function onSearchFromButton() {
  pagination.page = 1
  loadData()
}

function onReset() {
  filterForm.qcCode = ''
  filterForm.result = ''
  filterForm.status = ''
  filterForm.procurementId = undefined
  filterForm.shipmentBatchId = undefined
  pagination.page = 1
  loadData()
}

function onShipmentBatchSelected(id: number) {
  const batch = shipmentBatchList.value.find(b => b.id === id)
  if (!batch) return
  form.shipmentBatchId = batch.id
  form.procurementId = batch.procurementId
  // 通过 procurementId 补充商品信息（productCode, sellerName, material, destination）
  if (batch.procurementId) {
    procurementApi.list({ page: 0, pageSize: 200 }).then(res => {
      const p = (res.data?.content ?? []).find((p: ProcurementPageVO) => p.id === batch.procurementId)
      if (p) {
        form.productCode = p.productCode || ''
        form.subProductCode = p.subProductCode || ''
        form.sellerName = p.factoryName || ''
        form.material = p.material || ''
        form.destination = p.destination || ''
        form.orderDate = p.orderDate || ''
        form.quantity = p.quantity ?? 0
      }
    }).catch(() => {})
  }
  if (!form.qcDate) {
    form.qcDate = new Date().toISOString().slice(0, 10)
  }
}

function onNew() {
  formRef.value?.resetFields()
  currentRow.value = null  // 必须重置，否则 onSubmit 会误入编辑分支
  const today = new Date().toISOString().slice(0, 10)
  Object.assign(form, {
    shipmentBatchId: undefined, procurementId: undefined,
    productCode: '', subProductCode: '', qcUserId: undefined,
    qcType: 'ONSITE', qcDate: today, result: 'PASS', status: 'PENDING',
    inspectionCount: 0, passedCount: 0,
    boxCount: 0, boxLengthCm: 0, boxWidthCm: 0,
    boxHeightCm: 0, netWeightPerUnit: 0, grossWeight: 0,
    taxInclusivePrice: 0, material: '',
    qcStandard: '', remarks: '', images: '', destination: '', quantity: 0, orderDate: '', sellerName: '',
  })
  uploadFileList.value = []
  dialogVisible.value = true
}

async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (form.inspectionCount !== undefined && form.passedCount !== undefined && form.passedCount > form.inspectionCount) {
      ElMessage.warning(t('inspection.validation.passedCountExceeds'))
      submitting.value = false
      return
    }

    if (currentRow.value) {
      // ── 编辑模式 ──────────────────────────────────────────
      // 1. 记录编辑前的已有图片ID，用于检测被移除的图片
      const originalIds: number[] = []
      if (currentRow.value.id) {
        try {
          const existing = await inspectionApi.listImages(currentRow.value.id)
          existing.data?.forEach((img: QcImageVO) => originalIds.push(img.id))
        } catch { /* ignore */ }
      }
      const currentIds = uploadFileList.value.filter(f => f.id != null).map(f => f.id!)
      const toDeleteIds = originalIds.filter(id => !currentIds.includes(id))

      // 2. 更新 QC 记录
      await inspectionApi.update(currentRow.value.id, {
        sellerName: form.sellerName || undefined,
        qcUserId: form.qcUserId,
        qcType: form.qcType,
        qcDate: form.qcDate || undefined,
        result: form.result,
        status: form.status,
        inspectionCount: form.inspectionCount,
        passedCount: form.passedCount,
        boxCount: form.boxCount,
        boxLengthCm: form.boxLengthCm,
        boxWidthCm: form.boxWidthCm,
        boxHeightCm: form.boxHeightCm,
        netWeightPerUnit: form.netWeightPerUnit,
        grossWeight: form.grossWeight,
        taxInclusivePrice: form.taxInclusivePrice,
        material: form.material || undefined,
        qcStandard: form.qcStandard || undefined,
        remarks: form.remarks || undefined,
      })

      // 3. 删除被移除的已有图片
      if (toDeleteIds.length) {
        await Promise.all(toDeleteIds.map(id => inspectionApi.deleteImage(id)))
      }

      // 4. 上传新增的本地文件
      const newLocalFiles = uploadFileList.value.filter(f => f.raw)
      if (newLocalFiles.length) {
        await inspectionApi.uploadImages(
          newLocalFiles.map(f => f.raw!),
          currentRow.value.id,
        )
      }

      ElMessage.success(t('inspection.message.updateSuccess'))
    } else {
      // ── 创建模式 ──────────────────────────────────────────
      const res = await inspectionApi.create({
        shipmentBatchId: form.shipmentBatchId!,
        procurementId: form.procurementId,
        productCode: form.productCode,
        subProductCode: form.subProductCode || undefined,
        qcUserId: form.qcUserId,
        qcType: form.qcType,
        qcDate: form.qcDate || undefined,
        result: form.result,
        inspectionCount: form.inspectionCount,
        passedCount: form.passedCount,
        boxCount: form.boxCount,
        boxLengthCm: form.boxLengthCm,
        boxWidthCm: form.boxWidthCm,
        boxHeightCm: form.boxHeightCm,
        netWeightPerUnit: form.netWeightPerUnit,
        grossWeight: form.grossWeight,
        taxInclusivePrice: form.taxInclusivePrice,
        material: form.material || undefined,
        qcStandard: form.qcStandard || undefined,
        remarks: form.remarks || undefined,
        images: undefined,
        destination: form.destination || undefined,
        quantity: form.quantity,
        orderDate: form.orderDate || undefined,
        sellerName: form.sellerName || undefined,
      })
      const newQcRecordId = res.data

      // 批量上传
      const newLocalFiles = uploadFileList.value.filter(f => f.raw)
      if (newLocalFiles.length && newQcRecordId) {
        await inspectionApi.uploadImages(
          newLocalFiles.map(f => f.raw!),
          newQcRecordId,
        )
      }

      ElMessage.success(t('inspection.message.createSuccess'))
    }

    dialogVisible.value = false
    loadData()
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : String(e)
    ElMessage.error(t('inspection.message.createFailed') + ': ' + msg)
  } finally {
    submitting.value = false
  }
}

function onView(row: QcRecordVO) {
  currentRow.value = row
  drawerImages.value = []
  if (row.id) {
    inspectionApi.listImages(row.id).then(res => {
      drawerImages.value = res.data ?? []
    }).catch(() => {
      drawerImages.value = []
    })
  }
  drawerVisible.value = true
}

function onEdit(row: QcRecordVO) {
  formRef.value?.resetFields()
  Object.assign(form, {
    shipmentBatchId: row.shipmentBatchId ?? undefined,
    procurementId: row.procurementId ?? undefined,
    productCode: row.productCode || '',
    subProductCode: row.subProductCode || '',
    qcUserId: row.qcUserId ?? undefined,
    qcType: row.qcType as QcType || undefined,
    qcDate: row.qcDate || '',
    result: row.result as QcResult || undefined,
    status: row.status as QcStatus || 'PENDING',
    inspectionCount: row.inspectionCount ?? undefined,
    passedCount: row.passedCount ?? undefined,
    boxCount: row.boxCount ?? undefined,
    boxLengthCm: row.boxLengthCm ?? undefined,
    boxWidthCm: row.boxWidthCm ?? undefined,
    boxHeightCm: row.boxHeightCm ?? undefined,
    netWeightPerUnit: row.netWeightPerUnit ?? undefined,
    grossWeight: row.grossWeight ?? undefined,
    taxInclusivePrice: row.taxInclusivePrice ?? undefined,
    material: row.material || '',
    qcStandard: row.qcStandard || '',
    remarks: row.remarks || '',
    images: row.images || '',
    destination: row.destination || '',
    quantity: row.quantity ?? undefined,
    orderDate: row.orderDate || '',
    sellerName: row.sellerName || '',
  })
  // 加载已有图片到 uploadFileList（带 DB id 标识）
  uploadFileList.value = []
  if (row.id) {
    inspectionApi.listImages(row.id).then(res => {
      uploadFileList.value = (res.data ?? []).map((img: QcImageVO) => ({
        id: img.id,
        name: img.originalName,
        url: img.url,
        status: 'uploaded' as const,
      }))
    }).catch(() => {
      uploadFileList.value = []
    })
  }
  // 加载出货批次下拉（编辑时用于参考）
  if (row.shipmentBatchId) {
    shipmentBatchApi.get(row.shipmentBatchId).then(res => {
      if (res.data) {
        shipmentBatchList.value = [res.data]
      }
    }).catch(() => {})
  }
  currentRow.value = row
  dialogVisible.value = true
}

async function onDelete(row: QcRecordVO) {
  try {
    await ElMessageBox.confirm(
      t('inspection.message.deleteConfirm', { code: row.qcCode }),
      t('inspection.message.deleteConfirmTitle'),
      { confirmButtonText: t('common.delete'), cancelButtonText: t('common.cancel'), type: 'warning' },
    )
  } catch { return }
  try {
    await inspectionApi.delete(row.id)
    ElMessage.success(t('inspection.message.deleteSuccess'))
    loadData()
  } catch {
    ElMessage.error(t('inspection.message.deleteFailed') || t('inspection.message.actionFailed'))
  }
}


function qcTypeLabel(qcType?: string): string {
  return { ONSITE: t('inspection.qcType.onsite'), REMOTE: t('inspection.qcType.remote') }[qcType ?? ''] ?? '-'
}

function qcStatusLabel(status?: string): string {
  return { PENDING: t('inspection.qcStatus.pending'), COMPLETED: t('inspection.qcStatus.completed'), RETURN_REQUESTED: t('inspection.qcStatus.returnRequested') }[status ?? ''] ?? status ?? '-'
}

onMounted(() => {
  loadData()
  // 预加载出货批次下拉（筛选用）
  shipmentBatchApi.list({ page: 0, pageSize: 200 }).then(res => {
    shipmentBatchList.value = res.data?.content ?? []
  }).catch(() => {})
})
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.filter-card :deep(.el-card__body) { padding-bottom: 0; }
.stats-row { margin-bottom: 0; }
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
.divider-label { font-size: 13px; font-weight: 600; color: var(--text-secondary); }
.drawer-label { font-weight: 600; }
.drawer-images { display: flex; flex-wrap: wrap; gap: 8px; }
.drawer-image-thumb { width: 80px; height: 80px; border-radius: 4px; cursor: pointer; }
.upload-tip { font-size: 12px; color: var(--text-secondary); margin-top: 4px; }
.batch-info-tag { margin-top: 6px; display: flex; align-items: center; flex-wrap: wrap; gap: 2px; }
</style>
