<template>
  <el-config-provider :locale="currentElLocale">
    <RouterView />
  </el-config-provider>
</template>

<script setup lang="ts">
import { watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElConfigProvider } from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn'
import ja from 'element-plus/dist/locale/ja'

const route = useRoute()
const { t, locale } = useI18n()

const currentElLocale = computed(() => locale.value === 'ja' ? ja as any : zhCn as any)

watch(
  () => route.meta.titleKey,
  (titleKey) => {
    if (titleKey) {
      document.title = t(titleKey as string)
    }
  },
  { immediate: true },
)
</script>
