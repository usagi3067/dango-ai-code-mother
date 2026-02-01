<!--
  UserRegisterPage.vue - 用户注册页面
  功能：提供用户注册表单，验证用户输入，调用注册接口
-->
<template>
  <!-- 
    注册页面容器
    id: 用于样式定位
  -->
  <div id="userRegisterPage">
    <!-- 
      标题区域
      class="title": 应用自定义样式
    -->
    <h2 class="title">AI 应用生成 - 用户注册</h2>
    
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
        表单项：确认密码
        
        自定义验证规则：
        1. required: 必填验证
        2. validator: 自定义验证函数
           - 检查两次密码是否一致
           - 不一致时返回错误提示
      -->
      <a-form-item
        name="checkPassword"
        :rules="[
          { required: true, message: '请再次输入密码' },
          { validator: validatePassword }
        ]"
      >
        <!-- 
          确认密码输入框
          
          用途：
          - 防止用户输入错误的密码
          - 确保用户知道自己设置的密码
        -->
        <a-input-password
          v-model:value="formState.checkPassword"
          placeholder="请再次输入密码"
        />
      </a-form-item>

      <!-- 
        提示信息区域
        class="tips": 应用自定义样式
      -->
      <div class="tips">
        已有账号？
        <!-- 
          RouterLink: Vue Router 的导航组件
          to: 目标路由路径
          点击后会跳转到登录页面
        -->
        <RouterLink to="/user/login">去登录</RouterLink>
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
          注册
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
 * 导入注册 API 接口
 * userRegister: 调用后端注册接口
 */
import { userRegister } from '@/api/user/userController'

/**
 * 获取路由实例
 * 用于注册成功后跳转到登录页
 */
const router = useRouter()

/**
 * 表单数据对象
 * 
 * reactive: 创建响应式对象
 * - 对象的属性变化时，视图会自动更新
 * - 与 ref 的区别：reactive 用于对象，ref 用于基本类型
 * 
 * reactive<API.UserRegisterRequest>: TypeScript 泛型
 * - 指定对象的类型为 API.UserRegisterRequest
 * - 这个类型是从后端 API 自动生成的
 * - 确保前端发送的数据格式与后端期望的一致
 */
const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',      // 用户账号，初始为空字符串
  userPassword: '',     // 用户密码，初始为空字符串
  checkPassword: '',    // 确认密码，初始为空字符串
})

/**
 * 自定义密码验证函数
 * 
 * 用途：验证两次输入的密码是否一致
 * 
 * 参数说明：
 * - _rule: 验证规则对象（这里用不到，所以用下划线开头表示忽略）
 * - value: 当前输入框的值（确认密码的值）
 * 
 * 返回值：
 * - Promise.resolve(): 验证通过
 * - Promise.reject(new Error('...')): 验证失败，显示错误信息
 * 
 * 执行时机：
 * - 用户输入确认密码时
 * - 用户提交表单时
 */
const validatePassword = (_rule: any, value: string) => {
  /**
   * 判断两次密码是否一致
   * 
   * value: 确认密码的值
   * formState.userPassword: 第一次输入的密码
   */
  if (value !== formState.userPassword) {
    /**
     * 密码不一致，返回错误
     * Promise.reject: 表示验证失败
     * new Error('两次密码不一致'): 错误信息，会显示在输入框下方
     */
    return Promise.reject(new Error('两次密码不一致'))
  }
  
  /**
   * 密码一致，验证通过
   * Promise.resolve(): 表示验证成功
   */
  return Promise.resolve()
}

/**
 * 表单提交处理函数
 * 
 * 触发时机：
 * - 用户点击"注册"按钮
 * - 表单验证全部通过（包括自定义的密码验证）
 * 
 * @param values - Ant Design 表单自动传入的表单数据
 *   - 包含通过验证的所有字段值
 *   - 类型为 any，实际上是 { userAccount: string, userPassword: string, checkPassword: string }
 * 
 * async: 异步函数
 * - 因为需要等待网络请求完成
 * - 可以使用 await 等待 Promise
 * 
 * 执行流程：
 * 1. 调用后端注册接口
 * 2. 等待响应结果
 * 3. 判断是否成功
 * 4. 成功：显示提示，跳转到登录页
 * 5. 失败：显示错误提示
 */
const handleSubmit = async (values: any) => {
  /**
   * 调用注册 API
   * 
   * userRegister(values): 发送注册请求
   * - values 是 Ant Design 表单自动传入的，包含用户输入的所有数据
   * - 后端会验证账号是否已存在、密码格式是否正确等
   * 
   * await: 等待请求完成
   * - 暂停函数执行，等待 Promise 完成
   * - 请求成功后继续执行后面的代码
   * 
   * res: 响应结果对象
   * - res.data.code: 响应状态码（0 表示成功）
   * - res.data.data: 返回的用户 ID（注册成功后的用户 ID）
   * - res.data.message: 响应消息
   */
  const res = await userRegister(values)
  
  /**
   * 判断注册是否成功
   * 
   * res.data.code === 0: 后端约定的成功状态码
   * res.data.data: 确保返回了用户 ID（表示注册成功）
   */
  if (res.data.code === 0 && res.data.data) {
    /**
     * 注册成功的处理流程
     */
    
    /**
     * 1. 显示成功提示
     * 
     * message.success: Ant Design 的成功提示
     * - 会在页面顶部显示绿色的成功消息
     * - 几秒后自动消失
     * - 提示用户注册成功，可以去登录了
     */
    message.success('注册成功，请登录')
    
    /**
     * 2. 跳转到登录页面
     * 
     * router.push: 编程式导航
     * - path: '/user/login': 跳转到登录页面路径
     * - replace: true: 使用替换模式
     * 
     * replace 模式的作用：
     * - 不会在浏览器历史记录中留下注册页的记录
     * - 用户点击浏览器的"后退"按钮时，不会回到注册页
     * - 而是回到注册页之前的页面
     * 
     * 为什么要用 replace？
     * - 用户已经注册成功，不应该再回到注册页
     * - 避免用户重复注册
     * - 提升用户体验
     */
    router.push({
      path: '/user/login',
      replace: true
    })
  } else {
    /**
     * 注册失败的处理
     * 
     * message.error: Ant Design 的错误提示
     * - 会在页面顶部显示红色的错误消息
     * - 显示后端返回的具体错误信息
     * 
     * 错误信息格式：'注册失败，' + res.data.message
     * - 例如：'注册失败，账号已存在'
     * - 例如：'注册失败，密码格式不正确'
     */
    message.error('注册失败，' + res.data.message)
  }
}


/**
 * 知识点总结：
 * 
 * 1. reactive 响应式对象
 *    - 用于创建响应式的表单数据
 *    - 直接访问属性，不需要 .value
 *    - 适合多个字段的表单
 * 
 * 2. v-model:value 双向绑定
 *    - 用户输入 → 自动更新 formState
 *    - formState 变化 → 自动更新输入框
 *    - 实现数据和视图的同步
 * 
 * 3. 表单验证规则
 *    - required: 必填验证
 *    - min: 最小长度验证
 *    - validator: 自定义验证函数
 *    - 验证失败自动显示错误提示
 * 
 * 4. 自定义验证函数
 *    - 用于复杂的验证逻辑（如密码一致性）
 *    - 返回 Promise.resolve() 表示通过
 *    - 返回 Promise.reject(new Error('...')) 表示失败
 *    - 可以访问表单的其他字段进行比较
 * 
 * 5. Ant Design 表单的 @finish 事件
 *    - 所有验证通过后才触发
 *    - 自动传入 values 参数（包含所有表单字段）
 *    - 不需要手动获取表单数据
 * 
 * 6. 路由跳转的 replace 模式
 *    - replace: true 替换当前历史记录
 *    - 防止用户后退到注册页
 *    - 避免重复注册
 * 
 * 7. 注册成功后的流程
 *    - 显示成功提示
 *    - 跳转到登录页（不是自动登录）
 *    - 让用户手动登录，确保流程完整
 * 
 * 8. 与登录页的区别
 *    - 多了"确认密码"字段
 *    - 使用自定义验证函数检查密码一致性
 *    - 成功后跳转到登录页（而不是首页）
 *    - 不需要更新全局用户状态（因为还没登录）
 */
</script>

<style scoped>
/**
 * scoped 样式：只在当前组件内生效
 * 避免样式污染其他组件
 */

/* 注册页面容器样式 */
#userRegisterPage {
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
  #userRegisterPage {
    margin: 40px 16px;        /* 减小上下边距，左右边距 16px */
    padding: 24px;            /* 减小内边距 */
  }

  .title {
    font-size: 20px;          /* 减小标题字体 */
  }
}
</style>
