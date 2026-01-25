<!--
  AppChatPage.vue - 应用生成对话页
  
  【页面功能】
  这是应用的核心页面，用户在这里与 AI 对话生成网站代码：
  1. 顶部栏：显示应用名称，提供部署按钮
  2. 左侧对话区：用户与 AI 的对话消息列表 + 输入框
  3. 右侧预览区：实时展示生成的网站效果
  
  【核心技术点】
  1. SSE (Server-Sent Events): 服务器推送事件，实现流式输出
  2. iframe: 内嵌框架，用于预览生成的网站
  3. 路由参数: 从 URL 获取应用 ID
  4. 响应式布局: 适配不同屏幕尺寸
  
  【学习要点】
  1. SSE 流式数据处理
  2. Vue 3 watch 监听器
  3. DOM 操作 (scrollToBottom)
  4. 路由参数获取
-->
<template>
  <!-- 
    页面根容器
    使用 flex 布局，垂直方向排列
  -->
  <div class="app-chat-page">
    
    <!-- ==================== 顶部栏 ==================== -->
    <!-- 
      顶部栏包含：
      - 左侧：应用名称（可下拉操作）
      - 右侧：部署按钮
    -->
    <div class="top-bar">
      <!-- 左侧区域 -->
      <div class="top-left">
        <!-- 
          a-dropdown: Ant Design Vue 的下拉菜单组件
          点击触发区域会显示下拉菜单
        -->
        <a-dropdown>
          <!-- 
            下拉菜单的触发区域
            显示 Logo + 应用名称 + 下拉箭头
          -->
          <div class="app-name-wrapper">
            <img src="@/assets/logo.png" alt="Logo" class="app-logo" />
            <!-- 
              ?. 可选链操作符
              如果 appInfo 为 null/undefined，不会报错，返回 undefined
              || 短路求值：如果左边为假值，返回右边的值
            -->
            <span class="app-name">{{ appInfo?.appName || '未命名应用' }}</span>
            <DownOutlined />
          </div>
          
          <!-- 
            template #overlay: 具名插槽
            定义下拉菜单的内容
          -->
          <template #overlay>
            <a-menu>
              <a-menu-item @click="goToEdit">
                <EditOutlined /> 编辑应用
              </a-menu-item>
              <a-menu-item @click="goBack">
                <HomeOutlined /> 返回首页
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </div>
      
      <!-- 右侧区域：下载代码 + 部署按钮 -->
      <div class="top-right">
        <a-button 
          :loading="downloading"
          @click="handleDownload"
        >
          <template #icon><DownloadOutlined /></template>
          下载代码
        </a-button>
        <a-button 
          type="primary" 
          :loading="deploying"
          @click="handleDeploy"
        >
          <template #icon><CloudUploadOutlined /></template>
          部署
        </a-button>
      </div>
    </div>

    <!-- ==================== 主内容区 ==================== -->
    <!-- 
      主内容区使用 flex 布局
      左侧对话区 + 右侧预览区
    -->
    <div class="main-content">
      
      <!-- ========== 左侧对话区 ========== -->
      <div class="chat-panel">
        
        <!-- 消息列表区域 -->
        <!-- 
          ref="messageListRef": 模板引用
          可以在 script 中通过 messageListRef.value 访问这个 DOM 元素
          用于实现滚动到底部功能
        -->
        <div ref="messageListRef" class="message-list">
          <!-- 加载更多按钮 -->
          <div v-if="hasMore" class="load-more-wrapper">
            <a-button 
              type="link" 
              :loading="loadingHistory"
              @click="loadChatHistory(true)"
            >
              <template #icon><LoadingOutlined v-if="loadingHistory" /></template>
              {{ loadingHistory ? '加载中...' : '加载更多历史消息' }}
            </a-button>
          </div>
          
          <!-- 
            遍历消息列表
            :class: 动态绑定 class
            - 数组语法: ['固定类名', 动态类名]
            - msg.role 的值会作为类名添加（'user' 或 'ai'）
          -->
          <div 
            v-for="(msg, index) in messages" 
            :key="index" 
            :class="['message-item', msg.role]"
          >
            <div class="message-content">
              <!-- 
                用户消息：显示在右侧
                v-if: 条件渲染
              -->
              <template v-if="msg.role === 'user'">
                <div class="message-text user-message">
                  {{ msg.content }}
                </div>
              </template>
              
              <!-- AI 消息：显示在左侧，带头像 -->
              <template v-else>
                <div class="message-text ai-message">
                  <div class="ai-avatar">
                    <img src="@/assets/logo.png" alt="AI" />
                  </div>
                  <div class="ai-content">
                    <!-- 
                      v-html: 渲染 HTML 内容
                      【安全警告】v-html 可能导致 XSS 攻击
                      只能用于可信的内容，不要用于用户输入
                      这里用于渲染 Markdown 转换后的 HTML
                    -->
                    <div class="markdown-content" v-html="renderMarkdown(msg.content)"></div>
                    <!-- 
                      打字光标效果
                      当消息正在加载时显示闪烁的光标
                    -->
                    <span v-if="msg.loading" class="typing-cursor">|</span>
                  </div>
                </div>
              </template>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="input-area">
          <!-- 
            消息输入框
            :disabled: 生成中或非所有者时禁用输入
            @pressEnter.prevent: 
            - @pressEnter: 按回车键触发
            - .prevent: 事件修饰符，阻止默认行为（防止换行）
          -->
          <a-tooltip :title="!isOwner ? '无法在别人的作品下对话哦~' : ''" placement="top">
            <a-textarea
              v-model:value="inputText"
              :auto-size="{ minRows: 1, maxRows: 4 }"
              placeholder="描述越详细，页面越具体，可以一步一步完善生成结果..."
              class="chat-input"
              :disabled="isGenerating || !isOwner"
              @pressEnter.prevent="handleSend"
            />
          </a-tooltip>
          
          <!-- 输入框下方的操作按钮 -->
          <div class="input-actions">
            <a-button type="text" disabled>
              <template #icon><UploadOutlined /></template>
              上传
            </a-button>
            <a-button type="text" disabled>
              <template #icon><EditOutlined /></template>
              编辑
            </a-button>
            <a-button type="text" disabled>
              <template #icon><ThunderboltOutlined /></template>
              优化
            </a-button>
            <a-button 
              type="primary" 
              shape="circle" 
              class="send-btn"
              :loading="isGenerating"
              :disabled="!isOwner"
              @click="handleSend"
            >
              <template #icon><SendOutlined /></template>
            </a-button>
          </div>
        </div>
      </div>

      <!-- ========== 右侧预览区 ========== -->
      <div class="preview-panel">
        <!-- 预览区头部 -->
        <div class="preview-header">
          <span>生成后的网页展示</span>
          <!-- 
            v-if: 只有当有预览 URL 时才显示"新窗口打开"按钮
          -->
          <a-button 
            v-if="previewUrl" 
            type="link" 
            size="small"
            @click="openPreviewInNewTab"
          >
            <ExportOutlined /> 新窗口打开
          </a-button>
        </div>
        
        <!-- 预览内容区 -->
        <div class="preview-content">
          <!-- 
            iframe: 内嵌框架
            用于在当前页面中嵌入另一个网页
            
            v-if: 有预览 URL 且不在生成中时显示
            :src: 动态绑定 iframe 的源地址
            :key: 强制刷新 iframe
            - 当 key 变化时，Vue 会销毁旧元素，创建新元素
            - 用于在生成完成后刷新预览
          -->
          <iframe 
            v-if="previewUrl && !isGenerating" 
            :src="previewUrl" 
            class="preview-iframe"
            :key="iframeKey"
          />
          
          <!-- 生成中的加载状态 -->
          <div v-else-if="isGenerating" class="preview-loading">
            <a-spin size="large" />
            <p>正在生成中...</p>
          </div>
          
          <!-- 空状态：没有预览 URL 或历史记录不足 -->
          <div v-else class="preview-empty">
            <FileTextOutlined class="empty-icon" />
            <p v-if="messages.length < 2">发送消息后，生成的网页将在这里展示</p>
            <p v-else>加载预览中...</p>
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== 部署成功弹窗 ==================== -->
    <!-- 
      a-modal: Ant Design Vue 的模态框组件
      v-model:open: 双向绑定显示状态
      :footer="null": 不显示默认的底部按钮
    -->
    <a-modal
      v-model:open="deployModalVisible"
      title="部署成功"
      :footer="null"
    >
      <div class="deploy-result">
        <CheckCircleOutlined class="success-icon" />
        <p>您的应用已成功部署！</p>
        
        <!-- 部署 URL 显示和复制 -->
        <div class="deploy-url">
          <a-input :value="deployedUrl" readonly />
          <a-button type="primary" @click="copyUrl">复制链接</a-button>
        </div>
        
        <a-button type="link" @click="openDeployedUrl">
          <ExportOutlined /> 访问应用
        </a-button>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
/**
 * ==================== 应用生成对话页脚本 ====================
 * 
 * 【核心功能】
 * 1. 加载应用信息
 * 2. 发送消息并接收 AI 流式响应 (SSE)
 * 3. 预览生成的网站
 * 4. 部署应用
 * 
 * 【SSE (Server-Sent Events) 简介】
 * SSE 是一种服务器向客户端推送数据的技术：
 * - 单向通信：服务器 → 客户端
 * - 基于 HTTP：使用普通的 HTTP 连接
 * - 自动重连：连接断开后会自动重连
 * - 文本格式：数据以文本形式传输
 * 
 * 与 WebSocket 的区别：
 * - SSE 是单向的，WebSocket 是双向的
 * - SSE 更简单，适合服务器推送场景
 * - SSE 基于 HTTP，更容易穿透防火墙
 */

// ==================== 导入部分 ====================

import { ref, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'

/**
 * 导入图标组件
 * 这些图标来自 @ant-design/icons-vue 包
 */
import {
  DownOutlined,         // 下拉箭头
  EditOutlined,         // 编辑图标
  HomeOutlined,         // 首页图标
  CloudUploadOutlined,  // 云上传图标（部署）
  DownloadOutlined,     // 下载图标
  UploadOutlined,       // 上传图标
  ThunderboltOutlined,  // 闪电图标（优化）
  SendOutlined,         // 发送图标
  ExportOutlined,       // 导出/外链图标
  FileTextOutlined,     // 文件图标
  CheckCircleOutlined,  // 成功勾选图标
  LoadingOutlined       // 加载图标
} from '@ant-design/icons-vue'

/**
 * 导入 API 接口
 */
import { getAppVoById, deployApp } from '@/api/appController'
import { listChatHistoryByAppId } from '@/api/chatHistoryController'

/**
 * 导入环境变量配置
 * API_BASE_URL: API 基础地址
 * getStaticPreviewUrl: 获取静态资源预览 URL
 */
import { API_BASE_URL, getStaticPreviewUrl } from '@/config/env'

/**
 * 导入 Pinia Store
 * 用于获取当前登录用户信息
 */
import { useLoginUserStore } from '@/stores/loginUser'

/**
 * 导入 Markdown 渲染工具
 */
import { renderMarkdown } from '@/utils/markdown'

/**
 * 导入 Markdown 样式
 */
import '@/styles/markdown.css'

// ==================== 路由相关 ====================

/**
 * useRoute: 获取当前路由信息
 * 可以访问路由参数、查询参数等
 * 
 * useRouter: 获取路由实例
 * 用于编程式导航
 */
const route = useRoute()
const router = useRouter()

/**
 * 获取登录用户 Store 实例
 */
const loginUserStore = useLoginUserStore()

// ==================== 响应式数据 ====================

/**
 * 应用 ID
 * 从路由参数获取，始终作为字符串处理
 * 
 * 【为什么是字符串？】
 * 后端的 ID 是 Long 类型（64位整数）
 * JavaScript 的 Number 只能精确表示 53 位
 * 为避免精度丢失，我们将 ID 作为字符串处理
 */
const appId = ref<string>('')

/**
 * 应用信息
 * 从后端获取的应用详情
 */
const appInfo = ref<API.AppVO | null>(null)

/**
 * 消息接口定义
 * 
 * TypeScript interface: 定义对象的结构
 * 用于类型检查，确保数据格式正确
 */
interface Message {
  role: 'user' | 'ai'  // 消息角色：用户或 AI
  content: string       // 消息内容
  loading?: boolean     // 是否正在加载（AI 消息专用）
}

/**
 * 消息列表
 */
const messages = ref<Message[]>([])

/**
 * 消息列表 DOM 元素的引用
 * 
 * ref<HTMLElement | null>(null):
 * - 初始值为 null
 * - 组件挂载后，Vue 会自动将 DOM 元素赋值给它
 * - 用于操作 DOM（如滚动到底部）
 */
const messageListRef = ref<HTMLElement | null>(null)

/**
 * 输入框内容
 */
const inputText = ref('')

/**
 * 是否正在生成中
 * 用于控制：
 * - 输入框禁用状态
 * - 发送按钮加载状态
 * - 预览区显示状态
 */
const isGenerating = ref(false)

/**
 * 预览相关
 */
const previewUrl = ref('')   // 预览 URL
const iframeKey = ref(0)     // iframe 的 key，用于强制刷新

/**
 * 部署相关
 */
const deploying = ref(false)           // 是否正在部署
const deployModalVisible = ref(false)  // 部署成功弹窗是否显示
const deployedUrl = ref('')            // 部署后的 URL

/**
 * 下载相关
 */
const downloading = ref(false)         // 是否正在下载

/**
 * 是否为应用所有者
 * 只有所有者才能在对话页发送消息
 */
const isOwner = ref(true)

// ==================== 对话历史相关 ====================

/**
 * 游标 ID，用于分页加载历史消息
 * undefined 表示加载最新的消息
 */
const lastId = ref<number | undefined>(undefined)

/**
 * 是否还有更多历史消息可加载
 */
const hasMore = ref(false)

/**
 * 是否正在加载历史消息
 */
const loadingHistory = ref(false)

/**
 * 历史记录总数，用于判断是否显示预览
 */
const historyCount = ref(0)

/**
 * 对话历史是否已加载完成
 * 用于确保在历史加载完成后才判断是否自动发送
 */
const historyLoaded = ref(false)

/**
 * 将 ChatHistoryVO 转换为 Message 格式
 * @param history - 后端返回的对话历史记录
 * @returns Message 格式的消息对象
 */
const convertToMessage = (history: API.ChatHistoryVO): Message => {
  return {
    role: history.messageType === 'ai' ? 'ai' : 'user',
    content: history.message || '',
    loading: false
  }
}

/**
 * 加载对话历史
 * 
 * @param isLoadMore - 是否为加载更多（true: 加载更早的消息，false: 初始加载）
 */
const loadChatHistory = async (isLoadMore = false) => {
  if (!appId.value || loadingHistory.value) return
  
  loadingHistory.value = true
  
  try {
    // 使用 as any 绕过类型检查，保持 appId 为字符串格式避免精度丢失
    const res = await listChatHistoryByAppId({
      appId: appId.value as any,
      lastId: isLoadMore ? lastId.value : undefined,
      size: 10
    })
    
    if (res.data.code === 0 && res.data.data) {
      const records = res.data.data.records || []
      const totalRow = res.data.data.totalRow || 0
      
      // 更新历史记录总数
      if (!isLoadMore) {
        historyCount.value = totalRow
      }
      
      if (records.length > 0) {
        // 将 ChatHistoryVO 转换为 Message 格式
        // API 返回的是按时间降序（最新的在前），需要反转为升序
        const newMessages = records.map(convertToMessage).reverse()
        
        if (isLoadMore) {
          // 加载更多：保存当前滚动位置
          const scrollContainer = messageListRef.value
          const previousScrollHeight = scrollContainer?.scrollHeight || 0
          
          // 将更早的消息添加到列表开头
          messages.value = [...newMessages, ...messages.value]
          
          // 恢复滚动位置
          nextTick(() => {
            if (scrollContainer) {
              const newScrollHeight = scrollContainer.scrollHeight
              scrollContainer.scrollTop = newScrollHeight - previousScrollHeight
            }
          })
        } else {
          // 初始加载：直接设置消息列表
          messages.value = newMessages
          // 滚动到底部
          scrollToBottom()
        }
        
        // 更新游标：使用最早消息的 ID（records 反转前的最后一条）
        lastId.value = records[records.length - 1].id
        
        // 判断是否还有更多历史消息
        // 如果当前加载的消息数量等于请求的数量，可能还有更多
        hasMore.value = records.length === 10
      } else {
        hasMore.value = false
      }
    }
  } catch (error) {
    console.error('加载对话历史失败：', error)
    message.error('加载对话历史失败')
  } finally {
    loadingHistory.value = false
    // 标记历史加载完成
    historyLoaded.value = true
  }
}

// ==================== 方法定义 ====================

/**
 * 获取应用信息
 * 
 * 页面加载时调用，获取应用的详细信息
 * 先加载对话历史，再判断是否自动发送初始消息
 */
const loadAppInfo = async () => {
  if (!appId.value) return
  
  try {
    /**
     * 调用后端接口获取应用信息
     * 
     * { id: appId.value as any }:
     * - as any: TypeScript 类型断言
     * - 因为后端期望 number 类型，但我们传的是 string
     * - 实际上后端会自动转换，这里用 as any 绕过类型检查
     */
    const res = await getAppVoById({ id: appId.value as any })
    
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data
      
      /**
       * 权限校验：判断当前用户是否为应用所有者
       * 只有所有者才能发送消息
       */
      const currentUserId = loginUserStore.loginUser.id
      const appUserId = appInfo.value.userId
      isOwner.value = !!(currentUserId && appUserId && String(currentUserId) === String(appUserId))
      
      /**
       * 先加载对话历史
       */
      await loadChatHistory()
      
      /**
       * 根据前端消息列表数量决定是否显示预览
       * 如果有至少 2 条对话记录，显示网站预览
       */
      if (messages.value.length >= 2 && appInfo.value.codeGenType) {
        previewUrl.value = getStaticPreviewUrl(appInfo.value.codeGenType, appId.value)
        iframeKey.value++
      }
      
      /**
       * 自动发送初始消息的条件：
       * 1. 应用有初始提示词
       * 2. 用户是应用所有者
       * 3. 历史已加载完成（historyLoaded === true）
       * 4. 前端消息列表为空（messages.value.length === 0）
       */
      if (appInfo.value.initPrompt && isOwner.value && historyLoaded.value && messages.value.length === 0) {
        inputText.value = appInfo.value.initPrompt
        await handleSend()
      }
    } else {
      message.error('获取应用信息失败')
    }
  } catch (error) {
    message.error('获取应用信息失败')
  }
}

/**
 * 发送消息
 * 
 * 【核心流程】
 * 1. 验证输入
 * 2. 添加用户消息到列表
 * 3. 添加 AI 消息占位（显示加载状态）
 * 4. 建立 SSE 连接
 * 5. 接收流式数据，实时更新 AI 消息
 * 6. 连接关闭后，更新预览
 */
const handleSend = async () => {
  const text = inputText.value.trim()
  
  // 验证：内容不能为空，且不能在生成中重复发送
  if (!text || isGenerating.value) return

  // 1. 添加用户消息
  messages.value.push({ role: 'user', content: text })
  
  // 清空输入框
  inputText.value = ''
  
  // 2. 添加 AI 消息占位
  // 记录索引，后续用于更新这条消息的内容
  const aiMessageIndex = messages.value.length
  messages.value.push({ role: 'ai', content: '', loading: true })
  
  // 3. 设置生成状态
  isGenerating.value = true
  
  // 滚动到底部，显示最新消息
  scrollToBottom()

  try {
    /**
     * ==================== SSE 流式请求 ====================
     * 
     * 【EventSource 简介】
     * EventSource 是浏览器原生的 SSE 客户端 API
     * 用于接收服务器推送的事件流
     * 
     * 【使用方法】
     * 1. 创建 EventSource 实例，传入 URL
     * 2. 监听 onmessage 事件，接收数据
     * 3. 监听 onerror 事件，处理错误
     * 4. 监听自定义事件（如 'done'）
     * 5. 调用 close() 关闭连接
     */
    
    // 构建请求 URL
    // encodeURIComponent: URL 编码，处理特殊字符
    const url = `${API_BASE_URL}/app/chat/gen/code?appId=${appId.value}&message=${encodeURIComponent(text)}`
    
    /**
     * 创建 EventSource 实例
     * 
     * withCredentials: true
     * - 允许跨域请求携带 Cookie
     * - 用于保持登录状态
     */
    const eventSource = new EventSource(url, { withCredentials: true })
    
    /**
     * 流式传输完成标志
     * 用于区分正常关闭和异常错误
     */
    let streamCompleted = false
    
    /**
     * 监听消息事件
     * 
     * 每当服务器发送一条数据，就会触发这个回调
     * event.data 包含服务器发送的数据
     */
    eventSource.onmessage = (event) => {
      try {
        /**
         * 解析服务器返回的 JSON 数据
         * 
         * 后端返回格式: { "d": "内容片段" }
         * d 字段包含 AI 生成的内容片段
         */
        const data = JSON.parse(event.data)
        
        if (data && data.d) {
          // 将内容片段追加到 AI 消息中
          messages.value[aiMessageIndex].content += data.d
          // 滚动到底部
          scrollToBottom()
        }
      } catch (e) {
        // 如果解析失败，尝试直接使用原始数据
        if (event.data) {
          messages.value[aiMessageIndex].content += event.data
          scrollToBottom()
        }
      }
    }

    /**
     * 监听错误事件
     * 
     * 【重要】onerror 会在以下情况触发：
     * 1. 网络错误
     * 2. 服务器错误
     * 3. 正常的连接关闭（服务器发送完数据后关闭连接）
     * 
     * 所以需要区分"正常关闭"和"真正的错误"
     * 
     * EventSource.readyState 的值：
     * - 0 (CONNECTING): 正在连接或正在重连
     * - 1 (OPEN): 连接已建立
     * - 2 (CLOSED): 连接已关闭
     */
    eventSource.onerror = () => {
      // 如果已经标记为完成，或者不在生成状态，直接返回
      // 避免重复处理
      if (streamCompleted || !isGenerating.value) return
      
      /**
       * 判断是正常关闭还是真正的错误
       * 
       * 当 readyState === EventSource.CONNECTING (0) 时：
       * - 通常表示服务器正常关闭了连接
       * - SSE 会尝试重连，但我们不需要重连
       * - 按正常流程处理即可
       * 
       * 其他情况（如 readyState === CLOSED 且有错误）：
       * - 可能是网络错误、服务器错误等
       * - 需要显示错误信息
       */
      if (eventSource.readyState === EventSource.CONNECTING) {
        // 正常关闭：服务器发送完数据后关闭连接
        streamCompleted = true
        isGenerating.value = false
        messages.value[aiMessageIndex].loading = false
        eventSource.close()
        
        // 延迟更新预览，确保后端文件已写入完成
        setTimeout(async () => {
          // 重新获取应用信息（可能有新的 codeGenType），但不重新加载对话历史
          const res = await getAppVoById({ id: appId.value as any })
          if (res.data.code === 0 && res.data.data) {
            appInfo.value = res.data.data
          }
          updatePreview()
        }, 1000)
      } else {
        // 真正的错误：显示错误信息
        handleStreamError(new Error('SSE 连接错误'), aiMessageIndex)
        eventSource.close()
      }
    }

    /**
     * 监听自定义事件 'done'
     * 
     * 后端在数据发送完毕后，会发送一个 'done' 事件
     * 用于明确告知客户端传输已完成
     * 
     * 这是最可靠的完成信号，优先使用
     */
    eventSource.addEventListener('done', () => {
      streamCompleted = true
      eventSource.close()
      messages.value[aiMessageIndex].loading = false
      isGenerating.value = false
      
      // 延迟更新预览，但不重新加载对话历史
      setTimeout(async () => {
        // 重新获取应用信息（可能有新的 codeGenType），但不重新加载对话历史
        const res = await getAppVoById({ id: appId.value as any })
        if (res.data.code === 0 && res.data.data) {
          appInfo.value = res.data.data
        }
        updatePreview()
      }, 1000)
    })

  } catch (error) {
    // 发生异常时的处理（如创建 EventSource 失败）
    handleStreamError(error, aiMessageIndex)
  }
}

/**
 * 流式传输错误处理函数
 * 
 * 统一处理 SSE 连接过程中的错误：
 * 1. 清理状态
 * 2. 显示友好的错误信息
 * 3. 重置生成状态
 * 
 * @param error - 错误对象
 * @param aiMessageIndex - AI 消息在数组中的索引
 */
const handleStreamError = (error: unknown, aiMessageIndex: number) => {
  // 打印错误到控制台，方便调试
  console.error('生成代码失败：', error)
  
  // 更新 AI 消息内容为错误提示
  messages.value[aiMessageIndex].content = '抱歉，生成过程中出现了错误，请重试。'
  
  // 移除加载状态
  messages.value[aiMessageIndex].loading = false
  
  // 显示错误提示
  message.error('生成失败，请重试')
  
  // 重置生成状态
  isGenerating.value = false
}

/**
 * 更新预览
 * 
 * 生成完成后，构建预览 URL 并刷新 iframe
 */
const updatePreview = () => {
  /**
   * 预览 URL 格式:
   * {API_BASE_URL}/static/{codeGenType}_{appId}/
   * 
   * codeGenType: 代码生成类型（如 'react', 'vue' 等）
   * appId: 应用 ID
   */
  if (appInfo.value?.codeGenType && appId.value) {
    previewUrl.value = getStaticPreviewUrl(appInfo.value.codeGenType, appId.value)
    // 增加 key 值，强制 iframe 刷新
    iframeKey.value++
  } else if (appId.value) {
    // 如果没有 codeGenType，尝试重新获取应用信息
    getAppVoById({ id: appId.value as any }).then(res => {
      if (res.data.code === 0 && res.data.data) {
        appInfo.value = res.data.data
        if (appInfo.value?.codeGenType) {
          previewUrl.value = getStaticPreviewUrl(appInfo.value.codeGenType, appId.value)
          iframeKey.value++
        }
      }
    })
  }
}

/**
 * 滚动到底部
 * 
 * 使消息列表始终显示最新的消息
 */
const scrollToBottom = () => {
  /**
   * nextTick: Vue 的异步更新机制
   * 
   * Vue 的 DOM 更新是异步的，数据变化后不会立即更新 DOM
   * nextTick 会在 DOM 更新完成后执行回调
   * 确保我们操作的是最新的 DOM
   */
  nextTick(() => {
    if (messageListRef.value) {
      // scrollTop: 滚动条距离顶部的距离
      // scrollHeight: 内容的总高度
      // 设置 scrollTop = scrollHeight 即滚动到底部
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

/**
 * 下载应用代码
 * 
 * 调用后端下载接口，获取 ZIP 压缩包
 * 后端返回的是文件流，需要使用 fetch 处理 blob 响应
 */
const handleDownload = async () => {
  if (!appId.value) return
  
  downloading.value = true
  
  try {
    // 构建下载 URL
    const downloadUrl = `${API_BASE_URL}/app/download/${appId.value}`
    
    // 使用 fetch 发送请求，携带 Cookie
    const response = await fetch(downloadUrl, {
      method: 'GET',
      credentials: 'include'  // 携带 Cookie
    })
    
    // 检查响应状态
    if (!response.ok) {
      // 尝试解析错误信息
      const contentType = response.headers.get('Content-Type')
      if (contentType && contentType.includes('application/json')) {
        const errorData = await response.json()
        throw new Error(errorData.message || '下载失败')
      }
      throw new Error(`下载失败：${response.status}`)
    }
    
    // 从响应头获取文件名
    const contentDisposition = response.headers.get('Content-Disposition')
    let fileName = `${appId.value}.zip`
    if (contentDisposition) {
      const match = contentDisposition.match(/filename="?([^"]+)"?/)
      if (match && match[1]) {
        fileName = match[1]
      }
    }
    
    // 获取 blob 数据
    const blob = await response.blob()
    
    // 创建下载链接并触发下载
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = fileName
    document.body.appendChild(link)
    link.click()
    
    // 清理
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    
    message.success('下载成功')
  } catch (error: any) {
    console.error('下载代码失败：', error)
    message.error(error.message || '下载失败，请稍后重试')
  } finally {
    downloading.value = false
  }
}

/**
 * 部署应用
 * 
 * 调用后端部署接口，获取部署后的访问 URL
 */
const handleDeploy = async () => {
  if (!appId.value) return
  
  deploying.value = true
  
  try {
    const res = await deployApp({ appId: appId.value as any })
    
    if (res.data.code === 0 && res.data.data) {
      // 保存部署 URL
      deployedUrl.value = res.data.data
      // 显示成功弹窗
      deployModalVisible.value = true
    } else {
      message.error('部署失败：' + res.data.message)
    }
  } catch (error) {
    message.error('部署失败，请稍后重试')
  } finally {
    deploying.value = false
  }
}

/**
 * 复制 URL 到剪贴板
 * 
 * navigator.clipboard: 浏览器剪贴板 API
 * writeText(): 将文本写入剪贴板
 */
const copyUrl = () => {
  navigator.clipboard.writeText(deployedUrl.value)
  message.success('链接已复制')
}

/**
 * 在新窗口打开部署的应用
 * 
 * window.open(url, target):
 * - url: 要打开的 URL
 * - target: '_blank' 表示在新标签页打开
 */
const openDeployedUrl = () => {
  window.open(deployedUrl.value, '_blank')
}

/**
 * 在新窗口打开预览
 */
const openPreviewInNewTab = () => {
  if (previewUrl.value) {
    window.open(previewUrl.value, '_blank')
  }
}

/**
 * 跳转到编辑页
 */
const goToEdit = () => {
  router.push(`/app/edit/${appId.value}`)
}

/**
 * 返回首页
 */
const goBack = () => {
  router.push('/')
}

// ==================== 生命周期和监听器 ====================

/**
 * 组件挂载后执行
 */
onMounted(() => {
  /**
   * 从路由参数获取应用 ID
   * 
   * route.params: 路由参数对象
   * 路由配置: /app/chat/:id
   * 访问 /app/chat/123 时，route.params.id = '123'
   */
  const id = route.params.id
  
  if (id) {
    // 确保 ID 是字符串
    appId.value = String(id)
    // 加载应用信息
    loadAppInfo()
  }
})

/**
 * 监听路由参数变化
 * 
 * watch: Vue 3 的监听器
 * 当监听的数据变化时，执行回调函数
 * 
 * 语法: watch(source, callback)
 * - source: 要监听的数据（可以是 ref、reactive、getter 函数等）
 * - callback: 数据变化时执行的函数
 * 
 * 【为什么需要监听？】
 * 当用户在对话页之间切换时（如从应用A切换到应用B）
 * 组件不会重新创建，只是路由参数变化
 * 所以需要监听参数变化，重新加载数据
 */
watch(() => route.params.id, (newId) => {
  if (newId) {
    appId.value = String(newId)
    // 重置状态
    messages.value = []
    previewUrl.value = ''
    isOwner.value = true
    lastId.value = undefined
    hasMore.value = false
    historyCount.value = 0
    historyLoaded.value = false  // 重置历史加载标志
    // 重新加载应用信息
    loadAppInfo()
  }
})
</script>

<style scoped>
/**
 * ==================== 对话页样式 ====================
 * 
 * 【布局结构】
 * .app-chat-page (flex, column)
 *   ├── .top-bar (顶部栏)
 *   └── .main-content (flex, row)
 *         ├── .chat-panel (左侧对话区, 45%)
 *         │     ├── .message-list (消息列表, flex: 1)
 *         │     └── .input-area (输入区)
 *         └── .preview-panel (右侧预览区, flex: 1)
 */

/* 页面根容器 */
.app-chat-page {
  display: flex;
  flex-direction: column;  /* 垂直排列 */
  /* 
   * 高度计算：视口高度 - 头部高度
   * 因为这个页面使用 hideLayout，没有全局头部
   * 但保留了自己的顶部栏
   */
  height: calc(100vh - 64px);
  background: #f5f7fa;
}

/* ==================== 顶部栏 ==================== */

.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
}

/* 应用名称区域 */
.app-name-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 8px;
  transition: background 0.2s;
}

.app-name-wrapper:hover {
  background: #f5f5f5;
}

/* 右侧按钮区域 */
.top-right {
  display: flex;
  gap: 12px;
}

.app-logo {
  width: 28px;
  height: 28px;
}

.app-name {
  font-size: 16px;
  font-weight: 500;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ==================== 主内容区 ==================== */

.main-content {
  display: flex;
  flex: 1;  /* 占据剩余空间 */
  overflow: hidden;  /* 防止内容溢出 */
}

/* ==================== 对话面板 ==================== */

.chat-panel {
  width: 45%;
  min-width: 400px;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-right: 1px solid #e8e8e8;
}

/* 消息列表 */
.message-list {
  flex: 1;
  overflow-y: auto;  /* 垂直滚动 */
  padding: 24px;
}

/* 加载更多按钮 */
.load-more-wrapper {
  text-align: center;
  margin-bottom: 16px;
}

.load-more-wrapper .ant-btn-link {
  color: #999;
}

.load-more-wrapper .ant-btn-link:hover {
  color: #52c4a0;
}

/* 单条消息 */
.message-item {
  margin-bottom: 24px;
}

/* 用户消息：右对齐 */
.message-item.user {
  display: flex;
  justify-content: flex-end;
}

/* 用户消息气泡 */
.user-message {
  background: #f0f0f0;
  padding: 12px 16px;
  /* 
   * border-radius: 四个角的圆角
   * 顺序: 左上 右上 右下 左下
   * 用户消息右下角是尖角
   */
  border-radius: 12px 12px 4px 12px;
  max-width: 80%;
  word-break: break-word;  /* 长单词换行 */
}

/* AI 消息容器 */
.ai-message {
  display: flex;
  gap: 12px;
}

/* AI 头像 */
.ai-avatar {
  width: 32px;
  height: 32px;
  flex-shrink: 0;  /* 不缩小 */
}

.ai-avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;  /* 圆形 */
}

/* AI 消息内容 */
.ai-content {
  flex: 1;
  line-height: 1.6;
}

/* 打字光标动画 */
.typing-cursor {
  animation: blink 1s infinite;
}

/* 
 * @keyframes: 定义动画关键帧
 * blink: 动画名称
 * 0%, 50%: 时间点（百分比）
 */
@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

/* ==================== 输入区 ==================== */

.input-area {
  padding: 16px 24px;
  border-top: 1px solid #e8e8e8;
  background: #fafafa;
}

.chat-input {
  border: 1px solid #e8e8e8;
  border-radius: 12px;
  padding: 12px 16px;
  font-size: 14px;
  resize: none;
}

/* 输入框获得焦点时的样式 */
.chat-input:focus {
  border-color: #52c4a0;
  /* 
   * box-shadow: 阴影
   * 0 0 0 2px: 无偏移，无模糊，2px 扩展
   * 创建一个 2px 的边框效果
   */
  box-shadow: 0 0 0 2px rgba(82, 196, 160, 0.1);
}

.input-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
}

.send-btn {
  background: linear-gradient(135deg, #52c4a0 0%, #3db389 100%);
  border: none;
}

/* ==================== 预览面板 ==================== */

.preview-panel {
  flex: 1;  /* 占据剩余空间 */
  display: flex;
  flex-direction: column;
  background: #fff;
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  border-bottom: 1px solid #e8e8e8;
  font-weight: 500;
}

.preview-content {
  flex: 1;
  position: relative;  /* 为绝对定位的子元素提供参考 */
}

/* iframe 样式 */
.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
}

/* 加载状态和空状态 */
.preview-loading,
.preview-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  color: #d9d9d9;
}

/* ==================== 部署弹窗 ==================== */

.deploy-result {
  text-align: center;
  padding: 24px 0;
}

.success-icon {
  font-size: 64px;
  color: #52c41a;  /* 绿色 */
  margin-bottom: 16px;
}

.deploy-url {
  display: flex;
  gap: 8px;
  margin: 16px 0;
}

.deploy-url .ant-input {
  flex: 1;
}

/* ==================== 响应式设计 ==================== */

/* 平板设备：改为上下布局 */
@media (max-width: 992px) {
  .main-content {
    flex-direction: column;
  }
  
  .chat-panel {
    width: 100%;
    min-width: auto;
    height: 50%;
    border-right: none;
    border-bottom: 1px solid #e8e8e8;
  }
  
  .preview-panel {
    height: 50%;
  }
}
</style>
