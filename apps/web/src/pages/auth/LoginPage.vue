<template>
  <div class="login-layout">

    <!-- 左：品牌面板 -->
    <div class="brand-panel">
      <div class="brand-bg" aria-hidden="true"/>
      <div class="brand-overlay" aria-hidden="true"/>
      <div class="brand-content">
        <div class="brand-logo">
          <img :src="logoImg" alt="Hill Stone" class="logo-img"/>
        </div>

        <div class="brand-headline">
          <h1>{{ $t('auth.welcomeTitle') }}</h1>
          <p>{{ $t('auth.welcomeDesc') }}</p>
        </div>

        <ul class="feature-list">
          <li v-for="(feature, i) in $tm('auth.features')" :key="i">
            <span class="feature-dot"/>
            <span>{{ feature }}</span>
          </li>
        </ul>

        <div class="brand-footer">
          <span class="footer-line"/>
          <span class="footer-text">MANPOU China System · Enterprise Edition</span>
        </div>
      </div>
    </div>

    <!-- 右：登录表单 -->
    <div class="form-panel">
      <div class="lang-switcher">
        <span class="lang-label">{{ $t('auth.language') }}</span>
        <el-radio-group v-model="currentLocale" size="small">
          <el-radio-button value="zh">{{ $t('auth.languageZh') }}</el-radio-button>
          <el-radio-button value="ja">{{ $t('auth.languageJa') }}</el-radio-button>
        </el-radio-group>
      </div>

      <div class="form-card">
        <div class="form-header">
          <h2>{{ $t('auth.title') }}</h2>
          <p class="form-subtitle">{{ $t('auth.subtitle') }}</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          @submit.prevent="submitLogin"
        >
          <el-form-item :label="$t('auth.username')" prop="username">
            <el-input
              v-model="form.username"
              :placeholder="$t('auth.usernamePlaceholder')"
              :prefix-icon="User"
              autocomplete="username"
              size="large"
            />
          </el-form-item>

          <el-form-item :label="$t('auth.password')" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              :placeholder="$t('auth.passwordPlaceholder')"
              :prefix-icon="Lock"
              autocomplete="current-password"
              show-password
              size="large"
              @keyup.enter="submitLogin"
            />
          </el-form-item>

          <el-form-item style="margin-top: 8px;">
            <el-button
              type="primary"
              class="login-btn"
              :loading="loading"
              @click="submitLogin"
            >
              {{ $t('auth.login') }}
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <p class="copyright">&copy; 2024–{{ new Date().getFullYear() }} MANPOU China. All rights reserved.</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useI18n } from 'vue-i18n'
import type { Locale } from '@/locales'
import { setLocale, getStoredLocale } from '@/locales'
import logoImg from '@/assets/images/LOGO.png'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const { t } = useI18n()

const formRef = ref<FormInstance>()
const loading = ref(false)
const currentLocale = ref<Locale>(getStoredLocale())

watch(currentLocale, (val) => {
  setLocale(val)
})

const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: () => t('auth.usernameRequired'), trigger: 'blur' }],
  password: [{ required: true, message: () => t('auth.passwordRequired'), trigger: 'blur' }],
}

async function submitLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await auth.login({ username: form.username, password: form.password })
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.push(redirect)
  } catch {
    ElMessage.error(t('auth.loginFailed'))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* ── 整体布局 ── */
.login-layout {
  display: flex;
  min-height: 100vh;
  width: 100%;
}

/* ── 左品牌面板 ── */
.brand-panel {
  position: relative;
  flex: 0 0 55%;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* 背景图 */
.brand-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  background-image: url('@/assets/images/back.png');
  background-size: cover;
  background-position: left center;
  background-repeat: no-repeat;
}

/* 暗色遮罩 */
.brand-overlay {
  position: absolute;
  inset: 0;
  z-index: 1;
  background: linear-gradient(
    135deg,
    rgba(0, 0, 0, 0.58) 0%,
    rgba(0, 0, 0, 0.32) 60%,
    rgba(0, 0, 0, 0.18) 100%
  );
}

/* 品牌内容 */
.brand-content {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: 100%;
  padding: 52px 56px;
  color: #fff;
}

/* Logo */
.brand-logo {
  display: flex;
  align-items: center;
}
.logo-img {
  height: 52px;
  width: auto;
  object-fit: contain;
  filter: drop-shadow(0 3px 10px rgba(0,0,0,0.5));
}

/* 标语 */
.brand-headline {
  max-width: 460px;
}
.brand-headline h1 {
  margin: 0 0 14px;
  font-size: 38px;
  font-weight: 800;
  line-height: 1.2;
  color: #fff;
  letter-spacing: 0.5px;
}
.brand-headline p {
  margin: 0;
  font-size: 15px;
  line-height: 1.75;
  color: rgba(255, 255, 255, 0.72);
}

/* 功能列表 */
.feature-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.feature-list li {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.82);
}
.feature-dot {
  flex-shrink: 0;
  width: 7px;
  height: 7px;
  background: rgba(255, 255, 255, 0.90);
  border-radius: 50%;
  box-shadow: 0 0 6px rgba(255, 255, 255, 0.5);
}

/* 底部 */
.brand-footer {
  display: flex;
  align-items: center;
  gap: 14px;
}
.footer-line {
  display: block;
  width: 36px;
  height: 2px;
  background: rgba(255, 255, 255, 0.45);
  border-radius: 2px;
}
.footer-text {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.38);
  letter-spacing: 1px;
}

/* ── 右登录面板 ── */
.form-panel {
  flex: 0 0 45%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 52px;
  background: #fff;
  position: relative;
}

/* 语言切换 */
.lang-switcher {
  position: absolute;
  top: 28px;
  right: 36px;
  display: flex;
  align-items: center;
  gap: 10px;
}
.lang-label {
  font-size: 12px;
  color: var(--text-muted);
}

/* 登录卡片 */
.form-card {
  width: 100%;
  max-width: 400px;
}

.form-header {
  margin-bottom: 30px;
  padding-bottom: 22px;
  border-bottom: 1px solid var(--border-color);
}
.form-header h2 {
  margin: 0 0 5px;
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 1px;
}
.form-subtitle {
  margin: 0;
  font-size: 12px;
  color: var(--text-muted);
  letter-spacing: 2px;
  text-transform: uppercase;
}

/* 登录按钮 */
.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 4px;
  background: linear-gradient(135deg, #E8650A 0%, #FF8C3A 100%) !important;
  border: none !important;
  border-radius: 8px;
  box-shadow: 0 4px 14px rgba(232, 101, 10, 0.30);
  transition: all 0.25s ease;
  color: #fff !important;
}
.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(232, 101, 10, 0.42);
  filter: brightness(1.05);
}
.login-btn:active {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(232, 101, 10, 0.22);
}

/* 版权 */
.copyright {
  position: absolute;
  bottom: 24px;
  font-size: 11px;
  color: var(--text-muted);
  text-align: center;
}

/* ── 响应式 ── */
@media (max-width: 900px) {
  .login-layout {
    flex-direction: column;
  }
  .brand-panel {
    flex: 0 0 auto;
    height: 240px;
    min-height: 240px;
  }
  .brand-content {
    padding: 28px 28px;
  }
  .brand-headline h1 {
    font-size: 26px;
  }
  .brand-headline p {
    font-size: 13px;
  }
  .feature-list {
    display: none;
  }
  .brand-footer {
    display: none;
  }
  .form-panel {
    flex: 1;
    padding: 32px 24px;
  }
}
</style>
