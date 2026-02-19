<template>
  <div class="flow-diagram">
    <div v-if="topic.flowSteps?.length" class="flow-container">
      <div v-for="(step, i) in topic.flowSteps" :key="step.id" class="flow-item">
        <div class="flow-step">
          <div class="step-number">{{ i + 1 }}</div>
          <div class="step-content">
            <div class="step-label">{{ step.label }}</div>
            <div class="step-desc">{{ step.description }}</div>
          </div>
        </div>
        <div v-if="i < topic.flowSteps!.length - 1" class="flow-arrow">↓</div>
      </div>
    </div>
    <div v-else class="empty-hint">暂无流程数据</div>
  </div>
</template>

<script setup lang="ts">
import type { VisualizationProps } from '@/types'

defineProps<VisualizationProps>()
</script>

<style scoped>
.flow-diagram { padding: 15px; }
.flow-container {
  display: flex; flex-direction: column; align-items: center; gap: 4px;
}
.flow-item { display: flex; flex-direction: column; align-items: center; width: 100%; max-width: 500px; }
.flow-step {
  display: flex; align-items: flex-start; gap: 14px; width: 100%;
  padding: 16px; background: var(--card-bg); border-radius: 10px;
  border-left: 4px solid var(--accent); transition: background 0.2s;
}
.flow-step:hover { background: var(--card-hover); }
.step-number {
  min-width: 32px; height: 32px; border-radius: 50%;
  background: var(--accent); color: white; display: flex;
  align-items: center; justify-content: center; font-weight: 700; font-size: 14px;
}
.step-label { color: var(--text); font-weight: 600; margin-bottom: 4px; }
.step-desc { color: var(--text-secondary); font-size: 13px; line-height: 1.6; }
.flow-arrow { color: var(--accent); font-size: 20px; line-height: 1; }
.empty-hint {
  text-align: center; color: var(--muted); padding: 40px;
  background: var(--card-bg); border-radius: 10px;
}
</style>
