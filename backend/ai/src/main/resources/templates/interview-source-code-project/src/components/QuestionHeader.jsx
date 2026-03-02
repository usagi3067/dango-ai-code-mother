import { Tag } from 'antd'

const importanceMap = {
  high: { color: 'red', text: '高频' },
  medium: { color: 'orange', text: '中频' },
  low: { color: 'blue', text: '低频' },
}

export default function QuestionHeader({ question }) {
  const imp = importanceMap[question.importance]
  return (
    <div className="mb-6">
      <div className="flex items-center gap-3 mb-3">
        <h1 className="text-2xl font-bold text-gray-100 m-0">
          {question.title}
        </h1>
        <Tag color="cyan">{question.topic}</Tag>
        <Tag color="geekblue">{question.category}</Tag>
        {imp && <Tag color={imp.color}>{imp.text}</Tag>}
      </div>
    </div>
  )
}
