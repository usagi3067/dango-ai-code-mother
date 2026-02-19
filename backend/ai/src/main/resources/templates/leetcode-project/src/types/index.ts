import type { Component } from 'vue'

// ===== 核心思路 =====

export interface IdeaStep {
  title: string
  content: string
  formula?: string
}

export interface FlowNode {
  text: string
  highlight?: boolean
}

export interface CoreIdeaData {
  steps: IdeaStep[]
  summary: {
    flowNodes: FlowNode[]
    footnote: string
  }
}

// ===== 解法对比 =====

export type ComplexityLevel = 'good' | 'bad' | 'medium'

export interface ComparisonItem {
  name: string
  timeComplexity: string
  spaceComplexity: string
  timeLevel: ComplexityLevel
  spaceLevel: ComplexityLevel
  coreIdea: string
  whyThisApproach: string
}

export interface RecommendCard {
  title: string
  content: string
  borderColor?: string
}

// ===== 题目数据 =====

export interface ProblemData {
  number: number
  title: string
  slug: string
  description: string
  coreIdea: CoreIdeaData
  comparison: ComparisonItem[]
  recommendCards: RecommendCard[]
}

// ===== 代码 =====

export interface CodeToken {
  type: 'keyword' | 'type' | 'method' | 'number' | 'string' | 'comment' | 'plain'
  text: string
}

export interface CodeData {
  raw: string
  lines: CodeToken[][]
}

// ===== 动画 =====

export interface AnimationStep<T = Record<string, any>> {
  state: T
  highlightLines: { java: number[]; cpp: number[] }
  highlightGreenLines?: { java: number[]; cpp: number[] }
  explanation: string
}

export interface SolutionData<T = Record<string, any>> {
  id: string
  name: string
  complexity: string
  complexityLevel: ComplexityLevel
  code: { java: CodeData; cpp: CodeData }
  steps: AnimationStep<T>[]
  visualization: Component
}

// ===== 可视化组件 Props =====

export interface VisualizationProps<T = Record<string, any>> {
  step: AnimationStep<T>
  stepIndex: number
  totalSteps: number
}
