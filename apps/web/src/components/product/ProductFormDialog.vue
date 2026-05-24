<template>
  <!-- 新规/编辑商品弹窗（完整版，供 ProductPage / ProcurementPage 复用） -->
  <el-dialog
    v-model="visible"
    :title="title"
    width="840px"
    :close-on-click-modal="false"
    @closed="onClosed"
  >
    <el-form ref="formRef" :model="form" :rules="formRules" label-width="110px">
      <!-- 基本信息 -->
      <div class="dialog-section-title">{{ $t('product.drawer.section.basicInfo') }}</div>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item :label="$t('product.dialog.masterCode')" prop="masterCode">
            <el-input v-model="form.masterCode" :placeholder="$t('product.dialog.masterCodePlaceholder')" maxlength="32" :disabled="editMode" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="$t('product.dialog.subCode')">
            <el-input v-model="form.subCode" :placeholder="$t('product.dialog.subCodePlaceholder')" maxlength="64" :disabled="editMode" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="$t('product.dialog.janCode')">
            <el-input v-model="form.janCode" :placeholder="$t('product.dialog.janCodePlaceholder')" maxlength="64" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="9">
          <el-form-item :label="$t('product.dialog.nameZh')" prop="nameZh">
            <el-input v-model="form.nameZh" :placeholder="$t('product.dialog.nameZhPlaceholder')" maxlength="255" />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item :label="$t('product.dialog.oneClickTranslate')" label-width="110px">
            <el-button
              type="primary"
              size="small"
              :loading="translatingAll"
              :disabled="!form.nameZh"
              @click="onTranslateAll"
            >
              <span v-if="!translatingAll">🔄</span>
              {{ translatingAll ? $t('product.dialog.translating') : $t('product.dialog.oneClickTranslate') }}
            </el-button>
          </el-form-item>
        </el-col>
        <el-col :span="9">
          <el-form-item :label="$t('product.dialog.nameJa')">
            <el-input v-model="form.nameJa" :placeholder="$t('product.dialog.nameJaPlaceholder')" maxlength="128" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item :label="$t('product.dialog.nameEn')">
            <el-input v-model="form.nameEn" :placeholder="$t('product.dialog.nameEnPlaceholder')" maxlength="255" />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item :label="$t('product.dialog.category')">
            <el-select v-model="form.category" clearable style="width:100%">
              <el-option value="OEM" :label="$t('product.category.OEM')" />
              <el-option value="ORDINARY" :label="$t('product.category.ORDINARY')" />
              <el-option value="FACTORY_DIRECT" :label="$t('product.category.FACTORY_DIRECT')" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item :label="$t('product.dialog.status')">
            <el-input v-model="form.status" maxlength="32" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item :label="$t('product.dialog.material')">
            <el-input v-model="form.material" :placeholder="$t('product.dialog.materialPlaceholder')" maxlength="64" />
          </el-form-item>
        </el-col>
        <el-col :span="4">
          <el-form-item :label="$t('product.dialog.translate')" label-width="60px">
            <el-button
              type="info"
              size="small"
              :loading="translatingMaterial"
              :disabled="!form.material"
              @click="onTranslateMaterial"
            >{{ $t('product.dialog.translate') }}</el-button>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="$t('product.dialog.materialJa')">
            <el-input v-model="form.materialJa" :placeholder="$t('product.dialog.materialJaPlaceholder')" maxlength="255" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="$t('product.dialog.origin')">
            <el-input v-model="form.origin" :placeholder="$t('product.dialog.originPlaceholder')" maxlength="100" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item :label="$t('product.dialog.colorName')">
            <el-input v-model="form.colorName" maxlength="64" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="$t('product.dialog.unit')">
            <el-input v-model="form.unit" :placeholder="$t('product.dialog.unitPlaceholder')" maxlength="50" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="$t('product.dialog.quantities')">
            <el-input-number v-model="form.quantities" :min="0" style="width:100%" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item :label="$t('product.dialog.cartonQty')">
            <el-input-number v-model="form.cartonQty" :min="0" style="width:100%" />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 规格信息 -->
      <div class="dialog-section-title">{{ $t('product.drawer.section.specInfo') }}</div>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item :label="$t('product.dialog.unitPriceRmb')">
            <el-input-number v-model="form.unitPriceRmb" :min="0" :precision="2" style="width:100%" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="$t('product.dialog.taxPoint')">
            <el-input-number v-model="form.taxPoint" :min="0" :precision="4" style="width:100%" :placeholder="$t('product.dialog.taxPointPlaceholder')" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="$t('product.dialog.taxRate')">
            <el-input-number v-model="form.taxRate" :min="0" :max="1" :precision="4" style="width:100%" :placeholder="$t('product.dialog.taxRatePlaceholder')" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item :label="$t('product.dialog.hsCode')">
            <el-input v-model="form.hsCode" :placeholder="$t('product.dialog.hsCodePlaceholder')" maxlength="20" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="$t('product.dialog.hsCodeJp')">
            <el-input v-model="form.hsCodeJp" :placeholder="$t('product.dialog.hsCodeJpPlaceholder')" maxlength="20" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item :label="$t('product.dialog.lengthCm')">
            <el-input-number v-model="form.lengthCm" :min="0" :precision="2" style="width:100%" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="$t('product.dialog.widthCm')">
            <el-input-number v-model="form.widthCm" :min="0" :precision="2" style="width:100%" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="$t('product.dialog.heightCm')">
            <el-input-number v-model="form.heightCm" :min="0" :precision="2" style="width:100%" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item :label="$t('product.dialog.netWeight')">
            <el-input-number v-model="form.netWeightKg" :min="0" :precision="4" style="width:100%" :placeholder="$t('product.dialog.netWeightPlaceholder')" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="$t('product.dialog.grossWeight')">
            <el-input-number v-model="form.grossWeightKg" :min="0" :precision="4" style="width:100%" :placeholder="$t('product.dialog.grossWeightPlaceholder')" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item :label="$t('product.dialog.declarationElements')">
        <el-input v-model="form.declarationElements" type="textarea" :rows="2" :placeholder="$t('product.dialog.declarationElementsPlaceholder')" />
      </el-form-item>

      <el-form-item :label="$t('product.drawer.imageUrl')">
        <el-input v-model="form.imageUrl" :placeholder="$t('product.drawer.imageUrl')" maxlength="512" />
      </el-form-item>

      <!-- 仓储信息 -->
      <div class="dialog-section-title">{{ $t('product.drawer.section.warehouseInfo') }}</div>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item :label="$t('product.dialog.warehouse')">
            <el-input v-model="form.warehouse" :placeholder="$t('product.dialog.warehousePlaceholder')" maxlength="64" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="$t('product.dialog.requiresQc')">
            <el-switch v-model="form.requiresQc" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item :label="$t('product.dialog.remarks')">
        <el-input v-model="form.remarks" type="textarea" :rows="2" maxlength="512" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">{{ $t('product.dialog.cancel') }}</el-button>
      <el-button type="primary" :loading="submitting" @click="onSubmit">{{ $t('product.dialog.save') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { productApi } from '@/api/product'
import { aiApi } from '@/api/ai'
import type { CreateProductRequest, UpdateProductRequest } from '@/api/product'

const props = defineProps<{
  /** v-model: 控制显隐 */
  modelValue: boolean
  /** true = 编辑模式，false = 新建模式 */
  editMode?: boolean
  /** 编辑模式时的主货号 ID（用于 PATCH） */
  editId?: number | null
  /** 新建模式时预填 masterCode */
  prefilledMasterCode?: string
  /** 新建模式时预填 subCode */
  prefilledSubCode?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  /** 保存成功后回调，参数为是否新建了商品（用于刷新调用方数据） */
  saved: [isNew: boolean, masterCode: string]
}>()

const { t } = useI18n()
const submitting = ref(false)
const translatingAll = ref(false)
const translatingMaterial = ref(false)
const formRef = ref()

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const title = computed(() =>
  props.editMode ? t('product.dialog.editTitle') : t('product.dialog.newTitle'),
)

const defaultForm = (): CreateProductRequest => ({
  masterCode: props.prefilledMasterCode ?? '',
  subCode: props.prefilledSubCode ?? '',
  janCode: '',
  nameZh: '',
  nameEn: '',
  nameJa: '',
  material: '',
  materialJa: '',
  origin: '',
  colorName: '',
  imageUrl: '',
  category: undefined,
  status: '',
  unit: '',
  quantities: undefined,
  cartonQty: undefined,
  lengthCm: undefined,
  widthCm: undefined,
  heightCm: undefined,
  netWeightKg: undefined,
  grossWeightKg: undefined,
  unitPriceRmb: undefined,
  taxPoint: undefined,
  taxRate: undefined,
  amountRmb: undefined,
  hsCode: '',
  hsCodeJp: '',
  declarationElements: '',
  unitsPerPackage: undefined,
  packageLengthCm: undefined,
  packageWidthCm: undefined,
  packageHeightCm: undefined,
  warehouse: '',
  requiresQc: false,
  remarks: '',
})

const form = reactive<CreateProductRequest>(defaultForm())

const formRules = {
  nameZh: [{ required: true, message: () => t('product.validation.masterCodeRequired'), trigger: 'blur' }],
}

function resetForm() {
  Object.assign(form, defaultForm())
  form.masterCode = props.prefilledMasterCode ?? ''
  form.subCode = props.prefilledSubCode ?? ''
}

watch(() => props.modelValue, async (v) => {
  if (!v) return
  if (props.editMode && props.editId != null) {
    // 编辑模式：加载已有数据
    try {
      const res = await productApi.get(props.editId)
      const p = res.data
      Object.assign(form, defaultForm())
      form.masterCode = p.masterCode ?? ''
      form.subCode = p.subCode ?? ''
      form.janCode = p.janCode ?? ''
      form.nameZh = p.nameZh ?? ''
      form.nameEn = p.nameEn ?? ''
      form.nameJa = p.nameJa ?? ''
      form.material = p.material ?? ''
      form.materialJa = p.materialJa ?? ''
      form.origin = p.origin ?? ''
      form.colorName = p.colorName ?? ''
      form.imageUrl = p.imageUrl ?? ''
      form.category = p.category
      form.status = p.status ?? ''
      form.unit = p.unit ?? ''
      form.quantities = p.quantities
      form.cartonQty = p.cartonQty
      form.lengthCm = p.lengthCm
      form.widthCm = p.widthCm
      form.heightCm = p.heightCm
      form.netWeightKg = p.netWeightKg
      form.grossWeightKg = p.grossWeightKg
      form.unitPriceRmb = p.unitPriceRmb
      form.taxPoint = p.taxPoint
      form.taxRate = p.taxRate
      form.amountRmb = p.amountRmb
      form.hsCode = p.hsCode ?? ''
      form.hsCodeJp = p.hsCodeJp ?? ''
      form.declarationElements = p.declarationElements ?? ''
      form.warehouse = p.warehouse ?? ''
      form.requiresQc = p.requiresQc ?? false
      form.remarks = p.remarks ?? ''
    } catch {
      ElMessage.error(t('order.productCreateDialog.loadError'))
      visible.value = false
    }
  } else {
    // 新建模式：重置表单，预填 masterCode / subCode
    resetForm()
  }
})

async function onTranslateAll() {
  if (!form.nameZh) return
  translatingAll.value = true
  try {
    const promises = [aiApi.translateZhToJa({ sourceText: form.nameZh, targetLang: 'ja' })]
    promises.push(aiApi.translateZhToJa({ sourceText: form.nameZh, targetLang: 'en' }))
    const results = await Promise.all(promises)
    const jaRes = results[0]
    const enRes = results[1]
    if (jaRes.data?.nameJa) form.nameJa = jaRes.data.nameJa
    if (enRes.data?.nameEn) form.nameEn = enRes.data.nameEn
    ElMessage.success(t('product.dialog.translateSuccess'))
  } catch {
    ElMessage.error(t('product.dialog.translateError'))
  } finally {
    translatingAll.value = false
  }
}

async function onTranslateMaterial() {
  if (!form.material) return
  translatingMaterial.value = true
  try {
    const res = await aiApi.translateZhToJa({ sourceText: form.material, targetLang: 'ja' })
    if (res.data?.nameJa) {
      form.materialJa = res.data.nameJa
      ElMessage.success(t('product.dialog.translateSuccess'))
    }
  } catch {
    ElMessage.error(t('product.dialog.translateError'))
  } finally {
    translatingMaterial.value = false
  }
}

function onClosed() {
  formRef.value?.resetFields()
  Object.assign(form, defaultForm())
}

async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (props.editMode && props.editId != null) {
      await productApi.update(props.editId, form as UpdateProductRequest)
      ElMessage.success(t('common.message.updateSuccess'))
    } else {
      await productApi.create(form)
      ElMessage.success(t('common.message.createSuccess'))
    }
    visible.value = false
    emit('saved', !props.editMode, form.masterCode || form.subCode || '')
  } catch {
    // error handled by interceptor
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.dialog-section-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-primary);
  margin: 8px 0 6px;
  padding-bottom: 4px;
  border-bottom: 1px solid var(--border-color);
}
</style>
