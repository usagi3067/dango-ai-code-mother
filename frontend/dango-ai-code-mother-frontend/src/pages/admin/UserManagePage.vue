<!--
  UserManagePage.vue - 用户管理页面
  功能：管理员可以查看、搜索、删除用户
  权限：只有管理员可以访问
-->
<template>
  <!-- 
    用户管理页面容器
  -->
  <div class="user-manage-page">
    <!-- 
      a-card: Ant Design 的卡片组件
      title: 卡片标题
      :bordered="false": 不显示边框
    -->
    <a-card title="用户管理" :bordered="false">
      <!-- 
        搜索栏区域
        使用 a-form 实现搜索功能
      -->
      <a-form
        layout="inline"
        :model="searchParams"
        @finish="handleSearch"
      >
        <!-- 
          用户名搜索框
          layout="inline": 表单项横向排列
        -->
        <a-form-item label="用户名" name="userName">
          <!-- 
            a-input: 输入框组件
            v-model:value: 双向绑定搜索参数
            placeholder: 占位符文本
            allow-clear: 显示清除按钮
          -->
          <a-input
            v-model:value="searchParams.userName"
            placeholder="请输入用户名"
            allow-clear
            style="width: 200px"
          />
        </a-form-item>
        
        <!-- 
          搜索按钮
        -->
        <a-form-item>
          <a-space>
            <!-- 
              搜索按钮
              type="primary": 主要按钮样式（蓝色）
              html-type="submit": 提交表单
            -->
            <a-button type="primary" html-type="submit">
              搜索
            </a-button>
            
            <!-- 
              重置按钮
              @click: 点击事件，重置搜索条件
            -->
            <a-button @click="handleReset">
              重置
            </a-button>
          </a-space>
        </a-form-item>
      </a-form>
      
      <!-- 
        分割线
        用于分隔搜索栏和表格
      -->
      <a-divider />
      
      <!-- 
        用户列表表格
        
        :columns: 表格列配置
        :data-source: 表格数据源
        :loading: 加载状态
        :pagination: 分页配置
        row-key: 行的唯一标识（使用 id）
        @change: 表格变化事件（分页、排序、筛选）
      -->
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <!-- 
          自定义列：用户头像
          #bodyCell: 自定义单元格内容的插槽
          column.key === 'userAvatar': 只对头像列生效
        -->
        <template #bodyCell="{ column, record }">
          <!-- 头像列 -->
          <template v-if="column.key === 'userAvatar'">
            <!-- 
              a-avatar: Ant Design 的头像组件
              :src: 头像图片地址
              :size: 头像大小
            -->
            <a-avatar :src="record.userAvatar" :size="48" />
          </template>
          
          <!-- 用户角色列 -->
          <template v-else-if="column.key === 'userRole'">
            <!-- 
              a-tag: Ant Design 的标签组件
              color: 标签颜色
              - admin: 红色（管理员）
              - user: 蓝色（普通用户）
            -->
            <a-tag :color="record.userRole === 'admin' ? 'red' : 'blue'">
              {{ record.userRole === 'admin' ? '管理员' : '普通用户' }}
            </a-tag>
          </template>
          
          <!-- 创建时间列 -->
          <template v-else-if="column.key === 'createTime'">
            <!-- 
              格式化时间显示
              使用 dayjs 或自定义函数格式化
            -->
            {{ formatDate(record.createTime) }}
          </template>
          
          <!-- 更新时间列 -->
          <template v-else-if="column.key === 'updateTime'">
            {{ formatDate(record.updateTime) }}
          </template>
          
          <!-- 操作列 -->
          <template v-else-if="column.key === 'action'">
            <a-space>
              <!-- 
                删除按钮
                danger: 危险按钮样式（红色）
                @click: 点击事件，删除用户
              -->
              <a-button
                type="link"
                danger
                @click="handleDelete(record)"
              >
                删除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
/**
 * 导入 Vue 3 的响应式 API
 * ref: 用于创建响应式数据
 * reactive: 用于创建响应式对象
 * onMounted: 生命周期钩子，组件挂载后执行
 */
import { ref, reactive, onMounted } from 'vue'

/**
 * 导入 Ant Design Vue 的消息提示和确认框组件
 * message: 用于显示成功、错误等提示信息
 * Modal: 用于显示确认对话框
 */
import { message, Modal } from 'ant-design-vue'

/**
 * 导入用户相关的 API 接口
 * listUserVoByPage: 分页查询用户列表
 * deleteUser: 删除用户
 */
import { listUserVoByPage, deleteUser } from '@/api/userController'

/**
 * 导入 Ant Design Vue 的表格类型定义
 */
import type { TableProps } from 'ant-design-vue'

/**
 * 表格列配置
 * 
 * 定义表格的列结构和显示方式
 * 
 * 字段说明：
 * - title: 列标题
 * - dataIndex: 数据字段名
 * - key: 列的唯一标识
 * - width: 列宽度
 * - align: 对齐方式
 */
const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    key: 'id',
    width: 80,
  },
  {
    title: '头像',
    dataIndex: 'userAvatar',
    key: 'userAvatar',
    width: 80,
    align: 'center' as const,
  },
  {
    title: '用户名',
    dataIndex: 'userName',
    key: 'userName',
    width: 150,
  },
  {
    title: '账号',
    dataIndex: 'userAccount',
    key: 'userAccount',
    width: 150,
  },
  {
    title: '用户角色',
    dataIndex: 'userRole',
    key: 'userRole',
    width: 120,
    align: 'center' as const,
  },
  {
    title: '个人简介',
    dataIndex: 'userProfile',
    key: 'userProfile',
    ellipsis: true,  // 超出省略
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime',
    width: 180,
  },
  {
    title: '更新时间',
    dataIndex: 'updateTime',
    key: 'updateTime',
    width: 180,
  },
  {
    title: '操作',
    key: 'action',
    width: 120,
    align: 'center' as const,
    fixed: 'right' as const,  // 固定在右侧
  },
]

/**
 * 搜索参数
 * 
 * 使用 reactive 创建响应式对象
 * 用于存储搜索条件
 */
const searchParams = reactive({
  userName: '',  // 用户名搜索条件
})

/**
 * 表格数据源
 * 
 * 存储从后端获取的用户列表数据
 */
const dataSource = ref<API.UserVO[]>([])

/**
 * 加载状态
 * 
 * 控制表格的加载动画
 * true: 显示加载动画
 * false: 隐藏加载动画
 */
const loading = ref(false)

/**
 * 分页配置
 * 
 * 控制表格的分页功能
 * 
 * 字段说明：
 * - current: 当前页码
 * - pageSize: 每页显示条数
 * - total: 总记录数
 * - showSizeChanger: 是否显示每页条数选择器
 * - showTotal: 显示总记录数的函数
 */
const pagination = reactive({
  current: 1,           // 当前页码，默认第 1 页
  pageSize: 10,         // 每页显示 10 条
  total: 0,             // 总记录数，从后端获取
  showSizeChanger: true,  // 显示每页条数选择器
  showTotal: (total: number) => `共 ${total} 条记录`,  // 显示总记录数
})

/**
 * 加载用户列表数据
 * 
 * 功能：从后端获取用户列表数据
 * 
 * async: 异步函数
 * - 因为需要等待网络请求完成
 * 
 * 执行流程：
 * 1. 设置加载状态为 true
 * 2. 调用后端接口获取数据
 * 3. 更新表格数据和分页信息
 * 4. 设置加载状态为 false
 */
const loadData = async () => {
  /**
   * 开始加载，显示加载动画
   */
  loading.value = true
  
  try {
    /**
     * 调用后端接口获取用户列表
     * 
     * 参数说明：
     * - pageNum: 当前页码（注意：后端使用 pageNum，不是 current）
     * - pageSize: 每页显示条数
     * - userName: 用户名搜索条件（可选）
     */
    const res = await listUserVoByPage({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      userName: searchParams.userName || undefined,  // 如果为空，传 undefined
    })
    
    /**
     * 判断请求是否成功
     */
    if (res.data.code === 0 && res.data.data) {
      /**
       * 更新表格数据
       * 
       * res.data.data.records: 当前页的用户列表
       */
      dataSource.value = res.data.data.records || []
      
      /**
       * 更新分页信息
       * 
       * res.data.data.totalRow: 总记录数（注意：后端使用 totalRow，不是 total）
       */
      pagination.total = res.data.data.totalRow || 0
    } else {
      /**
       * 请求失败，显示错误提示
       */
      message.error('加载用户列表失败：' + res.data.message)
    }
  } catch (error) {
    /**
     * 捕获异常，显示错误提示
     */
    message.error('加载用户列表失败，请稍后重试')
    console.error('加载用户列表错误:', error)
  } finally {
    /**
     * 无论成功还是失败，都要关闭加载动画
     * 
     * finally: 无论 try 还是 catch，都会执行
     */
    loading.value = false
  }
}

/**
 * 搜索处理函数
 * 
 * 功能：用户点击搜索按钮时触发
 * 
 * 执行流程：
 * 1. 重置页码为第 1 页
 * 2. 重新加载数据
 */
const handleSearch = () => {
  /**
   * 搜索时重置到第 1 页
   * 
   * 原因：搜索条件变化后，之前的页码可能无效
   */
  pagination.current = 1
  
  /**
   * 重新加载数据
   */
  loadData()
}

/**
 * 重置处理函数
 * 
 * 功能：用户点击重置按钮时触发
 * 
 * 执行流程：
 * 1. 清空搜索条件
 * 2. 重置页码为第 1 页
 * 3. 重新加载数据
 */
const handleReset = () => {
  /**
   * 清空搜索条件
   */
  searchParams.userName = ''
  
  /**
   * 重置页码为第 1 页
   */
  pagination.current = 1
  
  /**
   * 重新加载数据
   */
  loadData()
}

/**
 * 表格变化处理函数
 * 
 * 功能：处理分页、排序、筛选等表格变化事件
 * 
 * @param pag - 分页信息
 * 
 * 执行流程：
 * 1. 更新当前页码和每页条数
 * 2. 重新加载数据
 */
const handleTableChange: TableProps['onChange'] = (pag) => {
  /**
   * 更新分页信息
   * 
   * pag.current: 用户点击的页码
   * pag.pageSize: 用户选择的每页条数
   */
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
  
  /**
   * 重新加载数据
   */
  loadData()
}

/**
 * 删除用户处理函数
 * 
 * 功能：删除指定用户
 * 
 * @param record - 要删除的用户记录
 * 
 * 执行流程：
 * 1. 显示确认对话框
 * 2. 用户确认后调用删除接口
 * 3. 删除成功后重新加载数据
 */
const handleDelete = (record: API.UserVO) => {
  /**
   * 显示确认对话框
   * 
   * Modal.confirm: Ant Design 的确认对话框
   * - title: 对话框标题
   * - content: 对话框内容
   * - onOk: 用户点击确定后的回调函数
   */
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除用户"${record.userName}"吗？此操作不可恢复。`,
    okText: '确定',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        /**
         * 调用删除接口
         * 
         * 参数：
         * - id: 要删除的用户 ID
         */
        const res = await deleteUser({
          id: record.id,
        })
        
        /**
         * 判断删除是否成功
         */
        if (res.data.code === 0) {
          /**
           * 删除成功
           */
          message.success('删除成功')
          
          /**
           * 重新加载数据
           * 
           * 注意：如果当前页只有一条数据，删除后应该跳转到上一页
           */
          if (dataSource.value.length === 1 && pagination.current > 1) {
            pagination.current -= 1
          }
          
          loadData()
        } else {
          /**
           * 删除失败
           */
          message.error('删除失败：' + res.data.message)
        }
      } catch (error) {
        /**
         * 捕获异常
         */
        message.error('删除失败，请稍后重试')
        console.error('删除用户错误:', error)
      }
    },
  })
}

/**
 * 格式化日期时间
 * 
 * 功能：将时间戳或日期字符串格式化为易读的格式
 * 
 * @param dateStr - 日期字符串或时间戳
 * @returns 格式化后的日期字符串
 * 
 * 示例：
 * - 输入：'2024-01-20T10:30:00'
 * - 输出：'2024-01-20 10:30:00'
 */
const formatDate = (dateStr: string | undefined) => {
  if (!dateStr) {
    return '-'
  }
  
  /**
   * 创建 Date 对象
   */
  const date = new Date(dateStr)
  
  /**
   * 格式化日期
   * 
   * 使用 toLocaleString 方法格式化
   * - 'zh-CN': 中文格式
   * - hour12: false: 使用 24 小时制
   */
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  })
}

/**
 * 组件挂载后执行
 * 
 * 加载初始数据
 */
onMounted(() => {
  loadData()
})
</script>

<style scoped>
/**
 * scoped 样式：只在当前组件内生效
 * 避免样式污染其他组件
 */

/* 用户管理页面容器样式 */
.user-manage-page {
  padding: 24px;              /* 内边距 24px */
  background: #f0f2f5;        /* 浅灰色背景 */
  min-height: 100vh;          /* 最小高度为视口高度 */
}
</style>
