<template>
  <div v-if="logs.length > 0" class="workflow-log-panel">
    <div class="log-header" @click="expanded = !expanded">
      <span class="log-toggle">{{ expanded ? '▼' : '▶' }}</span>
      <span class="log-summary">
        <template v-if="isComplete">
          <span class="log-icon done">✓</span> 工作流执行完成
        </template>
        <template v-else>
          <span class="log-icon running">⟳</span> {{ currentStepText }}
        </template>
      </span>
    </div>
    <div v-show="expanded" class="log-list">
      <div v-for="(log, index) in logs" :key="index" class="log-item">
        <span class="log-icon" :class="getLogStatus(index)">
          {{ getLogIcon(index) }}
        </span>
        <span class="log-text">{{ log }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

const props = defineProps<{
  logs: string[]
  isComplete: boolean
}>()

const expanded = ref(false)

const currentStepText = computed(() => {
  if (props.logs.length === 0) return '准备中...'
  const lastLog = props.logs[props.logs.length - 1]
  const match = lastLog.match(/^\[(.+?)\]/)
  return match ? `正在执行: ${match[1]}` : '执行中...'
})

const getLogStatus = (index: number) => {
  if (props.isComplete) return 'done'
  return index < props.logs.length - 1 ? 'done' : 'running'
}

const getLogIcon = (index: number) => {
  if (props.isComplete) return '✓'
  return index < props.logs.length - 1 ? '✓' : '⟳'
}
</script>

<style scoped>
.workflow-log-panel {
  margin-bottom: 8px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  font-size: 13px;
  background: #fafafa;
}

.log-header {
  padding: 6px 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  user-select: none;
}

.log-header:hover {
  background: #f5f5f5;
}

.log-toggle {
  font-size: 10px;
  color: #999;
  width: 12px;
}

.log-summary {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #666;
}

.log-list {
  padding: 4px 12px 8px 12px;
  border-top: 1px solid #f0f0f0;
}

.log-item {
  padding: 2px 0;
  display: flex;
  align-items: center;
  gap: 6px;
  color: #666;
}

.log-icon {
  font-size: 12px;
  width: 16px;
  text-align: center;
  flex-shrink: 0;
}

.log-icon.done {
  color: #52c41a;
}

.log-icon.running {
  color: #1890ff;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.log-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
