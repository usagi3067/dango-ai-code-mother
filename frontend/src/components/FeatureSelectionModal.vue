<!--
  FeatureSelectionModal.vue - 功能选择弹窗组件

  【功能】
  展示 AI 分析的功能列表，用户可勾选需要的功能
  支持修改应用名称、分类，添加补充说明
  支持重新分析和确认生成

  【Props】
  - visible: 控制显示（v-model）
  - appName: AI 生成的应用名称
  - tag: AI 生成的标签
  - features: AI 分析的功能列表
  - loading: 加载状态

  【Events】
  - update:visible: 关闭弹窗
  - confirm: 确认生成
  - reanalyze: 重新分析
-->
<template>
  <a-modal
    :open="visible"
    title="🎯 确认你的应用需求"
    :width="520"
    :footer="null"
    @cancel="$emit('update:visible', false)"
  >
    <a-spin :spinning="loading">
      <a-form layout="vertical">
        <!-- 应用名称 -->
        <a-form-item label="应用名称">
          <a-input v-model:value="localAppName" placeholder="输入应用名称" />
        </a-form-item>

        <!-- 应用分类 -->
        <a-form-item label="应用分类">
          <a-select v-model:value="localTag" :options="tagOptions" placeholder="选择分类" />
        </a-form-item>

        <!-- 功能列表 -->
        <a-form-item label="功能列表">
          <div v-for="(feature, index) in localFeatures" :key="index" style="margin-bottom: 8px;">
            <a-checkbox v-model:checked="feature.checked">
              <span style="font-weight: 500;">{{ feature.name }}</span>
              <a-tag v-if="feature.recommended" color="blue" style="margin-left: 8px;">推荐</a-tag>
            </a-checkbox>
            <div style="margin-left: 24px; color: #999; font-size: 12px;">{{ feature.description }}</div>
          </div>
          <div style="margin-top: 8px; color: #faad14; font-size: 12px;">
            ⚡ 勾选更多功能会增加生成时间
          </div>
        </a-form-item>

        <!-- 补充说明 -->
        <a-form-item label="补充说明（可选）">
          <a-textarea v-model:value="supplement" placeholder="添加补充说明..." :rows="2" />
        </a-form-item>
      </a-form>

      <!-- 底部按钮 -->
      <div style="display: flex; justify-content: flex-end; gap: 8px;">
        <a-button @click="handleReanalyze" :loading="loading">重新分析</a-button>
        <a-button type="primary" @click="handleConfirm" :loading="loading">确认生成</a-button>
      </div>
    </a-spin>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { APP_TAG_OPTIONS } from '@/config/appTag'

/**
 * 定义组件 Props
 */
const props = defineProps<{
  visible: boolean
  appName: string
  tag: string
  features: API.FeatureItemVO[]
  loading: boolean
}>()

/**
 * 定义组件 Events
 */
const emit = defineEmits<{
  'update:visible': [value: boolean]
  confirm: [payload: { appName: string; tag: string; initPrompt: string }]
  reanalyze: [supplement: string]
}>()

const localAppName = ref('')
const localTag = ref('')
const localFeatures = ref<Array<{ name: string; description: string; checked: boolean; recommended: boolean }>>([])
const supplement = ref('')

const tagOptions = APP_TAG_OPTIONS

// 监听 props 变化，同步到本地状态
watch(() => props.appName, (val) => { localAppName.value = val }, { immediate: true })
watch(() => props.tag, (val) => { localTag.value = val }, { immediate: true })
watch(() => props.features, (val) => {
  localFeatures.value = (val || []).map(f => ({
    name: f.name || '',
    description: f.description || '',
    checked: f.checked ?? false,
    recommended: f.recommended ?? false,
  }))
}, { immediate: true, deep: true })

const handleReanalyze = () => {
  emit('reanalyze', supplement.value)
}

const handleConfirm = () => {
  const selectedFeatures = localFeatures.value.filter(f => f.checked)
  if (selectedFeatures.length === 0) {
    return
  }

  // 组装 initPrompt
  // 注意：原始提示词由父组件管理，这里只组装功能列表部分
  // 父组件会用 originalPrompt + 这里的功能列表来组装完整的 initPrompt
  let initPrompt = ''

  // 功能列表
  initPrompt += '\n\n## 需要实现的功能：\n'
  selectedFeatures.forEach((f, i) => {
    initPrompt += `${i + 1}. ${f.name}：${f.description}\n`
  })

  // 补充说明
  if (supplement.value.trim()) {
    initPrompt += `\n## 补充说明：\n${supplement.value.trim()}\n`
  }

  initPrompt += '\n## 注意：只实现以上列出的功能，不要添加额外页面或功能。\n'

  emit('confirm', {
    appName: localAppName.value,
    tag: localTag.value,
    initPrompt: initPrompt,
  })
}
</script>
