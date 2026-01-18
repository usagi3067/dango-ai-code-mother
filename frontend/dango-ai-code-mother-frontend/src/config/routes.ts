/**
 * routes.ts - 路由和菜单统一配置文件
 * 
 * 这个文件统一管理路由和菜单的配置，避免重复定义
 * 优点：
 * 1. 单一数据源：路由和菜单信息只定义一次
 * 2. 易于维护：新增页面只需在这里添加一条配置
 * 3. 类型安全：使用 TypeScript 确保配置正确
 * 4. 自动生成：路由配置和菜单配置自动生成，减少人工错误
 */

import type { RouteRecordRaw } from 'vue-router'

/**
 * 路由配置项的类型定义
 * 扩展了 Vue Router 的路由配置，添加了菜单相关的字段
 */
export interface RouteConfig {
  path: string              // 路由路径，如 '/' 或 '/about'
  name: string              // 路由名称，如 'home' 或 'about'
  component: () => Promise<any>  // 组件（懒加载）
  meta?: {
    title?: string          // 页面标题，用于设置浏览器标题栏
    showInMenu?: boolean    // 是否在菜单中显示（默认 true）
    menuLabel?: string      // 菜单显示的文本（如果不设置，使用 title）
    icon?: string           // 菜单图标（可选，暂未使用）
    requiresAuth?: boolean  // 是否需要登录（可选，用于权限控制）
  }
}

/**
 * 路由配置数组
 * 这是整个应用的路由和菜单的唯一数据源
 * 
 * 使用说明：
 * 1. 新增页面：在这个数组中添加一条配置即可
 * 2. 隐藏菜单：设置 meta.showInMenu = false
 * 3. 自定义菜单文本：设置 meta.menuLabel
 */
export const routeConfigs: RouteConfig[] = [
  {
    path: '/',
    name: 'home',
    component: () => import('@/pages/HomePage.vue'),
    meta: {
      title: '首页',
      menuLabel: '主页',  // 菜单显示"主页"，页面标题显示"首页"
      showInMenu: true
    }
  },
  {
    path: '/about',
    name: 'about',
    component: () => import('@/pages/AboutPage.vue'),
    meta: {
      title: '关于我们',
      menuLabel: '关于',
      showInMenu: true
    }
  }
  
  /**
   * 新增页面示例：
   * 
   * {
   *   path: '/user',
   *   name: 'user',
   *   component: () => import('@/pages/UserPage.vue'),
   *   meta: {
   *     title: '用户中心',
   *     menuLabel: '用户',
   *     showInMenu: true,
   *     requiresAuth: true  // 需要登录才能访问
   *   }
   * }
   */
]

/**
 * 将配置转换为 Vue Router 的路由配置格式
 * 
 * 这个函数会被 router/index.ts 使用
 * 自动生成符合 Vue Router 要求的路由配置
 */
export function generateRoutes(): RouteRecordRaw[] {
  return routeConfigs.map(config => ({
    path: config.path,
    name: config.name,
    component: config.component,
    meta: config.meta || {}
  }))
}

/**
 * 获取需要在菜单中显示的路由配置
 * 
 * 这个函数会被 GlobalHeader.vue 使用
 * 过滤出需要显示在菜单中的路由
 */
export function getMenuRoutes(): RouteConfig[] {
  return routeConfigs.filter(route => {
    // 如果没有设置 showInMenu，默认显示
    return route.meta?.showInMenu !== false
  })
}

/**
 * 根据路由路径查找对应的路由配置
 * 
 * 这个函数用于菜单高亮逻辑
 * 根据当前路径快速找到对应的路由名称
 * 
 * @param path 路由路径，如 '/about'
 * @returns 路由配置对象，如果找不到返回 undefined
 */
export function findRouteByPath(path: string): RouteConfig | undefined {
  return routeConfigs.find(route => route.path === path)
}

/**
 * 根据路由名称查找对应的路由配置
 * 
 * @param name 路由名称，如 'about'
 * @returns 路由配置对象，如果找不到返回 undefined
 */
export function findRouteByName(name: string): RouteConfig | undefined {
  return routeConfigs.find(route => route.name === name)
}
