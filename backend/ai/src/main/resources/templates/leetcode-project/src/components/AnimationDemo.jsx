import { useState, forwardRef, useImperativeHandle } from 'react'
import { Tabs } from 'antd'
import CodePanel from './CodePanel'
import AnimationControls from './AnimationControls'
import ExplanationBox from './ExplanationBox'
import { useAnimation } from '../hooks/useAnimation'

const AnimationDemo = forwardRef(function AnimationDemo({ solutions }, ref) {
  const [solutionIdx, setSolutionIdx] = useState(0)
  const [lang, setLang] = useState('java')
  const current = solutions[solutionIdx]
  const { step, playing, next, prev, reset, toggle } = useAnimation(current.steps.length)

  const changeSolution = (dir) => {
    if (dir === 'prev' && solutionIdx > 0) { setSolutionIdx(solutionIdx - 1); reset() }
    if (dir === 'next' && solutionIdx < solutions.length - 1) { setSolutionIdx(solutionIdx + 1); reset() }
  }

  useImperativeHandle(ref, () => ({
    next, prev, toggle, reset,
    setLang, changeSolution,
  }))

  const currentStep = current.steps[step]
  const Vis = current.Visualization

  return (
    <div className="space-y-4">
      <Tabs
        activeKey={current.id}
        onChange={(key) => {
          const idx = solutions.findIndex(s => s.id === key)
          if (idx >= 0) { setSolutionIdx(idx); reset() }
        }}
        items={solutions.map(s => ({ key: s.id, label: s.name }))}
        size="small"
      />

      <div className="flex gap-4 min-h-[350px]">
        <div className="w-[45%] flex-shrink-0">
          <CodePanel
            code={current.code}
            highlightLines={currentStep.highlightLines[lang] || []}
            lang={lang}
            onLangChange={setLang}
          />
        </div>
        <div className="flex-1 flex flex-col gap-4">
          <div className="flex-1 bg-[#16213e] rounded-lg p-4 flex items-center justify-center">
            <Vis state={currentStep.state} />
          </div>
          <ExplanationBox explanation={currentStep.explanation} />
        </div>
      </div>

      <AnimationControls
        step={step}
        totalSteps={current.steps.length}
        playing={playing}
        onNext={next}
        onPrev={prev}
        onToggle={toggle}
        onReset={reset}
      />
    </div>
  )
})

export default AnimationDemo
