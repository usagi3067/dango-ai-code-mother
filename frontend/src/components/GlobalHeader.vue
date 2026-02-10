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
        <!-- 
          用户登录状态显示区域
          v-if: 条件渲染指令，只有当用户已登录时才显示
          loginUserStore.loginUser.id: 判断用户是否有 id，有 id 说明已登录
        -->
        <div v-if="loginUserStore.loginUser.id" class="user-login-status">
          <a-dropdown>
            <!-- 
              a-space: Ant Design 的间距组件
              自动为子元素添加合适的间距，避免手动设置 margin
            -->
            <a-space>
              <!-- 用户头像 -->
              <UserAvatar 
                :src="loginUserStore.loginUser.userAvatar" 
                :name="loginUserStore.loginUser.userName"
              />

              <!-- 
                显示用户名
                {{ }}: Vue 的插值语法，用于在模板中显示数据
                ??: 空值合并运算符，如果 userName 为空则显示 '无名'
              -->
              {{ loginUserStore.loginUser.userName ?? '无名' }}
            </a-space>
            <template #overlay>
              <a-menu>
                <a-menu-item @click="doLogout">
                  <LogoutOutlined />
                  退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>

        </div>

        <!-- 
          未登录状态：显示登录按钮
          v-else: 与上面的 v-if 配合使用，当用户未登录时显示
        -->
        <div v-else>
          <!-- 
            Ant Design 按钮组件
            type="primary": 主要按钮样式（蓝色背景）
            href="/user/login": 点击跳转到登录页面
          -->
          <a-button type="primary" href="/user/login">登录</a-button>
        </div>
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
 * 导入 Ant Design Vue 的图标组件
 * LogoutOutlined: 退出登录图标
 */
import { LogoutOutlined } from '@ant-design/icons-vue'

/**
 * 导入 Ant Design Vue 的消息提示组件
 * message: 用于显示成功、错误等提示信息
 */
import { message } from 'ant-design-vue'

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
 * 导入用户注销 API 接口
 * userLogout: 调用后端注销接口
 */
import { userLogout } from '@/api/user/userController'

/**
 * 导入 Pinia store
 * useLoginUserStore: 登录用户状态管理仓库
 * 
 * 为什么在这里导入？
 * - GlobalHeader 需要显示用户登录状态
 * - 通过 store 可以访问全局的用户信息
 * - 当用户信息变化时，组件会自动更新
 */
import { useLoginUserStore } from '@/stores/loginUser.ts'

/**
 * 导入用户头像组件
 */
import UserAvatar from '@/components/UserAvatar.vue'

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

/**
 * 用户注销函数
 * 
 * 功能：退出登录，清除用户状态，跳转到登录页
 * 
 * async: 异步函数
 * - 因为需要等待网络请求完成
 * - 可以使用 await 等待 Promise
 * 
 * 执行流程：
 * 1. 调用后端注销接口
 * 2. 等待响应结果
 * 3. 判断是否成功
 * 4. 成功：清除用户状态，跳转到登录页
 * 5. 失败：显示错误提示
 */
const doLogout = async () => {
  /**
   * 调用注销 API
   * 
   * userLogout(): 发送注销请求
   * - 后端会清除 Session，销毁登录状态
   * - 清除服务器端的用户会话信息
   * 
   * await: 等待请求完成
   * - 暂停函数执行，等待 Promise 完成
   * - 请求成功后继续执行后面的代码
   * 
   * res: 响应结果对象
   * - res.data.code: 响应状态码（0 表示成功）
   * - res.data.message: 响应消息
   */
  const res = await userLogout()
  
  /**
   * 判断注销是否成功
   * 
   * res.data.code === 0: 后端约定的成功状态码
   */
  if (res.data.code === 0) {
    /**
     * 注销成功的处理流程
     */
    
    /**
     * 1. 清除前端的用户状态
     * 
     * setLoginUser: 设置用户信息为"未登录"状态
     * - 清除用户 ID、用户名、头像等信息
     * - 将用户名设置为 '未登录'
     * 
     * 为什么要清除前端状态？
     * - 后端已经清除了 Session，但前端还保留着用户信息
     * - 需要同步清除前端状态，确保数据一致
     * - 否则页面上还会显示用户信息，造成混乱
     */
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    
    /**
     * 2. 显示成功提示
     * 
     * message.success: Ant Design 的成功提示
     * - 会在页面顶部显示绿色的成功消息
     * - 几秒后自动消失
     */
    message.success('退出登录成功')
    
    /**
     * 3. 跳转到登录页面
     * 
     * router.push: 编程式导航
     * - path: '/user/login': 跳转到登录页面路径
     * 
     * 为什么要跳转到登录页？
     * - 用户已经退出登录，不应该停留在需要登录的页面
     * - 引导用户重新登录
     * - 防止用户误操作
     * 
     * 注意：这里没有使用 replace: true
     * - 允许用户点击后退按钮回到之前的页面
     * - 如果之前的页面需要登录，路由守卫会再次跳转到登录页
     */
    await router.push('/user/login')
  } else {
    /**
     * 注销失败的处理
     * 
     * message.error: Ant Design 的错误提示
     * - 会在页面顶部显示红色的错误消息
     * - 显示后端返回的具体错误信息
     * 
     * 错误信息格式：'退出登录失败，' + res.data.message
     * - 例如：'退出登录失败，会话已过期'
     */
    message.error('退出登录失败，' + res.data.message)
  }
}
</script>

<style scoped>
/**
 * scoped 样式：只在当前组件内生效
 * 避免样式污染其他组件
 */

/* 头部容器样式 */
.header {
  background: #fff;
  /* 白色背景 */
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  /* 阴影效果：水平偏移 0，垂直偏移 2px，模糊 8px */
  padding: 0;
  /* 去除默认内边距 */
  position: sticky;
  /* 粘性定位：滚动时固定在顶部 */
  top: 0;
  /* 固定在顶部位置 */
  z-index: 999;
  /* 层级：确保在其他元素之上 */
}

/* 头部内容容器：控制布局和最大宽度 */
.header-content {
  max-width: 1200px;
  /* 最大宽度 1200px */
  margin: 0 auto;
  /* 水平居中 */
  display: flex;
  /* 使用 Flexbox 布局 */
  align-items: center;
  /* 垂直居中对齐 */
  justify-content: space-between;
  /* 两端对齐，中间自动分配空间 */
  padding: 0 24px;
  /* 左右内边距 24px */
  height: 64px;
  /* 固定高度 64px */
}

/* Logo 区域样式 */
.logo-section {
  display: flex;
  /* Flexbox 布局 */
  align-items: center;
  /* 垂直居中 */
  gap: 12px;
  /* 子元素之间的间距 12px */
  flex-shrink: 0;
  /* 不允许缩小，保持固定大小 */
}

/* Logo 图片样式 */
.logo {
  height: 40px;
  /* 固定高度 40px */
  width: auto;
  /* 宽度自动，保持图片比例 */
}

/* 网站标题样式 */
.title {
  font-size: 18px;
  /* 字体大小 18px */
  font-weight: 600;
  /* 字体粗细：半粗体 */
  color: #1890ff;
  /* Ant Design 主题蓝色 */
  white-space: nowrap;
  /* 不换行，保持在一行显示 */
}

/* 菜单样式 */
.menu {
  flex: 1;
  /* 占据剩余空间，自动伸缩 */
  border-bottom: none;
  /* 去除底部边框 */
  line-height: 64px;
  /* 行高与头部高度一致，实现垂直居中 */
  margin: 0 24px;
  /* 左右外边距 24px */
}

/* 用户操作区域样式 */
.user-section {
  flex-shrink: 0;
  /* 不允许缩小，保持固定大小 */
}

/* 用户登录状态容器样式 */
.user-login-status {
  display: flex;
  /* Flexbox 布局 */
  align-items: center;
  /* 垂直居中对齐 */
  cursor: pointer;
  /* 鼠标悬停时显示手型光标 */
}

/**
 * 媒体查询：平板设备（屏幕宽度 ≤ 768px）
 * 调整布局以适应中等屏幕
 */
@media (max-width: 768px) {
  .header-content {
    padding: 0 16px;
    /* 减小左右内边距 */
  }

  .title {
    font-size: 16px;
    /* 减小标题字体 */
  }

  .menu {
    margin: 0 12px;
    /* 减小菜单左右间距 */
  }

  .logo {
    height: 32px;
    /* 减小 Logo 高度 */
  }
}

/**
 * 媒体查询：手机设备（屏幕宽度 ≤ 576px）
 * 进一步优化小屏幕显示
 */
@media (max-width: 576px) {
  .title {
    display: none;
    /* 隐藏标题，节省空间 */
  }

  .menu {
    margin: 0 8px;
    /* 进一步减小菜单间距 */
  }
}
</style>
