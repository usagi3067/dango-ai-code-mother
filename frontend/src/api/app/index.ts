// @ts-ignore
/* eslint-disable */
// API 更新时间：
// API 唯一标识：
import request from '@/request'
import * as chatHistoryController from './chatHistoryController'
import * as appController from './appController'
import * as staticResourceController from './staticResourceController'
export default {
  chatHistoryController,
  appController,
  staticResourceController,
}

/** 查询生成任务状态 GET /app/chat/gen/status */
export async function getGenStatus(
  params: { appId: number },
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseGenStatusResponse>('/app/chat/gen/status', {
    method: 'GET',
    params: { ...params },
    ...(options || {}),
  })
}
