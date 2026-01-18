<!--
  BasicLayout.vue - 基础布局组件
  功能：定义整个应用的基础布局结构（上中下三栏布局）
  这是应用的主要布局容器，所有页面都会在这个布局中渲染
-->
<template>
  <!-- 
    a-layout: Ant Design Vue 的布局容器组件
    class="basic-layout": 绑定自定义样式类
    这个组件会包裹整个应用的内容
  -->
  <a-layout class="basic-layout">
    <!-- 
      顶部：全局导航栏组件
      这是一个自定义组件，显示在页面最顶部
    -->
    <GlobalHeader />

    <!-- 
      中间：内容区域
      a-layout-content: Ant Design Vue 的内容区域组件
      这里会根据路由显示不同的页面内容
    -->
    <a-layout-content class="content">
      <!-- 内容包装器：用于控制内容的最大宽度和样式 -->
      <div class="content-wrapper">
        <!-- 
          RouterView: Vue Router 的路由视图组件
          作用：根据当前路由地址，动态渲染对应的页面组件
          例如：访问 / 时显示首页，访问 /about 时显示关于页面
        -->
        <RouterView />
      </div>
    </a-layout-content>

    <!-- 
      底部：全局页脚组件
      这是一个自定义组件，显示在页面最底部
    -->
    <GlobalFooter />
  </a-layout>
</template>

<script setup lang="ts">
/**
 * script setup 是 Vue 3 的组合式 API 语法糖
 * 优点：代码更简洁，不需要 return 导出，自动注册组件
 * lang="ts": 使用 TypeScript 语言
 */

/**
 * 导入 Vue Router 的路由视图组件
 * RouterView: 用于在模板中显示当前路由对应的页面组件
 */
import { RouterView } from 'vue-router'

/**
 * 导入自定义组件
 * @/: 路径别名，指向 src 目录（在 vite.config.ts 中配置）
 * .vue: Vue 单文件组件的扩展名
 */
import GlobalHeader from '@/components/GlobalHeader.vue' // 全局头部组件
import GlobalFooter from '@/components/GlobalFooter.vue' // 全局页脚组件

/**
 * 注意：在 script setup 中导入的组件会自动注册
 * 不需要像 Vue 2 那样在 components 选项中手动注册
 */
</script>

<style scoped>
/**
 * scoped: CSS 作用域限制
 * 这里定义的样式只会应用到当前组件，不会影响其他组件
 * Vue 会自动为每个元素添加唯一的属性选择器来实现样式隔离
 */

/* 基础布局容器样式 */
.basic-layout {
  min-height: 100vh; /* 最小高度为视口高度的 100%（vh = viewport height） */
  display: flex; /* 使用 Flexbox 弹性布局 */
  flex-direction: column; /* 子元素垂直排列（从上到下） */
  /**
   * 布局说明：
   * - GlobalHeader 在顶部
   * - content 在中间，会自动占据剩余空间
   * - GlobalFooter 在底部
   */
}

/* 内容区域样式 */
.content {
  flex: 1; /* 占据剩余的所有空间，确保页脚始终在底部 */
  background: #f0f2f5; /* 浅灰色背景，与 Ant Design 风格一致 */
  padding: 24px; /* 内边距 24px，为内容提供呼吸空间 */
}

/* 内容包装器样式 */
.content-wrapper {
  max-width: 1200px; /* 最大宽度 1200px，防止在大屏幕上内容过宽 */
  margin: 0 auto; /* 水平居中：上下 0，左右 auto */
  background: #fff; /* 白色背景 */
  padding: 24px; /* 内边距 24px */
  border-radius: 4px; /* 圆角 4px，使边角更柔和 */
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1); /* 阴影效果，增加层次感 */
  /**
   * 最小高度计算：
   * 100vh: 视口高度
   * - 64px: 头部高度
   * - 72px: 页脚高度
   * - 48px: 上下内边距（24px * 2）
   * 确保内容区域至少占满屏幕，避免页脚浮动
   */
  min-height: calc(100vh - 64px - 72px - 48px);
}

/**
 * 响应式设计：媒体查询
 * 当屏幕宽度小于等于 768px 时（平板设备）
 */
@media (max-width: 768px) {
  .content {
    padding: 16px; /* 减小内边距以适应中等屏幕 */
  }

  .content-wrapper {
    padding: 16px; /* 减小内容包装器的内边距 */
  }
}

/**
 * 响应式设计：媒体查询
 * 当屏幕宽度小于等于 576px 时（手机设备）
 */
@media (max-width: 576px) {
  .content {
    padding: 12px; /* 进一步减小内边距 */
  }

  .content-wrapper {
    padding: 12px; /* 进一步减小内容包装器的内边距 */
    border-radius: 0; /* 移除圆角，让内容占满整个宽度 */
  }
}
</style>
