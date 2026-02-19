<template>
  <div>
    <div v-for="(step, i) in data.steps" :key="i" class="core-idea">
      <div class="step-title">{{ step.title }}</div>
      <div v-html="step.content" />
      <div v-if="step.formula" class="formula">{{ step.formula }}</div>
    </div>

    <div class="core-idea">
      <div class="step-title">思考总结：演进路径</div>
      <div class="flow-chart">
        <template v-for="(node, i) in data.summary.flowNodes" :key="i">
          <div v-if="i > 0" class="flow-arrow">→</div>
          <div
            class="flow-node"
            :class="{ 'highlight-node': node.highlight }"
          >
            {{ node.text }}
          </div>
        </template>
      </div>
      <p
        v-if="data.summary.footnote"
        style="text-align:center; color:#888; margin-top:10px;"
      >
        {{ data.summary.footnote }}
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { CoreIdeaData } from '../types'

defineProps<{ data: CoreIdeaData }>()
</script>
