<template>
  <div
    class="mcp-config-page"
    :style="{
      backgroundColor: themeStore.theme.colors.background
    }"
  >
    <div class="page-header">
      <h1>MCP服务器配置</h1>
      <p class="header-desc">管理和配置MCP（Model Context Protocol）服务器</p>
    </div>

    <!-- 操作栏 -->
    <div class="action-bar">
      <n-button type="primary" @click="handleCreate">
        <template #icon>
          <n-icon :component="Add" />
        </template>
        新增配置
      </n-button>
    </div>

    <!-- 数据表格 -->
    <n-data-table
      :columns="columns"
      :data="tableData"
      :loading="loading"
      :pagination="pagination"
      :scroll-x="1200"
      striped
      @update:page="handlePageChange"
      @update:page-size="handlePageSizeChange"
    />

    <!-- 编辑/新增对话框 -->
    <n-modal
      v-model:show="showDialog"
      preset="dialog"
      :title="dialogTitle"
      :mask-closable="false"
      :closable="true"
    >
      <n-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-placement="left"
        label-width="100"
      >
        <n-form-item label="服务器名称" path="name">
          <n-input
            v-model:value="formData.name"
            placeholder="输入MCP服务器名称"
          />
        </n-form-item>

        <n-form-item label="传输类型" path="type">
          <n-select
            v-model:value="formData.type"
            :options="typeOptions"
            placeholder="选择传输类型"
            @update:value="handleTypeChange"
          />
        </n-form-item>

        <!-- STDIO 模式 -->
        <template v-if="formData.type === 'stdio'">
          <n-form-item label="完整命令" path="fullCommand">
            <n-input
              v-model:value="formData.fullCommand"
              type="textarea"
              placeholder="例如: npx -y @baidumap/mcp-server-baidu-map"
              :rows="3"
            />
            <template #feedback>
              <span style="font-size: 12px; color: #999;">
                输入完整的启动命令，后端会自动分割为命令和参数
              </span>
            </template>
          </n-form-item>

          <n-form-item label="环境变量" path="env">
            <n-input
              v-model:value="formData.env"
              type="textarea"
              placeholder="例如: BAIDU_MAP_API_KEY=xxx,OTHER_KEY=value"
              :rows="2"
            />
            <template #feedback>
              <span style="font-size: 12px; color: #999;">
                多个环境变量用逗号分隔（可选）
              </span>
            </template>
          </n-form-item>
        </template>

        <!-- SSE/HTTP 模式 -->
        <template v-if="formData.type === 'sse' || formData.type === 'http'">
          <n-form-item label="服务地址" path="url">
            <n-input
              v-model:value="formData.url"
              placeholder="例如: http://localhost:8080/sse"
            />
          </n-form-item>

          <n-form-item label="环境变量" path="env">
            <n-input
              v-model:value="formData.env"
              type="textarea"
              placeholder="例如: API_KEY=xxx,TOKEN=value"
              :rows="2"
            />
            <template #feedback>
              <span style="font-size: 12px; color: #999;">
                多个环境变量用逗号分隔（可选）
              </span>
            </template>
          </n-form-item>
        </template>
      </n-form>

      <template #action>
        <n-space>
          <n-button @click="showDialog = false">取消</n-button>
          <n-button
            v-if="isEditing"
            type="error"
            @click="handleDelete"
          >
            删除
          </n-button>
          <n-button @click="handleTestConnection" :loading="testLoading">
            测试连接
          </n-button>
          <n-button type="primary" @click="handleSubmit">保存</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 工具列表对话框 -->
    <n-modal
      v-model:show="showToolsDialog"
      preset="dialog"
      title="MCP服务器工具列表"
      :mask-closable="false"
      :closable="true"
      style="width: 600px"
    >
      <div v-if="toolsList.length > 0" class="tools-list">
        <div v-for="(description, toolName) in toolsMap" :key="toolName" class="tool-item">
          <div class="tool-name">{{ toolName }}</div>
          <div class="tool-description">{{ description }}</div>
        </div>
      </div>
      <div v-else class="empty-state">
        <p>未找到任何工具</p>
      </div>

      <template #action>
        <n-button type="primary" @click="showToolsDialog = false">关闭</n-button>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, h, computed } from 'vue'
import {
  NButton,
  NIcon,
  NModal,
  NForm,
  NFormItem,
  NInput,
  NSpace,
  NDataTable,
  NTag,
  NSwitch,
  NSelect,
  useMessage,
  useDialog,
  type DataTableColumns
} from 'naive-ui'
import { Add, Trash, Edit } from '@vicons/ionicons5'
import { useThemeStore } from '@/stores/theme'
import { mcpApi, type AiMcpServerConfig, type McpServerDTO } from '@/api/mcp'

const themeStore = useThemeStore()
const message = useMessage()
const dialog = useDialog()

const loading = ref(false)
const tableData = ref<AiMcpServerConfig[]>([])
const showDialog = ref(false)
const dialogTitle = ref('')
const isEditing = ref(false)
const editingId = ref<string | null>(null)
const testLoading = ref(false)
const showToolsDialog = ref(false)
const toolsMap = ref<Record<string, string>>({})

const formRef = ref()
const formData = ref<McpServerDTO>({
  name: '',
  type: 'stdio',
  fullCommand: '',
  url: '',
  env: ''
})

const typeOptions = [
  { label: 'STDIO', value: 'stdio' },
  { label: 'SSE', value: 'sse' },
  { label: 'HTTP', value: 'http' }
]

const formRules = {
  name: { required: true, message: '请输入服务器名称', trigger: 'blur' },
  type: { required: true, message: '请选择传输类型', trigger: 'change' },
  fullCommand: {
    validator: (rule: any, value: any) => {
      if (formData.value.type === 'stdio' && !value) {
        return new Error('请输入完整命令')
      }
      return true
    },
    trigger: 'blur'
  },
  url: {
    validator: (rule: any, value: any) => {
      if ((formData.value.type === 'sse' || formData.value.type === 'http') && !value) {
        return new Error('请输入服务地址')
      }
      return true
    },
    trigger: 'blur'
  }
}

const pagination = ref({
  page: 1,
  pageSize: 10,
  pageCount: 1,
  itemCount: 0,
  prefix: (info: any) => `共 ${info.itemCount} 条`
})

const columns: DataTableColumns<AiMcpServerConfig> = [
  {
    title: '服务器名称',
    key: 'name',
    width: 150,
    ellipsis: {
      tooltip: true
    }
  },
  {
    title: '传输类型',
    key: 'type',
    width: 100,
    align: 'center',
    render: (row) => {
      const typeMap: Record<string, string> = {
        'stdio': 'STDIO',
        'sse': 'SSE',
        'http': 'HTTP'
      }
      const colorMap: Record<string, string> = {
        'stdio': 'default',
        'sse': 'info',
        'http': 'success'
      }
      return h(
        NTag,
        { type: colorMap[row.type] || 'default', size: 'small' },
        { default: () => typeMap[row.type] || row.type }
      )
    }
  },
  {
    title: '配置信息',
    key: 'config',
    width: 200,
    ellipsis: {
      tooltip: true
    },
    render: (row) => {
      if (row.type === 'stdio') {
        return row.fullCommand || '-'
      } else {
        return row.url || '-'
      }
    }
  },
  {
    title: '状态',
    key: 'isEnabled',
    width: 100,
    align: 'center',
    render: (row) => {
      return h(
        NSwitch,
        {
          value: row.isEnabled === 1,
          onUpdateValue: () => handleToggleEnabled(row.id)
        }
      )
    }
  },
  {
    title: '创建时间',
    key: 'createdAt',
    width: 180,
    render: (row) => {
      return new Date(row.createdAt).toLocaleString('zh-CN')
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 120,
    align: 'center',
    fixed: 'right',
    render: (row) => {
      return h(
        NSpace,
        { size: 'small', justify: 'center' },
        {
          default: () => [
            h(
              NButton,
              {
                text: true,
                type: 'primary',
                size: 'small',
                onClick: () => handleEdit(row)
              },
              { default: () => '编辑' }
            ),
            h(
              NButton,
              {
                text: true,
                type: 'error',
                size: 'small',
                onClick: () => handleDeleteConfirm(row)
              },
              { default: () => '删除' }
            )
          ]
        }
      )
    }
  }
]

const loadData = async () => {
  try {
    loading.value = true
    const res = await mcpApi.searchPage(pagination.value.page, pagination.value.pageSize)
    tableData.value = res.data.records || []
    pagination.value.itemCount = res.data.total || 0
    pagination.value.pageCount = res.data.pages || 1
  } catch (error) {
    message.error('加载MCP配置列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  dialogTitle.value = '新增MCP服务器配置'
  isEditing.value = false
  editingId.value = null
  formData.value = {
    name: '',
    type: 'stdio',
    fullCommand: '',
    url: '',
    env: ''
  }
  showDialog.value = true
}

const handleEdit = (row: AiMcpServerConfig) => {
  dialogTitle.value = '编辑MCP服务器配置'
  isEditing.value = true
  editingId.value = row.id
  formData.value = {
    name: row.name,
    type: row.type,
    fullCommand: row.fullCommand || '',
    url: row.url || '',
    env: row.env || ''
  }
  showDialog.value = true
}

const handleTypeChange = () => {
  // 清空其他类型的字段
  if (formData.value.type === 'stdio') {
    formData.value.url = ''
  } else {
    formData.value.fullCommand = ''
  }
}

const toolsList = computed(() => {
  return Object.keys(toolsMap.value)
})

const handleTestConnection = async () => {
  try {
    // 先验证必填字段
    if (!formData.value.name) {
      message.error('请输入服务器名称')
      return
    }
    if (!formData.value.type) {
      message.error('请选择传输类型')
      return
    }
    if (formData.value.type === 'stdio' && !formData.value.fullCommand) {
      message.error('请输入完整命令')
      return
    }
    if ((formData.value.type === 'sse' || formData.value.type === 'http') && !formData.value.url) {
      message.error('请输入服务地址')
      return
    }

    testLoading.value = true
    const res = await mcpApi.getAllTools(formData.value)
    toolsMap.value = res.data || {}

    if (Object.keys(toolsMap.value).length === 0) {
      message.warning('连接成功，但未找到任何工具')
    } else {
      message.success(`连接成功，找到 ${Object.keys(toolsMap.value).length} 个工具`)
    }

    showToolsDialog.value = true
  } catch (error: any) {
    message.error(error?.message || '连接失败，请检查配置')
    console.error(error)
  } finally {
    testLoading.value = false
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()

    if (isEditing.value && editingId.value) {
      await mcpApi.update(editingId.value, formData.value)
      message.success('配置更新成功')
    } else {
      await mcpApi.create(formData.value)
      message.success('配置添加成功')
    }

    showDialog.value = false
    pagination.value.page = 1
    await loadData()
  } catch (error: any) {
    if (error?.message) {
      message.error(error.message)
    }
  }
}

const handleDeleteConfirm = (row: AiMcpServerConfig) => {
  dialog.warning({
    title: '确认删除',
    content: `确定要删除 "${row.name}" 的配置吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await mcpApi.delete(row.id)
        message.success('删除成功')
        await loadData()
      } catch (error) {
        message.error('删除失败')
      }
    }
  })
}

const handleDelete = () => {
  if (!editingId.value) return

  const row = tableData.value.find(r => r.id === editingId.value)
  if (!row) return

  dialog.warning({
    title: '确认删除',
    content: `确定要删除 "${row.name}" 的配置吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await mcpApi.delete(editingId.value!)
        message.success('删除成功')
        showDialog.value = false
        await loadData()
      } catch (error) {
        message.error('删除失败')
      }
    }
  })
}

const handleToggleEnabled = async (id: string) => {
  try {
    await mcpApi.toggleEnabled(id)
    message.success('状态更新成功')
    await loadData()
  } catch (error) {
    message.error('状态更新失败')
  }
}

const handlePageChange = (page: number) => {
  pagination.value.page = page
  loadData()
}

const handlePageSizeChange = (pageSize: number) => {
  pagination.value.pageSize = pageSize
  pagination.value.page = 1
  loadData()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.mcp-config-page {
  padding: 24px;
  min-height: 100vh;
}

.page-header {
  margin-bottom: 24px;
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

.action-bar {
  margin-bottom: 16px;
  display: flex;
  gap: 8px;
}

.tools-list {
  max-height: 400px;
  overflow-y: auto;
}

.tool-item {
  padding: 12px;
  margin-bottom: 8px;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  background-color: #fafafa;
}

.tool-name {
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
  font-family: monospace;
  font-size: 13px;
}

.tool-description {
  font-size: 13px;
  color: #666;
  line-height: 1.5;
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: #999;
}
</style>
