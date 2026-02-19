<template>
  <div class="answer-guide">
    <div class="guide-section">
      <h3 class="section-title">回答结构</h3>
      <div class="structure-card">{{ topic.answerStructure }}</div>
    </div>
    <div class="guide-section">
      <h3 class="section-title">详细要点</h3>
      <div v-for="(point, i) in explanation.detailedPoints" :key="i" class="detail-card">
        <div class="detail-title">{{ point.title }}</div>
        <div class="detail-content">{{ point.content }}</div>
      </div>
    </div>
    <div class="guide-section">
      <h3 class="section-title">面试技巧</h3>
      <div v-for="(tip, i) in explanation.interviewTips" :key="i" class="tip-card">
        <span class="tip-icon">💡</span>
        <span class="tip-text">{{ tip }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { VisualizationProps } from '@/types'

defineProps<VisualizationProps>()
</script>

<style scoped>
.answer-guide { padding: 15px; display: flex; flex-direction: column; gap: 20px; }
.section-title { color: var(--accent); font-size: 16px; margin-bottom: 10px; }
.structure-card {
  padding: 16px; background: var(--card-bg); border-radius: 10px;
  border-left: 4px solid var(--highlight); color: var(--text); line-height: 1.8;
}
.detail-card {
  padding: 14px 16px; background: var(--card-bg); border-radius: 10px;
  margin-bottom: 8px; border-left: 4px solid var(--secondary);
}
.detail-title { color: var(--text); font-weight: 600; margin-bottom: 6px; }
.detail-content { color: var(--text-secondary); font-size: 14px; line-height: 1.7; }
.tip-card {
  display: flex; align-items: flex-start; gap: 10px;
  padding: 12px 16px; background: var(--card-bg); border-radius: 10px;
  margin-bottom: 6px;
}
.tip-icon { font-size: 16px; line-height: 1.5; }
.tip-text { color: var(--text); line-height: 1.6; }
</style>
