<!--
  AboutPage.vue - 关于页面
  功能：展示项目介绍和相关信息
-->
<template>
  <div class="about-page">
    <a-card title="关于 AI 代码母体" :bordered="false">
      <a-descriptions :column="1" bordered>
        <a-descriptions-item label="项目名称">
          AI 代码母体
        </a-descriptions-item>
        <a-descriptions-item label="版本">
          1.0.0
        </a-descriptions-item>
        <a-descriptions-item label="作者">
          <a href="https://github.com/usagi3067" target="_blank" rel="noopener noreferrer">
            dango
          </a>
        </a-descriptions-item>
        <a-descriptions-item label="技术栈">
          Vue 3 + TypeScript + Ant Design Vue + Vite
        </a-descriptions-item>
        <a-descriptions-item label="项目简介">
          一个基于 AI 的智能代码生成平台，旨在帮助开发者快速构建高质量的应用程序。
          通过 AI 辅助，提供代码生成、智能提示、自动优化等功能，大幅提升开发效率。
        </a-descriptions-item>
      </a-descriptions>
    </a-card>

    <a-card title="核心功能" :bordered="false" style="margin-top: 24px">
      <a-list :data-source="features" :split="false">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta>
              <template #avatar>
                <a-avatar :style="{ backgroundColor: '#1890ff' }">
                  {{ item.icon }}
                </a-avatar>
              </template>
              <template #title>{{ item.title }}</template>
              <template #description>{{ item.description }}</template>
            </a-list-item-meta>
          </a-list-item>
        </template>
      </a-list>
    </a-card>

    <a-card title="联系方式" :bordered="false" style="margin-top: 24px">
      <a-space direction="vertical" size="middle">
        <div>
          <strong>GitHub：</strong>
          <a href="https://github.com/usagi3067" target="_blank" rel="noopener noreferrer">
            https://github.com/usagi3067
          </a>
        </div>
        <div>
          <strong>邮箱：</strong>
          <a href="mailto:contact@example.com">contact@example.com</a>
        </div>
      </a-space>
    </a-card>
  </div>
</template>

<script setup lang="ts">
/**
 * AboutPage 组件
 * 
 * 展示项目的详细信息：
 * 1. 项目基本信息
 * 2. 核心功能列表
 * 3. 联系方式
 */

/**
 * 功能列表数据
 */
const features = [
  {
    icon: '🚀',
    title: '快速开发',
    description: '通过 AI 辅助，快速生成代码模板，提高开发效率'
  },
  {
    icon: '💡',
    title: '智能提示',
    description: '智能代码补全和优化建议，让代码更加优雅'
  },
  {
    icon: '🔧',
    title: '灵活配置',
    description: '支持多种配置选项，满足不同项目需求'
  },
  {
    icon: '📦',
    title: '组件库',
    description: '丰富的组件库，开箱即用，快速搭建界面'
  },
  {
    icon: '🔒',
    title: '安全可靠',
    description: '完善的权限控制和数据加密，保障系统安全'
  },
  {
    icon: '📱',
    title: '响应式设计',
    description: '完美适配各种设备，提供一致的用户体验'
  }
]
</script>

<style scoped>
.about-page {
  max-width: 1000px;
  margin: 0 auto;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .about-page {
    padding: 0 16px;
  }
}
</style>
