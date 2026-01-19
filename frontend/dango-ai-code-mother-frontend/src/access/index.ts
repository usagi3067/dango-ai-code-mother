/**
 * index.ts - 权限控制入口文件
 * 
 * 在路由跳转前进行权限检查
 * 如果用户没有权限，则跳转到登录页或无权限页面
 */

import router from '@/router'
import { useLoginUserStore } from '@/stores/loginUser'
import ACCESS_ENUM from './accessEnum'
import checkAccess from './checkAccess'

/**
 * 全局前置路由守卫
 * 
 * 在每次路由跳转前执行，用于权限控制
 * 
 * 参数说明：
 * - to: 即将要进入的目标路由对象
 * - from: 当前导航正要离开的路由对象
 * - next: 一定要调用该方法来 resolve 这个钩子
 * 
 * 执行时机：
 * 1. 用户点击菜单跳转
 * 2. 用户在地址栏输入 URL
 * 3. 代码中调用 router.push()
 */
router.beforeEach(async (to, from, next) => {
  /**
   * 1. 获取登录用户 store 实例
   * 
   * 用于访问当前登录用户的信息
   */
  const loginUserStore = useLoginUserStore()
  
  /**
   * 2. 获取当前登录用户信息
   * 
   * loginUser 包含：
   * - id: 用户 ID
   * - userName: 用户名
   * - userRole: 用户角色（user 或 admin）
   * - 等等
   */
  let loginUser = loginUserStore.loginUser
  
  /**
   * 3. 如果用户信息不存在或没有角色，尝试自动登录
   * 
   * 场景：
   * - 用户刷新页面，store 中的数据丢失
   * - 用户首次访问网站
   * 
   * 解决方案：
   * - 调用 fetchLoginUser() 从后端获取用户信息
   * - 如果后端 Session 还在，会返回用户信息
   * - 如果 Session 过期，会返回未登录状态
   */
  if (!loginUser || !loginUser.userRole) {
    /**
     * 加 await 是为了等用户登录成功之后，再执行后续的代码
     * 
     * 这样可以确保后续的权限检查使用的是最新的用户信息
     */
    await loginUserStore.fetchLoginUser()
    
    /**
     * 更新 loginUser 变量
     * 因为 fetchLoginUser() 会更新 store 中的数据
     */
    loginUser = loginUserStore.loginUser
  }
  
  /**
   * 4. 获取目标页面需要的权限
   * 
   * to.meta?.access: 从路由配置的 meta 中获取 access 字段
   * - 如果没有配置 access，默认为 NOT_LOGIN（不需要权限）
   * 
   * 示例：
   * {
   *   path: '/admin/userManage',
   *   meta: {
   *     access: ACCESS_ENUM.ADMIN  // 需要管理员权限
   *   }
   * }
   */
  const needAccess = (to.meta?.access as string) ?? ACCESS_ENUM.NOT_LOGIN
  
  /**
   * 5. 如果页面需要登录权限（不是公开页面）
   * 
   * 需要进行权限检查
   */
  if (needAccess !== ACCESS_ENUM.NOT_LOGIN) {
    /**
     * 5.1 检查用户是否已登录
     * 
     * 判断条件：
     * - loginUser 不存在
     * - 或者 userRole 不存在
     * - 或者 userRole 是 NOT_LOGIN
     * 
     * 如果满足任一条件，说明用户未登录
     */
    if (
      !loginUser ||
      !loginUser.userRole ||
      loginUser.userRole === ACCESS_ENUM.NOT_LOGIN
    ) {
      /**
       * 用户未登录，跳转到登录页
       * 
       * redirect 参数：
       * - 记录用户原本想访问的页面
       * - 登录成功后可以跳转回来
       * 
       * to.fullPath: 完整的目标路径（包括查询参数）
       * 
       * 示例：
       * 用户访问 /admin/userManage
       * 跳转到 /user/login?redirect=/admin/userManage
       * 登录成功后跳转回 /admin/userManage
       */
      next(`/user/login?redirect=${to.fullPath}`)
      return
    }
    
    /**
     * 5.2 用户已登录，检查是否有足够的权限
     * 
     * 调用 checkAccess 函数判断权限
     * - loginUser: 当前登录用户
     * - needAccess: 目标页面需要的权限
     * 
     * 返回 false 表示权限不足
     */
    if (!checkAccess(loginUser, needAccess)) {
      /**
       * 权限不足，跳转到无权限页面
       * 
       * 场景：
       * - 普通用户访问管理员页面
       * - 显示友好的提示信息
       */
      next('/noAuth')
      return
    }
  }
  
  /**
   * 6. 权限检查通过，允许访问
   * 
   * 调用 next() 继续路由跳转
   */
  next()
})

/**
 * 权限控制流程图：
 * 
 * 用户访问页面
 *      ↓
 * 获取用户信息
 *      ↓
 * 用户信息不存在？ → 是 → 尝试自动登录
 *      ↓ 否
 * 页面需要权限？ → 否 → 允许访问
 *      ↓ 是
 * 用户已登录？ → 否 → 跳转到登录页
 *      ↓ 是
 * 权限足够？ → 否 → 跳转到无权限页面
 *      ↓ 是
 * 允许访问
 */
