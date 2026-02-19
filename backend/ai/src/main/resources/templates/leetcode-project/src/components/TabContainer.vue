<template>
  <div>
    <div class="tabs">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="tab-btn"
        :class="{ active: modelValue === tab.key }"
        @click="$emit('update:modelValue', tab.key)"
      >
        {{ tab.label }}
      </button>
    </div>
    <div
      v-for="tab in tabs"
      :key="tab.key"
      class="tab-content"
      :class="{ active: modelValue === tab.key }"
    >
      <slot :name="tab.key" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  tabs: { key: string; label: string }[]
  modelValue: string
}>()
defineEmits<{ 'update:modelValue': [value: string] }>()
</script>
