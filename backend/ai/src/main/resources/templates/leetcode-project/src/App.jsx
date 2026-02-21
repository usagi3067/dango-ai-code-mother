import { useState, useRef, useCallback } from 'react'
import { Tabs, ConfigProvider, theme } from 'antd'
import ProblemHeader from './components/ProblemHeader'
import CoreIdea from './components/CoreIdea'
import CompareTable from './components/CompareTable'
import AnimationDemo from './components/AnimationDemo'
import KeyboardHelp from './components/KeyboardHelp'
import { useKeyboard } from './hooks/useKeyboard'
import { problem } from './data/problem'
import { solutions } from './solutions'

const tabKeys = ['idea', 'compare', 'animation']

export default function App() {
  const [activeTab, setActiveTab] = useState('idea')
  const [helpOpen, setHelpOpen] = useState(false)
  const animRef = useRef()

  const handleTabChange = useCallback((key) => {
    const idx = parseInt(key) - 1
    if (tabKeys[idx]) setActiveTab(tabKeys[idx])
  }, [])

  useKeyboard({
    onTabChange: handleTabChange,
    onHelp: () => setHelpOpen(true),
    onNext: () => animRef.current?.next(),
    onPrev: () => animRef.current?.prev(),
    onToggle: () => animRef.current?.toggle(),
    onReset: () => animRef.current?.reset(),
    onLangChange: (lang) => animRef.current?.setLang(lang),
    onSolutionChange: (dir) => animRef.current?.changeSolution(dir),
  })

  const items = [
    {
      key: 'idea',
      label: '💡 核心思路',
      children: <CoreIdea coreIdea={problem.coreIdea} />,
    },
    {
      key: 'compare',
      label: '📊 解法对比',
      children: <CompareTable comparison={problem.comparison} />,
    },
    {
      key: 'animation',
      label: '🎬 动画演示',
      children: <AnimationDemo ref={animRef} solutions={solutions} />,
    },
  ]

  return (
    <ConfigProvider theme={{ algorithm: theme.darkAlgorithm }}>
      <div className="max-w-6xl mx-auto p-6">
        <ProblemHeader problem={problem} />
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={items}
          size="large"
        />
        <div className="text-center mt-4 text-xs text-gray-600">
          按 <kbd className="px-1.5 py-0.5 bg-[#0f3460] border border-[#1a4a7a] rounded text-gray-400 font-mono">?</kbd> 查看快捷键
        </div>
        <KeyboardHelp open={helpOpen} onClose={() => setHelpOpen(false)} />
      </div>
    </ConfigProvider>
  )
}
