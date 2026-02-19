<template>
  <div>
    <table class="compare-table">
      <thead>
        <tr>
          <th>解法</th>
          <th>时间复杂度</th>
          <th>空间复杂度</th>
          <th>核心思想</th>
          <th>为什么会想到？</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(item, i) in items" :key="i">
          <td>{{ item.name }}</td>
          <td><span :class="'tag-' + item.timeLevel">{{ item.timeComplexity }}</span></td>
          <td><span :class="'tag-' + item.spaceLevel">{{ item.spaceComplexity }}</span></td>
          <td>{{ item.coreIdea }}</td>
          <td>{{ item.whyThisApproach }}</td>
        </tr>
      </tbody>
    </table>

    <div
      v-for="(card, i) in cards"
      :key="i"
      class="recommend-card"
      :style="card.borderColor ? `border-left-color: ${card.borderColor}` : ''"
    >
      <h4 :style="card.borderColor ? `color: ${card.borderColor}` : ''">
        {{ card.title }}
      </h4>
      <p v-html="card.content" />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ComparisonItem, RecommendCard } from '../types'

defineProps<{
  items: ComparisonItem[]
  cards: RecommendCard[]
}>()
</script>
