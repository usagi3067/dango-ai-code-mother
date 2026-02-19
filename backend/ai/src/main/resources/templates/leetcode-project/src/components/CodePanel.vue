<template>
  <div class="code-section">
    <div class="code-header">
      <div class="lang-tabs">
        <button
          class="lang-tab"
          :class="{ active: activeLang === 'java' }"
          @click="activeLang = 'java'"
        >Java</button>
        <button
          class="lang-tab"
          :class="{ active: activeLang === 'cpp' }"
          @click="activeLang = 'cpp'"
        >C++</button>
      </div>
      <button class="copy-btn" :class="{ copied }" @click="copyCode">
        {{ copied ? '✅ 已复制' : '📋 复制' }}
      </button>
    </div>
    <div class="code-container">
      <div
        v-for="lang in (['java', 'cpp'] as const)"
        :key="lang"
        class="lang-code"
        :class="{ active: activeLang === lang }"
      >
        <div
          v-for="(line, i) in code[lang].lines"
          :key="i"
          class="code-line"
          :class="{
            highlight: hlLines[lang]?.includes(i + 1),
            'highlight-green': hlgLines[lang]?.includes(i + 1),
          }"
        >
          <span class="line-number">{{ i + 1 }}</span>
          <span class="line-content">
            <span
              v-for="(token, j) in line"
              :key="j"
              :class="token.type !== 'plain' ? token.type : undefined"
            >{{ token.text }}</span>
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { CodeData } from '../types'

const props = defineProps<{
  code: { java: CodeData; cpp: CodeData }
  solutionId: string
  highlightLines: { java: number[]; cpp: number[] }
  highlightGreenLines?: { java: number[]; cpp: number[] }
}>()

const activeLang = ref<'java' | 'cpp'>('java')
const copied = ref(false)

const hlLines = computed(() => props.highlightLines)
const hlgLines = computed(() => props.highlightGreenLines ?? { java: [], cpp: [] })

function copyCode() {
  const raw = props.code[activeLang.value].raw
  navigator.clipboard.writeText(raw).then(() => {
    copied.value = true
    setTimeout(() => { copied.value = false }, 2000)
  })
}
</script>
