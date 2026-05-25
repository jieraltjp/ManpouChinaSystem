<template>
  <div class="role-page">
    <!-- 角色表格 -->
    <el-card shadow="never" class="role-card">
      <div class="role-card-header">
        <span class="section-title">{{ $t('role.title') }}</span>
        <el-button v-if="hasPermission('role:create')" type="primary" size="small" @click="onNew">
          <el-icon><Plus /></el-icon>{{ $t('role.newButton') }}
        </el-button>
      </div>
      <el-table
        ref="roleTableRef"
        :data="allRoles"
        :row-class-name="getRowClassName"
        highlight-current-row
        @row-click="onRowClick"
        stripe
        style="width: 100%"
      >
        <el-table-column :label="$t('role.column.roleName')" min-width="140">
          <template #default="{ row }">
            <div class="role-name-cell">
              <span class="role-name">{{ currentLocale === 'ja' ? row.roleNameJp : row.roleNameCn }}</span>
              <el-tag v-if="row.isEditable === 0" size="small" type="warning" effect="plain">
                <el-icon><Lock /></el-icon>{{ $t('role.tag.system') }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="$t('role.column.roleCode')" prop="roleCode" min-width="120" />
        <el-table-column :label="$t('role.column.roleType')" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.roleType === 'SYSTEM' ? 'warning' : 'primary'" effect="plain">
              {{ row.roleType === 'SYSTEM' ? $t('role.type.SYSTEM') : $t('role.type.BUSINESS') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('role.column.userCount')" prop="userCount" width="80" align="center" />
        <el-table-column :label="$t('role.column.actions')" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="hasPermission('role:update')"
              link
              class="btn-blue"
              size="small"
              @click.stop="onEdit(row)"
            >{{ $t('role.action.edit') }}</el-button>
            <el-button
              v-if="row.isEditable !== 0 && hasPermission('role:delete')"
              link
              type="danger"
              size="small"
              @click.stop="onDelete(row)"
            >{{ $t('role.action.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 权限配置面板 -->
    <el-card shadow="never" class="perm-card" :class="{ 'perm-card--active': !!selectedRole }">
      <template #header>
        <div class="perm-header">
          <div class="perm-header-left">
            <el-icon class="perm-header-icon"><Key /></el-icon>
            <div>
              <div class="perm-header-title">
                {{ selectedRole ? (currentLocale === 'ja' ? selectedRole.roleNameJp : selectedRole.roleNameCn) : $t('role.permission.selectTip') }}
              </div>
              <div v-if="selectedRole" class="perm-header-sub">
                {{ selectedRole.roleCode }} &nbsp;·&nbsp; {{ checkedPermIds.size }} {{ $t('role.permission.selectedCount') }}
              </div>
            </div>
          </div>
          <div class="perm-header-right">
            <el-tag v-if="selectedRole?.isEditable === 0" type="warning" size="small" effect="plain">
              <el-icon><Lock /></el-icon>{{ $t('role.tag.readonly') }}
            </el-tag>
            <el-button
              v-if="selectedRole && selectedRole.isEditable !== 0 && hasPermission('role:assign')"
              type="primary"
              size="small"
              :loading="permSaving"
              @click="onSavePermissions"
            >
              <el-icon><CircleCheck /></el-icon>{{ $t('role.dialog.save') }}
            </el-button>
          </div>
        </div>
      </template>

      <!-- 未选角色空状态 -->
      <div v-if="!selectedRole" class="perm-empty">
        <el-icon class="perm-empty-icon"><Key /></el-icon>
        <div class="perm-empty-text">{{ $t('role.permission.selectTip') }}</div>
      </div>

      <!-- 权限加载中 -->
      <div v-else-if="permLoading" v-loading="permLoading" class="perm-loading" />

      <!-- 权限列表 -->
      <div v-else class="perm-content">
        <div
          v-for="mod in permTreeData"
          :key="mod.module"
          class="perm-module-group"
        >
          <!-- 模块标题行 -->
          <div class="perm-module-header">
            <el-checkbox
              v-if="selectedRole?.isEditable !== 0"
              :model-value="mod.children?.every((p: PermNode) => checkedPermIds.has(p.id))"
              :indeterminate="mod.children?.some((p: PermNode) => checkedPermIds.has(p.id)) && !mod.children?.every((p: PermNode) => checkedPermIds.has(p.id))"
              @change="(val: boolean) => toggleModulePerms(mod, val)"
              @click.stop
            />
            <el-icon class="perm-module-icon"><FolderOpened /></el-icon>
            <span class="perm-module-name">{{ mod.label }}</span>
            <span class="perm-module-count">
              {{ mod.children?.filter((p: PermNode) => checkedPermIds.has(p.id)).length ?? 0 }}/{{ mod.children?.length ?? 0 }}
            </span>
          </div>

          <!-- 权限表格 -->
          <el-table
            :data="mod.children"
            :row-key="(row: PermNode) => row.id"
            size="small"
            class="perm-table"
            :header-cell-style="{ background: '#f5f7fa', color: '#606266', fontWeight: '600', fontSize: '12px' }"
          >
            <el-table-column width="50" align="center">
              <template #default="{ row }">
                <el-checkbox
                  :model-value="checkedPermIds.has(row.id)"
                  :disabled="selectedRole?.isEditable === 0"
                  @change="(val: boolean) => togglePerm(row.id, val)"
                  @click.stop
                />
              </template>
            </el-table-column>
            <el-table-column :label="$t('role.permission.column.name')" min-width="160">
              <template #default="{ row }">
                <span class="perm-name">{{ row.label }}</span>
              </template>
            </el-table-column>
            <el-table-column :label="$t('role.permission.column.code')" min-width="200">
              <template #default="{ row }">
                <code class="perm-code">{{ row.code }}</code>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-card>

    <!-- 新增/编辑角色弹窗 -->
    <el-dialog
      v-model="editVisible"
      :title="editMode === 'create' ? $t('role.dialog.createTitle') : $t('role.dialog.editTitle')"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="110px">
        <el-form-item :label="$t('role.dialog.roleCode')" prop="roleCode">
          <el-input v-model="editForm.roleCode" :disabled="editMode === 'update'" :placeholder="$t('role.dialog.roleCodeHint')" />
        </el-form-item>
        <el-form-item :label="$t('role.dialog.roleNameCn')" prop="roleNameCn">
          <el-input v-model="editForm.roleNameCn" :placeholder="$t('role.dialog.roleNameCnHint')" />
        </el-form-item>
        <el-form-item :label="$t('role.dialog.roleNameJp')">
          <el-input v-model="editForm.roleNameJp" :placeholder="$t('role.dialog.roleNameJpHint')" />
        </el-form-item>
        <el-form-item v-if="editMode === 'create'" :label="$t('role.dialog.roleType')" prop="roleType">
          <el-select v-model="editForm.roleType" style="width:100%">
            <el-option :label="$t('role.type.SYSTEM')" value="SYSTEM" />
            <el-option :label="$t('role.type.BUSINESS')" value="BUSINESS" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('role.dialog.description')">
          <el-input v-model="editForm.description" type="textarea" :rows="2" :placeholder="$t('role.dialog.descriptionHint')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">{{ $t('role.dialog.cancel') }}</el-button>
        <el-button type="primary" :loading="editSaving" @click="onSaveEdit">{{ $t('role.dialog.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Lock, Key, CircleCheck, FolderOpened } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { usePermission } from '@/composables/usePermission'
import * as roleApi from '@/api/role'
import type { RoleVO, RoleCreateCmd, RoleUpdateCmd } from '@/api/role'

const { t, locale } = useI18n()
const currentLocale = computed(() => locale.value)
const { hasPermission } = usePermission()

interface PermNode {
  id: number
  label: string
  code: string
  children?: PermNode[]
  disabled?: boolean
  module?: string
}

// ===== 状态 =====
const allRoles = ref<RoleVO[]>([])
const selectedRole = ref<RoleVO | null>(null)
const permTreeData = ref<PermNode[]>([])
const permLoading = ref(false)
const permSaving = ref(false)
const checkedPermIds = ref<Set<number>>(new Set())

const editVisible = ref(false)
const editMode = ref<'create' | 'update'>('create')
const editSaving = ref(false)
const editFormRef = ref<FormInstance>()
const editForm = reactive<Partial<RoleCreateCmd & RoleUpdateCmd>>({})
const editRules: FormRules = {
  roleCode: [{ required: true, message: t('role.validation.roleCodeRequired'), trigger: 'blur' }],
  roleNameCn: [{ required: true, message: t('role.validation.roleNameCnRequired'), trigger: 'blur' }],
}

// ===== 角色表格 =====
const roleTableRef = ref()

async function loadRoles() {
  allRoles.value = await roleApi.listRoles()
}

function getRowClassName({ row }: { row: RoleVO }) {
  return selectedRole.value?.id === row.id ? 'current-role-row' : ''
}

function onRowClick(row: RoleVO) {
  selectedRole.value = row
  loadPermissionTree()
}

// ===== 权限面板 =====
async function loadPermissionTree() {
  if (!selectedRole.value) return
  permLoading.value = true
  try {
    const [modules, role] = await Promise.all([
      roleApi.getPermissionTree(),
      roleApi.getRole(selectedRole.value.id),
    ])
    selectedRole.value = role

    permTreeData.value = modules.map(mod => ({
      module: mod.module,
      id: -mod.module.charCodeAt(0),
      label: (currentLocale.value === 'ja' ? mod.moduleNameJp : mod.moduleNameCn) ?? mod.module,
      code: mod.module,
      children: mod.permissions.map(p => ({
        id: p.id,
        label: (currentLocale.value === 'ja' ? p.permissionNameJp : p.permissionNameCn) ?? p.permissionCode,
        code: p.permissionCode,
        module: mod.module,
      })),
    }))

    checkedPermIds.value = new Set(role.permissions?.map(p => p.id) ?? [])
  } catch {
    // error handled
  } finally {
    permLoading.value = false
  }
}

function togglePerm(id: number, checked: boolean) {
  const next = new Set(checkedPermIds.value)
  if (checked) next.add(id)
  else next.delete(id)
  checkedPermIds.value = next
}

function toggleModulePerms(mod: PermNode, checked: boolean) {
  const next = new Set(checkedPermIds.value)
  mod.children?.forEach(p => {
    if (checked) next.add(p.id)
    else next.delete(p.id)
  })
  checkedPermIds.value = next
}

async function onSavePermissions() {
  if (!selectedRole.value) return
  permSaving.value = true
  try {
    await roleApi.assignRolePermissions(selectedRole.value.id, { permissionIds: Array.from(checkedPermIds.value) })
    ElMessage.success(t('role.message.permissionSaved'))
  } catch {
    // error handled
  } finally {
    permSaving.value = false
  }
}

// ===== 编辑角色 =====
function onNew() {
  editMode.value = 'create'
  Object.assign(editForm, { roleCode: '', roleNameCn: '', roleNameJp: '', roleType: 'BUSINESS', description: '' })
  editVisible.value = true
}

function onEdit(role: RoleVO) {
  editMode.value = 'update'
  Object.assign(editForm, {
    roleNameCn: role.roleNameCn ?? '',
    roleNameJp: role.roleNameJp ?? '',
    description: role.description ?? '',
  })
  selectedRole.value = role
  editVisible.value = true
}

async function onSaveEdit() {
  if (!editFormRef.value) return
  await editFormRef.value.validate()
  editSaving.value = true
  try {
    if (editMode.value === 'create') {
      await roleApi.createRole(editForm as RoleCreateCmd)
      ElMessage.success(t('role.message.createSuccess'))
    } else if (selectedRole.value) {
      await roleApi.updateRole(selectedRole.value.id, editForm as RoleUpdateCmd)
      ElMessage.success(t('role.message.updateSuccess'))
    }
    editVisible.value = false
    loadRoles()
  } catch {
    // error handled
  } finally {
    editSaving.value = false
  }
}

async function onDelete(role: RoleVO) {
  try {
    await ElMessageBox.confirm(
      t('role.message.deleteConfirm', { name: currentLocale.value === 'ja' ? role.roleNameJp : role.roleNameCn }),
      t('role.message.deleteConfirmTitle'),
      { confirmButtonText: t('role.dialog.confirmDelete'), cancelButtonText: t('role.dialog.cancel'), type: 'warning' }
    )
    await roleApi.deleteRole(role.id)
    ElMessage.success(t('role.message.deleteSuccess'))
    if (selectedRole.value?.id === role.id) {
      selectedRole.value = null
      permTreeData.value = []
    }
    loadRoles()
  } catch {
    // cancelled
  }
}

loadRoles()
</script>

<style scoped>
.role-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: calc(100vh - 120px);
  overflow-y: auto;
}

/* ── 角色卡片 ── */
.role-card {
  flex-shrink: 0;
}
.role-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}
.role-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.role-name {
  font-weight: 600;
  color: var(--text-primary);
}

/* ── 权限卡片 ── */
.perm-card {
  flex: 1;
  min-height: 0;
  transition: opacity 0.3s;
}
.perm-card--active {
  opacity: 1;
}

.perm-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.perm-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.perm-header-icon {
  font-size: 20px;
  color: var(--color-primary);
}
.perm-header-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}
.perm-header-sub {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}
.perm-header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* ── 空状态 ── */
.perm-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: var(--text-placeholder);
  gap: 12px;
}
.perm-empty-icon {
  font-size: 40px;
  color: #d0d5dd;
}
.perm-empty-text {
  font-size: 14px;
  color: var(--text-placeholder);
}

.perm-loading {
  height: 300px;
}

/* ── 权限内容 ── */
.perm-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.perm-module-group {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  overflow: hidden;
}

.perm-module-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: linear-gradient(135deg, #f0f4ff 0%, #e8efff 100%);
  border-bottom: 1px solid var(--border-color);
  font-size: 13px;
  font-weight: 600;
  color: #3b5bdb;
}
.perm-module-icon {
  font-size: 14px;
  color: #3b5bdb;
}
.perm-module-name {
  flex: 1;
  color: #3b5bdb;
  font-size: 13px;
  letter-spacing: 0.3px;
}
.perm-module-count {
  font-size: 11px;
  color: #8b9cf4;
  font-weight: 400;
  background: rgba(59, 91, 219, 0.08);
  padding: 2px 8px;
  border-radius: 10px;
}

.perm-table {
  border-radius: 0;
}

.perm-name {
  font-size: 13px;
  color: var(--text-primary);
}

.perm-code {
  font-size: 11px;
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
  color: var(--text-secondary);
  background: var(--bg-page);
  padding: 2px 6px;
  border-radius: 4px;
  border: 1px solid var(--border-color);
}

/* ── 全局覆写：当前行高亮 ── */
:deep(.current-role-row) {
  background-color: #f0f4ff !important;
}
:deep(.current-role-row td) {
  background-color: #f0f4ff !important;
}

.btn-blue {
  color: var(--color-primary) !important;
}
</style>
