<!--
  AboutPage.vue - 关于页面
  技术博客风格，展示平台核心技术亮点
-->
<template>
  <div class="about-page">
    <!-- 区块 1：项目概览 -->
    <a-card title="关于 AI 应用生成平台" :bordered="false">
      <a-descriptions :column="1" bordered>
        <a-descriptions-item label="项目名称">AI 应用生成平台</a-descriptions-item>
        <a-descriptions-item label="版本">1.0.0</a-descriptions-item>
        <a-descriptions-item label="作者">
          <a href="https://github.com/usagi3067" target="_blank" rel="noopener noreferrer">dango</a>
        </a-descriptions-item>
        <a-descriptions-item label="定位">
          基于 LangGraph4j 工作流的 AI 代码生成平台，通过自然语言描述快速生成完整的 Vue 3 Web 应用
        </a-descriptions-item>
        <a-descriptions-item label="技术栈">
          <div class="tech-stack-group">
            <div class="tech-stack-row">
              <span class="tech-label">前端</span>
              <a-tag color="green">Vue 3</a-tag>
              <a-tag color="green">TypeScript</a-tag>
              <a-tag color="green">Ant Design Vue</a-tag>
              <a-tag color="green">Vite</a-tag>
            </div>
            <div class="tech-stack-row">
              <span class="tech-label">后端</span>
              <a-tag color="blue">Java 21</a-tag>
              <a-tag color="blue">Spring Boot 3.5</a-tag>
              <a-tag color="blue">LangChain4j</a-tag>
              <a-tag color="blue">LangGraph4j</a-tag>
              <a-tag color="blue">MyBatis-Flex</a-tag>
              <a-tag color="blue">Dubbo 3.3</a-tag>
            </div>
            <div class="tech-stack-row">
              <span class="tech-label">基础设施</span>
              <a-tag color="purple">Nacos</a-tag>
              <a-tag color="purple">Higress</a-tag>
              <a-tag color="purple">Supabase</a-tag>
              <a-tag color="purple">SkyWalking</a-tag>
              <a-tag color="purple">Prometheus</a-tag>
              <a-tag color="purple">Grafana</a-tag>
              <a-tag color="purple">ELK</a-tag>
            </div>
          </div>
        </a-descriptions-item>
      </a-descriptions>
    </a-card>

    <!-- 区块 2：AI 代码生成工作流 -->
    <a-card title="AI 代码生成工作流" :bordered="false" style="margin-top: 24px">
      <a-typography-paragraph type="secondary">
        基于 LangGraph4j 构建的多阶段工作流，支持子图编排、并发执行、条件分流和循环修复。
      </a-typography-paragraph>

      <a-divider orientation="left">创建流程（Vue 应用）</a-divider>
      <a-timeline>
        <a-timeline-item color="blue">
          <template #dot><RocketOutlined /></template>
          <strong>图片规划 (image_plan)</strong>
          <br />AI 分析用户需求，规划需要收集的图片素材类型
        </a-timeline-item>
        <a-timeline-item color="blue">
          <template #dot><PictureOutlined /></template>
          <strong>并发图片收集（4 路并发）</strong>
          <br />
          <a-space :size="4" wrap>
            <a-tag>content_image_collector</a-tag>
            <a-tag>illustration_collector</a-tag>
            <a-tag>diagram_collector</a-tag>
            <a-tag>logo_collector</a-tag>
          </a-space>
        </a-timeline-item>
        <a-timeline-item color="blue">
          <template #dot><AppstoreOutlined /></template>
          <strong>图片聚合 (image_aggregator)</strong>
          <br />汇总所有收集到的图片素材
        </a-timeline-item>
        <a-timeline-item color="green">
          <template #dot><ThunderboltOutlined /></template>
          <strong>Prompt 增强 (prompt_enhancer)</strong>
          <br />结合图片素材和用户需求，生成增强的代码生成提示词
        </a-timeline-item>
        <a-timeline-item color="green">
          <template #dot><CodeOutlined /></template>
          <strong>代码生成 (code_generator)</strong>
          <br />AI 生成完整的 Vue 3 应用代码
        </a-timeline-item>
      </a-timeline>

      <a-divider orientation="left">修改流程（已有代码）</a-divider>
      <a-timeline>
        <a-timeline-item color="orange">
          <template #dot><ReadOutlined /></template>
          <strong>代码读取 (code_reader)</strong>
          <br />读取现有项目结构和代码
        </a-timeline-item>
        <a-timeline-item color="orange">
          <template #dot><BulbOutlined /></template>
          <strong>意图识别 (intent_classifier)</strong>
          <br />AI 判断用户意图：修改代码 or 纯问答
        </a-timeline-item>
        <a-timeline-item color="red">
          <template #dot><FileSearchOutlined /></template>
          <strong>修改规划 (modification_planner)</strong>
          <a-tag color="red" style="margin-left: 8px">MODIFY 分支</a-tag>
          <br />全局决策：分析需求，制定 SQL + 代码修改计划
        </a-timeline-item>
        <a-timeline-item color="red">
          <template #dot><DatabaseOutlined /></template>
          <strong>数据库操作 (database_operator)</strong>
          <br />执行 SQL（有 SQL 时），获取最新 Schema；无 SQL 则跳过
        </a-timeline-item>
        <a-timeline-item color="red">
          <template #dot><EditOutlined /></template>
          <strong>代码修改 (code_modifier)</strong>
          <br />按照修改计划执行代码变更
        </a-timeline-item>
        <a-timeline-item color="gray">
          <template #dot><MessageOutlined /></template>
          <strong>QA 问答 (qa_node)</strong>
          <a-tag style="margin-left: 8px">QA 分支</a-tag>
          <br />纯问答分支，直接回答用户问题，跳过构建检查
        </a-timeline-item>
      </a-timeline>

      <a-divider orientation="left">构建检查修复</a-divider>
      <a-timeline>
        <a-timeline-item color="green">
          <template #dot><CheckCircleOutlined /></template>
          <strong>构建检查 (build_check)</strong>
          <br />自动执行 npm run build，检查构建是否通过
        </a-timeline-item>
        <a-timeline-item color="red">
          <template #dot><ToolOutlined /></template>
          <strong>自动修复 (code_fixer)</strong>
          <br />构建失败时 AI 自动分析错误并修复，循环直到通过或达到上限
        </a-timeline-item>
      </a-timeline>
    </a-card>

    <!-- 区块 3：数据库集成 -->
    <a-card title="Supabase 数据库集成" :bordered="false" style="margin-top: 24px">
      <a-typography-paragraph type="secondary">
        为生成的应用提供数据持久化能力，AI 自动建表、改表、生成 CRUD 代码。
      </a-typography-paragraph>

      <a-divider orientation="left">Schema 隔离策略</a-divider>
      <div class="code-block">
        <pre>Supabase Project
├── public (系统默认)
├── app_{appId_1}/
│   ├── todos
│   └── categories
├── app_{appId_2}/
│   ├── products
│   └── orders
└── ...</pre>
      </div>
      <a-typography-paragraph style="margin-top: 12px">
        每个应用独立 Schema，前端通过 <a-tag>Accept-Profile</a-tag> 头指定 Schema，天然隔离。
      </a-typography-paragraph>

      <a-divider orientation="left">工作流集成</a-divider>
      <a-steps :current="3" size="small" style="margin-bottom: 16px">
        <a-step title="ModificationPlanner" description="分析需求，输出 SQL + 代码修改计划" />
        <a-step title="DatabaseOperator" description="执行建表/改表 SQL，获取最新 Schema" />
        <a-step title="CodeModifier" description="基于最新 Schema 生成前端 CRUD 代码" />
      </a-steps>

      <a-divider orientation="left">基础设施文件保护</a-divider>
      <a-typography-paragraph>
        工具层硬保护，防止 AI 覆盖关键文件：
      </a-typography-paragraph>
      <a-space wrap>
        <a-tag color="error">index.html</a-tag>
        <a-tag color="error">package.json</a-tag>
        <a-tag color="error">vite.config.js</a-tag>
        <a-tag color="error">src/main.js</a-tag>
      </a-space>
    </a-card>

    <!-- 区块 4：基础设施与微服务治理 -->
    <a-card title="基础设施与微服务治理" :bordered="false" style="margin-top: 24px">
      <a-list :data-source="infraFeatures" :split="false">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta>
              <template #avatar>
                <a-avatar :style="{ backgroundColor: item.color }">
                  <template #icon><component :is="item.icon" /></template>
                </a-avatar>
              </template>
              <template #title>{{ item.title }}</template>
              <template #description>{{ item.description }}</template>
            </a-list-item-meta>
          </a-list-item>
        </template>
      </a-list>
    </a-card>

    <!-- 区块 5：更多生成模式 -->
    <a-card title="更多生成模式" :bordered="false" style="margin-top: 24px">
      <a-list :data-source="moreModes" :split="false">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta>
              <template #avatar>
                <a-avatar :style="{ backgroundColor: item.color }">
                  <template #icon><component :is="item.icon" /></template>
                </a-avatar>
              </template>
              <template #title>{{ item.title }}</template>
              <template #description>{{ item.description }}</template>
            </a-list-item-meta>
          </a-list-item>
        </template>
      </a-list>
    </a-card>

    <!-- 区块 6：完整工作流架构图 -->
    <a-card title="完整工作流架构图" :bordered="false" style="margin-top: 24px">
      <div class="workflow-diagram">
        <pre>{{ workflowDiagram }}</pre>
      </div>
    </a-card>

    <!-- 联系方式 -->
    <a-card title="联系方式" :bordered="false" style="margin-top: 24px">
      <a-space direction="vertical" size="middle">
        <div>
          <strong>GitHub：</strong>
          <a href="https://github.com/usagi3067" target="_blank" rel="noopener noreferrer">
            https://github.com/usagi3067
          </a>
        </div>
        <div>
          <strong>邮箱：</strong>
          <a href="mailto:contact@example.com">contact@example.com</a>
        </div>
      </a-space>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import {
  RocketOutlined,
  PictureOutlined,
  AppstoreOutlined,
  ThunderboltOutlined,
  CodeOutlined,
  ReadOutlined,
  BulbOutlined,
  FileSearchOutlined,
  DatabaseOutlined,
  EditOutlined,
  MessageOutlined,
  CheckCircleOutlined,
  ToolOutlined,
  CloudServerOutlined,
  SendOutlined,
  EyeOutlined,
  ClusterOutlined,
  PlayCircleOutlined,
  ExperimentOutlined,
} from '@ant-design/icons-vue'
import { h } from 'vue'

const infraFeatures = [
  {
    icon: CloudServerOutlined,
    color: '#722ed1',
    title: 'Higress AI Gateway',
    description: '多模型统一接入（Anthropic / DeepSeek / Kimi），OpenAI 兼容协议，Failover 故障转移',
  },
  {
    icon: SendOutlined,
    color: '#1890ff',
    title: '工具流式输出',
    description: 'AI 生成代码时实时流式展示（TOOL_STREAMING），用户可以看到 AI 正在写的每一行代码',
  },
  {
    icon: EyeOutlined,
    color: '#52c41a',
    title: '可观测性',
    description: 'SkyWalking 分布式追踪 + Prometheus 指标采集 + Grafana 监控面板 + Elasticsearch + Kibana 日志',
  },
  {
    icon: ClusterOutlined,
    color: '#fa8c16',
    title: '服务治理',
    description: 'Nacos 服务注册与配置中心 + Dubbo 3.3 RPC 通信 + SA-Token 认证鉴权',
  },
]

const moreModes = [
  {
    icon: PlayCircleOutlined,
    color: '#1890ff',
    title: '力扣题解模式',
    description: '输入题号，AI 自动生成 React + GSAP 交互式算法动画题解（Advisor → Prompt 增强 → 代码生成）',
  },
  {
    icon: ExperimentOutlined,
    color: '#eb2f96',
    title: '面试题解模式',
    description: '输入面试题，AI 自动生成带图解的可视化讲解页面（Advisor → Prompt 增强 → 代码生成）',
  },
]

const workflowDiagram = `┌─────────────────────────────────────────────────────────────────────────────────┐
│                              主工作流 (Main Workflow)                            │
│                                                                                 │
│  START → mode_router → ┬─ [create] ──────────────────────────────┐              │
│                         ├─ [leetcode_create] ─────────────────────┤              │
│                         ├─ [interview_create] ────────────────────┤              │
│                         └─ [existing_code] ──┬─ [MODIFY] ────────┤              │
│                                              └─ [QA] ──→ END     │              │
│                                                                   ↓              │
│                                                          build_check_subgraph    │
│                                                                   ↓              │
│                                                                  END             │
└─────────────────────────────────────────────────────────────────────────────────┘

┌─ 创建模式子图 (Vue 应用) ───────────────────────────────────────────────────────┐
│                                                                                 │
│                         ┌→ content_image_collector ─┐                           │
│  image_plan ───────────→├→ illustration_collector  ─┼→ image_aggregator         │
│                         ├→ diagram_collector       ─┘        ↓                  │
│                         └→ logo_collector ──────────→  prompt_enhancer           │
│                                                              ↓                  │
│                                                       code_generator            │
└─────────────────────────────────────────────────────────────────────────────────┘

┌─ 已有代码子图 (修改/问答) ──────────────────────────────────────────────────────┐
│                                                                                 │
│  code_reader → intent_classifier → ┬─ [MODIFY] → modification_planner          │
│                                    │                     ↓                      │
│                                    │              ┌─ [has_sql] → database_operator│
│                                    │              └─ [skip_sql] ─┐               │
│                                    │                     ↓       ↓               │
│                                    │               code_modifier ←┘              │
│                                    └─ [QA] → qa_node → END                      │
└─────────────────────────────────────────────────────────────────────────────────┘

┌─ 构建检查修复子图 ──────────────────────────────────────────────────────────────┐
│                                                                                 │
│  build_check ──→ [FAIL & 未达上限] ──→ code_fixer ──→ build_check (循环)        │
│       ↓                                                                         │
│  [PASS] → END                                                                   │
└─────────────────────────────────────────────────────────────────────────────────┘

┌─ 力扣创建子图 ──────────────────────────────────────────────────────────────────┐
│  animation_advisor → leetcode_prompt_enhancer → code_generator                  │
└─────────────────────────────────────────────────────────────────────────────────┘

┌─ 面试题解创建子图 ──────────────────────────────────────────────────────────────┐
│  interview_animation_advisor → interview_prompt_enhancer → code_generator       │
└─────────────────────────────────────────────────────────────────────────────────┘`
</script>

<style scoped>
.about-page {
  max-width: 1000px;
  margin: 0 auto;
}

.tech-stack-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tech-stack-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.tech-label {
  display: inline-block;
  width: 56px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.65);
  flex-shrink: 0;
}

.code-block {
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  padding: 16px;
  overflow-x: auto;
}

.code-block pre {
  margin: 0;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 13px;
  line-height: 1.6;
  color: #333;
}

.workflow-diagram {
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  padding: 16px;
  overflow-x: auto;
}

.workflow-diagram pre {
  margin: 0;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
  line-height: 1.5;
  color: #333;
  white-space: pre;
}

@media (max-width: 768px) {
  .about-page {
    padding: 0 16px;
  }
}
</style>