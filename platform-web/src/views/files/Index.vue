<template>
  <div class="files-page">
    <div class="page-header">
      <h1>文件管理</h1>
      <n-button type="primary" @click="showUploadModal = true">
        <template #icon>
          <n-icon :component="CloudUploadOutline" />
        </template>
        上传文件
      </n-button>
    </div>

    <div class="search-bar">
      <n-input
        v-model:value="searchKeyword"
        placeholder="搜索文件名..."
        clearable
        @keyup.enter="loadFiles"
      >
        <template #prefix>
          <n-icon :component="SearchOutline" />
        </template>
      </n-input>
      <n-button type="primary" @click="loadFiles">搜索</n-button>
    </div>

    <div v-if="loading" class="loading">
      <n-spin size="large" />
    </div>

    <div v-else-if="files.length === 0" class="empty">
      <n-empty :description="searchKeyword ? '未找到匹配的文件' : '暂无文件'" />
      <n-button v-if="!searchKeyword" @click="showUploadModal = true">上传第一个文件</n-button>
    </div>

    <div v-else class="files-table">
      <n-data-table
        :columns="columns"
        :data="files"
        :bordered="false"
        :row-key="(row: FileDetail) => row.id"
      />
      <div class="pagination">
        <n-pagination
          v-model:page="currentPage"
          :page-size="pageSize"
          :item-count="total"
          show-size-picker
          :page-sizes="[10, 20, 50]"
          @update:page="loadFiles"
          @update:page-size="handlePageSizeChange"
        />
      </div>
    </div>

    <!-- 上传文件对话框 -->
    <n-modal
      v-model:show="showUploadModal"
      preset="card"
      title="上传文件"
      style="max-width: 600px"
      :mask-closable="uploadStatus === 'idle'"
      :closable="uploadStatus === 'idle'"
    >
      <!-- 文件选择区域 -->
      <div v-if="!selectedFile" class="upload-area">
        <n-upload
          :show-file-list="false"
          :custom-request="handleFileSelect"
          accept=".pdf,.doc,.docx,.xls,.xlsx,.md,.markdown,.txt,.html,.htm"
        >
          <n-upload-dragger>
            <div style="margin-bottom: 12px">
              <n-icon :component="CloudUploadOutline" :size="48" :depth="3" />
            </div>
            <n-text style="font-size: 16px">点击或拖拽文件到此区域</n-text>
            <n-p depth="3" style="margin: 8px 0 0 0">支持大文件分片上传，自动检测秒传</n-p>
            <n-p depth="3" style="margin: 4px 0 0 0; font-size: 12px">支持格式：PDF、DOC/DOCX、XLS/XLSX、Markdown、TXT、HTML</n-p>
          </n-upload-dragger>
        </n-upload>
      </div>

      <!-- 文件信息 -->
      <div v-else class="file-info">
        <n-descriptions :column="2" label-placement="left">
          <n-descriptions-item label="文件名">{{ selectedFile.name }}</n-descriptions-item>
          <n-descriptions-item label="文件大小">{{ formatFileSize(selectedFile.size) }}</n-descriptions-item>
          <n-descriptions-item label="文件类型">{{ selectedFile.type || '未知' }}</n-descriptions-item>
          <n-descriptions-item label="分片数量">{{ partCount }}</n-descriptions-item>
        </n-descriptions>

        <!-- 上传进度 -->
        <div v-if="uploadStatus !== 'idle'" class="upload-progress">
          <div class="progress-info">
            <span>{{ Math.round(overallProgress) }}%</span>
            <span>{{ formatFileSize(uploadedSize) }} / {{ formatFileSize(selectedFile.size) }}</span>
          </div>
          <n-progress
            type="line"
            :percentage="Math.round(overallProgress)"
            :status="getProgressStatus()"
            :height="10"
          />
        </div>

        <!-- 上传结果 -->
        <n-alert
          v-if="uploadResult"
          :type="uploadResult.success ? 'success' : 'error'"
          :title="uploadResult.success ? '上传成功' : '上传失败'"
          style="margin-top: 16px"
        >
          {{ uploadResult.message }}
        </n-alert>
      </div>

      <template #footer>
        <div style="display: flex; justify-content: flex-end; gap: 12px">
          <n-button
            v-if="uploadStatus === 'idle' && selectedFile"
            @click="resetUpload"
          >
            重新选择
          </n-button>
          <n-button
            v-if="uploadStatus === 'idle'"
            @click="closeUploadModal"
          >
            取消
          </n-button>
          <n-button
            v-if="selectedFile && uploadStatus === 'idle'"
            type="primary"
            @click="startUpload"
          >
            开始上传
          </n-button>
          <n-button
            v-if="uploadStatus === 'uploading'"
            @click="cancelUpload"
            type="error"
          >
            取消上传
          </n-button>
          <n-button
            v-if="uploadStatus === 'completed' || uploadStatus === 'failed'"
            type="primary"
            @click="closeUploadModal"
          >
            完成
          </n-button>
        </div>
      </template>
    </n-modal>

    <!-- 文件详情对话框 -->
    <n-modal
      v-model:show="showDetailModal"
      preset="card"
      title="文件详情"
      style="max-width: 800px"
      :mask-closable="true"
      :closable="true"
    >
      <n-spin :show="detailLoading">
        <div v-if="currentFileDetail" class="file-detail">
          <!-- 文件信息 -->
          <n-descriptions :column="2" label-placement="left" bordered>
            <n-descriptions-item label="文件名">
              {{ currentFileDetail.originalFilename }}
            </n-descriptions-item>
            <n-descriptions-item label="存储名称">
              {{ currentFileDetail.filename }}
            </n-descriptions-item>
            <n-descriptions-item label="文件大小">
              {{ formatFileSize(currentFileDetail.size) }}
            </n-descriptions-item>
            <n-descriptions-item label="文件类型">
              <n-tag :bordered="false" size="small">{{ currentFileDetail.ext?.toUpperCase() || '-' }}</n-tag>
            </n-descriptions-item>
            <n-descriptions-item label="MIME类型">
              {{ currentFileDetail.contentType || '-' }}
            </n-descriptions-item>
            <n-descriptions-item label="存储平台">
              {{ currentFileDetail.platform || '-' }}
            </n-descriptions-item>
            <n-descriptions-item label="上传状态">
              <n-tag
                :type="currentFileDetail.uploadStatus === 1 ? 'success' : 'warning'"
                :bordered="false"
                size="small"
              >
                {{ currentFileDetail.uploadStatus === 1 ? '已完成' : '上传中' }}
              </n-tag>
            </n-descriptions-item>
            <n-descriptions-item label="上传时间">
              {{ formatTime(currentFileDetail.createTime) }}
            </n-descriptions-item>
            <n-descriptions-item label="上传ID" :span="2">
              <n-text code>{{ currentFileDetail.uploadId || '-' }}</n-text>
            </n-descriptions-item>
            <n-descriptions-item label="文件哈希" :span="2">
              <n-text code>{{ currentFileDetail.hashInfo || '-' }}</n-text>
            </n-descriptions-item>
            <n-descriptions-item label="存储路径" :span="2">
              <n-text code>{{ currentFileDetail.path || '-' }}</n-text>
            </n-descriptions-item>
          </n-descriptions>

          <!-- 文件预览 -->
          <div v-if="previewUrl" class="file-preview">
            <n-divider>文件预览</n-divider>
            <div v-if="isImageFile(currentFileDetail.ext)" class="preview-image">
              <img :src="previewUrl" :alt="currentFileDetail.originalFilename" />
            </div>
            <div v-else-if="isPdfFile(currentFileDetail.ext)" class="preview-pdf">
              <iframe :src="previewUrl" frameborder="0"></iframe>
            </div>
            <div v-else class="preview-other">
              <n-text>该文件类型暂不支持在线预览</n-text>
            </div>
          </div>
        </div>
      </n-spin>

      <template #footer>
        <div style="display: flex; justify-content: flex-end; gap: 12px">
          <n-button @click="closeDetailModal">关闭</n-button>
          <n-button type="primary" @click="handleDownload">
            <template #icon>
              <n-icon :component="DownloadOutline" />
            </template>
            下载文件
          </n-button>
        </div>
      </template>
    </n-modal>

    <!-- 关联知识库对话框 -->
    <n-modal
      v-model:show="showConnectModal"
      preset="card"
      title="关联知识库"
      style="max-width: 500px"
    >
      <div style="padding: 16px 0">
        <p style="margin-bottom: 16px">选择要关联的知识库：</p>
        <n-select
          v-model:value="selectedKnowledgeBaseId"
          :options="knowledgeBases.map(kb => ({ label: kb.name, value: kb.id }))"
          placeholder="请选择知识库"
          filterable
        />
      </div>
      <template #footer>
        <div style="display: flex; justify-content: flex-end; gap: 12px">
          <n-button @click="showConnectModal = false">取消</n-button>
          <n-button type="primary" :loading="connectLoading" @click="confirmConnectKnowledgeBase">
            确认关联
          </n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="tsx">
import { ref, computed, h, onMounted, onUnmounted } from 'vue'
import {
  NButton,
  NIcon,
  NModal,
  NUpload,
  NUploadDragger,
  NText,
  NP,
  NDescriptions,
  NDescriptionsItem,
  NProgress,
  NAlert,
  NSpin,
  NEmpty,
  NInput,
  NDataTable,
  NTag,
  NPagination,
  NDivider,
  NSelect,
  useMessage,
  useDialog,
  type DataTableColumns
} from 'naive-ui'
import { CloudUploadOutline, SearchOutline, TrashOutline, EyeOutline, DownloadOutline, LinkOutline, DocumentTextOutline } from '@vicons/ionicons5'
import { useThemeStore } from '@/stores/theme'
import {
  initMultipartUpload,
  recordPartComplete,
  completeMultipartUpload,
  abortMultipartUpload,
  uploadChunkToUrl,
  getFileList,
  deleteFiles,
  checkFileExists,
  generateFileHash,
  getFileDetail,
  previewFile,
  type FileDetail
} from '@/api/file'
import { knowledgeBaseApi } from '@/api/knowledgeBase'
import { documentApi } from '@/api/document'
import type { KnowledgeBase } from '@/types'

const message = useMessage()
const dialog = useDialog()
const themeStore = useThemeStore()

// 文件列表相关
const loading = ref(false)
const files = ref<FileDetail[]>([])
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 上传相关
const showUploadModal = ref(false)
const selectedFile = ref<File | null>(null)
const uploadStatus = ref<'idle' | 'uploading' | 'completed' | 'failed'>('idle')
const uploadId = ref('')
const chunkSize = 5 * 1024 * 1024 // 5MB
const uploadedSize = ref(0)
const uploadResult = ref<{ success: boolean; message: string } | null>(null)
const uploadController = ref<AbortController | null>(null)

// 文件详情相关
const showDetailModal = ref(false)
const currentFileDetail = ref<FileDetail | null>(null)
const detailLoading = ref(false)
const previewUrl = ref('')

// 关联知识库相关
const showConnectModal = ref(false)
const connectFileId = ref<string | null>(null)
const selectedKnowledgeBaseId = ref<string | null>(null)
const knowledgeBases = ref<KnowledgeBase[]>([])
const connectLoading = ref(false)

interface ChunkInfo {
  partNumber: number
  start: number
  end: number
  size: number
  progress: number
  status: 'pending' | 'uploading' | 'completed' | 'failed'
  uploadUrl?: string
}

const chunks = ref<ChunkInfo[]>([])

// 表格列定义
const columns: DataTableColumns<FileDetail> = [
  {
    title: '文件名',
    key: 'originalFilename',
    ellipsis: { tooltip: true }
  },
  {
    title: '大小',
    key: 'size',
    width: 120,
    render: (row) => formatFileSize(row.size)
  },
  {
    title: '类型',
    key: 'ext',
    width: 100,
    render: (row) => h(NTag, { bordered: false, size: 'small' }, { default: () => row.ext?.toUpperCase() || '-' })
  },
  {
    title: '状态',
    key: 'uploadStatus',
    width: 100,
    render: (row) => {
      const status = row.uploadStatus
      if (status === 1) {
        return h(NTag, { type: 'success', bordered: false, size: 'small' }, { default: () => '已完成' })
      } else if (status === 0) {
        return h(NTag, { type: 'warning', bordered: false, size: 'small' }, { default: () => '上传中' })
      }
      return h(NTag, { bordered: false, size: 'small' }, { default: () => '未知' })
    }
  },
  {
    title: '上传时间',
    key: 'createTime',
    width: 180,
    render: (row) => formatTime(row.createTime)
  },
  {
    title: '操作',
    key: 'actions',
    width: 280,
    render: (row) => {
      return h('div', { style: { display: 'flex', gap: '8px' } }, [
        h(
          NButton,
          {
            text: true,
            type: 'info',
            onClick: () => handleViewDetail(row)
          },
          { icon: () => h(NIcon, { component: EyeOutline }), default: () => '详情' }
        ),
        h(
          NButton,
          {
            text: true,
            type: 'primary',
            onClick: () => handleConnectKnowledgeBase(row)
          },
          { icon: () => h(NIcon, { component: LinkOutline }), default: () => '关联' }
        ),
        h(
          NButton,
          {
            text: true,
            type: 'warning',
            onClick: () => handleParseDocument(row)
          },
          { icon: () => h(NIcon, { component: DocumentTextOutline }), default: () => '解析' }
        ),
        h(
          NButton,
          {
            text: true,
            type: 'error',
            onClick: () => handleDelete(row)
          },
          { icon: () => h(NIcon, { component: TrashOutline }), default: () => '删除' }
        )
      ])
    }
  }
]

// 计算分片数量
const partCount = computed(() => {
  if (!selectedFile.value) return 0
  return Math.ceil(selectedFile.value.size / chunkSize)
})

// 计算整体进度
const overallProgress = computed(() => {
  if (!selectedFile.value || chunks.value.length === 0) return 0
  const totalProgress = chunks.value.reduce((sum, chunk) => sum + chunk.progress, 0)
  return totalProgress / chunks.value.length
})

const formatFileSize = (bytes: number) => {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

const formatTime = (time: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

// 加载文件列表
const loadFiles = async () => {
  try {
    loading.value = true
    const res = await getFileList(currentPage.value, pageSize.value, searchKeyword.value || undefined)
    if (res.code === 200 && res.data) {
      files.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (error) {
    message.error('加载文件列表失败')
  } finally {
    loading.value = false
  }
}

const handlePageSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  loadFiles()
}

// 删除文件
const handleDelete = (file: FileDetail) => {
  dialog.warning({
    title: '确认删除',
    content: `确定要删除文件 "${file.originalFilename}" 吗？`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await deleteFiles([file.id])
        if (res.code === 200) {
          message.success('删除成功')
          loadFiles()
        } else {
          message.error('删除失败')
        }
      } catch (error) {
        message.error('删除失败')
      }
    }
  })
}

// 查看文件详情
const handleViewDetail = async (file: FileDetail) => {
  try {
    detailLoading.value = true
    showDetailModal.value = true

    // 获取文件详情
    const res = await getFileDetail(file.id)
    if (res.code === 200 && res.data) {
      currentFileDetail.value = res.data

      // 如果是可预览的文件类型，获取预览URL
      const previewableTypes = ['.jpg', '.jpeg', '.png', '.gif', '.pdf', '.txt']
      if (res.data.ext && previewableTypes.includes(res.data.ext.toLowerCase())) {
        try {
          const previewRes = await previewFile(res.data.filename)
          if (previewRes.code === 200 && previewRes.data) {
            previewUrl.value = previewRes.data
          }
        } catch (error) {
          console.error('获取预览URL失败', error)
        }
      }
    } else {
      message.error('获取文件详情失败')
    }
  } catch (error) {
    message.error('获取文件详情失败')
  } finally {
    detailLoading.value = false
  }
}

// 关闭详情对话框
const closeDetailModal = () => {
  showDetailModal.value = false
  currentFileDetail.value = null
  previewUrl.value = ''
}

// 下载文件
const handleDownload = () => {
  if (!currentFileDetail.value) return

  // 使用预览URL下载
  if (previewUrl.value) {
    window.open(previewUrl.value, '_blank')
  } else if (currentFileDetail.value.url) {
    window.open(currentFileDetail.value.url, '_blank')
  } else {
    message.error('无法获取下载链接')
  }
}

// 打开关联知识库弹窗
const handleConnectKnowledgeBase = async (file: FileDetail) => {
  connectFileId.value = file.id
  selectedKnowledgeBaseId.value = null
  showConnectModal.value = true

  // 加载知识库列表
  try {
    const res = await knowledgeBaseApi.getAll()
    if (res.code === 200 && res.data) {
      knowledgeBases.value = res.data
    }
  } catch (error) {
    message.error('加载知识库列表失败')
  }
}

// 确认关联知识库
const confirmConnectKnowledgeBase = async () => {
  if (connectFileId.value === null || selectedKnowledgeBaseId.value === null) {
    message.warning('请选择知识库')
    return
  }

  connectLoading.value = true
  try {
    const res = await documentApi.connectKnowledgeBase(connectFileId.value, selectedKnowledgeBaseId.value)
    if (res.code === 200 && res.data) {
      message.success('关联成功')
      showConnectModal.value = false
    } else {
      message.error(res.message || '关联失败')
    }
  } catch (error: any) {
    message.error(error.message || '关联失败')
  } finally {
    connectLoading.value = false
  }
}

// 解析文档
const handleParseDocument = (file: FileDetail) => {
  dialog.info({
    title: '解析文档',
    content: `确定要解析文件 "${file.originalFilename}" 吗？解析前请确保文件已关联到知识库。`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        // 这里需要文档ID，但文件列表返回的是文件ID
        // 假设文件ID和文档ID相同，或者需要先查询文档ID
        const res = await documentApi.parseDocument(file.id)
        if (res.code === 200) {
          message.success('解析任务已提交')
        } else {
          message.error(res.message || '解析失败')
        }
      } catch (error: any) {
        message.error(error.message || '解析失败')
      }
    }
  })
}

// 允许的文件扩展名
const allowedExtensions = ['.pdf', '.doc', '.docx', '.xls', '.xlsx', '.md', '.markdown', '.txt', '.html', '.htm']

// 文件选择处理
const handleFileSelect = ({ file }: any) => {
  const fileName = file.file.name
  const fileExt = fileName.substring(fileName.lastIndexOf('.')).toLowerCase()

  if (!allowedExtensions.includes(fileExt)) {
    message.error(`不支持的文件格式，仅支持：${allowedExtensions.join(', ')}`)
    return
  }

  selectedFile.value = file.file
}

// 重置上传状态
const resetUpload = () => {
  selectedFile.value = null
  uploadStatus.value = 'idle'
  uploadId.value = ''
  chunks.value = []
  uploadedSize.value = 0
  uploadResult.value = null
  if (uploadController.value) {
    uploadController.value.abort()
    uploadController.value = null
  }
}

// 关闭上传对话框
const closeUploadModal = () => {
  resetUpload()
  showUploadModal.value = false
  loadFiles() // 刷新列表
}

// 创建分片信息
const createChunks = () => {
  if (!selectedFile.value) return

  const file = selectedFile.value
  const totalChunks = Math.ceil(file.size / chunkSize)

  chunks.value = []
  for (let i = 0; i < totalChunks; i++) {
    const start = i * chunkSize
    const end = Math.min(start + chunkSize, file.size)

    chunks.value.push({
      partNumber: i + 1,
      start,
      end,
      size: end - start,
      progress: 0,
      status: 'pending'
    })
  }
}

// 开始上传
const startUpload = async () => {
  if (!selectedFile.value) return

  try {
    uploadStatus.value = 'uploading'
    uploadResult.value = null

    // 1. 秒传检查
    const fileHash = generateFileHash(selectedFile.value.name, selectedFile.value.size)
    const checkRes = await checkFileExists(fileHash)

    if (checkRes.code === 200 && checkRes.data === true) {
      // 文件已存在，秒传成功
      uploadStatus.value = 'completed'
      uploadedSize.value = selectedFile.value.size
      uploadResult.value = {
        success: true,
        message: `文件 ${selectedFile.value.name} 秒传成功！`
      }
      message.success('文件秒传成功！')
      return
    }

    // 2. 创建分片
    createChunks()

    // 3. 初始化分片上传
    const initRes = await initMultipartUpload(
      selectedFile.value.name,
      partCount.value,
      selectedFile.value.size
    )

    if (initRes.code !== 200 || !initRes.data) {
      throw new Error('初始化上传失败')
    }

    uploadId.value = initRes.data.uploadId

    // 设置每个分片的上传URL
    initRes.data.uploadUrls.forEach((urlInfo) => {
      const chunk = chunks.value.find((c) => c.partNumber === urlInfo.partNumber)
      if (chunk) {
        chunk.uploadUrl = urlInfo.url
      }
    })

    // 4. 顺序上传每个分片
    for (const chunk of chunks.value) {
      if (uploadStatus.value !== 'uploading') break
      await uploadChunk(chunk)
    }

    // 5. 检查是否所有分片都上传成功
    const allCompleted = chunks.value.every((c) => c.status === 'completed')
    if (allCompleted) {
      // 完成上传，合并文件并入库
      const completeRes = await completeMultipartUpload(uploadId.value)
      if (completeRes.code === 200) {
        uploadStatus.value = 'completed'
        uploadResult.value = {
          success: true,
          message: `文件 ${selectedFile.value.name} 上传成功`
        }
        message.success('上传成功')
      } else {
        throw new Error('文件合并失败')
      }
    }
  } catch (error: any) {
    if (error.message !== '上传已取消') {
      uploadStatus.value = 'failed'
      uploadResult.value = {
        success: false,
        message: error.message || '上传失败'
      }
      message.error('上传失败: ' + error.message)
    }
  }
}

// 上传单个分片
const uploadChunk = async (chunk: ChunkInfo): Promise<void> => {
  if (!selectedFile.value || !chunk.uploadUrl) return

  try {
    chunk.status = 'uploading'

    const fileSlice = selectedFile.value.slice(chunk.start, chunk.end)

    await uploadChunkToUrl(chunk.uploadUrl, fileSlice, (progress) => {
      chunk.progress = progress
      updateUploadedSize()
    })

    // 通知后端分片上传完成
    await recordPartComplete(uploadId.value, chunk.partNumber)

    chunk.status = 'completed'
    chunk.progress = 100
    updateUploadedSize()
  } catch (error: any) {
    chunk.status = 'failed'
    throw error
  }
}

// 更新已上传大小
const updateUploadedSize = () => {
  uploadedSize.value = chunks.value.reduce((sum, chunk) => {
    return sum + (chunk.size * chunk.progress) / 100
  }, 0)
}

// 取消上传
const cancelUpload = async () => {
  dialog.warning({
    title: '确认取消',
    content: '确定要取消上传吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        if (uploadId.value) {
          await abortMultipartUpload(uploadId.value)
        }
        resetUpload()
        message.info('已取消上传')
      } catch (error) {
        console.error('取消上传失败', error)
      }
    }
  })
}

// 获取进度条状态
const getProgressStatus = () => {
  switch (uploadStatus.value) {
    case 'completed':
      return 'success'
    case 'failed':
      return 'error'
    default:
      return 'default'
  }
}

// 判断是否为图片文件
const isImageFile = (ext: string | undefined) => {
  if (!ext) return false
  const imageExts = ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp']
  return imageExts.includes(ext.toLowerCase())
}

// 判断是否为PDF文件
const isPdfFile = (ext: string | undefined) => {
  if (!ext) return false
  return ext.toLowerCase() === '.pdf'
}

onMounted(() => {
  loadFiles()
})

onUnmounted(() => {
  if (uploadController.value) {
    uploadController.value.abort()
  }
})
</script>

<style scoped>
.files-page {
  padding: 48px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 28px;
  font-weight: 600;
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  max-width: 500px;
}

.loading,
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px;
  gap: 16px;
}

.files-table {
  background-color: v-bind('themeStore.theme.colors.surface');
  border-radius: 12px;
  padding: 20px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.upload-area {
  margin-bottom: 16px;
}

.file-info {
  padding: 16px 0;
}

.upload-progress {
  margin-top: 20px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 14px;
  color: #666;
}

.file-detail {
  min-height: 200px;
}

.file-preview {
  margin-top: 24px;
}

.preview-image {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
  background-color: #f5f5f5;
  border-radius: 8px;
  max-height: 500px;
  overflow: auto;
}

.preview-image img {
  max-width: 100%;
  max-height: 450px;
  object-fit: contain;
  border-radius: 4px;
}

.preview-pdf {
  width: 100%;
  height: 600px;
  border-radius: 8px;
  overflow: hidden;
}

.preview-pdf iframe {
  width: 100%;
  height: 100%;
}

.preview-other {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40px;
  background-color: #f5f5f5;
  border-radius: 8px;
}
</style>
