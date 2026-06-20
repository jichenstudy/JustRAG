import request from '@/utils/request'
import type { Result } from '@/types'

export interface AiModelConfig {
  id: string
  modelName: string
  modelType: string  // CHAT 或 EMBEDDING
  provider: string
  apiEndpoint?: string
  modelId: string
  description?: string
  createdAt: string
  updatedAt: string
}

export interface CreateAiModelConfigDTO {
  modelName: string
  modelType: string  // CHAT 或 EMBEDDING
  provider: string
  apiKey?: string
  apiEndpoint?: string
  description?: string
}

export const modelApi = {
  // 获取所有模型配置
  getAllModels(): Promise<Result<AiModelConfig[]>> {
    return request.get('/model')
  },

  // 获取所有启用的模型配置
  getEnabledModels(): Promise<Result<AiModelConfig[]>> {
    return request.get('/model/enabled')
  },

  // 创建模型配置
  createModel(data: CreateAiModelConfigDTO): Promise<Result<number>> {
    return request.post('/model', data)
  },

  // 更新模型配置
  updateModel(id: string, data: CreateAiModelConfigDTO): Promise<Result<void>> {
    return request.put(`/model/${id}`, data)
  },

  // 删除模型配置
  deleteModel(id: string): Promise<Result<void>> {
    return request.delete(`/model/${id}`)
  },

  // 设置默认模型
  setDefaultModel(id: string): Promise<Result<void>> {
    return request.put(`/model/${id}/default`)
  },

  // 启用/禁用模型
  toggleModelStatus(id: string, enabled: boolean): Promise<Result<void>> {
    return request.put(`/model/${id}/status?enabled=${enabled}`)
  },

  // 获取所有可用的聊天模型配置（已配置的，不管是否启用）
  getAvailableChatModelConfigs(): Promise<Result<AiModelConfig[]>> {
    return request.get('/model/getAvailableChatModelConfigs')
  },

  // 获取所有可用的嵌入模型配置
  getAvailableEmbeddingModelConfigs(): Promise<Result<AiModelConfig[]>> {
    return request.get('/model/getAvailableEmbeddingModelConfigs')
  }
}
