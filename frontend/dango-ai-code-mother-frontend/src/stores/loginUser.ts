// 从 pinia 导入 defineStore 函数，用于创建一个状态管理仓库
import { defineStore } from 'pinia'
// 从 vue 导入 ref 函数，用于创建响应式数据
import { ref } from 'vue'
// 导入后端 API 接口，用于获取登录用户信息
import { getLoginUser } from '@/api/userController.ts'

/**
 * 定义登录用户的状态管理仓库
 * 
 * defineStore 的两个参数：
 * 1. 'loginUser' - 仓库的唯一标识符（ID），用于在应用中区分不同的 store
 * 2. () => {...} - setup 函数，类似于 Vue 3 组合式 API 的写法
 * 
 * 这种写法叫做 "Setup Store"，更接近 Vue 3 的 Composition API 风格
 */
export const useLoginUserStore = defineStore('loginUser', () => {
  
  // ==================== 状态定义 ====================
  /**
   * loginUser - 存储当前登录用户的信息
   * 
   * ref() 创建一个响应式引用：
   * - 当数据变化时，使用这个数据的组件会自动更新
   * - ref<API.LoginUserVO> 表示这个 ref 的类型是 API.LoginUserVO
   * - .value 用于访问或修改 ref 的实际值
   * 
   * 默认值设置为 '未登录'，表示用户还没有登录
   */
  const loginUser = ref<API.LoginUserVO>({
    userName: '未登录',
  })

  // ==================== 方法定义 ====================
  
  /**
   * fetchLoginUser - 从后端获取登录用户信息
   * 
   * async 表示这是一个异步函数，因为需要等待网络请求
   * 
   * 执行流程：
   * 1. 调用 getLoginUser() 向后端发送请求
   * 2. await 等待请求完成，获取响应结果
   * 3. 检查响应是否成功（code === 0）且有数据
   * 4. 如果成功，更新 loginUser 的值
   */
  async function fetchLoginUser() {
    // 发送请求获取用户信息
    const res = await getLoginUser()
    
    // 判断请求是否成功
    // res.data.code === 0 表示后端返回成功
    // res.data.data 存在表示有用户数据
    if (res.data.code === 0 && res.data.data) {
      // 更新登录用户信息
      // 注意：ref 类型的数据需要通过 .value 来访问和修改
      loginUser.value = res.data.data
    }
  }

  /**
   * setLoginUser - 手动设置登录用户信息
   * 
   * @param newLoginUser - 新的用户信息对象
   * 
   * 使用场景：
   * - 用户登录成功后，直接设置用户信息
   * - 用户退出登录时，清空用户信息
   * - 用户信息更新时，手动更新状态
   */
  function setLoginUser(newLoginUser: any) {
    loginUser.value = newLoginUser
  }

  // ==================== 导出 ====================
  /**
   * 返回需要暴露给组件使用的状态和方法
   * 
   * 在组件中使用方式：
   * const loginUserStore = useLoginUserStore()
   * console.log(loginUserStore.loginUser)  // 访问用户信息
   * loginUserStore.fetchLoginUser()        // 调用获取用户信息方法
   * loginUserStore.setLoginUser({...})     // 调用设置用户信息方法
   */
  return { 
    loginUser,        // 响应式的用户信息状态
    setLoginUser,     // 设置用户信息的方法
    fetchLoginUser    // 从后端获取用户信息的方法
  }
})
