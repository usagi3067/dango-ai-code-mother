<!--
  BasicLayout.vue - 基础布局组件
  功能：定义整个应用的基础布局结构
  支持全屏模式（hideLayout）和普通布局模式
-->
<template>
  <!-- 全屏模式：不显示头部和底部 -->
  <template v-if="isFullScreen">
    <RouterView />
  </template>
  
  <!-- 普通布局模式 -->
  <a-layout v-else class="basic-layout">
    <GlobalHeader />
    <a-layout-content :class="['content', { 'content-home': isHomePage }]">
      <div v-if="!isHomePage" class="content-wrapper">
        <RouterView />
      </div>
      <RouterView v-else />
    </a-layout-content>
    <GlobalFooter />
  </a-layout>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'

const route = useRoute()

// 判断是否全屏模式
const isFullScreen = computed(() => {
  return route.meta?.hideLayout === true
})

// 判断是否首页（首页有特殊背景）
const isHomePage = computed(() => {
  return route.path === '/'
})
</script>

<style scoped>
.basic-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.content {
  flex: 1;
  background: #f0f2f5;
  padding: 24px;
}

.content-home {
  padding: 0;
  background: transparent;
}

.content-wrapper {
  max-width: 1200px;
  margin: 0 auto;
  background: #fff;
  padding: 24px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  min-height: calc(100vh - 64px - 72px - 48px);
}

@media (max-width: 768px) {
  .content {
    padding: 16px;
  }
  .content-wrapper {
    padding: 16px;
  }
}

@media (max-width: 576px) {
  .content {
    padding: 12px;
  }
  .content-wrapper {
    padding: 12px;
    border-radius: 0;
  }
}
</style>
