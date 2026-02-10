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
 * 导入 Vue 的生命周期钩子
 * onMounted: 组件挂载完成后执行的钩子函数
 * 适合在这里执行初始化操作，比如获取用户信息
 */
import { onMounted } from 'vue'

/**
 * 导入 Pinia store
 * useLoginUserStore: 登录用户状态管理仓库
 * 用于管理全局的用户登录状态
 */
import { useLoginUserStore } from '@/stores/loginUser.ts'

/**
 * 获取登录用户 store 实例
 * 
 * 调用 useLoginUserStore() 会返回一个 store 对象，包含：
 * - loginUser: 响应式的用户信息状态
 * - fetchLoginUser: 获取用户信息的方法
 * - setLoginUser: 设置用户信息的方法
 */
const loginUserStore = useLoginUserStore()

/**
 * 组件挂载后执行
 * 
 * 为什么在这里获取用户信息？
 * 1. App.vue 是根组件，最先加载
 * 2. 在这里获取用户信息，确保所有子组件都能访问到用户状态
 * 3. 只需要获取一次，避免重复请求
 * 
 * 执行流程：
 * 1. 用户打开网站
 * 2. App.vue 组件挂载
 * 3. 调用 fetchLoginUser() 向后端请求用户信息
 * 4. 如果用户已登录，后端返回用户信息并更新 store
 * 5. 所有使用 loginUserStore 的组件都会自动更新显示
 */
onMounted(() => {
  // 调用 store 中的方法，从后端获取登录用户信息
  loginUserStore.fetchLoginUser()
})

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
