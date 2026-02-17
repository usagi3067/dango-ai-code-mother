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
            <!-- 生成类型标签 -->
            <a-tag v-if="appInfo?.codeGenType" color="processing" class="code-gen-type-tag">
              {{ getCodeGenTypeLabel(appInfo.codeGenType) }}
            </a-tag>
            <DownOutlined />
          </div>
          
          <!-- 
            template #overlay: 具名插槽
            定义下拉菜单的内容
          -->
          <template #overlay>
            <a-menu>
              <a-menu-item @click="showAppDetail">
                <InfoCircleOutlined /> 应用详情
              </a-menu-item>
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
      
      <!-- 右侧区域：数据库 + 下载代码 + 部署按钮 -->
      <div class="top-right">
        <!-- 数据库按钮 -->
        <a-tooltip v-if="appInfo?.codeGenType !== 'vue_project'" title="仅支持 Vue 项目">
          <a-button disabled>
            <template #icon><DatabaseOutlined /></template>
            数据库
          </a-button>
        </a-tooltip>
        <a-button
          v-else-if="!appInfo?.hasDatabase"
          :loading="databaseInitializing"
          @click="handleInitDatabase"
        >
          <template #icon><DatabaseOutlined /></template>
          新建 Database
        </a-button>
        <a-badge v-else dot color="green">
          <a-button @click="databaseDrawerVisible = true">
            <template #icon><DatabaseOutlined /></template>
            数据库
          </a-button>
        </a-badge>
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
          
          @scroll="handleScroll": 监听滚动事件
          用于检测用户是否主动滚动离开底部
        -->
        <div ref="messageListRef" class="message-list" @scroll="handleScroll">
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
              <!-- 用户消息：显示在右侧，带头像 -->
              <template v-if="msg.role === 'user'">
                <div class="message-text user-message">
                  <div class="user-bubble">{{ msg.content }}</div>
                  <UserAvatar
                    :src="loginUserStore.loginUser.userAvatar"
                    :name="loginUserStore.loginUser.userName"
                    :size="32"
                  />
                </div>
              </template>

              <!-- AI 消息：显示在左侧，带头像 -->
              <template v-else>
                <div class="message-text ai-message">
                  <div class="ai-avatar">AI</div>
                  <div class="ai-content">
                    <div class="markdown-content" v-html="renderMarkdown(msg.content)"></div>
                    <span v-if="msg.loading" class="typing-cursor">|</span>
                    <span v-if="isThinking" class="thinking-indicator">
                      {{ thinkingTime }}·thinking
                    </span>
                  </div>
                </div>
              </template>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="input-area">
          <!-- 选中元素信息展示 -->
          <a-alert
            v-if="selectedElementInfo"
            class="selected-element-alert"
            type="info"
            closable
            @close="clearSelectedElement"
          >
            <template #message>
              <div class="selected-element-info">
                <div class="element-header">
                  <span class="element-tag">选中元素：{{ selectedElementInfo.tagName.toLowerCase() }}</span>
                  <span v-if="selectedElementInfo.id" class="element-id">#{{ selectedElementInfo.id }}</span>
                  <span v-if="selectedElementInfo.className" class="element-class">.{{ selectedElementInfo.className.split(' ').filter(c => c && !c.startsWith('edit-')).join('.') }}</span>
                </div>
                <div class="element-details">
                  <div v-if="selectedElementInfo.textContent" class="element-item">
                    内容: {{ selectedElementInfo.textContent.substring(0, 50) }}{{ selectedElementInfo.textContent.length > 50 ? '...' : '' }}
                  </div>
                  <div v-if="selectedElementInfo.pagePath" class="element-item">
                    页面路径: {{ selectedElementInfo.pagePath }}
                  </div>
                  <div class="element-item">
                    选择器: <code class="element-selector-code">{{ selectedElementInfo.selector }}</code>
                  </div>
                </div>
              </div>
            </template>
          </a-alert>

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
              :placeholder="getInputPlaceholder()"
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
            <!-- 编辑模式按钮 -->
            <a-button 
              v-if="isOwner && previewUrl"
              :type="isEditMode ? 'primary' : 'text'"
              :danger="isEditMode"
              @click="toggleEditMode"
              :class="{ 'edit-mode-btn-active': isEditMode }"
            >
              <template #icon><AimOutlined /></template>
              {{ isEditMode ? '退出编辑' : '编辑模式' }}
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
            @load: iframe 加载完成时触发，用于初始化可视化编辑器
          -->
          <iframe 
            v-if="previewUrl && !isGenerating" 
            :src="previewUrl" 
            class="preview-iframe"
            :key="iframeKey"
            @load="onIframeLoad"
          />
          
          <!-- 生成中的加载状态 -->
          <div v-else-if="isGenerating" class="preview-loading">
            <a-spin size="large" />
            <!-- 根据是否为修改模式显示不同的提示信息 -->
            <p v-if="isModifyModeGeneration">正在修改中...</p>
            <p v-else>正在生成中...</p>
            <p v-if="isModifyModeGeneration" class="preview-loading-hint">修改模式：快速定向修改选中元素</p>
            <p v-else class="preview-loading-hint">创建模式：收集素材、智能路由、生成代码</p>
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

    <!-- ==================== 应用详情弹窗 ==================== -->
    <a-modal
      v-model:open="appDetailModalVisible"
      title="应用详情"
      :footer="null"
      width="500px"
    >
      <div class="app-detail-content">
        <a-descriptions :column="1" bordered size="small">
          <a-descriptions-item label="应用名称">
            {{ appInfo?.appName || '未命名应用' }}
          </a-descriptions-item>
          <a-descriptions-item label="生成类型">
            <a-tag v-if="appInfo?.codeGenType" color="processing">
              {{ getCodeGenTypeLabel(appInfo.codeGenType) }}
            </a-tag>
            <span v-else class="text-muted">未生成</span>
          </a-descriptions-item>
          <a-descriptions-item label="应用标签">
            <a-tag v-if="appInfo?.tag" color="blue">{{ appInfo.tag }}</a-tag>
            <span v-else class="text-muted">无</span>
          </a-descriptions-item>
          <a-descriptions-item label="创建时间">
            {{ appInfo?.createTime || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="更新时间">
            {{ appInfo?.updateTime || '-' }}
          </a-descriptions-item>
          <a-descriptions-item v-if="appInfo?.deployedTime" label="部署时间">
            {{ appInfo.deployedTime }}
          </a-descriptions-item>
        </a-descriptions>
      </div>
    </a-modal>

    <!-- ==================== 数据库管理 Drawer ==================== -->
    <a-drawer
      v-model:open="databaseDrawerVisible"
      title="数据库管理"
      placement="right"
      :width="720"
      :destroyOnClose="true"
    >
      <div class="database-drawer-content">
        <!-- 状态栏 -->
        <div class="database-status">
          <a-descriptions :column="2" size="small">
            <a-descriptions-item label="Schema">
              app_{{ appId }}
            </a-descriptions-item>
            <a-descriptions-item label="状态">
              <a-badge status="success" text="已启用" />
            </a-descriptions-item>
          </a-descriptions>
        </div>

        <!-- Platform Kit iframe -->
        <div class="database-iframe-wrapper">
          <iframe
            :src="supabaseManagerUrl"
            class="database-iframe"
            frameborder="0"
          />
        </div>

        <!-- 底部提示 -->
        <div class="database-tips">
          <a-typography-text type="secondary">
            通过对话管理数据库："帮我新建一个用户表"、"给 todos 表添加 priority 字段"
          </a-typography-text>
        </div>
      </div>
    </a-drawer>
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

import { ref, computed, onMounted, nextTick, watch } from 'vue'
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
  LoadingOutlined,      // 加载图标
  InfoCircleOutlined,   // 信息图标
  AimOutlined,          // 瞄准图标（编辑模式）
  DatabaseOutlined      // 数据库图标
} from '@ant-design/icons-vue'

/**
 * 导入 API 接口
 */
import { getAppVoById, deployApp } from '@/api/app/appController'
import { listChatHistoryByAppId } from '@/api/app/chatHistoryController'
import { initializeDatabase } from '@/api/app/databaseController'

/**
 * 导入环境变量配置
 * API_BASE_URL: API 基础地址
 * getStaticPreviewUrl: 获取静态资源预览 URL
 */
import { API_BASE_URL, getStaticPreviewUrl } from '@/config/env'

/**
 * 导入代码生成类型配置
 */
import { getCodeGenTypeLabel } from '@/config/codeGenType'

/**
 * 导入 Pinia Store
 * 用于获取当前登录用户信息
 */
import { useLoginUserStore } from '@/stores/loginUser'
import UserAvatar from '@/components/UserAvatar.vue'

/**
 * 导入 Markdown 渲染工具
 */
import { renderMarkdown } from '@/utils/markdown'

/**
 * 导入 Markdown 样式
 */
import '@/styles/markdown.css'

/**
 * 导入可视化编辑器
 */
import { VisualEditor, type ElementInfo } from '@/utils/visualEditor'


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
 * 思考中状态
 * 超过 5 秒没有响应时显示
 */
const isThinking = ref(false)
const thinkingStartTime = ref(0)
const thinkingTime = ref('0s')
let thinkingTimer: ReturnType<typeof setInterval> | null = null
const THINKING_TIMEOUT_MS = 5000

// 格式化思考时间
const formatThinkingTime = (seconds: number) => {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  if (mins > 0) return `${mins}m ${secs}s`
  return `${secs}s`
}

// 启动思考计时器
const startThinkingTimer = () => {
  thinkingStartTime.value = Date.now()
  isThinking.value = false

  if (thinkingTimer) clearInterval(thinkingTimer)
  thinkingTimer = setInterval(() => {
    const elapsed = Date.now() - thinkingStartTime.value
    if (elapsed >= THINKING_TIMEOUT_MS) {
      isThinking.value = true
    }
    thinkingTime.value = formatThinkingTime(Math.floor(elapsed / 1000))
  }, 1000)
}

// 停止思考计时器
const stopThinkingTimer = () => {
  if (thinkingTimer) {
    clearInterval(thinkingTimer)
    thinkingTimer = null
  }
  isThinking.value = false
  thinkingStartTime.value = 0
  thinkingTime.value = '0s'
}

// 重置思考计时器（收到输出时调用）
const resetThinkingTimer = () => {
  thinkingStartTime.value = Date.now()
  isThinking.value = false
  thinkingTime.value = '0s'
}

/**
 * 当前生成是否为修改模式
 * 用于显示不同的进度提示信息
 */
const isModifyModeGeneration = ref(false)

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
 * 应用详情弹窗
 */
const appDetailModalVisible = ref(false)

/**
 * 下载相关
 */
const downloading = ref(false)         // 是否正在下载

/**
 * 数据库相关
 */
const databaseDrawerVisible = ref(false)
const databaseInitializing = ref(false)

/** Supabase Manager iframe URL */
const supabaseManagerUrl = computed(() => {
  const baseUrl = import.meta.env.VITE_SUPABASE_MANAGER_URL || 'http://localhost:3001'
  return `${baseUrl}?ref=${import.meta.env.VITE_SUPABASE_PROJECT_REF || ''}&schema=app_${appId.value}`
})

/**
 * 是否为应用所有者
 * 只有所有者才能在对话页发送消息
 */
const isOwner = ref(true)

// ==================== 可视化编辑相关 ====================

/**
 * 是否处于编辑模式
 */
const isEditMode = ref(false)

/**
 * 选中的元素信息
 */
const selectedElementInfo = ref<ElementInfo | null>(null)

/**
 * iframe 元素引用
 */
const previewIframeRef = ref<HTMLIFrameElement | null>(null)

/**
 * 可视化编辑器实例
 */
const visualEditor = new VisualEditor({
  onElementSelected: (elementInfo: ElementInfo) => {
    selectedElementInfo.value = elementInfo
  }
})

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
 * 思考中指示器组件引用
 * 用于在收到新消息时重置计时器
 */
// const thinkingIndicatorRef = ref<any>(null)

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
       * 如果有代码生成类型，显示网站预览
       */
      if (appInfo.value.codeGenType) {
        previewUrl.value = getStaticPreviewUrl(appInfo.value.codeGenType, appId.value)
        iframeKey.value++
      }
      
    } else {
      message.error('获取应用信息失败')
    }
  } catch (error) {
    message.error('获取应用信息失败')
  }
}

/**
 * 元素信息 DTO 接口
 * 用于向后端传递选中元素的信息
 */
interface ElementInfoDTO {
  tagName: string
  id?: string
  className?: string
  textContent?: string
  selector: string
  pagePath?: string
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
  let text = inputText.value.trim()
  
  // 验证：内容不能为空，且不能在生成中重复发送
  if (!text || isGenerating.value) return

  // 保存当前选中的元素信息（用于后续构建 URL 参数）
  const currentElementInfo = selectedElementInfo.value

  // 如果有选中的元素，将元素信息添加到提示词中（用于用户消息显示）
  if (currentElementInfo) {
    let elementContext = `\n\n【选中元素信息】`
    if (currentElementInfo.pagePath) {
      elementContext += `\n- 页面路径: ${currentElementInfo.pagePath}`
    }
    elementContext += `\n- 标签: ${currentElementInfo.tagName.toLowerCase()}`
    elementContext += `\n- 选择器: ${currentElementInfo.selector}`
    if (currentElementInfo.textContent) {
      elementContext += `\n- 当前内容: ${currentElementInfo.textContent.substring(0, 100)}`
    }
    text += elementContext
  }

  // 1. 添加用户消息（包含元素信息）
  messages.value.push({ role: 'user', content: text })
  
  // 清空输入框
  inputText.value = ''
  
  // 发送消息后，清除选中元素并退出编辑模式
  if (currentElementInfo) {
    clearSelectedElement()
    if (isEditMode.value) {
      toggleEditMode()
    }
  }
  
  // 2. 添加 AI 消息占位
  // 记录索引，后续用于更新这条消息的内容
  const aiMessageIndex = messages.value.length
  messages.value.push({ role: 'ai', content: '', loading: true })
  
  // 3. 设置生成状态
  isGenerating.value = true
  // 设置是否为修改模式生成（用于显示不同的进度提示）
  isModifyModeGeneration.value = !!currentElementInfo
  
  // 重置用户滚动状态，新消息发送时应该滚动到底部
  userScrolledAway.value = false
  
  // 滚动到底部，显示最新消息（强制滚动）
  scrollToBottom(true)

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
    // agent: 是否使用 Agent 模式
    let url = `${API_BASE_URL}/app/chat/gen/code?appId=${appId.value}&message=${encodeURIComponent(text)}`
    
    // 如果有选中的元素，将 elementInfo 作为 JSON 字符串添加到 URL 参数
    // 这会触发后端的修改模式（Modify Mode）
    if (currentElementInfo) {
      const elementInfoDTO: ElementInfoDTO = {
        tagName: currentElementInfo.tagName,
        id: currentElementInfo.id || undefined,
        className: currentElementInfo.className || undefined,
        textContent: currentElementInfo.textContent || undefined,
        selector: currentElementInfo.selector,
        pagePath: currentElementInfo.pagePath || undefined
      }
      url += `&elementInfo=${encodeURIComponent(JSON.stringify(elementInfoDTO))}`
    }
    
    /**
     * 创建 EventSource 实例
     * 
     * withCredentials: true
     * - 允许跨域请求携带 Cookie
     * - 用于保持登录状态
     */
    const eventSource = new EventSource(url, { withCredentials: true })

    // 启动思考计时器
    startThinkingTimer()

    /**
     * 流式传输完成标志
     * 用于区分正常关闭和异常错误
     */
    let streamCompleted = false
    
    /**
     * 是否为修改模式
     * 当有 elementInfo 时为修改模式，需要过滤掉图片收集等无关步骤的进度信息
     */
    const isModifyMode = !!currentElementInfo
    
    /**
     * 需要在修改模式下过滤的进度消息关键词
     * 这些是创建模式特有的步骤，修改模式不需要显示
     */
    const createModeProgressKeywords = [
      '[图片规划]',
      '[内容图片收集]',
      '[插画收集]',
      '[架构图收集]',
      '[Logo收集]',
      '[图片聚合]',
      '[提示词增强]',
      '[智能路由]',
      '正在分析需求并收集相关图片资源',
      '开始执行搜索任务',
      '已整合图片资源',
      '无图片资源需要整合'
    ]
    
    /**
     * 检查消息是否应该被过滤（仅在修改模式下）
     */
    const shouldFilterMessage = (message: string): boolean => {
      if (!isModifyMode) return false
      return createModeProgressKeywords.some(keyword => message.includes(keyword))
    }
    
    /**
     * 消息缓冲区 - 用于节流更新
     * 累积多条消息后批量更新，避免频繁 DOM 操作导致页面卡顿
     */
    let messageBuffer = ''

    /**
     * 刷新定时器 - 用于批量更新消息
     */
    let flushTimer: number | null = null

    /**
     * 刷新缓冲区消息到 UI
     * 每隔 100ms 刷新一次，而不是每条消息都刷新
     */
    const flushMessageBuffer = () => {
      if (!messageBuffer) return

      // 追加缓冲区内容
      messages.value[aiMessageIndex].content += messageBuffer
      messageBuffer = ''

      // 只在需要时滚动到底部（减少 scrollToBottom 调用频率）
      scrollToBottom()

      // 清除定时器
      flushTimer = null
    }

    /**
     * 将内容添加到缓冲区（带节流）
     * @param content 要添加的内容
     */
    const bufferContent = (content: string) => {
      messageBuffer += content

      // 收到输出时重置思考计时器
      resetThinkingTimer()

      // 如果没有定时器，创建一个
      if (!flushTimer) {
        flushTimer = window.setTimeout(flushMessageBuffer, 100)
      }
    }

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
          // 修改模式下过滤掉创建模式特有的进度消息
          if (shouldFilterMessage(data.d)) {
            return
          }
          // 使用缓冲区节流更新，而不是直接追加
          bufferContent(data.d)
        }
      } catch (e) {
        // 如果解析失败，尝试直接使用原始数据
        if (event.data) {
          // 修改模式下过滤掉创建模式特有的进度消息
          if (shouldFilterMessage(event.data)) {
            return
          }
          // 使用缓冲区节流更新
          bufferContent(event.data)
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

        // 刷新缓冲区中的剩余消息
        if (flushTimer) {
          clearTimeout(flushTimer)
          flushMessageBuffer()
        }

        isGenerating.value = false
        isModifyModeGeneration.value = false
        messages.value[aiMessageIndex].loading = false

        // 停止思考计时器
        stopThinkingTimer()

        eventSource.close()
        
        // 生成完成，重置滚动状态
        userScrolledAway.value = false
        
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
        // 先刷新缓冲区，保留已接收的内容
        if (flushTimer) {
          clearTimeout(flushTimer)
          flushMessageBuffer()
        }
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

      // 刷新缓冲区中的剩余消息
      if (flushTimer) {
        clearTimeout(flushTimer)
        flushMessageBuffer()
      }

      eventSource.close()
      messages.value[aiMessageIndex].loading = false
      isGenerating.value = false
      isModifyModeGeneration.value = false

      // 停止思考计时器
      stopThinkingTimer()

      // 生成完成，重置滚动状态
      userScrolledAway.value = false
      
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
  isModifyModeGeneration.value = false
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
 * 用户是否手动滚动离开了底部
 * 用于智能滚动：如果用户正在查看历史消息，不要打扰他
 */
const userScrolledAway = ref(false)

/**
 * 判断滚动条是否在底部附近
 * 
 * @param threshold - 阈值，距离底部多少像素内算"在底部"，默认 100px
 * @returns 是否在底部附近
 */
const isNearBottom = (threshold = 100): boolean => {
  if (!messageListRef.value) return true
  
  const { scrollTop, scrollHeight, clientHeight } = messageListRef.value
  // 距离底部的距离 = 总高度 - 已滚动距离 - 可视区域高度
  const distanceFromBottom = scrollHeight - scrollTop - clientHeight
  return distanceFromBottom <= threshold
}

/**
 * 处理用户滚动事件
 * 检测用户是否主动滚动离开了底部
 */
const handleScroll = () => {
  // 如果不在生成中，不需要跟踪滚动状态
  if (!isGenerating.value) {
    userScrolledAway.value = false
    return
  }
  
  // 更新用户滚动状态
  userScrolledAway.value = !isNearBottom()
}

/**
 * 滚动到底部
 * 
 * 智能滚动：只有当用户没有主动向上滚动时才自动滚动
 * 
 * @param force - 是否强制滚动（忽略用户滚动状态）
 */
const scrollToBottom = (force = false) => {
  /**
   * nextTick: Vue 的异步更新机制
   * 
   * Vue 的 DOM 更新是异步的，数据变化后不会立即更新 DOM
   * nextTick 会在 DOM 更新完成后执行回调
   * 确保我们操作的是最新的 DOM
   */
  nextTick(() => {
    if (!messageListRef.value) return
    
    // 如果用户主动滚动离开了底部，且不是强制滚动，则不自动滚动
    if (userScrolledAway.value && !force) return
    
    // scrollTop: 滚动条距离顶部的距离
    // scrollHeight: 内容的总高度
    // 设置 scrollTop = scrollHeight 即滚动到底部
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  })
}

/** 初始化数据库 */
const handleInitDatabase = async () => {
  if (!appId.value) return
  databaseInitializing.value = true
  try {
    const res = await initializeDatabase(Number(appId.value))
    if (res.data) {
      message.success('数据库初始化成功')
      // 刷新应用信息
      await loadAppInfo()
      // 打开 Drawer
      databaseDrawerVisible.value = true
      // 自动发送初始化消息
      inputText.value = '数据库已启用，请分析应用并创建合适的数据库表，然后更新代码使用数据库'
      await handleSend()
    }
  } catch (e: any) {
    message.error(e.message || '数据库初始化失败')
  } finally {
    databaseInitializing.value = false
  }
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
 * 显示应用详情弹窗
 */
const showAppDetail = () => {
  appDetailModalVisible.value = true
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

// ==================== 可视化编辑相关方法 ====================

/**
 * 切换编辑模式
 */
const toggleEditMode = () => {
  // 检查 iframe 是否已经加载
  if (!previewIframeRef.value) {
    message.warning('请等待页面加载完成')
    return
  }
  
  const newEditMode = visualEditor.toggleEditMode()
  isEditMode.value = newEditMode
  
  // 退出编辑模式时清除选中元素
  if (!newEditMode) {
    selectedElementInfo.value = null
  }
}

/**
 * 清除选中的元素
 */
const clearSelectedElement = () => {
  selectedElementInfo.value = null
  visualEditor.clearSelection()
}

/**
 * iframe 加载完成回调
 */
const onIframeLoad = () => {
  // 获取 iframe 元素引用
  const iframe = document.querySelector('.preview-iframe') as HTMLIFrameElement
  if (iframe) {
    previewIframeRef.value = iframe
    visualEditor.init(iframe)
    visualEditor.onIframeLoad()
  }
}

/**
 * 获取输入框占位符文本
 */
const getInputPlaceholder = () => {
  if (selectedElementInfo.value) {
    return `正在编辑 ${selectedElementInfo.value.tagName.toLowerCase()} 元素，描述您想要的修改...`
  }
  return '描述越详细，页面越具体，可以一步一步完善生成结果...'
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
  
  // 监听 iframe 消息（用于可视化编辑）
  window.addEventListener('message', (event) => {
    visualEditor.handleIframeMessage(event)
  })
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

.message-item.user .message-content {
  display: flex;
  justify-content: flex-end;
}

/* 用户消息容器 */
.user-message {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  max-width: 80%;
}

.user-message :deep(.ant-avatar) {
  flex-shrink: 0;
}

/* 用户消息气泡 */
.user-bubble {
  background: #e8f5e9;
  padding: 12px 16px;
  border-radius: 12px 12px 4px 12px;
  word-break: break-word;
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
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

/* AI 消息内容 */
.ai-content {
  flex: 1;
  background: #f8f7ff;
  border-radius: 12px 12px 12px 4px;
  padding: 12px 16px;
  line-height: 1.6;
  min-width: 0;
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

/* 思考中指示器 */
.thinking-indicator {
  display: inline-block;
  margin-left: 4px;
  font-size: 12px;
  color: #999;
  vertical-align: middle;
  animation: thinking-pulse 1.5s ease-in-out infinite;
}

@keyframes thinking-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
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

/* 加载状态提示文字 */
.preview-loading-hint {
  font-size: 12px;
  color: #bbb;
  margin-top: 8px;
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

/* ==================== 应用详情弹窗 ==================== */

.app-detail-content {
  padding: 8px 0;
}

.app-detail-content .text-muted {
  color: #999;
}

/* ==================== 生成类型标签 ==================== */

.code-gen-type-tag {
  margin-left: 8px;
  font-size: 12px;
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

/* ==================== 可视化编辑相关样式 ==================== */

/* 选中元素信息提示框 */
.selected-element-alert {
  margin-bottom: 12px;
}

.selected-element-info {
  line-height: 1.4;
}

.element-header {
  margin-bottom: 6px;
  font-weight: 500;
}

.element-tag {
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 13px;
  color: #1890ff;
}

.element-id {
  color: #52c41a;
  margin-left: 4px;
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
}

.element-class {
  color: #faad14;
  margin-left: 4px;
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
}

.element-details {
  margin-top: 6px;
}

.element-item {
  margin-bottom: 4px;
  font-size: 12px;
  color: #666;
}

.element-item:last-child {
  margin-bottom: 0;
}

.element-selector-code {
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
  background: #f6f8fa;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 11px;
  color: #d73a49;
  border: 1px solid #e1e4e8;
  word-break: break-all;
}

/* 编辑模式按钮激活状态 */
.edit-mode-btn-active {
  background-color: #ff4d4f !important;
  border-color: #ff4d4f !important;
  color: white !important;
}

.edit-mode-btn-active:hover {
  background-color: #ff7875 !important;
  border-color: #ff7875 !important;
}

/* ==================== 数据库管理 Drawer ==================== */

.database-drawer-content {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 110px);
}

.database-status {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.database-iframe-wrapper {
  flex: 1;
  min-height: 0;
}

.database-iframe {
  width: 100%;
  height: 100%;
  min-height: 500px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
}

.database-tips {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}
</style>
