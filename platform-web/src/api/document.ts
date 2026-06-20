import request from '@/utils/request'
import type { Result, Document, CreateDocumentDTO, DocumentChunk, SearchResult } from '@/types'

export const documentApi = {
  // 根据ID获取文档
  getById(id: string): Promise<Result<Document>> {
    return request.get(`/document/${id}`)
  },

  // 根据知识库ID获取文档列表
  getByKnowledgeBaseId(knowledgeBaseId: string): Promise<Result<Document[]>> {
    return request.get(`/document/knowledge-base/${knowledgeBaseId}`)
  },

  // 创建文档
  create(data: CreateDocumentDTO): Promise<Result<number>> {
    return request.post('/document', data)
  },

  // 更新文档名称
  updateName(id: string, name: string): Promise<Result<void>> {
    return request.put(`/document/${id}/name?name=${encodeURIComponent(name)}`)
  },

  // 删除文档
  delete(id: string): Promise<Result<void>> {
    return request.delete(`/document/${id}`)
  },

  // 获取文档分块
  getChunks(id: string): Promise<Result<DocumentChunk[]>> {
    return request.get(`/document/${id}/chunks`)
  },

  // 统计文档数量
  count(knowledgeBaseId: string): Promise<Result<number>> {
    return request.get(`/document/knowledge-base/${knowledgeBaseId}/count`)
  },

  // 上传文档
  upload(file: File, knowledgeBaseId: string, isParse: boolean): Promise<Result<number>> {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('knowledgeBaseId', knowledgeBaseId.toString())
    formData.append('isParse', isParse.toString())

    return request.post('/document/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  // 关联文件到知识库
  connectKnowledgeBase(fileId: string, knowledgeBaseId: string): Promise<Result<boolean>> {
    return request.post('/document/connect/knowledge-base', null, {
      params: { fileId, knowledgeBaseId }
    })
  },

  // 解析文档
  parseDocument(fileId: string): Promise<Result<void>> {
    return request.post('/document/parse/document', { fileId: fileId })
  },

  // 智能搜索 - 语义相似度搜索
  searchSimilar(knowledgeBaseId: string, query: string, topK: number): Promise<Result<SearchResult[]>> {
    return request.get('/document/searchSimilar', {
      params: { knowledgeBaseId, query, topK }
    })
  }
}
