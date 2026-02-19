import type { ProblemData } from '../types'

// 占位数据 — 由 skill 生成时覆盖此文件
export const problem: ProblemData = {
  number: 0,
  title: '示例题目',
  slug: 'example',
  description: '这是一个占位题目描述。请使用 skill 生成实际题目数据。',
  coreIdea: {
    steps: [
      {
        title: '第一步：理解问题',
        content: '<p>占位内容</p>',
      },
    ],
    summary: {
      flowNodes: [{ text: '占位', highlight: true }],
      footnote: '由 skill 生成实际内容',
    },
  },
  comparison: [
    {
      name: '示例解法',
      timeComplexity: 'O(n)',
      spaceComplexity: 'O(1)',
      timeLevel: 'good',
      spaceLevel: 'good',
      coreIdea: '占位',
      whyThisApproach: '占位',
    },
  ],
  recommendCards: [],
}
