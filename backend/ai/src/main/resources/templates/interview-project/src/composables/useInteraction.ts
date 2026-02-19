import { reactive } from 'vue'

export function useInteraction() {
  const expandedIds = reactive(new Set<string>())

  function toggle(id: string) {
    if (expandedIds.has(id)) {
      expandedIds.delete(id)
    } else {
      expandedIds.add(id)
    }
  }

  function isExpanded(id: string) {
    return expandedIds.has(id)
  }

  function expandAll(ids: string[]) {
    ids.forEach(id => expandedIds.add(id))
  }

  function collapseAll() {
    expandedIds.clear()
  }

  return { expandedIds, toggle, isExpanded, expandAll, collapseAll }
}
