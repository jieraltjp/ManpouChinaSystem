<template>
  <el-card class="step-card" :class="cardClass" shadow="never">
    <template #header>
      <div class="step-card__header">
        <div class="step-card__title">
          <span class="step-badge">{{ stepNumber }}</span>
          <span>{{ title }}</span>
        </div>
        <div class="step-card__actions">
          <el-tag v-if="status === 'COMPLETED'" type="success" size="small">{{ $t('orderOverview.stepStatusUI.status.completed') }}</el-tag>
          <el-tag v-else-if="status === 'IN_PROGRESS'" type="warning" size="small">{{ $t('orderOverview.stepStatusUI.status.inProgress') }}</el-tag>
        </div>
      </div>
    </template>
    <slot />
  </el-card>
</template>

<script setup lang="ts">
import type { StepStatus } from '@/api/orderOverview'

const props = defineProps<{
  stepNumber: number
  title: string
  status: StepStatus
}>()

const cardClass = {
  'step-card--completed': props.status === 'COMPLETED',
  'step-card--progress': props.status === 'IN_PROGRESS',
  'step-card--pending': props.status === 'NOT_STARTED',
}
</script>

<style scoped>
.step-card {
  border-left: 4px solid #dcdfe6;
  transition: border-color 0.2s;
}
.step-card--completed {
  border-left-color: #67C23A;
}
.step-card--progress {
  border-left-color: #E6A23C;
}
.step-card--pending {
  border-left-color: #dcdfe6;
}
.step-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.step-card__title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 14px;
  color: #303133;
}
.step-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #409EFF;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}
.step-card--completed .step-badge {
  background: #67C23A;
}
.step-card--progress .step-badge {
  background: #E6A23C;
}
</style>
