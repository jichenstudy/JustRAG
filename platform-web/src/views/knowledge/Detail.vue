<template>
  <div class="knowledge-detail-page">
    <div v-if="loading" class="loading">
      <n-spin size="large" />
    </div>

    <template v-else-if="knowledgeBase">
      <div class="page-header">
        <div class="back-button">
          <n-button text @click="router.back()">
            <n-icon :component="ArrowBackOutline" :size="20" />
            返回
          </n-button>
        </div>
        <div class="header-content">
          <h1>{{ knowledgeBase.name }}</h1>
          <p>{{ knowledgeBase.description }}</p>
        </div>
      </div>

      <n-tabs type="line" animated>
        <n-tab-pane name="documents" tab="文档列表">
          <div class="documents-section">
            <div class="section-header">
              <h2>文档 ({{ documents.length }})</h2>
              <n-button type="primary" @click="showUploadModal = true">
                <template #icon>
                  <n-icon :component="AddOutline" />
                </template>
                上传文档
              </n-button>
            </div>

            <div v-if="documentsLoading" class="loading">
              <n-spin />
            </div>

            <div v-else-if="documents.length === 0" class="empty">
              <n-empty description="暂无文档" />
              <n-button @click="showUploadModal = true">上传第一个文档</n-button>
            </div>

            <div v-else class="documents-table">
              <n-data-table
                :columns="docColumns"
                :data="documents"
                :bordered="false"
                :row-key="(row: Document) => row.id"
              />
            </div>
          </div>
        </n-tab-pane>

        <n-tab-pane name="config" tab="配置">
          <div class="config-section">
            <n-form ref="configFormRef" :model="configForm" :rules="configRules" label-placement="left" label-width="120px">
              <n-form-item label="名称" path="name">
                <n-input v-model:value="configForm.name" placeholder="请输入知识库名称" />
              </n-form-item>
              <n-form-item label="简介" path="description">
                <n-input
                  v-model:value="configForm.description"
                  type="textarea"
                  :rows="3"
                  placeholder="请输入知识库简介"
                />
              </n-form-item>
              <n-form-item label="向量模型" path="modelId">
                <n-select
                  v-model:value="configForm.modelId"
                  :options="embeddingModels"
                  :loading="loadingModels"
                  placeholder="请选择向量模型"
                  disabled
                  clearable
                />
              </n-form-item>
              <n-form-item label="切分策略">
                <n-select
                  v-model:value="configForm.chunkStrategy"
                  :options="strategyOptions"
                  placeholder="结构感知"
                />
              </n-form-item>
              <n-form-item label="分片大小">
                <n-input-number v-model:value="configForm.chunkSize" :min="100" :max="5000" />
                <span class="form-hint">{{ configForm.chunkStrategy === 'SMART' ? '超过此大小的章节按段落递归细分' : '固定窗口字符数' }}</span>
              </n-form-item>
              <n-form-item label="最小分片">
                <n-input-number v-model:value="configForm.chunkMinSize" :min="50" :max="500" placeholder="100" />
                <span class="form-hint">小于此大小的分片将被过滤</span>
              </n-form-item>
              <template v-if="configForm.chunkStrategy === 'FIXED'">
                <n-form-item label="重叠大小">
                  <n-input-number v-model:value="configForm.chunkOverlap" :min="0" :max="1000" placeholder="200" />
                  <span class="form-hint">相邻分片重叠字符数，避免边界截断</span>
                </n-form-item>
              </template>
              <n-form-item label="创建时间">
                <span>{{ formatTime(knowledgeBase.createdAt) }}</span>
              </n-form-item>
              <n-form-item label="更新时间">
                <span>{{ formatTime(knowledgeBase.updatedAt) }}</span>
              </n-form-item>
              <n-form-item>
                <n-button type="primary" @click="handleSaveConfig" :loading="savingConfig">
                  保存配置
                </n-button>
              </n-form-item>
            </n-form>
          </div>
        </n-tab-pane>
      </n-tabs>
    </template>

    <!-- 文件详情对话框 -->
    <n-modal
      v-model:show="showFileDetailModal"
      preset="card"
      title="文件详情"
      style="max-width: 700px"
    >
      <n-spin :show="fileDetailLoading">
        <div v-if="currentFileDetail" class="file-detail-content">
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
            <n-descriptions-item label="文件哈希" :span="2">
              <n-text code>{{ currentFileDetail.hashInfo || '-' }}</n-text>
            </n-descriptions-item>
            <n-descriptions-item label="存储路径" :span="2">
              <n-text code>{{ currentFileDetail.path || '-' }}</n-text>
            </n-descriptions-item>
          </n-descriptions>
        </div>
        <n-empty v-else-if="!fileDetailLoading" description="无法获取文件详情" />
      </n-spin>
      <template #footer>
        <div style="display: flex; justify-content: flex-end">
          <n-button @click="showFileDetailModal = false">关闭</n-button>
        </div>
      </template>
    </n-modal>

    <!-- 分块查看对话框 -->
    <n-modal
      v-model:show="showChunksModal"
      preset="card"
      :title="`文档分块 - ${currentDocName}`"
      style="max-width: 900px; max-height: 80vh"
    >
      <n-spin :show="chunksLoading">
        <div v-if="currentChunks.length > 0" class="chunks-list">
          <div
            v-for="chunk in currentChunks"
            :key="chunk.id"
            class="chunk-item"
            :style="{ backgroundColor: themeStore.theme.colors.surface, borderColor: themeStore.theme.colors.border }"
          >
            <div class="chunk-header">
              <n-tag size="small" :bordered="false">分块 #{{ chunk.chunkIndex + 1 }}</n-tag>
              <span class="chunk-tokens">{{ chunk.tokenSize }} tokens</span>
            </div>
            <div class="chunk-content">{{ chunk.content }}</div>
          </div>
        </div>
        <n-empty v-else-if="!chunksLoading" description="暂无分块数据" />
      </n-spin>
      <template #footer>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span style="color: #999; font-size: 12px">共 {{ currentChunks.length }} 个分块</span>
          <n-button @click="showChunksModal = false">关闭</n-button>
        </div>
      </template>
    </n-modal>

    <!-- 上传文档对话框 -->
    <n-modal
      v-model:show="showUploadModal"
      preset="card"
      title="上传文档"
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
              <n-icon :component="DocumentTextOutline" :size="48" :depth="3" />
            </div>
            <n-text style="font-size: 16px">点击或拖拽文件到此区域上传</n-text>
            <n-p depth="3" style="margin: 8px 0 0 0">支持文件上传</n-p>
            <n-p depth="3" style="margin: 4px 0 0 0; font-size: 12px">
              支持格式: PDF, Word, Excel, TXT, Markdown, HTML
            </n-p>
          </n-upload-dragger>
        </n-upload>
      </div>

      <!-- 文件信息 -->
      <div v-else class="file-info">
        <n-descriptions :column="2" label-placement="left">
          <n-descriptions-item label="文件名">{{ selectedFile.name }}</n-descriptions-item>
          <n-descriptions-item label="文件大小">{{ formatFileSize(selectedFile.size) }}</n-descriptions-item>
          <n-descriptions-item label="文件类型">{{ selectedFile.type || '未知' }}</n-descriptions-item>
        </n-descriptions>

        <!-- 立即解析选项 -->
        <div v-if="uploadStatus === 'idle'" class="parse-option">
          <span>立即解析：</span>
          <n-switch v-model:value="isParse">
            <template #checked>是</template>
            <template #unchecked>否</template>
          </n-switch>
          <n-text depth="3" style="margin-left: 12px; font-size: 12px">
            开启后将自动解析文档并生成向量索引
          </n-text>
        </div>

        <!-- 上传状态 -->
        <div v-if="uploadStatus !== 'idle'" style="margin-top: 16px; text-align: center;">
          <n-spin v-if="uploadStatus === 'uploading'" size="large" />
          <n-icon v-else-if="uploadStatus === 'completed'" :component="DocumentTextOutline" size="48" color="#18a058" />
          <n-icon v-else-if="uploadStatus === 'failed'" :component="DocumentTextOutline" size="48" color="#d03050" />
          <div style="margin-top: 12px; font-size: 14px;">
            {{ uploadStatus === 'uploading' ? '上传中...' : uploadStatus === 'completed' ? '上传完成' : '上传失败' }}
          </div>
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
            @click="handleCancelUpload"
          >
            取消
          </n-button>
          <n-button
            v-if="selectedFile && uploadStatus === 'idle'"
            type="primary"
            @click="handleUpload"
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
  </div>
</template>

<script setup lang="tsx">
import { ref, computed, h, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  NButton,
  NIcon,
  NSpin,
  NEmpty,
  NTag,
  NTabs,
  NTabPane,
  NModal,
  NForm,
  NFormItem,
  NUpload,
  NUploadDragger,
  NSwitch,
  NProgress,
  NText,
  NP,
  NInput,
  NSelect,
  NInputNumber,
  NDivider,
  NDescriptions,
  NDescriptionsItem,
  NAlert,
  NDataTable,
  useMessage,
  useDialog,
  type DataTableColumns
} from 'naive-ui'
import {
  ArrowBackOutline,
  AddOutline,
  DocumentTextOutline,
  EyeOutline,
  PlayOutline,
  TrashOutline,
  ListOutline
} from '@vicons/ionicons5'
import { useThemeStore } from '@/stores/theme'
import { knowledgeBaseApi } from '@/api/knowledgeBase'
import { modelApi } from '@/api/model'
import { documentApi } from '@/api/document'
import {
  uploadFile,
  getFileDetail,
  type FileDetail
} from '@/api/file'
import type { KnowledgeBase, Document, DocumentChunk } from '@/types'

const router = useRouter()
const route = useRoute()
const message = useMessage()
const dialog = useDialog()
const themeStore = useThemeStore()

const loading = ref(false)
const documentsLoading = ref(false)
const showUploadModal = ref(false)
const savingConfig = ref(false)
const loadingModels = ref(false)

// 上传相关
const selectedFile = ref<File | null>(null)
const uploadStatus = ref<'idle' | 'uploading' | 'completed' | 'failed'>('idle')
const uploadResult = ref<{ success: boolean; message: string } | null>(null)

// 是否立即解析
const isParse = ref(true)

// 文件详情相关
const showFileDetailModal = ref(false)
const fileDetailLoading = ref(false)
const currentFileDetail = ref<FileDetail | null>(null)

// 分块查看相关
const showChunksModal = ref(false)
const chunksLoading = ref(false)
const currentChunks = ref<DocumentChunk[]>([])
const currentDocName = ref('')

const knowledgeBase = ref<KnowledgeBase | null>(null)
const documents = ref<Document[]>([])
const embeddingModels = ref<Array<{ label: string; value: number }>>([])

// 配置表单
const configFormRef = ref()
const configForm = ref({
  name: '',
  description: '',
  modelId: undefined as string | undefined,
  chunkStrategy: 'SMART' as string,
  chunkSize: undefined as number | undefined,
  chunkOverlap: undefined as number | undefined,
  chunkMinSize: undefined as number | undefined
})

const strategyOptions = [
  { label: '结构感知', value: 'SMART' },
  { label: '固定长度', value: 'FIXED' }
]

const configRules = {
  name: { required: true, message: '请输入知识库名称', trigger: 'blur' },
  modelId: {
    validator: (rule: any, value: any) => {
      if (!value) {
        return new Error('请选择向量模型')
      }
      return true
    },
    trigger: 'change'
  }
}

// 表格列定义
const docColumns: DataTableColumns<Document> = [
  {
    title: '文档名称',
    key: 'name',
    ellipsis: { tooltip: true }
  },
  {
    title: '类型',
    key: 'docType',
    width: 100,
    render: (row) => h(NTag, { bordered: false, size: 'small' }, { default: () => row.docType?.toUpperCase() || '-' })
  },
  {
    title: '状态',
    key: 'parseStatus',
    width: 100,
    render: (row) => h(NTag, {
      type: getStatusType(row.parseStatus),
      bordered: false,
      size: 'small'
    }, { default: () => getStatusText(row.parseStatus) })
  },
  {
    title: '分块数',
    key: 'chunkCount',
    width: 80,
    render: (row) => row.chunkCount || 0
  },
  {
    title: '创建时间',
    key: 'createdAt',
    width: 170,
    render: (row) => formatTime(row.createdAt)
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
            size: 'small',
            onClick: () => handleViewFileDetail(row)
          },
          { icon: () => h(NIcon, { component: EyeOutline }), default: () => '详情' }
        ),
        h(
          NButton,
          {
            text: true,
            type: 'warning',
            size: 'small',
            disabled: row.parseStatus === 'PARSED' || row.parseStatus === 'PARSING',
            onClick: () => handleParseDoc(row)
          },
          { icon: () => h(NIcon, { component: PlayOutline }), default: () => '解析' }
        ),
        h(
          NButton,
          {
            text: true,
            type: 'default',
            size: 'small',
            onClick: () => handleViewChunks(row)
          },
          { icon: () => h(NIcon, { component: ListOutline }), default: () => '分块' }
        ),
        h(
          NButton,
          {
            text: true,
            type: 'error',
            size: 'small',
            onClick: () => handleDeleteDoc(row)
          },
          { icon: () => h(NIcon, { component: TrashOutline }), default: () => '删除' }
        )
      ])
    }
  }
]

const getStatusType = (status: string) => {
  switch (status) {
    case 'PARSED':
      return 'success'
    case 'PARSING':
      return 'warning'
    case 'FAILED':
      return 'error'
    default:
      return 'info'
  }
}

const getStatusText = (status: string) => {
  switch (status) {
    case 'UPLOADED':
      return '已上传'
    case 'PARSING':
      return '解析中'
    case 'PARSED':
      return '已解析'
    case 'FAILED':
      return '解析失败'
    default:
      return status
  }
}

const formatTime = (time: string) => {
  return new Date(time).toLocaleString('zh-CN')
}

const loadKnowledgeBase = async () => {
  try {
    loading.value = true
    const id = route.params.id as string
    const res = await knowledgeBaseApi.getById(id)
    knowledgeBase.value = res.data
    // 初始化配置表单
    configForm.value = {
      name: res.data.name,
      description: res.data.description || '',
      modelId: res.data.modelId,
      chunkStrategy: res.data.chunkStrategy || 'SMART',
      chunkSize: res.data.chunkSize ?? undefined,
      chunkOverlap: res.data.chunkOverlap ?? undefined,
      chunkMinSize: res.data.chunkMinSize ?? undefined
    }
  } catch (error) {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 查看文件详情
const handleViewFileDetail = async (doc: Document) => {
  showFileDetailModal.value = true
  fileDetailLoading.value = true
  currentFileDetail.value = null

  try {
    const res = await getFileDetail(doc.fileId)
    if (res.code === 200 && res.data) {
      currentFileDetail.value = res.data
    } else {
      message.error('获取文件详情失败')
    }
  } catch (error) {
    message.error('获取文件详情失败')
  } finally {
    fileDetailLoading.value = false
  }
}

const loadDocuments = async () => {
  try {
    documentsLoading.value = true
    const id = route.params.id as string
    const res = await documentApi.getByKnowledgeBaseId(id)
    documents.value = res.data
  } catch (error) {
    message.error('加载文档失败')
  } finally {
    documentsLoading.value = false
  }
}

// 解析文档
const handleParseDoc = (doc: Document) => {
  if (doc.parseStatus === 'PARSED') {
    message.info('该文档已解析')
    return
  }
  if (doc.parseStatus === 'PARSING') {
    message.info('文档正在解析中，请耐心等待')
    return
  }
  dialog.info({
    title: '解析文档',
    content: `确定要解析文档"${doc.name}"吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      const loadingMessage = message.loading('正在提交解析任务...', { duration: 0 })
      try {
        await documentApi.parseDocument(doc.fileId)
        loadingMessage.destroy()
        message.success('解析任务已提交')
        loadDocuments()
      } catch (error: any) {
        loadingMessage.destroy()
        message.error(error?.message || '解析失败')
      }
    }
  })
}

// 查看分块
const handleViewChunks = async (doc: Document) => {
  if (doc.parseStatus === 'UPLOADED') {
    message.warning('文档尚未解析，无法查看分块')
    return
  }
  if (doc.parseStatus === 'PARSING') {
    message.info('文档正在解析中，请稍后再查看分块')
    return
  }

  showChunksModal.value = true
  chunksLoading.value = true
  currentDocName.value = doc.name
  currentChunks.value = []

  try {
    const res = await documentApi.getChunks(doc.id)
    if (res.code === 200 && res.data) {
      currentChunks.value = res.data
    } else {
      message.error('获取分块失败')
    }
  } catch (error) {
    message.error('获取分块失败')
  } finally {
    chunksLoading.value = false
  }
}

// 删除文档
const handleDeleteDoc = (doc: Document) => {
  dialog.warning({
    title: '确认删除',
    content: `确定要删除文档"${doc.name}"吗？`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await documentApi.delete(doc.id)
        message.success('删除成功')
        loadDocuments()
      } catch (error) {
        message.error('删除失败')
      }
    }
  })
}

// 文件选择变化
const handleFileSelect = ({ file }: any) => {
  const allowedExtensions = ['.pdf', '.doc', '.docx', '.xls', '.xlsx', '.md', '.markdown', '.txt', '.html', '.htm']
  const fileName = file.file.name
  const fileExt = fileName.substring(fileName.lastIndexOf('.')).toLowerCase()

  if (!allowedExtensions.includes(fileExt)) {
    message.error(`不支持的文件格式，仅支持：${allowedExtensions.join(', ')}`)
    return
  }

  selectedFile.value = file.file
}

// 格式化文件大小
const formatFileSize = (bytes: number) => {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

// 重置上传状态
const resetUpload = () => {
  selectedFile.value = null
  uploadStatus.value = 'idle'
  uploadResult.value = null
  isParse.value = true
}

// 取消上传
const handleCancelUpload = () => {
  if (uploadStatus.value === 'idle') {
    resetUpload()
    showUploadModal.value = false
  }
}

// 关闭上传弹窗
const closeUploadModal = () => {
  resetUpload()
  showUploadModal.value = false
  loadDocuments()
}

// 上传文档（简单上传）
const handleUpload = async () => {
  if (!selectedFile.value) {
    message.warning('请选择文件')
    return
  }

  try {
    uploadStatus.value = 'uploading'
    uploadResult.value = null

    // 1. 上传文件
    const uploadRes = await uploadFile(selectedFile.value)

    if (uploadRes.code !== 200 || !uploadRes.data) {
      throw new Error('上传文件失败')
    }

    // 2. 获取文件ID
    const fileId = uploadRes.data.fileId

    // 3. 关联知识库
    const knowledgeBaseId = route.params.id as string
    const connectRes = await documentApi.connectKnowledgeBase(fileId, knowledgeBaseId)

    if (connectRes.code !== 200 || !connectRes.data) {
      throw new Error('关联知识库失败')
    }

    // 4. 如果开启解析，调用解析接口
    if (isParse.value) {
      try {
        await documentApi.parseDocument(fileId)
        message.success('上传成功，文档正在解析中')
      } catch (parseError) {
        message.warning('上传成功，但解析任务提交失败')
      }
    } else {
      message.success('上传成功')
    }

    uploadStatus.value = 'completed'
    uploadResult.value = {
      success: true,
      message: `文件 ${selectedFile.value.name} 上传成功`
    }
  } catch (error: any) {
    uploadStatus.value = 'failed'
    uploadResult.value = {
      success: false,
      message: error.message || '上传失败'
    }
    message.error('上传失败: ' + error.message)
  }
}

// 取消上传
const cancelUpload = () => {
  dialog.warning({
    title: '确认取消',
    content: '确定要取消上传吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: () => {
      resetUpload()
      message.info('已取消上传')
    }
  })
}

const loadEmbeddingModels = async () => {
  try {
    loadingModels.value = true
    const res = await modelApi.getAvailableEmbeddingModelConfigs()
    embeddingModels.value = res.data.map(model => ({
      label: model.modelName,
      value: model.id
    }))
  } catch (error) {
    message.error('加载向量模型失败')
    console.error(error)
  } finally {
    loadingModels.value = false
  }
}

const handleSaveConfig = async () => {
  try {
    await configFormRef.value?.validate()
    savingConfig.value = true
    const id = route.params.id as string
    await knowledgeBaseApi.update(id, {
      name: configForm.value.name,
      description: configForm.value.description,
      modelId: configForm.value.modelId,
      chunkStrategy: configForm.value.chunkStrategy,
      chunkSize: configForm.value.chunkSize,
      chunkOverlap: configForm.value.chunkOverlap,
      chunkMinSize: configForm.value.chunkMinSize
    })
    message.success('配置保存成功')
    await loadKnowledgeBase()
  } catch (error: any) {
    message.error(error?.message || '保存失败')
  } finally {
    savingConfig.value = false
  }
}

onMounted(() => {
  loadKnowledgeBase()
  loadDocuments()
  loadEmbeddingModels()
})

// 切分策略切换时自动调整默认值
watch(() => configForm.value.chunkStrategy, (newVal) => {
  if (newVal === 'SMART') {
    if (!configForm.value.chunkSize) configForm.value.chunkSize = 2000
    configForm.value.chunkOverlap = undefined
  } else if (newVal === 'FIXED') {
    if (!configForm.value.chunkSize) configForm.value.chunkSize = 1000
    if (configForm.value.chunkOverlap == null) configForm.value.chunkOverlap = 200
  }
})
</script>

<style scoped>
.knowledge-detail-page {
  padding: 24px 48px;
  max-width: 1400px;
  margin: 0 auto;
}

.loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
}

.back-button {
  margin-bottom: 16px;
}

.page-header {
  margin-bottom: 32px;
}

.header-content h1 {
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 8px;
}

.header-content p {
  font-size: 16px;
  color: v-bind('themeStore.theme.colors.textSecondary');
}

.documents-section,
.config-section {
  padding: 24px 0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.section-header h2 {
  font-size: 20px;
  font-weight: 600;
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px;
  gap: 16px;
}

.documents-table {
  background-color: v-bind('themeStore.theme.colors.surface');
  border-radius: 12px;
  padding: 16px;
}

.config-section {
  max-width: 600px;
}

.upload-area {
  margin-bottom: 16px;
}

.file-info {
  padding: 16px 0;
}

.parse-option {
  display: flex;
  align-items: center;
  margin-top: 16px;
  padding: 12px;
  background-color: v-bind('themeStore.theme.colors.surface');
  border-radius: 8px;
}

.upload-progress {
  margin-top: 20px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 14px;
  color: v-bind('themeStore.theme.colors.textSecondary');
}

.file-detail-content {
  min-height: 200px;
}

.chunks-list {
  max-height: 60vh;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chunk-item {
  padding: 16px;
  border-radius: 8px;
  border: 1px solid;
}

.chunk-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.chunk-tokens {
  font-size: 12px;
  color: v-bind('themeStore.theme.colors.textSecondary');
}

.chunk-content {
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.form-hint {
  font-size: 12px;
  color: v-bind('themeStore.theme.colors.textSecondary');
  margin-left: 8px;
}
</style>
