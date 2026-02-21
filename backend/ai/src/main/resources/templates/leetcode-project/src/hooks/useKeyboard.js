import { useEffect } from 'react'

export function useKeyboard({ onTabChange, onNext, onPrev, onToggle, onReset, onLangChange, onSolutionChange, onHelp }) {
  useEffect(() => {
    const handler = (e) => {
      const tag = e.target.tagName
      if (tag === 'INPUT' || tag === 'TEXTAREA') return

      switch (e.key) {
        case '1': case '2': case '3':
          e.preventDefault()
          onTabChange?.(e.key)
          break
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
        case 'j': case 'J':
          e.preventDefault()
          onLangChange?.('java')
          break
        case 'c': case 'C':
          e.preventDefault()
          onLangChange?.('cpp')
          break
        case '[':
          e.preventDefault()
          onSolutionChange?.('prev')
          break
        case ']':
          e.preventDefault()
          onSolutionChange?.('next')
          break
        case '?':
          e.preventDefault()
          onHelp?.()
          break
      }
    }

    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [onTabChange, onNext, onPrev, onToggle, onReset, onLangChange, onSolutionChange, onHelp])
}
