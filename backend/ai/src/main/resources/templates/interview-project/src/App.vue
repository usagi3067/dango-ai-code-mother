<template>
  <div class="container">
    <div class="header">
      <span class="category-badge">{{ topic.category }}</span>
      <h1>{{ topic.title }}</h1>
      <p class="summary">{{ explanation.summary }}</p>
    </div>
    <TabContainer :tabs="mainTabs" v-model="activeTab">
      <template #concept>
        <ConceptMap :topic="topic" :explanation="explanation" />
      </template>
      <template #compare>
        <ComparisonTable :topic="topic" :explanation="explanation" />
      </template>
      <template #flow>
        <FlowDiagram :topic="topic" :explanation="explanation" />
      </template>
      <template #keypoints>
        <KeyPoints :topic="topic" :explanation="explanation" />
      </template>
      <template #guide>
        <AnswerGuide :topic="topic" :explanation="explanation" />
      </template>
    </TabContainer>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { topic } from '@/data/topic'
import { explanation } from '@/data/explanation'
import TabContainer from '@/components/TabContainer.vue'
import ConceptMap from '@/components/ConceptMap.vue'
import ComparisonTable from '@/components/ComparisonTable.vue'
import FlowDiagram from '@/components/FlowDiagram.vue'
import KeyPoints from '@/components/KeyPoints.vue'
import AnswerGuide from '@/components/AnswerGuide.vue'

const mainTabs = [
  { key: 'concept', label: '概念图谱' },
  { key: 'compare', label: '对比分析' },
  { key: 'flow', label: '流程图解' },
  { key: 'keypoints', label: '要点速记' },
  { key: 'guide', label: '回答指南' },
]
const activeTab = ref('concept')
</script>
