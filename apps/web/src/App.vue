<template>
  <RouterView />
</template>

<script setup lang="ts">
import { watch } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'

const route = useRoute()
const { t } = useI18n()

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
