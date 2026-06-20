import request from '@/utils/request'
import type { Result, ChatAssistant, CreateChatAssistantDTO } from '@/types'

export const assistantApi = {
  // 创建聊天助理
  create(data: CreateChatAssistantDTO): Promise<Result<number>> {
    return request.post('/assistant', data)
  },

  // 获取聊天助理列表
  getList(): Promise<Result<ChatAssistant[]>> {
    return request.get('/assistant/list')
  },

  // 根据ID获取聊天助理
  getById(id: string): Promise<Result<ChatAssistant>> {
    return request.get(`/assistant/${id}`)
  },

  // 更新聊天助理
  update(id: string, data: CreateChatAssistantDTO): Promise<Result<void>> {
    return request.put(`/assistant/${id}`, data)
  },

  // 删除聊天助理
  delete(id: string): Promise<Result<void>> {
    return request.delete(`/assistant/${id}`)
  },

  // 搜索聊天助理
  search(keyword: string): Promise<Result<ChatAssistant[]>> {
    return request.get('/assistant/search', { params: { keyword } })
  }
}
