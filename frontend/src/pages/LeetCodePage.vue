<template>
  <div class="leetcode-page">
    <div class="page-container">
      <!-- 顶部输入区 -->
      <div class="header-section">
        <h1 class="page-title">力扣题解生成器</h1>
        <p class="page-desc">输入力扣题号，AI 自动生成算法可视化题解</p>
        <div class="input-row">
          <a-input-number
            v-model:value="problemNumber"
            :min="1"
            :max="3000"
            placeholder="输入题号 (1-3000)"
            class="problem-input"
            @pressEnter="handleGenerate"
          />
          <a-button
            type="primary"
            :loading="creating"
            class="generate-btn"
            @click="handleGenerate"
          >
            生成题解
          </a-button>
        </div>
      </div>

      <!-- 已生成的题解列表 -->
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
          <a-empty v-else description="暂无题解，输入题号开始生成吧" />
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
const problemNumber = ref<number>()
const creating = ref(false)

const appList = ref<API.AppVO[]>([])
const loading = ref(false)

const loadApps = async () => {
  loading.value = true
  try {
    const res = await listMyAppVoByPage({
      pageNum: 1,
      pageSize: 50,
      tag: 'algorithm',
      codeGenType: 'leetcode_project',
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
  if (!problemNumber.value) {
    message.warning('请输入力扣题号')
    return
  }
  creating.value = true
  try {
    const res = await addApp({
      initPrompt: `请生成力扣第 ${problemNumber.value} 题的算法可视化题解`,
      appName: `LC-${problemNumber.value}`,
      tag: 'algorithm',
      codeGenType: 'leetcode_project'
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
.leetcode-page {
  padding: 0;
  background: linear-gradient(180deg, #e8f0f4 0%, #f5f7fa 30%);
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

.input-row {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
}

.problem-input {
  width: 220px;
}

.generate-btn {
  background: linear-gradient(135deg, #52c4a0 0%, #3db389 100%);
  border: none;
}

.generate-btn:hover {
  background: linear-gradient(135deg, #45b894 0%, #35a07a 100%);
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

  .input-row {
    flex-direction: column;
  }

  .problem-input {
    width: 100%;
  }
}
</style>
