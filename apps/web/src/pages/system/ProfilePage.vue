<template>
  <div class="page profile-page">
    <div class="profile-layout">
      <!-- 左侧：头像卡片 -->
      <div class="profile-left">
        <el-card shadow="never">
          <div class="avatar-section">
            <div class="avatar-wrap">
              <el-avatar :src="userInfo?.avatarUrl ?? ''" :size="100" fit="cover">
                {{ avatarText }}
              </el-avatar>
            </div>
            <div class="user-name">{{ currentLocale === 'ja' ? userInfo?.nameJp : userInfo?.nameCn || userInfo?.username }}</div>
            <div class="user-role">
              <el-tag v-for="r in userInfo?.roles?.slice(0, 2)" :key="r.id" size="small" type="info">
                {{ currentLocale === 'ja' ? r.roleNameJp : r.roleNameCn }}
              </el-tag>
            </div>
            <div class="user-email">{{ userInfo?.email }}</div>
          </div>

          <el-divider />

          <div class="info-list">
            <div class="info-item">
              <span class="info-label">{{ $t('profile.info.company') }}</span>
              <span class="info-value">{{ userInfo?.companyName || '—' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">{{ $t('profile.info.department') }}</span>
              <span class="info-value">{{ userInfo?.departmentName || '—' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">{{ $t('profile.info.positions') }}</span>
              <span class="info-value">
                <template v-if="userInfo?.positions?.length">
                  {{ userInfo.positions.map(p => currentLocale === 'ja' ? p.nameJp : p.nameCn).join('、') }}
                </template>
                <template v-else>—</template>
              </span>
            </div>
            <div class="info-item">
              <span class="info-label">{{ $t('profile.info.status') }}</span>
              <el-tag :type="userInfo?.status === 1 ? 'success' : 'danger'" size="small">
                {{ userInfo?.status === 1 ? $t('profile.status.NORMAL') : $t('profile.status.DISABLED') }}
              </el-tag>
            </div>
            <div class="info-item">
              <span class="info-label">{{ $t('profile.info.createdAt') }}</span>
              <span class="info-value">{{ formatTime(userInfo?.createTime) }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">{{ $t('profile.info.lastLogin') }}</span>
              <span class="info-value">{{ formatTime(userInfo?.lastLoginTime) }}</span>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 右侧：Tab 表单 -->
      <div class="profile-right">
        <el-card shadow="never">
          <el-tabs v-model="activeTab">
            <!-- Tab1: 基本信息 -->
            <el-tab-pane :label="$t('profile.tab.info')" name="info">
              <el-form ref="infoFormRef" :model="infoForm" :rules="infoRules" label-width="120" class="profile-form">
                <el-form-item :label="$t('profile.info.nameCn')" prop="nameCn">
                  <el-input v-model="infoForm.nameCn" maxlength="64" />
                </el-form-item>
                <el-form-item :label="$t('profile.info.nameJp')" prop="nameJp">
                  <el-input v-model="infoForm.nameJp" maxlength="64" />
                </el-form-item>
                <el-form-item :label="$t('profile.info.email')">
                  <el-input :model-value="userInfo?.email" disabled />
                </el-form-item>
                <el-form-item :label="$t('profile.info.phone')" prop="phone">
                  <el-input v-model="infoForm.phone" maxlength="32" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="infoSaving" @click="onSaveInfo">
                    {{ $t('common.button.save') }}
                  </el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <!-- Tab2: 账户安全 -->
            <el-tab-pane :label="$t('profile.tab.security')" name="security">
              <div class="security-section">
                <h4>{{ $t('profile.security.changePassword') }}</h4>
                <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="140" class="profile-form">
                  <el-form-item :label="$t('profile.security.oldPassword')" prop="oldPassword">
                    <el-input v-model="pwdForm.oldPassword" type="password" show-password maxlength="20" />
                  </el-form-item>
                  <el-form-item :label="$t('profile.security.newPassword')" prop="newPassword">
                    <el-input v-model="pwdForm.newPassword" type="password" show-password maxlength="20" />
                    <div class="strength-wrap">
                      <el-progress :percentage="passwordStrength" :color="strengthColor" :show-text="false" style="width:120px" />
                      <span class="strength-label" :style="{ color: strengthColor }">
                        {{ passwordStrength <= 33 ? $t('profile.strength.weak')
                           : passwordStrength <= 66 ? $t('profile.strength.medium')
                           : $t('profile.strength.strong') }}
                      </span>
                    </div>
                    <div class="password-rules">{{ passwordRules[0] }}，{{ passwordRules[1] }}</div>
                  </el-form-item>
                  <el-form-item :label="$t('profile.security.confirmPassword')" prop="confirmPassword">
                    <el-input v-model="pwdForm.confirmPassword" type="password" show-password maxlength="20" />
                  </el-form-item>
                  <el-form-item>
                    <el-button type="primary" :loading="pwdSaving" @click="onChangePassword">
                      {{ $t('profile.security.changePassword') }}
                    </el-button>
                  </el-form-item>
                </el-form>
              </div>
            </el-tab-pane>

            <!-- Tab3: 偏好设置 -->
            <el-tab-pane :label="$t('profile.tab.preferences')" name="preferences">
              <el-form ref="prefFormRef" :model="prefForm" label-width="140" class="profile-form">
                <el-form-item :label="$t('profile.preferences.language')">
                  <el-radio-group v-model="prefForm.language">
                    <el-radio value="zh">中文</el-radio>
                    <el-radio value="ja">日本語</el-radio>
                  </el-radio-group>
                </el-form-item>
                <el-form-item :label="$t('profile.preferences.timezone')">
                  <el-select v-model="prefForm.timezone" style="width:220px">
                    <el-option value="Asia/Shanghai" label="中国时区 (UTC+8)" />
                    <el-option value="Asia/Tokyo" label="日本时区 (UTC+9)" />
                  </el-select>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="prefSaving" @click="onSavePreferences">
                    {{ $t('common.button.save') }}
                  </el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getCurrentUser, updateCurrentUser, changePassword, type ProfileUpdateCmd, type ChangePasswordCmd } from '@/api/user'
import type { UserVO } from '@/api/user'
import { useI18n } from 'vue-i18n'

const { locale, t } = useI18n()
const currentLocale = computed(() => locale.value)

const avatarText = computed(() => {
  const name = currentLocale.value === 'ja'
    ? (userInfo.value?.nameJp || userInfo.value?.nameCn || userInfo.value?.username || '?')
    : (userInfo.value?.nameCn || userInfo.value?.nameJp || userInfo.value?.username || '?')
  return name[0] ?? '?'
})

function formatTime(ts: string | undefined | null): string {
  if (!ts) return '-'
  return new Date(ts).toLocaleString(locale.value === 'ja' ? 'ja-JP' : 'zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit',
  })
}

const activeTab = ref('info')
const userInfo = ref<UserVO | null>(null)
const loading = ref(false)

// Tab1: 基本信息
const infoFormRef = ref()
const infoSaving = ref(false)
const infoForm = reactive<ProfileUpdateCmd>({
  nameCn: '',
  nameJp: '',
  phone: '',
})

const infoRules = {
  phone: [{ pattern: /^[\d\-+ ]*$/, message: 'phone_invalid' }],
}

async function loadUserInfo() {
  loading.value = true
  try {
    const data = await getCurrentUser()
    userInfo.value = data
    infoForm.nameCn = data.nameCn || ''
    infoForm.nameJp = data.nameJp || ''
    infoForm.phone = data.phone || ''
    prefForm.language = data.language || 'zh'
    prefForm.timezone = data.timezone || 'Asia/Shanghai'
  } catch {
    ElMessage.error(String(t('profile.message.loadFailed')))
  } finally {
    loading.value = false
  }
}

async function onSaveInfo() {
  await infoFormRef.value?.validate().catch(() => null)
  if (!infoFormRef.value) return
  infoSaving.value = true
  try {
    const updated = await updateCurrentUser(infoForm)
    userInfo.value = updated
    ElMessage.success(String(t('profile.message.updateSuccess')))
  } catch {
    ElMessage.error(String(t('profile.message.updateFailed')))
  } finally {
    infoSaving.value = false
  }
}

// Tab2: 账户安全
const pwdFormRef = ref()
const pwdSaving = ref(false)
const pwdForm = reactive<{ oldPassword: string; newPassword: string; confirmPassword: string }>({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const validateConfirm = (_rule: unknown, value: string, callback: (e?: Error) => void) => {
  if (value !== pwdForm.newPassword) {
    callback(new Error(String(t('profile.message.passwordMismatch'))))
  } else {
    callback()
  }
}

const pwdRules = {
  oldPassword: [{ required: true, message: 'oldPassword_required', trigger: 'blur' }],
  newPassword: [
    { required: true, message: 'newPassword_required', trigger: 'blur' },
    { min: 8, max: 20, message: 'password_length', trigger: 'blur' },
    { pattern: /^(?=.*[A-Z])(?=.*[a-z])(?=.*\d).{8,20}$/, message: 'password_complexity', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: 'confirmPassword_required', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
}

const passwordStrength = computed(() => {
  const p = pwdForm.newPassword
  if (!p) return 0
  let score = 0
  if (p.length >= 8) score += 33
  if (/[A-Z]/.test(p) && /[a-z]/.test(p)) score += 33
  if (/\d/.test(p)) score += 34
  return score
})

const passwordRules = computed(() => {
  const rules = t('profile.security.rules')
  return Array.isArray(rules) ? rules : []
})

const strengthColor = computed(() => {
  if (passwordStrength.value <= 33) return '#F56C6C'
  if (passwordStrength.value <= 66) return '#E6A23C'
  return '#67C23A'
})

async function onChangePassword() {
  await pwdFormRef.value?.validate().catch(() => null)
  if (!pwdFormRef.value) return
  pwdSaving.value = true
  try {
    const cmd: ChangePasswordCmd = {
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
    }
    await changePassword(cmd)
    ElMessage.success(String(t('profile.message.passwordChangeSuccess')))
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
  } catch (err: unknown) {
    const msg = (err as { message?: string })?.message || ''
    if (msg.includes('旧密码') || msg.includes('wrongPassword')) {
      ElMessage.error(String(t('profile.message.passwordWrong')))
    } else {
      ElMessage.error(String(t('profile.message.passwordChangeFailed')))
    }
  } finally {
    pwdSaving.value = false
  }
}

// Tab3: 偏好设置
const prefFormRef = ref()
const prefSaving = ref(false)
const prefForm = reactive({ language: 'zh', timezone: 'Asia/Shanghai' })

async function onSavePreferences() {
  prefSaving.value = true
  try {
    await updateCurrentUser({ language: prefForm.language, timezone: prefForm.timezone })
    locale.value = prefForm.language
    localStorage.setItem('locale', prefForm.language)
    ElMessage.success(String(t('profile.message.updateSuccess')))
  } catch {
    ElMessage.error(String(t('profile.message.updateFailed')))
  } finally {
    prefSaving.value = false
  }
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.profile-page {
  padding: 16px;
}

.profile-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.profile-left {
  width: 280px;
  flex-shrink: 0;
}

.profile-right {
  flex: 1;
  min-width: 0;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
}

.avatar-wrap :deep(.el-avatar) {
  background: var(--el-color-primary-light-7);
  color: var(--el-color-primary);
  font-size: 36px;
  font-weight: 600;
}

.user-name {
  font-size: 16px;
  font-weight: 600;
}

.user-role {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
  justify-content: center;
}

.user-email {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
}

.info-label {
  color: var(--el-text-color-secondary);
  flex-shrink: 0;
}

.info-value {
  color: var(--el-text-color-primary);
  text-align: right;
  word-break: break-all;
}

.profile-form {
  max-width: 480px;
}

.strength-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}

.strength-label {
  font-size: 12px;
}

.password-rules {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}

.security-section h4 {
  margin: 0 0 16px;
  font-size: 14px;
  font-weight: 600;
}

@media (max-width: 768px) {
  .profile-layout {
    flex-direction: column;
  }

  .profile-left {
    width: 100%;
  }
}
</style>
