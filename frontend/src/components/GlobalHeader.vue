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
        <!-- 顶部导航保留品牌图标，增强站点识别 -->
        <img src="@/assets/logo.png" alt="Logo" class="logo" />
        <!-- 网站标题 -->
        <span class="title">AI 应用生成</span>
      </div>

      <!-- 
        导航菜单组件
        v-model:selectedKeys: 双向绑定选中的菜单项 key 值
        mode="horizontal": 水平模式显示菜单
        :items: 绑定菜单项数据（冒号表示绑定动态数据）
        class="menu": 自定义样式类
      -->
      <a-menu v-model:selectedKeys="selectedKeys" mode="horizontal" :items="menuItems" class="menu" />

      <!-- 用户操作区域：显示用户信息或登录按钮 -->
      <div class="user-section">
        <UserDropdown />
      </div>
    </div>
  </a-layout-header>
</template>

<script setup lang="ts">
/**
 * 导入 Vue 3 的响应式 API
 * ref: 用于创建响应式数据，数据变化时会自动更新视图
 * onMounted: 生命周期钩子，组件挂载后执行
 * computed: 计算属性，依赖的数据变化时自动重新计算
 */
import { ref, onMounted, computed } from 'vue'

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
 * 导入 Pinia store
 * useLoginUserStore: 登录用户状态管理仓库
 */
import { useLoginUserStore } from '@/stores/loginUser'

/**
 * 导入用户下拉菜单组件
 */
import UserDropdown from '@/components/UserDropdown.vue'

/**
 * 获取登录用户 store 实例
 * 
 * 这个 store 在 App.vue 中已经初始化并获取了用户信息
 * 这里直接使用，可以访问到最新的用户状态
 * 
 * 包含的内容：
 * - loginUser: 用户信息对象（响应式）
 * - fetchLoginUser: 获取用户信息的方法
 * - setLoginUser: 设置用户信息的方法
 */
const loginUserStore = useLoginUserStore()

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
 * 菜单项配置数组（响应式）
 * 
 * 使用 computed 计算属性：
 * - 当 loginUserStore.loginUser 变化时，自动重新计算
 * - 用户登录/注销后，菜单会自动更新
 * 
 * 为什么要用 computed？
 * - 如果直接赋值，只会在组件初始化时计算一次
 * - 用户登录后，loginUser 变化了，但菜单不会更新
 * - 使用 computed 后，loginUser 变化时会自动重新计算菜单
 * 
 * 工作原理：
 * 1. getMenuRoutes(loginUserStore.loginUser) 获取用户有权限访问的路由
 * 2. 当 loginUserStore.loginUser 变化时，computed 会自动重新执行
 * 3. map() 将路由配置转换为 Ant Design Menu 需要的格式
 * 4. 自动生成 key、label 和 onClick 处理函数
 * 
 * 示例：
 * - 未登录：菜单显示 [首页, 关于]
 * - 登录普通用户：菜单显示 [首页, 关于]
 * - 登录管理员：菜单显示 [首页, 关于, 用户管理]
 */
const menuItems = computed<MenuProps['items']>(() => {
  return getMenuRoutes(loginUserStore.loginUser).map(route => ({
    key: route.name,  // 使用路由名称作为菜单 key
    label: route.meta?.menuLabel || route.meta?.title || route.name,  // 菜单显示文本
    onClick: () => router.push(route.path)  // 点击时跳转到对应路径
  }))
})

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
  /* 统一公共区的轻量质感：浅底 + 细边框 + 柔和阴影 */
  background: rgba(255, 255, 255, 0.92);
  border-bottom: 1px solid #e2e8f0;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.05);
  backdrop-filter: saturate(180%) blur(10px);
  padding: 0;
  position: sticky;
  top: 0;
  z-index: 999;
}

/* 头部内容容器：控制布局和最大宽度 */
.header-content {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 64px;
  gap: 20px;
}

/* Logo 区域样式 */
.logo-section {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

/* 顶部导航图标仅用于品牌识别，尺寸控制在低干扰范围 */
.logo {
  height: 34px;
  width: auto;
}

/* 网站标题样式 */
.title {
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
  white-space: nowrap;
  letter-spacing: 0.2px;
}

/* 菜单样式 */
.menu {
  flex: 1;
  border-bottom: none;
  line-height: 64px;
  margin: 0 12px;
  background: transparent;
}

/* 用户操作区域样式 */
.user-section {
  flex-shrink: 0;
}

/* 仅覆盖当前头部菜单的配色，避免影响其他页面菜单 */
.menu :deep(.ant-menu-item) {
  color: #334155;
  font-weight: 500;
}

.menu :deep(.ant-menu-item:hover),
.menu :deep(.ant-menu-item-active) {
  color: #22c55e;
}

.menu :deep(.ant-menu-item-selected) {
  color: #16a34a;
}

.menu :deep(.ant-menu-item-selected::after) {
  border-bottom: 2px solid #22c55e;
}

/**
 * 媒体查询：平板设备（屏幕宽度 ≤ 768px）
 * 调整布局以适应中等屏幕
 */
@media (max-width: 768px) {
  .header-content {
    padding: 0 16px;
    gap: 12px;
  }

  .title {
    font-size: 16px;
  }

  .menu {
    margin: 0 8px;
  }

  .logo {
    height: 30px;
  }
}

/**
 * 媒体查询：手机设备（屏幕宽度 ≤ 576px）
 * 进一步优化小屏幕显示
 */
@media (max-width: 576px) {
  .title {
    display: none;
  }

  .menu {
    margin: 0 2px;
  }
}
</style>
