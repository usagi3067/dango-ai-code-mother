/**
 * checkAccess.ts - 权限检查函数
 * 
 * 用于判断当前登录用户是否具有访问某个页面的权限
 */

import ACCESS_ENUM from './accessEnum'

/**
 * 检查权限
 * 
 * 功能：判断当前登录用户是否具有某个权限
 * 
 * @param loginUser - 当前登录用户对象
 *   - 包含 userRole 字段，表示用户角色
 *   - 如果为 null/undefined，表示未登录
 * 
 * @param needAccess - 需要的权限级别
 *   - 默认为 NOT_LOGIN，表示不需要任何权限
 *   - 可以是 USER 或 ADMIN
 * 
 * @returns boolean - 是否有权限
 *   - true: 有权限，可以访问
 *   - false: 无权限，不能访问
 * 
 * 使用示例：
 * ```typescript
 * const hasAccess = checkAccess(loginUser, ACCESS_ENUM.ADMIN)
 * if (hasAccess) {
 *   // 用户有管理员权限
 * } else {
 *   // 用户没有管理员权限
 * }
 * ```
 */
const checkAccess = (loginUser: any, needAccess = ACCESS_ENUM.NOT_LOGIN): boolean => {
  /**
   * 获取当前登录用户的权限级别
   * 
   * loginUser?.userRole: 使用可选链操作符，安全地访问 userRole
   * - 如果 loginUser 为 null/undefined，返回 undefined
   * - 如果 loginUser 存在，返回 userRole 的值
   * 
   * ?? ACCESS_ENUM.NOT_LOGIN: 空值合并运算符
   * - 如果 userRole 为 null/undefined，使用 NOT_LOGIN
   * - 表示未登录状态
   */
  const loginUserAccess = loginUser?.userRole ?? ACCESS_ENUM.NOT_LOGIN
  
  /**
   * 情况 1：页面不需要任何权限（公开页面）
   * 
   * 例如：首页、关于页面、登录页、注册页
   * 任何人都可以访问，直接返回 true
   */
  if (needAccess === ACCESS_ENUM.NOT_LOGIN) {
    return true
  }
  
  /**
   * 情况 2：页面需要用户登录
   * 
   * 例如：个人中心、我的应用
   * 只要登录了就可以访问，不管是普通用户还是管理员
   */
  if (needAccess === ACCESS_ENUM.USER) {
    /**
     * 检查用户是否已登录
     * 
     * 如果用户未登录（loginUserAccess === NOT_LOGIN）
     * 返回 false，表示无权限
     */
    if (loginUserAccess === ACCESS_ENUM.NOT_LOGIN) {
      return false
    }
    
    /**
     * 用户已登录（可能是普通用户或管理员）
     * 返回 true，表示有权限
     */
    return true
  }
  
  /**
   * 情况 3：页面需要管理员权限
   * 
   * 例如：用户管理、系统设置
   * 只有管理员才能访问
   */
  if (needAccess === ACCESS_ENUM.ADMIN) {
    /**
     * 检查用户是否是管理员
     * 
     * 如果不是管理员（loginUserAccess !== ADMIN）
     * 返回 false，表示无权限
     * 
     * 注意：这里包括了两种情况
     * 1. 用户未登录（NOT_LOGIN）
     * 2. 用户已登录但是普通用户（USER）
     */
    if (loginUserAccess !== ACCESS_ENUM.ADMIN) {
      return false
    }
    
    /**
     * 用户是管理员
     * 返回 true，表示有权限
     */
    return true
  }
  
  /**
   * 默认情况：返回 true
   * 
   * 理论上不会走到这里
   * 因为 needAccess 只能是 NOT_LOGIN、USER 或 ADMIN
   */
  return true
}

/**
 * 导出权限检查函数
 * 
 * 使用方式：
 * import checkAccess from '@/access/checkAccess'
 */
export default checkAccess
