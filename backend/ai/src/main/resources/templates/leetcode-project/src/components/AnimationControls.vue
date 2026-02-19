<template>
  <div class="controls">
    <button class="btn-prev" @click="$emit('prev')">⬅ 上一步</button>
    <button class="btn-play" @click="$emit('toggle')">
      {{ playing ? '⏸ 暂停' : '▶ 播放' }}
    </button>
    <button class="btn-next" @click="$emit('next')">下一步 ➡</button>
    <button class="btn-reset" @click="$emit('reset')">↺ 重置</button>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  current: number
  total: number
  playing: boolean
}>()
defineEmits<{
  prev: []
  next: []
  toggle: []
  reset: []
}>()
</script>
