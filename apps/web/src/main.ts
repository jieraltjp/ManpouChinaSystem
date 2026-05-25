// 橙色商业主题 CSS 变量（必须优先引入）
import './assets/variables.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'dayjs/locale/zh-cn'
import 'dayjs/locale/ja'

import App from './App.vue'
import router from './router'
import { i18n, setLocale, getStoredLocale, type Locale } from './locales'
import { setupErrorHandler } from '@/utils/errorHandler'

const app = createApp(App)

// 注册所有 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 初始化语言设置
const storedLocale = getStoredLocale()
setLocale(storedLocale)

app.use(createPinia())
app.use(router)
app.use(i18n)
app.use(ElementPlus)

// 暴露语言切换方法供全局使用
app.config.globalProperties.$setLocale = (locale: Locale) => {
  setLocale(locale)
}

// 全局错误收集（必须在 mount 之前）
setupErrorHandler(app)

app.mount('#app')
