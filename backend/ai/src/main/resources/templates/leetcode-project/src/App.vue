<template>
  <div class="container">
    <h1>力扣 {{ problem.number }}: {{ problem.title }}</h1>
    <div class="problem-desc" v-html="problem.description" />
    <TabContainer :tabs="mainTabs" v-model="activeTab">
      <template #core>
        <CoreIdea :data="problem.coreIdea" />
      </template>
      <template #compare>
        <CompareTable
          :items="problem.comparison"
          :cards="problem.recommendCards"
        />
      </template>
      <template #animation>
        <AnimationDemo :solutions="solutions" />
      </template>
    </TabContainer>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { problem } from './data/problem'
import { solutions } from './data/solutions'
import TabContainer from './components/TabContainer.vue'
import CoreIdea from './components/CoreIdea.vue'
import CompareTable from './components/CompareTable.vue'
import AnimationDemo from './components/AnimationDemo.vue'

const mainTabs = [
  { key: 'core', label: '💡 核心思路' },
  { key: 'compare', label: '📊 解法对比' },
  { key: 'animation', label: '🎬 动画演示' },
]
const activeTab = ref('core')
</script>
