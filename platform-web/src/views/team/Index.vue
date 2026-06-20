<template>
  <div class="team-container">
    <div class="team-header">
      <h1>团队管理</h1>
    </div>

    <div class="team-content">
      <!-- 我的团队成员 -->
      <div class="team-section" :style="{ backgroundColor: themeStore.theme.colors.surface, borderColor: themeStore.theme.colors.border }">
        <div class="section-header">
          <h2>我的团队成员</h2>
          <n-button type="primary" size="small" @click="showInviteModal = true">
            <template #icon>
              <n-icon :component="PersonAddOutline" />
            </template>
            邀请成员
          </n-button>
        </div>

        <n-spin :show="loadingTeamUsers">
          <div v-if="teamUsers.length > 0" class="member-list">
            <div
              v-for="user in teamUsers"
              :key="user.teamId"
              class="member-item"
              :style="{ borderColor: themeStore.theme.colors.border }"
            >
              <div class="member-info">
                <n-avatar :size="40" round>
                  {{ user.username?.charAt(0) || '?' }}
                </n-avatar>
                <div class="member-detail">
                  <span class="member-name">{{ user.username }}</span>
                  <span class="member-email" :style="{ color: themeStore.theme.colors.textSecondary }">{{ user.email || '-' }}</span>
                </div>
              </div>
              <div class="member-actions">
                <n-tag :type="user.status === 1 ? 'success' : 'warning'" size="small">
                  {{ user.status === 1 ? '已加入' : '待确认' }}
                </n-tag>
                <n-button text type="error" size="small" @click="handleRemoveTeamUser(user)">
                  <template #icon>
                    <n-icon :component="TrashOutline" />
                  </template>
                </n-button>
              </div>
            </div>
          </div>
          <n-empty v-else description="暂无团队成员" />
        </n-spin>
      </div>

      <!-- 已加入的团队 -->
      <div class="team-section" :style="{ backgroundColor: themeStore.theme.colors.surface, borderColor: themeStore.theme.colors.border }">
        <div class="section-header">
          <h2>已加入的团队</h2>
        </div>

        <n-spin :show="loadingJoinTeams">
          <div v-if="joinTeams.length > 0" class="member-list">
            <div
              v-for="team in joinTeams"
              :key="team.teamId"
              class="member-item"
              :style="{ borderColor: themeStore.theme.colors.border }"
            >
              <div class="member-info">
                <n-avatar :size="40" round>
                  {{ team.username?.charAt(0) || '?' }}
                </n-avatar>
                <div class="member-detail">
                  <span class="member-name">{{ team.username }} 的团队</span>
                  <span class="member-email" :style="{ color: themeStore.theme.colors.textSecondary }">加入时间: {{ formatDate(team.createAt) }}</span>
                </div>
              </div>
              <div class="member-actions">
                <n-button text type="error" size="small" @click="handleLeaveTeam(team)">
                  <template #icon>
                    <n-icon :component="ExitOutline" />
                  </template>
                  退出
                </n-button>
              </div>
            </div>
          </div>
          <n-empty v-else description="暂未加入任何团队" />
        </n-spin>
      </div>
    </div>

    <!-- 邀请成员弹窗 -->
    <n-modal v-model:show="showInviteModal" preset="dialog" title="邀请成员">
      <n-form ref="inviteFormRef" :model="inviteForm" :rules="inviteRules">
        <n-form-item label="用户名" path="userName">
          <n-input
            v-model:value="inviteForm.userName"
            placeholder="请输入要邀请的用户名"
          />
        </n-form-item>
      </n-form>
      <template #action>
        <n-button @click="showInviteModal = false">取消</n-button>
        <n-button type="primary" :loading="inviting" @click="handleInvite">确认邀请</n-button>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  NButton,
  NIcon,
  NAvatar,
  NTag,
  NEmpty,
  NSpin,
  NModal,
  NForm,
  NFormItem,
  NInput,
  useMessage,
  useDialog,
  type FormInst,
  type FormRules
} from 'naive-ui'
import { PersonAddOutline, TrashOutline, ExitOutline } from '@vicons/ionicons5'
import { useThemeStore } from '@/stores/theme'
import { teamApi } from '@/api/team'
import type { SysUserTeamVo } from '@/types'

const themeStore = useThemeStore()
const message = useMessage()
const dialog = useDialog()

const loadingTeamUsers = ref(false)
const loadingJoinTeams = ref(false)
const teamUsers = ref<SysUserTeamVo[]>([])
const joinTeams = ref<SysUserTeamVo[]>([])

const showInviteModal = ref(false)
const inviteFormRef = ref<FormInst | null>(null)
const inviting = ref(false)
const inviteForm = reactive({
  userName: ''
})

const inviteRules: FormRules = {
  userName: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ]
}

function formatDate(dateStr: string) {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

async function loadTeamUsers() {
  loadingTeamUsers.value = true
  try {
    const res = await teamApi.listTeamUser()
    if (res.code === 200) {
      teamUsers.value = res.data || []
    }
  } catch (error) {
    console.error('加载团队成员失败', error)
  } finally {
    loadingTeamUsers.value = false
  }
}

async function loadJoinTeams() {
  loadingJoinTeams.value = true
  try {
    const res = await teamApi.listJoinTeam()
    if (res.code === 200) {
      joinTeams.value = res.data || []
    }
  } catch (error) {
    console.error('加载已加入团队失败', error)
  } finally {
    loadingJoinTeams.value = false
  }
}

async function handleInvite() {
  try {
    await inviteFormRef.value?.validate()
  } catch {
    return
  }

  inviting.value = true
  try {
    const res = await teamApi.inviteTeamUser(inviteForm.userName)
    if (res.code === 200) {
      message.success('邀请成功')
      showInviteModal.value = false
      inviteForm.userName = ''
      loadTeamUsers()
    } else {
      message.error(res.message || '邀请失败')
    }
  } catch (error: any) {
    message.error(error.message || '邀请失败')
  } finally {
    inviting.value = false
  }
}

function handleRemoveTeamUser(user: SysUserTeamVo) {
  dialog.warning({
    title: '确认移除',
    content: `确定要将 ${user.username} 从团队中移除吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await teamApi.deleteTeamUser(user.userId)
        if (res.code === 200) {
          message.success('移除成功')
          loadTeamUsers()
        } else {
          message.error(res.message || '移除失败')
        }
      } catch (error: any) {
        message.error(error.message || '移除失败')
      }
    }
  })
}

function handleLeaveTeam(team: SysUserTeamVo) {
  dialog.warning({
    title: '确认退出',
    content: `确定要退出 ${team.username} 的团队吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await teamApi.deleteJoinTeam(team.teamId)
        if (res.code === 200 && res.data) {
          message.success('退出成功')
          loadJoinTeams()
        } else if (res.code === 200 && !res.data) {
          message.warning('不允许退出默认团队')
        } else {
          message.error(res.message || '退出失败')
        }
      } catch (error: any) {
        message.error(error.message || '退出失败')
      }
    }
  })
}

onMounted(() => {
  loadTeamUsers()
  loadJoinTeams()
})
</script>

<style scoped>
.team-container {
  padding: 24px;
  max-width: 900px;
  margin: 0 auto;
}

.team-header h1 {
  margin: 0 0 24px 0;
  font-size: 24px;
  font-weight: 600;
}

.team-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.team-section {
  padding: 24px;
  border-radius: 12px;
  border: 1px solid;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.member-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.member-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid;
}

.member-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.member-detail {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.member-name {
  font-size: 14px;
  font-weight: 500;
}

.member-email {
  font-size: 12px;
}

.member-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
