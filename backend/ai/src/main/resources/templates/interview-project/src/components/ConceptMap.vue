<template>
  <div class="concept-map">
    <div class="concept-tree">
      <div v-for="concept in topic.concepts" :key="concept.id" class="concept-branch">
        <div
          class="concept-node"
          :class="{ highlight: concept.highlight, expandable: concept.children?.length }"
          @click="concept.children?.length && toggle(concept.id)"
        >
          <span v-if="concept.children?.length" class="toggle-icon">
            {{ isExpanded(concept.id) ? '▼' : '▶' }}
          </span>
          <span class="concept-label">{{ concept.label }}</span>
          <span class="concept-desc">{{ concept.description }}</span>
        </div>
        <div v-if="concept.children?.length && isExpanded(concept.id)" class="children">
          <div v-for="child in concept.children" :key="child.id" class="concept-branch">
            <div
              class="concept-node child-node"
              :class="{ highlight: child.highlight, expandable: child.children?.length }"
              @click="child.children?.length && toggle(child.id)"
            >
              <span v-if="child.children?.length" class="toggle-icon">
                {{ isExpanded(child.id) ? '▼' : '▶' }}
              </span>
              <span class="concept-label">{{ child.label }}</span>
              <span class="concept-desc">{{ child.description }}</span>
            </div>
            <div v-if="child.children?.length && isExpanded(child.id)" class="children">
              <div v-for="leaf in child.children" :key="leaf.id" class="concept-branch">
                <div class="concept-node leaf-node" :class="{ highlight: leaf.highlight }">
                  <span class="concept-label">{{ leaf.label }}</span>
                  <span class="concept-desc">{{ leaf.description }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { VisualizationProps } from '@/types'
import { useInteraction } from '@/composables/useInteraction'

defineProps<VisualizationProps>()
const { toggle, isExpanded } = useInteraction()
</script>

<style scoped>
.concept-map { padding: 15px; }
.concept-branch { margin-bottom: 8px; }
.concept-node {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 16px; background: var(--card-bg); border-radius: 10px;
  border-left: 4px solid var(--accent); cursor: default; transition: all 0.2s;
}
.concept-node.expandable { cursor: pointer; }
.concept-node.expandable:hover { background: var(--card-hover); }
.concept-node.highlight { border-left-color: var(--highlight); background: var(--highlight-bg); }
.child-node { margin-left: 30px; border-left-color: var(--secondary); }
.leaf-node { margin-left: 60px; border-left-color: var(--muted); }
.toggle-icon { color: var(--accent); font-size: 12px; min-width: 14px; }
.concept-label { color: var(--text); font-weight: 600; white-space: nowrap; }
.concept-desc { color: var(--text-secondary); font-size: 13px; }
.children { margin-top: 6px; }
</style>
