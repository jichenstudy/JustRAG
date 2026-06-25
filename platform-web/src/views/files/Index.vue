<template>
  <div class="files-page">
    <div class="page-header">
      <h1>文件管理</h1>
      <div class="header-actions">
        <n-button type="primary" @click="handleOssConfigClick" style="margin-right: 12px">
          <template #icon>
            <n-icon :component="SettingsOutline" />
          </template>
          存储配置
        </n-button>
        <n-button type="primary" @click="showUploadModal = true">
          <template #icon>
            <n-icon :component="CloudUploadOutline" />
          </template>
          上传文件
        </n-button>
      </div>
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
      style="max-width: 700px"
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
              <n-icon :component="CloudUploadOutline" :size="48" :depth="3" />
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
        <n-icon v-else-if="uploadStatus === 'completed'" :component="CloudUploadOutline" size="48" color="#18a058" />
        <n-icon v-else-if="uploadStatus === 'failed'" :component="CloudUploadOutline" size="48" color="#d03050" />
        <div style="margin-top: 12px; font-size: 14px;">
          {{ uploadStatus === 'uploading' ? '上传中...' : uploadStatus === 'completed' ? '上传完成' : '上传失败' }}
        </div>
      </div>

      <!-- 上传结果 -->
      <n-alert
        v-if="uploadResult"
        :type="uploadResult.success ? 'success' : 'error'"
        :title="uploadResult.success ? '上传完成' : '上传失败'"
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
            @click="startUpload"
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
            <n-descriptions-item label="关联知识库">
              <n-tag v-if="currentFileDetail.knowledgeBaseName" type="success" :bordered="false" size="small">
                {{ currentFileDetail.knowledgeBaseName }}
              </n-tag>
            </n-descriptions-item>
            <n-descriptions-item label="文件哈希" :span="2">
              <n-text code>{{ currentFileDetail.hashInfo || '-' }}</n-text>
            </n-descriptions-item>
            <n-descriptions-item label="存储路径" :span="2">
              <n-text code>{{ currentFileDetail.path || '-' }}</n-text>
            </n-descriptions-item>
          </n-descriptions>

          <!-- 文件预览
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
          -->
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

    <!-- 存储配置对话框 -->
    <n-modal
      v-model:show="showOssConfigModal"
      preset="card"
      title="存储配置管理"
      style="max-width: 1000px"
    >
      <div class="oss-config-content">
        <div class="config-actions">
          <n-button type="primary" @click="handleAddConfig">
            <template #icon>
              <n-icon :component="AddOutline" />
            </template>
            新增配置
          </n-button>
        </div>

        <n-data-table
          :columns="configColumns"
          :data="ossConfigs"
          :bordered="false"
          :loading="configLoading"
          :row-key="(row: OssConfig) => row.ossConfigId"
        />
      </div>
    </n-modal>

    <!-- 新增/编辑配置对话框 -->
    <n-modal
      v-model:show="showConfigFormModal"
      preset="card"
      :title="configFormData.ossConfigId ? '编辑配置' : '新增配置'"
      style="max-width: 600px"
    >
      <n-form
        ref="configFormRef"
        :model="configFormData"
        :rules="configFormRules"
        label-placement="left"
        label-width="120"
      >
        <n-form-item label="配置Key" path="configKey">
          <n-input v-model:value="configFormData.configKey" placeholder="如：minio, aliyun, qcloud" />
        </n-form-item>
        <n-form-item label="访问站点" path="endpoint">
          <n-input v-model:value="configFormData.endpoint" placeholder="如：localhost:9000" />
        </n-form-item>
        <n-form-item label="自定义域名">
          <n-input v-model:value="configFormData.domain" placeholder="可选，自定义访问域名" />
        </n-form-item>
        <n-form-item label="Access Key" path="accessKey">
          <n-input v-model:value="configFormData.accessKey" placeholder="Access Key" />
        </n-form-item>
        <n-form-item label="Secret Key" path="secretKey">
          <n-input v-model:value="configFormData.secretKey" type="password" show-password-on="click" placeholder="Secret Key" />
        </n-form-item>
        <n-form-item label="存储桶名称" path="bucketName">
          <n-input v-model:value="configFormData.bucketName" placeholder="存储桶名称" />
        </n-form-item>
        <n-form-item label="路径前缀">
          <n-input v-model:value="configFormData.prefix" placeholder="可选，如：rag" />
        </n-form-item>
        <n-form-item label="区域">
          <n-input v-model:value="configFormData.region" placeholder="可选，如：us-east-1" />
        </n-form-item>
        <n-form-item label="桶权限类型">
          <n-select v-model:value="configFormData.accessPolicy" :options="accessPolicyOptions" />
        </n-form-item>
        <n-form-item label="是否HTTPS">
          <n-switch v-model:value="configFormData.isHttps" />
        </n-form-item>
        <n-form-item label="是否默认">
          <n-switch v-model:value="configFormData.isDefault" />
        </n-form-item>
        <n-form-item label="备注">
          <n-input v-model:value="configFormData.remark" type="textarea" placeholder="备注信息" />
        </n-form-item>
      </n-form>
      <template #footer>
        <div style="display: flex; justify-content: flex-end; gap: 12px">
          <n-button @click="showConfigFormModal = false">取消</n-button>
          <n-button type="primary" :loading="configFormLoading" @click="handleSaveConfig">保存</n-button>
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
  NSwitch,
  NForm,
  NFormItem,
  useMessage,
  useDialog,
  type DataTableColumns,
  type FormInst,
  type FormRules
} from 'naive-ui'
import { CloudUploadOutline, SearchOutline, TrashOutline, EyeOutline, DownloadOutline, LinkOutline, DocumentTextOutline, SettingsOutline, AddOutline, CreateOutline } from '@vicons/ionicons5'
import { useThemeStore } from '@/stores/theme'
import { useAuthStore } from '@/stores/auth'
import {
  getFileList,
  deleteFiles,
  getFileDetail,
  previewFile,
  uploadFile,
  type FileDetail
} from '@/api/file'
import { knowledgeBaseApi } from '@/api/knowledgeBase'
import { documentApi } from '@/api/document'
import { ossConfigApi, type OssConfig, type CreateOssConfigDTO } from '@/api/ossConfig'
import type { KnowledgeBase } from '@/types'

const message = useMessage()
const dialog = useDialog()
const themeStore = useThemeStore()
const authStore = useAuthStore()

// 文件列表相关
const loading = ref(false)
const files = ref<FileDetail[]>([])
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 上传相关（多文件批量上传）
interface SelectedFileItem {
  uid: number
  file: File
}
let fileUidCounter = 0
const showUploadModal = ref(false)
const selectedFiles = ref<SelectedFileItem[]>([])
const uploadStatus = ref<'idle' | 'uploading' | 'completed' | 'failed'>('idle')
const uploadResult = ref<{ success: boolean; message: string } | null>(null)

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

// 存储配置相关
const showOssConfigModal = ref(false)
const showConfigFormModal = ref(false)
const configLoading = ref(false)
const configFormLoading = ref(false)
const configFormRef = ref<FormInst | null>(null)
const ossConfigs = ref<OssConfig[]>([])

interface ConfigFormData {
  ossConfigId?: string
  configKey: string
  accessKey: string
  secretKey: string
  bucketName: string
  prefix: string
  endpoint: string
  domain: string
  isHttps: boolean
  region: string
  accessPolicy: string
  status: string
  remark: string
  isDefault: boolean
}

const configFormData = ref<ConfigFormData>({
  configKey: '',
  accessKey: '',
  secretKey: '',
  bucketName: '',
  prefix: '',
  endpoint: '',
  domain: '',
  isHttps: false,
  region: '',
  accessPolicy: '1',
  status: '1',
  remark: '',
  isDefault: false
})

const configFormRules: FormRules = {
  configKey: { required: true, message: '请输入配置Key', trigger: 'blur' },
  endpoint: { required: true, message: '请输入访问站点', trigger: 'blur' },
  accessKey: { required: true, message: '请输入Access Key', trigger: 'blur' },
  secretKey: { required: true, message: '请输入Secret Key', trigger: 'blur' },
  bucketName: { required: true, message: '请输入存储桶名称', trigger: 'blur' }
}

const accessPolicyOptions = [
  { label: '私有', value: '0' },
  { label: '公开', value: '1' },
  { label: '自定义', value: '2' }
]

// 表格列定义
const columns: DataTableColumns<FileDetail> = [
  {
    title: '文件名',
    key: 'originalFilename',
    ellipsis: { tooltip: true }
  },
  {
    title: '关联知识库',
    key: 'knowledgeBaseName',
    render: (row) => row.knowledgeBaseName || h(NTag, { bordered: false, size: 'small', type: 'default' }, { default: () => '' })
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

// 存储配置表格列定义
const configColumns: DataTableColumns<OssConfig> = [
  {
    title: '配置Key',
    key: 'configKey',
    width: 120
  },
  {
    title: '访问站点',
    key: 'endpoint',
    width: 180,
    ellipsis: { tooltip: true }
  },
  {
    title: '存储桶',
    key: 'bucketName',
    width: 120
  },
  {
    title: 'HTTPS',
    key: 'isHttps',
    width: 80,
    render: (row) => h(NTag, { type: row.isHttps === 'Y' ? 'success' : 'default', bordered: false, size: 'small' }, { default: () => row.isHttps === 'Y' ? '是' : '否' })
  },
  {
    title: '默认',
    key: 'status',
    width: 80,
    render: (row) => h(NTag, { type: row.status === '0' ? 'success' : 'default', bordered: false, size: 'small' }, { default: () => row.status === '0' ? '是' : '否' })
  },
  {
    title: '创建时间',
    key: 'createdAt',
    width: 160,
    render: (row) => formatTime(row.createdAt)
  },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    render: (row) => {
      return h('div', { style: { display: 'flex', gap: '8px' } }, [
        h(
          NButton,
          {
            text: true,
            type: 'primary',
            onClick: () => handleEditConfig(row)
          },
          { icon: () => h(NIcon, { component: CreateOutline }), default: () => '编辑' }
        ),
        h(
          NButton,
          {
            text: true,
            type: row.status === '0' ? 'default' : 'success',
            disabled: row.status === '0',
            onClick: () => handleSetDefault(row)
          },
          { default: () => '设为默认' }
        ),
        h(
          NButton,
          {
            text: true,
            type: 'error',
            onClick: () => handleDeleteConfig(row)
          },
          { icon: () => h(NIcon, { component: TrashOutline }), default: () => '删除' }
        )
      ])
    }
  }
]

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
      // const previewableTypes = ['.jpg', '.jpeg', '.png', '.gif', '.pdf', '.txt']
      // if (res.data.ext && previewableTypes.includes(res.data.ext.toLowerCase())) {
      //   try {
      //     const previewRes = await previewFile(res.data.filename)
      //     if (previewRes.code === 200 && previewRes.data) {
      //       previewUrl.value = previewRes.data
      //     }
      //   } catch (error) {
      //     console.error('获取预览URL失败', error)
      //   }
      // }
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

// 文件选择处理（多文件，逐个回调）
const handleFileSelect = ({ file }: any) => {
  const fileName = file.file.name
  const fileExt = fileName.substring(fileName.lastIndexOf('.')).toLowerCase()

  if (!allowedExtensions.includes(fileExt)) {
    message.error(`不支持的文件格式：${fileName}，仅支持：${allowedExtensions.join(', ')}`)
    return
  }

  // 避免重复添加同名+同大小的文件
  const exists = selectedFiles.value.some(
    f => f.file.name === fileName && f.file.size === file.file.size
  )
  if (!exists) {
    selectedFiles.value.push({ uid: ++fileUidCounter, file: file.file })
  }
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

// 重置上传状态
const resetUpload = () => {
  selectedFiles.value = []
  uploadStatus.value = 'idle'
  uploadResult.value = null
}

// 关闭上传对话框
const closeUploadModal = () => {
  resetUpload()
  showUploadModal.value = false
  loadFiles() // 刷新列表
}

// 开始上传（批量上传）
const startUpload = async () => {
  if (selectedFiles.value.length === 0) return

  try {
    uploadStatus.value = 'uploading'
    uploadResult.value = null

    const fileObjects = selectedFiles.value.map(f => f.file)
    const res = await uploadFile(fileObjects)
    if (res.code === 200 && res.data) {
      const successCount = res.data.length
      uploadStatus.value = 'completed'
      uploadResult.value = {
        success: true,
        message: `${successCount} 个文件全部上传成功`
      }
      message.success(`${successCount} 个文件上传成功`)
      loadFiles()
    } else {
      throw new Error(res.message || '上传失败')
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

// 存储配置按钮点击（仅超级管理员可操作）
const handleOssConfigClick = () => {
  if (authStore.user?.id !== '1') {
    message.warning('仅超级管理员可操作存储配置')
    return
  }
  showOssConfigModal.value = true
}

// ========== 存储配置管理 ==========

// 加载配置列表
const loadOssConfigs = async () => {
  try {
    configLoading.value = true
    const res = await ossConfigApi.getConfigList()
    if (res.code === 200 && res.data) {
      ossConfigs.value = res.data
    }
  } catch (error) {
    message.error('加载配置列表失败')
  } finally {
    configLoading.value = false
  }
}

// 新增配置
const handleAddConfig = () => {
  configFormData.value = {
    configKey: '',
    accessKey: '',
    secretKey: '',
    bucketName: '',
    prefix: '',
    endpoint: '',
    domain: '',
    isHttps: false,
    region: '',
    accessPolicy: '1',
    status: '1',
    remark: '',
    isDefault: false
  }
  showConfigFormModal.value = true
}

// 编辑配置
const handleEditConfig = (config: OssConfig) => {
  configFormData.value = {
    ossConfigId: config.ossConfigId,
    configKey: config.configKey,
    accessKey: config.accessKey,
    secretKey: config.secretKey,
    bucketName: config.bucketName,
    prefix: config.prefix || '',
    endpoint: config.endpoint,
    domain: config.domain || '',
    isHttps: config.isHttps === 'Y',
    region: config.region || '',
    accessPolicy: config.accessPolicy || '1',
    status: config.status || '1',
    remark: config.remark || '',
    isDefault: config.status === '0'
  }
  showConfigFormModal.value = true
}

// 保存配置
const handleSaveConfig = async () => {
  try {
    await configFormRef.value?.validate()
  } catch (error) {
    return
  }

  configFormLoading.value = true
  try {
    const data: CreateOssConfigDTO = {
      configKey: configFormData.value.configKey,
      accessKey: configFormData.value.accessKey,
      secretKey: configFormData.value.secretKey,
      bucketName: configFormData.value.bucketName,
      prefix: configFormData.value.prefix,
      endpoint: configFormData.value.endpoint,
      domain: configFormData.value.domain,
      isHttps: configFormData.value.isHttps ? 'Y' : 'N',
      region: configFormData.value.region,
      accessPolicy: configFormData.value.accessPolicy,
      status: configFormData.value.isDefault ? '0' : '1',
      remark: configFormData.value.remark
    }

    let res
    if (configFormData.value.ossConfigId) {
      res = await ossConfigApi.updateConfig({
        ossConfigId: configFormData.value.ossConfigId,
        ...data,
        userId: 0,
        createdAt: '',
        updatedAt: ''
      } as any)
    } else {
      res = await ossConfigApi.createConfig(data)
    }

    if (res.code === 200) {
      message.success(configFormData.value.ossConfigId ? '更新成功' : '新增成功')
      showConfigFormModal.value = false
      loadOssConfigs()
    } else {
      message.error(res.message || '操作失败')
    }
  } catch (error: any) {
    message.error(error.message || '操作失败')
  } finally {
    configFormLoading.value = false
  }
}

// 删除配置
const handleDeleteConfig = (config: OssConfig) => {
  dialog.warning({
    title: '确认删除',
    content: `确定要删除配置 "${config.configKey}" 吗？`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await ossConfigApi.deleteConfig(config.ossConfigId)
        if (res.code === 200) {
          message.success('删除成功')
          loadOssConfigs()
        } else {
          message.error(res.message || '删除失败')
        }
      } catch (error) {
        message.error('删除失败')
      }
    }
  })
}

// 设为默认配置
const handleSetDefault = (config: OssConfig) => {
  dialog.warning({
    title: '确认设置',
    content: `确定要将 "${config.configKey}" 设为默认配置吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await ossConfigApi.setDefaultConfig(config.ossConfigId)
        if (res.code === 200) {
          message.success('设置成功')
          loadOssConfigs()
        } else {
          message.error(res.message || '设置失败')
        }
      } catch (error) {
        message.error('设置失败')
      }
    }
  })
}

onMounted(() => {
  loadFiles()
})

// 监听配置弹窗打开时加载数据
import { watch } from 'vue'
watch(showOssConfigModal, (val) => {
  if (val) {
    loadOssConfigs()
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
