<template>
  <div v-if="steps && steps.length > 0" class="process-trace-panel">
    <div class="panel-header" @click="toggleCollapse">
      <div class="header-left">
        <n-icon :component="isCollapsed ? ChevronDownOutline : ChevronUpOutline" :size="16" />
        <span class="intent-label">{{ intentLabel }}</span>
        <span class="step-count">{{ steps.length }} 个步骤</span>
        <span v-if="totalElapsedMs" class="elapsed-time">{{ formatElapsed(totalElapsedMs) }}</span>
      </div>
    </div>

    <div v-if="!isCollapsed" class="panel-content">
      <div v-for="(step, index) in steps" :key="index" class="step-item">
        <div class="step-icon">
          <n-icon
            :component="getStepIcon(step)"
            :size="14"
            :style="{ color: getStepColor(step) }"
          />
        </div>
        <div class="step-content">
          <div class="step-header">
            <span class="step-label">
              <template v-if="step.type === 'TOOL_CALL_START'">
                调用工具: <span class="tool-name-blue">{{ formatToolName(step.toolName) || '未知' }}</span>
              </template>
              <template v-else-if="step.type === 'TOOL_CALL_END'">
                工具返回: <span class="tool-name-blue">{{ formatToolName(step.toolName) || '未知' }}</span>
              </template>
              <template v-else>
                {{ getStepLabel(step) }}
              </template>
            </span>
            <span v-if="step.elapsedMs" class="step-elapsed">{{ formatElapsed(step.elapsedMs) }}</span>
          </div>
          <div v-if="step.documentsCount != null" class="step-detail">
            检索到 {{ step.documentsCount }} 个相关片段
          </div>
          <div v-if="step.content && step.type === 'MODEL_INFO'" class="step-detail">
            {{ step.content }}
          </div>
          <div v-if="step.type === 'TOOL_CALL_START' && step.input" class="step-detail">
            <pre class="tool-json-input">{{ formatToolInput(step.input) }}</pre>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { NIcon } from 'naive-ui'
import {
  ChevronDownOutline,
  ChevronUpOutline,
  SearchOutline,
  BuildOutline,
  InformationCircleOutline,
  CheckmarkCircleOutline
} from '@vicons/ionicons5'
import type { ProcessStep } from '@/types'

const props = defineProps<{
  steps: ProcessStep[]
  totalElapsedMs?: number
}>()

const isCollapsed = ref(true)

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

const intentLabel = computed(() => {
  const hasRetrieve = props.steps.some(s => s.type.startsWith('RETRIEVE'))
  const hasTool = props.steps.some(s => s.type.startsWith('TOOL_CALL'))

  if (hasRetrieve && hasTool) return '知识问答 + 工具辅助'
  if (hasRetrieve) return '知识问答'
  if (hasTool) return '工具调用'
  return '自由对话'
})

const getStepIcon = (step: ProcessStep) => {
  if (step.type.startsWith('RETRIEVE')) return SearchOutline
  if (step.type.startsWith('TOOL_CALL')) return BuildOutline
  if (step.type === 'MODEL_INFO') return InformationCircleOutline
  return CheckmarkCircleOutline
}

const getStepColor = (step: ProcessStep) => {
  if (step.type.endsWith('_END')) return '#18a058'
  if (step.type.endsWith('_START')) return '#2080f0'
  if (step.type === 'MODEL_INFO') return '#f0a020'
  return '#666'
}

const getStepLabel = (step: ProcessStep) => {
  if (step.type === 'RETRIEVE_START') return '知识库检索'
  if (step.type === 'RETRIEVE_END') return '检索完成'
  if (step.type === 'TOOL_CALL_START') return `调用工具: ${formatToolName(step.toolName) || '未知'}`
  if (step.type === 'TOOL_CALL_END') return `工具返回: ${formatToolName(step.toolName) || '未知'}`
  if (step.type === 'MODEL_INFO') return '模型信息'
  if (step.type === 'THINKING') return '思考过程'
  if (step.type === 'ERROR') return '错误'
  return step.label || step.type
}

// 格式化工具名称，去掉常见前缀
const formatToolName = (name?: string) => {
  if (!name) return ''
  return name.replace(/^JavaSDKMCPClient_/, '')
}

// 格式化工具输入参数为格式化的 JSON
const formatToolInput = (input?: string): string => {
  if (!input) return ''
  try {
    const obj = JSON.parse(input)
    return JSON.stringify(obj, null, 2)
  } catch {
    return input
  }
}

const formatElapsed = (ms: number) => {
  if (ms < 1000) return `${ms}ms`
  return `${(ms / 1000).toFixed(1)}s`
}
</script>

<style scoped>
.process-trace-panel {
  background: rgba(0, 0, 0, 0.02);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 8px;
  margin-bottom: 8px;
  font-size: 13px;
}

.panel-header {
  padding: 8px 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  user-select: none;
}

.panel-header:hover {
  background: rgba(0, 0, 0, 0.02);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.intent-label {
  font-weight: 500;
  color: #333;
}

.step-count {
  color: #666;
  font-size: 12px;
}

.elapsed-time {
  color: #999;
  font-size: 12px;
}

.panel-content {
  padding: 0 12px 8px 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.step-item {
  display: flex;
  gap: 8px;
  padding: 6px 0;
  align-items: flex-start;
}

.step-icon {
  flex-shrink: 0;
  margin-top: 2px;
}

.step-content {
  flex: 1;
  min-width: 0;
}

.step-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.step-label {
  color: #333;
  font-weight: 500;
}

.step-elapsed {
  color: #999;
  font-size: 12px;
}

.step-detail {
  color: #666;
  font-size: 12px;
  margin-top: 2px;
}

.tool-name-blue {
  color: #2080f0;
  font-weight: 600;
}

.tool-json-input {
  background: rgba(0, 0, 0, 0.04);
  border-radius: 6px;
  padding: 6px 10px;
  margin: 0;
  font-family: 'Cascadia Code', 'Fira Code', 'JetBrains Mono', Consolas, monospace;
  font-size: 11px;
  line-height: 1.5;
  white-space: pre;
  overflow-x: auto;
  color: #444;
}
</style>
