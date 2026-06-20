<template>
  <div class="knowledge-page">
    <div class="page-header">
      <h1>知识库管理</h1>
      <n-button type="primary" @click="showCreateModal = true">
        <template #icon>
          <n-icon :component="AddOutline" />
        </template>
        创建知识库
      </n-button>
    </div>

    <div v-if="loading" class="loading">
      <n-spin size="large" />
    </div>

    <div v-else-if="knowledgeBases.length === 0" class="empty">
      <n-empty description="暂无知识库" />
      <n-button @click="showCreateModal = true">创建第一个知识库</n-button>
    </div>

    <div v-else class="knowledge-grid">
      <div
        v-for="kb in knowledgeBases"
        :key="kb.id"
        class="knowledge-card"
        :style="{
          backgroundColor: themeStore.theme.colors.surface,
          borderColor: themeStore.theme.colors.border
        }"
      >
        <div class="card-header">
          <h3 @click="router.push(`/knowledge/${kb.id}`)">{{ kb.name }}</h3>
          <n-dropdown :options="getMenuOptions()" @select="handleMenuSelect($event, kb)">
            <n-button text>
              <n-icon :component="EllipsisVerticalOutline" :size="20" />
            </n-button>
          </n-dropdown>
        </div>

        <p class="description">{{ kb.description || '暂无描述' }}</p>

        <div class="card-footer">
          <span class="time">更新于 {{ formatTime(kb.updatedAt) }}</span>
        </div>
      </div>
    </div>

    <!-- 创建/编辑知识库对话框 -->
    <n-modal v-model:show="showCreateModal" preset="card" title="创建知识库" style="max-width: 560px">
      <n-form ref="formRef" :model="formData" :rules="rules">
        <n-form-item label="名称" path="name">
          <n-input v-model:value="formData.name" placeholder="请输入知识库名称" />
        </n-form-item>
        <n-form-item label="描述" path="description">
          <n-input
            v-model:value="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入知识库描述"
          />
        </n-form-item>
        <n-form-item label="向量模型" path="modelId">
          <n-select
            v-model:value="formData.modelId"
            :options="embeddingModels"
            :loading="loadingModels"
            placeholder="请选择向量模型"
            clearable
          />
        </n-form-item>
        <n-form-item label="切分策略" path="chunkStrategy">
          <n-select
            v-model:value="formData.chunkStrategy"
            :options="strategyOptions"
            placeholder="结构感知"
          />
        </n-form-item>
        <n-form-item label="分片大小" path="chunkSize">
          <n-input-number v-model:value="formData.chunkSize" :min="100" :max="5000" />
          <span class="form-hint">{{ formData.chunkStrategy === 'SMART' ? '超过此大小的章节按段落递归细分' : '固定窗口字符数' }}</span>
        </n-form-item>
        <n-form-item label="最小分片" path="chunkMinSize">
          <n-input-number v-model:value="formData.chunkMinSize" :min="50" :max="500" placeholder="100" />
          <span class="form-hint">小于此大小的分片将被过滤</span>
        </n-form-item>
        <template v-if="formData.chunkStrategy === 'FIXED'">
          <n-form-item label="重叠大小" path="chunkOverlap">
            <n-input-number v-model:value="formData.chunkOverlap" :min="0" :max="1000" placeholder="200" />
            <span class="form-hint">相邻分片重叠字符数，避免边界截断</span>
          </n-form-item>
        </template>
      </n-form>
      <template #footer>
        <div style="display: flex; justify-content: flex-end; gap: 12px">
          <n-button @click="showCreateModal = false">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="handleSubmit">创建</n-button>
        </div>
      </template>
    </n-modal>

    <!-- 编辑对话框 -->
    <n-modal v-model:show="showEditModal" preset="card" title="编辑知识库" style="max-width: 560px">
      <n-form ref="editFormRef" :model="editFormData">
        <n-form-item label="名称">
          <n-input v-model:value="editFormData.name" placeholder="请输入知识库名称" />
        </n-form-item>
        <n-form-item label="描述">
          <n-input
            v-model:value="editFormData.description"
            type="textarea"
            :rows="2"
            placeholder="请输入知识库描述"
          />
        </n-form-item>
        <n-form-item label="切分策略">
          <n-select
            v-model:value="editFormData.chunkStrategy"
            :options="strategyOptions"
            placeholder="不修改"
            clearable
          />
        </n-form-item>
        <n-form-item label="分片大小">
          <n-input-number v-model:value="editFormData.chunkSize" :min="100" :max="5000" placeholder="不修改" />
        </n-form-item>
        <n-form-item label="最小分片">
          <n-input-number v-model:value="editFormData.chunkMinSize" :min="50" :max="500" placeholder="不修改" />
        </n-form-item>
        <template v-if="editFormData.chunkStrategy === 'FIXED'">
          <n-form-item label="重叠大小">
            <n-input-number v-model:value="editFormData.chunkOverlap" :min="0" :max="1000" placeholder="不修改" />
          </n-form-item>
        </template>
      </n-form>
      <template #footer>
        <div style="display: flex; justify-content: flex-end; gap: 12px">
          <n-button @click="showEditModal = false">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="handleUpdate">保存</n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  NButton,
  NIcon,
  NSpin,
  NEmpty,
  NDropdown,
  NModal,
  NForm,
  NFormItem,
  NInput,
  NSelect,
  NInputNumber,
  NDivider,
  useMessage,
  useDialog
} from 'naive-ui'
import { AddOutline, EllipsisVerticalOutline } from '@vicons/ionicons5'
import { useThemeStore } from '@/stores/theme'
import { knowledgeBaseApi } from '@/api/knowledgeBase'
import { modelApi } from '@/api/model'
import type { KnowledgeBase, CreateKnowledgeBaseDTO } from '@/types'

const router = useRouter()
const message = useMessage()
const dialog = useDialog()
const themeStore = useThemeStore()

const loading = ref(false)
const submitting = ref(false)
const showCreateModal = ref(false)
const showEditModal = ref(false)

const knowledgeBases = ref<KnowledgeBase[]>([])
const embeddingModels = ref<Array<{ label: string; value: number }>>([])
const loadingModels = ref(false)

const formRef = ref()
const formData = ref<CreateKnowledgeBaseDTO>({
  name: '',
  description: '',
  modelId: undefined,
  chunkStrategy: 'SMART',
  chunkSize: 2000,
  chunkOverlap: 0,
  chunkMinSize: 100
})

const editFormRef = ref()
const editFormData = ref({
  id: 0,
  name: '',
  description: '',
  chunkStrategy: undefined as string | undefined,
  chunkSize: undefined as number | undefined,
  chunkOverlap: undefined as number | undefined,
  chunkMinSize: undefined as number | undefined
})

const rules = {
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

const strategyOptions = [
  { label: '结构感知', value: 'SMART' },
  { label: '固定长度', value: 'FIXED' }
]

const formatTime = (time: string) => {
  return new Date(time).toLocaleDateString('zh-CN')
}

const getMenuOptions = () => [
  { label: '查看详情', key: 'view' },
  { label: '重命名', key: 'edit' },
  { label: '删除', key: 'delete' }
]

const handleMenuSelect = async (key: string, kb: KnowledgeBase) => {
  switch (key) {
    case 'view':
      router.push(`/knowledge/${kb.id}`)
      break
    case 'edit':
      editFormData.value = {
        id: kb.id,
        name: kb.name,
        description: kb.description,
        chunkStrategy: kb.chunkStrategy || undefined,
        chunkSize: kb.chunkSize || undefined,
        chunkOverlap: kb.chunkOverlap || undefined,
        chunkMinSize: kb.chunkMinSize || undefined
      }
      showEditModal.value = true
      break
    case 'delete':
      handleDelete(kb)
      break
  }
}

const loadData = async () => {
  try {
    loading.value = true
    const res = await knowledgeBaseApi.getAll()
    knowledgeBases.value = res.data
  } catch (error) {
    message.error('加载失败')
  } finally {
    loading.value = false
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

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
    submitting.value = true
    await knowledgeBaseApi.create(formData.value)
    message.success('创建成功')
    showCreateModal.value = false
    formData.value = { name: '', description: '', modelId: undefined, chunkStrategy: 'SMART', chunkSize: 2000, chunkOverlap: 0, chunkMinSize: 100 }
    loadData()
  } catch (error) {
    message.error('创建失败')
  } finally {
    submitting.value = false
  }
}

const handleUpdate = async () => {
  try {
    submitting.value = true
    await knowledgeBaseApi.update(editFormData.value.id, {
      name: editFormData.value.name,
      description: editFormData.value.description,
      chunkStrategy: editFormData.value.chunkStrategy,
      chunkSize: editFormData.value.chunkSize,
      chunkOverlap: editFormData.value.chunkOverlap,
      chunkMinSize: editFormData.value.chunkMinSize
    })
    message.success('更新成功')
    showEditModal.value = false
    loadData()
  } catch (error) {
    message.error('更新失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = (kb: KnowledgeBase) => {
  dialog.warning({
    title: '确认删除',
    content: `确定要删除知识库"${kb.name}"吗？此操作不可恢复。`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await knowledgeBaseApi.delete(kb.id)
        message.success('删除成功')
        loadData()
      } catch (error) {
        message.error('删除失败')
      }
    }
  })
}

onMounted(() => {
  loadData()
  loadEmbeddingModels()
})

// 当打开创建对话框时，重新加载模型列表
watch(showCreateModal, (newVal) => {
  if (newVal) {
    loadEmbeddingModels()
  }
})

// 切分策略切换时自动调整默认值
watch(() => formData.value.chunkStrategy, (newVal) => {
  if (newVal === 'SMART') {
    formData.value.chunkSize = 2000
    formData.value.chunkOverlap = 0
  } else if (newVal === 'FIXED') {
    formData.value.chunkSize = 1000
    formData.value.chunkOverlap = 200
  }
})
</script>

<style scoped>
.knowledge-page {
  padding: 48px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}

.page-header h1 {
  font-size: 28px;
  font-weight: 600;
}

.loading,
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px;
  gap: 16px;
}

.knowledge-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.knowledge-card {
  padding: 24px;
  border-radius: 12px;
  border: 1px solid;
  transition: all 0.2s ease;
}

.knowledge-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.card-header h3 {
  font-size: 18px;
  font-weight: 600;
  cursor: pointer;
  flex: 1;
}

.card-header h3:hover {
  color: v-bind('themeStore.theme.colors.textSecondary');
}

.description {
  font-size: 14px;
  color: v-bind('themeStore.theme.colors.textSecondary');
  margin-bottom: 16px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 40px;
}

.card-footer {
  padding-top: 12px;
  border-top: 1px solid v-bind('themeStore.theme.colors.border');
}

.time {
  font-size: 12px;
  color: v-bind('themeStore.theme.colors.textSecondary');
}

.form-hint {
  font-size: 12px;
  color: v-bind('themeStore.theme.colors.textSecondary');
  margin-left: 8px;
}
</style>
