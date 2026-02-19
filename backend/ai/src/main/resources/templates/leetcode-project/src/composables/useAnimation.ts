import { ref, onUnmounted } from 'vue'

export function useAnimation(totalSteps: number) {
  const index = ref(0)
  const playing = ref(false)
  let timer: ReturnType<typeof setInterval> | null = null

  function next() {
    if (index.value < totalSteps - 1) index.value++
    else stop()
  }

  function prev() {
    if (index.value > 0) index.value--
  }

  function reset() {
    stop()
    index.value = 0
  }

  function play() {
    timer = setInterval(() => {
      if (index.value >= totalSteps - 1) { stop(); return }
      index.value++
    }, 2000)
    playing.value = true
  }

  function stop() {
    if (timer) { clearInterval(timer); timer = null }
    playing.value = false
  }

  function toggle() {
    playing.value ? stop() : play()
  }

  onUnmounted(() => stop())

  return { index, playing, next, prev, reset, toggle }
}
