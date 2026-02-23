/**
 * 环境变量配置
 * 
 * 统一管理应用中使用的域名和地址配置
 * 通过环境变量实现不同环境的配置切换
 */

import { CodeGenTypeEnum } from "./codeGenType"

// 应用部署域名（nginx 服务，80 端口）
// import.meta.env 是 Vite 提供的环境变量访问接口
// VITE_DEPLOY_DOMAIN 指向 .env 文件中定义的环境变量（如 .env.development、.env.production 等）
// 如果未设置环境变量，则使用默认值 'http://localhost'
export const DEPLOY_DOMAIN = import.meta.env.VITE_DEPLOY_DOMAIN || 'http://localhost'

// API 基础地址（后端服务）
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8123/api'

// 静态资源地址（生成的应用预览）
export const STATIC_BASE_URL = `${API_BASE_URL}/static`

/**
 * 获取部署应用的完整 URL
 * 
 * @param deployKey - 部署标识
 * @returns 部署应用的访问地址
 */
export const getDeployUrl = (deployKey: string) => {
  return `${DEPLOY_DOMAIN}/d/${deployKey}/`
}

/**
 * 获取静态资源预览 URL
 * 
 * @param codeGenType - 代码生成类型
 * @param appId - 应用 ID
 * @returns 静态资源预览地址
 */
// 获取静态资源预览URL
export const getStaticPreviewUrl = (codeGenType: string, appId: string) => {
  const baseUrl = `${STATIC_BASE_URL}/${codeGenType}_${appId}/`
  // 如果是 Vue 项目，浏览地址需要添加 dist 后缀
  if (codeGenType === CodeGenTypeEnum.VUE_PROJECT) {
    return `${baseUrl}dist/index.html`
  }
  return baseUrl
}
