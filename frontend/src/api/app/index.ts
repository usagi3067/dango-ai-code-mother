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

/** 启动代码生成任务 POST /app/chat/gen/code */
export async function startGenCode(
  data: { appId: number | string; message: string; elementInfo?: API.ElementInfoDTO },
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/app/chat/gen/code', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data,
    ...(options || {}),
  })
}
