import request from '@/request'

/** 初始化数据库 POST /app/{appId}/database */
export async function initializeDatabase(appId: number, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>(`/app/${appId}/database`, {
    method: 'POST',
    ...(options || {}),
  })
}

/** 获取表摘要 GET /supabase/tables/{appId} */
export async function getTableSummary(appId: number, options?: { [key: string]: any }) {
  return request<API.BaseResponseListTableSummaryDTO>(`/supabase/tables/${appId}`, {
    method: 'GET',
    ...(options || {}),
  })
}
