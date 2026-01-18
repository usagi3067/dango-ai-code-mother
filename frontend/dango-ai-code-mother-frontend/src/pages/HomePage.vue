<!--
  HomePage.vue - 首页
  功能：展示应用的主页内容
-->
<template>
  <div class="home-page">
    <div class="welcome-section">
      <h1 class="title">欢迎使用 AI 代码母体</h1>
      <p class="description">
        一个基于 AI 的智能代码生成平台，帮助开发者快速构建高质量的应用程序
      </p>
    </div>

    <div class="features-section">
      <a-row :gutter="[24, 24]">
        <a-col :xs="24" :sm="12" :lg="8">
          <a-card hoverable>
            <template #cover>
              <div class="feature-icon">🚀</div>
            </template>
            <a-card-meta title="快速开发" description="通过 AI 辅助，快速生成代码模板，提高开发效率" />
          </a-card>
        </a-col>

        <a-col :xs="24" :sm="12" :lg="8">
          <a-card hoverable>
            <template #cover>
              <div class="feature-icon">💡</div>
            </template>
            <a-card-meta title="智能提示" description="智能代码补全和优化建议，让代码更加优雅" />
          </a-card>
        </a-col>

        <a-col :xs="24" :sm="12" :lg="8">
          <a-card hoverable>
            <template #cover>
              <div class="feature-icon">🔧</div>
            </template>
            <a-card-meta title="灵活配置" description="支持多种配置选项，满足不同项目需求" />
          </a-card>
        </a-col>
      </a-row>
    </div>

    <div class="action-section">
      <a-space size="large">
        <a-button type="primary" size="large">开始使用</a-button>
        <a-button size="large">了解更多</a-button>
      </a-space>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * HomePage 组件
 * 
 * 这是应用的主页，展示：
 * 1. 欢迎信息
 * 2. 核心功能介绍
 * 3. 快速操作入口
 */
</script>

<style scoped>
.home-page {
  padding: 40px 0;
}

/* 欢迎区域 */
.welcome-section {
  text-align: center;
  margin-bottom: 60px;
}

.title {
  font-size: 48px;
  font-weight: 600;
  color: #1890ff;
  margin-bottom: 20px;
}

.description {
  font-size: 18px;
  color: rgba(0, 0, 0, 0.65);
  max-width: 600px;
  margin: 0 auto;
  line-height: 1.8;
}

/* 功能区域 */
.features-section {
  margin-bottom: 60px;
}

.feature-icon {
  font-size: 64px;
  text-align: center;
  padding: 40px 0;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
}

/* 操作区域 */
.action-section {
  text-align: center;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .title {
    font-size: 32px;
  }

  .description {
    font-size: 16px;
  }

  .feature-icon {
    font-size: 48px;
    padding: 30px 0;
  }
}
</style>
