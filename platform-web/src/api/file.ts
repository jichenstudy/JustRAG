import request from '@/utils/request'
import type { Result } from '@/types'
import SparkMD5 from 'spark-md5'

// 分片上传相关接口
export interface UploadUrlInfo {
  partNumber: number
  url: string
}

export interface InitUploadResponse {
  uploadId: string
  uploadUrls: UploadUrlInfo[]
}

export interface FileInfoVO {
  id: string
  fileId: string
  uploadId: string
  url: string
  fileName: string
  originalFilename: string
  size: number
  contentType: string
}

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
  thUrl: string
  thFilename: string
  thSize: number
  thContentType: string
  objectId: string
  objectType: string
  metadata: string
  userMetadata: string
  thMetadata: string
  thUserMetadata: string
  attr: string
  fileAcl: string
  thFileAcl: string
  hashInfo: string
  uploadId: string
  uploadStatus: number
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
  return request.get(`/file/minio/preview/${fileName}`)
}

// 秒传检查
export function checkFileExists(md5: string): Promise<Result<boolean>> {
  return request.get(`/file/fileDetail/check/${md5}`)
}

// 初始化分片上传
export function initMultipartUpload(
  fileName: string,
  partCount: number,
  totalSize: number
): Promise<Result<InitUploadResponse>> {
  return request.post('/file/minio/multipart/init', null, {
    params: { fileName, partCount, totalSize }
  })
}

// 记录分片上传完成
export function recordPartComplete(
  uploadId: string,
  partNumber: number
): Promise<Result<{ allCompleted: boolean }>> {
  return request.post('/file/minio/multipart/part/complete', null, {
    params: { uploadId, partNumber }
  })
}

// 完成分片上传、合并文件并入库
export function completeMultipartUpload(
  uploadId: string
): Promise<Result<FileInfoVO>> {
  return request.post('/file/fileUpload/multipart/complete', null, {
    params: { uploadId }
  })
}

// 取消分片上传
export function abortMultipartUpload(uploadId: string): Promise<Result<string>> {
  return request.post('/file/minio/multipart/abort', null, {
    params: { uploadId }
  })
}

// 上传分片到预签名URL
export async function uploadChunkToUrl(
  url: string,
  chunk: Blob,
  onProgress?: (progress: number) => void
): Promise<void> {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest()

    xhr.upload.onprogress = (event) => {
      if (event.lengthComputable && onProgress) {
        const progress = Math.round((event.loaded / event.total) * 100)
        onProgress(progress)
      }
    }

    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        resolve()
      } else {
        reject(new Error(`上传失败: ${xhr.status}`))
      }
    }

    xhr.onerror = () => reject(new Error('网络错误'))
    xhr.onabort = () => reject(new Error('上传已取消'))

    xhr.open('PUT', url)
    xhr.send(chunk)
  })
}

// 生成文件MD5哈希（基于文件名和大小，与后端保持一致）
export function generateFileHash(fileName: string, fileSize: number): string {
  const input = fileName + fileSize
  return SparkMD5.hash(input)
}
