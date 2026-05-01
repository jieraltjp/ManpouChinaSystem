<template>
  <div class="page role-page">
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true">
        <el-form-item>
          <el-button type="primary" @click="onNew">
            <el-icon><Plus /></el-icon>{{ $t('role.newButton') }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="role-layout">
      <!-- 左侧：角色列表 -->
      <div class="role-list-panel">
        <el-card shadow="never" class="list-card">
          <el-scrollbar height="100%">
            <div
              v-for="r in allRoles"
              :key="r.id"
              class="role-item"
              :class="{ active: selectedRole?.id === r.id }"
              @click="onSelectRole(r)"
            >
              <div class="role-name">
                <span>{{ currentLocale === 'ja' ? r.roleNameJp : r.roleNameCn }}</span>
                <el-tag v-if="r.isEditable === 0" size="small" type="warning" style="margin-left:4px">
                  <el-icon><Lock /></el-icon>
                </el-tag>
              </div>
              <div class="role-code">{{ r.roleCode }}</div>
              <div class="role-meta">
                <span v-if="r.userCount !== undefined">{{ r.userCount }} {{ $t('role.column.userCount') }}</span>
                <span class="role-actions">
                  <el-button link class="btn-blue" size="small" @click.stop="onEdit(r)">{{ $t('role.action.edit') }}</el-button>
                  <el-button v-if="r.isEditable !== 0" link type="danger" size="small" @click.stop="onDelete(r)">{{ $t('role.action.delete') }}</el-button>
                </span>
              </div>
            </div>
          </el-scrollbar>
        </el-card>
      </div>

      <!-- 右侧：权限配置 -->
      <div class="permission-panel">
        <el-card shadow="never" class="perm-card">
          <template #header>
            <div class="perm-header">
              <span>{{ selectedRole ? (currentLocale === 'ja' ? selectedRole.roleNameJp : selectedRole.roleNameCn) : $t('role.permission.selectTip') }}</span>
              <el-button v-if="selectedRole && selectedRole.isEditable !== 0" type="primary" size="small" :loading="permSaving" @click="onSavePermissions">
                {{ $t('role.dialog.save') }}
              </el-button>
            </div>
          </template>
          <div v-if="!selectedRole" class="perm-empty">
            {{ $t('role.permission.selectTip') }}
          </div>
          <div v-else-if="permLoading" v-loading="permLoading" style="height:300px" />
          <div v-else class="perm-tree-wrap">
            <el-tree
              ref="permTreeRef"
              :data="permTreeData"
              :props="{ label: 'label', children: 'children', disabled: () => selectedRole?.isEditable === 0 }"
              node-key="id"
              show-checkbox
              default-expand-all
              :expand-on-click-node="false"
              @check="onPermCheck"
            >
              <template #default="{ data }">
                <span class="perm-node">
                  <span class="perm-label">{{ data.label }}</span>
                  <span class="perm-code">{{ data.code }}</span>
                </span>
              </template>
            </el-tree>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 新增/编辑角色弹窗 -->
    <el-dialog v-model="editVisible" :title="editMode === 'create' ? $t('role.dialog.createTitle') : $t('role.dialog.editTitle')" width="480px">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="100px">
        <el-form-item :label="$t('role.dialog.roleCode')" prop="roleCode">
          <el-input v-model="editForm.roleCode" :placeholder="$t('role.dialog.roleCode')" :disabled="editMode === 'update'" />
        </el-form-item>
        <el-form-item :label="$t('role.dialog.roleNameCn')" prop="roleNameCn">
          <el-input v-model="editForm.roleNameCn" :placeholder="$t('role.dialog.roleNameCn')" />
        </el-form-item>
        <el-form-item :label="$t('role.dialog.roleNameJp')" prop="roleNameJp">
          <el-input v-model="editForm.roleNameJp" :placeholder="$t('role.dialog.roleNameJp')" />
        </el-form-item>
        <el-form-item v-if="editMode === 'create'" :label="$t('role.dialog.roleType')" prop="roleType">
          <el-select v-model="editForm.roleType" style="width:100%">
            <el-option :label="$t('role.type.SYSTEM')" value="SYSTEM" />
            <el-option :label="$t('role.type.BUSINESS')" value="BUSINESS" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('role.dialog.description')">
          <el-input v-model="editForm.description" type="textarea" :rows="2" />
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
import { ref, reactive, computed, nextTick } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Lock } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import * as roleApi from '@/api/role'
import type { RoleVO, RoleCreateCmd, RoleUpdateCmd } from '@/api/role'

const { t, locale } = useI18n()
const currentLocale = computed(() => locale.value)

interface PermNode {
  id: number
  label: string
  code: string
  children?: PermNode[]
  disabled?: boolean
}

// ===== 状态 =====
const allRoles = ref<RoleVO[]>([])
const selectedRole = ref<RoleVO | null>(null)
const permTreeData = ref<PermNode[]>([])
const permTreeRef = ref()
const permLoading = ref(false)
const permSaving = ref(false)
const checkedPermIds = ref<number[]>([])

const editVisible = ref(false)
const editMode = ref<'create' | 'update'>('create')
const editSaving = ref(false)
const editFormRef = ref<FormInstance>()
const editForm = reactive<Partial<RoleCreateCmd & RoleUpdateCmd>>({})
const editRules: FormRules = {
  roleCode: [{ required: true, message: t('role.validation.roleCodeRequired'), trigger: 'blur' }],
  roleNameCn: [{ required: true, message: t('role.validation.roleNameCnRequired'), trigger: 'blur' }],
}

// ===== 方法 =====
async function loadRoles() {
  allRoles.value = await roleApi.listRoles()
}

async function loadPermissionTree() {
  if (!selectedRole.value) return
  permLoading.value = true
  try {
    const modules = await roleApi.getPermissionTree()
    const role = await roleApi.getRole(selectedRole.value.id)
    selectedRole.value = role

    permTreeData.value = modules.map(mod => ({
      id: -mod.module.charCodeAt(0),
      label: (currentLocale.value === 'ja' ? mod.moduleNameJp : mod.moduleNameCn) ?? mod.module,
      code: mod.module,
      children: mod.permissions.map(p => ({
        id: p.id,
        label: (currentLocale.value === 'ja' ? p.permissionNameJp : p.permissionNameCn) ?? p.permissionCode,
        code: p.permissionCode,
      })),
    }))

    // 回显已有权限
    checkedPermIds.value = role.permissions?.map(p => p.id) ?? []
    nextTick(() => {
      permTreeRef.value?.setCheckedKeys(checkedPermIds.value)
    })
  } catch {
    // error handled
  } finally {
    permLoading.value = false
  }
}

function onSelectRole(role: RoleVO) {
  selectedRole.value = role
  loadPermissionTree()
}

function onPermCheck(_node: PermNode, { checkedKeys }: { checkedKeys: number[] }) {
  checkedPermIds.value = checkedKeys
}

async function onSavePermissions() {
  if (!selectedRole.value) return
  permSaving.value = true
  try {
    await roleApi.assignRolePermissions(selectedRole.value.id, { permissionIds: checkedPermIds.value })
    ElMessage.success(t('role.message.permissionSaved'))
  } catch {
    // error handled
  } finally {
    permSaving.value = false
  }
}

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
    await ElMessageBox.confirm(t('role.message.deleteConfirm', { name: currentLocale.value === 'ja' ? role.roleNameJp : role.roleNameCn }), t('role.message.deleteConfirmTitle'))
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
.role-page { height: calc(100vh - 120px); }
.role-layout { display:flex; gap:16px; height: calc(100% - 60px); }
.role-list-panel { width: 280px; flex-shrink: 0; }
.list-card { height: 100%; }
.perm-card { flex: 1; }
.perm-header { display:flex; justify-content:space-between; align-items:center; }
.perm-empty { text-align:center; color:#909399; padding:60px 0; }

.role-item { padding:10px 12px; border-radius:6px; cursor:pointer; border-bottom:1px solid #f0f0f0; transition:background .15s; }
.role-item:hover { background:#f5f7fa; }
.role-item.active { background:#ecf5ff; border-left:3px solid #409EFF; }
.role-name { font-weight:600; color:#303133; display:flex; align-items:center; }
.role-code { font-size:12px; color:#909399; margin-top:2px; }
.role-meta { display:flex; justify-content:space-between; align-items:center; margin-top:4px; font-size:12px; color:#909399; }
.role-actions { opacity:0; transition:opacity .15s; }
.role-item:hover .role-actions { opacity:1; }
.btn-blue { color:#409EFF; }

.perm-tree-wrap { max-height:500px; overflow:auto; }
.perm-node { display:flex; gap:8px; align-items:center; }
.perm-label { }
.perm-code { font-size:11px; color:#909399; font-family:monospace; }
</style>
