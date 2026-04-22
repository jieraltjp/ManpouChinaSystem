<template>
  <div class="page">
    <div class="page-header">
      <h2 class="page-title">商品目录</h2>
      <div class="header-actions">
        <el-button type="primary" @click="onNew">
          <el-icon><Plus /></el-icon> 新增商品
        </el-button>
      </div>
    </div>

    <!-- 统计行 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#E8650A"><Goods /></el-icon></div>
            <div>
              <div class="stat-value">{{ pagination.total }}</div>
              <div class="stat-label">商品总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="主货号">
          <el-input v-model="filterForm.masterCode" placeholder="主货号" clearable style="width:160px" />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="filterForm.keyword" placeholder="中文/英文名称" clearable style="width:160px" />
        </el-form-item>
        <el-form-item label="HS编码">
          <el-input v-model="filterForm.hsCode" placeholder="HS编码" clearable style="width:140px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="tableData" stripe style="width:100%">
        <el-table-column prop="masterCode" label="主货号" width="120" />
        <el-table-column prop="subCode" label="子货号" width="100" />
        <el-table-column prop="nameZh" label="中文名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="nameEn" label="英文名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="hsCode" label="HS编码" width="120" />
        <el-table-column prop="unitPriceRmb" label="含税单价(CNY)" width="120" align="right">
          <template #default="{ row }">{{ row.unitPriceRmb != null ? `¥${Number(row.unitPriceRmb).toFixed(2)}` : '—' }}</template>
        </el-table-column>
        <el-table-column prop="origin" label="原产国" width="90" />
        <el-table-column prop="material" label="材质" width="100" show-overflow-tooltip />
        <el-table-column prop="unit" label="单位" width="70" align="center" />
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="onView(row)">详情</el-button>
            <el-button link type="primary" size="small" @click.stop="onEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click.stop="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <el-pagination
          background
          :current-page="pagination.page"
          :page-size="pagination.pageSize"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @current-change="onPageChange"
        />
      </div>
    </el-card>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="商品详情" size="560px" direction="rtl">
      <div v-if="currentRow" class="detail-grid">
        <div class="detail-item"><span class="detail-label">主货号</span><span class="detail-value">{{ currentRow.masterCode }}</span></div>
        <div class="detail-item"><span class="detail-label">子货号</span><span class="detail-value">{{ currentRow.subCode || '—' }}</span></div>
        <div class="detail-item"><span class="detail-label">中文名称</span><span class="detail-value">{{ currentRow.nameZh || '—' }}</span></div>
        <div class="detail-item"><span class="detail-label">英文名称</span><span class="detail-value">{{ currentRow.nameEn || '—' }}</span></div>
        <div class="detail-item"><span class="detail-label">日文名称</span><span class="detail-value">{{ currentRow.nameJa || '—' }}</span></div>
        <div class="detail-item"><span class="detail-label">HS编码</span><span class="detail-value">{{ currentRow.hsCode || '—' }}</span></div>
        <div class="detail-item"><span class="detail-label">含税单价</span><span class="detail-value">{{ currentRow.unitPriceRmb != null ? `¥${Number(currentRow.unitPriceRmb).toFixed(2)}` : '—' }}</span></div>
        <div class="detail-item"><span class="detail-label">材质</span><span class="detail-value">{{ currentRow.material || '—' }}</span></div>
        <div class="detail-item"><span class="detail-label">规格(cm)</span><span class="detail-value">{{ dims(currentRow) }}</span></div>
        <div class="detail-item"><span class="detail-label">重量</span><span class="detail-value">{{ weightStr(currentRow) }}</span></div>
        <div class="detail-item full-width"><span class="detail-label">申报要素</span><span class="detail-value">{{ currentRow.declarationElements || '—' }}</span></div>
      </div>
    </el-drawer>

    <!-- 新规/编辑弹窗 -->
    <el-dialog v-model="formVisible" :title="formTitle" width="720px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="主货号" prop="masterCode">
              <el-input v-model="form.masterCode" placeholder="如 odn012" maxlength="32" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="子货号">
              <el-input v-model="form.subCode" placeholder="如 re（红色）" maxlength="64" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="中文名称" prop="nameZh">
              <el-input v-model="form.nameZh" placeholder="中国用名称" maxlength="255" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="英文名称">
              <el-input v-model="form.nameEn" placeholder="报关用" maxlength="255" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="日文名称">
              <el-input v-model="form.nameJa" placeholder="日本用" maxlength="128" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="HS编码">
              <el-input v-model="form.hsCode" placeholder="如 9403200000" maxlength="20" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="含税单价(CNY)">
              <el-input-number v-model="form.unitPriceRmb" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="票点">
              <el-input-number v-model="form.taxPoint" :min="0" :max="10" :precision="4" style="width:100%" placeholder="默认1.1" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="增值税率">
              <el-input-number v-model="form.taxRate" :min="0" :max="1" :precision="4" style="width:100%" placeholder="默认0.1" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="单品长(cm)">
              <el-input-number v-model="form.lengthCm" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单品宽(cm)">
              <el-input-number v-model="form.widthCm" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单品高(cm)">
              <el-input-number v-model="form.heightCm" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="净重(kg)">
              <el-input-number v-model="form.netWeightKg" :min="0" :precision="4" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="毛重(kg)">
              <el-input-number v-model="form.grossWeightKg" :min="0" :precision="4" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="计量单位">
              <el-input v-model="form.unit" placeholder="个/台/套" maxlength="50" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="申报要素">
          <el-input v-model="form.declarationElements" type="textarea" :rows="2" placeholder="材质|用途|品牌" />
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="form.remarks" type="textarea" :rows="2" maxlength="512" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Goods } from '@element-plus/icons-vue'
import { productApi } from '@/api/product'
import type { ProductPageVO, CreateProductRequest, UpdateProductRequest } from '@/api/product'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<ProductPageVO[]>([])
const detailVisible = ref(false)
const formVisible = ref(false)
const currentRow = ref<ProductPageVO | null>(null)
const isEdit = ref(false)
const formRef = ref()

const pagination = reactive({ page: 1, pageSize: 20, total: 0 })

const filterForm = reactive({ masterCode: '', keyword: '', hsCode: '' })

const defaultForm = (): CreateProductRequest => ({
  masterCode: '',
  subCode: '',
  nameJa: '',
  nameEn: '',
  nameZh: '',
  hsCode: '',
  unitPriceRmb: undefined,
  taxPoint: 1.1,
  taxRate: 0.1,
  lengthCm: undefined,
  widthCm: undefined,
  heightCm: undefined,
  netWeightKg: undefined,
  grossWeightKg: undefined,
  unit: '',
  declarationElements: '',
  remarks: '',
})

const form = reactive<CreateProductRequest>(defaultForm())

const formRules = {
  masterCode: [{ required: true, message: '主货号不能为空', trigger: 'blur' }],
  nameZh: [{ required: true, message: '中文名称不能为空', trigger: 'blur' }],
}

const formTitle = computed(() => isEdit.value ? '编辑商品' : '新增商品')

async function loadData() {
  loading.value = true
  try {
    const res = await productApi.list({
      page: pagination.page,
      pageSize: pagination.pageSize,
      masterCode: filterForm.masterCode || undefined,
      keyword: filterForm.keyword || undefined,
      hsCode: filterForm.hsCode || undefined,
    })
    const data = res.data.data
    tableData.value = data.content
    pagination.total = data.totalElements
  } finally {
    loading.value = false
  }
}

function onReset() {
  filterForm.masterCode = ''
  filterForm.keyword = ''
  filterForm.hsCode = ''
  pagination.page = 1
  loadData()
}

function onNew() {
  isEdit.value = false
  Object.assign(form, defaultForm())
  formVisible.value = true
}

function onView(row: ProductPageVO) {
  currentRow.value = row
  detailVisible.value = true
}

function onEdit(row: ProductPageVO) {
  isEdit.value = true
  Object.assign(form, {
    masterCode: row.masterCode,
    subCode: row.subCode,
    nameJa: row.nameJa,
    nameEn: row.nameEn,
    nameZh: row.nameZh,
    hsCode: row.hsCode,
    unitPriceRmb: row.unitPriceRmb,
    taxPoint: row.taxPoint,
    taxRate: row.taxRate,
    lengthCm: row.lengthCm,
    widthCm: row.widthCm,
    heightCm: row.heightCm,
    netWeightKg: row.netWeightKg,
    grossWeightKg: row.grossWeightKg,
    unit: row.unit,
    declarationElements: row.declarationElements,
    remarks: row.remarks,
  })
  formVisible.value = true
}

async function onDelete(row: ProductPageVO) {
  await ElMessageBox.confirm(`确定删除商品「${row.masterCode}」？`, '确认删除')
  await productApi.delete(row.id)
  ElMessage.success('删除成功')
  loadData()
}

async function onSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value && currentRow.value) {
      await productApi.update(currentRow.value.id, form as UpdateProductRequest)
      ElMessage.success('更新成功')
    } else {
      await productApi.create(form)
      ElMessage.success('创建成功')
    }
    formVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

function onPageChange(page: number) {
  pagination.page = page
  loadData()
}

function dims(row: ProductPageVO) {
  const l = row.lengthCm, w = row.widthCm, h = row.heightCm
  if (l || w || h) return `${l ?? '—'} × ${w ?? '—'} × ${h ?? '—'} cm`
  return '—'
}

function weightStr(row: ProductPageVO) {
  const n = row.netWeightKg, g = row.grossWeightKg
  if (n || g) return `净${n ?? '—'}kg / 毛${g ?? '—'}kg`
  return '—'
}

loadData()
</script>

<style scoped>
.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  padding: 8px 16px;
}
.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.detail-item.full-width {
  grid-column: 1 / -1;
}
.detail-label {
  font-size: 12px;
  color: #909399;
}
.detail-value {
  font-size: 14px;
  color: #303133;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
