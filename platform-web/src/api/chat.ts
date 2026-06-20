import request from '@/utils/request'
import type {
  Result,
  ChatSession,
  CreateChatSessionDTO,
  ChatMessage,
  SendMessageDTO
} from '@/types'

export const chatApi = {
  // 创建会话
  createSession(data: CreateChatSessionDTO): Promise<Result<string>> {
    return request.post('/chat/session', data)
  },

  // 获取所有会话
  getAllSessions(): Promise<Result<ChatSession[]>> {
    return request.get('/chat/session')
  },

  // 根据助理ID获取会话列表
  getSessionsByAssistantId(assistantId: string): Promise<Result<ChatSession[]>> {
    return request.get(`/chat/session/assistant/${assistantId}`)
  },

  // 根据ID获取会话
  getSessionById(id: string): Promise<Result<ChatSession>> {
    return request.get(`/chat/session/${id}`)
  },

  // 根据知识库ID获取会话
  getSessionsByKnowledgeBaseId(knowledgeBaseId: string): Promise<Result<ChatSession[]>> {
    return request.get(`/chat/session/knowledge-base/${knowledgeBaseId}`)
  },

  // 获取自由对话会话
  getFreeChatSessions(): Promise<Result<ChatSession[]>> {
    return request.get('/chat/session/free-chat')
  },

  // 更新会话标题
  updateSessionTitle(id: string, title: string): Promise<Result<void>> {
    return request.put(`/chat/session/${id}/title?title=${encodeURIComponent(title)}`)
  },

  // 删除会话
  deleteSession(id: string): Promise<Result<void>> {
    return request.delete(`/chat/session/${id}`)
  },

  // 发送消息
  sendMessage(sessionId: string, data: SendMessageDTO): Promise<Result<string>> {
    return request.post(`/chat/session/${sessionId}/message`, data)
  },

  // 获取会话消息
  getMessages(sessionId: string): Promise<Result<ChatMessage[]>> {
    return request.get(`/chat/session/${sessionId}/messages`)
  },

  // 获取最近消息
  getLatestMessages(sessionId: string, limit: number = 10): Promise<Result<ChatMessage[]>> {
    return request.get(`/chat/session/${sessionId}/messages/latest?limit=${limit}`)
  },

  // 流式对话 (支持可选的assistantId参数)
  streamChat(sessionId: string, message: string, assistantId?: string): EventSource {
    const encodedMessage = encodeURIComponent(message)
    const assistantIdParam = assistantId ? `&assistantId=${assistantId}` : ''
    // EventSource不支持自定义header，需要通过URL参数传递token
    const token = localStorage.getItem('token') || ''
    const tokenParam = token ? `&token=${encodeURIComponent(token)}` : ''
    // 使用当前域名，通过 Nginx 代理到后端
    const url = `/api/chat/session/${sessionId}/stream?message=${encodedMessage}${assistantIdParam}${tokenParam}`
    return new EventSource(url)
  }
}
