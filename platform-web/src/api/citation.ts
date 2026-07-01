import request from '@/utils/request'

export interface CitationChunkDTO {
  id: string
  documentId: string
  content: string
  chunkIndex: number
  tokenSize: number
  sectionPath: string
  sectionTitle: string
  position: number
  charStartIndex: number
  charEndIndex: number
}

export function getCitationDetails(chunkIds: string[]) {
  return request.post<any, { data: CitationChunkDTO[] }>('/document/chunks/batch', chunkIds)
}
