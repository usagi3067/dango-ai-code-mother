/**
 * accessEnum.ts - 权限枚举定义
 * 
 * 定义系统中的所有权限级别
 * 用于路由守卫、菜单显示等权限控制场景
 */

/**
 * 权限枚举对象
 * 
 * 三个权限级别：
 * 1. NOT_LOGIN: 未登录（游客）- 可以访问公开页面
 * 2. USER: 普通用户 - 需要登录才能访问
 * 3. ADMIN: 管理员 - 需要管理员权限才能访问
 */
const ACCESS_ENUM = {
  /**
   * 未登录状态
   * 
   * 使用场景：
   * - 首页、关于页面等公开页面
   * - 登录页、注册页
   * - 不需要任何权限即可访问
   */
  NOT_LOGIN: 'notLogin',
  
  /**
   * 普通用户权限
   * 
   * 使用场景：
   * - 需要登录才能访问的页面
   * - 个人中心、我的应用等
   * - 只要登录了就可以访问
   */
  USER: 'user',
  
  /**
   * 管理员权限
   * 
   * 使用场景：
   * - 用户管理、系统设置等管理功能
   * - 只有管理员才能访问
   * - 普通用户无法访问
   */
  ADMIN: 'admin',
}

/**
 * 导出权限枚举
 * 
 * 使用方式：
 * import ACCESS_ENUM from '@/access/accessEnum'
 * 
 * 示例：
 * if (user.userRole === ACCESS_ENUM.ADMIN) {
 *   // 用户是管理员
 * }
 */
export default ACCESS_ENUM
