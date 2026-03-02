import { Button, Space } from 'antd'
import {
  StepBackwardOutlined,
  StepForwardOutlined,
  CaretRightOutlined,
  PauseOutlined,
  UndoOutlined,
} from '@ant-design/icons'

export default function AnimationControls({ step, totalSteps, playing, onNext, onPrev, onToggle, onReset }) {
  return (
    <div className="flex items-center justify-center gap-4 py-3 px-4 bg-[#0f3460] rounded-lg">
      <Space>
        <Button
          icon={<StepBackwardOutlined />}
          onClick={onPrev}
          disabled={step === 0}
          size="small"
        >
          上一步
        </Button>
        <Button
          type="primary"
          icon={playing ? <PauseOutlined /> : <CaretRightOutlined />}
          onClick={onToggle}
          className={playing ? '!bg-[#e94560]' : '!bg-[#4ecca3] !text-[#1a1a2e]'}
          size="small"
        >
          {playing ? '暂停' : '播放'}
        </Button>
        <Button
          icon={<StepForwardOutlined />}
          onClick={onNext}
          disabled={step >= totalSteps - 1}
          size="small"
        >
          下一步
        </Button>
        <Button
          icon={<UndoOutlined />}
          onClick={onReset}
          danger
          size="small"
        >
          重置
        </Button>
      </Space>
      <div className="flex items-center gap-2 ml-4">
        <span className="text-xs text-gray-500">{step + 1} / {totalSteps}</span>
        <div className="flex gap-1">
          {Array.from({ length: totalSteps }, (_, i) => (
            <div
              key={i}
              className={`w-2 h-2 rounded-full transition-all duration-300 ${
                i === step ? 'bg-[#e94560] scale-125' : i < step ? 'bg-[#4ecca3]' : 'bg-[#0f3460]'
              }`}
            />
          ))}
        </div>
      </div>
    </div>
  )
}
