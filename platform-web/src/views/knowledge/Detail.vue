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
              <n-form-item label="向量模型" path="embeddingModelId">
                <n-select
                  v-model:value="configForm.embeddingModelId"
                  :options="embeddingModels"
                  :loading="loadingModels"
                  placeholder="请选择向量模型"
                  disabled
                  clearable
                />
              </n-form-item>
              <n-form-item label="视觉模型" path="visionModelId">
                <n-select
                  v-model:value="configForm.visionModelId"
                  :options="visionModels"
                  :loading="loadingVisionModels"
                  placeholder="请选择视觉模型（可选）"
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
      style="max-width: 1200px; width: 95vw; max-height: 85vh"
    >
      <!-- 视图切换 + 统计 -->
      <div class="chunks-toolbar">
        <n-radio-group v-model:value="chunkViewMode" size="small">
          <n-radio-button value="preview" label="Markdown 预览" />
          <n-radio-button value="raw" label="原文" />
          <n-radio-button value="split" label="对照视图" />
        </n-radio-group>
        <span class="chunks-count">共 {{ currentChunks.length }} 个分块，{{ totalTokens }} tokens</span>
      </div>

      <n-spin :show="chunksLoading">
        <div v-if="currentChunks.length > 0" class="chunks-list">
          <div
            v-for="chunk in currentChunks"
            :key="chunk.id"
            class="chunk-item"
            :style="{ backgroundColor: themeStore.theme.colors.surface, borderColor: themeStore.theme.colors.border }"
          >
            <div class="chunk-header">
              <div class="chunk-header-left">
                <n-tag size="small" :bordered="false" type="info">分块 #{{ chunk.chunkIndex + 1 }}</n-tag>
                <span v-if="chunk.sectionPath" class="chunk-section-path">{{ chunk.sectionPath }}</span>
              </div>
              <div class="chunk-header-right">
                <n-tag v-if="chunk.charStartIndex != null" size="tiny" :bordered="false" type="default">
                  {{ chunk.charStartIndex }}-{{ chunk.charEndIndex }}
                </n-tag>
                <n-tag size="tiny" :bordered="false" type="default">{{ chunk.tokenSize }} tokens</n-tag>
              </div>
            </div>

            <!-- Markdown 预览模式 -->
            <div v-if="chunkViewMode === 'preview'" class="chunk-preview">
              <MarkdownRenderer :content="chunk.content" />
            </div>

            <!-- 原文模式 -->
            <div v-else-if="chunkViewMode === 'raw'" class="chunk-raw">{{ chunk.content }}</div>

            <!-- 对照视图模式 -->
            <div v-else class="chunk-split">
              <div class="chunk-split-left">
                <div class="chunk-split-label">原文</div>
                <div class="chunk-raw">{{ chunk.content }}</div>
              </div>
              <div class="chunk-split-divider" />
              <div class="chunk-split-right">
                <div class="chunk-split-label">Markdown 预览</div>
                <MarkdownRenderer :content="chunk.content" />
              </div>
            </div>
          </div>
        </div>
        <n-empty v-else-if="!chunksLoading" description="暂无分块数据" />
      </n-spin>
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
      <!-- 文件选择区域 - 始终可见，支持多选 -->
      <div class="upload-area">
        <n-upload
          multiple
          :show-file-list="false"
          :custom-request="handleFileSelect"
          accept=".pdf,.doc,.docx,.xls,.xlsx,.md,.markdown,.txt,.html,.htm"
        >
          <n-upload-dragger>
            <div style="margin-bottom: 12px">
              <n-icon :component="DocumentTextOutline" :size="48" :depth="3" />
            </div>
            <n-text style="font-size: 16px">点击或拖拽文件到此区域</n-text>
            <n-p depth="3" style="margin: 8px 0 0 0">支持多文件批量上传，可多次选择</n-p>
            <n-p depth="3" style="margin: 4px 0 0 0; font-size: 12px">支持格式：PDF、DOC/DOCX、XLS/XLSX、Markdown、TXT、HTML</n-p>
          </n-upload-dragger>
        </n-upload>
      </div>

      <!-- 已选文件列表 -->
      <div v-if="selectedFiles.length > 0" style="margin-top: 16px">
        <div style="margin-bottom: 8px; font-weight: 500; display: flex; justify-content: space-between; align-items: center">
          <span>已选择 {{ selectedFiles.length }} 个文件</span>
          <n-button text type="warning" size="small" @click="resetUpload" v-if="uploadStatus === 'idle'">清空列表</n-button>
        </div>
        <n-data-table
          :columns="selectedFileColumns"
          :data="selectedFiles"
          :bordered="false"
          size="small"
          max-height="240"
          :row-key="(row: SelectedFileItem) => row.uid"
        />
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

      <template #footer>
        <div style="display: flex; justify-content: flex-end; gap: 12px">
          <n-button
            v-if="uploadStatus === 'idle'"
            @click="closeUploadModal"
          >
            取消
          </n-button>
          <n-button
            v-if="selectedFiles.length > 0 && uploadStatus === 'idle'"
            type="primary"
            @click="handleUpload"
          >
            开始上传（{{ selectedFiles.length }}个文件）
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
  NRadioGroup,
  NRadioButton,
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
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import {
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
const loadingVisionModels = ref(false)

// 上传相关
interface SelectedFileItem {
  uid: number
  file: File
}
let fileUidCounter = 0
const selectedFiles = ref<SelectedFileItem[]>([])
const uploadStatus = ref<'idle' | 'uploading' | 'completed' | 'failed'>('idle')
const uploadResult = ref<{ success: boolean; message: string } | null>(null)

// 文件详情相关
const showFileDetailModal = ref(false)
const fileDetailLoading = ref(false)
const currentFileDetail = ref<FileDetail | null>(null)

// 分块查看相关
const showChunksModal = ref(false)
const chunksLoading = ref(false)
const currentChunks = ref<DocumentChunk[]>([])
const currentDocName = ref('')
const chunkViewMode = ref<'preview' | 'raw' | 'split'>('preview')
const totalTokens = computed(() => currentChunks.value.reduce((sum, c) => sum + (c.tokenSize || 0), 0))

const knowledgeBase = ref<KnowledgeBase | null>(null)
const documents = ref<Document[]>([])
const embeddingModels = ref<Array<{ label: string; value: number }>>([])
const visionModels = ref<Array<{ label: string; value: number }>>([])

// 配置表单
const configFormRef = ref()
const configForm = ref({
  name: '',
  description: '',
  embeddingModelId: undefined as string | undefined,
  visionModelId: undefined as string | undefined,
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
  embeddingModelId: {
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
      embeddingModelId: res.data.embeddingModelId,
      visionModelId: res.data.visionModelId,
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
  chunkViewMode.value = 'preview' // 每次打开默认回到预览模式
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

  // 检查是否已存在同名文件
  const exists = selectedFiles.value.some(f => f.file.name === file.file.name)
  if (exists) {
    message.warning(`文件 ${file.file.name} 已存在`)
    return
  }

  selectedFiles.value.push({ uid: ++fileUidCounter, file: file.file })
}

// 移除单个已选文件
const removeSelectedFile = (item: SelectedFileItem) => {
  selectedFiles.value = selectedFiles.value.filter(f => f.uid !== item.uid)
}

// 已选文件表格列定义
const selectedFileColumns: DataTableColumns<SelectedFileItem> = [
  {
    title: '文件名',
    key: 'file.name',
    ellipsis: { tooltip: true }
  },
  {
    title: '大小',
    key: 'file.size',
    width: 100,
    render: (row) => formatFileSize(row.file.size)
  },
  {
    title: '类型',
    key: 'file.type',
    width: 80,
    render: (row) => getFileTypeDisplay(row.file)
  },
  {
    title: '操作',
    key: 'actions',
    width: 70,
    render: (row) => {
      return h(
        NButton,
        {
          text: true,
          type: 'error',
          size: 'tiny',
          onClick: () => removeSelectedFile(row)
        },
        { icon: () => h(NIcon, { component: TrashOutline }) }
      )
    }
  }
]

// 从文件名获取文件类型展示文本
const getFileTypeDisplay = (file: File) => {
  const dotIndex = file.name.lastIndexOf('.')
  if (dotIndex > 0) {
    return file.name.substring(dotIndex + 1).toUpperCase()
  }
  return file.type || '未知'
}
const formatFileSize = (bytes: number) => {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

// 重置上传状态
const resetUpload = () => {
  selectedFiles.value = []
  uploadStatus.value = 'idle'
  uploadResult.value = null
}

// 关闭上传弹窗
const closeUploadModal = () => {
  resetUpload()
  showUploadModal.value = false
  loadDocuments()
}

// 上传文档（批量上传）
const handleUpload = async () => {
  if (selectedFiles.value.length === 0) {
    message.warning('请选择文件')
    return
  }

  try {
    uploadStatus.value = 'uploading'
    uploadResult.value = null

    const knowledgeBaseId = route.params.id as string

    // 使用统一上传接口
    const fileObjects = selectedFiles.value.map(f => f.file)
    const uploadRes = await documentApi.upload(fileObjects, knowledgeBaseId)

    if (uploadRes.code !== 200 || !uploadRes.data) {
      throw new Error('批量上传失败')
    }

    // 统计成功和失败的数量
    const results = uploadRes.data
    const successCount = results.filter(r => r.success).length
    const failedFiles = results.filter(r => !r.success).map(r => r.fileName)

    if (successCount === selectedFiles.value.length) {
      message.success(`成功上传 ${successCount} 个文件`)
      uploadStatus.value = 'completed'
      uploadResult.value = {
        success: true,
        message: `成功上传 ${successCount} 个文件`
      }
    } else if (successCount > 0) {
      message.warning(`成功上传 ${successCount} 个文件，失败 ${failedFiles.length} 个文件`)
      uploadStatus.value = 'completed'
      uploadResult.value = {
        success: true,
        message: `成功 ${successCount} 个，失败 ${failedFiles.length} 个：${failedFiles.join(', ')}`
      }
    } else {
      message.error('所有文件上传失败')
      uploadStatus.value = 'failed'
      uploadResult.value = {
        success: false,
        message: `全部 ${selectedFiles.value.length} 个文件上传失败`
      }
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

const loadVisionModels = async () => {
  try {
    loadingVisionModels.value = true
    const res = await modelApi.getAvailableVisionModelConfigs()
    visionModels.value = res.data.map(model => ({
      label: model.modelName,
      value: model.id
    }))
  } catch (error) {
    message.error('加载视觉模型失败')
    console.error(error)
  } finally {
    loadingVisionModels.value = false
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
      embeddingModelId: configForm.value.embeddingModelId,
      visionModelId: configForm.value.visionModelId,
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
  loadVisionModels()
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

.chunks-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid v-bind('themeStore.theme.colors.border');
}

.chunks-count {
  font-size: 13px;
  color: v-bind('themeStore.theme.colors.textSecondary');
}

.chunks-list {
  max-height: calc(85vh - 160px);
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
  flex-wrap: wrap;
  gap: 8px;
}

.chunk-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.chunk-header-right {
  display: flex;
  align-items: center;
  gap: 6px;
}

.chunk-section-path {
  font-size: 12px;
  color: v-bind('themeStore.theme.colors.textSecondary');
  background-color: v-bind('themeStore.theme.colors.background');
  padding: 2px 8px;
  border-radius: 4px;
  max-width: 400px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chunk-preview {
  padding: 8px 0;
}

.chunk-raw {
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
}

/* 对照视图 */
.chunk-split {
  display: flex;
  gap: 0;
  min-height: 120px;
}

.chunk-split-left,
.chunk-split-right {
  flex: 1;
  min-width: 0;
  overflow: auto;
  max-height: 500px;
}

.chunk-split-left {
  padding-right: 8px;
}

.chunk-split-right {
  padding-left: 8px;
}

.chunk-split-divider {
  width: 1px;
  background-color: v-bind('themeStore.theme.colors.border');
  flex-shrink: 0;
}

.chunk-split-label {
  font-size: 12px;
  font-weight: 600;
  color: v-bind('themeStore.theme.colors.textSecondary');
  margin-bottom: 8px;
  padding-bottom: 4px;
  border-bottom: 1px dashed v-bind('themeStore.theme.colors.border');
}

.form-hint {
  font-size: 12px;
  color: v-bind('themeStore.theme.colors.textSecondary');
  margin-left: 8px;
}
</style>
