<template>
  <span v-if="loading">
    <el-icon class="is-loading"><Loading /></el-icon>
  </span>
</template>

<script setup lang="ts">
import { Loading } from '@element-plus/icons-vue'

defineProps<{ loading?: boolean }>()
</script>
