import { ref, onMounted, onUnmounted } from 'vue'

interface UseInfiniteScrollOptions {
  /** 每页数量，默认 12 */
  pageSize?: number
  /** 距底部多少 px 触发加载，默认 200 */
  threshold?: number
  /** 数据加载函数，传入 lastId，返回记录数组 */
  fetchFn: (lastId: number | undefined) => Promise<API.AppVO[]>
}

export function useInfiniteScroll(options: UseInfiniteScrollOptions) {
  const { pageSize = 12, threshold = 200, fetchFn } = options

  const items = ref<API.AppVO[]>([])
  const lastId = ref<number | undefined>(undefined)
  const hasMore = ref(true)
  const loading = ref(false)

  const loadMore = async () => {
    if (loading.value || !hasMore.value) return
    loading.value = true
    try {
      const newRecords = await fetchFn(lastId.value)
      items.value.push(...newRecords)
      if (newRecords.length > 0) {
        lastId.value = newRecords[newRecords.length - 1].id as number
      }
      hasMore.value = newRecords.length === pageSize
    } catch (error) {
      console.error('加载应用列表失败:', error)
    } finally {
      loading.value = false
    }
  }

  const reset = () => {
    items.value = []
    lastId.value = undefined
    hasMore.value = true
    loadMore()
  }

  const handleScroll = () => {
    const scrollTop = document.documentElement.scrollTop || document.body.scrollTop
    const scrollHeight = document.documentElement.scrollHeight || document.body.scrollHeight
    const clientHeight = document.documentElement.clientHeight || window.innerHeight
    if (scrollHeight - scrollTop - clientHeight < threshold && !loading.value && hasMore.value) {
      loadMore()
    }
  }

  const handleWheel = (e: WheelEvent) => {
    if (e.deltaY <= 0) return
    const scrollTop = document.documentElement.scrollTop || document.body.scrollTop
    const scrollHeight = document.documentElement.scrollHeight || document.body.scrollHeight
    const clientHeight = window.innerHeight
    if (scrollHeight - scrollTop - clientHeight < 10 && !loading.value && hasMore.value) {
      loadMore()
    }
  }

  onMounted(() => {
    window.addEventListener('scroll', handleScroll)
    window.addEventListener('wheel', handleWheel)
    loadMore()
  })

  onUnmounted(() => {
    window.removeEventListener('scroll', handleScroll)
    window.removeEventListener('wheel', handleWheel)
  })

  return { items, loading, hasMore, loadMore, reset }
}
