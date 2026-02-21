import { useState, useRef, useCallback } from 'react'

export function useAnimation(totalSteps) {
  const [step, setStep] = useState(0)
  const [playing, setPlaying] = useState(false)
  const intervalRef = useRef(null)

  const stop = useCallback(() => {
    clearInterval(intervalRef.current)
    setPlaying(false)
  }, [])

  const next = useCallback(() => {
    setStep(s => {
      if (s >= totalSteps - 1) return s
      return s + 1
    })
  }, [totalSteps])

  const prev = useCallback(() => {
    setStep(s => Math.max(0, s - 1))
  }, [])

  const reset = useCallback(() => {
    stop()
    setStep(0)
  }, [stop])

  const play = useCallback(() => {
    setPlaying(true)
    intervalRef.current = setInterval(() => {
      setStep(s => {
        if (s >= totalSteps - 1) {
          clearInterval(intervalRef.current)
          setPlaying(false)
          return s
        }
        return s + 1
      })
    }, 1500)
  }, [totalSteps])

  const toggle = useCallback(() => {
    playing ? stop() : play()
  }, [playing, stop, play])

  return { step, playing, next, prev, reset, toggle, setStep }
}
