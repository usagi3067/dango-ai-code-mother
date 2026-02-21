import { useState, forwardRef, useImperativeHandle } from 'react'
import { Segmented } from 'antd'
import AnimationControls from './AnimationControls'
import SpeechBox from './ExplanationBox'
import { useAnimation } from '../hooks/useAnimation'

const DiagramDemo = forwardRef(function DiagramDemo({ diagrams }, ref) {
  const [diagramIdx, setDiagramIdx] = useState(0)
  const current = diagrams[diagramIdx]
  const { step, playing, next, prev, reset, toggle } = useAnimation(current.steps.length)

  const changeDiagram = (dir) => {
    if (dir === 'prev' && diagramIdx > 0) { setDiagramIdx(diagramIdx - 1); reset() }
    if (dir === 'next' && diagramIdx < diagrams.length - 1) { setDiagramIdx(diagramIdx + 1); reset() }
  }

  useImperativeHandle(ref, () => ({
    next, prev, toggle, reset, changeDiagram,
  }))

  const currentStep = current.steps[step]
  const Vis = current.Visualization

  return (
    <div className="space-y-4">
      {diagrams.length > 1 && (
        <Segmented
          value={current.id}
          onChange={(val) => {
            const idx = diagrams.findIndex(d => d.id === val)
            if (idx >= 0) { setDiagramIdx(idx); reset() }
          }}
          options={diagrams.map(d => ({ value: d.id, label: d.name }))}
        />
      )}

      <div className="bg-[#16213e] rounded-lg p-6 flex items-center justify-center min-h-[350px]">
        <Vis state={currentStep.state} />
      </div>

      <SpeechBox speech={currentStep.speech} note={currentStep.note} />

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

export default DiagramDemo
