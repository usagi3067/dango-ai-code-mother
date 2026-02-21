import { useEffect } from 'react'

export function useKeyboard({ onNext, onPrev, onToggle, onReset, onDiagramChange, onHelp }) {
  useEffect(() => {
    const handler = (e) => {
      const tag = e.target.tagName
      if (tag === 'INPUT' || tag === 'TEXTAREA') return

      switch (e.key) {
        case 'ArrowRight':
          e.preventDefault()
          onNext?.()
          break
        case 'ArrowLeft':
          e.preventDefault()
          onPrev?.()
          break
        case ' ':
          e.preventDefault()
          onToggle?.()
          break
        case 'r': case 'R':
          e.preventDefault()
          onReset?.()
          break
        case '[':
          e.preventDefault()
          onDiagramChange?.('prev')
          break
        case ']':
          e.preventDefault()
          onDiagramChange?.('next')
          break
        case '?':
          e.preventDefault()
          onHelp?.()
          break
      }
    }

    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [onNext, onPrev, onToggle, onReset, onDiagramChange, onHelp])
}
