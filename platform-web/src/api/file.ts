import request from '@/utils/request'
import type { Result } from '@/types'

// 文件详情
export interface FileDetail {
  id: string
  url: string
  size: number
  filename: string
  originalFilename: string
  basePath: string
  path: string
  ext: string
  contentType: string
  platform: string
  hashInfo: string
  uploadId: string
  uploadStatus: number
  knowledgeBaseId: number | null
  knowledgeBaseName: string | null
  createTime: string
}

// 分页响应
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// 获取文件列表（分页）
export function getFileList(
  page: number = 1,
  pageSize: number = 10,
  filename?: string
): Promise<Result<PageResult<FileDetail>>> {
  return request.get('/file/fileDetail/list', {
    params: { page, pageSize, filename }
  })
}

// 获取文件详情
export function getFileDetail(id: string): Promise<Result<FileDetail>> {
  return request.get(`/file/fileDetail/${id}`)
}

// 删除文件
export function deleteFiles(ids: string[]): Promise<Result<boolean>> {
  return request.delete(`/file/fileDetail/delete/${ids.join(',')}`)
}

// 预览文件（获取预签名URL）
export function previewFile(fileName: string): Promise<Result<string>> {
  return request.get(`/file/preview/${fileName}`)
}

// 秒传检查
export function checkFileExists(md5: string): Promise<Result<boolean>> {
  return request.get(`/file/fileDetail/check/${md5}`)
}

// 文件上传
export function uploadFile(files: File[]): Promise<Result<Array<{
  fileId: string
  fileName: string
  originalFilename: string
  url: string
  size: string
  contentType: string
}>>> {
  const formData = new FormData()
  files.forEach(file => formData.append('files', file))
  return request.post('/file/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
