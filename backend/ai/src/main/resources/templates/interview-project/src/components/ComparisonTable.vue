<template>
  <div class="comparison-table">
    <div v-if="topic.comparisons?.length">
      <div v-for="(comp, ci) in topic.comparisons" :key="ci" class="comp-section">
        <table class="compare-table">
          <thead>
            <tr>
              <th>项目</th>
              <th v-for="dim in comp.dimensions" :key="dim">{{ dim }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in comp.items" :key="item.name">
              <td class="item-name">{{ item.name }}</td>
              <td v-for="(val, vi) in item.values" :key="vi">{{ val }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    <div v-else class="empty-hint">暂无对比数据</div>
  </div>
</template>

<script setup lang="ts">
import type { VisualizationProps } from '@/types'

defineProps<VisualizationProps>()
</script>

<style scoped>
.comparison-table { padding: 15px; }
.comp-section { margin-bottom: 20px; }
.compare-table { width: 100%; border-collapse: collapse; }
.compare-table th,
.compare-table td {
  padding: 12px 16px; border-bottom: 1px solid var(--border);
  text-align: left; color: var(--text-secondary);
}
.compare-table th { background: var(--card-bg); color: var(--accent); font-weight: 600; }
.compare-table tr:hover { background: var(--card-hover); }
.item-name { color: var(--text); font-weight: 600; }
.empty-hint {
  text-align: center; color: var(--muted); padding: 40px;
  background: var(--card-bg); border-radius: 10px;
}
</style>
