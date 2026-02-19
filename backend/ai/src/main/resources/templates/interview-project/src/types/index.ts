// 概念节点
export interface Concept {
  id: string
  label: string
  description: string
  children?: Concept[]
  highlight?: boolean
}

// 对比项
export interface Comparison {
  dimensions: string[]
  items: { name: string; values: string[] }[]
}

// 流程步骤
export interface FlowStep {
  id: string
  label: string
  description: string
  next?: string[]
}

// 题目数据
export interface InterviewTopic {
  title: string
  category: string
  keyPoints: string[]
  answerStructure: string
  concepts: Concept[]
  comparisons?: Comparison[]
  flowSteps?: FlowStep[]
}

// 讲解数据
export interface ExplanationData {
  summary: string
  detailedPoints: { title: string; content: string }[]
  interviewTips: string[]
}

// 可视化组件 Props
export interface VisualizationProps {
  topic: InterviewTopic
  explanation: ExplanationData
}
