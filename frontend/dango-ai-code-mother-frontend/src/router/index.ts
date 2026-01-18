/**
 * router/index.ts - Vue Router 路由配置文件
 * 
 * 优化后的版本：
 * - 路由配置从统一的配置文件中生成
 * - 避免重复定义路由和菜单
 * - 易于维护和扩展
 */

/**
 * 从 Vue Router 库导入必要的函数
 * 
 * createRouter: 创建路由实例的工厂函数
 * createWebHistory: 创建 HTML5 History 模式的路由历史记录
 */
import { createRouter, createWebHistory } from 'vue-router'

/**
 * 导入统一的路由配置
 * generateRoutes: 从配置文件生成符合 Vue Router 格式的路由数组
 * 
 * 优点：
 * 1. 单一数据源：路由和菜单配置在一个地方管理
 * 2. 自动生成：不需要手动编写重复的路由配置
 * 3. 类型安全：TypeScript 确保配置正确
 */
import { generateRoutes } from '@/config/routes'

/**
 * 创建路由实例
 * 这个实例会在 main.ts 中通过 app.use(router) 注册到 Vue 应用
 */
const router = createRouter({
  /**
   * history 模式配置
   * 
   * createWebHistory: 使用 HTML5 History API 实现路由
   * 特点：
   * 1. URL 看起来像正常的路径：http://localhost:5173/about
   * 2. 没有 # 号（对比 Hash 模式：http://localhost:5173/#/about）
   * 3. 需要服务器配置支持（所有路径都返回 index.html）
   * 
   * import.meta.env.BASE_URL: Vite 提供的环境变量，表示应用的基础路径
   * 通常是 '/'，如果部署在子目录下可能是 '/my-app/'
   */
  history: createWebHistory(import.meta.env.BASE_URL),

  /**
   * routes: 路由规则数组
   * 
   * 从统一配置文件自动生成，包含：
   * - 路径映射
   * - 组件懒加载
   * - 元信息（标题、权限等）
   * 
   * 新增路由的方法：
   * 只需在 src/config/routes.ts 中添加配置即可，无需修改此文件
   */
  routes: generateRoutes()
})

/**
 * 全局前置路由守卫（可选）
 * 在路由跳转前执行，可用于：
 * - 权限验证
 * - 页面标题设置
 * - 加载状态控制
 * - 数据预加载
 */
router.beforeEach((to, from, next) => {
  /**
   * 动态设置页面标题
   * 根据路由的 meta.title 设置浏览器标题栏
   */
  if (to.meta.title) {
    document.title = `${to.meta.title} - AI 代码母体`
  } else {
    document.title = 'AI 代码母体'
  }

  /**
   * 权限验证示例（可选）
   * 检查路由是否需要登录，如果需要但用户未登录，则重定向到登录页
   */
  // if (to.meta.requiresAuth && !isLoggedIn()) {
  //   next('/login')  // 重定向到登录页
  // } else {
  //   next()  // 继续导航
  // }

  // 继续导航
  next()
})

/**
 * 导出路由实例
 * 这个实例会在 main.ts 中被导入并注册到 Vue 应用
 */
export default router
