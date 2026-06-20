import request from '@/utils/request'
import type {
  Result,
  KnowledgeBase,
  CreateKnowledgeBaseDTO,
  UpdateKnowledgeBaseDTO
} from '@/types'

export const knowledgeBaseApi = {
  // 获取所有知识库
  getAll(): Promise<Result<KnowledgeBase[]>> {
    return request.get('/knowledge-base')
  },

  // 根据ID获取知识库
  getById(id: string): Promise<Result<KnowledgeBase>> {
    return request.get(`/knowledge-base/${id}`)
  },

  // 创建知识库
  create(data: CreateKnowledgeBaseDTO): Promise<Result<number>> {
    return request.post('/knowledge-base', data)
  },

  // 更新知识库
  update(id: string, data: UpdateKnowledgeBaseDTO): Promise<Result<void>> {
    return request.put(`/knowledge-base/${id}`, data)
  },

  // 删除知识库
  delete(id: string): Promise<Result<void>> {
    return request.delete(`/knowledge-base/${id}`)
  }
}
