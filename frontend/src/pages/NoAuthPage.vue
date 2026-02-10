<!--
  NoAuthPage.vue - 无权限页面
  当用户访问没有权限的页面时显示
-->
<template>
  <!-- 
    无权限页面容器
    使用 Flexbox 布局，内容居中显示
  -->
  <div class="no-auth-page">
    <!-- 
      a-result: Ant Design 的结果页组件
      用于显示操作结果或状态信息
      
      status="403": 显示 403 状态（无权限）
      - 会自动显示对应的图标和颜色
      
      title: 主标题
      sub-title: 副标题，说明原因
    -->
    <a-result
      status="403"
      title="403"
      sub-title="抱歉，您没有权限访问此页面"
    >
      <!-- 
        extra 插槽：额外的操作按钮
        显示在标题下方
      -->
      <template #extra>
        <!-- 
          a-space: 自动为按钮添加间距
        -->
        <a-space>
          <!-- 
            返回首页按钮
            type="primary": 主要按钮样式（蓝色）
            @click: 点击事件，跳转到首页
          -->
          <a-button type="primary" @click="goHome">
            返回首页
          </a-button>
          
          <!-- 
            返回上一页按钮
            @click: 点击事件，返回上一页
          -->
          <a-button @click="goBack">
            返回上一页
          </a-button>
        </a-space>
      </template>
    </a-result>
  </div>
</template>

<script setup lang="ts">
/**
 * 导入 Vue Router 的路由钩子
 * useRouter: 用于获取路由实例，进行页面跳转
 */
import { useRouter } from 'vue-router'

/**
 * 获取路由实例
 * 用于页面跳转操作
 */
const router = useRouter()

/**
 * 返回首页
 * 
 * 点击"返回首页"按钮时调用
 * 跳转到首页路径 '/'
 */
const goHome = () => {
  router.push('/')
}

/**
 * 返回上一页
 * 
 * 点击"返回上一页"按钮时调用
 * router.back(): 返回浏览器历史记录的上一页
 * 
 * 注意：
 * - 如果没有上一页，这个方法不会有任何效果
 * - 相当于用户点击浏览器的后退按钮
 */
const goBack = () => {
  router.back()
}
</script>

<style scoped>
/**
 * scoped 样式：只在当前组件内生效
 * 避免样式污染其他组件
 */

/* 无权限页面容器样式 */
.no-auth-page {
  display: flex;              /* 使用 Flexbox 布局 */
  justify-content: center;    /* 水平居中 */
  align-items: center;        /* 垂直居中 */
  min-height: 100vh;          /* 最小高度为视口高度 */
  background: #f0f2f5;        /* 浅灰色背景 */
}

/**
 * 样式说明：
 * 
 * 1. display: flex
 *    - 启用 Flexbox 布局
 *    - 可以轻松实现居中对齐
 * 
 * 2. justify-content: center
 *    - 主轴（水平方向）居中对齐
 *    - 让内容在水平方向上居中
 * 
 * 3. align-items: center
 *    - 交叉轴（垂直方向）居中对齐
 *    - 让内容在垂直方向上居中
 * 
 * 4. min-height: 100vh
 *    - 最小高度为视口高度（viewport height）
 *    - 确保页面至少占满整个屏幕
 *    - vh 是相对单位，100vh = 视口高度的 100%
 * 
 * 5. background: #f0f2f5
 *    - 设置背景颜色
 *    - 与 Ant Design 的默认背景色一致
 */
</style>
