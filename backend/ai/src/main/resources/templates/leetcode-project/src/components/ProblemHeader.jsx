import { Typography, Tag } from 'antd'

const difficultyMap = {
  easy: { color: 'green', text: '简单' },
  medium: { color: 'orange', text: '中等' },
  hard: { color: 'red', text: '困难' },
}

export default function ProblemHeader({ problem }) {
  const diff = difficultyMap[problem.difficulty]
  return (
    <div className="mb-6">
      <div className="flex items-center gap-3 mb-3">
        <h1 className="text-2xl font-bold text-gray-100 m-0">
          {problem.number}. {problem.title}
        </h1>
        <Tag color={diff.color}>{diff.text}</Tag>
      </div>
      <Typography.Paragraph className="!text-gray-400 text-sm leading-relaxed bg-[#0f3460] rounded-lg p-4 border-l-4 border-[#e94560]">
        {problem.description}
      </Typography.Paragraph>
    </div>
  )
}
