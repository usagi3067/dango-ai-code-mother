declare namespace API {
  type AppAddRequest = {
    initPrompt?: string
    appName?: string
    tag?: string
    codeGenType?: string
  }

  type AppAdminUpdateRequest = {
    id?: string
    appName?: string
    cover?: string
    priority?: number
    tag?: string
  }

  type AppDeployRequest = {
    appId?: string
  }

  type AppQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: string
    appName?: string
    cover?: string
    initPrompt?: string
    codeGenType?: string
    tag?: string
    deployKey?: string
    priority?: number
    userId?: string
    /** 游标（上一批最后一条记录的 id，用于游标分页） */
    lastId?: string
    /** 搜索关键词（搜索 appName 和 initPrompt） */
    searchText?: string
  }

  type AppUpdateRequest = {
    id?: string
    appName?: string
    tag?: string
  }

  type AppVO = {
    id?: string
    appName?: string
    cover?: string
    initPrompt?: string
    codeGenType?: string
    tag?: string
    deployKey?: string
    deployedTime?: string
    priority?: number
    userId?: string
    createTime?: string
    updateTime?: string
    hasDatabase?: boolean
    user?: UserVO
  }

  type BaseResponseAppVO = {
    code?: number
    data?: AppVO
    message?: string
  }

  type BaseResponseBoolean = {
    code?: number
    data?: boolean
    message?: string
  }

  type BaseResponseLong = {
    code?: number
    data?: number
    message?: string
  }

  type BaseResponsePageAppVO = {
    code?: number
    data?: PageAppVO
    message?: string
  }

  type BaseResponsePageChatHistoryVO = {
    code?: number
    data?: PageChatHistoryVO
    message?: string
  }

  type BaseResponseString = {
    code?: number
    data?: string
    message?: string
  }

  type BaseResponseListTableSummaryDTO = {
    code?: number
    data?: TableSummaryDTO[]
    message?: string
  }

  type ChatHistoryQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    appId?: string
    lastId?: string
    userId?: string
    messageType?: string
  }

  type ChatHistoryVO = {
    id?: string
    message?: string
    messageType?: string
    status?: string
    appId?: string
    userId?: string
    createTime?: string
  }

  type chatToGenCodeParams = {
    appId: string
    message: string
    agent?: boolean
  }

  type DeleteRequest = {
    id?: string
  }

  type downloadAppCodeParams = {
    appId: string
  }

  type getAppVOByIdByAdminParams = {
    id: string
  }

  type getAppVOByIdParams = {
    id: string
  }

  type listChatHistoryByAppIdParams = {
    appId: string
    lastId?: string
    size?: number
  }

  type PageAppVO = {
    records?: AppVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageChatHistoryVO = {
    records?: ChatHistoryVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type ServerSentEventString = true

  type serveStaticResourceParams = {
    deployKey: string
  }

  type UserVO = {
    id?: string
    userAccount?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    createTime?: string
  }

  /** 表摘要信息 */
  type TableSummaryDTO = {
    tableName?: string
    rowCount?: number
  }

  type FeatureItemVO = {
    name?: string
    description?: string
    checked?: boolean
    recommended?: boolean
  }

  type FeatureAnalysisVO = {
    features?: FeatureItemVO[]
  }

  type AppInfoVO = {
    appName?: string
    tag?: string
  }

  type BaseResponseFeatureAnalysisVO = {
    code?: number
    data?: FeatureAnalysisVO
    message?: string
  }

  type BaseResponseAppInfoVO = {
    code?: number
    data?: AppInfoVO
    message?: string
  }

  type GenStatusResponse = {
    status: 'generating' | 'completed' | 'error' | 'none'
    chatHistoryId?: string
  }

  type BaseResponseGenStatusResponse = {
    code?: number
    data?: GenStatusResponse
    message?: string
  }

  type ElementInfoDTO = {
    tagName?: string
    id?: string
    className?: string
    textContent?: string
    selector?: string
    pagePath?: string
  }
}
