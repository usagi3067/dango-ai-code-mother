import { useRef, useMemo } from 'react'
import gsap from 'gsap'
import { useGSAP } from '@gsap/react'
import Prism from 'prismjs'
import 'prismjs/components/prism-java'
import 'prismjs/components/prism-javascript'
import 'prismjs/components/prism-sql'
import 'prismjs/components/prism-go'
import 'prismjs/components/prism-python'
import 'prismjs/themes/prism-tomorrow.css'
import AnnotationBubble from './AnnotationBubble'

gsap.registerPlugin(useGSAP)

export default function CodeViewer({ step, prevStep }) {
  const containerRef = useRef()

  const lang = step.language || 'java'
  const codeLines = useMemo(() => step.code.split('\n'), [step.code])

  const grammar = Prism.languages[lang] || Prism.languages.plain

  useGSAP(() => {
    if (!containerRef.current) return
    const tl = gsap.timeline()

    // File switch transition
    if (prevStep && prevStep.file !== step.file) {
      tl.fromTo(containerRef.current,
        { opacity: 0, x: 30 },
        { opacity: 1, x: 0, duration: 0.4, ease: 'power2.out' },
        0
      )
    }

    // Reset all line highlights
    const allLines = containerRef.current.querySelectorAll('[data-line]')
    tl.to(allLines, {
      backgroundColor: 'transparent',
      borderLeftColor: 'transparent',
      duration: 0.2
    }, 0)

    // Highlight target lines
    step.highlightLines?.forEach((lineNum, i) => {
      const el = containerRef.current.querySelector(`[data-line="${lineNum}"]`)
      if (el) {
        tl.to(el, {
          backgroundColor: 'rgba(78,204,163,0.12)',
          borderLeftColor: '#4ecca3',
          duration: 0.4,
          ease: 'power2.out'
        }, i === 0 ? '>' : '<0.05')
      }
    })

    // Annotation bubbles pop in
    const bubbles = containerRef.current.querySelectorAll('[data-annotation]')
    if (bubbles.length) {
      tl.fromTo(bubbles,
        { opacity: 0, x: 15 },
        { opacity: 1, x: 0, duration: 0.4, ease: 'back.out(2)', stagger: 0.1 }
      )
    }

    // Keyword flash emphasis
    step.keywords?.forEach(kw => {
      const kwEls = containerRef.current.querySelectorAll(`[data-keyword="${kw}"]`)
      if (kwEls.length) {
        tl.to(kwEls, {
          color: '#4ecca3',
          textShadow: '0 0 8px rgba(78,204,163,0.5)',
          duration: 0.3, yoyo: true, repeat: 1
        })
      }
    })

    // Auto-scroll to highlighted line
    if (step.highlightLines?.[0]) {
      const targetLine = containerRef.current.querySelector(
        `[data-line="${step.highlightLines[0]}"]`
      )
      const codeArea = containerRef.current.querySelector('[data-code-area]')
      if (targetLine && codeArea) {
        const lineTop = targetLine.offsetTop
        const areaHeight = codeArea.clientHeight
        const scrollTarget = Math.max(0, lineTop - areaHeight / 3)
        tl.to(codeArea, { scrollTop: scrollTarget, duration: 0.5, ease: 'power2.out' }, '<')
      }
    }
  }, { scope: containerRef, dependencies: [step] })

  return (
    <div ref={containerRef} className="bg-[#1a1a2e] rounded-lg overflow-hidden border border-[#0f3460]">
      {/* File name tab + source */}
      <div className="flex items-center justify-between px-4 py-2.5 bg-[#0f3460] border-b border-[#1a4a7a]">
        <div className="flex items-center gap-2">
          <span className="text-[#4ecca3] text-sm">📄</span>
          <span className="text-sm text-gray-200 font-mono">{step.file}</span>
        </div>
        {step.source && (
          <span className="text-xs text-gray-500 italic">来源: {step.source}</span>
        )}
      </div>

      {/* Code area */}
      <div data-code-area className="overflow-y-auto max-h-[420px] py-3">
        {codeLines.map((line, i) => {
          const lineNum = i + 1
          const annotation = step.annotations?.find(a => a.line === lineNum)
          const isHighlighted = step.highlightLines?.includes(lineNum)

          return (
            <div key={i} data-line={lineNum}
              className="flex items-start px-4 py-[3px] border-l-[3px] border-transparent"
            >
              {/* Line number */}
              <span className={`text-xs font-mono w-8 text-right shrink-0 select-none leading-6 ${
                isHighlighted ? 'text-[#4ecca3]' : 'text-gray-600'
              }`}>
                {lineNum}
              </span>

              {/* Code content */}
              <pre className="m-0 ml-4 flex-1 leading-6 overflow-x-auto">
                <code
                  dangerouslySetInnerHTML={{
                    __html: Prism.highlight(line, grammar, lang) || '&nbsp;'
                  }}
                />
              </pre>

              {/* Inline annotation */}
              {annotation && (
                <AnnotationBubble
                  label={annotation.label}
                  detail={annotation.detail}
                  lineNum={lineNum}
                />
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}
