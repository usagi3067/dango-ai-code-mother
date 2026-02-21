import { Card } from 'antd'

export default function CoreIdea({ coreIdea }) {
  return (
    <div className="space-y-4">
      {coreIdea.steps.map((step, i) => (
        <Card
          key={i}
          size="small"
          className="!bg-[#0f3460] !border-[#1a4a7a]"
        >
          <div className="flex gap-3">
            <span className="flex-shrink-0 w-7 h-7 rounded-full bg-[#e94560] text-white text-sm flex items-center justify-center font-bold">
              {i + 1}
            </span>
            <div>
              <h4 className="text-[#e94560] font-bold m-0 mb-1">{step.title}</h4>
              <p className="text-gray-300 m-0 leading-relaxed text-sm">{step.content}</p>
              {step.formula && (
                <code className="block mt-2 bg-[#1a1a2e] text-[#4ecca3] px-3 py-2 rounded text-sm font-mono">
                  {step.formula}
                </code>
              )}
            </div>
          </div>
        </Card>
      ))}
      <div className="bg-[#1a3d2e] border border-[#4ecca3] rounded-lg p-4 text-sm text-[#4ecca3]">
        💡 {coreIdea.summary}
      </div>
    </div>
  )
}
