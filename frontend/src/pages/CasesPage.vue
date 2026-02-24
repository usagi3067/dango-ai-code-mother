<template>
  <div class="cases-page">
    <!-- 简化 Header -->
    <div class="cases-header">
      <div class="header-left">
        <router-link to="/">
          <img src="@/assets/logo.png" alt="Logo" class="header-logo" />
        </router-link>
      </div>

      <div class="header-center">
        <!-- 标签下拉 -->
        <a-select
          v-model:value="tag"
          placeholder="全部分类"
          allow-clear
          style="width: 140px"
          @change="resetAndLoad"
        >
          <a-select-option v-for="t in APP_TAG_OPTIONS" :key="t.value" :value="t.value">
            {{ t.label }}
          </a-select-option>
        </a-select>

        <!-- 搜索框 -->
        <a-input-search
          v-model:value="searchText"
          placeholder="搜索应用"
          style="width: 280px"
          allow-clear
          @search="resetAndLoad"
        />
      </div>

      <div class="header-right">
        <UserDropdown :size="36" />
      </div>
    </div>

    <!-- 应用卡片网格 -->
    <div class="cases-content">
      <div v-if="apps.length > 0" class="app-grid">
        <AppCard
          v-for="app in apps"
          :key="String(app.id)"
          :app="app"
          @view-chat="goToAppChat"
          @view-work="openDeployedApp"
        />
      </div>

      <!-- 加载状态 -->
      <div class="load-status">
        <a-spin v-if="loading" />
        <span v-else-if="!hasMore && apps.length > 0" class="no-more">没有更多了</span>
        <a-empty v-else-if="!loading && apps.length === 0" description="暂无应用" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import AppCard from '@/components/AppCard.vue'
import UserDropdown from '@/components/UserDropdown.vue'
import { APP_TAG_OPTIONS } from '@/config/appTag'
import { getDeployUrl } from '@/config/env'
import { listAppByCursor } from '@/api/app/appController'
import { useInfiniteScroll } from '@/composables/useInfiniteScroll'

const router = useRouter()
const tag = ref<string | null>(null)
const searchText = ref('')

const { items: apps, loading, hasMore, reset: resetAndLoad } = useInfiniteScroll({
  pageSize: 12,
  fetchFn: async (lastId) => {
    const res = await listAppByCursor({
      lastId: lastId,
      pageSize: 12,
      tag: tag.value || undefined,
      searchText: searchText.value || undefined
    })
    if (res.data.code === 0 && res.data.data) {
      return res.data.data.records || []
    }
    return []
  }
})

const goToAppChat = (app: API.AppVO) => {
  router.push(`/app/chat/${String(app.id)}`)
}

const openDeployedApp = (app: API.AppVO) => {
  if (app.deployKey) {
    window.open(getDeployUrl(app.deployKey), '_blank')
  }
}
</script>

<style scoped>
.cases-page {
  min-height: 100vh;
  background: #f5f7fa;
}

/* Header 样式 */
.cases-header {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.header-left {
  display: flex;
  align-items: center;
}

.header-logo {
  height: 36px;
  cursor: pointer;
}

.header-center {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-right {
  display: flex;
  align-items: center;
}

/* 内容区域 */
.cases-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 32px 24px;
}

/* 应用卡片网格 */
.app-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

/* 加载状态 */
.load-status {
  display: flex;
  justify-content: center;
  padding: 32px 0;
}

.no-more {
  color: #999;
  font-size: 14px;
}

/* 响应式布局 */
@media (max-width: 1200px) {
  .app-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 992px) {
  .app-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .header-center {
    flex-direction: column;
    gap: 8px;
  }
}

@media (max-width: 576px) {
  .app-grid {
    grid-template-columns: 1fr;
  }

  .cases-header {
    flex-wrap: wrap;
    gap: 12px;
  }

  .header-center {
    order: 3;
    width: 100%;
  }

  .header-center :deep(.ant-select),
  .header-center :deep(.ant-input-search) {
    width: 100% !important;
  }
}
</style>
