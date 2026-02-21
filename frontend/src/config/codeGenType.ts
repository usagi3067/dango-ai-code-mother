/**
 * 代码生成类型枚举
 */
export enum CodeGenTypeEnum {
  VUE_PROJECT = 'vue_project',
  LEETCODE_PROJECT = 'leetcode_project',
  INTERVIEW_PROJECT = 'interview_project'
}

/**
 * 代码生成类型配置
 */
export const CODE_GEN_TYPE_CONFIG = {
    [CodeGenTypeEnum.VUE_PROJECT]: {
    label: 'Vue 项目模式',
    value: CodeGenTypeEnum.VUE_PROJECT,
  },
  [CodeGenTypeEnum.LEETCODE_PROJECT]: {
    label: '力扣题解模式',
    value: CodeGenTypeEnum.LEETCODE_PROJECT,
  },
  [CodeGenTypeEnum.INTERVIEW_PROJECT]: {
    label: '面试题解模式',
    value: CodeGenTypeEnum.INTERVIEW_PROJECT,
  },
} as const

/**
 * 代码生成类型选项（用于下拉选择）
 */
export const CODE_GEN_TYPE_OPTIONS = Object.values(CODE_GEN_TYPE_CONFIG).map((config) => ({
  label: config.label,
  value: config.value,
}))

/**
 * 根据 value 获取 label
 */
export const getCodeGenTypeLabel = (value: string | undefined): string => {
  if (!value) return ''
  const config = CODE_GEN_TYPE_CONFIG[value as CodeGenTypeEnum]
  return config?.label || value
}
