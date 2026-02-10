<!--
  HomePage.vue - 主页组件
  
  【页面功能】
  这是应用的主页，包含以下核心功能：
  1. Hero 区域：展示网站标题和 AI 提示词输入框
  2. 我的作品：展示当前登录用户创建的应用列表（需要登录）
  3. 精选案例：展示被管理员设为精选的优秀应用
  
  【Vue 单文件组件结构】
  一个 .vue 文件由三部分组成：
  - <template>: HTML 模板，定义页面结构
  - <script>: JavaScript/TypeScript 逻辑
  - <style>: CSS 样式
  
  【学习要点】
  1. Vue 3 组合式 API (Composition API)
  2. 响应式数据 (ref, reactive)
  3. 生命周期钩子 (onMounted)
  4. 条件渲染 (v-if, v-else)
  5. 列表渲染 (v-for)
  6. 事件绑定 (@click, @pressEnter)
  7. 双向绑定 (v-model)
-->
<template>
  <!-- 
    页面根容器
    class="home-page": 绑定 CSS 类名，用于样式控制
  -->
  <div class="home-page">
    
    <!-- ==================== Hero 区域 ==================== -->
    <!-- 
      Hero 区域是网站的"门面"，通常包含：
      - 吸引眼球的标题
      - 简短的描述
      - 核心操作入口（这里是提示词输入框）
    -->
    <div class="hero-section">
      <!-- 
        标题区域
        使用 flex 布局让 Logo 和文字水平排列
      -->
      <div class="hero-title">
        <span class="title-text">一句话</span>
        <!-- 
          @/assets/logo.png: @ 是路径别名，指向 src 目录
          这是在 vite.config.ts 中配置的
        -->
        <img src="@/assets/logo.png" alt="Logo" class="hero-logo" />
        <span class="title-text">呈所想</span>
      </div>
      
      <!-- 副标题 -->
      <p class="hero-subtitle">与 AI 对话轻松创建应用和网站</p>
      
      <!-- 
        提示词输入框区域
        这是页面的核心交互区域
      -->
      <div class="prompt-input-wrapper">
        <!-- 
          a-textarea: Ant Design Vue 的多行文本输入框组件
          
          v-model:value: Vue 3 的双向绑定语法
          - v-model 是 Vue 的双向数据绑定指令
          - :value 表示绑定的是 value 属性
          - 当用户输入时，promptText 会自动更新
          - 当 promptText 变化时，输入框内容也会更新
          
          :auto-size: 自动调整高度
          - minRows: 最小行数
          - maxRows: 最大行数
          
          @pressEnter: 按下回车键时触发的事件
          - @ 是 v-on: 的简写，用于绑定事件
        -->
        <a-textarea
          v-model:value="promptText"
          :auto-size="{ minRows: 2, maxRows: 4 }"
          placeholder="使用 NoCode 创建一个高效的小工具，帮我计算......"
          class="prompt-input"
          @pressEnter="handleCreateApp"
        />
        
        <!-- 输入框下方的操作按钮 -->
        <div class="input-actions">
          <!-- 左侧按钮组 -->
          <div class="input-actions-left">
            <!-- 
              a-button: Ant Design Vue 的按钮组件
              type="text": 文字按钮样式（无背景色）
              disabled: 禁用状态（这些功能暂未实现）
            -->
            <a-upload
              :show-upload-list="false"
              :before-upload="beforeUpload"
              :custom-request="handleUpload"
              accept=".html"
            >
              <a-button type="text" :loading="uploading">
                <template #icon><UploadOutlined /></template>
                上传
              </a-button>
            </a-upload>
            <a-button type="text" disabled>
              <template #icon><ThunderboltOutlined /></template>
              优化
            </a-button>
          </div>
          
          <!-- 右侧：发送按钮 -->
          <div class="input-actions-right">
            <!--
              发送按钮
              type="primary": 主要按钮样式（蓝色/绑定主题色）
              shape="circle": 圆形按钮
              :loading: 动态绑定加载状态
              - 冒号 : 表示这是一个动态绑定（v-bind 的简写）
              - 当 creating 为 true 时，按钮显示加载动画
            -->
            <a-button
              type="primary"
              shape="circle"
              class="send-btn"
              :loading="creating"
              @click="handleCreateApp"
            >
              <template #icon><SendOutlined /></template>
            </a-button>
          </div>
        </div>
      </div>

      <!-- 
        快捷标签区域
        点击标签可以快速填充提示词
      -->
      <div class="quick-tags">
        <!-- 
          v-for: Vue 的列表渲染指令
          语法: v-for="item in array"
          
          :key: 列表渲染时必须提供的唯一标识
          - Vue 使用 key 来追踪每个节点的身份
          - 有助于 Vue 高效地更新 DOM
          - 通常使用 id 或唯一值作为 key
          
          @click: 点击事件
          - 这里直接赋值，点击后 promptText 变为标签内容
        -->
        <a-tag 
          v-for="tag in quickTags" 
          :key="tag" 
          class="quick-tag"
          @click="promptText = tag"
        >
          <!-- 
            {{ }}: Vue 的插值语法
            用于在模板中显示变量的值
          -->
          {{ tag }}
        </a-tag>
      </div>
    </div>

    <!-- ==================== 我的作品区域 ==================== -->
    <!-- 
      v-if: 条件渲染指令
      只有当条件为真时，这个元素才会被渲染到 DOM 中
      
      loginUserStore.loginUser.id: 判断用户是否已登录
      - 如果用户已登录，会有 id
      - 如果未登录，id 为 undefined 或 null
    -->
    <div v-if="loginUserStore.loginUser.id" class="section my-apps-section">
      <!-- 区块头部：标题 + 分页器 -->
      <div class="section-header">
        <h2 class="section-title">我的作品</h2>
        
        <!-- 
          a-pagination: Ant Design Vue 的分页组件
          
          v-if: 只有当有数据时才显示分页器
          
          v-model:current: 双向绑定当前页码
          v-model:pageSize: 双向绑定每页条数
          :total: 总记录数（单向绑定，只读）
          
          @change: 页码变化时触发的事件
        -->
        <a-pagination
          v-if="myAppsPagination.total > 0"
          v-model:current="myAppsPagination.current"
          v-model:pageSize="myAppsPagination.pageSize"
          :total="myAppsPagination.total"
          :show-size-changer="false"
          size="small"
          @change="loadMyApps"
        />
      </div>
      
      <!-- 
        a-spin: Ant Design Vue 的加载动画组件
        :spinning: 是否显示加载动画
        包裹的内容在加载时会显示遮罩效果
      -->
      <a-spin :spinning="myAppsLoading">
        <!-- 
          应用卡片网格
          v-if/v-else: 条件渲染
          - 有数据时显示卡片列表
          - 无数据时显示空状态
        -->
        <div v-if="myApps.length > 0" class="app-grid">
          <AppCard
            v-for="app in myApps"
            :key="String(app.id)"
            :app="app"
            @view-chat="goToAppChat"
            @view-work="openDeployedApp"
          />
        </div>
        
        <!-- 
          a-empty: Ant Design Vue 的空状态组件
          当没有数据时显示友好的提示
        -->
        <a-empty v-else description="暂无应用，快去创建吧~" />
      </a-spin>
    </div>

    <!-- ==================== 精选案例区域 ==================== -->
    <!-- 
      精选案例不需要登录就能看到
      结构与"我的作品"类似
    -->
    <div class="section featured-section">
      <div class="section-header">
        <h2 class="section-title">精选案例</h2>
        <a-pagination
          v-if="featuredPagination.total > 0"
          v-model:current="featuredPagination.current"
          v-model:pageSize="featuredPagination.pageSize"
          :total="featuredPagination.total"
          :show-size-changer="false"
          size="small"
          @change="loadFeaturedApps"
        />
      </div>
      
      <!-- 
        标签筛选区域
        显示"全部"和 8 个预定义标签按钮
        点击按钮可以筛选对应类别的应用
      -->
      <div class="tag-filter-section">
        <a-space wrap :size="[8, 8]">
          <!-- 
            "全部"按钮
            selectedTag 为空字符串时高亮
          -->
          <a-button 
            :type="selectedTag === '' ? 'primary' : 'default'"
            size="small"
            @click="handleTagChange('')"
          >
            全部
          </a-button>
          <!-- 
            标签按钮列表
            v-for 遍历 APP_TAG_OPTIONS 数组
            当前选中的标签显示为 primary 类型（高亮）
          -->
          <a-button 
            v-for="tag in APP_TAG_OPTIONS" 
            :key="tag.value"
            :type="selectedTag === tag.value ? 'primary' : 'default'"
            size="small"
            @click="handleTagChange(tag.value)"
          >
            {{ tag.label }}
          </a-button>
        </a-space>
      </div>
      
      <a-spin :spinning="featuredLoading">
        <div v-if="featuredApps.length > 0" class="app-grid">
          <AppCard
            v-for="app in featuredApps"
            :key="String(app.id)"
            :app="app"
            @view-chat="goToAppChat"
            @view-work="openDeployedApp"
          />
        </div>
        <a-empty v-else description="暂无精选案例" />
      </a-spin>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * ==================== Vue 3 组合式 API 入门 ====================
 * 
 * 【什么是组合式 API？】
 * Vue 3 引入了组合式 API (Composition API)，它是一种新的编写组件逻辑的方式。
 * 相比 Vue 2 的选项式 API (Options API)，组合式 API 更灵活、更易于复用。
 * 
 * 【script setup 是什么？】
 * <script setup> 是组合式 API 的语法糖，它让代码更简洁：
 * - 不需要 export default
 * - 不需要 return 导出变量
 * - 导入的组件自动注册
 * - 顶层变量和函数自动暴露给模板
 * 
 * 【lang="ts" 是什么？】
 * 表示使用 TypeScript 语言
 * TypeScript 是 JavaScript 的超集，添加了类型系统
 * 可以在编译时发现错误，提高代码质量
 */

// ==================== 导入部分 ====================

/**
 * 从 Vue 导入响应式 API
 * 
 * ref: 创建响应式的基本类型数据（字符串、数字、布尔值等）
 *      访问/修改值需要使用 .value
 *      例如: const count = ref(0); count.value++
 * 
 * reactive: 创建响应式的对象/数组
 *           直接访问属性，不需要 .value
 *           例如: const state = reactive({ count: 0 }); state.count++
 * 
 * onMounted: 生命周期钩子，组件挂载到 DOM 后执行
 *            适合做初始化操作，如加载数据
 */
import { ref, reactive, onMounted } from 'vue'

/**
 * 从 Vue Router 导入路由相关函数
 * 
 * useRouter: 获取路由实例，用于编程式导航
 *            例如: router.push('/path') 跳转到指定路径
 */
import { useRouter } from 'vue-router'

/**
 * 从 Ant Design Vue 导入消息提示组件
 * 
 * message: 全局消息提示
 *          message.success('成功') - 绿色成功提示
 *          message.error('失败') - 红色错误提示
 *          message.warning('警告') - 黄色警告提示
 */
import { message } from 'ant-design-vue'
import type { UploadProps } from 'ant-design-vue'

/**
 * 从 Ant Design Vue 图标库导入图标组件
 * 
 * 图标命名规则: [图标名]Outlined / [图标名]Filled / [图标名]TwoTone
 * - Outlined: 线框风格（最常用）
 * - Filled: 实心风格
 * - TwoTone: 双色风格
 */
import {
  UploadOutlined,      // 上传图标
  ThunderboltOutlined, // 闪电图标（优化）
  SendOutlined         // 发送图标
} from '@ant-design/icons-vue'

/**
 * 导入应用卡片组件
 */
import AppCard from '@/components/AppCard.vue'

/**
 * 导入 API 接口函数
 * 
 * 这些函数是通过 OpenAPI 自动生成的
 * 每个函数对应后端的一个接口
 * 
 * addApp: 创建应用
 * listMyAppVoByPage: 分页查询我的应用
 * listGoodAppVoByPage: 分页查询精选应用
 */
import { addApp, listMyAppVoByPage, listGoodAppVoByPage, uploadHtmlFile } from '@/api/app/appController'

/**
 * 导入环境变量配置
 * getDeployUrl: 获取部署应用的完整 URL
 */
import { getDeployUrl } from '@/config/env'

/**
 * 导入 Pinia Store
 * 
 * Pinia 是 Vue 3 官方推荐的状态管理库
 * Store 用于在组件之间共享状态
 * 
 * useLoginUserStore: 登录用户状态管理
 * 包含当前登录用户的信息
 */
import { useLoginUserStore } from '@/stores/loginUser'

/**
 * 导入应用标签配置
 * 
 * APP_TAG_OPTIONS: 标签选项数组，用于渲染筛选按钮
 */
import { APP_TAG_OPTIONS } from '@/config/appTag'

// ==================== 初始化 ====================

/**
 * 获取路由实例
 * 用于页面跳转
 */
const router = useRouter()

/**
 * 获取登录用户 Store 实例
 * 可以访问 loginUserStore.loginUser 获取用户信息
 */
const loginUserStore = useLoginUserStore()

// ==================== 响应式数据定义 ====================

/**
 * 提示词输入框的内容
 * 
 * ref<string>(''): 
 * - ref: 创建响应式数据
 * - <string>: TypeScript 泛型，指定数据类型为字符串
 * - '': 初始值为空字符串
 * 
 * 在模板中使用时，Vue 会自动解包，不需要 .value
 * 在 script 中使用时，需要通过 .value 访问
 */
const promptText = ref('')

/**
 * 创建应用的加载状态
 * true: 正在创建中，按钮显示加载动画
 * false: 空闲状态
 */
const creating = ref(false)

/**
 * 上传文件的加载状态
 * true: 正在上传中，按钮显示加载动画
 * false: 空闲状态
 */
const uploading = ref(false)

/**
 * 快捷标签数组
 * 这是一个普通数组，不是响应式的
 * 因为它不会变化，所以不需要响应式
 */
const quickTags = ['波普风电商页面', '企业网站', '电商运营后台', '暗黑话题社区']

/**
 * 我的应用列表
 * 
 * ref<API.AppVO[]>([]):
 * - API.AppVO: 应用的数据类型（在 typings.d.ts 中定义）
 * - []: 初始值为空数组
 */
const myApps = ref<API.AppVO[]>([])

/**
 * 我的应用加载状态
 */
const myAppsLoading = ref(false)

/**
 * 我的应用分页配置
 * 
 * reactive: 创建响应式对象
 * 适合包含多个属性的对象
 * 
 * 分页参数说明:
 * - current: 当前页码（从 1 开始）
 * - pageSize: 每页显示条数
 * - total: 总记录数（从后端获取）
 */
const myAppsPagination = reactive({
  current: 1,
  pageSize: 6,
  total: 0
})

/**
 * 精选应用列表
 */
const featuredApps = ref<API.AppVO[]>([])

/**
 * 精选应用加载状态
 */
const featuredLoading = ref(false)

/**
 * 精选应用分页配置
 */
const featuredPagination = reactive({
  current: 1,
  pageSize: 6,
  total: 0
})

/**
 * 当前选中的标签筛选
 * 空字符串表示"全部"
 */
const selectedTag = ref('')

// ==================== 方法定义 ====================

/**
 * 创建应用
 * 
 * async: 异步函数标记
 * 异步函数可以使用 await 等待 Promise 完成
 * 
 * 【执行流程】
 * 1. 验证输入（提示词不能为空）
 * 2. 验证登录状态
 * 3. 调用后端接口创建应用
 * 4. 创建成功后跳转到对话页面
 */
const handleCreateApp = async () => {
  // trim(): 去除字符串首尾空格
  // 如果去除空格后为空，说明用户没有输入有效内容
  if (!promptText.value.trim()) {
    message.warning('请输入提示词')
    return  // 提前返回，不执行后续代码
  }
  
  // 检查用户是否已登录
  // 如果没有 id，说明未登录
  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    // 跳转到登录页
    router.push('/user/login')
    return
  }
  
  // 设置加载状态为 true，按钮显示加载动画
  creating.value = true
  
  /**
   * try-catch-finally: 异常处理结构
   * 
   * try: 尝试执行的代码（可能会出错）
   * catch: 捕获错误并处理
   * finally: 无论成功失败都会执行（可选）
   */
  try {
    // 调用后端接口创建应用
    // await: 等待异步操作完成
    const res = await addApp({ initPrompt: promptText.value })
    
    /**
     * 判断请求是否成功
     * 
     * res.data.code === 0: 后端约定的成功状态码
     * res.data.data: 返回的数据（这里是应用 ID）
     */
    if (res.data.code === 0 && res.data.data) {
      /**
       * 【重要】ID 精度问题处理
       * 
       * 后端返回的 ID 是 Long 类型（64位整数）
       * JavaScript 的 Number 只能精确表示 53 位整数
       * 超过 Number.MAX_SAFE_INTEGER (9007199254740991) 会丢失精度
       * 
       * 解决方案：始终将 ID 作为字符串处理
       * String(res.data.data): 将数字转为字符串
       */
      const appId = String(res.data.data)
      
      message.success('应用创建成功')
      
      /**
       * 路由跳转
       *
       * router.push(): 编程式导航
       * 模板字符串 ``: 可以嵌入变量 ${变量名}
       *
       * 跳转到应用对话页面，路径格式: /app/chat/应用ID
       */
      router.push(`/app/chat/${appId}`)
    } else {
      // 请求失败，显示错误信息
      message.error('创建失败：' + res.data.message)
    }
  } catch (error) {
    // 捕获异常（如网络错误）
    message.error('创建失败，请稍后重试')
  } finally {
    // 无论成功失败，都要关闭加载状态
    creating.value = false
  }
}

/**
 * 上传前校验
 */
const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  // 校验登录状态
  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    router.push('/user/login')
    return false
  }

  // 校验文件类型
  const isHtml = file.name.endsWith('.html')
  if (!isHtml) {
    message.error('仅支持上传 .html 文件')
    return false
  }

  // 校验文件大小 (2MB)
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    message.error('文件大小不能超过 2MB')
    return false
  }

  return true
}

/**
 * 自定义上传行为
 */
const handleUpload: UploadProps['customRequest'] = async (options) => {
  const { file } = options

  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file as File)

    const res = await uploadHtmlFile(formData)
    if (res.data.code === 0 && res.data.data) {
      message.success('上传成功')
      router.push(`/app/chat/${String(res.data.data)}`)
    } else {
      message.error('上传失败：' + res.data.message)
    }
  } catch (error) {
    message.error('上传失败，请稍后重试')
  } finally {
    uploading.value = false
  }
}

/**
 * 加载我的应用列表
 * 
 * 这个函数会在以下情况被调用：
 * 1. 页面初始化时 (onMounted)
 * 2. 用户切换分页时 (@change)
 */
const loadMyApps = async () => {
  // 如果用户未登录，不加载数据
  if (!loginUserStore.loginUser.id) return
  
  // 显示加载动画
  myAppsLoading.value = true
  
  try {
    /**
     * 调用后端分页查询接口
     * 
     * 参数说明：
     * - pageNum: 页码（后端使用 pageNum，不是 current）
     * - pageSize: 每页条数
     * - sortField: 排序字段
     * - sortOrder: 排序方式 (asc 升序 / desc 降序)
     */
    const res = await listMyAppVoByPage({
      pageNum: myAppsPagination.current,
      pageSize: myAppsPagination.pageSize,
      sortField: 'createTime',  // 按创建时间排序
      sortOrder: 'desc'         // 降序（最新的在前面）
    })
    
    if (res.data.code === 0 && res.data.data) {
      /**
       * 更新数据
       * 
       * res.data.data.records: 当前页的数据列表
       * res.data.data.totalRow: 总记录数
       * 
       * || []: 如果 records 为 null/undefined，使用空数组
       * || 0: 如果 totalRow 为 null/undefined，使用 0
       */
      myApps.value = res.data.data.records || []
      myAppsPagination.total = res.data.data.totalRow || 0
    }
  } catch (error) {
    // 打印错误到控制台，方便调试
    console.error('加载我的应用失败:', error)
  } finally {
    // 关闭加载动画
    myAppsLoading.value = false
  }
}

/**
 * 加载精选应用列表
 * 逻辑与 loadMyApps 类似
 * 支持按标签筛选
 */
const loadFeaturedApps = async () => {
  featuredLoading.value = true
  
  try {
    // 构建查询参数
    const params: API.AppQueryRequest = {
      pageNum: featuredPagination.current,
      pageSize: featuredPagination.pageSize,
      sortField: 'createTime',  // 按创建时间排序
      sortOrder: 'desc'       // 优先级高的在前面
    }
    
    // 如果选中了标签，添加标签筛选条件
    if (selectedTag.value) {
      params.tag = selectedTag.value
    }
    
    const res = await listGoodAppVoByPage(params)
    
    if (res.data.code === 0 && res.data.data) {
      featuredApps.value = res.data.data.records || []
      featuredPagination.total = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('加载精选应用失败:', error)
  } finally {
    featuredLoading.value = false
  }
}

/**
 * 处理标签筛选切换
 * 
 * @param tag - 选中的标签值，空字符串表示"全部"
 */
const handleTagChange = (tag: string) => {
  selectedTag.value = tag
  // 切换标签时重置到第 1 页
  featuredPagination.current = 1
  // 重新加载数据
  loadFeaturedApps()
}

/**
 * 跳转到应用对话页
 * 
 * @param app - 应用对象
 */
const goToAppChat = (app: API.AppVO) => {
  // 将 ID 转为字符串，避免精度丢失
  // 添加 view=1 参数，表示是查看模式，不自动发送消息
  router.push(`/app/chat/${String(app.id)}?view=1`)
}

/**
 * 打开部署的应用
 * 
 * @param app - 应用对象
 */
const openDeployedApp = (app: API.AppVO) => {
  if (app.deployKey) {
    // 使用环境变量配置的部署域名
    window.open(getDeployUrl(app.deployKey), '_blank')
  }
}

/**
 * 格式化相对时间
 * 
 * 将时间戳转换为人类可读的相对时间
 * 例如: "5分钟前"、"2小时前"、"3天前"
 * 
 * @param dateStr - 日期字符串（ISO 格式）
 * @returns 格式化后的相对时间字符串
 */
const formatRelativeTime = (dateStr: string | undefined) => {
  // 如果没有日期，返回空字符串
  if (!dateStr) return ''
  
  // 创建 Date 对象
  const date = new Date(dateStr)
  const now = new Date()
  
  // 计算时间差（毫秒）
  const diff = now.getTime() - date.getTime()
  
  // 转换为各种时间单位
  const minutes = Math.floor(diff / 60000)      // 1分钟 = 60000毫秒
  const hours = Math.floor(diff / 3600000)      // 1小时 = 3600000毫秒
  const days = Math.floor(diff / 86400000)      // 1天 = 86400000毫秒
  const weeks = Math.floor(diff / 604800000)    // 1周 = 604800000毫秒
  
  // 根据时间差返回不同的格式
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  if (weeks < 4) return `${weeks}周前`
  
  // 超过4周，显示具体日期
  return date.toLocaleDateString('zh-CN')
}

// ==================== 生命周期钩子 ====================

/**
 * onMounted: 组件挂载完成后执行
 * 
 * 【Vue 组件生命周期】
 * 1. setup(): 组件初始化（组合式 API 的入口）
 * 2. onBeforeMount: 挂载前
 * 3. onMounted: 挂载后 ← 我们在这里
 * 4. onBeforeUpdate: 更新前
 * 5. onUpdated: 更新后
 * 6. onBeforeUnmount: 卸载前
 * 7. onUnmounted: 卸载后
 * 
 * onMounted 适合做的事情：
 * - 发送网络请求获取初始数据
 * - 操作 DOM 元素
 * - 初始化第三方库
 */
onMounted(() => {
  // 页面加载时，获取应用列表
  loadMyApps()
  loadFeaturedApps()
})
</script>

<style scoped>
/**
 * ==================== CSS 样式入门 ====================
 * 
 * 【scoped 是什么？】
 * scoped 属性让样式只在当前组件内生效
 * Vue 会自动为每个元素添加唯一的属性选择器
 * 避免样式污染其他组件
 * 
 * 【CSS 选择器】
 * .class-name: 类选择器，匹配 class="class-name" 的元素
 * #id-name: ID 选择器，匹配 id="id-name" 的元素
 * element: 元素选择器，匹配指定的 HTML 元素
 * 
 * 【常用 CSS 属性】
 * - display: 显示模式 (flex, block, inline, grid 等)
 * - padding: 内边距（元素内容与边框的距离）
 * - margin: 外边距（元素与其他元素的距离）
 * - background: 背景（颜色、图片、渐变等）
 * - border: 边框
 * - border-radius: 圆角
 * - box-shadow: 阴影
 */

/* 页面根容器 */
.home-page {
  padding: 0;
  /* 
   * linear-gradient: CSS 渐变函数
   * 180deg: 渐变方向（从上到下）
   * #e8f4f0 0%: 起始颜色和位置
   * #f5f7fa 30%: 结束颜色和位置
   */
  background: linear-gradient(180deg, #e8f4f0 0%, #f5f7fa 30%);
  /* 
   * min-height: 最小高度
   * calc(): CSS 计算函数
   * 100vh: 视口高度的 100%
   * 减去头部(64px)和底部(72px)的高度
   */
  min-height: calc(100vh - 64px - 72px);
}

/* ==================== Hero 区域样式 ==================== */

.hero-section {
  text-align: center;  /* 文字居中 */
  padding: 60px 24px 40px;  /* 上 左右 下 的内边距 */
}

/* 标题容器 */
.hero-title {
  /* 
   * Flexbox 布局
   * display: flex 启用弹性布局
   * align-items: center 垂直居中
   * justify-content: center 水平居中
   * gap: 子元素之间的间距
   */
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 12px;
}

/* 标题文字 */
.title-text {
  font-size: 42px;
  font-weight: 700;  /* 字体粗细：700 是粗体 */
  /* 
   * 文字渐变效果
   * 1. 设置背景为渐变色
   * 2. background-clip: text 让背景只显示在文字区域
   * 3. -webkit-text-fill-color: transparent 让文字透明，显示背景
   */
  background: linear-gradient(135deg, #1a1a1a 0%, #4a4a4a 100%);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

/* Hero Logo */
.hero-logo {
  width: 48px;
  height: 48px;
}

/* 副标题 */
.hero-subtitle {
  font-size: 16px;
  color: #666;
  margin-bottom: 32px;
}

/* ==================== 输入框样式 ==================== */

/* 输入框外层容器 */
.prompt-input-wrapper {
  max-width: 680px;  /* 最大宽度 */
  margin: 0 auto;    /* 水平居中 */
  background: #fff;
  border-radius: 16px;  /* 圆角 */
  /* 
   * box-shadow: 阴影效果
   * 参数: 水平偏移 垂直偏移 模糊半径 颜色
   */
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  padding: 16px;
}

/* 输入框本身 */
.prompt-input {
  /* 
   * !important: 提高优先级，覆盖组件库的默认样式
   * 通常不推荐使用，但有时需要覆盖第三方组件样式
   */
  border: none !important;
  box-shadow: none !important;
  resize: none;  /* 禁止调整大小 */
  font-size: 15px;
}

/* 输入框获得焦点时的样式 */
.prompt-input:focus {
  box-shadow: none !important;
}

/* 输入框下方的操作区域 */
.input-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;  /* 两端对齐 */
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;  /* 顶部分隔线 */
}

/* 左侧按钮组 */
.input-actions-left {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 右侧按钮组 */
.input-actions-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 发送按钮 */
.send-btn {
  /* 渐变背景 */
  background: linear-gradient(135deg, #52c4a0 0%, #3db389 100%);
  border: none;
}

/* 发送按钮悬停效果 */
.send-btn:hover {
  background: linear-gradient(135deg, #45b894 0%, #35a07a 100%);
}

/* ==================== 快捷标签样式 ==================== */

.quick-tags {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 24px;
  flex-wrap: wrap;  /* 允许换行 */
}

.quick-tag {
  cursor: pointer;  /* 鼠标悬停时显示手型 */
  padding: 6px 16px;
  border-radius: 20px;  /* 圆角（胶囊形状） */
  border: 1px solid #d9d9d9;
  background: #fff;
  /* 
   * transition: 过渡动画
   * all: 所有属性
   * 0.2s: 动画时长
   */
  transition: all 0.2s;
}

/* 标签悬停效果 */
.quick-tag:hover {
  border-color: #52c4a0;
  color: #52c4a0;
}

/* ==================== 区块通用样式 ==================== */

.section {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 24px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.section-title {
  font-size: 24px;
  font-weight: 600;
  margin: 0;
  color: #1a1a1a;
}

/* ==================== 标签筛选样式 ==================== */

/* 标签筛选区域容器 */
.tag-filter-section {
  margin-bottom: 20px;
}

/* 标签筛选按钮样式 */
.tag-filter-section :deep(.ant-btn) {
  border-radius: 16px;  /* 圆角胶囊形状 */
  transition: all 0.2s;
}

/* 标签筛选按钮悬停效果 */
.tag-filter-section :deep(.ant-btn:not(.ant-btn-primary):hover) {
  border-color: #52c4a0;
  color: #52c4a0;
}

/* 选中状态的按钮样式 */
.tag-filter-section :deep(.ant-btn-primary) {
  background: linear-gradient(135deg, #52c4a0 0%, #3db389 100%);
  border: none;
}

.tag-filter-section :deep(.ant-btn-primary:hover) {
  background: linear-gradient(135deg, #45b894 0%, #35a07a 100%);
}

/* ==================== 应用卡片网格样式 ==================== */

.app-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

/* ==================== 响应式设计 ==================== */

/* 
 * @media: 媒体查询
 * 根据屏幕宽度应用不同的样式
 * 实现响应式布局，适配不同设备
 */

/* 平板设备（屏幕宽度 ≤ 992px） */
@media (max-width: 992px) {
  .app-grid {
    grid-template-columns: repeat(2, 1fr);  /* 改为 2 列 */
  }
}

/* 手机设备（屏幕宽度 ≤ 576px） */
@media (max-width: 576px) {
  .title-text {
    font-size: 28px;  /* 减小标题字号 */
  }
  
  .hero-logo {
    width: 36px;
    height: 36px;
  }
  
  .app-grid {
    grid-template-columns: 1fr;  /* 改为 1 列 */
  }
  
  .prompt-input-wrapper {
    margin: 0 16px;
  }
  
  /* 标签筛选响应式：小屏幕上按钮更紧凑 */
  .tag-filter-section :deep(.ant-btn) {
    padding: 0 10px;
    font-size: 12px;
  }
}
</style>
