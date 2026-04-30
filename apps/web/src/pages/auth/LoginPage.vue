<template>
  <div class="login-layout">
    <!-- 全屏背景图 -->
    <div class="fullscreen-bg" aria-hidden="true"/>

    <!-- 弧形遮罩 + 内容层 -->
    <div class="scene">
      <!-- 左：品牌内容（弧形遮罩内嵌文字） -->
      <div class="brand-panel">
        <div class="brand-content">
          <!-- Logo -->
          <div class="brand-logo">
            <img :src="logoImg" alt="Hill Stone" class="logo-img"/>
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

          <!-- 底部 -->
          <div class="brand-footer">
            <span class="footer-line"/>
            <span class="footer-text">MANPOU China System · Enterprise Edition</span>
          </div>
        </div>
      </div>

      <!-- 右：登录表单（SVG 弧形曲线切割） -->
      <div class="form-panel">
        <!-- 语言切换 -->
        <div class="lang-switcher">
          <span class="lang-label">{{ $t('auth.language') }}</span>
          <el-radio-group v-model="currentLocale" size="small">
            <el-radio-button value="zh">{{ $t('auth.languageZh') }}</el-radio-button>
            <el-radio-button value="ja">{{ $t('auth.languageJa') }}</el-radio-button>
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

        <p class="copyright">&copy; 2024–{{ new Date().getFullYear() }} MANPOU China. All rights reserved.</p>
      </div>
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
/* ── 全屏背景 ── */
.login-layout {
  position: relative;
  width: 100%;
  min-height: 100vh;
  overflow: hidden;
}

.fullscreen-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  background-image: url('@/assets/images/back.png');
  background-size: cover;
  background-position: left center;
  background-repeat: no-repeat;
}

/* ── 场景层 ── */
.scene {
  position: relative;
  z-index: 1;
  display: flex;
  min-height: 100vh;
  width: 100%;
}

/* ── 左品牌面板：弧形曲线遮罩 ── */
.brand-panel {
  position: relative;
  flex: 1;
  display: flex;
  /* 弧形 SVG clip-path：右侧向内凹陷 */
  clip-path: path('M 0,0 L 65%,0 C 58%,18 52%,40 56%,62 C 60%,84 67%,100 72%,100 L 0,100 Z');
}

.brand-panel::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(
    135deg,
    rgba(0, 0, 0, 0.60) 0%,
    rgba(0, 0, 0, 0.40) 50%,
    rgba(0, 0, 0, 0.20) 100%
  );
  z-index: 0;
}

.brand-panel::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(
    to right,
    rgba(232, 101, 10, 0.08) 0%,
    transparent 60%
  );
  z-index: 0;
}

.brand-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  width: 100%;
  height: 100%;
  padding: 56px 48px 56px 64px;
  color: #fff;
}

/* Logo */
.brand-logo {
  display: flex;
  align-items: center;
}
.logo-img {
  height: 48px;
  width: auto;
  object-fit: contain;
  filter: drop-shadow(0 2px 8px rgba(0,0,0,0.3));
}

/* 标语 */
.brand-headline {
  max-width: 380px;
}
.brand-headline h1 {
  margin: 0 0 14px;
  font-size: 36px;
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
  background: rgba(255, 255, 255, 0.85);
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

/* ── 右登录面板：白色表单区，弧形左边缘 ── */
.form-panel {
  position: relative;
  flex: 0 0 42%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 48px;
  background: rgba(255, 255, 255, 0.97);
  backdrop-filter: blur(12px);
  /* 弧形 SVG clip-path：左侧向外凸出，形成与左面板互补的弧线 */
  clip-path: path('M 14%,100 C 9%,84 4%,62 8%,40 C 12%,18 18%,0 22%,0 L 100%,0 L 100%,100 Z');
  box-shadow: -8px 0 40px rgba(0, 0, 0, 0.12);
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
  max-width: 380px;
}

.form-header {
  margin-bottom: 28px;
  padding-bottom: 20px;
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
  .fullscreen-bg {
    background-size: auto 100%;
    background-position: center;
  }
  .scene {
    flex-direction: column;
  }
  .brand-panel {
    clip-path: none;
    flex: 0 0 auto;
    min-height: 220px;
  }
  .brand-panel::before {
    background: linear-gradient(to bottom, rgba(0,0,0,0.55), rgba(0,0,0,0.35));
  }
  .brand-panel::after {
    display: none;
  }
  .brand-content {
    padding: 32px 28px;
    gap: 24px;
  }
  .brand-headline h1 {
    font-size: 26px;
  }
  .feature-list {
    display: none;
  }
  .brand-footer {
    display: none;
  }
  .form-panel {
    flex: 1;
    clip-path: none;
    padding: 32px 24px;
    box-shadow: none;
  }
}
</style>
