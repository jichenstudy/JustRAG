import request from '@/utils/request'
import type { Result, Page } from '@/types'

export interface AiMcpServerConfig {
  id: string
  name: string
  type: string
  fullCommand?: string
  url?: string
  env?: string
  isEnabled: number
  createdAt: string
  updatedAt: string
}

export interface McpServerDTO {
  name: string
  type: string
  fullCommand?: string
  url?: string
  env?: string
}

export const mcpApi = {
  // 分页查询
  searchPage(page: number, pageSize: number): Promise<Result<Page<AiMcpServerConfig>>> {
    return request.get('/mcp/page', {
      params: { page, pageSize }
    })
  },

  // 创建MCP服务器配置
  create(data: McpServerDTO): Promise<Result<void>> {
    return request.post('/mcp', data)
  },

  // 更新MCP服务器配置
  update(id: string, data: McpServerDTO): Promise<Result<void>> {
    return request.put(`/mcp/${id}`, data)
  },

  // 删除MCP服务器配置
  delete(id: string): Promise<Result<void>> {
    return request.delete(`/mcp/${id}`)
  },

  // 启用/禁用MCP服务器
  toggleEnabled(id: string): Promise<Result<void>> {
    return request.get(`/mcp/enabled`, {
      params: { id }
    })
  },

  // 获取MCP服务器支持的所有工具
  getAllTools(data: McpServerDTO): Promise<Result<Record<string, string>>> {
    return request.post('/mcp/getAllTools', data)
  }
}
