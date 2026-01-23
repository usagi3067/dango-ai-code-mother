<!--
  ChatHistoryManagePage.vue - 对话管理页面（管理员专用）
  
  【页面功能】
  管理员可以在这个页面：
  1. 查看所有用户的对话历史记录
  2. 搜索对话（按应用ID、用户ID、消息类型）
  3. 分页浏览对话记录
  
  【学习要点】
  1. 表格组件 (a-table) 的使用
  2. 表格列配置 (columns)
  3. 自定义列渲染 (#bodyCell)
  4. 分页处理
  5. 搜索和重置功能
-->
<template>
  <div class="chat-history-manage-page">
    <a-card title="对话管理" :bordered="false">
      
      <!-- ==================== 搜索栏 ==================== -->
      <a-form layout="inline" :model="searchParams" @finish="handleSearch">
        <a-form-item label="应用ID" name="appId">
          <a-input
            v-model:value="searchParams.appId"
            placeholder="请输入应用ID"
            allow-clear
            style="width: 120px"
          />
        </a-form-item>
        <a-form-item label="用户ID" name="userId">
          <a-input
            v-model:value="searchParams.userId"
            placeholder="请输入用户ID"
            allow-clear
            style="width: 120px"
          />
        </a-form-item>
        <a-form-item label="消息类型" name="messageType">
          <a-select
            v-model:value="searchParams.messageType"
            placeholder="请选择类型"
            allow-clear
            style="width: 120px"
            :options="MESSAGE_TYPE_OPTIONS"
          />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit">搜索</a-button>
            <a-button @click="handleReset">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>

      <a-divider />

      <!-- ==================== 对话历史列表表格 ==================== -->
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        :scroll="{ x: 1000 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <!-- 消息内容列：截断显示 -->
          <template v-if="column.key === 'message'">
            <a-tooltip :title="record.message" placement="topLeft">
              <span class="message-content">{{ truncateMessage(record.message) }}</span>
            </a-tooltip>
          </template>

          <!-- 消息类型列：显示标签 -->
          <template v-else-if="column.key === 'messageType'">
            <a-tag :color="record.messageType === 'ai' ? 'blue' : 'green'">
              {{ record.messageType === 'ai' ? 'AI' : '用户' }}
            </a-tag>
          </template>

          <!-- 创建时间列 -->
          <template v-else-if="column.key === 'createTime'">
            {{ formatDate(record.createTime) }}
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
/**
 * ==================== 对话管理页脚本 ====================
 * 
 * 【功能说明】
 * 1. 表格列配置
 * 2. 搜索和分页
 * 3. 数据加载
 */

import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import type { TableProps } from 'ant-design-vue'
import { listChatHistoryByPageForAdmin } from '@/api/chatHistoryController'

/**
 * 消息类型选项
 */
const MESSAGE_TYPE_OPTIONS = [
  { label: 'AI', value: 'ai' },
  { label: '用户', value: 'user' }
]

/**
 * 表格列配置
 */
const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '消息内容', dataIndex: 'message', key: 'message', width: 300, ellipsis: true },
  { title: '消息类型', dataIndex: 'messageType', key: 'messageType', width: 100, align: 'center' as const },
  { title: '应用ID', dataIndex: 'appId', key: 'appId', width: 100 },
  { title: '用户ID', dataIndex: 'userId', key: 'userId', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160 }
]

/**
 * 搜索参数
 */
const searchParams = reactive({
  appId: '',
  userId: '',
  messageType: ''
})

/**
 * 表格数据源
 */
const dataSource = ref<API.ChatHistoryVO[]>([])

/**
 * 加载状态
 */
const loading = ref(false)

/**
 * 分页配置
 */
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条记录`
})

/**
 * 加载数据
 */
const loadData = async () => {
  loading.value = true
  try {
    const res = await listChatHistoryByPageForAdmin({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      sortField: 'createTime',
      sortOrder: 'desc',
      appId: searchParams.appId ? Number(searchParams.appId) : undefined,
      userId: searchParams.userId ? Number(searchParams.userId) : undefined,
      messageType: searchParams.messageType || undefined
    })
    
    if (res.data.code === 0 && res.data.data) {
      dataSource.value = res.data.data.records || []
      pagination.total = res.data.data.totalRow || 0
    } else {
      message.error('加载对话历史失败：' + res.data.message)
    }
  } catch (error) {
    message.error('加载对话历史失败')
  } finally {
    loading.value = false
  }
}

/**
 * 搜索
 */
const handleSearch = () => {
  pagination.current = 1
  loadData()
}

/**
 * 重置搜索条件
 */
const handleReset = () => {
  searchParams.appId = ''
  searchParams.userId = ''
  searchParams.messageType = ''
  pagination.current = 1
  loadData()
}

/**
 * 表格变化处理
 */
const handleTableChange: TableProps['onChange'] = (pag) => {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
  loadData()
}

/**
 * 截断消息内容
 */
const truncateMessage = (msg: string | undefined) => {
  if (!msg) return '-'
  return msg.length > 50 ? msg.substring(0, 50) + '...' : msg
}

/**
 * 格式化日期
 */
const formatDate = (dateStr: string | undefined) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  })
}

/**
 * 组件挂载后加载数据
 */
onMounted(() => {
  loadData()
})
</script>

<style scoped>
.chat-history-manage-page {
  padding: 24px;
  background: #f0f2f5;
  min-height: 100vh;
}

.message-content {
  display: inline-block;
  max-width: 280px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
