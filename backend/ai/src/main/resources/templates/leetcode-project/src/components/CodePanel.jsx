import { Tabs } from 'antd'

export default function CodePanel({ code, highlightLines = [], lang, onLangChange }) {
  const renderCode = (source, highlights) => {
    const lines = source.split('\n')
    return (
      <pre className="m-0 bg-[#1a1a2e] overflow-x-auto text-sm">
        {lines.map((line, i) => {
          const lineNum = i + 1
          const isHighlight = highlights.includes(lineNum)
          return (
            <div
              key={i}
              className={`flex transition-colors duration-300 ${
                isHighlight
                  ? 'bg-[#3d3d00] border-l-[3px] border-[#ffd700]'
                  : 'hover:bg-[#252545]'
              }`}
            >
              <span className="min-w-[40px] px-2 text-right text-gray-600 bg-[#0f0f1a] font-mono text-xs leading-6 select-none">
                {lineNum}
              </span>
              <span className="px-3 text-gray-300 font-mono text-xs leading-6 whitespace-pre">
                {line}
              </span>
            </div>
          )
        })}
      </pre>
    )
  }

  const items = [
    {
      key: 'java',
      label: '☕ Java',
      children: renderCode(code.java, lang === 'java' ? highlightLines : []),
    },
    {
      key: 'cpp',
      label: '⚡ C++',
      children: renderCode(code.cpp, lang === 'cpp' ? highlightLines : []),
    },
  ]

  return (
    <div className="border border-[#0f3460] rounded-lg overflow-hidden h-full">
      <Tabs
        activeKey={lang}
        onChange={onLangChange}
        items={items}
        size="small"
        className="[&_.ant-tabs-nav]:!mb-0 [&_.ant-tabs-nav]:!bg-[#0f3460] [&_.ant-tabs-nav]:px-2"
      />
    </div>
  )
}
