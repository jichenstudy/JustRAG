<template>
  <div class="citation-panel" v-if="visible" :style="{ width: panelWidth + 'px' }">
    <!-- 拖拽调节宽度的手柄 -->
    <div 
      class="resize-handle" 
      @mousedown="startResize"
      :class="{ active: isResizing }"
    />
    
    <div class="citation-panel-header">
      <h3>引用来源</h3>
      <button class="close-btn" @click="handleClose">
        <n-icon :component="CloseOutline" :size="20" />
      </button>
    </div>
    <div class="citation-panel-content">
      <div v-if="citations.length === 0" class="empty-citations">
        暂无引用来源
      </div>
      <div v-else class="citation-list">
        <div v-for="citation in citations" :key="citation.index" class="citation-item">
          <div class="citation-header" @click="toggleCitation(citation)">
            <span class="citation-index">[{{ citation.index }}]</span>
            <div class="citation-title-wrapper">
              <span class="citation-title">{{ citation.docName || '未命名文档' }}</span>
              <span v-if="citationDetails[citation.chunkId]?.sectionPath" class="citation-section">
                {{ citationDetails[citation.chunkId]?.sectionPath }}
              </span>
            </div>
            <span class="expand-icon" :class="{ expanded: expandedCitations[citation.index] }">
              <n-icon :component="ChevronDownOutline" :size="16" />
            </span>
          </div>
          <div v-if="expandedCitations[citation.index]" class="citation-content">
            <div v-if="loadingCitations[citation.index]" class="loading-content">
              <n-spin size="small" />
              <span>加载中...</span>
            </div>
            <div v-else-if="citationDetails[citation.chunkId]" class="citation-detail">
              <div class="detail-meta">
                <span class="meta-item">
                  <n-icon :component="DocumentOutline" :size="14" />
                  分片 #{{ citationDetails[citation.chunkId]?.chunkIndex ?? 0 }}
                </span>
                <span v-if="citation.score" class="meta-item">
                  <n-icon :component="PulseOutline" :size="14" />
                  相似度 {{ (citation.score * 100).toFixed(1) }}%
                </span>
              </div>
              <div class="detail-content">
                <MarkdownRenderer :content="citationDetails[citation.chunkId]?.content || ''" />
              </div>
            </div>
            <div v-else class="citation-preview">
              {{ citation.preview || '暂无内容' }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { NIcon, NSpin } from 'naive-ui'
import { CloseOutline, ChevronDownOutline, DocumentOutline, PulseOutline } from '@vicons/ionicons5'
import MarkdownRenderer from './MarkdownRenderer.vue'
import { getCitationDetails, type CitationChunkDTO } from '@/api/citation'

const props = defineProps<{
  visible: boolean
  citations: any[]
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

// 面板宽度相关
const panelWidth = ref(420)
const isResizing = ref(false)
const MIN_WIDTH = 320
const MAX_WIDTH_RATIO = 0.5 // 最大占聊天区域一半

const startResize = (e: MouseEvent) => {
  isResizing.value = true
  e.preventDefault()
}

const handleMouseMove = (e: MouseEvent) => {
  if (!isResizing.value) return
  
  // 计算新宽度（从右边界向左计算）
  const windowWidth = window.innerWidth
  const newWidth = windowWidth - e.clientX
  
  // 限制范围
  const maxWidth = windowWidth * MAX_WIDTH_RATIO
  panelWidth.value = Math.max(MIN_WIDTH, Math.min(newWidth, maxWidth))
}

const handleMouseUp = () => {
  isResizing.value = false
}

onMounted(() => {
  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('mouseup', handleMouseUp)
})

onUnmounted(() => {
  document.removeEventListener('mousemove', handleMouseMove)
  document.removeEventListener('mouseup', handleMouseUp)
})

const expandedCitations = ref<Record<number, boolean>>({})
const loadingCitations = ref<Record<number, boolean>>({})
const citationDetails = ref<Record<string, CitationChunkDTO>>({})

// 监听 visible 变化，当面板打开时批量加载所有引用详情
watch(() => props.visible, async (newVisible) => {
  if (newVisible && props.citations.length > 0) {
    await loadAllCitationDetails()
  }
})

const handleClose = () => {
  emit('close')
}

const toggleCitation = (citation: any) => {
  const index = citation.index
  
  if (expandedCitations.value[index]) {
    expandedCitations.value = { ...expandedCitations.value, [index]: false }
  } else {
    expandedCitations.value = { ...expandedCitations.value, [index]: true }
  }
}

const loadAllCitationDetails = async () => {
  // 收集所有需要加载的 chunkId（去重，保持字符串类型避免精度丢失）
  const chunkIds = [...new Set(
    props.citations
      .filter(c => c.chunkId && !citationDetails.value[c.chunkId])
      .map(c => String(c.chunkId))
  )]
  
  if (chunkIds.length === 0) {
    return
  }
  
  // 设置加载状态
  const loadingState: Record<number, boolean> = {}
  props.citations.forEach(c => {
    loadingState[c.index] = true
  })
  loadingCitations.value = loadingState
  
  try {
    const response = await getCitationDetails(chunkIds)
    if (response.data && Array.isArray(response.data)) {
      const details: Record<string, CitationChunkDTO> = { ...citationDetails.value }
      response.data.forEach((chunk: CitationChunkDTO) => {
        details[String(chunk.id)] = chunk
      })
      citationDetails.value = details
    }
  } catch (error) {
    console.error('批量加载引用详情失败:', error)
  } finally {
    loadingCitations.value = {}
  }
}
</script>

<style scoped>
.citation-panel {
  position: relative;
  height: 100vh;
  background: #ffffff;
  border-left: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  transition: width 0.1s ease;
}

/* 拖拽调节宽度手柄 */
.resize-handle {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: transparent;
  cursor: col-resize;
  z-index: 10;
  transition: background 0.2s;
}

.resize-handle:hover,
.resize-handle.active {
  background: #1890ff;
}

.citation-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e8e8e8;
  background: #fafafa;
}

.citation-panel-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: background 0.2s;
  color: #666;
}

.close-btn:hover {
  background: #f0f0f0;
  color: #333;
}

.citation-panel-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
}

.empty-citations {
  text-align: center;
  color: #999;
  padding: 40px 0;
  font-size: 14px;
}

.citation-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.citation-item {
  background: #f9f9f9;
  border-radius: 8px;
  border: 1px solid #e8e8e8;
  overflow: hidden;
  transition: box-shadow 0.2s, border-color 0.2s;
}

.citation-item:hover {
  border-color: #d0d0d0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.citation-header {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.citation-header:hover {
  background: #f0f0f0;
}

.citation-index {
  font-weight: 600;
  color: #1890ff;
  font-size: 14px;
  flex-shrink: 0;
  line-height: 1.5;
}

.citation-title-wrapper {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.citation-title {
  font-weight: 500;
  color: #333;
  font-size: 14px;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.citation-section {
  font-size: 12px;
  color: #888;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.expand-icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  transition: transform 0.2s;
  margin-top: 2px;
}

.expand-icon.expanded {
  transform: rotate(180deg);
}

.citation-content {
  border-top: 1px solid #e8e8e8;
  background: #fff;
}

.loading-content {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px;
  color: #999;
  font-size: 13px;
  justify-content: center;
}

.citation-detail {
  padding: 12px 14px;
}

.detail-meta {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0f0f0;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #888;
}

.detail-content {
  max-height: 400px;
  overflow-y: auto;
  font-size: 13px;
  line-height: 1.6;
  color: #444;
}

.citation-preview {
  padding: 12px 14px;
  color: #666;
  font-size: 13px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
