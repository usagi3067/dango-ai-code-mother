<template>
  <div class="interview-page">
    <div class="page-container">
      <div class="header-section">
        <h1 class="page-title">面试题解生成器</h1>
        <p class="page-desc">输入面试题目和参考答案，AI 自动生成交互式可视化讲解</p>
        <div class="mode-switch">
          <a-segmented
            v-model:value="selectedMode"
            :options="modeOptions"
          />
        </div>
        <div class="input-area">
          <a-input
            v-model:value="questionTitle"
            placeholder="输入面试题目，如：讲一下 JVM 的内存结构"
            class="title-input"
            @pressEnter="handleGenerate"
          />
          <a-textarea
            v-model:value="referenceAnswer"
            placeholder="输入参考答案（支持 Markdown 格式）"
            :rows="6"
            class="answer-input"
          />
          <a-button
            type="primary"
            :loading="creating"
            class="generate-btn"
            @click="handleGenerate"
          >
            生成可视化题解
          </a-button>
        </div>
      </div>

      <div class="list-section">
        <h2 class="section-title">已生成的题解</h2>
        <div v-if="appList.length > 0" class="app-grid">
          <AppCard
            v-for="app in appList"
            :key="String(app.id)"
            :app="app"
            @view-chat="goToAppChat"
            @view-work="openDeployedApp"
          />
        </div>

        <div class="load-status">
          <a-spin v-if="loading" />
          <span v-else-if="!hasMore && appList.length > 0" class="no-more">没有更多了</span>
          <a-empty v-else-if="!loading && appList.length === 0" description="暂无题解，输入题目开始生成吧" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { addApp, listMyAppByCursor } from '@/api/app/appController'
import AppCard from '@/components/AppCard.vue'
import { getDeployUrl } from '@/config/env'
import { useInfiniteScroll } from '@/composables/useInfiniteScroll'
import { CodeGenTypeEnum } from '@/config/codeGenType'

const router = useRouter()
const questionTitle = ref('')
const referenceAnswer = ref('')
const creating = ref(false)

const selectedMode = ref<string>(CodeGenTypeEnum.INTERVIEW_PROJECT)
const modeOptions = [
  { label: '图解动画', value: CodeGenTypeEnum.INTERVIEW_PROJECT },
  { label: '源码剧场', value: CodeGenTypeEnum.INTERVIEW_SOURCE_CODE_PROJECT },
]

const { items: appList, loading, hasMore, reset } = useInfiniteScroll({
  pageSize: 12,
  fetchFn: async (lastId) => {
    const res = await listMyAppByCursor({
      lastId: lastId,
      pageSize: 12,
      tag: 'interview',
      codeGenType: selectedMode.value
    })
    if (res.data.code === 0 && res.data.data) {
      return res.data.data.records || []
    }
    return []
  }
})

watch(selectedMode, () => {
  reset()
})

const handleGenerate = async () => {
  if (!questionTitle.value.trim()) {
    message.warning('请输入面试题目')
    return
  }
  if (!referenceAnswer.value.trim()) {
    message.warning('请输入参考答案')
    return
  }
  creating.value = true
  try {
    const initPrompt = `请基于以下面试题目和参考答案，生成交互式可视化讲解页面：

## 题目
${questionTitle.value.trim()}

## 参考答案
${referenceAnswer.value.trim()}`

    const res = await addApp({
      initPrompt,
      appName: questionTitle.value.trim().slice(0, 50),
      tag: 'interview',
      codeGenType: selectedMode.value
    })
    if (res.data.code === 0 && res.data.data) {
      const appId = String(res.data.data)
      message.success('题解创建成功')
      router.push(`/app/chat/${appId}?autoSend=1`)
    } else {
      message.error('创建失败：' + res.data.message)
    }
  } catch (e) {
    message.error('创建失败，请稍后重试')
  } finally {
    creating.value = false
  }
}

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
.interview-page {
  padding: 0;
  background: linear-gradient(180deg, #f0e8f4 0%, #f5f7fa 30%);
  min-height: calc(100vh - 64px - 72px);
}

.page-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 48px 24px;
}

.header-section {
  text-align: center;
  margin-bottom: 48px;
}

.page-title {
  font-size: 36px;
  font-weight: 700;
  background: linear-gradient(135deg, #1a1a1a 0%, #4a4a4a 100%);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin-bottom: 8px;
}

.page-desc {
  font-size: 16px;
  color: #666;
  margin-bottom: 20px;
}

.mode-switch {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
}

.input-area {
  max-width: 600px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.title-input {
  font-size: 16px;
}

.answer-input {
  font-size: 14px;
}

.generate-btn {
  background: linear-gradient(135deg, #9b59b6 0%, #8e44ad 100%);
  border: none;
  height: 40px;
  font-size: 16px;
}

.generate-btn:hover {
  background: linear-gradient(135deg, #8e44ad 0%, #7d3c98 100%);
}

.list-section {
  margin-top: 16px;
}

.section-title {
  font-size: 24px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 24px;
}

.app-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.load-status {
  display: flex;
  justify-content: center;
  padding: 32px 0;
}

.no-more {
  color: #999;
  font-size: 14px;
}

@media (max-width: 992px) {
  .app-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 576px) {
  .page-title {
    font-size: 24px;
  }

  .app-grid {
    grid-template-columns: 1fr;
  }
}
</style>
