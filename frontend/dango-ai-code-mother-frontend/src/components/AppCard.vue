<!--
  AppCard.vue - 应用卡片组件
  
  【功能】
  展示应用的封面、名称、作者等信息
  支持悬浮显示操作按钮
  
  【Props】
  - app: 应用数据对象
  
  【Events】
  - viewChat: 点击查看对话
  - viewWork: 点击查看作品
-->
<template>
  <div class="app-card">
    <!-- 应用封面 -->
    <div class="app-cover">
      <img v-if="app.cover" :src="app.cover" alt="封面" />
      <div v-else class="app-cover-placeholder">
        <AppstoreOutlined />
      </div>
      
      <!-- 悬浮操作按钮 -->
      <div class="app-card-overlay">
        <a-button type="primary" size="small" @click="$emit('viewChat', app)">
          <template #icon><MessageOutlined /></template>
          查看对话
        </a-button>
        <a-button 
          v-if="app.deployKey" 
          size="small" 
          @click="$emit('viewWork', app)"
        >
          <template #icon><ExportOutlined /></template>
          查看作品
        </a-button>
      </div>
    </div>
    
    <!-- 应用信息 -->
    <div class="app-info">
      <div class="app-info-layout">
        <UserAvatar 
          :src="app.user?.userAvatar" 
          :name="app.user?.userName"
          :size="36"
        />
        <div class="app-info-right">
          <div class="app-name-row">
            <span class="app-name">{{ app.appName || '未命名应用' }}</span>
            <a-tag :color="getAppTagColor(app.tag)" size="small">
              {{ getAppTagLabel(app.tag) }}
            </a-tag>
          </div>
          <div class="app-author">{{ app.user?.userName || '匿名用户' }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { AppstoreOutlined, MessageOutlined, ExportOutlined } from '@ant-design/icons-vue'
import UserAvatar from '@/components/UserAvatar.vue'
import { getAppTagColor, getAppTagLabel } from '@/config/appTag'

/**
 * 定义组件 Props
 */
defineProps<{
  app: API.AppVO
}>()

/**
 * 定义组件 Events
 */
defineEmits<{
  viewChat: [app: API.AppVO]
  viewWork: [app: API.AppVO]
}>()
</script>

<style scoped>
.app-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.app-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

/* 应用封面 */
.app-cover {
  height: 160px;
  background: #f5f7fa;
  overflow: hidden;
  position: relative;
}

.app-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.app-cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  color: #d9d9d9;
}

/* 悬浮操作层 */
.app-card-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  opacity: 0;
  transition: opacity 0.3s;
}

.app-card:hover .app-card-overlay {
  opacity: 1;
}

/* 应用信息区域 */
.app-info {
  padding: 12px 16px;
}

.app-info-layout {
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-info-right {
  flex: 1;
  min-width: 0;
}

.app-name {
  font-size: 15px;
  font-weight: 500;
  color: #1a1a1a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}

.app-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.app-name-row :deep(.ant-tag) {
  flex-shrink: 0;
  margin: 0;
  font-size: 12px;
  line-height: 18px;
  padding: 0 6px;
}

.app-author {
  font-size: 13px;
  color: #999;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
