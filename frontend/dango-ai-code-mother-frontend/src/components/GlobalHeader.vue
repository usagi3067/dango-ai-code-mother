<!--
  GlobalHeader.vue - 全局导航栏组件
  功能：展示网站顶部导航，包括 logo、标题、菜单和用户操作区
-->
<template>
  <!-- 
    a-layout-header: Ant Design Vue 提供的布局头部组件
    class="header": 绑定自定义样式类
  -->
  <a-layout-header class="header">
    <!-- 头部内容容器，用于控制最大宽度和布局 -->
    <div class="header-content">
      <!-- Logo 区域：包含图标和网站标题 -->
      <div class="logo-section">
        <!-- 
          Logo 图片
          src="@/assets/logo.png": @ 是 Vue 项目中的路径别名，指向 src 目录
          alt: 图片加载失败时显示的替代文本
        -->
        <img src="@/assets/logo.png" alt="Logo" class="logo" />
        <!-- 网站标题 -->
        <span class="title">AI 代码母体</span>
      </div>

      <!-- 
        导航菜单组件
        v-model:selectedKeys: 双向绑定选中的菜单项 key 值
        mode="horizontal": 水平模式显示菜单
        :items: 绑定菜单项数据（冒号表示绑定动态数据）
        class="menu": 自定义样式类
      -->
      <a-menu
        v-model:selectedKeys="selectedKeys"
        mode="horizontal"
        :items="menuItems"
        class="menu"
      />

      <!-- 用户操作区域：目前显示登录按钮 -->
      <div class="user-section">
        <!-- 
          Ant Design 按钮组件
          type="primary": 主要按钮样式（蓝色背景）
        -->
        <a-button type="primary">登录</a-button>
      </div>
    </div>
  </a-layout-header>
</template>

<script setup lang="ts">
/**
 * 导入 Vue 3 的响应式 API
 * ref: 用于创建响应式数据，数据变化时会自动更新视图
 * onMounted: 生命周期钩子，组件挂载后执行
 */
import { ref, onMounted } from 'vue'

/**
 * 导入 Vue Router 的路由钩子
 * useRouter: 用于获取路由实例，可以进行页面跳转等操作
 * useRoute: 用于获取当前路由信息
 */
import { useRouter, useRoute } from 'vue-router'

/**
 * 导入 Ant Design Vue 的类型定义
 * type: TypeScript 的类型导入关键字
 * MenuProps: 菜单组件的属性类型定义
 */
import type { MenuProps } from 'ant-design-vue'

/**
 * 导入统一的路由配置
 * 
 * getMenuRoutes: 获取需要在菜单中显示的路由
 * findRouteByPath: 根据路径查找路由配置
 * 
 * 优点：
 * 1. 菜单配置自动从路由配置生成，无需重复定义
 * 2. 路由和菜单保持同步，避免不一致
 * 3. 新增页面只需在配置文件中添加一条记录
 */
import { getMenuRoutes, findRouteByPath } from '@/config/routes'

/**
 * 获取路由实例
 * 用于在菜单点击时进行页面跳转
 */
const router = useRouter()

/**
 * 获取当前路由信息
 * 用于获取当前页面的路由路径，实现菜单高亮同步
 */
const route = useRoute()

/**
 * 创建响应式数据：当前选中的菜单项
 * ref<string[]>: TypeScript 泛型语法，指定数据类型为字符串数组
 * []: 初始为空数组，会在组件挂载后根据当前路由设置
 */
const selectedKeys = ref<string[]>([])

/**
 * 根据路由路径更新菜单高亮
 * 
 * 优化后的版本：
 * - 使用 findRouteByPath 自动查找匹配的路由
 * - 不需要手动编写 if-else 判断
 * - 新增路由时无需修改此函数
 * 
 * 这个函数会在页面加载和路由变化时调用
 */
const updateSelectedKeys = () => {
  // 获取当前路由的路径，例如：'/' 或 '/about'
  const currentPath = route.path
  
  // 从配置中查找匹配的路由
  const matchedRoute = findRouteByPath(currentPath)
  
  // 如果找到匹配的路由，设置菜单高亮
  if (matchedRoute) {
    selectedKeys.value = [matchedRoute.name]
  } else {
    // 如果没有找到匹配的路由，清空选中状态
    selectedKeys.value = []
  }
}

/**
 * 组件挂载后执行
 * onMounted: Vue 3 的生命周期钩子
 * 在组件首次渲染到页面后执行，确保能正确获取当前路由
 */
onMounted(() => {
  // 初始化菜单高亮：根据当前 URL 设置选中的菜单项
  updateSelectedKeys()
})

/**
 * 监听路由变化，自动更新菜单高亮
 * router.afterEach: 全局后置路由守卫
 * 在每次路由跳转完成后执行，确保菜单高亮与当前页面同步
 */
router.afterEach(() => {
  // 路由跳转完成后，更新菜单高亮
  updateSelectedKeys()
})

/**
 * 菜单项配置数组
 * 
 * 优化后的版本：
 * - 从统一配置文件自动生成菜单项
 * - 不需要手动编写菜单配置
 * - 路由和菜单自动保持同步
 * 
 * 工作原理：
 * 1. getMenuRoutes() 获取所有需要显示在菜单中的路由
 * 2. map() 将路由配置转换为 Ant Design Menu 需要的格式
 * 3. 自动生成 key、label 和 onClick 处理函数
 */
const menuItems: MenuProps['items'] = getMenuRoutes().map(route => ({
  key: route.name,  // 使用路由名称作为菜单 key
  label: route.meta?.menuLabel || route.meta?.title || route.name,  // 菜单显示文本
  onClick: () => router.push(route.path)  // 点击时跳转到对应路径
}))

/**
 * 菜单配置说明：
 * 
 * 1. key: 菜单项的唯一标识，使用路由名称
 * 2. label: 菜单显示的文本，优先级：
 *    - menuLabel（菜单专用文本）
 *    - title（页面标题）
 *    - name（路由名称，兜底）
 * 3. onClick: 点击处理函数，跳转到对应的路由路径
 * 
 * 新增菜单的方法：
 * 只需在 src/config/routes.ts 中添加路由配置即可
 * 菜单会自动生成，无需修改此文件
 */
</script>

<style scoped>
/**
 * scoped 样式：只在当前组件内生效
 * 避免样式污染其他组件
 */

/* 头部容器样式 */
.header {
  background: #fff; /* 白色背景 */
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1); /* 阴影效果：水平偏移 0，垂直偏移 2px，模糊 8px */
  padding: 0; /* 去除默认内边距 */
  position: sticky; /* 粘性定位：滚动时固定在顶部 */
  top: 0; /* 固定在顶部位置 */
  z-index: 999; /* 层级：确保在其他元素之上 */
}

/* 头部内容容器：控制布局和最大宽度 */
.header-content {
  max-width: 1200px; /* 最大宽度 1200px */
  margin: 0 auto; /* 水平居中 */
  display: flex; /* 使用 Flexbox 布局 */
  align-items: center; /* 垂直居中对齐 */
  justify-content: space-between; /* 两端对齐，中间自动分配空间 */
  padding: 0 24px; /* 左右内边距 24px */
  height: 64px; /* 固定高度 64px */
}

/* Logo 区域样式 */
.logo-section {
  display: flex; /* Flexbox 布局 */
  align-items: center; /* 垂直居中 */
  gap: 12px; /* 子元素之间的间距 12px */
  flex-shrink: 0; /* 不允许缩小，保持固定大小 */
}

/* Logo 图片样式 */
.logo {
  height: 40px; /* 固定高度 40px */
  width: auto; /* 宽度自动，保持图片比例 */
}

/* 网站标题样式 */
.title {
  font-size: 18px; /* 字体大小 18px */
  font-weight: 600; /* 字体粗细：半粗体 */
  color: #1890ff; /* Ant Design 主题蓝色 */
  white-space: nowrap; /* 不换行，保持在一行显示 */
}

/* 菜单样式 */
.menu {
  flex: 1; /* 占据剩余空间，自动伸缩 */
  border-bottom: none; /* 去除底部边框 */
  line-height: 64px; /* 行高与头部高度一致，实现垂直居中 */
  margin: 0 24px; /* 左右外边距 24px */
}

/* 用户操作区域样式 */
.user-section {
  flex-shrink: 0; /* 不允许缩小，保持固定大小 */
}

/**
 * 媒体查询：平板设备（屏幕宽度 ≤ 768px）
 * 调整布局以适应中等屏幕
 */
@media (max-width: 768px) {
  .header-content {
    padding: 0 16px; /* 减小左右内边距 */
  }

  .title {
    font-size: 16px; /* 减小标题字体 */
  }

  .menu {
    margin: 0 12px; /* 减小菜单左右间距 */
  }

  .logo {
    height: 32px; /* 减小 Logo 高度 */
  }
}

/**
 * 媒体查询：手机设备（屏幕宽度 ≤ 576px）
 * 进一步优化小屏幕显示
 */
@media (max-width: 576px) {
  .title {
    display: none; /* 隐藏标题，节省空间 */
  }

  .menu {
    margin: 0 8px; /* 进一步减小菜单间距 */
  }
}
</style>
