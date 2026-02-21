import { useState, useRef } from 'react'
import { ConfigProvider, theme } from 'antd'
import QuestionHeader from './components/QuestionHeader'
import DiagramDemo from './components/DiagramDemo'
import KeyboardHelp from './components/KeyboardHelp'
import { useKeyboard } from './hooks/useKeyboard'
import { question } from './data/question'
import { diagrams } from './diagrams'

export default function App() {
  const [helpOpen, setHelpOpen] = useState(false)
  const diagramRef = useRef()

  useKeyboard({
    onHelp: () => setHelpOpen(true),
    onNext: () => diagramRef.current?.next(),
    onPrev: () => diagramRef.current?.prev(),
    onToggle: () => diagramRef.current?.toggle(),
    onReset: () => diagramRef.current?.reset(),
    onDiagramChange: (dir) => diagramRef.current?.changeDiagram(dir),
  })

  return (
    <ConfigProvider theme={{ algorithm: theme.darkAlgorithm }}>
      <div className="max-w-5xl mx-auto p-6">
        <QuestionHeader question={question} />
        <DiagramDemo ref={diagramRef} diagrams={diagrams} />
        <div className="text-center mt-4 text-xs text-gray-600">
          按 <kbd className="px-1.5 py-0.5 bg-[#0f3460] border border-[#1a4a7a] rounded text-gray-400 font-mono">?</kbd> 查看快捷键
        </div>
        <KeyboardHelp open={helpOpen} onClose={() => setHelpOpen(false)} />
      </div>
    </ConfigProvider>
  )
}
