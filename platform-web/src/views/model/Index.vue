<template>
  <div
    class="model-config-page"
    :style="{
      backgroundColor: themeStore.theme.colors.background
    }"
  >
    <div class="page-header">
      <h1>AI模型配置</h1>
      <p class="header-desc">配置你的聊天模型和嵌入模型</p>
    </div>

    <!-- Tab切换 -->
    <n-tabs v-model:value="activeTab" type="line" animated>
      <!-- 聊天模型Tab -->
      <n-tab-pane name="chat" tab="聊天模型">
        <div class="tab-content">
          <p class="section-desc">用于对话生成</p>
          <div class="provider-cards">
            <!-- OpenAI 卡片 -->
            <div
              class="provider-card"
              :class="getCardClass('OPENAI', 'CHAT')"
              @click="handleConfigProvider('OPENAI', 'CHAT')"
            >
              <div class="card-header">
                <div class="provider-icon openai">
                  <n-icon :component="Code" :size="32" />
                </div>
                <div class="provider-info">
                  <h3>OpenAI</h3>
                  <p class="provider-desc">GPT-4, GPT-3.5 等强大模型</p>
                </div>
              </div>
              <div class="card-status">
                <n-tag v-if="getProviderInfo('OPENAI', 'CHAT').isConfigured" type="success" size="small">
                  已配置
                </n-tag>
                <n-tag v-else type="default" size="small">未配置</n-tag>
              </div>
              <div class="card-footer">
                <n-icon :component="ChevronForward" :size="20" />
              </div>
            </div>

            <!-- Ollama 卡片 -->
            <div
              class="provider-card"
              :class="getCardClass('OLLAMA', 'CHAT')"
              @click="handleConfigProvider('OLLAMA', 'CHAT')"
            >
              <div class="card-header">
                <div class="provider-icon ollama">
                  <n-icon :component="Server" :size="32" />
                </div>
                <div class="provider-info">
                  <h3>Ollama</h3>
                  <p class="provider-desc">本地部署的开源模型</p>
                </div>
              </div>
              <div class="card-status">
                <n-tag v-if="getProviderInfo('OLLAMA', 'CHAT').isConfigured" type="success" size="small">
                  <template #icon>
                    <n-icon :component="CheckmarkCircle" />
                  </template>
                  已配置
                </n-tag>
                <n-tag v-else type="default" size="small">未配置</n-tag>
              </div>
              <div class="card-footer">
                <n-icon :component="ChevronForward" :size="20" />
              </div>
            </div>

            <!-- DashScope 卡片 -->
            <div
              class="provider-card"
              :class="getCardClass('DASHSCOPE', 'CHAT')"
              @click="handleConfigProvider('DASHSCOPE', 'CHAT')"
            >
              <div class="card-header">
                <div class="provider-icon dashscope">
                  <n-icon :component="Cloud" :size="32" />
                </div>
                <div class="provider-info">
                  <h3>阿里云通义千问</h3>
                  <p class="provider-desc">国内高性能 AI 模型</p>
                </div>
              </div>
              <div class="card-status">
                <n-tag v-if="getProviderInfo('DASHSCOPE', 'CHAT').isConfigured" type="success" size="small">
                  <template #icon>
                    <n-icon :component="CheckmarkCircle" />
                  </template>
                  已配置
                </n-tag>
                <n-tag v-else type="default" size="small">未配置</n-tag>
              </div>
              <div class="card-footer">
                <n-icon :component="ChevronForward" :size="20" />
              </div>
            </div>

            <!-- Azure OpenAI 卡片 -->
            <div
              class="provider-card"
              :class="getCardClass('AZURE_OPENAI', 'CHAT')"
              @click="handleConfigProvider('AZURE_OPENAI', 'CHAT')"
            >
              <div class="card-header">
                <div class="provider-icon azure">
                  <n-icon :component="Cloud" :size="32" />
                </div>
                <div class="provider-info">
                  <h3>Azure OpenAI</h3>
                  <p class="provider-desc">微软 Azure 上的 GPT 模型</p>
                </div>
              </div>
              <div class="card-status">
                <n-tag v-if="getProviderInfo('AZURE_OPENAI', 'CHAT').isConfigured" type="success" size="small">
                  <template #icon>
                    <n-icon :component="CheckmarkCircle" />
                  </template>
                  已配置
                </n-tag>
                <n-tag v-else type="default" size="small">未配置</n-tag>
              </div>
              <div class="card-footer">
                <n-icon :component="ChevronForward" :size="20" />
              </div>
            </div>

            <!-- Anthropic 卡片 -->
            <div
              class="provider-card"
              :class="getCardClass('ANTHROPIC', 'CHAT')"
              @click="handleConfigProvider('ANTHROPIC', 'CHAT')"
            >
              <div class="card-header">
                <div class="provider-icon anthropic">
                  <n-icon :component="Code" :size="32" />
                </div>
                <div class="provider-info">
                  <h3>Anthropic Claude</h3>
                  <p class="provider-desc">Claude 3.5 等先进模型</p>
                </div>
              </div>
              <div class="card-status">
                <n-tag v-if="getProviderInfo('ANTHROPIC', 'CHAT').isConfigured" type="success" size="small">
                  <template #icon>
                    <n-icon :component="CheckmarkCircle" />
                  </template>
                  已配置
                </n-tag>
                <n-tag v-else type="default" size="small">未配置</n-tag>
              </div>
              <div class="card-footer">
                <n-icon :component="ChevronForward" :size="20" />
              </div>
            </div>

            <!-- DeepSeek 卡片 -->
            <div
              class="provider-card"
              :class="getCardClass('DEEPSEEK', 'CHAT')"
              @click="handleConfigProvider('DEEPSEEK', 'CHAT')"
            >
              <div class="card-header">
                <div class="provider-icon deepseek">
                  <n-icon :component="Server" :size="32" />
                </div>
                <div class="provider-info">
                  <h3>DeepSeek</h3>
                  <p class="provider-desc">DeepSeek 系列模型</p>
                </div>
              </div>
              <div class="card-status">
                <n-tag v-if="getProviderInfo('DEEPSEEK', 'CHAT').isConfigured" type="success" size="small">
                  <template #icon>
                    <n-icon :component="CheckmarkCircle" />
                  </template>
                  已配置
                </n-tag>
                <n-tag v-else type="default" size="small">未配置</n-tag>
              </div>
              <div class="card-footer">
                <n-icon :component="ChevronForward" :size="20" />
              </div>
            </div>

            <!-- 智谱AI 卡片 -->
            <div
              class="provider-card"
              :class="getCardClass('ZHIPU', 'CHAT')"
              @click="handleConfigProvider('ZHIPU', 'CHAT')"
            >
              <div class="card-header">
                <div class="provider-icon zhipu">
                  <n-icon :component="Cloud" :size="32" />
                </div>
                <div class="provider-info">
                  <h3>智谱AI</h3>
                  <p class="provider-desc">GLM-4 等国产大模型</p>
                </div>
              </div>
              <div class="card-status">
                <n-tag v-if="getProviderInfo('ZHIPU', 'CHAT').isConfigured" type="success" size="small">
                  <template #icon>
                    <n-icon :component="CheckmarkCircle" />
                  </template>
                  已配置
                </n-tag>
                <n-tag v-else type="default" size="small">未配置</n-tag>
              </div>
              <div class="card-footer">
                <n-icon :component="ChevronForward" :size="20" />
              </div>
            </div>
          </div>
        </div>
      </n-tab-pane>

      <!-- 嵌入模型Tab -->
      <n-tab-pane name="embedding" tab="嵌入模型">
        <div class="tab-content">
          <p class="section-desc">用于文档向量化和语义搜索</p>
          <div class="provider-cards">
            <!-- OpenAI 嵌入模型 -->
            <div
              class="provider-card"
              :class="getCardClass('OPENAI', 'EMBEDDING')"
              @click="handleConfigProvider('OPENAI', 'EMBEDDING')"
            >
              <div class="card-header">
                <div class="provider-icon openai">
                  <n-icon :component="Code" :size="32" />
                </div>
                <div class="provider-info">
                  <h3>OpenAI Embeddings</h3>
                  <p class="provider-desc">text-embedding-3-large 等</p>
                </div>
              </div>
              <div class="card-status">
                <n-tag v-if="getProviderInfo('OPENAI', 'EMBEDDING').isConfigured" type="success" size="small">
                  <template #icon>
                    <n-icon :component="CheckmarkCircle" />
                  </template>
                  已配置
                </n-tag>
                <n-tag v-else type="default" size="small">未配置</n-tag>
              </div>
              <div class="card-footer">
                <n-icon :component="ChevronForward" :size="20" />
              </div>
            </div>

            <!-- Ollama 嵌入模型 -->
            <div
              class="provider-card"
              :class="getCardClass('OLLAMA', 'EMBEDDING')"
              @click="handleConfigProvider('OLLAMA', 'EMBEDDING')"
            >
              <div class="card-header">
                <div class="provider-icon ollama">
                  <n-icon :component="Server" :size="32" />
                </div>
                <div class="provider-info">
                  <h3>Ollama Embeddings</h3>
                  <p class="provider-desc">nomic-embed-text 等</p>
                </div>
              </div>
              <div class="card-status">
                <n-tag v-if="getProviderInfo('OLLAMA', 'EMBEDDING').isConfigured" type="success" size="small">
                  <template #icon>
                    <n-icon :component="CheckmarkCircle" />
                  </template>
                  已配置
                </n-tag>
                <n-tag v-else type="default" size="small">未配置</n-tag>
              </div>
              <div class="card-footer">
                <n-icon :component="ChevronForward" :size="20" />
              </div>
            </div>

            <!-- DashScope 嵌入模型 -->
            <div
              class="provider-card"
              :class="getCardClass('DASHSCOPE', 'EMBEDDING')"
              @click="handleConfigProvider('DASHSCOPE', 'EMBEDDING')"
            >
              <div class="card-header">
                <div class="provider-icon dashscope">
                  <n-icon :component="Cloud" :size="32" />
                </div>
                <div class="provider-info">
                  <h3>通义千问 Embeddings</h3>
                  <p class="provider-desc">text-embedding-v3 等</p>
                </div>
              </div>
              <div class="card-status">
                <n-tag v-if="getProviderInfo('DASHSCOPE', 'EMBEDDING').isConfigured" type="success" size="small">
                  <template #icon>
                    <n-icon :component="CheckmarkCircle" />
                  </template>
                  已配置
                </n-tag>
                <n-tag v-else type="default" size="small">未配置</n-tag>
              </div>
              <div class="card-footer">
                <n-icon :component="ChevronForward" :size="20" />
              </div>
            </div>

            <!-- Azure OpenAI 嵌入模型 -->
            <div
              class="provider-card"
              :class="getCardClass('AZURE_OPENAI', 'EMBEDDING')"
              @click="handleConfigProvider('AZURE_OPENAI', 'EMBEDDING')"
            >
              <div class="card-header">
                <div class="provider-icon azure">
                  <n-icon :component="Cloud" :size="32" />
                </div>
                <div class="provider-info">
                  <h3>Azure OpenAI Embeddings</h3>
                  <p class="provider-desc">text-embedding-ada-002 等</p>
                </div>
              </div>
              <div class="card-status">
                <n-tag v-if="getProviderInfo('AZURE_OPENAI', 'EMBEDDING').isConfigured" type="success" size="small">
                  <template #icon>
                    <n-icon :component="CheckmarkCircle" />
                  </template>
                  已配置
                </n-tag>
                <n-tag v-else type="default" size="small">未配置</n-tag>
              </div>
              <div class="card-footer">
                <n-icon :component="ChevronForward" :size="20" />
              </div>
            </div>

            <!-- 智谱AI 嵌入模型 -->
            <div
              class="provider-card"
              :class="getCardClass('ZHIPU', 'EMBEDDING')"
              @click="handleConfigProvider('ZHIPU', 'EMBEDDING')"
            >
              <div class="card-header">
                <div class="provider-icon zhipu">
                  <n-icon :component="Cloud" :size="32" />
                </div>
                <div class="provider-info">
                  <h3>智谱AI Embeddings</h3>
                  <p class="provider-desc">embedding-2 等</p>
                </div>
              </div>
              <div class="card-status">
                <n-tag v-if="getProviderInfo('ZHIPU', 'EMBEDDING').isConfigured" type="success" size="small">
                  <template #icon>
                    <n-icon :component="CheckmarkCircle" />
                  </template>
                  已配置
                </n-tag>
                <n-tag v-else type="default" size="small">未配置</n-tag>
              </div>
              <div class="card-footer">
                <n-icon :component="ChevronForward" :size="20" />
              </div>
            </div>
          </div>
        </div>
      </n-tab-pane>
    </n-tabs>

    <!-- 配置对话框 -->
    <n-modal v-model:show="showDialog" preset="dialog" :title="dialogTitle">
      <n-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-placement="left"
        label-width="120"
      >

        <n-form-item label="模型名称" path="modelName">
          <n-input
            v-model:value="formData.modelName"
            :placeholder="getModelNamePlaceholder(formData.provider)"
          />
        </n-form-item>

        <n-form-item
          v-if="formData.provider !== 'OLLAMA'"
          label="API Key"
          path="apiKey"
        >
          <n-input
            v-model:value="formData.apiKey"
            type="password"
            show-password-on="click"
            :placeholder="isEditing && formData.apiKey ? '保持不变' : '输入API密钥'"
          />
          <template #feedback>
            <span v-if="isEditing && formData.apiKey && formData.apiKey.includes('****')" style="font-size: 12px; color: #999;">
              已设置API Key，留空或不修改将保留原值
            </span>
          </template>
        </n-form-item>

        <n-form-item label="API端点" path="apiEndpoint">
          <n-input
            v-model:value="formData.apiEndpoint"
            :placeholder="getEndpointPlaceholder(formData.provider)"
          />
        </n-form-item>

      </n-form>

      <template #action>
        <n-space>
          <n-button @click="showDialog = false">取消</n-button>
          <n-button
            v-if="isEditing"
            type="error"
            @click="handleDelete"
          >
            删除配置
          </n-button>
          <n-button type="primary" @click="handleSubmit">保存</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  NButton,
  NIcon,
  NModal,
  NForm,
  NFormItem,
  NInput,
  NSpace,
  NTag,
  NTabs,
  NTabPane,
  useMessage,
  useDialog
} from 'naive-ui'
import {
  CheckmarkCircle,
  ChevronForward,
  Server,
  Cloud,
  Code
} from '@vicons/ionicons5'
import { useThemeStore } from '@/stores/theme'
import { modelApi, type AiModelConfig, type CreateAiModelConfigDTO } from '@/api/model'

const themeStore = useThemeStore()
const message = useMessage()
const dialog = useDialog()

const loading = ref(false)
const models = ref<AiModelConfig[]>([])
const showDialog = ref(false)
const dialogTitle = ref('')
const isEditing = ref(false)
const editingId = ref<string | null>(null)
const activeTab = ref('chat')

const formRef = ref()
const formData = ref<CreateAiModelConfigDTO>({
  modelName: '',
  modelType: 'CHAT',  // 默认为聊天模型
  provider: '',
  apiKey: '',
  apiEndpoint: '',
  description: ''
})

const formRules = {
  modelName: { required: true, message: '请输入模型名称', trigger: 'blur' }
}

// 获取提供商的配置信息（是否已配置）
const getProviderInfo = (provider: string, modelType: string) => {
  const model = models.value.find(m => m.provider === provider && m.modelType === modelType)
  return {
    isConfigured: !!model
  }
}

// 获取卡片的状态class
const getCardClass = (provider: string, modelType: string) => {
  const info = getProviderInfo(provider, modelType)
  return info.isConfigured ? 'card-configured' : ''
}

// 获取占位符文本
const getModelNamePlaceholder = (provider: string) => {
  switch (provider) {
    case 'OPENAI': return '如: gpt-4-turbo'
    case 'AZURE_OPENAI': return '如: gpt-4-turbo'
    case 'OLLAMA': return '如: llama3, qwen2'
    case 'DASHSCOPE': return '如: qwen-max, qwen-plus'
    case 'ANTHROPIC': return '如: claude-3-5-sonnet'
    case 'DEEPSEEK': return '如: deepseek-chat'
    case 'ZHIPU': return '如: glm-4, glm-4-flash'
    default: return '输入模型名称'
  }
}

const getEndpointPlaceholder = (provider: string) => {
  switch (provider) {
    case 'OPENAI': return 'https://api.openai.com/v1'
    case 'AZURE_OPENAI': return 'https://{your-resource}.openai.azure.com'
    case 'OLLAMA': return 'http://localhost:11434'
    case 'DASHSCOPE': return 'https://dashscope.aliyuncs.com'
    case 'ANTHROPIC': return 'https://api.anthropic.com'
    case 'DEEPSEEK': return 'https://api.deepseek.com'
    case 'ZHIPU': return 'https://open.bigmodel.cn/api/paas/v4'
    default: return '输入API端点'
  }
}

const loadModels = async () => {
  try {
    loading.value = true
    const res = await modelApi.getAllModels()
    models.value = res.data
  } catch (error) {
    message.error('加载模型列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleConfigProvider = (provider: string, modelType: string) => {
  const existingModel = models.value.find(m => m.provider === provider && m.modelType === modelType)

  const providerNames = {
    'OPENAI': 'OpenAI',
    'AZURE_OPENAI': 'Azure OpenAI',
    'OLLAMA': 'Ollama',
    'DASHSCOPE': '阿里云通义千问',
    'ANTHROPIC': 'Anthropic Claude',
    'DEEPSEEK': 'DeepSeek',
    'ZHIPU': '智谱AI'
  }

  const modelTypeNames = {
    'CHAT': '聊天模型',
    'EMBEDDING': '嵌入模型'
  }

  if (existingModel) {
    // 编辑现有配置
    dialogTitle.value = `配置 ${providerNames[provider as keyof typeof providerNames]} - ${modelTypeNames[modelType as keyof typeof modelTypeNames]}`
    isEditing.value = true
    editingId.value = existingModel.id
    formData.value = {
      modelName: existingModel.modelName,
      modelType: existingModel.modelType,
      provider: existingModel.provider,
      apiKey: existingModel.apiKey || '',  // 显示原API Key（如果后端返回）
      apiEndpoint: existingModel.apiEndpoint || '',  // 只显示实际值，没有则为空
    }
  } else {
    // 新增配置
    dialogTitle.value = `配置 ${providerNames[provider as keyof typeof providerNames]} - ${modelTypeNames[modelType as keyof typeof modelTypeNames]}`
    isEditing.value = false
    editingId.value = null
    formData.value = {
      modelName: '',
      modelType: modelType,
      provider: provider,
      apiKey: '',
      apiEndpoint: '',  // 不填充默认值，让placeholder显示
    }
  }

  showDialog.value = true
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()

    if (isEditing.value && editingId.value) {
      await modelApi.updateModel(editingId.value, formData.value)
      message.success('配置更新成功')
    } else {
      await modelApi.createModel(formData.value)
      message.success('配置添加成功')
    }

    showDialog.value = false
    await loadModels()
  } catch (error: any) {
    if (error?.message) {
      message.error(error.message)
    }
  }
}

const handleDelete = () => {
  if (!editingId.value) return

  const model = models.value.find(m => m.id === editingId.value)
  if (!model) return

  dialog.warning({
    title: '确认删除',
    content: `确定要删除 "${model.modelName}" 的配置吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await modelApi.deleteModel(editingId.value!)
        message.success('删除成功')
        showDialog.value = false
        await loadModels()
      } catch (error) {
        message.error('删除失败')
      }
    }
  })
}

onMounted(() => {
  loadModels()
})
</script>

<style scoped>
.model-config-page {
  padding: 24px;
  min-height: 100vh;
}

.page-header {
  margin-bottom: 32px;
}

.page-header h1 {
  font-size: 28px;
  font-weight: 600;
  margin: 0 0 8px 0;
}

.header-desc {
  font-size: 14px;
  color: #666;
  margin: 0;
}

.tab-content {
  padding-top: 16px;
}

.section-desc {
  font-size: 13px;
  color: #999;
  margin: 0 0 16px 0;
}

.provider-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
  max-width: 1200px;
}

.provider-card {
  background: white;
  border: 2px solid #e8e8e8;
  border-radius: 12px;
  padding: 24px;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.provider-card:hover {
  border-color: #18a058;
  box-shadow: 0 4px 12px rgba(24, 160, 88, 0.15);
  transform: translateY(-2px);
}

/* 已配置状态 */
.provider-card.card-configured {
  border-color: #18a058;
  background: linear-gradient(to bottom right, #ffffff 0%, #f0fdf4 100%);
}

.provider-card.card-configured:hover {
  border-color: #0f7c44;
  box-shadow: 0 6px 16px rgba(24, 160, 88, 0.25);
}

.card-header {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}

.provider-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.provider-icon.openai {
  background: linear-gradient(135deg, #10a37f 0%, #1a7f64 100%);
  color: white;
}

.provider-icon.ollama {
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
  color: white;
}

.provider-icon.dashscope {
  background: linear-gradient(135deg, #ff6b35 0%, #f7931e 100%);
  color: white;
}

.provider-icon.azure {
  background: linear-gradient(135deg, #0078d4 0%, #005a9e 100%);
  color: white;
}

.provider-icon.anthropic {
  background: linear-gradient(135deg, #d97757 0%, #c9644a 100%);
  color: white;
}

.provider-icon.deepseek {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  color: white;
}

.provider-icon.zhipu {
  background: linear-gradient(135deg, #4a90e2 0%, #357abd 100%);
  color: white;
}

.provider-info {
  flex: 1;
}

.provider-info h3 {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 4px 0;
  color: #333;
}

.provider-desc {
  font-size: 13px;
  color: #999;
  margin: 0;
}

.card-status {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.card-footer {
  display: flex;
  justify-content: flex-end;
  color: #999;
}
</style>
