/**
 * request.ts - 全局 Axios 请求配置
 * 
 * 功能：
 * 1. 统一配置请求基础地址、超时时间等
 * 2. 全局请求拦截器：在发送请求前统一处理
 * 3. 全局响应拦截器：在接收响应后统一处理
 * 4. 统一错误处理和用户提示
 * 
 * 参考：Axios 官方文档 https://axios-http.com/
 */

/**
 * 导入 axios 库
 * axios: 基于 Promise 的 HTTP 客户端，用于浏览器和 node.js
 */
import axios from 'axios'

/**
 * 导入 Ant Design Vue 的消息提示组件
 * message: 用于显示全局提示信息（成功、警告、错误等）
 */
import { message } from 'ant-design-vue'

/**
 * 导入环境变量配置
 */
import { API_BASE_URL } from '@/config/env'

/**
 * 创建 Axios 实例
 * 
 * 为什么要创建实例而不是直接使用 axios？
 * 1. 可以为不同的 API 创建不同的实例（如：用户 API、管理 API）
 * 2. 每个实例可以有独立的配置（基础地址、超时时间等）
 * 3. 不会影响全局的 axios 默认配置
 * 4. 便于维护和扩展
 */
const myAxios = axios.create({
  /**
   * baseURL: 请求的基础地址
   * 
   * 所有请求的 URL 都会自动拼接这个基础地址
   * 例如：
   * - 请求 /user/login → 实际请求 http://localhost:8123/api/user/login
   * - 请求 /post/list → 实际请求 http://localhost:8123/api/post/list
   * 
   * 优点：
   * - 统一管理 API 地址，切换环境时只需修改这一处
   * - 代码中只需写相对路径，更简洁
   * 
   * 环境配置建议：
   * - 开发环境：http://localhost:8123/api
   * - 测试环境：https://test-api.example.com/api
   * - 生产环境：https://api.example.com/api
   */
  baseURL: API_BASE_URL,

  /**
   * timeout: 请求超时时间（毫秒）
   * 
   * 如果请求超过这个时间还没有响应，会自动取消请求并抛出错误
   * 60000 毫秒 = 60 秒 = 1 分钟
   * 
   * 设置原因：
   * - 防止请求一直等待，影响用户体验
   * - 及时发现网络问题或服务器问题
   * 
   * 建议值：
   * - 普通请求：10000 (10秒)
   * - 文件上传：60000 (60秒)
   * - 大数据查询：30000 (30秒)
   */
  timeout: 60000,

  /**
   * withCredentials: 是否携带 Cookie
   * 
   * 设置为 true 时：
   * - 跨域请求会自动携带 Cookie
   * - 可以实现基于 Cookie 的身份认证（Session）
   * - 服务器需要设置 CORS 响应头：Access-Control-Allow-Credentials: true
   * 
   * 应用场景：
   * - 用户登录状态保持（Session）
   * - 跨域请求需要携带认证信息
   * 
   * 注意事项：
   * - 服务器必须配置 CORS 允许携带凭证
   * - 不能使用通配符 * 作为 Access-Control-Allow-Origin
   * - 必须指定具体的域名
   * 
   * 示例：
   * 用户登录后，服务器会设置 Cookie（如 JSESSIONID）
   * 后续请求会自动携带这个 Cookie，服务器就能识别用户身份
   */
  withCredentials: true,
})

/**
 * 全局请求拦截器
 * 
 * 作用：在请求发送到服务器之前，统一处理请求配置
 * 
 * 应用场景：
 * 1. 添加认证 Token 到请求头
 * 2. 统一添加时间戳防止缓存
 * 3. 显示全局 Loading 状态
 * 4. 请求参数加密
 * 5. 请求日志记录
 * 
 * 参数说明：
 * - config: 请求配置对象，包含 url、method、headers、data 等
 * - error: 请求配置错误对象
 */
myAxios.interceptors.request.use(
  function (config) {
    /**
     * 请求成功的处理函数
     * 
     * 这里可以对请求配置进行修改
     * 例如：添加 Token、修改请求头等
     */

    /**
     * 示例：添加认证 Token 到请求头
     * 
     * 如果使用 JWT 认证，可以这样添加：
     */
    // const token = localStorage.getItem('token')
    // if (token) {
    //   config.headers.Authorization = `Bearer ${token}`
    // }

    /**
     * 示例：添加时间戳防止 GET 请求缓存
     */
    // if (config.method === 'get') {
    //   config.params = {
    //     ...config.params,
    //     _t: Date.now()
    //   }
    // }

    /**
     * 示例：显示全局 Loading
     */
    // showLoading()

    /**
     * 返回修改后的配置
     * 必须返回 config，否则请求不会发送
     */
    return config
  },
  function (error) {
    /**
     * 请求配置错误的处理函数
     * 
     * 这种情况很少见，通常是代码错误导致的
     * 例如：config 对象格式不正确
     */

    /**
     * 返回一个被拒绝的 Promise
     * 这样调用方可以通过 catch 捕获错误
     */
    return Promise.reject(error)
  }
)

/**
 * 全局响应拦截器
 * 
 * 作用：在接收到服务器响应后，统一处理响应数据
 * 
 * 应用场景：
 * 1. 统一处理业务错误码（如未登录、权限不足）
 * 2. 统一提取响应数据（response.data）
 * 3. 统一错误提示
 * 4. 隐藏全局 Loading 状态
 * 5. 响应数据解密
 * 6. 响应日志记录
 * 
 * 参数说明：
 * - response: 响应对象，包含 data、status、headers 等
 * - error: 响应错误对象
 */
myAxios.interceptors.response.use(
  function (response) {
    /**
     * 响应成功的处理函数
     * 
     * HTTP 状态码在 2xx 范围内时会触发这个函数
     * 例如：200、201、204 等
     */

    /**
     * 从响应对象中解构出 data
     * 
     * response 对象结构：
     * {
     *   data: { code: 0, data: {...}, message: 'success' },  // 服务器返回的数据
     *   status: 200,                                          // HTTP 状态码
     *   statusText: 'OK',                                     // HTTP 状态文本
     *   headers: {...},                                       // 响应头
     *   config: {...},                                        // 请求配置
     *   request: {...}                                        // 原始请求对象
     * }
     */
    const { data } = response

    /**
     * 处理未登录的情况
     * 
     * 业务逻辑：
     * 1. 检查响应数据中的业务错误码
     * 2. 如果是 40100（未登录），则跳转到登录页
     * 3. 避免在登录页或获取用户信息接口时重复跳转
     */
    if (data.code === 40100) {
      /**
       * 判断是否需要跳转到登录页
       * 
       * 不跳转的情况：
       * 1. 当前请求是获取用户信息的接口（避免死循环）
       * 2. 当前页面已经是登录页（避免重复跳转）
       * 
       * response.request.responseURL: 实际请求的完整 URL
       * window.location.pathname: 当前页面的路径
       */
      if (
        !response.request.responseURL.includes('user/get/login') &&
        !window.location.pathname.includes('/user/login')
      ) {
        /**
         * 显示警告提示
         * message.warning: Ant Design 的警告提示
         */
        message.warning('请先登录')

        /**
         * 跳转到登录页，并携带重定向参数
         * 
         * redirect 参数：登录成功后跳转回原页面
         * window.location.href: 当前页面的完整 URL
         * 
         * 例如：
         * 用户在 /user/profile 页面未登录
         * 跳转到 /user/login?redirect=http://localhost:5173/user/profile
         * 登录成功后，可以跳转回 /user/profile
         */
        window.location.href = `/user/login?redirect=${window.location.href}`
      }
    }

    /**
     * 示例：统一处理其他业务错误码
     */
    // if (data.code === 40300) {
    //   message.error('权限不足')
    //   return Promise.reject(new Error('权限不足'))
    // }

    /**
     * 示例：隐藏全局 Loading
     */
    // hideLoading()

    /**
     * 返回响应对象
     * 
     * 注意：这里返回的是完整的 response 对象，而不是 response.data
     * 这样调用方可以访问 status、headers 等信息
     * 
     * 如果只需要返回数据，可以改为：
     * return response.data
     * 
     * 但这样会丢失 HTTP 状态码等信息，不推荐
     */
    return response
  },
  function (error) {
    /**
     * 响应错误的处理函数
     * 
     * HTTP 状态码不在 2xx 范围内时会触发这个函数
     * 例如：400、401、403、404、500 等
     * 
     * 或者请求超时、网络错误等也会触发
     */

    /**
     * 统一错误提示
     * 
     * 根据不同的错误类型显示不同的提示信息
     */
    if (error.response) {
      /**
       * 服务器返回了错误响应（状态码不是 2xx）
       * 
       * error.response 包含：
       * - status: HTTP 状态码
       * - data: 错误信息
       * - headers: 响应头
       */
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
      /**
       * 请求已发送，但没有收到响应
       * 
       * 可能的原因：
       * - 网络断开
       * - 服务器无响应
       * - 请求超时
       */
      message.error('网络错误，请检查网络连接')
    } else {
      /**
       * 请求配置错误或其他错误
       * 
       * 例如：
       * - 请求被取消
       * - 代码错误
       */
      message.error('请求失败，请稍后重试')
    }

    /**
     * 示例：隐藏全局 Loading
     */
    // hideLoading()

    /**
     * 返回一个被拒绝的 Promise
     * 这样调用方可以通过 catch 捕获错误
     */
    return Promise.reject(error)
  }
)

/**
 * 导出配置好的 axios 实例
 * 
 * 使用方式：
 * import myAxios from '@/request'
 * 
 * // GET 请求
 * myAxios.get('/user/list')
 * 
 * // POST 请求
 * myAxios.post('/user/login', { username: 'admin', password: '123456' })
 * 
 * // 带参数的 GET 请求
 * myAxios.get('/user/detail', { params: { id: 1 } })
 * 
 * // 自定义配置
 * myAxios.request({
 *   url: '/user/upload',
 *   method: 'post',
 *   data: formData,
 *   headers: { 'Content-Type': 'multipart/form-data' }
 * })
 */
export default myAxios
