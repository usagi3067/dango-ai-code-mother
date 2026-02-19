<template>
  <div class="key-points">
    <div v-for="(point, i) in topic.keyPoints" :key="i" class="point-card">
      <div class="point-index">{{ i + 1 }}</div>
      <div class="point-text">{{ point }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { VisualizationProps } from '@/types'

defineProps<VisualizationProps>()
</script>

<style scoped>
.key-points { padding: 15px; display: flex; flex-direction: column; gap: 10px; }
.point-card {
  display: flex; align-items: flex-start; gap: 14px;
  padding: 16px; background: var(--card-bg); border-radius: 10px;
  border-left: 4px solid var(--secondary); transition: background 0.2s;
}
.point-card:hover { background: var(--card-hover); }
.point-index {
  min-width: 28px; height: 28px; border-radius: 50%;
  background: var(--secondary); color: white; display: flex;
  align-items: center; justify-content: center; font-weight: 700; font-size: 13px;
}
.point-text { color: var(--text); line-height: 1.7; }
</style>
