<template>
  <div class="assistant-page">
    <div class="page-header">
      <h1>聊天助理</h1>
      <n-button type="primary" @click="handleCreateAssistant">
        <template #icon>
          <n-icon :component="AddOutline" />
        </template>
        新建助理
      </n-button>
    </div>

    <div v-if="loading" class="loading">
      <n-spin size="large" />
    </div>

    <div v-else-if="assistants.length === 0" class="empty">
      <n-empty description="暂无助理" />
      <n-button @click="handleCreateAssistant">创建第一个助理</n-button>
    </div>

    <div v-else class="assistants-list">
      <div
        v-for="assistant in assistants"
        :key="assistant.id"
        class="assistant-item"
        :style="{
          backgroundColor: themeStore.theme.colors.surface,
          borderColor: themeStore.theme.colors.border
        }"
        @click="router.push(`/chat/${assistant.id}`)"
      >
        <div class="assistant-icon">
          <n-icon :component="ChatbubbleEllipsesOutline" :size="24" />
        </div>
        <div class="assistant-info">
          <h3>{{ assistant.assistantName }}</h3>
          <p class="description">{{ assistant.assistantDescription || '暂无描述' }}</p>
          <div class="assistant-meta">
            <span>{{ formatTime(assistant.updatedTime) }}</span>
          </div>
        </div>
        <n-dropdown :options="menuOptions" @select="handleMenuSelect($event, assistant)">
          <n-button text @click.stop>
            <n-icon :component="EllipsisVerticalOutline" :size="20" />
          </n-button>
        </n-dropdown>
      </div>
    </div>

    <!-- 创建助理对话框 -->
    <n-modal v-model:show="showCreateModal" preset="card" title="新建聊天助理" style="max-width: 500px">
      <n-form ref="formRef" :model="formData">
        <n-form-item label="助理名称" required>
          <n-input v-model:value="formData.assistantName" placeholder="请输入助理名称" />
        </n-form-item>
        <n-form-item label="助理描述">
          <n-input
            v-model:value="formData.assistantDescription"
            type="textarea"
            placeholder="请输入助理描述(可选)"
            :autosize="{ minRows: 2, maxRows: 4 }"
          />
        </n-form-item>
      </n-form>
      <template #footer>
        <div style="display: flex; justify-content: flex-end; gap: 12px">
          <n-button @click="showCreateModal = false">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="handleSubmit">创建</n-button>
        </div>
      </template>
    </n-modal>

    <!-- 编辑助理对话框 -->
    <n-modal v-model:show="showEditModal" preset="card" title="编辑聊天助理" style="max-width: 500px">
      <n-form ref="editFormRef" :model="editFormData">
        <n-form-item label="助理名称" required>
          <n-input v-model:value="editFormData.assistantName" placeholder="请输入助理名称" />
        </n-form-item>
        <n-form-item label="助理描述">
          <n-input
            v-model:value="editFormData.assistantDescription"
            type="textarea"
            placeholder="请输入助理描述(可选)"
            :autosize="{ minRows: 2, maxRows: 4 }"
          />
        </n-form-item>
      </n-form>
      <template #footer>
        <div style="display: flex; justify-content: flex-end; gap: 12px">
          <n-button @click="showEditModal = false">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="handleEditSubmit">保存</n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
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
  useMessage,
  useDialog
} from 'naive-ui'
import { AddOutline, ChatbubbleEllipsesOutline, EllipsisVerticalOutline } from '@vicons/ionicons5'
import { useThemeStore } from '@/stores/theme'
import { assistantApi } from '@/api/assistant'
import type { ChatAssistant } from '@/types'

const router = useRouter()
const message = useMessage()
const dialog = useDialog()
const themeStore = useThemeStore()

const loading = ref(false)
const submitting = ref(false)
const showCreateModal = ref(false)
const showEditModal = ref(false)
const editingAssistant = ref<ChatAssistant | null>(null)

const assistants = ref<ChatAssistant[]>([])

const formRef = ref()
const formData = ref({
  assistantName: '',
  assistantDescription: ''
})

const editFormRef = ref()
const editFormData = ref({
  assistantName: '',
  assistantDescription: ''
})

const menuOptions = [
  { label: '编辑', key: 'edit' },
  { label: '删除', key: 'delete' }
]

const formatTime = (time: string) => {
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const day = 24 * 60 * 60 * 1000

  if (diff < day) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  } else if (diff < 7 * day) {
    return Math.floor(diff / day) + ' 天前'
  } else {
    return date.toLocaleDateString('zh-CN')
  }
}

const handleCreateAssistant = () => {
  formData.value = { assistantName: '', assistantDescription: '' }
  showCreateModal.value = true
}

const loadAssistants = async () => {
  try {
    loading.value = true
    const res = await assistantApi.getList()
    assistants.value = res.data
  } catch (error) {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  if (!formData.value.assistantName.trim()) {
    message.warning('请输入助理名称')
    return
  }

  try {
    submitting.value = true
    const res = await assistantApi.create(formData.value)
    message.success('创建成功')
    showCreateModal.value = false
    router.push(`/chat/${res.data}`)
  } catch (error) {
    message.error('创建失败')
  } finally {
    submitting.value = false
  }
}

const handleEditSubmit = async () => {
  if (!editFormData.value.assistantName.trim()) {
    message.warning('请输入助理名称')
    return
  }
  if (!editingAssistant.value) return

  try {
    submitting.value = true
    await assistantApi.update(editingAssistant.value.id, editFormData.value)
    message.success('保存成功')
    showEditModal.value = false
    loadAssistants()
  } catch (error) {
    message.error('保存失败')
  } finally {
    submitting.value = false
  }
}

const handleMenuSelect = async (key: string, assistant: ChatAssistant) => {
  switch (key) {
    case 'edit':
      editingAssistant.value = assistant
      editFormData.value = {
        assistantName: assistant.assistantName,
        assistantDescription: assistant.assistantDescription || ''
      }
      showEditModal.value = true
      break
    case 'delete':
      dialog.warning({
        title: '确认删除',
        content: `确定要删除助理"${assistant.assistantName}"吗？这将删除该助理下的所有会话和消息。`,
        positiveText: '删除',
        negativeText: '取消',
        onPositiveClick: async () => {
          try {
            await assistantApi.delete(assistant.id)
            message.success('删除成功')
            loadAssistants()
          } catch (error) {
            message.error('删除失败')
          }
        }
      })
      break
  }
}

onMounted(() => {
  loadAssistants()
})
</script>

<style scoped>
.assistant-page {
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

.assistants-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 16px;
}

.assistant-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  border-radius: 12px;
  border: 1px solid;
  cursor: pointer;
  transition: all 0.2s ease;
}

.assistant-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.assistant-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: v-bind('themeStore.theme.colors.active');
  border-radius: 12px;
  flex-shrink: 0;
}

.assistant-info {
  flex: 1;
  min-width: 0;
}

.assistant-info h3 {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.description {
  font-size: 13px;
  color: v-bind('themeStore.theme.colors.textSecondary');
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.assistant-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: v-bind('themeStore.theme.colors.textSecondary');
}
</style>
