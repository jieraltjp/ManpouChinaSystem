<template>
  <div class="page">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#409EFF"><User /></el-icon></div>
            <div>
              <div class="stat-value">{{ stats.total }}</div>
              <div class="stat-label">{{ $t('user.stat.total') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#67C23A"><CircleCheck /></el-icon></div>
            <div>
              <div class="stat-value">{{ stats.normal }}</div>
              <div class="stat-label">{{ $t('user.stat.normal') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#909399"><Lock /></el-icon></div>
            <div>
              <div class="stat-value">{{ stats.disabled }}</div>
              <div class="stat-label">{{ $t('user.stat.disabled') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><el-icon class="stat-icon" color="#E6A23C"><Clock /></el-icon></div>
            <div>
              <div class="stat-value">{{ stats.todayLogin }}</div>
              <div class="stat-label">{{ $t('user.stat.todayLogin') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filterForm">
        <el-form-item>
          <el-input v-model="filterForm.keyword" :placeholder="$t('user.filter.keyword')" clearable style="width:180px" />
        </el-form-item>
        <el-form-item :label="$t('user.filter.status')">
          <el-select v-model="filterForm.status" :placeholder="$t('user.filter.status')" clearable style="width:120px">
            <el-option :label="$t('user.status.normal')" :value="1" />
            <el-option :label="$t('user.status.disabled')" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">{{ $t('common.button.search') }}</el-button>
          <el-button @click="onReset">{{ $t('common.button.reset') }}</el-button>
          <el-button type="primary" @click="onNew">
            <el-icon><Plus /></el-icon>{{ $t('user.newButton') }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="tableData" stripe style="width:100%" min-height="200">
        <el-table-column prop="userCode" :label="$t('user.column.userCode')" min-width="100" />
        <el-table-column prop="username" :label="$t('user.column.username')" min-width="120" />
        <el-table-column prop="nameCn" :label="$t('user.column.nameCn')" min-width="100" />
        <el-table-column prop="nameJp" :label="$t('user.column.nameJp')" min-width="100" />
        <el-table-column prop="email" :label="$t('user.column.email')" min-width="180" show-overflow-tooltip />
        <el-table-column prop="phone" :label="$t('user.column.phone')" min-width="130" />
        <el-table-column prop="departmentName" :label="$t('user.column.department')" min-width="110" />
        <el-table-column :label="$t('user.column.roles')" min-width="120">
          <template #default="{ row }">
            <template v-if="row.roles?.length">
              <el-tag v-for="r in row.roles.slice(0, 1)" :key="r.id" size="small" type="info">
                {{ currentLocale === 'ja' ? r.roleNameJp : r.roleNameCn }}
              </el-tag>
              <el-tag v-if="row.roles.length > 1" size="small" type="info">+{{ row.roles.length - 1 }}</el-tag>
            </template>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('user.column.status')" min-width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? $t('user.status.normal') : $t('user.status.disabled') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('user.column.action')" min-width="220" align="center">
          <template #default="{ row }">
            <el-button link class="btn-blue" size="small" @click.stop="onView(row)">{{ $t('user.action.detail') }}</el-button>
            <el-button link class="btn-blue" size="small" @click.stop="onEdit(row)">{{ $t('user.action.edit') }}</el-button>
            <el-button link type="warning" size="small" @click.stop="onAssignRole(row)">{{ $t('user.action.assignRole') }}</el-button>
            <el-button link :type="row.status === 1 ? 'danger' : 'success'" size="small" @click.stop="onToggleStatus(row)">
              {{ row.status === 1 ? $t('user.action.disable') : $t('user.action.enable') }}
            </el-button>
            <el-button link class="btn-blue" size="small" @click.stop="onResetPwd(row)">
              {{ $t('user.action.resetPassword') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next"
          @size-change="loadUsers"
          @current-change="loadUsers"
        />
      </div>
    </el-card>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" :title="$t('user.dialog.detailTitle')" size="480px">
      <el-descriptions :column="1" border>
        <el-descriptions-item :label="$t('user.column.userCode')">{{ currentUser?.userCode }}</el-descriptions-item>
        <el-descriptions-item :label="$t('user.column.username')">{{ currentUser?.username }}</el-descriptions-item>
        <el-descriptions-item :label="$t('user.column.nameCn')">{{ currentUser?.nameCn || '—' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('user.column.nameJp')">{{ currentUser?.nameJp || '—' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('user.column.email')">{{ currentUser?.email }}</el-descriptions-item>
        <el-descriptions-item :label="$t('user.column.phone')">{{ currentUser?.phone || '—' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('user.column.department')">{{ currentUser?.departmentName || '—' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('user.column.roles')">
          <template v-if="currentUser?.roles?.length">
            <el-tag v-for="r in currentUser.roles" :key="r.id" size="small" type="info" style="margin-right:4px">
              {{ currentLocale === 'ja' ? r.roleNameJp : r.roleNameCn }}
            </el-tag>
          </template>
          <span v-else>—</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('user.dialog.customsCode')">{{ currentUser?.customsCode || '—' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('user.dialog.customsLicense')">{{ currentUser?.customsLicense || '—' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('user.column.lastLoginTime')">{{ currentUser?.lastLoginTime || '—' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('user.column.lastLoginIp')">{{ currentUser?.lastLoginIp || '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="editVisible" :title="editMode === 'create' ? $t('user.dialog.createTitle') : $t('user.dialog.editTitle')" width="560px">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="100px">
        <el-form-item :label="$t('user.dialog.username')" prop="username">
          <el-input v-model="editForm.username" :placeholder="$t('user.dialog.username')" :disabled="editMode === 'update'" />
        </el-form-item>
        <el-form-item v-if="editMode === 'create'" :label="$t('user.dialog.password')" prop="password">
          <el-input v-model="editForm.password" type="password" :placeholder="$t('user.dialog.password')" show-password />
        </el-form-item>
        <el-form-item :label="$t('user.dialog.nameCn')" prop="nameCn">
          <el-input v-model="editForm.nameCn" :placeholder="$t('user.dialog.nameCn')" />
        </el-form-item>
        <el-form-item :label="$t('user.dialog.nameJp')" prop="nameJp">
          <el-input v-model="editForm.nameJp" :placeholder="$t('user.dialog.nameJp')" />
        </el-form-item>
        <el-form-item :label="$t('user.dialog.email')" prop="email">
          <el-input v-model="editForm.email" :placeholder="$t('user.dialog.email')" />
        </el-form-item>
        <el-form-item :label="$t('user.dialog.phone')" prop="phone">
          <el-input v-model="editForm.phone" :placeholder="$t('user.dialog.phone')" />
        </el-form-item>
        <el-form-item :label="$t('user.dialog.customsCode')" prop="customsCode">
          <el-input v-model="editForm.customsCode" :placeholder="$t('user.dialog.customsCode')" />
        </el-form-item>
        <el-form-item :label="$t('user.dialog.customsLicense')" prop="customsLicense">
          <el-input v-model="editForm.customsLicense" :placeholder="$t('user.dialog.customsLicense')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">{{ $t('user.dialog.cancel') }}</el-button>
        <el-button type="primary" :loading="editSaving" @click="onSaveEdit">{{ $t('user.dialog.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色弹窗 -->
    <el-dialog v-model="roleVisible" :title="$t('user.dialog.roleTitle')" width="400px">
      <el-checkbox-group v-model="selectedRoleIds">
        <el-checkbox v-for="r in allRoles" :key="r.id" :value="r.id" style="display:block;margin-bottom:8px">
          {{ currentLocale === 'ja' ? r.roleNameJp : r.roleNameCn }}
          <span style="color:#909399;font-size:12px"> ({{ r.roleCode }})</span>
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="roleVisible = false">{{ $t('user.dialog.cancel') }}</el-button>
        <el-button type="primary" :loading="roleSaving" @click="onSaveRoles">{{ $t('user.dialog.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="pwdVisible" :title="$t('user.dialog.passwordTitle')" width="400px">
      <el-alert v-if="newPassword" type="success" :closable="false">
        <template #title>
          <div>{{ $t('user.message.resetPasswordSuccess') }}</div>
          <div style="font-family:monospace;font-size:18px;margin-top:8px;letter-spacing:2px">{{ newPassword }}</div>
        </template>
      </el-alert>
      <div v-else style="text-align:center;color:#909399">—</div>
      <template #footer>
        <el-button @click="pwdVisible = false">{{ $t('user.dialog.cancel') }}</el-button>
        <el-button type="primary" :loading="pwdLoading" @click="doResetPassword" :disabled="!!newPassword">
          {{ $t('user.action.resetPassword') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { User, Plus, Lock, Clock, CircleCheck } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import * as userApi from '@/api/user'
import * as roleApi from '@/api/role'
import type { UserVO, UserCreateCmd, UserUpdateCmd } from '@/api/user'
import type { RoleVO } from '@/api/role'

const { t, locale } = useI18n()
const currentLocale = computed(() => locale.value)

// ===== 状态 =====
const loading = ref(false)
const tableData = ref<UserVO[]>([])
const filterForm = reactive({ keyword: '', status: undefined as number | undefined })
const pagination = reactive({ page: 1, size: 20, total: 0 })
const stats = reactive({ total: 0, normal: 0, disabled: 0, todayLogin: 0 })

const editVisible = ref(false)
const editMode = ref<'create' | 'update'>('create')
const editSaving = ref(false)
const editFormRef = ref<FormInstance>()
const editForm = reactive<Partial<UserCreateCmd & UserUpdateCmd & { password: string }>>({})
const editRules: FormRules = {
  username: [{ required: true, message: t('user.validation.usernameRequired'), trigger: 'blur' }],
  password: [{ required: true, message: t('user.validation.passwordRequired'), trigger: 'blur' }],
  email: [
    { required: true, message: t('user.validation.emailRequired'), trigger: 'blur' },
    { type: 'email', message: t('user.validation.emailFormat'), trigger: 'blur' },
  ],
}

const detailVisible = ref(false)
const currentUser = ref<UserVO | null>(null)

const roleVisible = ref(false)
const roleSaving = ref(false)
const allRoles = ref<RoleVO[]>([])
const selectedRoleIds = ref<number[]>([])
const currentRoleUserId = ref<number | null>(null)

const pwdVisible = ref(false)
const pwdLoading = ref(false)
const newPassword = ref('')

// ===== 方法 =====
async function loadUsers() {
  loading.value = true
  try {
    const res = await userApi.pageUsers({
      keyword: filterForm.keyword || undefined,
      status: filterForm.status,
      page: pagination.page - 1,
      size: pagination.size,
    })
    tableData.value = res.content ?? []
    pagination.total = res.totalElements ?? 0
    // 统计
    stats.total = res.totalElements ?? 0
    stats.normal = (res.content ?? []).filter(u => u.status === 1).length
    stats.disabled = (res.content ?? []).filter(u => u.status === 0).length
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

function onSearch() {
  pagination.page = 1
  loadUsers()
}

function onReset() {
  filterForm.keyword = ''
  filterForm.status = undefined
  pagination.page = 1
  loadUsers()
}

function onNew() {
  editMode.value = 'create'
  Object.assign(editForm, { username: '', password: '', nameCn: '', nameJp: '', email: '', phone: '', customsCode: '', customsLicense: '' })
  editVisible.value = true
}

async function onEdit(row: UserVO) {
  editMode.value = 'update'
  const full = await userApi.getUser(row.id)
  currentUser.value = full
  Object.assign(editForm, {
    nameCn: full.nameCn ?? '',
    nameJp: full.nameJp ?? '',
    email: full.email ?? '',
    phone: full.phone ?? '',
    customsCode: full.customsCode ?? '',
    customsLicense: full.customsLicense ?? '',
  })
  editVisible.value = true
}

function onView(row: UserVO) {
  currentUser.value = row
  detailVisible.value = true
}

async function onSaveEdit() {
  if (!editFormRef.value) return
  await editFormRef.value.validate()
  editSaving.value = true
  try {
    if (editMode.value === 'create') {
      await userApi.createUser(editForm as UserCreateCmd)
      ElMessage.success(t('user.message.createSuccess'))
    } else {
      await userApi.updateUser(currentUser.value!.id, editForm as UserUpdateCmd)
      ElMessage.success(t('user.message.updateSuccess'))
    }
    editVisible.value = false
    loadUsers()
  } catch {
    // error handled
  } finally {
    editSaving.value = false
  }
}

async function onToggleStatus(row: UserVO) {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 0 ? t('user.action.disable') : t('user.action.enable')
  try {
    await ElMessageBox.confirm('', action)
    await userApi.updateUserStatus(row.id, { status: newStatus })
    ElMessage.success(action + ' ' + t('common.success'))
    loadUsers()
  } catch {
    // cancelled
  }
}

async function onAssignRole(row: UserVO) {
  currentRoleUserId.value = row.id
  selectedRoleIds.value = row.roles?.map(r => r.id) ?? []
  if (!allRoles.value.length) {
    allRoles.value = await roleApi.listRoles()
  }
  roleVisible.value = true
}

async function onSaveRoles() {
  if (!currentRoleUserId.value) return
  roleSaving.value = true
  try {
    await userApi.assignUserRoles(currentRoleUserId.value, { roleIds: selectedRoleIds.value })
    ElMessage.success(t('common.success'))
    roleVisible.value = false
    loadUsers()
  } catch {
    // error handled
  } finally {
    roleSaving.value = false
  }
}

function onResetPwd(row: UserVO) {
  currentUser.value = row
  newPassword.value = ''
  pwdVisible.value = true
}

async function doResetPassword() {
  if (!currentUser.value) return
  pwdLoading.value = true
  try {
    const res = await userApi.resetUserPassword(currentUser.value.id)
    newPassword.value = res.newPassword
  } catch {
    // error handled
  } finally {
    pwdLoading.value = false
  }
}

loadUsers()
</script>

<style scoped>
.page { }
.stats-row { margin-bottom: 16px; }
.stat-card { cursor: default; }
.stat-content { display:flex; align-items:center; gap:12px; }
.stat-icon-wrap { width:40px; height:40px; border-radius:8px; display:flex; align-items:center; justify-content:center; background:#f5f7fa; }
.stat-icon { font-size:20px; }
.stat-value { font-size:22px; font-weight:700; color:#303133; }
.stat-label { font-size:13px; color:#909399; margin-top:2px; }
.filter-card { margin-bottom: 16px; }
.table-card { }
.pagination-wrap { display:flex; justify-content:flex-end; margin-top:16px; }
.btn-blue { color:#409EFF; }
</style>
