/**
 * request.ts - 全局 Axios 请求配置
 *
 * 1. 统一配置请求基础地址、超时时间
 * 2. 请求拦截器：自动携带 Sa-Token
 * 3. 响应拦截器：统一处理未登录和错误
 */

import axios from 'axios'
import { message } from 'ant-design-vue'
import { API_BASE_URL } from '@/config/env'

const myAxios = axios.create({
  baseURL: API_BASE_URL,
  timeout: 60000,
})

// 请求拦截器：自动携带 Sa-Token
myAxios.interceptors.request.use(
  function (config) {
    const token = localStorage.getItem('satoken')
    if (token) {
      config.headers['Authorization'] = token
    }
    return config
  },
  function (error) {
    return Promise.reject(error)
  }
)

// 响应拦截器：统一处理未登录和错误
myAxios.interceptors.response.use(
  function (response) {
    const { data } = response

    // 未登录处理：清除 Token 并跳转登录页
    if (data.code === 40100) {
      localStorage.removeItem('satoken')
      if (
        !response.request.responseURL.includes('user/get/login') &&
        !window.location.pathname.includes('/user/login')
      ) {
        message.warning('请先登录')
        window.location.href = `/user/login?redirect=${window.location.href}`
      }
    }

    return response
  },
  function (error) {
    if (error.response) {
      const status = error.response.status
      switch (status) {
        case 400:
          message.error('请求参数错误')
          break
        case 401:
          message.error('未授权，请重新登录')
          break
        case 403:
          message.error('拒绝访问')
          break
        case 404:
          message.error('请求的资源不存在')
          break
        case 500:
          message.error('服务器内部错误')
          break
        case 502:
          message.error('网关错误')
          break
        case 503:
          message.error('服务不可用')
          break
        case 504:
          message.error('网关超时')
          break
        default:
          message.error(`请求失败：${status}`)
      }
    } else if (error.request) {
      message.error('网络错误，请检查网络连接')
    } else {
      message.error('请求失败，请稍后重试')
    }

    return Promise.reject(error)
  }
)

export default myAxios
