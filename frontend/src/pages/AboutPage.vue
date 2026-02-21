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
      <div class="flow-tree">
        <div class="flow-node flow-node--blue">
          <RocketOutlined class="flow-icon" />
          <div class="flow-content">
            <strong>图片规划 (image_plan)</strong>
            <p>AI 分析用户需求，规划需要收集的图片素材类型</p>
          </div>
        </div>
        <div class="flow-arrow">↓</div>
        <div class="flow-parallel">
          <div class="flow-parallel-label"><a-tag color="blue">4 路并发收集</a-tag></div>
          <div class="flow-parallel-line"></div>
          <div class="flow-parallel-items">
            <div class="flow-node flow-node--blue flow-node--sm">
              <PictureOutlined class="flow-icon" />
              <div class="flow-content"><strong>内容图片</strong></div>
            </div>
            <div class="flow-node flow-node--blue flow-node--sm">
              <PictureOutlined class="flow-icon" />
              <div class="flow-content"><strong>插画素材</strong></div>
            </div>
            <div class="flow-node flow-node--blue flow-node--sm">
              <PictureOutlined class="flow-icon" />
              <div class="flow-content"><strong>图表素材</strong></div>
            </div>
            <div class="flow-node flow-node--blue flow-node--sm">
              <PictureOutlined class="flow-icon" />
              <div class="flow-content"><strong>Logo</strong></div>
            </div>
          </div>
          <div class="flow-parallel-line-bottom"></div>
        </div>
        <div class="flow-arrow">↓</div>
        <div class="flow-node flow-node--blue">
          <AppstoreOutlined class="flow-icon" />
          <div class="flow-content">
            <strong>图片聚合 (image_aggregator)</strong>
            <p>汇总所有收集到的图片素材</p>
          </div>
        </div>
        <div class="flow-arrow">↓</div>
        <div class="flow-node flow-node--green">
          <ThunderboltOutlined class="flow-icon" />
          <div class="flow-content">
            <strong>Prompt 增强 (prompt_enhancer)</strong>
            <p>结合图片素材和用户需求，生成增强的代码生成提示词</p>
          </div>
        </div>
        <div class="flow-arrow">↓</div>
        <div class="flow-node flow-node--green">
          <CodeOutlined class="flow-icon" />
          <div class="flow-content">
            <strong>代码生成 (code_generator)</strong>
            <p>AI 生成完整的 Vue 3 应用代码</p>
          </div>
        </div>
      </div>

      <a-divider orientation="left">修改流程（已有代码）</a-divider>
      <div class="flow-tree">
        <div class="flow-node flow-node--orange">
          <ReadOutlined class="flow-icon" />
          <div class="flow-content">
            <strong>代码读取 (code_reader)</strong>
            <p>读取现有项目结构和代码</p>
          </div>
        </div>
        <div class="flow-arrow">↓</div>
        <div class="flow-node flow-node--orange">
          <BulbOutlined class="flow-icon" />
          <div class="flow-content">
            <strong>意图识别 (intent_classifier)</strong>
            <p>AI 判断用户意图：修改代码 or 纯问答</p>
          </div>
        </div>
        <div class="flow-branch">
          <div class="flow-branch-line"></div>
          <div class="flow-branch-sides">
            <div class="flow-branch-left">
              <div class="flow-branch-label"><a-tag color="red">MODIFY 分支</a-tag></div>
              <div class="flow-node flow-node--red">
                <FileSearchOutlined class="flow-icon" />
                <div class="flow-content">
                  <strong>修改规划 (modification_planner)</strong>
                  <p>全局决策：制定 SQL + 代码修改计划</p>
                </div>
              </div>
              <div class="flow-arrow">↓</div>
              <div class="flow-node flow-node--red">
                <DatabaseOutlined class="flow-icon" />
                <div class="flow-content">
                  <strong>数据库操作 (database_operator)</strong>
                  <p>执行 SQL（有则执行，无则跳过）</p>
                </div>
              </div>
              <div class="flow-arrow">↓</div>
              <div class="flow-node flow-node--red">
                <EditOutlined class="flow-icon" />
                <div class="flow-content">
                  <strong>代码修改 (code_modifier)</strong>
                  <p>按照修改计划执行代码变更</p>
                </div>
              </div>
              <div class="flow-arrow">↓</div>
              <div class="flow-node flow-node--green">
                <CheckCircleOutlined class="flow-icon" />
                <div class="flow-content">
                  <strong>→ 构建检查修复子图</strong>
                </div>
              </div>
            </div>
            <div class="flow-branch-right">
              <div class="flow-branch-label"><a-tag>QA 分支</a-tag></div>
              <div class="flow-node flow-node--gray">
                <MessageOutlined class="flow-icon" />
                <div class="flow-content">
                  <strong>QA 问答 (qa_node)</strong>
                  <p>直接回答用户问题</p>
                </div>
              </div>
              <div class="flow-arrow">↓</div>
              <div class="flow-node flow-node--gray">
                <div class="flow-content">
                  <strong>→ END（跳过构建检查）</strong>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <a-divider orientation="left">构建检查修复</a-divider>
      <div class="flow-tree">
        <div class="flow-loop">
          <div class="flow-loop-body">
            <div class="flow-node flow-node--green">
              <CheckCircleOutlined class="flow-icon" />
              <div class="flow-content">
                <strong>构建检查 (build_check)</strong>
                <p>自动执行 npm run build</p>
              </div>
            </div>
            <div class="flow-branch">
              <div class="flow-branch-line"></div>
              <div class="flow-branch-sides">
                <div class="flow-branch-left">
                  <div class="flow-branch-label"><a-tag color="green">PASS</a-tag></div>
                  <div class="flow-node flow-node--green">
                    <div class="flow-content"><strong>→ END</strong></div>
                  </div>
                </div>
                <div class="flow-branch-right">
                  <div class="flow-branch-label"><a-tag color="red">FAIL & 未达上限</a-tag></div>
                  <div class="flow-node flow-node--red">
                    <ToolOutlined class="flow-icon" />
                    <div class="flow-content">
                      <strong>自动修复 (code_fixer)</strong>
                      <p>AI 分析错误并修复</p>
                    </div>
                  </div>
                  <div class="flow-arrow">↑ 循环回到 build_check</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
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
      <div id="workflow-graph" style="width: 100%; height: 680px"></div>
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
  EyeOutlined,
  ClusterOutlined,
  PlayCircleOutlined,
  ExperimentOutlined,
} from '@ant-design/icons-vue'

const infraFeatures = [
  {
    icon: CloudServerOutlined,
    color: '#722ed1',
    title: 'Higress AI Gateway',
    description: '多模型统一接入（Anthropic / DeepSeek / Kimi），OpenAI 兼容协议，Failover 故障转移',
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

import { onMounted, onUnmounted } from 'vue'
import { Graph } from '@antv/g6'

let graph: Graph | null = null

const COLORS: Record<string, { fill: string; stroke: string; text: string }> = {
  blue: { fill: '#e6f7ff', stroke: '#1890ff', text: '#096dd9' },
  cyan: { fill: '#e6fffb', stroke: '#13c2c2', text: '#08979c' },
  purple: { fill: '#f9f0ff', stroke: '#722ed1', text: '#531dab' },
  green: { fill: '#f6ffed', stroke: '#52c41a', text: '#389e0d' },
  orange: { fill: '#fff7e6', stroke: '#fa8c16', text: '#d46b08' },
  red: { fill: '#fff1f0', stroke: '#f5222d', text: '#cf1322' },
  gray: { fill: '#f5f5f5', stroke: '#d9d9d9', text: '#595959' },
  dark: { fill: '#434343', stroke: '#434343', text: '#ffffff' },
}

function n(id: string, label: string, color: string, combo?: string) {
  const c = COLORS[color]
  const isTerminal = label === 'START' || label === 'END'
  const node: any = {
    id,
    type: isTerminal ? 'circle' : 'rect',
    combo,
    style: {
      labelText: label,
      labelFill: c.text,
      labelFontSize: 11,
      labelFontWeight: 600,
      labelFontFamily: "'SFMono-Regular', Consolas, monospace",
      fill: c.fill,
      stroke: c.stroke,
      lineWidth: 1.5,
    },
  }
  if (isTerminal) {
    node.style.size = 32
    node.style.labelFontSize = 9
  } else {
    // 根据文字长度动态计算宽度
    const width = Math.max(label.length * 8 + 24, 80)
    node.style.size = [width, 28]
    node.style.radius = 4
  }
  return node
}

function e(source: string, target: string, label?: string, color?: string) {
  const edge: any = {
    source,
    target,
    style: {
      stroke: color || '#d9d9d9',
      lineWidth: 1.2,
      endArrow: true,
      endArrowFill: color || '#d9d9d9',
      endArrowSize: 6,
    },
  }
  if (label) {
    edge.style.labelText = label
    edge.style.labelFontSize = 9
    edge.style.labelFill = color || '#8c8c8c'
    edge.style.labelBackground = true
    edge.style.labelBackgroundFill = '#fafafa'
    edge.style.labelBackgroundRadius = 2
  }
  return edge
}

const nodes = [
  n('start', 'START', 'dark'),
  n('mode_router', 'mode_router', 'blue'),
  // create (combo: create_group)
  n('image_plan', 'image_plan', 'blue', 'create_group'),
  n('content_img', 'content_image', 'blue', 'create_group'),
  n('illustration', 'illustration', 'blue', 'create_group'),
  n('diagram', 'diagram', 'blue', 'create_group'),
  n('logo', 'logo', 'blue', 'create_group'),
  n('img_agg', 'image_aggregator', 'blue', 'create_group'),
  n('prompt_enh', 'prompt_enhancer', 'green', 'create_group'),
  n('code_gen_1', 'code_generator', 'green', 'create_group'),
  // leetcode (combo: create_group)
  n('anim_adv', 'animation_advisor', 'cyan', 'create_group'),
  n('lc_enh', 'lc_prompt_enhancer', 'cyan', 'create_group'),
  n('code_gen_2', 'code_generator', 'green', 'create_group'),
  // interview (combo: create_group)
  n('iv_adv', 'interview_advisor', 'purple', 'create_group'),
  n('iv_enh', 'iv_prompt_enhancer', 'purple', 'create_group'),
  n('code_gen_3', 'code_generator', 'green', 'create_group'),
  // existing_code (combo: existing_group)
  n('reader', 'code_reader', 'orange', 'existing_group'),
  n('classifier', 'intent_classifier', 'orange', 'existing_group'),
  n('planner', 'modification_planner', 'red', 'existing_group'),
  n('db_op', 'database_operator', 'red', 'existing_group'),
  n('modifier', 'code_modifier', 'red', 'existing_group'),
  n('qa', 'qa_node', 'gray', 'existing_group'),
  n('end_qa', 'END', 'dark', 'existing_group'),
  // build check
  n('build', 'build_check', 'green'),
  n('fixer', 'code_fixer', 'red'),
  n('end', 'END', 'dark'),
]

const combos = [
  {
    id: 'create_group',
    style: {
      labelText: '创建模式',
      labelFontSize: 12,
      labelFill: '#1890ff',
      labelFontWeight: 600,
      fill: '#f0f8ff',
      stroke: '#1890ff',
      lineWidth: 1,
      lineDash: [4, 4],
      radius: 8,
      labelPosition: 'top',
      padding: 16,
    },
  },
  {
    id: 'existing_group',
    style: {
      labelText: '已有代码模式',
      labelFontSize: 12,
      labelFill: '#fa8c16',
      labelFontWeight: 600,
      fill: '#fffaf0',
      stroke: '#fa8c16',
      lineWidth: 1,
      lineDash: [4, 4],
      radius: 8,
      labelPosition: 'top',
      padding: 16,
    },
  },
]

const edges = [
  e('start', 'mode_router'),
  // mode_router → 4 branches
  e('mode_router', 'image_plan', 'create', '#1890ff'),
  e('mode_router', 'anim_adv', 'leetcode', '#13c2c2'),
  e('mode_router', 'iv_adv', 'interview', '#722ed1'),
  e('mode_router', 'reader', 'existing', '#fa8c16'),
  // create branch
  e('image_plan', 'content_img'),
  e('image_plan', 'illustration'),
  e('image_plan', 'diagram'),
  e('image_plan', 'logo'),
  e('content_img', 'img_agg'),
  e('illustration', 'img_agg'),
  e('diagram', 'img_agg'),
  e('logo', 'img_agg'),
  e('img_agg', 'prompt_enh'),
  e('prompt_enh', 'code_gen_1'),
  e('code_gen_1', 'build'),
  // leetcode branch
  e('anim_adv', 'lc_enh'),
  e('lc_enh', 'code_gen_2'),
  e('code_gen_2', 'build'),
  // interview branch
  e('iv_adv', 'iv_enh'),
  e('iv_enh', 'code_gen_3'),
  e('code_gen_3', 'build'),
  // existing code branch
  e('reader', 'classifier'),
  e('classifier', 'planner', 'MODIFY', '#f5222d'),
  e('classifier', 'qa', 'QA', '#8c8c8c'),
  e('planner', 'db_op'),
  e('db_op', 'modifier'),
  e('modifier', 'build'),
  e('qa', 'end_qa'),
  // build check
  e('build', 'end', 'PASS', '#52c41a'),
  e('build', 'fixer', 'FAIL', '#f5222d'),
  e('fixer', 'build', '循环', '#f5222d'),
]

onMounted(() => {
  graph = new Graph({
    container: 'workflow-graph',
    autoFit: 'view',
    padding: [20, 20, 20, 20],
    node: {
      type: 'rect',
      style: {
        size: [120, 28],
      },
    },
    edge: {
      type: 'cubic-vertical',
      style: {
        endArrow: true,
      },
    },
    combo: {
      type: 'rect',
      style: {
        padding: 16,
      },
    },
    layout: {
      type: 'dagre',
      rankdir: 'TB',
      nodesep: 16,
      ranksep: 36,
    },
    behaviors: ['zoom-canvas', 'drag-canvas'],
    data: { nodes, edges, combos },
  })

  graph.render()
})

onUnmounted(() => {
  if (graph) {
    graph.destroy()
    graph = null
  }
})
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

@media (max-width: 768px) {
  .about-page {
    padding: 0 16px;
  }
}

/* 树形流程图 */
.flow-tree {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 0;
}

.flow-node {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  border-radius: 8px;
  border: 1px solid #d9d9d9;
  background: #fff;
  min-width: 200px;
  max-width: 320px;
  width: 100%;
}

.flow-node p {
  margin: 2px 0 0;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

.flow-node--orange { border-color: #fa8c16; background: #fff7e6; }
.flow-node--red { border-color: #f5222d; background: #fff1f0; }
.flow-node--green { border-color: #52c41a; background: #f6ffed; }
.flow-node--blue { border-color: #1890ff; background: #e6f7ff; }
.flow-node--gray { border-color: #d9d9d9; background: #fafafa; }

.flow-node--sm {
  min-width: auto;
  max-width: 140px;
  padding: 6px 10px;
}

.flow-node--sm .flow-icon {
  font-size: 16px;
}

.flow-node--sm .flow-content strong {
  font-size: 12px;
}

.flow-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.flow-node--orange .flow-icon { color: #fa8c16; }
.flow-node--red .flow-icon { color: #f5222d; }
.flow-node--green .flow-icon { color: #52c41a; }
.flow-node--blue .flow-icon { color: #1890ff; }
.flow-node--gray .flow-icon { color: #8c8c8c; }

.flow-content strong {
  font-size: 13px;
}

.flow-arrow {
  font-size: 18px;
  color: #8c8c8c;
  line-height: 1;
  padding: 4px 0;
}

.flow-branch {
  width: 100%;
  max-width: 700px;
  position: relative;
  margin-top: 4px;
}


.flow-branch-line {
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 50%;
  height: 20px;
  border-left: 2px solid #d9d9d9;
  border-right: 2px solid #d9d9d9;
  border-top: 2px solid #d9d9d9;
  border-radius: 8px 8px 0 0;
}

.flow-branch-sides {
  display: flex;
  gap: 24px;
  padding-top: 24px;
}

.flow-branch-left,
.flow-branch-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.flow-branch-label {
  margin-bottom: 8px;
}

/* 并发布局 */
.flow-parallel {
  width: 100%;
  max-width: 700px;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.flow-parallel-label {
  margin-bottom: 8px;
}

.flow-parallel-line {
  width: 70%;
  height: 2px;
  background: #1890ff;
  position: relative;
}

.flow-parallel-line::before,
.flow-parallel-line::after {
  content: '';
  position: absolute;
  top: 0;
  width: 2px;
  height: 12px;
  background: #1890ff;
}

.flow-parallel-line::before { left: 0; }
.flow-parallel-line::after { right: 0; }

.flow-parallel-items {
  display: flex;
  gap: 8px;
  padding-top: 12px;
  flex-wrap: wrap;
  justify-content: center;
}

.flow-parallel-line-bottom {
  width: 70%;
  height: 2px;
  background: #1890ff;
  margin-top: 12px;
  position: relative;
}

.flow-parallel-line-bottom::before,
.flow-parallel-line-bottom::after {
  content: '';
  position: absolute;
  bottom: 0;
  width: 2px;
  height: 12px;
  background: #1890ff;
}

.flow-parallel-line-bottom::before { left: 0; }
.flow-parallel-line-bottom::after { right: 0; }

/* 循环布局 */
.flow-loop {
  width: 100%;
  max-width: 700px;
}

.flow-loop-body {
  display: flex;
  flex-direction: column;
  align-items: center;
}

@media (max-width: 768px) {
  .flow-branch-sides {
    flex-direction: column;
    align-items: center;
  }

  .flow-branch-line {
    display: none;
  }

  .flow-branch-left,
  .flow-branch-right {
    width: 100%;
  }
}
</style>