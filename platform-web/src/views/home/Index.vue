<template>
  <div class="home-page">
    <div class="page-header">
      <h1>欢迎使用 JustRAG</h1>
      <p>智能知识库检索与对话平台</p>
    </div>

    <div class="sections">
      <!-- 知识库部分 -->
      <section class="section">
        <div class="section-header">
          <h2>
            <n-icon :component="LibraryOutline" :size="24" />
            <span>知识库</span>
          </h2>
          <n-button text @click="router.push('/knowledge')">
            查看全部 →
          </n-button>
        </div>

        <div v-if="loading.knowledge" class="loading">
          <n-spin />
        </div>

        <div v-else-if="knowledgeBases.length === 0" class="empty">
          <p>暂无知识库</p>
          <n-button @click="router.push('/knowledge')">创建知识库</n-button>
        </div>

        <div v-else class="card-grid">
          <div
            v-for="kb in knowledgeBases"
            :key="kb.id"
            class="knowledge-card"
            :style="{
              backgroundColor: themeStore.theme.colors.surface,
              borderColor: themeStore.theme.colors.border
            }"
            @click="router.push(`/knowledge/${kb.id}`)"
          >
            <h3>{{ kb.name }}</h3>
            <p>{{ kb.description || '暂无描述' }}</p>
            <div class="card-footer">
              <span class="time">{{ formatTime(kb.updatedAt) }}</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 聊天助理部分 -->
      <section class="section">
        <div class="section-header">
          <h2>
            <n-icon :component="ChatbubbleEllipsesOutline" :size="24" />
            <span>聊天助理</span>
          </h2>
          <n-button text @click="router.push('/chat')">
            查看全部 →
          </n-button>
        </div>

        <div v-if="loading.assistant" class="loading">
          <n-spin />
        </div>

        <div v-else-if="assistants.length === 0" class="empty">
          <p>暂无聊天助理</p>
          <n-button @click="router.push('/chat')">创建助理</n-button>
        </div>

        <div v-else class="card-grid">
          <div
            v-for="assistant in assistants"
            :key="assistant.id"
            class="assistant-card"
            :style="{
              backgroundColor: themeStore.theme.colors.surface,
              borderColor: themeStore.theme.colors.border
            }"
            @click="router.push(`/chat/${assistant.id}`)"
          >
            <div class="assistant-header">
              <n-avatar :size="48" :src="assistant.assistantAvatar || undefined" round>
                <template v-if="!assistant.assistantAvatar">
                  {{ assistant.assistantName?.charAt(0) || '?' }}
                </template>
              </n-avatar>
              <div class="assistant-title">
                <h3>{{ assistant.assistantName }}</h3>
                <span class="time">{{ formatTime(assistant.updatedTime) }}</span>
              </div>
            </div>
            <p class="assistant-desc">{{ assistant.assistantDescription || '暂无描述' }}</p>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NIcon, NSpin, NAvatar, useMessage } from 'naive-ui'
import { LibraryOutline, ChatbubbleEllipsesOutline } from '@vicons/ionicons5'
import { useThemeStore } from '@/stores/theme'
import { knowledgeBaseApi } from '@/api/knowledgeBase'
import { assistantApi } from '@/api/assistant'
import type { KnowledgeBase, ChatAssistant } from '@/types'

const router = useRouter()
const message = useMessage()
const themeStore = useThemeStore()

const loading = ref({
  knowledge: false,
  assistant: false
})

const knowledgeBases = ref<KnowledgeBase[]>([])
const assistants = ref<ChatAssistant[]>([])

// 格式化时间
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

// 加载知识库
const loadKnowledgeBases = async () => {
  try {
    loading.value.knowledge = true
    const res = await knowledgeBaseApi.getAll()
    knowledgeBases.value = res.data.slice(0, 6)
  } catch (error) {
    message.error('加载知识库失败')
  } finally {
    loading.value.knowledge = false
  }
}

// 加载聊天助理
const loadAssistants = async () => {
  try {
    loading.value.assistant = true
    const res = await assistantApi.getList()
    assistants.value = res.data.slice(0, 6)
  } catch (error) {
    message.error('加载聊天助理失败')
  } finally {
    loading.value.assistant = false
  }
}

onMounted(() => {
  loadKnowledgeBases()
  loadAssistants()
})
</script>

<style scoped>
.home-page {
  padding: 48px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 48px;
}

.page-header h1 {
  font-size: 32px;
  font-weight: 600;
  margin-bottom: 8px;
}

.page-header p {
  font-size: 16px;
  color: v-bind('themeStore.theme.colors.textSecondary');
}

.sections {
  display: flex;
  flex-direction: column;
  gap: 48px;
}

.section {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-header h2 {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 20px;
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

.empty p {
  color: v-bind('themeStore.theme.colors.textSecondary');
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.knowledge-card {
  padding: 24px;
  border-radius: 12px;
  border: 1px solid;
  cursor: pointer;
  transition: all 0.2s ease;
}

.knowledge-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.knowledge-card h3 {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
}

.knowledge-card p {
  font-size: 14px;
  color: v-bind('themeStore.theme.colors.textSecondary');
  margin-bottom: 16px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-footer .time {
  font-size: 12px;
  color: v-bind('themeStore.theme.colors.textSecondary');
}

.assistant-card {
  padding: 24px;
  border-radius: 12px;
  border: 1px solid;
  cursor: pointer;
  transition: all 0.2s ease;
}

.assistant-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.assistant-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}

.assistant-title {
  flex: 1;
  min-width: 0;
}

.assistant-title h3 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.assistant-title .time {
  font-size: 12px;
  color: v-bind('themeStore.theme.colors.textSecondary');
}

.assistant-desc {
  font-size: 14px;
  color: v-bind('themeStore.theme.colors.textSecondary');
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-clamp: 2;
}
</style>
