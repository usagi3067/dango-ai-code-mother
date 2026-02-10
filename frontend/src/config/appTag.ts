/**
 * 应用标签枚举
 * text: 英文标识（数据库存储）
 * value: 中文描述（前端展示）
 */
export enum AppTagEnum {
  TOOL = 'tool',
  WEBSITE = 'website',
  DATA_ANALYSIS = 'data_analysis',
  ACTIVITY_PAGE = 'activity_page',
  MANAGEMENT_PLATFORM = 'management_platform',
  USER_APP = 'user_app',
  PERSONAL_MANAGEMENT = 'personal_management',
  ALGORITHM = 'algorithm',
  KNOWLEDGE = 'knowledge',
  GAME = 'game',
}

/**
 * 应用标签配置
 * 每个标签使用不同的颜色以便区分
 */
export const APP_TAG_CONFIG = {
  [AppTagEnum.TOOL]: {
    label: '工具',
    value: AppTagEnum.TOOL,
    color: '#2db7f5', // 蓝色 - 工具类
  },
  [AppTagEnum.WEBSITE]: {
    label: '网站',
    value: AppTagEnum.WEBSITE,
    color: '#87d068', // 绿色 - 网站类
  },
  [AppTagEnum.DATA_ANALYSIS]: {
    label: '数据分析',
    value: AppTagEnum.DATA_ANALYSIS,
    color: '#722ed1', // 紫色 - 数据分析
  },
  [AppTagEnum.ACTIVITY_PAGE]: {
    label: '活动页面',
    value: AppTagEnum.ACTIVITY_PAGE,
    color: '#fa8c16', // 橙色 - 活动页面
  },
  [AppTagEnum.MANAGEMENT_PLATFORM]: {
    label: '管理平台',
    value: AppTagEnum.MANAGEMENT_PLATFORM,
    color: '#13c2c2', // 青色 - 管理平台
  },
  [AppTagEnum.USER_APP]: {
    label: '用户应用',
    value: AppTagEnum.USER_APP,
    color: '#eb2f96', // 洋红色 - 用户应用
  },
  [AppTagEnum.PERSONAL_MANAGEMENT]: {
    label: '个人管理',
    value: AppTagEnum.PERSONAL_MANAGEMENT,
    color: '#faad14', // 金色 - 个人管理
  },
  [AppTagEnum.ALGORITHM]: {
    label: '算法',
    value: AppTagEnum.ALGORITHM,
    color: '#1890ff', // 天蓝色 - 算法
  },
  [AppTagEnum.KNOWLEDGE]: {
    label: '知识',
    value: AppTagEnum.KNOWLEDGE,
    color: '#52c41a', // 草绿色 - 知识
  },
  [AppTagEnum.GAME]: {
    label: '游戏',
    value: AppTagEnum.GAME,
    color: '#f5222d', // 红色 - 游戏
  },
} as const

/**
 * 应用标签选项（用于下拉选择）
 */
export const APP_TAG_OPTIONS = Object.values(APP_TAG_CONFIG).map((config) => ({
  label: config.label,
  value: config.value,
}))

/**
 * 默认标签
 */
export const DEFAULT_APP_TAG = AppTagEnum.WEBSITE

/**
 * 根据 text（英文标识）获取标签颜色
 */
export const getAppTagColor = (text: string | undefined): string => {
  if (!text) return '#87d068' // 默认绿色（网站）
  const config = APP_TAG_CONFIG[text as AppTagEnum]
  return config?.color || '#87d068'
}

/**
 * 根据 text（英文标识）获取 label（中文描述）
 */
export const getAppTagLabel = (text: string | undefined): string => {
  if (!text) return '网站'
  const config = APP_TAG_CONFIG[text as AppTagEnum]
  return config?.label || text
}
