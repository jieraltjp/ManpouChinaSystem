<template>
  <div class="page">
    <div class="page-header">
      <h2 class="page-title">工厂管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="onNew">
          <el-icon><Plus /></el-icon> 新增工厂
        </el-button>
      </div>
    </div>

    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="工厂名称">
          <el-input v-model="filterForm.factoryName" placeholder="工厂名称" clearable style="width:160px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="全部" clearable style="width:140px">
            <el-option value="ACTIVE" label="合作中" />
            <el-option value="INACTIVE" label="已停止" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="tableData" stripe style="width:100%">
        <el-table-column prop="factoryCode" label="工厂编号" width="160" />
        <el-table-column prop="factoryName" label="工厂名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="location" label="工厂位置" min-width="140" show-overflow-tooltip />
        <el-table-column prop="roughLocation" label="粗略位置" min-width="140" show-overflow-tooltip />
        <el-table-column prop="contactName" label="联系人" width="120" />
        <el-table-column prop="contactPhone" label="联系电话" width="140" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ row.status === 'ACTIVE' ? '合作中' : '已停止' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="onEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click.stop="onDelete(row)">删除</el-button>
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

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增工厂' : '编辑工厂'" width="560px">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
        <el-form-item label="工厂名称" prop="factoryName">
          <el-input v-model="formData.factoryName" placeholder="工厂全称" />
        </el-form-item>
        <el-form-item label="工厂位置">
          <el-input v-model="formData.location" placeholder="省/市，如 浙江省金华市" />
        </el-form-item>
        <el-form-item label="粗略位置">
          <el-input v-model="formData.roughLocation" placeholder="工业区/镇/园区" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="formData.contactName" placeholder="联系人姓名" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="formData.contactPhone" placeholder="手机或座机" />
        </el-form-item>
        <el-form-item v-if="dialogMode === 'update'" label="状态">
          <el-select v-model="formData.status" style="width:100%">
            <el-option value="ACTIVE" label="合作中" />
            <el-option value="INACTIVE" label="已停止" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { factoryApi, type FactoryPageVO, type CreateFactoryRequest, type UpdateFactoryRequest, type FactoryStatus } from '@/api/factory'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'update'>('create')
const currentRow = ref<FactoryPageVO | null>(null)
const formRef = ref<FormInstance>()

const filterForm = reactive({ factoryName: '', status: '' })
const pagination = reactive({ page: 1, pageSize: 20, total: 0 })
const tableData = ref<FactoryPageVO[]>([])

const defaultFormData = (): CreateFactoryRequest & { status?: FactoryStatus } => ({
  factoryName: '',
  location: '',
  roughLocation: '',
  contactName: '',
  contactPhone: '',
})
const formData = reactive<CreateFactoryRequest & { status?: FactoryStatus }>(defaultFormData())

const formRules = {
  factoryName: [{ required: true, message: '工厂名称不能为空', trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    const res = await factoryApi.list({
      page: pagination.page - 1,
      pageSize: pagination.pageSize,
      factoryName: filterForm.factoryName.trim() || undefined,
      status: filterForm.status || undefined,
    })
    const payload = res.data.data as { content: FactoryPageVO[]; totalElements: number }
    tableData.value = payload.content || []
    pagination.total = payload.totalElements || 0
  } catch { /* interceptor */ } finally {
    loading.value = false
  }
}

function onReset() {
  filterForm.factoryName = ''
  filterForm.status = ''
  pagination.page = 1
  loadData()
}

function onNew() {
  dialogMode.value = 'create'
  Object.assign(formData, defaultFormData())
  dialogVisible.value = true
}

function onEdit(row: FactoryPageVO) {
  dialogMode.value = 'update'
  currentRow.value = row
  Object.assign(formData, {
    factoryName: row.factoryName,
    location: row.location || '',
    roughLocation: row.roughLocation || '',
    contactName: row.contactName || '',
    contactPhone: row.contactPhone || '',
    status: row.status,
  })
  dialogVisible.value = true
}

async function onDelete(row: FactoryPageVO) {
  try {
    await ElMessageBox.confirm(`确认删除工厂「${row.factoryName}」？`, '删除确认', { type: 'warning' })
  } catch { return }
  try {
    await factoryApi.delete(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch { /* interceptor */ }
}

async function onSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    submitting.value = true
    try {
      if (dialogMode.value === 'create') {
        await factoryApi.create(formData as CreateFactoryRequest)
        ElMessage.success('工厂创建成功')
      } else if (currentRow.value) {
        await factoryApi.update(currentRow.value.id, formData as UpdateFactoryRequest)
        ElMessage.success('更新成功')
      }
      dialogVisible.value = false
      loadData()
    } finally {
      submitting.value = false
    }
  })
}

onMounted(() => loadData())
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.page-header { display: flex; align-items: center; justify-content: space-between; }
.page-title { margin: 0; font-size: 20px; font-weight: 700; color: var(--text-primary); }
.page-title::before { content: ''; display: inline-block; width: 4px; height: 20px; background: var(--color-primary); border-radius: 2px; margin-right: 10px; vertical-align: middle; }
.filter-card :deep(.el-card__body) { padding-bottom: 0; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
