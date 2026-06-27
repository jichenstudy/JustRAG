export interface Result<T = any> {
  code: number
  message: string
  data: T
}

// 知识库相关类型
export interface KnowledgeBase {
  id: string
  name: string
  description: string
  embeddingModelId: string
  visionModelId?: string
  chunkStrategy: string
  chunkSize: number
  chunkOverlap: number
  chunkMinSize: number
  createdAt: string
  updatedAt: string
}

export interface CreateKnowledgeBaseDTO {
  name: string
  description?: string
  embeddingModelId?: string
  visionModelId?: string
  chunkStrategy?: string
  chunkSize?: number
  chunkOverlap?: number
  chunkMinSize?: number
}

export interface UpdateKnowledgeBaseDTO {
  name?: string
  description?: string
  embeddingModelId?: string
  visionModelId?: string
  chunkStrategy?: string
  chunkSize?: number
  chunkOverlap?: number
  chunkMinSize?: number
}

// 文档相关类型
export interface Document {
  id: string
  knowledgeBaseId: string
  fileId: string
  name: string
  docType: string
  parseStatus: string
  chunkCount: number
  createdAt: string
  updatedAt: string
}

export interface CreateDocumentDTO {
  knowledgeBaseId: string
  fileId: string
  name: string
  docType: string
}

export interface DocumentChunk {
  id: string
  documentId: string
  content: string
  chunkIndex: number
  tokenSize: number
  sectionPath: string | null
  sectionTitle: string | null
  position: number
  charStartIndex: number
  charEndIndex: number
  createdAt: string
}

// 搜索结果类型
export interface SearchResult {
  content: string
  score: number
  metadata: {
    documentId?: string
    chunkId?: string
    chunkIndex?: number
    knowledgeBaseId?: string
    [key: string]: any
  }
}

// 聊天助理相关类型
export interface ChatAssistant {
  id: string
  assistantAvatar: string | null
  assistantName: string
  assistantDescription: string
  emptyReply: string | null
  openingStatement: string | null
  knowledgeBaseId: string | null
  systemPrompt: string | null
  topP: number | null
  topN: number | null
  enableReasoningMode: number | null
  modelId: string | null
  temperature: number | null
  presencePenalty: number | null
  frequencyPenalty: number | null
  maxTokens: number | null
  createdTime: string
  updatedTime: string
}

export interface CreateChatAssistantDTO {
  assistantName: string
  assistantDescription: string
  assistantAvatar?: string
  emptyReply?: string
  openingStatement?: string
  knowledgeBaseId?: string
  systemPrompt?: string
  similarityThreshold?: number
  vectorWeight?: number
  topN?: number
  enableReasoningMode?: number
  modelId?: string
  temperature?: number
  presencePenalty?: number
  frequencyPenalty?: number
  maxTokens?: number
}

// 聊天相关类型
export interface ChatSession {
  id: string
  assistantId: string
  title: string
  knowledgeBaseId: string | null
  modelId?: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateChatSessionDTO {
  assistantId: string
  knowledgeBaseId?: string
  title?: string
  modelId?: string
}

export interface ChatMessage {
  id: string
  sessionId: string
  role: string
  content: string
  createdAt: string
  processSteps?: ProcessStep[]
  totalTokens?: { prompt: number; completion: number }
  totalElapsedMs?: number
}

export interface SendMessageDTO {
  content: string
}

// 过程追踪相关类型
export interface ProcessStep {
  type: string
  label?: string
  toolName?: string
  input?: string
  output?: string
  documentsCount?: number
  elapsedMs?: number
  content?: string
  timestamp: number
}

// 文件相关类型
export interface FileDetail {
  id: string
  url: string
  size: number
  filename: string
  originalFilename: string
  ext: string
  contentType: string
  createTime: string
}

// 认证相关类型
export interface LoginDTO {
  username: string  // 用户名或邮箱均可
  password: string
  source?: string
}

export interface LoginUserInfo {
  id: string
  username: string
  nickname: string
  avatar: string
  sex: number
  signature: string
  email?: string
  mobile?: string
  token: string
}

// 注册相关类型
export interface RegisterDTO {
  username: string
  email: string
  password: string
  nickname?: string
  captchaCode: string
}

// 重置密码相关类型
export interface ResetPasswordDTO {
  email: string
  captchaCode: string
  newPassword: string
}

// 个人信息修改
export interface UpdateProfileDTO {
  nickname?: string
  avatar?: string
  sex?: number
  signature?: string
  email?: string
  mobile?: string
}

// 团队相关类型
export interface SysUserTeamVo {
  userId: string
  teamId: string
  username: string
  email: string
  status: number
  createAt: string
}
