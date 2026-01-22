<!--
  AppEditPage.vue - 应用信息修改页
  
  【页面功能】
  用于编辑应用的基本信息：
  - 普通用户：只能编辑自己应用的名称
  - 管理员：可以编辑应用名称、封面、优先级
  
  【学习要点】
  1. 表单组件的使用 (a-form)
  2. 表单验证 (rules)
  3. 路由查询参数 (route.query)
  4. 计算属性 (computed)
-->
<template>
  <div class="app-edit-page">
    <!-- 
      a-card: Ant Design Vue 的卡片组件
      :title: 动态标题，根据是否管理员显示不同文字
    -->
    <a-card :title="isAdmin ? '编辑应用（管理员）' : '编辑应用'" :bordered="false">
      <!-- 
        a-spin: 加载动画组件
        包裹表单，在加载数据时显示加载状态
      -->
      <a-spin :spinning="loading">
        <!-- 
          a-form: Ant Design Vue 的表单组件
          
          :model: 绑定表单数据对象
          :label-col: 标签列的布局（栅格系统，共24列）
          :wrapper-col: 输入框列的布局
          @finish: 表单验证通过后触发
        -->
        <a-form
          :model="formData"
          :label-col="{ span: 4 }"
          :wrapper-col="{ span: 16 }"
          @finish="handleSubmit"
        >
          <!-- 应用ID（只读） -->
          <a-form-item label="应用ID">
            <!-- 
              String(appId): 确保显示为字符串
              disabled: 禁用输入，只读显示
            -->
            <a-input :value="String(appId)" disabled />
          </a-form-item>

          <!-- 
            应用名称（可编辑）
            
            name: 字段名，用于表单验证
            :rules: 验证规则数组
            - required: true 表示必填
            - message: 验证失败时的提示信息
          -->
          <a-form-item 
            label="应用名称" 
            name="appName"
            :rules="[{ required: true, message: '请输入应用名称' }]"
          >
            <a-input 
              v-model:value="formData.appName" 
              placeholder="请输入应用名称"
              :maxlength="50"
              show-count
            />
          </a-form-item>

          <!-- 
            管理员专属字段
            v-if="isAdmin": 只有管理员才能看到这些字段
          -->
          <template v-if="isAdmin">
            <!-- 封面URL -->
            <a-form-item label="封面URL" name="cover">
              <a-input 
                v-model:value="formData.cover" 
                placeholder="请输入封面图片URL"
              />
              <!-- 封面预览 -->
              <div v-if="formData.cover" class="cover-preview">
                <a-image :src="formData.cover" :width="200" />
              </div>
            </a-form-item>

            <!-- 优先级 -->
            <a-form-item label="优先级" name="priority">
              <!-- 
                a-input-number: 数字输入框
                :min/:max: 限制输入范围
              -->
              <a-input-number 
                v-model:value="formData.priority" 
                :min="0"
                :max="999"
                placeholder="优先级（99为精选）"
              />
              <span class="priority-hint">设置为 99 即为精选应用</span>
            </a-form-item>
          </template>

          <!-- 只读信息展示 -->
          <a-form-item label="代码类型">
            <span>{{ getCodeGenTypeLabel(appInfo?.codeGenType) || '-' }}</span>
          </a-form-item>

          <a-form-item label="创建时间">
            <span>{{ formatDate(appInfo?.createTime) }}</span>
          </a-form-item>

          <a-form-item label="创建者" v-if="isAdmin">
            <div class="creator-info">
              <UserAvatar 
                :src="appInfo?.user?.userAvatar" 
                :name="appInfo?.user?.userName"
                :size="24"
              />
              <span>{{ appInfo?.user?.userName || '-' }}</span>
            </div>
          </a-form-item>

          <!-- 操作按钮 -->
          <a-form-item :wrapper-col="{ offset: 4, span: 16 }">
            <a-space>
              <!-- 
                html-type="submit": 点击时提交表单
                会触发表单的 @finish 事件
              -->
              <a-button type="primary" html-type="submit" :loading="submitting">
                保存
              </a-button>
              <a-button @click="goBack">返回</a-button>
              <!-- 普通用户可以删除自己的应用 -->
              <a-button v-if="!isAdmin" type="link" danger @click="handleDelete">
                删除应用
              </a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </a-spin>
    </a-card>
  </div>
</template>

<script setup lang="ts">
/**
 * ==================== 应用编辑页脚本 ====================
 * 
 * 【功能说明】
 * 1. 根据路由参数获取应用 ID
 * 2. 根据查询参数判断是否管理员模式
 * 3. 加载应用信息并填充表单
 * 4. 提交表单更新应用信息
 */

import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { 
  getAppVoById, 
  getAppVoByIdByAdmin,
  updateApp, 
  updateAppByAdmin,
  deleteApp 
} from '@/api/appController'

/**
 * 导入代码生成类型配置
 */
import { getCodeGenTypeLabel } from '@/config/codeGenType'

/**
 * 导入用户头像组件
 */
import UserAvatar from '@/components/UserAvatar.vue'

const route = useRoute()
const router = useRouter()

/**
 * 是否管理员模式
 * 
 * computed: 计算属性
 * - 根据依赖的数据自动计算
 * - 当依赖变化时自动重新计算
 * - 具有缓存，依赖不变时不会重新计算
 * 
 * route.query: URL 查询参数
 * 例如: /app/edit/123?admin=true
 * route.query.admin = 'true'
 */
const isAdmin = computed(() => route.query.admin === 'true')

// 应用 ID（字符串）
const appId = ref<string>('')

// 应用信息
const appInfo = ref<API.AppVO | null>(null)

/**
 * 表单数据
 * 
 * 使用 reactive 创建响应式对象
 * 表单组件会双向绑定这些字段
 */
const formData = reactive({
  appName: '',
  cover: '',
  priority: 0
})

// 加载状态
const loading = ref(false)
// 提交状态
const submitting = ref(false)

/**
 * 加载应用信息
 */
const loadAppInfo = async () => {
  if (!appId.value) return
  
  loading.value = true
  try {
    /**
     * 根据是否管理员调用不同的接口
     * 
     * 管理员接口可以查看任意应用
     * 普通用户接口只能查看自己的应用
     */
    const res = isAdmin.value 
      ? await getAppVoByIdByAdmin({ id: appId.value as any })
      : await getAppVoById({ id: appId.value as any })
    
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data
      // 填充表单数据
      formData.appName = res.data.data.appName || ''
      formData.cover = res.data.data.cover || ''
      formData.priority = res.data.data.priority || 0
    } else {
      message.error('获取应用信息失败')
      router.back()
    }
  } catch (error) {
    message.error('获取应用信息失败')
    router.back()
  } finally {
    loading.value = false
  }
}

/**
 * 提交表单
 * 
 * 当表单验证通过后，@finish 事件会触发这个函数
 */
const handleSubmit = async () => {
  submitting.value = true
  try {
    let res
    
    // 根据是否管理员调用不同的更新接口
    if (isAdmin.value) {
      // 管理员可以更新更多字段
      res = await updateAppByAdmin({
        id: appId.value as any,
        appName: formData.appName,
        cover: formData.cover,
        priority: formData.priority
      })
    } else {
      // 普通用户只能更新应用名称
      res = await updateApp({
        id: appId.value as any,
        appName: formData.appName
      })
    }
    
    if (res.data.code === 0) {
      message.success('保存成功')
      router.back()  // 返回上一页
    } else {
      message.error('保存失败：' + res.data.message)
    }
  } catch (error) {
    message.error('保存失败')
  } finally {
    submitting.value = false
  }
}

/**
 * 删除应用
 * 
 * 使用 Modal.confirm 显示确认对话框
 */
const handleDelete = () => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除这个应用吗？此操作不可恢复。',
    okText: '确定',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        const res = await deleteApp({ id: appId.value as any })
        if (res.data.code === 0) {
          message.success('删除成功')
          router.push('/')  // 跳转到首页
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
 * 返回上一页
 * 
 * router.back(): 等同于浏览器的后退按钮
 */
const goBack = () => {
  router.back()
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
 * 组件挂载后执行
 */
onMounted(() => {
  // 从路由参数获取应用 ID
  const id = route.params.id
  if (id) {
    appId.value = String(id)
    loadAppInfo()
  }
})
</script>

<style scoped>
.app-edit-page {
  padding: 24px;
  background: #f0f2f5;
  min-height: 100vh;
}

.cover-preview {
  margin-top: 12px;
}

.priority-hint {
  margin-left: 12px;
  color: #999;
  font-size: 12px;
}

.creator-info {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
