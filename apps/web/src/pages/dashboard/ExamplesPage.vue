<template>
  <div class="examples-page">
    <h2 class="page-title">示例列表</h2>

    <!-- 操作栏 -->
    <el-card shadow="never" class="toolbar-card">
      <el-space wrap>
        <el-button type="primary" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          新建
        </el-button>
        <el-input
          v-model="keyword"
          placeholder="搜索名称..."
          clearable
          style="width: 220px"
          @clear="loadItems"
          @keyup.enter="loadItems"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button @click="loadItems">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </el-space>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="hover" class="table-card">
      <el-table :data="items" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEditDialog(row)">
              编辑
            </el-button>
            <el-button link type="danger" size="small" @click="deleteRow(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="totalElements"
          layout="total, prev, pager, next"
          @current-change="loadItems"
        />
      </div>
    </el-card>

    <!-- 新建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑' : '新建'"
      width="500px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import client from '@/api/client'

interface ExampleItem {
  id: number
  name: string
  description: string
  status: string
  createTime: string
}

const items = ref<ExampleItem[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(20)
const totalElements = ref(0)
const keyword = ref('')

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  name: '',
  description: '',
})

const rules: FormRules = {
  name: [{ required: true, message: '名称不能为空', trigger: 'blur' }],
}

onMounted(loadItems)

async function loadItems() {
  loading.value = true
  try {
    const params: Record<string, unknown> = { page: page.value, size: pageSize.value }
    if (keyword.value) params.name = keyword.value

    const res = await client.get<{ data: { content: ExampleItem[]; totalElements: number } }>(
      '/examples',
      { params },
    )
    items.value = res.data.data?.content ?? []
    totalElements.value = res.data.data?.totalElements ?? 0
  } catch (err) {
    console.error('[ExamplesPage] loadItems failed', err)
    ElMessage.error('加载数据失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  editingId.value = null
  Object.assign(form, { name: '', description: '' })
  dialogVisible.value = true
}

function openEditDialog(row: ExampleItem) {
  editingId.value = row.id
  Object.assign(form, { name: row.name, description: row.description })
  dialogVisible.value = true
}

async function deleteRow(row: ExampleItem) {
  await client.delete(`/examples/${row.id}`)
  ElMessage.success('删除成功')
  loadItems()
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (editingId.value) {
      await client.put(`/examples/${editingId.value}`, form)
      ElMessage.success('更新成功')
    } else {
      await client.post('/examples', form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadItems()
  } catch (err) {
    console.error('[ExamplesPage] submitForm failed', err)
    ElMessage.error('提交失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.page-title {
  margin: 0 0 16px;
  font-size: 22px;
  color: #303133;
}

.toolbar-card {
  margin-bottom: 12px;
  border-radius: 8px;
}

.table-card {
  border-radius: 8px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
