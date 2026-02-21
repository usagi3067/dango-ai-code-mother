import { Modal } from 'antd'

const shortcuts = [
  { keys: ['1', '2', '3'], desc: '切换主 Tab' },
  { keys: ['←', '→'], desc: '动画上一步 / 下一步' },
  { keys: ['Space'], desc: '播放 / 暂停' },
  { keys: ['R'], desc: '重置动画' },
  { keys: ['J', 'C'], desc: '切换 Java / C++' },
  { keys: ['[', ']'], desc: '切换解法' },
  { keys: ['?'], desc: '显示快捷键帮助' },
]

export default function KeyboardHelp({ open, onClose }) {
  return (
    <Modal title="⌨️ 快捷键" open={open} onCancel={onClose} footer={null} width={400}>
      <div className="space-y-3">
        {shortcuts.map((s, i) => (
          <div key={i} className="flex items-center justify-between">
            <div className="flex gap-1">
              {s.keys.map(k => (
                <kbd key={k} className="px-2 py-1 bg-gray-100 border border-gray-300 rounded text-xs font-mono text-gray-700">
                  {k}
                </kbd>
              ))}
            </div>
            <span className="text-sm text-gray-500">{s.desc}</span>
          </div>
        ))}
      </div>
    </Modal>
  )
}
