<!--
  UserLoginPage.vue - 用户登录页面
  功能：提供用户登录表单，验证用户输入，调用登录接口
-->
<template>
  <!-- 
    登录页面容器
    id: 用于样式定位
  -->
  <div id="userLoginPage">
    <!-- 
      标题区域
      class="title": 应用自定义样式
    -->
    <h2 class="title">AI 应用生成 - 用户登录</h2>
    
    <!-- 
      描述文字
      class="desc": 应用自定义样式
      
      项目核心能力：
      1. 智能代码生成：用户输入需求描述，AI 自动分析并选择合适的生成策略，
         通过工具调用生成代码文件，采用流式输出让用户实时看到 AI 的执行过程
      2. 可视化编辑：生成的应用将实时展示，可以进入编辑模式，自由选择网页元素
         并且和 AI 对话来快速修改页面，直到满意为止
      3. 一键部署分享：可以将生成的应用一键部署到云端并自动截取封面图，获得可访问的
         地址进行分享，同时支持完整项目源码下载
    -->
    <div class="desc">输入需求描述，AI 自动生成代码文件</div>
    
    <!-- 
      Ant Design Vue 表单组件
      
      :model: 绑定表单数据对象（双向绑定）
      name="basic": 表单名称，用于表单验证
      autocomplete="off": 关闭浏览器自动填充
      @finish: 表单验证通过后触发的事件
      
      表单工作流程：
      1. 用户输入数据 → 自动更新 formState
      2. 用户点击提交 → 触发表单验证
      3. 验证通过 → 触发 @finish 事件，调用 handleSubmit
      4. 验证失败 → 显示错误提示，不触发 @finish
    -->
    <a-form
      :model="formState"
      name="basic"
      autocomplete="off"
      @finish="handleSubmit"
    >
      <!-- 
        表单项：用户账号
        
        name: 字段名称，对应 formState 中的属性
        :rules: 验证规则数组
          - required: 是否必填
          - message: 验证失败时显示的提示信息
      -->
      <a-form-item
        name="userAccount"
        :rules="[{ required: true, message: '请输入账号' }]"
      >
        <!-- 
          输入框组件
          
          v-model:value: 双向绑定输入值
          placeholder: 占位符文本
          
          双向绑定说明：
          - 用户输入 → 自动更新 formState.userAccount
          - formState.userAccount 变化 → 自动更新输入框显示
        -->
        <a-input
          v-model:value="formState.userAccount"
          placeholder="请输入账号"
        />
      </a-form-item>

      <!-- 
        表单项：用户密码
        
        多个验证规则：
        1. required: 必填验证
        2. min: 最小长度验证（至少 8 位）
      -->
      <a-form-item
        name="userPassword"
        :rules="[
          { required: true, message: '请输入密码' },
          { min: 8, message: '密码不能少于 8 位' }
        ]"
      >
        <!-- 
          密码输入框组件
          
          a-input-password: Ant Design 的密码输入框
          特点：
          - 自动隐藏密码字符
          - 提供显示/隐藏密码的切换按钮
        -->
        <a-input-password
          v-model:value="formState.userPassword"
          placeholder="请输入密码"
        />
      </a-form-item>

      <!-- 
        提示信息区域
        class="tips": 应用自定义样式
      -->
      <div class="tips">
        没有账号？
        <!-- 
          RouterLink: Vue Router 的导航组件
          to: 目标路由路径
          点击后会跳转到注册页面
        -->
        <RouterLink to="/user/register">去注册</RouterLink>
      </div>

      <!-- 
        表单项：提交按钮
        
        注意：这个 a-form-item 没有 name 属性
        因为它不是数据字段，只是一个操作按钮
      -->
      <a-form-item>
        <!-- 
          提交按钮
          
          type="primary": 主要按钮样式（蓝色）
          html-type="submit": HTML 原生的提交类型
          style: 内联样式，设置按钮宽度为 100%
          
          点击流程：
          1. 点击按钮 → 触发表单验证
          2. 验证通过 → 触发 @finish 事件
          3. 验证失败 → 显示错误提示
        -->
        <a-button
          type="primary"
          html-type="submit"
          style="width: 100%"
        >
          登录
        </a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
/**
 * 导入 Vue 的响应式 API
 * reactive: 创建响应式对象，用于表单数据
 */
import { reactive } from 'vue'

/**
 * 导入 Vue Router 的路由钩子
 * useRouter: 用于页面跳转
 */
import { useRouter } from 'vue-router'

/**
 * 导入 Ant Design Vue 的消息提示组件
 * message: 用于显示成功、错误等提示信息
 */
import { message } from 'ant-design-vue'

/**
 * 导入登录 API 接口
 * userLogin: 调用后端登录接口
 */
import { userLogin } from '@/api/userController'

/**
 * 导入 Pinia store
 * useLoginUserStore: 用于更新全局的用户登录状态
 */
import { useLoginUserStore } from '@/stores/loginUser'

/**
 * 获取路由实例
 * 用于登录成功后跳转到首页
 */
const router = useRouter()

/**
 * 获取登录用户 store 实例
 * 用于登录成功后更新全局用户状态
 */
const loginUserStore = useLoginUserStore()

/**
 * 表单数据对象
 * 
 * reactive: 创建响应式对象
 * - 对象的属性变化时，视图会自动更新
 * - 与 ref 的区别：reactive 用于对象，ref 用于基本类型
 * 
 * reactive<API.UserLoginRequest>: TypeScript 泛型
 * - 指定对象的类型为 API.UserLoginRequest
 * - 这个类型是从后端 API 自动生成的
 * - 确保前端发送的数据格式与后端期望的一致
 */
const formState = reactive<API.UserLoginRequest>({
  userAccount: '',    // 用户账号，初始为空字符串
  userPassword: '',   // 用户密码，初始为空字符串
})

/**
 * 表单提交处理函数
 * 
 * 触发时机：
 * - 用户点击"登录"按钮
 * - 表单验证全部通过
 * 
 * @param values - Ant Design 表单自动传入的表单数据
 *   - 包含通过验证的所有字段值
 *   - 类型为 any，实际上是 { userAccount: string, userPassword: string }
 * 
 * async: 异步函数
 * - 因为需要等待网络请求完成
 * - 可以使用 await 等待 Promise
 * 
 * 执行流程：
 * 1. 调用后端登录接口
 * 2. 等待响应结果
 * 3. 判断是否成功
 * 4. 成功：重新获取用户信息，跳转首页
 * 5. 失败：显示错误提示
 */
const handleSubmit = async (values: any) => {
  /**
   * 调用登录 API
   * 
   * userLogin(values): 发送登录请求
   * - values 是 Ant Design 表单自动传入的，包含用户输入的账号和密码
   * - 后端会验证账号密码是否正确
   * 
   * await: 等待请求完成
   * - 暂停函数执行，等待 Promise 完成
   * - 请求成功后继续执行后面的代码
   * 
   * res: 响应结果对象
   * - res.data.code: 响应状态码（0 表示成功）
   * - res.data.data: 返回的用户信息
   * - res.data.message: 响应消息
   */
  const res = await userLogin(values)
  
  /**
   * 判断登录是否成功
   * 
   * res.data.code === 0: 后端约定的成功状态码
   * res.data.data: 确保返回了用户数据
   */
  if (res.data.code === 0 && res.data.data) {
    /**
     * 登录成功的处理流程
     */
    
    /**
     * 1. 重新从后端获取完整的用户信息
     * 
     * 为什么不直接使用 res.data.data？
     * - 登录接口返回的可能是简化的用户信息
     * - fetchLoginUser() 会获取完整的用户信息（包括权限、设置等）
     * - 确保获取到的是最新的用户状态
     * 
     * await: 等待获取完成后再继续
     * - 确保用户信息已经更新到 store 中
     * - 跳转到首页时，头部能正确显示用户信息
     */
    await loginUserStore.fetchLoginUser()
    
    /**
     * 2. 显示成功提示
     * 
     * message.success: Ant Design 的成功提示
     * - 会在页面顶部显示绿色的成功消息
     * - 几秒后自动消失
     */
    message.success('登录成功')
    
    /**
     * 3. 跳转到首页
     * 
     * router.push: 编程式导航
     * - path: '/': 跳转到首页路径
     * - replace: true: 使用替换模式
     * 
     * replace 模式的作用：
     * - 不会在浏览器历史记录中留下登录页的记录
     * - 用户点击浏览器的"后退"按钮时，不会回到登录页
     * - 而是回到登录页之前的页面
     * 
     * 为什么要用 replace？
     * - 用户已经登录成功，不应该再回到登录页
     * - 提升用户体验，避免误操作
     */
    router.push({
      path: '/',
      replace: true
    })
  } else {
    /**
     * 登录失败的处理
     * 
     * message.error: Ant Design 的错误提示
     * - 会在页面顶部显示红色的错误消息
     * - 显示后端返回的具体错误信息
     * 
     * 错误信息格式：'登录失败，' + res.data.message
     * - 例如：'登录失败，账号或密码错误'
     * - 例如：'登录失败，账号已被禁用'
     */
    message.error('登录失败，' + res.data.message)
  }
}

/**
 * 知识点总结：
 * 
 * 1. reactive vs ref
 *    - reactive: 用于对象，直接访问属性（formState.userAccount）
 *    - ref: 用于基本类型，需要 .value 访问（count.value）
 * 
 * 2. v-model:value
 *    - 双向数据绑定
 *    - 输入框变化 → 更新数据
 *    - 数据变化 → 更新输入框
 * 
 * 3. 表单验证
 *    - :rules 定义验证规则
 *    - @finish 验证通过后触发，自动传入 values 参数
 *    - 自动显示错误提示
 * 
 * 4. async/await
 *    - async: 声明异步函数
 *    - await: 等待 Promise 完成
 *    - 可以连续 await 多个异步操作
 * 
 * 5. Pinia store
 *    - 全局状态管理
 *    - fetchLoginUser() 从后端获取最新用户信息
 *    - 一处修改，处处更新
 * 
 * 6. 路由跳转的两种模式
 *    - push: 添加历史记录，可以后退
 *    - replace: 替换当前记录，不可后退
 *    - 登录成功用 replace，防止用户后退到登录页
 * 
 * 7. Ant Design 表单的 @finish 事件
 *    - 验证通过后自动触发
 *    - 自动传入 values 参数（包含所有表单字段）
 *    - 不需要手动获取表单数据
 */
</script>

<style scoped>
/**
 * scoped 样式：只在当前组件内生效
 * 避免样式污染其他组件
 */

/* 登录页面容器样式 */
#userLoginPage {
  max-width: 400px;           /* 最大宽度 400px */
  margin: 80px auto;          /* 上下 80px，左右自动居中 */
  padding: 40px;              /* 内边距 40px */
  background: #fff;           /* 白色背景 */
  border-radius: 8px;         /* 圆角 8px */
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);  /* 阴影效果 */
}

/* 标题样式 */
.title {
  text-align: center;         /* 文字居中 */
  font-size: 24px;            /* 字体大小 24px */
  font-weight: 600;           /* 字体粗细：半粗体 */
  color: #1890ff;             /* Ant Design 主题蓝色 */
  margin-bottom: 8px;         /* 底部外边距 8px */
}

/* 描述文字样式 */
.desc {
  text-align: center;         /* 文字居中 */
  color: #666;                /* 灰色文字 */
  margin-bottom: 32px;        /* 底部外边距 32px */
  font-size: 14px;            /* 字体大小 14px */
}

/* 提示信息样式 */
.tips {
  text-align: center;         /* 文字居中 */
  margin-bottom: 16px;        /* 底部外边距 16px */
  color: #666;                /* 灰色文字 */
  font-size: 14px;            /* 字体大小 14px */
}

/* 链接样式 */
.tips a {
  color: #1890ff;             /* Ant Design 主题蓝色 */
  text-decoration: none;      /* 去除下划线 */
}

/* 链接悬停样式 */
.tips a:hover {
  text-decoration: underline; /* 悬停时显示下划线 */
}

/**
 * 响应式设计：手机端适配
 * 当屏幕宽度小于等于 576px 时应用这些样式
 */
@media (max-width: 576px) {
  #userLoginPage {
    margin: 40px 16px;        /* 减小上下边距，左右边距 16px */
    padding: 24px;            /* 减小内边距 */
  }

  .title {
    font-size: 20px;          /* 减小标题字体 */
  }
}
</style>
