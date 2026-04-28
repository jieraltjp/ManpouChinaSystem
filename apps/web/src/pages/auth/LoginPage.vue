<template>
  <div class="login-layout">
    <!-- ========== 左品牌面板 ========== -->
    <div class="brand-panel">
      <!-- 占位背景图（1920×1080 SVG） -->
      <div class="brand-bg" aria-hidden="true">
        <svg viewBox="0 0 1920 1080" xmlns="http://www.w3.org/2000/svg" class="bg-svg">
          <!-- 天空渐变 -->
          <defs>
            <linearGradient id="skyGrad" x1="0" y1="0" x2="1" y2="1">
              <stop offset="0%" stop-color="#1A1A2E"/>
              <stop offset="40%" stop-color="#16213E"/>
              <stop offset="100%" stop-color="#0F3460"/>
            </linearGradient>
            <linearGradient id="glowGrad" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="#E8650A" stop-opacity="0.5"/>
              <stop offset="100%" stop-color="#E8650A" stop-opacity="0"/>
            </linearGradient>
            <radialGradient id="circleGlow" cx="50%" cy="50%" r="50%">
              <stop offset="0%" stop-color="#FF8C3A" stop-opacity="0.35"/>
              <stop offset="100%" stop-color="#FF8C3A" stop-opacity="0"/>
            </radialGradient>
            <pattern id="gridPattern" width="60" height="60" patternUnits="userSpaceOnUse">
              <path d="M 60 0 L 0 0 0 60" fill="none" stroke="rgba(255,255,255,0.05)" stroke-width="1"/>
            </pattern>
            <clipPath id="clip">
              <rect width="1920" height="1080"/>
            </clipPath>
          </defs>

          <!-- 底色 -->
          <rect width="1920" height="1080" fill="url(#skyGrad)"/>

          <!-- 网格纹理 -->
          <rect width="1920" height="1080" fill="url(#gridPattern)" clip-path="url(#clip)"/>

          <!-- 大圆形光晕 -->
          <ellipse cx="960" cy="200" rx="700" ry="400" fill="url(#circleGlow)"/>

          <!-- 顶部橙色光晕条 -->
          <rect x="0" y="0" width="1920" height="4" fill="url(#glowGrad)"/>

          <!-- 装饰圆环 1 -->
          <circle cx="200" cy="800" r="180" fill="none" stroke="rgba(232,101,10,0.15)" stroke-width="1"/>
          <circle cx="200" cy="800" r="140" fill="none" stroke="rgba(232,101,10,0.1)" stroke-width="1"/>
          <circle cx="200" cy="800" r="100" fill="none" stroke="rgba(232,101,10,0.08)" stroke-width="1"/>

          <!-- 装饰圆环 2 -->
          <circle cx="1700" cy="150" r="120" fill="none" stroke="rgba(255,140,58,0.15)" stroke-width="1"/>
          <circle cx="1700" cy="150" r="80" fill="none" stroke="rgba(255,140,58,0.1)" stroke-width="1"/>

          <!-- 装饰线条 -->
          <line x1="0" y1="950" x2="600" y2="950" stroke="rgba(232,101,10,0.3)" stroke-width="1"/>
          <line x1="0" y1="960" x2="400" y2="960" stroke="rgba(232,101,10,0.2)" stroke-width="1"/>

          <!-- 右侧垂直线 -->
          <line x1="1850" y1="0" x2="1850" y2="1080" stroke="rgba(232,101,10,0.08)" stroke-width="1"/>
          <line x1="1870" y1="0" x2="1870" y2="1080" stroke="rgba(232,101,10,0.05)" stroke-width="1"/>

          <!-- Logo 占位符号（MANPOU 首字母 M） -->
          <text x="960" y="520" text-anchor="middle"
            font-family="'Inter', 'PingFang SC', sans-serif"
            font-size="180" font-weight="800"
            fill="rgba(232,101,10,0.12)" letter-spacing="-10">M</text>

          <!-- 小标签文字 -->
          <text x="960" y="600" text-anchor="middle"
            font-family="'Inter', 'PingFang SC', sans-serif"
            font-size="24" font-weight="400"
            fill="rgba(255,255,255,0.25)" letter-spacing="8">MANPOU  CHINA</text>
        </svg>
      </div>

      <!-- 遮罩层 -->
      <div class="brand-overlay" aria-hidden="true"/>

      <!-- 品牌内容 -->
      <div class="brand-content">
        <!-- Logo 文字 -->
        <div class="brand-logo">
          <span class="logo-icon">M</span>
          <div class="logo-text">
            <span class="logo-name">MANPOU</span>
            <span class="logo-sub">{{ $t('auth.title') }}</span>
          </div>
        </div>

        <!-- 标语 -->
        <div class="brand-headline">
          <h1>{{ $t('auth.welcomeTitle') }}</h1>
          <p>{{ $t('auth.welcomeDesc') }}</p>
        </div>

        <!-- 功能亮点 -->
        <ul class="feature-list">
          <li v-for="(feature, i) in $tm('auth.features')" :key="i">
            <span class="feature-dot"/>
            <span>{{ feature }}</span>
          </li>
        </ul>

        <!-- 底部装饰 -->
        <div class="brand-footer">
          <span class="footer-line"/>
          <span class="footer-text">MANPOU China System · Enterprise Edition</span>
        </div>
      </div>
    </div>

    <!-- ========== 右登录面板 ========== -->
    <div class="form-panel">
      <!-- 语言切换 -->
      <div class="lang-switcher">
        <span class="lang-label">{{ $t('auth.language') }}</span>
        <el-radio-group v-model="currentLocale" size="small">
          <el-radio-button value="zh">中文</el-radio-button>
          <el-radio-button value="ja">日本語</el-radio-button>
        </el-radio-group>
      </div>

      <!-- 登录卡片 -->
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

      <!-- 底部版权 -->
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

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const { t } = useI18n()

const formRef = ref<FormInstance>()
const loading = ref(false)
const currentLocale = ref<Locale>(getStoredLocale())

// 切换语言时同步 i18n
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

/* 占位背景 */
.brand-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
}
.bg-svg {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

/* 遮罩层 */
.brand-overlay {
  position: absolute;
  inset: 0;
  z-index: 1;
  background: linear-gradient(
    135deg,
    rgba(10, 10, 20, 0.55) 0%,
    rgba(10, 10, 20, 0.30) 60%,
    rgba(232, 101, 10, 0.12) 100%
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
  padding: 48px 56px;
  color: #fff;
}

/* Logo */
.brand-logo {
  display: flex;
  align-items: center;
  gap: 14px;
}
.logo-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  background: var(--color-primary);
  border-radius: 12px;
  font-size: 28px;
  font-weight: 800;
  color: #fff;
  box-shadow: 0 4px 16px rgba(232, 101, 10, 0.45);
}
.logo-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.logo-name {
  font-size: 22px;
  font-weight: 800;
  letter-spacing: 5px;
  color: #fff;
}
.logo-sub {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.55);
  letter-spacing: 1px;
}

/* 标语 */
.brand-headline {
  max-width: 480px;
}
.brand-headline h1 {
  margin: 0 0 16px;
  font-size: 40px;
  font-weight: 800;
  line-height: 1.2;
  color: #fff;
  letter-spacing: 1px;
}
.brand-headline p {
  margin: 0;
  font-size: 16px;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.70);
}

/* 功能列表 */
.feature-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.feature-list li {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.80);
}
.feature-dot {
  flex-shrink: 0;
  width: 8px;
  height: 8px;
  background: var(--color-primary);
  border-radius: 50%;
  box-shadow: 0 0 8px rgba(232, 101, 10, 0.7);
}

/* 底部 */
.brand-footer {
  display: flex;
  align-items: center;
  gap: 16px;
}
.footer-line {
  display: block;
  width: 40px;
  height: 2px;
  background: var(--color-primary);
  border-radius: 2px;
}
.footer-text {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.35);
  letter-spacing: 1px;
}

/* ── 右登录面板 ── */
.form-panel {
  flex: 0 0 45%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 48px;
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
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--border-color);
}
.form-header h2 {
  margin: 0 0 6px;
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 1px;
}
.form-subtitle {
  margin: 0;
  font-size: 13px;
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
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%) !important;
  border: none !important;
  border-radius: var(--radius-md);
  box-shadow: 0 4px 14px rgba(232, 101, 10, 0.35);
  transition: all var(--transition-normal);
  color: #fff !important;
}
.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(232, 101, 10, 0.45);
  filter: brightness(1.05);
}
.login-btn:active {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(232, 101, 10, 0.25);
}

/* 版权 */
.copyright {
  position: absolute;
  bottom: 28px;
  font-size: 11px;
  color: var(--text-muted);
  text-align: center;
}

/* ── 响应式：小屏隐藏左面板 ── */
@media (max-width: 900px) {
  .login-layout {
    flex-direction: column;
  }
  .brand-panel {
    flex: 0 0 auto;
    height: 260px;
    min-height: 260px;
  }
  .brand-content {
    padding: 32px 32px;
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
