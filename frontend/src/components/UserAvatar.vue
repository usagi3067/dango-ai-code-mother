<!--
  UserAvatar.vue - 用户头像组件
  
  【功能】
  显示用户头像，无头像时显示用户名首字
  
  【Props】
  - src: 头像图片地址
  - name: 用户名（用于显示首字）
  - size: 头像大小，默认 32
-->
<template>
  <a-avatar :src="src" :size="size">
    {{ displayChar }}
  </a-avatar>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  src?: string
  name?: string
  size?: number
}>(), {
  size: 32
})

/**
 * 计算显示的字符
 * 优先显示用户名首字，否则显示 ?
 */
const displayChar = computed(() => {
  return props.name?.charAt(0) || '?'
})
</script>
