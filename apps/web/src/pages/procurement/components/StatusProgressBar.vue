<template>
  <div class="progress-bar">
    <div v-for="(status, idx) in stepStatuses" :key="idx" class="step-wrapper">
      <div class="step-circle" :class="stepClass(status, idx + 1)" :title="stepLabel(idx + 1, status)">
        <span class="step-number">{{ idx + 1 }}</span>
      </div>
      <div class="step-label">{{ stepName(idx + 1) }}</div>
      <div v-if="idx < stepStatuses.length - 1" class="step-arrow">→</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { StepStatus } from '@/api/orderOverview'

const props = defineProps<{
  stepStatuses: StepStatus[]
  currentStep: number
}>()

const STEP_NAMES = ['补货', '发注', '验货', '调配', '国报', '日报', '退税', '运营']

function stepName(n: number) {
  return STEP_NAMES[n - 1] ?? ''
}

function stepLabel(n: number, status: StepStatus) {
  const name = stepName(n)
  const statusText = status === 'COMPLETED' ? '已完成' : status === 'IN_PROGRESS' ? '进行中' : '未开始'
  return `${name} - ${statusText}`
}

function stepClass(status: StepStatus, n: number) {
  const base = 'step-circle--' + (status === 'COMPLETED' ? 'completed' : status === 'IN_PROGRESS' ? 'progress' : 'pending')
  const active = n === props.currentStep ? ' step-circle--active' : ''
  return base + active
}
</script>

<style scoped>
.progress-bar {
  display: flex;
  align-items: center;
  padding: 16px 24px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 16px;
  overflow-x: auto;
  gap: 4px;
}
.step-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  position: relative;
}
.step-circle {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  border: 2px solid transparent;
  transition: all 0.2s;
}
.step-circle--completed {
  background: #67C23A;
  color: #fff;
  border-color: #67C23A;
}
.step-circle--progress {
  background: #E6A23C;
  color: #fff;
  border-color: #E6A23C;
}
.step-circle--pending {
  background: #fff;
  color: #909399;
  border-color: #dcdfe6;
}
.step-circle--active {
  box-shadow: 0 0 0 4px rgba(230, 162, 60, 0.3);
  transform: scale(1.1);
}
.step-number {
  line-height: 1;
}
.step-label {
  font-size: 11px;
  color: #606266;
  white-space: nowrap;
}
.step-arrow {
  color: #c0c4cc;
  font-size: 16px;
  margin: 0 4px;
  align-self: flex-start;
  margin-top: 8px;
}
</style>
