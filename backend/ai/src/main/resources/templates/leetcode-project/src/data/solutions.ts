import type { SolutionData } from '../types'
import PlaceholderVis from '../components/visualizations/PlaceholderVis.vue'

// 占位数据 — 由 skill 生成时覆盖此文件
export const solutions: SolutionData[] = [
  {
    id: 'placeholder',
    name: '示例解法',
    complexity: 'O(n)',
    complexityLevel: 'good',
    code: {
      java: {
        raw: '// placeholder',
        lines: [[{ type: 'comment', text: '// placeholder' }]],
      },
      cpp: {
        raw: '// placeholder',
        lines: [[{ type: 'comment', text: '// placeholder' }]],
      },
    },
    steps: [
      {
        state: {},
        highlightLines: { java: [], cpp: [] },
        explanation: '占位步骤 — 由 skill 生成实际动画数据。',
      },
    ],
    visualization: PlaceholderVis,
  },
]
