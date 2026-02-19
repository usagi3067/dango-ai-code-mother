/**
 * routes.ts - 路由和菜单统一配置文件
 */

import type { RouteRecordRaw } from 'vue-router'
import ACCESS_ENUM from '@/access/accessEnum'

export interface RouteConfig {
  path: string
  name: string
  component: () => Promise<any>
  meta?: {
    title?: string
    showInMenu?: boolean
    menuLabel?: string
    icon?: string
    requiresAuth?: boolean
    access?: string
    hideLayout?: boolean  // 是否隐藏布局（用于全屏页面）
  }
}

export const routeConfigs: RouteConfig[] = [
  {
    path: '/',
    name: 'home',
    component: () => import('@/pages/HomePage.vue'),
    meta: {
      title: '首页',
      menuLabel: '主页',
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
  },
  // 全部案例页（独立布局）
  {
    path: '/cases',
    name: 'cases',
    component: () => import('@/pages/CasesPage.vue'),
    meta: {
      title: '全部案例',
      showInMenu: false,
      hideLayout: true
    }
  },
  // 应用对话页（全屏布局）
  {
    path: '/app/chat/:id',
    name: 'appChat',
    component: () => import('@/pages/AppChatPage.vue'),
    meta: {
      title: '应用生成',
      showInMenu: false,
      access: ACCESS_ENUM.USER,
      hideLayout: true
    }
  },
  // 应用编辑页
  {
    path: '/app/edit/:id',
    name: 'appEdit',
    component: () => import('@/pages/AppEditPage.vue'),
    meta: {
      title: '编辑应用',
      showInMenu: false,
      access: ACCESS_ENUM.USER
    }
  },
  // 用户登录页面
  {
    path: '/user/login',
    name: 'userLogin',
    component: () => import('@/pages/UserLoginPage.vue'),
    meta: {
      title: '用户登录',
      showInMenu: false,
      access: ACCESS_ENUM.NOT_LOGIN,
      hideLayout: true
    }
  },
  // 用户注册页面
  {
    path: '/user/register',
    name: 'userRegister',
    component: () => import('@/pages/UserRegisterPage.vue'),
    meta: {
      title: '用户注册',
      showInMenu: false,
      access: ACCESS_ENUM.NOT_LOGIN,
      hideLayout: true
    }
  },
  // 个人设置页面
  {
    path: '/user/settings',
    name: 'userSettings',
    component: () => import('@/pages/UserSettingsPage.vue'),
    meta: {
      title: '个人设置',
      showInMenu: false,
      access: ACCESS_ENUM.USER
    }
  },
  // 无权限页面
  {
    path: '/noAuth',
    name: 'noAuth',
    component: () => import('@/pages/NoAuthPage.vue'),
    meta: {
      title: '无权限',
      showInMenu: false,
      access: ACCESS_ENUM.NOT_LOGIN
    }
  },
  // 用户管理页面（管理员）
  {
    path: '/admin/userManage',
    name: 'userManage',
    component: () => import('@/pages/admin/UserManagePage.vue'),
    meta: {
      title: '用户管理',
      menuLabel: '用户管理',
      showInMenu: true,
      access: ACCESS_ENUM.ADMIN
    }
  },
  // 应用管理页面（管理员）
  {
    path: '/admin/appManage',
    name: 'appManage',
    component: () => import('@/pages/admin/AppManagePage.vue'),
    meta: {
      title: '应用管理',
      menuLabel: '应用管理',
      showInMenu: true,
      access: ACCESS_ENUM.ADMIN
    }
  },
  // 对话管理页面（管理员）
  {
    path: '/admin/chatHistoryManage',
    name: 'chatHistoryManage',
    component: () => import('@/pages/admin/ChatHistoryManagePage.vue'),
    meta: {
      title: '对话管理',
      menuLabel: '对话管理',
      showInMenu: true,
      access: ACCESS_ENUM.ADMIN
    }
  }
]

export function generateRoutes(): RouteRecordRaw[] {
  return routeConfigs.map(config => ({
    path: config.path,
    name: config.name,
    component: config.component,
    meta: config.meta || {}
  }))
}

export function getMenuRoutes(loginUser?: any): RouteConfig[] {
  const checkAccess = (needAccess: string) => {
    if (!loginUser) {
      return needAccess === ACCESS_ENUM.NOT_LOGIN
    }
    const loginUserAccess = loginUser?.userRole ?? ACCESS_ENUM.NOT_LOGIN
    if (needAccess === ACCESS_ENUM.NOT_LOGIN) {
      return true
    }
    if (needAccess === ACCESS_ENUM.USER) {
      return loginUserAccess !== ACCESS_ENUM.NOT_LOGIN
    }
    if (needAccess === ACCESS_ENUM.ADMIN) {
      return loginUserAccess === ACCESS_ENUM.ADMIN
    }
    return true
  }

  return routeConfigs.filter(route => {
    if (route.meta?.showInMenu === false) {
      return false
    }
    const needAccess = route.meta?.access ?? ACCESS_ENUM.NOT_LOGIN
    return checkAccess(needAccess)
  })
}

export function findRouteByPath(path: string): RouteConfig | undefined {
  return routeConfigs.find(route => route.path === path)
}

export function findRouteByName(name: string): RouteConfig | undefined {
  return routeConfigs.find(route => route.name === name)
}
