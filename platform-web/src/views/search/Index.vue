<template>
  <div class="search-page">
    <div class="search-container">
      <div class="search-header">
        <h1>智能搜索</h1>
        <p>在知识库中搜索相关内容</p>
      </div>

      <div class="search-box">
        <n-input
          v-model:value="searchQuery"
          size="large"
          placeholder="输入关键词搜索..."
          clearable
          @keydown.enter="handleSearch"
        >
          <template #prefix>
            <n-icon :component="SearchOutline" :size="20" />
          </template>
        </n-input>
        <n-button type="primary" size="large" :loading="searching" @click="handleSearch">
          搜索
        </n-button>
      </div>

      <div class="search-options">
        <n-select
          v-model:value="selectedKnowledgeBase"
          :options="knowledgeBaseOptions"
          placeholder="选择知识库(必选)"
          style="width: 300px"
        />
        <n-input-number
          v-model:value="topK"
          :min="1"
          :max="50"
          placeholder="结果数量"
          style="width: 150px"
        >
          <template #prefix> Top </template>
        </n-input-number>
      </div>

      <div v-if="searching" class="loading">
        <n-spin size="large" />
        <p>搜索中...</p>
      </div>

      <div v-else-if="searchResults.length > 0" class="results">
        <div class="results-header">
          <h3>搜索结果 ({{ searchResults.length }})</h3>
        </div>

        <div class="results-list">
          <div
            v-for="(result, index) in searchResults"
            :key="index"
            class="result-item"
            :style="{
              backgroundColor: themeStore.theme.colors.surface,
              borderColor: themeStore.theme.colors.border
            }"
          >
            <div class="result-header">
              <n-tag :bordered="false" size="small">分块 #{{ (result.metadata?.chunkIndex ?? 0) + 1 }}</n-tag>
              <span class="score">相似度: {{ (result.score * 100).toFixed(1) }}%</span>
            </div>
            <div class="result-content">
              {{ result.content }}
            </div>
            <div class="result-meta">
              <span>文档ID: {{ result.metadata?.documentId }}</span>
              <span>分块索引: {{ result.metadata?.chunkIndex }}</span>
            </div>
          </div>
        </div>
      </div>

      <div v-else-if="hasSearched" class="empty-result">
        <n-empty description="未找到相关结果" />
      </div>

      <div v-else class="search-tips">
        <h3>搜索提示</h3>
        <ul>
          <li>输入关键词进行语义搜索</li>
          <li>可以选择特定知识库进行搜索</li>
          <li>调整 Top K 值来控制返回结果数量</li>
          <li>系统会自动匹配最相关的文档片段</li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  NInput,
  NButton,
  NIcon,
  NSpin,
  NEmpty,
  NTag,
  NSelect,
  NInputNumber,
  useMessage
} from 'naive-ui'
import { SearchOutline } from '@vicons/ionicons5'
import { useThemeStore } from '@/stores/theme'
import { knowledgeBaseApi } from '@/api/knowledgeBase'
import { documentApi } from '@/api/document'
import type { KnowledgeBase, SearchResult } from '@/types'

const message = useMessage()
const themeStore = useThemeStore()

const searchQuery = ref('')
const selectedKnowledgeBase = ref<string | null>(null)
const topK = ref(10)
const searching = ref(false)
const hasSearched = ref(false)

const knowledgeBases = ref<KnowledgeBase[]>([])
const knowledgeBaseOptions = ref<{ label: string; value: string }[]>([])

const searchResults = ref<SearchResult[]>([])

const loadKnowledgeBases = async () => {
  try {
    const res = await knowledgeBaseApi.getAll()
    knowledgeBases.value = res.data
    knowledgeBaseOptions.value = res.data.map(kb => ({
      label: kb.name,
      value: kb.id
    }))
  } catch (error) {
    console.error('加载知识库失败')
  }
}

const handleSearch = async () => {
  if (!searchQuery.value.trim()) {
    message.warning('请输入搜索关键词')
    return
  }

  if (!selectedKnowledgeBase.value) {
    message.warning('请选择知识库')
    return
  }

  try {
    searching.value = true
    hasSearched.value = true

    const res = await documentApi.searchSimilar(
      selectedKnowledgeBase.value,
      searchQuery.value.trim(),
      topK.value
    )

    if (res.code === 200) {
      searchResults.value = res.data || []
      if (searchResults.value.length === 0) {
        message.info('未找到相关结果')
      }
    } else {
      message.error(res.message || '搜索失败')
      searchResults.value = []
    }
  } catch (error) {
    message.error('搜索失败')
    searchResults.value = []
  } finally {
    searching.value = false
  }
}

onMounted(() => {
  loadKnowledgeBases()
})
</script>

<style scoped>
.search-page {
  min-height: 100%;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 48px 24px;
}

.search-container {
  width: 100%;
  max-width: 900px;
}

.search-header {
  text-align: center;
  margin-bottom: 48px;
}

.search-header h1 {
  font-size: 36px;
  font-weight: 600;
  margin-bottom: 12px;
}

.search-header p {
  font-size: 16px;
  color: v-bind('themeStore.theme.colors.textSecondary');
}

.search-box {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.search-box :deep(.n-input) {
  flex: 1;
}

.search-options {
  display: flex;
  gap: 12px;
  margin-bottom: 32px;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px;
  gap: 16px;
}

.results {
  margin-top: 32px;
}

.results-header {
  margin-bottom: 20px;
}

.results-header h3 {
  font-size: 20px;
  font-weight: 600;
}

.results-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.result-item {
  padding: 20px;
  border-radius: 12px;
  border: 1px solid;
  transition: all 0.2s ease;
}

.result-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.score {
  font-size: 12px;
  color: v-bind('themeStore.theme.colors.textSecondary');
  font-weight: 500;
}

.result-content {
  font-size: 15px;
  line-height: 1.7;
  margin-bottom: 12px;
  white-space: pre-wrap;
}

.result-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: v-bind('themeStore.theme.colors.textSecondary');
  padding-top: 12px;
  border-top: 1px solid v-bind('themeStore.theme.colors.border');
}

.empty-result {
  display: flex;
  justify-content: center;
  padding: 64px;
}

.search-tips {
  margin-top: 48px;
  padding: 32px;
  background-color: v-bind('themeStore.theme.colors.surface');
  border-radius: 12px;
}

.search-tips h3 {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 16px;
}

.search-tips ul {
  list-style: none;
  padding: 0;
}

.search-tips li {
  padding: 8px 0;
  padding-left: 24px;
  position: relative;
  color: v-bind('themeStore.theme.colors.textSecondary');
}

.search-tips li::before {
  content: '•';
  position: absolute;
  left: 8px;
  color: v-bind('themeStore.theme.colors.text');
}
</style>
