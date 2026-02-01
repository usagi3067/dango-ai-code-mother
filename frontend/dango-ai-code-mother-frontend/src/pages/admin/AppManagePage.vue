<!--
  AppManagePage.vue - 应用管理页面（管理员专用）
  
  【页面功能】
  管理员可以在这个页面：
  1. 查看所有用户的应用列表
  2. 搜索应用（按名称、用户ID、代码类型）
  3. 编辑应用信息
  4. 设置精选应用
  5. 删除应用
  
  【学习要点】
  1. 表格组件 (a-table) 的使用
  2. 表格列配置 (columns)
  3. 自定义列渲染 (#bodyCell)
  4. 分页处理
  5. 搜索和重置功能
-->
<template>
  <div class="app-manage-page">
    <a-card title="应用管理" :bordered="false">
      
      <!-- ==================== 搜索栏 ==================== -->
      <!-- 
        a-form: 表单组件
        layout="inline": 行内布局，表单项横向排列
        @finish: 表单提交时触发（点击搜索按钮）
      -->
      <a-form layout="inline" :model="searchParams" @finish="handleSearch">
        <a-form-item label="应用名称" name="appName">
          <a-input
            v-model:value="searchParams.appName"
            placeholder="请输入应用名称"
            allow-clear
            style="width: 160px"
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
        <a-form-item label="代码类型" name="codeGenType">
          <a-select
            v-model:value="searchParams.codeGenType"
            placeholder="请选择类型"
            allow-clear
            style="width: 150px"
            :options="CODE_GEN_TYPE_OPTIONS"
          />
        </a-form-item>
        <a-form-item label="标签" name="tag">
          <a-select
            v-model:value="searchParams.tag"
            placeholder="请选择标签"
            allow-clear
            style="width: 120px"
            :options="APP_TAG_OPTIONS"
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

      <!-- ==================== 应用列表表格 ==================== -->
      <!-- 
        a-table: Ant Design Vue 的表格组件
        
        :columns: 列配置数组
        :data-source: 数据源数组
        :loading: 是否显示加载状态
        :pagination: 分页配置
        row-key: 行的唯一标识字段
        :scroll: 滚动配置，x 表示横向滚动宽度
        @change: 表格状态变化时触发（分页、排序、筛选）
      -->
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        :scroll="{ x: 1200 }"
        @change="handleTableChange"
      >
        <!-- 
          自定义列渲染
          
          #bodyCell: 具名插槽，用于自定义单元格内容
          { column, record }: 插槽参数
          - column: 当前列的配置
          - record: 当前行的数据
        -->
        <template #bodyCell="{ column, record }">
          
          <!-- 封面列：显示图片或占位文字 -->
          <template v-if="column.key === 'cover'">
            <a-image
              v-if="record.cover"
              :src="record.cover"
              :width="80"
              :height="60"
              style="object-fit: cover; border-radius: 4px"
            />
            <span v-else class="no-cover">无封面</span>
          </template>

          <!-- 用户列：显示头像和用户名 -->
          <template v-else-if="column.key === 'user'">
            <div class="user-info">
              <UserAvatar 
                :src="record.user?.userAvatar" 
                :name="record.user?.userName"
                :size="24"
              />
              <span>{{ record.user?.userName || '-' }}</span>
            </div>
          </template>

          <!-- 优先级列：精选应用显示金色标签 -->
          <template v-else-if="column.key === 'priority'">
            <!-- 
              :color: 动态绑定颜色
              三元表达式: 条件 ? 真值 : 假值
              优先级 >= 99 显示金色，否则显示默认色
            -->
            <a-tag :color="record.priority >= 99 ? 'gold' : 'default'">
              {{ record.priority || 0 }}
            </a-tag>
          </template>

          <!-- 代码类型列：显示中文标签 -->
          <template v-else-if="column.key === 'codeGenType'">
            <span>{{ getCodeGenTypeLabel(record.codeGenType) || '-' }}</span>
          </template>

          <!-- 标签列：显示应用标签 -->
          <template v-else-if="column.key === 'tag'">
            <a-tag :color="getAppTagColor(record.tag)">
              {{ getAppTagLabel(record.tag) }}
            </a-tag>
          </template>

          <!-- 部署状态列 -->
          <template v-else-if="column.key === 'deployKey'">
            <a-tag v-if="record.deployKey" color="green">已部署</a-tag>
            <a-tag v-else color="default">未部署</a-tag>
          </template>

          <!-- 创建时间列 -->
          <template v-else-if="column.key === 'createTime'">
            {{ formatDate(record.createTime) }}
          </template>

          <!-- 操作列 -->
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleEdit(record)">
                编辑
              </a-button>
              <!-- 
                :disabled: 动态禁用
                已经是精选的应用（优先级 >= 99）禁用精选按钮
              -->
              <a-button 
                type="link" 
                size="small" 
                @click="handleSetFeatured(record)"
                :disabled="record.priority >= 99"
              >
                精选
              </a-button>
              <a-button type="link" size="small" danger @click="handleDelete(record)">
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
 * ==================== 应用管理页脚本 ====================
 * 
 * 【功能说明】
 * 1. 表格列配置
 * 2. 搜索和分页
 * 3. 编辑、精选、删除操作
 */

import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { TableProps } from 'ant-design-vue'
import { 
  listAppVoByPageByAdmin, 
  deleteAppByAdmin, 
  updateAppByAdmin 
} from '@/api/app/appController'

/**
 * 导入代码生成类型配置
 */
import { CODE_GEN_TYPE_OPTIONS, getCodeGenTypeLabel } from '@/config/codeGenType'

/**
 * 导入应用标签配置
 */
import { APP_TAG_OPTIONS, getAppTagColor, getAppTagLabel } from '@/config/appTag'

/**
 * 导入用户头像组件
 */
import UserAvatar from '@/components/UserAvatar.vue'

const router = useRouter()

/**
 * 表格列配置
 * 
 * 每个列配置对象包含：
 * - title: 列标题
 * - dataIndex: 数据字段名（对应数据源中的属性）
 * - key: 列的唯一标识（用于自定义渲染时判断）
 * - width: 列宽度
 * - align: 对齐方式
 * - ellipsis: 是否超出省略
 * - fixed: 固定列（'left' 或 'right'）
 * 
 * as const: TypeScript 类型断言
 * 将字符串字面量类型固定，避免类型推断为 string
 */
const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '封面', dataIndex: 'cover', key: 'cover', width: 100 },
  { title: '应用名称', dataIndex: 'appName', key: 'appName', width: 150, ellipsis: true },
  { title: '标签', dataIndex: 'tag', key: 'tag', width: 100 },
  { title: '代码类型', dataIndex: 'codeGenType', key: 'codeGenType', width: 100 },
  { title: '创建者', dataIndex: 'user', key: 'user', width: 120 },
  { title: '优先级', dataIndex: 'priority', key: 'priority', width: 80, align: 'center' as const },
  { title: '部署状态', dataIndex: 'deployKey', key: 'deployKey', width: 100, align: 'center' as const },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: 160, fixed: 'right' as const }
]

/**
 * 搜索参数
 */
const searchParams = reactive({
  appName: '',
  userId: '',
  codeGenType: '',
  tag: ''
})

/**
 * 表格数据源
 */
const dataSource = ref<API.AppVO[]>([])

/**
 * 加载状态
 */
const loading = ref(false)

/**
 * 分页配置
 * 
 * showSizeChanger: 是否显示每页条数选择器
 * showTotal: 显示总记录数的函数
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
    const res = await listAppVoByPageByAdmin({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      sortField: 'createTime',  // 按创建时间排序
      sortOrder: 'desc',         // 降序（最新的在前面）
      // || undefined: 如果为空字符串，传 undefined（不传这个参数）
      appName: searchParams.appName || undefined,
      // 用户 ID 需要转换类型
      userId: searchParams.userId ? (searchParams.userId as any) : undefined,
      codeGenType: searchParams.codeGenType || undefined,
      tag: searchParams.tag || undefined
    })
    
    if (res.data.code === 0 && res.data.data) {
      dataSource.value = res.data.data.records || []
      pagination.total = res.data.data.totalRow || 0
    } else {
      message.error('加载应用列表失败：' + res.data.message)
    }
  } catch (error) {
    message.error('加载应用列表失败')
  } finally {
    loading.value = false
  }
}

/**
 * 搜索
 * 搜索时重置到第一页
 */
const handleSearch = () => {
  pagination.current = 1
  loadData()
}

/**
 * 重置搜索条件
 */
const handleReset = () => {
  searchParams.appName = ''
  searchParams.userId = ''
  searchParams.codeGenType = ''
  searchParams.tag = ''
  pagination.current = 1
  loadData()
}

/**
 * 表格变化处理
 * 
 * TableProps['onChange']: 从 TableProps 类型中提取 onChange 的类型
 * 这是 TypeScript 的索引访问类型
 */
const handleTableChange: TableProps['onChange'] = (pag) => {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
  loadData()
}

/**
 * 编辑应用
 * 跳转到编辑页，带上 admin=true 查询参数
 */
const handleEdit = (record: API.AppVO) => {
  router.push(`/app/edit/${String(record.id)}?admin=true`)
}

/**
 * 设置精选
 * 将应用优先级设为 99
 */
const handleSetFeatured = (record: API.AppVO) => {
  Modal.confirm({
    title: '设置精选',
    content: `确定将应用"${record.appName}"设为精选吗？`,
    onOk: async () => {
      try {
        const res = await updateAppByAdmin({
          id: record.id,
          priority: 99  // 精选优先级
        })
        if (res.data.code === 0) {
          message.success('设置成功')
          loadData()  // 刷新列表
        } else {
          message.error('设置失败：' + res.data.message)
        }
      } catch (error) {
        message.error('设置失败')
      }
    }
  })
}

/**
 * 删除应用
 */
const handleDelete = (record: API.AppVO) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除应用"${record.appName}"吗？此操作不可恢复。`,
    okText: '确定',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        const res = await deleteAppByAdmin({ id: record.id })
        if (res.data.code === 0) {
          message.success('删除成功')
          // 如果当前页只有一条数据，删除后跳到上一页
          if (dataSource.value.length === 1 && pagination.current > 1) {
            pagination.current -= 1
          }
          loadData()
        } else {
          message.error('删除失败：' + res.data.message)
        }
      } catch (error) {
        message.error('删除失败')
      }
    }
  })
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
.app-manage-page {
  padding: 24px;
  background: #f0f2f5;
  min-height: 100vh;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.no-cover {
  color: #999;
  font-size: 12px;
}
</style>
