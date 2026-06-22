import request from '@/utils/request'
import type { Result } from '@/types'

export interface OssConfig {
  ossConfigId: string
  configKey: string
  accessKey: string
  secretKey: string
  bucketName: string
  prefix: string
  endpoint: string
  domain: string
  isHttps: string
  region: string
  accessPolicy: string
  status: string
  extJson: string
  remark: string
  userId: number
  createdAt: string
  updatedAt: string
}

export interface CreateOssConfigDTO {
  configKey: string
  accessKey: string
  secretKey: string
  bucketName: string
  prefix?: string
  endpoint: string
  domain?: string
  isHttps?: string
  region?: string
  accessPolicy?: string
  status?: string
  extJson?: string
  remark?: string
}

export interface OssConfigPageResult {
  records: OssConfig[]
  total: number
  size: number
  current: number
  pages: number
}

export const ossConfigApi = {
  // 分页查询配置
  getConfigPage(
    page: number = 1,
    pageSize: number = 10,
    configKey?: string
  ): Promise<Result<OssConfigPageResult>> {
    return request.get('/system/oss/config/page', {
      params: { page, pageSize, configKey }
    })
  },

  // 查询所有配置
  getConfigList(): Promise<Result<OssConfig[]>> {
    return request.get('/system/oss/config/list')
  },

  // 根据ID查询配置
  getConfigById(ossConfigId: string): Promise<Result<OssConfig>> {
    return request.get(`/system/oss/config/${ossConfigId}`)
  },

  // 根据configKey查询配置
  getConfigByKey(configKey: string): Promise<Result<OssConfig>> {
    return request.get(`/system/oss/config/key/${configKey}`)
  },

  // 查询默认配置
  getDefaultConfig(): Promise<Result<OssConfig>> {
    return request.get('/system/oss/config/default')
  },

  // 新增配置
  createConfig(data: CreateOssConfigDTO): Promise<Result<void>> {
    return request.post('/system/oss/config', data)
  },

  // 修改配置
  updateConfig(data: OssConfig): Promise<Result<void>> {
    return request.put('/system/oss/config', data)
  },

  // 删除配置
  deleteConfig(ossConfigId: string): Promise<Result<void>> {
    return request.delete(`/system/oss/config/${ossConfigId}`)
  },

  // 设置默认配置
  setDefaultConfig(ossConfigId: string): Promise<Result<void>> {
    return request.put(`/system/oss/config/default/${ossConfigId}`)
  }
}
