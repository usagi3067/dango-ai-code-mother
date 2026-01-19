<!--
  UserManagePage.vue - 用户管理页面
  功能：管理员可以查看、编辑、删除用户
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
        页面说明
      -->
      <p class="description">
        这是用户管理页面，只有管理员才能访问。
      </p>
      <p class="description">
        在这里可以查看所有用户信息，进行用户管理操作。
      </p>
      
      <!-- 
        占位内容
        实际项目中，这里应该是用户列表表格
      -->
      <a-divider />
      
      <a-alert
        message="功能开发中"
        description="用户列表、编辑、删除等功能正在开发中，敬请期待。"
        type="info"
        show-icon
      />
    </a-card>
  </div>
</template>

<script setup lang="ts">
/**
 * 用户管理页面
 * 
 * 这是一个占位页面，展示权限控制的效果
 * 
 * 实际项目中应该包含：
 * 1. 用户列表表格
 * 2. 搜索和筛选功能
 * 3. 编辑用户信息
 * 4. 删除用户
 * 5. 分页功能
 * 
 * 示例代码：
 * ```typescript
 * import { ref, onMounted } from 'vue'
 * import { listUserVoByPage } from '@/api/userController'
 * 
 * const userList = ref([])
 * const loading = ref(false)
 * 
 * const loadUsers = async () => {
 *   loading.value = true
 *   try {
 *     const res = await listUserVoByPage({
 *       current: 1,
 *       pageSize: 10
 *     })
 *     if (res.data.code === 0) {
 *       userList.value = res.data.data.records
 *     }
 *   } finally {
 *     loading.value = false
 *   }
 * }
 * 
 * onMounted(() => {
 *   loadUsers()
 * })
 * ```
 */
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

/* 描述文字样式 */
.description {
  color: #666;                /* 灰色文字 */
  font-size: 14px;            /* 字体大小 14px */
  line-height: 1.8;           /* 行高 1.8 倍 */
  margin-bottom: 8px;         /* 底部外边距 8px */
}
</style>
