<template>
  <div class="interview-page">
    <div class="page-container">
      <div class="header-section">
        <h1 class="page-title">面试题解生成器</h1>
        <p class="page-desc">输入面试题目和参考答案，AI 自动生成交互式可视化讲解</p>
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
        <a-spin :spinning="loading">
          <div v-if="appList.length > 0" class="app-grid">
            <a-card
              v-for="app in appList"
              :key="String(app.id)"
              hoverable
              class="app-card"
              @click="handleClickApp(app)"
            >
              <template #title>
                <span class="card-title">{{ app.appName }}</span>
              </template>
              <div class="card-time">{{ formatTime(app.createTime) }}</div>
            </a-card>
          </div>
          <a-empty v-else description="暂无题解，输入题目开始生成吧" />
        </a-spin>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { addApp, listMyAppVoByPage } from '@/api/app/appController'

const router = useRouter()
const questionTitle = ref('')
const referenceAnswer = ref('')
const creating = ref(false)

const appList = ref<API.AppVO[]>([])
const loading = ref(false)

const loadApps = async () => {
  loading.value = true
  try {
    const res = await listMyAppVoByPage({
      pageNum: 1,
      pageSize: 50,
      tag: 'interview',
      codeGenType: 'interview_project',
      sortField: 'createTime',
      sortOrder: 'desc'
    })
    if (res.data.code === 0 && res.data.data) {
      appList.value = res.data.data.records || []
    }
  } catch (e) {
    // ignore
  } finally {
    loading.value = false
  }
}

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
      codeGenType: 'interview_project'
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

const handleClickApp = (app: API.AppVO) => {
  router.push(`/app/chat/${String(app.id)}`)
}

const formatTime = (dateStr: string | undefined) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

onMounted(() => {
  loadApps()
})
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
  margin-bottom: 32px;
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

.app-card {
  cursor: pointer;
  transition: all 0.2s;
}

.app-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.card-title {
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-time {
  color: #999;
  font-size: 13px;
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
