<!--
  App.vue - Vue 应用的根组件
  这是整个应用的入口组件，所有其他组件都会在这里渲染
  在 main.ts 中被挂载到 index.html 的 #app 元素上
-->
<script setup lang="ts">
/**
 * 导入基础布局组件
 * BasicLayout 包含了整个应用的布局结构（头部、内容、页脚）
 * @/: 路径别名，指向 src 目录
 */
import BasicLayout from '@/layouts/BasicLayout.vue'

/**
 * 在 script setup 中：
 * - 导入的组件会自动注册，可以直接在模板中使用
 * - 不需要 return 语句
 * - 代码会在组件创建时执行
 */
</script>

<template>
  <!-- 
    渲染基础布局组件
    这是应用的唯一根组件，所有页面内容都会在 BasicLayout 中显示
    BasicLayout 内部使用 RouterView 来根据路由切换不同的页面
  -->
  <BasicLayout />
</template>

<style>
/**
 * 全局样式（没有 scoped 属性）
 * 这些样式会应用到整个应用的所有元素
 * 用于设置全局的基础样式和重置默认样式
 */

/**
 * 通配符选择器：选中所有元素
 * 重置浏览器的默认样式，确保不同浏览器显示一致
 */
* {
  margin: 0; /* 移除所有元素的外边距 */
  padding: 0; /* 移除所有元素的内边距 */
  box-sizing: border-box; /* 盒模型：宽高包含 padding 和 border */
  /**
   * box-sizing 说明：
   * - content-box（默认）：width = 内容宽度
   * - border-box：width = 内容 + padding + border
   * 使用 border-box 可以更方便地控制元素尺寸
   */
}

/**
 * body 元素样式
 * 设置整个页面的基础字体
 */
body {
  /**
   * 字体栈（Font Stack）：按优先级列出多个字体
   * 浏览器会从左到右依次尝试，使用第一个可用的字体
   * -apple-system: macOS 和 iOS 的系统字体
   * BlinkMacSystemFont: macOS Chrome 的系统字体
   * 'Segoe UI': Windows 的系统字体
   * Roboto: Android 的系统字体
   * 'Helvetica Neue': macOS 的经典字体
   * Arial: 通用的无衬线字体
   * sans-serif: 无衬线字体（兜底选项）
   */
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial,
    sans-serif;
}

/**
 * #app 元素样式
 * 这是 Vue 应用挂载的根 DOM 元素（在 index.html 中定义）
 */
#app {
  width: 100%; /* 宽度占满父容器 */
  height: 100%; /* 高度占满父容器 */
}
</style>
