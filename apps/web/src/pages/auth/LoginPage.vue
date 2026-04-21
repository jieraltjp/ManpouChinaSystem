<template>
  <div class="login-container">
    <el-card class="login-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <h2>漫普中国管理系统</h2>
          <p class="subtitle">Manpou China System</p>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleLogin"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
            autocomplete="username"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            autocomplete="current-password"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>

    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '用户名不能为空', trigger: 'blur' }],
  password: [{ required: true, message: '密码不能为空', trigger: 'blur' }],
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await auth.login({ username: form.username, password: form.password })
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.push(redirect)
  } catch (err) {
    console.error('[LoginPage] login failed', err)
    ElMessage.error('登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(ellipse 70% 55% at 18% 28%, rgba(232,101,10,0.14) 0%, transparent 65%),
    radial-gradient(ellipse 55% 45% at 82% 72%, rgba(255,140,58,0.10) 0%, transparent 65%),
    linear-gradient(160deg, #F7F8FA 0%, #FEF3E7 55%, #FFF8F0 100%);
}

.login-card {
  width: 420px;
  border-radius: var(--radius-lg);
  border-top: 4px solid var(--color-primary);
  box-shadow: var(--shadow-lg);
  background: rgba(255,255,255,0.96);
  backdrop-filter: blur(12px);
}

.card-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-color);
}

.card-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 2px;
  text-align: center;
}

.card-header h2::before {
  content: '';
  display: inline-block;
  width: 5px;
  height: 20px;
  background: var(--color-primary);
  border-radius: 3px;
  margin-right: 10px;
  vertical-align: middle;
  position: relative;
  top: -1px;
}

.subtitle {
  margin: 0;
  font-size: 12px;
  color: var(--text-muted);
  letter-spacing: 3px;
  text-transform: uppercase;
}

.login-btn {
  width: 100%;
  height: 46px;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 4px;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  border: none;
  border-radius: var(--radius-md);
  box-shadow: 0 4px 14px rgba(232,101,10,0.38);
  transition: all var(--transition-normal);
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(232,101,10,0.48);
  filter: brightness(1.05);
}

.login-btn:active {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(232,101,10,0.28);
}

.hint {
  text-align: center;
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-muted);
}
</style>
