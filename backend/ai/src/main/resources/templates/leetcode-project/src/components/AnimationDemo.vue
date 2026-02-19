<template>
  <div>
    <div class="solution-tabs">
      <button
        v-for="sol in solutions"
        :key="sol.id"
        class="solution-tab"
        :class="{ active: activeSolution === sol.id }"
        @click="activeSolution = sol.id"
      >
        {{ sol.name }}
        <span class="complexity-badge" :class="sol.complexityLevel">
          {{ sol.complexity }}
        </span>
      </button>
    </div>

    <div
      v-for="sol in solutions"
      :key="sol.id"
      class="solution-content"
      :class="{ active: activeSolution === sol.id }"
    >
      <div class="split-layout">
        <div class="code-panel">
          <CodePanel
            :code="sol.code"
            :solution-id="sol.id"
            :highlight-lines="getStep(sol.id).highlightLines"
            :highlight-green-lines="getStep(sol.id).highlightGreenLines"
          />
        </div>
        <div class="visualization-panel">
          <component
            :is="sol.visualization"
            :step="getStep(sol.id)"
            :step-index="anims[sol.id].index"
            :total-steps="sol.steps.length"
          />
          <AnimationControls
            :current="anims[sol.id].index"
            :total="sol.steps.length"
            :playing="anims[sol.id].playing"
            @prev="anims[sol.id].prev()"
            @next="anims[sol.id].next()"
            @toggle="anims[sol.id].toggle()"
            @reset="anims[sol.id].reset()"
          />
          <ExplanationBox :html="getStep(sol.id).explanation" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import type { SolutionData } from '../types'
import { useAnimation } from '../composables/useAnimation'
import CodePanel from './CodePanel.vue'
import AnimationControls from './AnimationControls.vue'
import ExplanationBox from './ExplanationBox.vue'

const props = defineProps<{ solutions: SolutionData[] }>()
const activeSolution = ref(props.solutions[0]?.id ?? '')

type AnimState = {
  index: number
  playing: boolean
  next: () => void
  prev: () => void
  reset: () => void
  toggle: () => void
}

const anims = reactive(
  Object.fromEntries(
    props.solutions.map(sol => [sol.id, useAnimation(sol.steps.length)])
  )
) as unknown as Record<string, AnimState>

function getStep(solId: string) {
  const sol = props.solutions.find(s => s.id === solId)!
  return sol.steps[anims[solId].index]
}

function onKeydown(e: KeyboardEvent) {
  if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) return
  const anim = anims[activeSolution.value]
  if (!anim) return
  if (e.key === 'ArrowRight') { e.preventDefault(); anim.next() }
  else if (e.key === 'ArrowLeft') { e.preventDefault(); anim.prev() }
  else if (e.key === ' ') { e.preventDefault(); anim.toggle() }
  else if (e.key === 'r' || e.key === 'R') { e.preventDefault(); anim.reset() }
}

onMounted(() => document.addEventListener('keydown', onKeydown))
onUnmounted(() => document.removeEventListener('keydown', onKeydown))
</script>
