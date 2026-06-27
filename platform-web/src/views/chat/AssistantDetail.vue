<template>
  <div class="assistant-chat-page">
    <!-- 左侧会话列表 -->
    <div class="sessions-sidebar" :style="{ borderRight: `1px solid ${themeStore.theme.colors.border}` }">
      <div class="sidebar-header">
        <n-button text @click="router.push('/chat')">
          <n-icon :component="ArrowBackOutline" :size="20" />
        </n-button>
        <h3>{{ assistant?.assistantName || '助理' }}</h3>
        <n-button
          text
          :disabled="!assistantConfigured"
          @click="handleCreateSession"
          :style="{ opacity: assistantConfigured ? 1 : 0.4 }"
        >
          <n-icon :component="AddOutline" :size="20" />
        </n-button>
      </div>

      <div class="sessions-list">
        <div v-if="loading" class="loading-sessions">
          <n-spin size="small" />
        </div>
        <div v-else-if="sessions.length === 0" class="empty-sessions">
          <n-empty description="暂无会话" size="small" />
        </div>
        <div
          v-else
          v-for="session in sessions"
          :key="session.id"
          class="session-item"
          :class="{ active: currentSessionId === session.id }"
          :style="{
            backgroundColor: currentSessionId === session.id
              ? themeStore.theme.colors.active
              : 'transparent'
          }"
        >
          <div class="session-content" @click="selectSession(session.id)">
            <div class="session-title">{{ session.title }}</div>
            <div class="session-time">{{ formatTime(session.updatedAt) }}</div>
          </div>
          <div class="session-actions">
            <n-button
              text
              size="small"
              class="action-btn"
              @click.stop="startEditTitle(session)"
            >
              <n-icon :component="CreateOutline" :size="16" />
            </n-button>
            <n-button
              text
              size="small"
              class="action-btn delete-btn"
              @click.stop="handleDeleteSession(session)"
            >
              <n-icon :component="TrashOutline" :size="16" />
            </n-button>
          </div>
        </div>
      </div>

      <div class="sidebar-footer">
        <n-button block @click="showSettings = true">
          <template #icon>
            <n-icon :component="SettingsOutline" />
          </template>
          助理设置
        </n-button>
      </div>
    </div>

    <!-- 中间聊天区域 -->
    <div class="chat-main" :style="{ backgroundColor: themeStore.theme.colors.background }">
      <div v-if="!currentSessionId" class="empty-chat">
        <n-empty description="请点击 + 创建会话">
          <template #extra>
            <n-button
              v-if="!assistantConfigured"
              type="primary"
              @click="showSettings = true"
            >
              先配置助理
            </n-button>
          </template>
        </n-empty>
      </div>

      <template v-else>
        <div
          class="chat-header"
        >
          <h2>{{ currentSession?.title || '对话' }}</h2>
        </div>

        <div ref="messagesContainer" class="messages-container">
          <div v-if="messagesLoading" class="loading">
            <n-spin />
          </div>

          <div v-else-if="!messages || messages.length === 0" class="empty-messages">
            <n-empty description="开始对话吧" />
          </div>

          <div v-else class="messages-list">
            <div
              v-for="msg in messages"
              :key="msg.id"
              class="message-item"
              :class="msg.role === 'USER' ? 'user-message' : 'assistant-message'"
            >
              <div class="message-avatar">
                <n-icon
                  :component="msg.role === 'USER' ? PersonOutline : ChatbubbleEllipsesOutline"
                  :size="20"
                />
              </div>
              <div
                class="message-content"
                :style="{
                  backgroundColor:
                    msg.role === 'USER'
                      ? themeStore.theme.colors.active
                      : themeStore.theme.colors.surface
                }"
              >
                <!-- 过程追踪面板 -->
                <ProcessTracePanel
                  v-if="showTrace && msg.role === 'ASSISTANT' && msg.processSteps && msg.processSteps.length > 0"
                  :steps="msg.processSteps"
                  :total-elapsed-ms="msg.totalElapsedMs"
                />

                <div v-if="msg.role === 'USER'" class="message-text">
                  {{ msg.content }}
                </div>
                <div
                  v-else-if="(msg as any).streaming"
                  class="message-text streaming-text"
                >
                  {{ msg.content }}<span class="typing-cursor">|</span>
                </div>
                <MarkdownRenderer
                  v-else
                  :content="msg.content"
                  class="markdown-preview"
                />
                <div class="message-time">{{ formatTime(msg.createdAt) }}</div>
              </div>
            </div>
          </div>
        </div>

        <div
          class="input-area"
        >
          <div class="input-wrapper">
            <div class="input-container">
              <n-input
                v-model:value="inputText"
                type="textarea"
                :autosize="{ minRows: 2, maxRows: 12 }"
                :placeholder="!assistantConfigured ? '请先配置助理' : '输入消息...'"
                :disabled="sending || !assistantConfigured"
                @keydown="handleKeyDown"
              />
              <div class="input-actions">
                <div class="action-left">
                  <n-button
                    :type="showTrace ? 'primary' : 'default'"
                    @click="showTrace = !showTrace"
                  >
                    <template #icon>
                      <img src="/src/assets/image/think.png" class="think-icon" />
                    </template>
                    推理过程
                  </n-button>
                </div>
                <div class="action-right">
                  <n-button
                    type="primary"
                    :loading="sending"
                    :disabled="!inputText.trim() || !assistantConfigured"
                    @click="handleSend"
                  >
                    <template #icon>
                      <n-icon :component="SendOutline" />
                    </template>
                    发送
                  </n-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- 右侧助理设置抽屉 -->
    <n-drawer v-model:show="showSettings" :width="500" placement="right">
      <n-drawer-content title="助理设置" closable>
        <n-form ref="settingsFormRef" :model="settingsForm" label-placement="top">
          <n-form-item label="助理名称" required>
            <n-input v-model:value="settingsForm.assistantName" placeholder="请输入助理名称" />
          </n-form-item>
          <n-form-item label="助理描述">
            <n-input
              v-model:value="settingsForm.assistantDescription"
              type="textarea"
              placeholder="请输入助理描述"
              :autosize="{ minRows: 2, maxRows: 4 }"
            />
          </n-form-item>
          <n-form-item label="开场白">
            <template #label>
              <span class="form-label-with-help">
                开场白
                <n-tooltip trigger="hover">
                  <template #trigger>
                    <n-icon :component="HelpCircleOutline" :size="16" class="help-icon" />
                  </template>
                  用户打开对话时，助理自动发送的第一条消息，用于引导用户开始对话
                </n-tooltip>
              </span>
            </template>
            <n-input
              v-model:value="settingsForm.openingStatement"
              type="textarea"
              placeholder="请输入开场白（可选）"
              :autosize="{ minRows: 2, maxRows: 4 }"
            />
          </n-form-item>
          <n-form-item label="空回复提示">
            <template #label>
              <span class="form-label-with-help">
                空回复提示
                <n-tooltip trigger="hover">
                  <template #trigger>
                    <n-icon :component="HelpCircleOutline" :size="16" class="help-icon" />
                  </template>
                  当知识库中没有找到相关内容时，助理返回的默认回复
                </n-tooltip>
              </span>
            </template>
            <n-input
              v-model:value="settingsForm.emptyReply"
              placeholder="当没有答案时的回复（可选）"
            />
          </n-form-item>
          <n-form-item label="聊天模型" required>
            <n-select
              v-model:value="settingsForm.modelId"
              :options="models.map(m => ({ label: m.modelName, value: m.id }))"
              placeholder="选择聊天模型"
              :loading="modelOptionsLoading"
            />
          </n-form-item>
          <n-form-item label="知识库">
            <n-select
              v-model:value="settingsForm.knowledgeBaseId"
              :options="knowledgeBases.map(kb => ({ label: kb.name, value: String(kb.id) }))"
              placeholder="选择知识库（可选）"
              :loading="knowledgeBaseOptionsLoading"
              clearable
            />
          </n-form-item>
          <n-form-item label="系统提示词">
            <n-input
              v-model:value="settingsForm.systemPrompt"
              type="textarea"
              placeholder="请输入系统提示词（可选）"
              :autosize="{ minRows: 3, maxRows: 6 }"
            />
          </n-form-item>

          <!-- 模型参数 -->
          <n-divider>模型参数</n-divider>
          <n-form-item label="Top N">
            <template #label>
              <span class="form-label-with-help">
                Top N
                <n-tooltip trigger="hover">
                  <template #trigger>
                    <n-icon :component="HelpCircleOutline" :size="16" class="help-icon" />
                  </template>
                  从知识库中检索最相似的 N 个文档片段作为上下文。值越大，上下文越丰富但可能引入噪音。建议值：3-10
                </n-tooltip>
              </span>
            </template>
            <n-input-number
              v-model:value="settingsForm.topN"
              :min="1"
              :max="20"
              placeholder="5"
              style="width: 100%"
            />
          </n-form-item>
          <n-form-item label="Top P">
            <template #label>
              <span class="form-label-with-help">
                Top P
                <n-tooltip trigger="hover">
                  <template #trigger>
                    <n-icon :component="HelpCircleOutline" :size="16" class="help-icon" />
                  </template>
                  核采样参数，控制模型考虑的词汇范围。值越小，输出越确定；值越大，输出越多样。建议值：0.7-0.95
                </n-tooltip>
              </span>
            </template>
            <n-input-number
                v-model:value="settingsForm.topP"
                :min="0"
                :max="1"
                :step="0.01"
                placeholder="0.0 - 1.0"
                style="width: 100%"
            />
          </n-form-item>
          <n-form-item label="启用推理模式">
            <template #label>
              <span class="form-label-with-help">
                启用推理模式
                <n-tooltip trigger="hover">
                  <template #trigger>
                    <n-icon :component="HelpCircleOutline" :size="16" class="help-icon" />
                  </template>
                  启用后，模型会进行更深入的推理分析，适合复杂问题。会增加响应时间和 Token 消耗
                </n-tooltip>
              </span>
            </template>
            <n-select
                v-model:value="settingsForm.enableReasoningMode"
                :options="[
                { label: '禁用', value: 0 },
                { label: '启用', value: 1 }
              ]"
                placeholder="选择是否启用推理模式"
            />
          </n-form-item>
          <n-form-item label="温度">
            <template #label>
              <span class="form-label-with-help">
                温度
                <n-tooltip trigger="hover">
                  <template #trigger>
                    <n-icon :component="HelpCircleOutline" :size="16" class="help-icon" />
                  </template>
                  控制输出的随机性。值越高，回复越有创意但可能不太准确；值越低，回复越确定和保守。建议值：0.3-0.7
                </n-tooltip>
              </span>
            </template>
            <n-input-number
              v-model:value="settingsForm.temperature"
              :min="0"
              :max="2"
              :step="0.1"
              placeholder="0.0 - 2.0"
              style="width: 100%"
            />
          </n-form-item>
          <n-form-item label="存在惩罚">
            <template #label>
              <span class="form-label-with-help">
                存在惩罚
                <n-tooltip trigger="hover">
                  <template #trigger>
                    <n-icon :component="HelpCircleOutline" :size="16" class="help-icon" />
                  </template>
                  惩罚已出现过的内容，鼓励模型讨论新话题。正值减少重复，负值增加重复。建议值：0-1
                </n-tooltip>
              </span>
            </template>
            <n-input-number
              v-model:value="settingsForm.presencePenalty"
              :min="-2"
              :max="2"
              :step="0.1"
              placeholder="-2.0 - 2.0"
              style="width: 100%"
            />
          </n-form-item>
          <n-form-item label="频率惩罚">
            <template #label>
              <span class="form-label-with-help">
                频率惩罚
                <n-tooltip trigger="hover">
                  <template #trigger>
                    <n-icon :component="HelpCircleOutline" :size="16" class="help-icon" />
                  </template>
                  根据词频惩罚重复词汇，减少逐字重复。正值减少重复用词，负值允许更多重复。建议值：0-1
                </n-tooltip>
              </span>
            </template>
            <n-input-number
              v-model:value="settingsForm.frequencyPenalty"
              :min="-2"
              :max="2"
              :step="0.1"
              placeholder="-2.0 - 2.0"
              style="width: 100%"
            />
          </n-form-item>
          <n-form-item label="最大Token数量">
            <template #label>
              <span class="form-label-with-help">
                最大Token数量
                <n-tooltip trigger="hover">
                  <template #trigger>
                    <n-icon :component="HelpCircleOutline" :size="16" class="help-icon" />
                  </template>
                  限制模型单次回复的最大长度。1个Token约等于1-2个汉字或3-4个英文字母。建议值：1000-4000
                </n-tooltip>
              </span>
            </template>
            <n-input-number
              v-model:value="settingsForm.maxTokens"
              :min="1"
              :max="32000"
              placeholder="最大输出Token数"
              style="width: 100%"
            />
          </n-form-item>
        </n-form>
        <template #footer>
          <div style="display: flex; justify-content: flex-end; gap: 12px">
            <n-button @click="showSettings = false">取消</n-button>
            <n-button type="primary" :loading="savingSettings" @click="handleSaveSettings">
              保存
            </n-button>
          </div>
        </template>
      </n-drawer-content>
    </n-drawer>

    <!-- 编辑标题弹窗 -->
    <n-modal v-model:show="editingTitle" preset="dialog" title="修改会话标题">
      <n-input
        v-model:value="editTitleValue"
        placeholder="请输入会话标题"
        @keydown.enter="handleSaveTitle"
      />
      <template #action>
        <n-button @click="cancelEditTitle">取消</n-button>
        <n-button type="primary" :loading="savingTitle" @click="handleSaveTitle">
          保存
        </n-button>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  NButton,
  NIcon,
  NSpin,
  NEmpty,
  NInput,
  NSelect,
  NDrawer,
  NDrawerContent,
  NForm,
  NFormItem,
  NInputNumber,
  NDivider,
  NModal,
  NTooltip,
  useMessage,
  useDialog
} from 'naive-ui'
import {
  ArrowBackOutline,
  AddOutline,
  ChatbubbleEllipsesOutline,
  PersonOutline,
  SendOutline,
  SettingsOutline,
  TrashOutline,
  CreateOutline,
  HelpCircleOutline,
  EyeOutline,
  EyeOffOutline
} from '@vicons/ionicons5'
import { useThemeStore } from '@/stores/theme'
import { assistantApi } from '@/api/assistant'
import { chatApi } from '@/api/chat'
import { modelApi, type AiModelConfig } from '@/api/model'
import { knowledgeBaseApi } from '@/api/knowledgeBase'
import type { ChatAssistant, ChatSession, ChatMessage, KnowledgeBase } from '@/types'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import ProcessTracePanel from '@/components/ProcessTracePanel.vue'

const router = useRouter()
const route = useRoute()
const message = useMessage()
const dialog = useDialog()
const themeStore = useThemeStore()

const assistantId = route.params.id as string

const loading = ref(false)
const messagesLoading = ref(false)
const sending = ref(false)
const showSettings = ref(false)
const savingSettings = ref(false)
const editingTitle = ref(false)
const editTitleValue = ref('')
const savingTitle = ref(false)
const editingSessionId = ref<string | null>(null)

const assistant = ref<ChatAssistant | null>(null)
const sessions = ref<ChatSession[]>([])
const currentSessionId = ref<string | null>(null)
const currentSession = ref<ChatSession | null>(null)
const messages = ref<ChatMessage[]>([])
const inputText = ref('')
const showTrace = ref(localStorage.getItem('chat_showTrace') !== 'false')
const messagesContainer = ref<HTMLElement>()

const models = ref<AiModelConfig[]>([])
const modelOptionsLoading = ref(false)

const knowledgeBases = ref<KnowledgeBase[]>([])
const knowledgeBaseOptionsLoading = ref(false)

const settingsFormRef = ref()
const settingsForm = ref({
  assistantName: '',
  assistantDescription: '',
  emptyReply: '',
  openingStatement: '',
  knowledgeBaseId: null as string | null,
  systemPrompt: '',
  topP: null as number | null,
  topN: null as number | null,
  enableReasoningMode: null as number | null,
  modelId: null as string | null,
  temperature: null as number | null,
  presencePenalty: null as number | null,
  frequencyPenalty: null as number | null,
  maxTokens: null as number | null
})

const assistantConfigured = computed(() => assistant.value?.modelId != null)
const isTemporarySession = computed(() => currentSessionId.value != null && currentSessionId.value?.startsWith('-'))

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

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

const loadAssistant = async () => {
  try {
    const res = await assistantApi.getById(assistantId)
    assistant.value = res.data

    // 加载设置表单
    settingsForm.value = {
      assistantName: res.data.assistantName,
      assistantDescription: res.data.assistantDescription || '',
      emptyReply: res.data.emptyReply || '',
      openingStatement: res.data.openingStatement || '',
      knowledgeBaseId: res.data.knowledgeBaseId,
      systemPrompt: res.data.systemPrompt || '',
      topP: res.data.topP,
      topN: res.data.topN,
      enableReasoningMode: res.data.enableReasoningMode,
      modelId: res.data.modelId,
      temperature: res.data.temperature,
      presencePenalty: res.data.presencePenalty,
      frequencyPenalty: res.data.frequencyPenalty,
      maxTokens: res.data.maxTokens
    }

    // 如果助理未配置模型，自动打开设置
    if (!assistantConfigured.value) {
      showSettings.value = true
    }
  } catch (error) {
    message.error('加载助理信息失败')
  }
}

const loadSessions = async () => {
  try {
    loading.value = true
    const res = await chatApi.getSessionsByAssistantId(assistantId)
    sessions.value = res.data
  } catch (error) {
    message.error('加载会话列表失败')
  } finally {
    loading.value = false
  }
}

const loadModels = async () => {
  try {
    modelOptionsLoading.value = true
    const res = await modelApi.getAvailableChatModelConfigs()
    models.value = res.data.filter(m => m.modelType === 'CHAT')
  } catch (error) {
    message.error('加载模型列表失败')
  } finally {
    modelOptionsLoading.value = false
  }
}

const loadKnowledgeBases = async () => {
  try {
    knowledgeBaseOptionsLoading.value = true
    const res = await knowledgeBaseApi.getAll()
    knowledgeBases.value = res.data
  } catch (error) {
    message.error('加载知识库列表失败')
  } finally {
    knowledgeBaseOptionsLoading.value = false
  }
}

const loadMessages = async (sessionId: string) => {
  // 临时会话没有消息，只显示开场白
  if (sessionId.startsWith('-')) {
    const openingMessage = assistant.value?.openingStatement || '你好！ 我是你的助理，有什么可以帮到你的吗？'
    messages.value = [
      {
        id: String(-(Date.now())),
        sessionId: sessionId,
        role: 'ASSISTANT',
        content: openingMessage,
        createdAt: new Date().toISOString()
      }
    ]
    await nextTick()
    await nextTick()
    scrollToBottom()
    return
  }

  try {
    messagesLoading.value = true
    const res = await chatApi.getMessages(sessionId)
    const loadedMessages = res.data

    // 解析历史消息中的过程步骤（JSON字符串 -> ProcessStep数组）
    const parsedMessages = loadedMessages.map(msg => {
      if (msg.role === 'ASSISTANT' && msg.processSteps && typeof msg.processSteps === 'string') {
        try {
          msg.processSteps = JSON.parse(msg.processSteps)
        } catch (e) {
          console.error('解析过程步骤失败:', e)
          msg.processSteps = []
        }
      }
      return msg
    })

    // 检查当前是否已经有前端生成的开场白（ID为负数），并且是同一个会话
    const hasOpeningMessage = messages.value.length > 0 &&
                               messages.value[0].role === 'ASSISTANT' &&
                               messages.value[0].id.startsWith('-') &&
                               messages.value[0].sessionId === sessionId

    if (parsedMessages.length === 0) {
      // 如果后端没有消息，显示开场白
      const openingMessage = assistant.value?.openingStatement || '你好！ 我是你的助理，有什么可以帮到你的吗？'
      messages.value = [
        {
          id: String(-(Date.now())),
          sessionId: sessionId,
          role: 'ASSISTANT',
          content: openingMessage,
          createdAt: new Date().toISOString()
        }
      ]
    } else if (hasOpeningMessage) {
      // 如果之前有开场白且是同一会话，保留它并追加后端消息
      messages.value = [
        messages.value[0],
        ...parsedMessages
      ]
    } else {
      // 否则直接使用后端消息
      messages.value = parsedMessages
    }

    messagesLoading.value = false
    await nextTick()
    await nextTick()
    scrollToBottom()
  } catch (error) {
    message.error('加载消息失败')
    messagesLoading.value = false
  }
}

const handleCreateSession = () => {
  if (!assistantConfigured.value) {
    message.warning('请先配置助理')
    return
  }

  // 创建临时会话
  const tempSessionId = -(Date.now())
  currentSessionId.value = String(tempSessionId)
  currentSession.value = {
    id: String(tempSessionId),
    assistantId: assistantId,
    title: '新会话',
    knowledgeBaseId: null,
    modelId: assistant.value?.modelId,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  }

  // 加载开场白消息
  loadMessages(String(tempSessionId))

  // 更新URL
  router.replace({ query: { session: String(tempSessionId) } })
}

const selectSession = async (sessionId: string) => {
  currentSessionId.value = sessionId
  currentSession.value = sessions.value.find(s => s.id === sessionId) || null

  // 更新URL
  router.replace({ query: { session: String(sessionId) } })

  // 加载消息
  await loadMessages(sessionId)
}

const handleDeleteSession = (session: ChatSession) => {
  dialog.warning({
    title: '确认删除',
    content: `确定要删除会话"${session.title}"吗？`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await chatApi.deleteSession(session.id)
        message.success('删除成功')

        // 如果删除的是当前会话，清空聊天区域
        if (currentSessionId.value === session.id) {
          currentSessionId.value = null
          currentSession.value = null
          messages.value = []
          router.replace({ query: {} })
        }

        // 重新加载会话列表
        await loadSessions()
      } catch (error) {
        message.error('删除失败')
      }
    }
  })
}

const startEditTitle = (session: ChatSession) => {
  editingSessionId.value = session.id
  editTitleValue.value = session.title || ''
  editingTitle.value = true
}

const cancelEditTitle = () => {
  editingTitle.value = false
  editTitleValue.value = ''
  editingSessionId.value = null
}

const handleSaveTitle = async () => {
  if (!editTitleValue.value.trim()) {
    message.warning('标题不能为空')
    return
  }

  if (!editingSessionId.value || editingSessionId.value?.startsWith('-')) {
    message.warning('无法修改临时会话标题')
    return
  }

  try {
    savingTitle.value = true
    await chatApi.updateSessionTitle(editingSessionId.value, editTitleValue.value.trim())
    message.success('标题修改成功')

    // 更新当前会话标题
    if (currentSession.value && currentSession.value.id === editingSessionId.value) {
      currentSession.value.title = editTitleValue.value.trim()
    }

    // 更新会话列表中的标题
    const sessionInList = sessions.value.find(s => s.id === editingSessionId.value)
    if (sessionInList) {
      sessionInList.title = editTitleValue.value.trim()
    }

    editingTitle.value = false
    editingSessionId.value = null
  } catch (error) {
    message.error('修改标题失败')
  } finally {
    savingTitle.value = false
  }
}

const handleSaveSettings = async () => {
  if (!settingsForm.value.modelId) {
    message.warning('请选择聊天模型')
    return
  }

  try {
    savingSettings.value = true
    await assistantApi.update(assistantId, settingsForm.value as any)
    message.success('保存成功')
    showSettings.value = false

    // 重新加载助理信息
    await loadAssistant()
    await loadModels()
  } catch (error) {
    message.error('保存失败')
  } finally {
    savingSettings.value = false
  }
}

const handleSend = async () => {
  if (!inputText.value.trim() || sending.value) return

  const content = inputText.value.trim()

  // 如果是临时会话，先创建真实会话
  if (isTemporarySession.value) {
    try {
      // 使用用户提问的前15个字符作为会话标题
      const sessionTitle = content.length > 15 ? content.substring(0, 15) + '...' : content
      const createRes = await chatApi.createSession({
        assistantId: assistantId,
        title: sessionTitle,
        modelId: assistant.value?.modelId ?? undefined
      })
      const newSessionId = createRes.data

      // 更新当前会话
      currentSessionId.value = newSessionId
      currentSession.value!.id = newSessionId

      // 更新开场白消息的sessionId（如果存在）
      if (messages.value.length > 0 && messages.value[0].id.startsWith('-') && messages.value[0].role === 'ASSISTANT') {
        messages.value[0].sessionId = newSessionId
      }

      // 更新URL
      router.replace({ query: { session: String(newSessionId) } })

      // 刷新会话列表
      loadSessions()
    } catch (error) {
      message.error('创建会话失败')
      return
    }
  }

  inputText.value = ''
  sending.value = true

  // 立即添加用户消息到界面
  const userMessage: ChatMessage = {
    id: String(Date.now()),
    sessionId: currentSessionId.value!,
    role: 'USER',
    content: content,
    createdAt: new Date().toISOString()
  }
  messages.value.push(userMessage)
  scrollToBottom()

  // 创建临时的助手消息对象
  const assistantMessage: ChatMessage & { streaming?: boolean } = {
    id: String(Date.now() + 1),
    sessionId: currentSessionId.value!,
    role: 'ASSISTANT',
    content: '',
    createdAt: new Date().toISOString(),
    streaming: true,
    processSteps: []
  }
  messages.value.push(assistantMessage as ChatMessage)
  scrollToBottom()

  let eventSource: EventSource | null = null
  let streamClosed = false

  try {
    eventSource = chatApi.streamChat(currentSessionId.value!, content, assistantId)

    // 处理步骤事件
    eventSource.addEventListener('step', (event) => {
      try {
        const step = JSON.parse(event.data)
        assistantMessage.processSteps!.push(step)
        messages.value = [...messages.value]
        scrollToBottom()
      } catch (e) {
        console.error('解析步骤事件失败:', e)
      }
    })

    // 处理消息内容事件
    eventSource.addEventListener('message', (event) => {
      try {
        const msg = JSON.parse(event.data)
        if (msg.content) {
          assistantMessage.content += msg.content
          messages.value = [...messages.value]
          scrollToBottom()
        }
      } catch (e) {
        console.error('解析消息事件失败:', e)
      }
    })

    // 处理完成事件
    eventSource.addEventListener('done', (event) => {
      try {
        const done = JSON.parse(event.data)
        assistantMessage.totalTokens = done.totalTokens
        assistantMessage.totalElapsedMs = done.totalElapsedMs
        messages.value = [...messages.value]
      } catch (e) {
        console.error('解析完成事件失败:', e)
      }
    })

    eventSource.addEventListener('close', async () => {
      if (!streamClosed) {
        streamClosed = true
        eventSource?.close()
        sending.value = false
        assistantMessage.streaming = false
        messages.value = [...messages.value]

        await nextTick()
        scrollToBottom()

        await loadMessages(currentSessionId.value!)
        await nextTick()
        scrollToBottom()
      }
    })

    eventSource.onerror = (error) => {
      if (!streamClosed) {
        streamClosed = true
        console.error('SSE连接错误:', error)
        eventSource?.close()
        sending.value = false

        if (assistantMessage.content && assistantMessage.content.trim()) {
          assistantMessage.streaming = false
        } else {
          assistantMessage.content = '抱歉，回复失败，请重试。'
          message.error('对话失败')
        }
        messages.value = [...messages.value]
      }
    }
  } catch (error) {
    console.error('发送失败:', error)
    message.error('发送失败')
    sending.value = false
    if (eventSource) {
      eventSource.close()
    }
    messages.value.pop()
  }
}

const handleKeyDown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

// 监听路由query变化
watch(() => route.query.session, (sessionParam) => {
  if (sessionParam) {
    const sessionId = String(sessionParam)
    if (sessionId !== currentSessionId.value) {
      selectSession(sessionId)
    }
  }
})

// 监听设置抽屉打开，重置表单为原始数据
watch(showSettings, (show) => {
  if (show && assistant.value) {
    // 每次打开抽屉时，从assistant重新加载数据，丢弃未保存的修改
    settingsForm.value = {
      assistantName: assistant.value.assistantName,
      assistantDescription: assistant.value.assistantDescription || '',
      emptyReply: assistant.value.emptyReply || '',
      openingStatement: assistant.value.openingStatement || '',
      knowledgeBaseId: assistant.value.knowledgeBaseId,
      systemPrompt: assistant.value.systemPrompt || '',
      topP: assistant.value.topP,
      topN: assistant.value.topN,
      enableReasoningMode: assistant.value.enableReasoningMode,
      modelId: assistant.value.modelId,
      temperature: assistant.value.temperature,
      presencePenalty: assistant.value.presencePenalty,
      frequencyPenalty: assistant.value.frequencyPenalty,
      maxTokens: assistant.value.maxTokens
    }
  }
})

// 持久化推理过程显示状态
watch(showTrace, (val) => {
  localStorage.setItem('chat_showTrace', String(val))
})

onMounted(async () => {
  await loadAssistant()
  await loadModels()
  await loadKnowledgeBases()
  await loadSessions()

  // 检查URL中是否有session参数
  if (route.query.session) {
    const sessionId = String(route.query.session)
    selectSession(sessionId)
  }
})
</script>

<style scoped>
.assistant-chat-page {
  display: flex;
  height: 100vh;
}

/* 左侧会话列表 */
.sessions-sidebar {
  width: 280px;
  display: flex;
  flex-direction: column;
  background-color: v-bind('themeStore.theme.colors.surface');
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid v-bind('themeStore.theme.colors.border');
}

.sidebar-header h3 {
  flex: 1;
  font-size: 16px;
  font-weight: 500;
  margin: 0 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sessions-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.loading-sessions {
  display: flex;
  justify-content: center;
  padding: 24px;
}

.empty-sessions {
  display: flex;
  justify-content: center;
  padding: 24px;
}

.session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 4px;
  transition: background-color 0.2s;
  position: relative;
}

.session-item:hover {
  background-color: v-bind('themeStore.theme.colors.active');
}

.session-item.active {
  background-color: v-bind('themeStore.theme.colors.active');
}

.session-content {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}

.session-title {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-time {
  font-size: 12px;
  color: v-bind('themeStore.theme.colors.textSecondary');
}

.session-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.session-item:hover .session-actions {
  opacity: 1;
}

.action-btn {
  color: v-bind('themeStore.theme.colors.textSecondary');
}

.action-btn:hover {
  color: v-bind('themeStore.theme.colors.primary');
}

.action-btn.delete-btn:hover {
  color: #d03050;
}

.sidebar-footer {
  padding: 12px;
  border-top: 1px solid v-bind('themeStore.theme.colors.border');
}

/* 中间聊天区域 */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.empty-chat {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  gap: 12px;
}

.chat-header h2 {
  font-size: 18px;
  font-weight: 500;
  flex: 1;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.loading,
.empty-messages {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.messages-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 900px;
  margin: 0 auto;
}

.message-item {
  display: flex;
  gap: 12px;
}

.user-message {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: v-bind('themeStore.theme.colors.active');
  flex-shrink: 0;
}

.message-content {
  max-width: 70%;
  padding: 16px 18px;
  border-radius: 12px;
}

.user-message .message-content {
  border-top-right-radius: 4px;
}

.assistant-message .message-content {
  border-top-left-radius: 4px;
}

.message-text {
  font-size: 15px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-time {
  font-size: 12px;
  color: v-bind('themeStore.theme.colors.textSecondary');
  margin-top: 4px;
}

.input-area {
  padding: 12px 24px 20px;
  background-color: v-bind('themeStore.theme.colors.background');
}

.input-wrapper {
  max-width: 900px;
  margin: 0 auto;
}

.input-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
  background-color: v-bind('themeStore.theme.colors.surface');
  border-radius: 16px;
  padding: 12px 16px;
}

.input-container :deep(.n-input) {
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
  padding: 0;
}

.input-container :deep(.n-input::before),
.input-container :deep(.n-input::after) {
  display: none;
}

.input-container :deep(.n-input .n-input__textarea-el) {
  font-size: 15px;
  line-height: 1.6;
  resize: none;
  border: none !important;
  box-shadow: none !important;
  background: transparent !important;
}

.input-container :deep(.n-input-wrapper) {
  padding: 0 !important;
  border: none !important;
  background: transparent !important;
  box-shadow: none !important;
}

.input-container :deep(.n-input-wrapper::before),
.input-container :deep(.n-input-wrapper::after) {
  display: none;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.think-icon {
  width: 18px;
  height: 18px;
}

.markdown-preview {
  background: transparent !important;
}

.markdown-preview :deep(pre) {
  background-color: rgba(0, 0, 0, 0.05) !important;
  border-radius: 8px;
}

.streaming-text {
  position: relative;
}

.typing-cursor {
  display: inline-block;
  margin-left: 2px;
  animation: blink 1s step-end infinite;
  color: v-bind('themeStore.theme.colors.primary');
  font-weight: bold;
}

@keyframes blink {
  0%, 50% {
    opacity: 1;
  }
  50.01%, 100% {
    opacity: 0;
  }
}

/* 帮助图标样式 */
.form-label-with-help {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.help-icon {
  color: v-bind('themeStore.theme.colors.textSecondary');
  cursor: help;
  opacity: 0.7;
  transition: opacity 0.2s;
}

.help-icon:hover {
  opacity: 1;
}
</style>
