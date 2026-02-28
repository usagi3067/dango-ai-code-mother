<!--
  GlobalFooter.vue - 全局页脚组件
  功能：展示网站底部的版权信息和作者链接
-->
<template>
  <!-- 
    a-layout-footer: Ant Design Vue 提供的布局页脚组件
    class="footer": 绑定自定义样式类
  -->
  <a-layout-footer class="footer">
    <div class="footer-content">
      <!-- 采用更专业、克制的品牌描述 -->
      AI 应用生成平台 · 由
      <!-- 
        外部链接标签
        href: 链接地址
        target="_blank": 在新标签页打开链接
        rel="noopener noreferrer": 安全属性，防止新页面访问原页面的 window 对象
      -->
      <a href="https://github.com/usagi3067" target="_blank" rel="noopener noreferrer">
        dango
      </a>
    </div>
  </a-layout-footer>
</template>

<script setup lang="ts">
/**
 * script setup 是 Vue 3 的语法糖，用于简化组件的编写
 * lang="ts" 表示使用 TypeScript 语言
 * 这个组件是纯展示组件，不需要任何逻辑代码
 */
</script>

<style scoped>
/**
 * scoped 表示样式只在当前组件内生效，不会影响其他组件
 * 这是 Vue 的 CSS 作用域特性
 */

/* 页脚容器样式 */
.footer {
  text-align: center; /* 文本居中对齐 */
  background: #f8fafc; /* 与主布局保持统一底色 */
  border-top: 1px solid #e2e8f0; /* 轻分隔，弱化页脚存在感 */
  padding: 20px 24px; /* 控制页脚高度，避免厚重 */
  position: relative; /* 相对定位 */
  bottom: 0; /* 距离底部 0 */
  width: 100%; /* 宽度占满父容器 */
}

/* 页脚内容文本样式 */
.footer-content {
  color: #64748b;
  font-size: 14px; /* 字体大小 14 像素 */
}

/* 页脚中的链接样式 */
.footer-content a {
  color: #16a34a;
  text-decoration: none; /* 去除下划线 */
  transition: color 0.3s; /* 颜色变化过渡动画，持续 0.3 秒 */
  font-weight: 600;
}

/* 鼠标悬停在链接上时的样式 */
.footer-content a:hover {
  color: #15803d;
  text-decoration: none;
}

/**
 * 媒体查询：响应式设计
 * 当屏幕宽度小于等于 576px 时（手机屏幕）应用以下样式
 */
@media (max-width: 576px) {
  .footer {
    padding: 14px 16px;
  }

  .footer-content {
    font-size: 12px; /* 减小字体大小 */
  }
}
</style>
