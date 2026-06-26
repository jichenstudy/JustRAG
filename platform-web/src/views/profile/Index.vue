<template>
  <div class="profile-container">
    <div class="profile-header">
      <h1>个人信息</h1>
    </div>

    <div class="profile-content" v-if="user">
      <div class="profile-card" :style="{ backgroundColor: themeStore.theme.colors.surface, borderColor: themeStore.theme.colors.border }">
        <div class="avatar-section">
          <n-avatar
            :size="80"
            :src="user.avatar || undefined"
            :fallback-src="defaultAvatar"
          >
            <template v-if="!user.avatar">
              {{ user.nickname?.charAt(0) || user.username?.charAt(0) }}
            </template>
          </n-avatar>
        </div>

        <!-- 查看模式 -->
        <div v-if="!editing" class="info-section">
          <div class="info-item">
            <span class="label">用户名</span>
            <span class="value">{{ user.username }}</span>
          </div>
          <div class="info-item">
            <span class="label">昵称</span>
            <span class="value">{{ user.nickname || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="label">性别</span>
            <span class="value">{{ sexText }}</span>
          </div>
          <div class="info-item">
            <span class="label">个性签名</span>
            <span class="value">{{ user.signature || '-' }}</span>
          </div>
          <div v-if="user.email" class="info-item">
            <span class="label">邮箱</span>
            <span class="value">{{ user.email }}</span>
          </div>
          <div v-if="user.mobile" class="info-item">
            <span class="label">手机号</span>
            <span class="value">{{ user.mobile }}</span>
          </div>
        </div>

        <!-- 编辑模式 -->
        <div v-else class="edit-section">
          <n-form label-placement="left" label-width="80">
            <n-form-item label="用户名">
              <n-input :value="user.username" disabled />
            </n-form-item>
            <n-form-item label="邮箱">
              <n-input v-if="!user.email" v-model:value="form.email" placeholder="请输入邮箱" />
              <n-input v-else :value="user.email" disabled />
            </n-form-item>
            <n-form-item label="手机号">
              <n-input v-if="!user.mobile" v-model:value="form.mobile" placeholder="请输入手机号" />
              <n-input v-else :value="user.mobile" disabled />
            </n-form-item>
            <n-form-item label="昵称">
              <n-input v-model:value="form.nickname" placeholder="请输入昵称" />
            </n-form-item>
            <n-form-item label="头像URL">
              <n-input v-model:value="form.avatar" placeholder="输入头像图片URL" />
            </n-form-item>
            <n-form-item label="性别">
              <n-radio-group v-model:value="form.sex">
                <n-radio :value="0">未知</n-radio>
                <n-radio :value="1">男</n-radio>
                <n-radio :value="2">女</n-radio>
              </n-radio-group>
            </n-form-item>
            <n-form-item label="个性签名">
              <n-input v-model:value="form.signature" type="textarea" placeholder="写一句话介绍自己" :rows="2" />
            </n-form-item>
          </n-form>

          <div class="edit-actions">
            <n-button @click="cancelEdit" :disabled="saving">取消</n-button>
            <n-button type="primary" @click="saveProfile" :loading="saving">保存</n-button>
          </div>
        </div>

        <div class="action-section" v-if="!editing">
          <n-button type="primary" @click="openChangePasswordModal">
            <template #icon>
              <n-icon :component="KeyOutline" />
            </template>
            修改密码
          </n-button>
          <n-button type="primary" @click="startEdit">
            <template #icon>
              <n-icon :component="CreateOutline" />
            </template>
            编辑资料
          </n-button>
          <n-button type="error" @click="handleLogout">
            <template #icon>
              <n-icon :component="LogOutOutline" />
            </template>
            退出登录
          </n-button>
        </div>
      </div>
    </div>

    <div class="profile-empty" v-else>
      <n-empty description="未登录" />
    </div>

    <!-- 修改密码弹窗 -->
    <n-modal v-model:show="showPasswordModal" preset="dialog" title="修改密码" :mask-closable="false">
      <n-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-placement="left" label-width="80">
        <n-form-item label="旧密码" path="oldPassword">
          <n-input v-model:value="passwordForm.oldPassword" type="password" show-password-on="click" placeholder="请输入旧密码" />
        </n-form-item>
        <n-form-item label="新密码" path="newPassword">
          <n-input v-model:value="passwordForm.newPassword" type="password" show-password-on="click" placeholder="请输入新密码" />
        </n-form-item>
        <n-form-item label="确认密码" path="confirmPassword">
          <n-input v-model:value="passwordForm.confirmPassword" type="password" show-password-on="click" placeholder="请再次输入新密码" />
        </n-form-item>
      </n-form>
      <template #action>
        <n-button @click="showPasswordModal = false">取消</n-button>
        <n-button type="primary" :loading="changingPassword" @click="handleChangePassword">确认修改</n-button>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive } from 'vue'
import { NAvatar, NButton, NIcon, NEmpty, NForm, NFormItem, NInput, NRadioGroup, NRadio, NModal, useDialog, useMessage, FormRules, FormInst } from 'naive-ui'
import { LogOutOutline, CreateOutline, KeyOutline } from '@vicons/ionicons5'
import { useThemeStore } from '@/stores/theme'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api/auth'

const themeStore = useThemeStore()
const authStore = useAuthStore()
const dialog = useDialog()
const message = useMessage()

const user = computed(() => authStore.user)
const editing = ref(false)
const saving = ref(false)

const form = reactive({
  nickname: '',
  avatar: '',
  sex: 0,
  signature: '',
  email: '',
  mobile: ''
})

const defaultAvatar = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI4MCIgaGVpZ2h0PSI4MCIgdmlld0JveD0iMCAwIDgwIDgwIj48cmVjdCB3aWR0aD0iODAiIGhlaWdodD0iODAiIGZpbGw9IiNlMGUwZTAiLz48dGV4dCB4PSI1MCUiIHk9IjUwJSIgZG9taW5hbnQtYmFzZWxpbmU9Im1pZGRsZSIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZm9udC1zaXplPSIzMiIgZmlsbD0iIzk5OSI+PzwvdGV4dD48L3N2Zz4='

const sexText = computed(() => {
  if (!user.value) return '-'
  switch (user.value.sex) {
    case 1: return '男'
    case 2: return '女'
    default: return '未知'
  }
})

function startEdit() {
  form.nickname = user.value?.nickname || ''
  form.avatar = user.value?.avatar || ''
  form.sex = user.value?.sex ?? 0
  form.signature = user.value?.signature || ''
  form.email = ''
  form.mobile = ''
  editing.value = true
}

function cancelEdit() {
  editing.value = false
}

async function saveProfile() {
  saving.value = true
  try {
    const payload: any = {}
    if (form.nickname !== (user.value?.nickname || '')) payload.nickname = form.nickname
    if (form.avatar !== (user.value?.avatar || '')) payload.avatar = form.avatar
    if (form.sex !== (user.value?.sex ?? 0)) payload.sex = form.sex
    if (form.signature !== (user.value?.signature || '')) payload.signature = form.signature
    if (form.email && !user.value?.email) payload.email = form.email
    if (form.mobile && !user.value?.mobile) payload.mobile = form.mobile

    if (Object.keys(payload).length === 0) {
      editing.value = false
      return
    }

    await authApi.updateProfile(payload)
    await authStore.fetchUserInfo()
    editing.value = false
    message.success('个人信息修改成功')
  } catch (error: any) {
    message.error(error.message || '修改失败')
  } finally {
    saving.value = false
  }
}

const showPasswordModal = ref(false)
const changingPassword = ref(false)
const passwordFormRef = ref<FormInst | null>(null)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordRules: FormRules = {
  oldPassword: {
    required: true,
    message: '请输入旧密码',
    trigger: 'blur'
  },
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value) => {
        if (value !== passwordForm.newPassword) {
          return new Error('两次输入的密码不一致')
        }
        return true
      },
      trigger: 'blur'
    }
  ]
}

function openChangePasswordModal() {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  showPasswordModal.value = true
}

async function handleChangePassword() {
  try {
    await passwordFormRef.value?.validate()
  } catch (e) {
    return
  }

  changingPassword.value = true
  try {
    await authApi.changePassword(passwordForm.oldPassword, passwordForm.newPassword)
    message.success('密码修改成功，请重新登录')
    showPasswordModal.value = false
    setTimeout(() => {
      authStore.logout()
    }, 1500)
  } catch (error: any) {
    message.error(error.message || '密码修改失败')
  } finally {
    changingPassword.value = false
  }
}

function handleLogout() {
  dialog.warning({
    title: '确认退出',
    content: '确定要退出登录吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: () => {
      authStore.logout()
    }
  })
}
</script>

<style scoped>
.profile-container {
  padding: 24px;
  max-width: 600px;
  margin: 0 auto;
}

.profile-header h1 {
  margin: 0 0 24px 0;
  font-size: 24px;
  font-weight: 600;
  text-align: center;
}

.profile-card {
  padding: 32px;
  border-radius: 12px;
  border: 1px solid;
}

.avatar-section {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
}

.info-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 24px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid v-bind('themeStore.theme.colors.border');
}

.info-item:last-child {
  border-bottom: none;
}

.info-item .label {
  color: v-bind('themeStore.theme.colors.textSecondary');
  font-size: 14px;
}

.info-item .value {
  font-size: 14px;
}

.info-item .value .muted,
.info-item .muted {
  color: #999;
  font-size: 13px;
}

.edit-section {
  margin-bottom: 24px;
}

.edit-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
}

.action-section {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding-top: 16px;
}

.profile-empty {
  display: flex;
  justify-content: center;
  padding: 48px;
}
</style>
