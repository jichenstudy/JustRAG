<template>
  <div class="login-container" :style="{ backgroundColor: themeStore.theme.colors.background }">
    <div class="login-card" :style="{ backgroundColor: themeStore.theme.colors.surface, borderColor: themeStore.theme.colors.border }">
      <h1 class="login-title" :style="{ color: themeStore.theme.colors.text }">Just RAG</h1>
      <p class="login-subtitle" :style="{ color: themeStore.theme.colors.textSecondary }">
        {{ isResetPassword ? '重置您的密码' : isRegister ? '创建新账户' : '登录到您的账户' }}
      </p>

      <!-- 登录表单 -->
      <n-form v-if="!isRegister && !isResetPassword" ref="loginFormRef" :model="loginForm" :rules="loginRules" class="login-form">
        <n-form-item path="username" label="账号">
          <n-input
            v-model:value="loginForm.username"
            placeholder="请输入用户名或邮箱"
            size="large"
            @keyup.enter="handleLogin"
          />
        </n-form-item>

        <n-form-item path="password" label="密码">
          <n-input
            v-model:value="loginForm.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            show-password-on="click"
            @keyup.enter="handleLogin"
          />
        </n-form-item>

        <n-button
          type="primary"
          block
          size="large"
          :loading="loading"
          @click="handleLogin"
        >
          登录
        </n-button>

        <div class="switch-mode">
          <n-button text type="primary" @click="isRegister = true">立即注册</n-button>
          <n-button text type="primary" @click="isResetPassword = true" style="margin-left: 16px">忘记密码</n-button>
        </div>
      </n-form>

      <!-- 找回密码表单 -->
      <n-form v-else-if="isResetPassword && !isRegister" ref="resetPasswordFormRef" :model="resetPasswordForm" :rules="resetPasswordRules" class="login-form">
        <n-form-item path="email" label="邮箱">
          <n-input
            v-model:value="resetPasswordForm.email"
            placeholder="请输入注册时使用的邮箱"
            size="large"
          />
        </n-form-item>

        <n-form-item path="captchaCode" label="验证码">
          <div style="display: flex; gap: 12px; width: 100%">
            <n-input
              v-model:value="resetPasswordForm.captchaCode"
              placeholder="请输入验证码"
              size="large"
              style="flex: 1"
              maxlength="4"
            />
            <n-button
              size="large"
              :loading="sendingResetCode"
              :disabled="resetCountdown > 0"
              @click="handleSendResetCode"
              style="width: 120px"
            >
              {{ resetCountdown > 0 ? `${resetCountdown}s 后重发` : '发送验证码' }}
            </n-button>
          </div>
        </n-form-item>

        <n-form-item path="newPassword" label="新密码">
          <n-input
            v-model:value="resetPasswordForm.newPassword"
            type="password"
            placeholder="请输入新密码（6-20位）"
            size="large"
            show-password-on="click"
          />
        </n-form-item>

        <n-form-item path="confirmPassword" label="确认密码">
          <n-input
            v-model:value="resetPasswordForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            size="large"
            show-password-on="click"
            @keyup.enter="handleResetPassword"
          />
        </n-form-item>

        <n-button
          type="primary"
          block
          size="large"
          :loading="loading"
          @click="handleResetPassword"
        >
          重置密码
        </n-button>

        <div class="switch-mode">
          <span :style="{ color: themeStore.theme.colors.textSecondary }">想起密码了？</span>
          <n-button text type="primary" @click="isResetPassword = false">返回登录</n-button>
        </div>
      </n-form>

      <!-- 注册表单 -->
      <n-form v-else ref="registerFormRef" :model="registerForm" :rules="registerRules" class="login-form">
        <n-form-item path="username" label="用户名">
          <n-input
            v-model:value="registerForm.username"
            placeholder="请输入用户名（唯一登录账号）"
            size="large"
            @blur="checkUsernameExists"
          />
          <template #feedback>
            <span v-if="usernameChecking" style="font-size:12px;color:#999">检查中...</span>
            <span v-else-if="usernameAvailable === true" style="font-size:12px;color:#18a058">用户名可用</span>
            <span v-else-if="usernameAvailable === false" style="font-size:12px;color:#d03050">用户名已存在</span>
          </template>
        </n-form-item>

        <n-form-item path="email" label="邮箱">
          <n-input
            v-model:value="registerForm.email"
            placeholder="请输入邮箱"
            size="large"
          />
        </n-form-item>

        <n-form-item v-if="emailCaptchaEnabled" path="captchaCode" label="验证码">
          <div style="display: flex; gap: 12px; width: 100%">
            <n-input
              v-model:value="registerForm.captchaCode"
              placeholder="请输入验证码"
              size="large"
              style="flex: 1"
              maxlength="4"
            />
            <n-button
              size="large"
              :loading="sendingCode"
              :disabled="countdown > 0"
              @click="handleSendCode"
              style="width: 120px"
            >
              {{ countdown > 0 ? `${countdown}s 后重发` : '发送验证码' }}
            </n-button>
          </div>
        </n-form-item>

        <n-form-item path="nickname" label="昵称">
          <n-input
            v-model:value="registerForm.nickname"
            placeholder="请输入昵称（选填，默认使用用户名）"
            size="large"
          />
        </n-form-item>

        <n-form-item path="password" label="密码">
          <n-input
            v-model:value="registerForm.password"
            type="password"
            placeholder="请输入密码（6-20位）"
            size="large"
            show-password-on="click"
          />
        </n-form-item>

        <n-form-item path="confirmPassword" label="确认密码">
          <n-input
            v-model:value="registerForm.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            size="large"
            show-password-on="click"
            @keyup.enter="handleRegister"
          />
        </n-form-item>

        <n-button
          type="primary"
          block
          size="large"
          :loading="loading"
          @click="handleRegister"
        >
          注册
        </n-button>

        <div class="switch-mode">
          <span :style="{ color: themeStore.theme.colors.textSecondary }">已有账户？</span>
          <n-button text type="primary" @click="isRegister = false">返回登录</n-button>
        </div>
      </n-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NForm, NFormItem, NInput, NButton, useMessage, type FormInst, type FormRules } from 'naive-ui'
import { useThemeStore } from '@/stores/theme'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api/auth'

const router = useRouter()
const message = useMessage()
const themeStore = useThemeStore()
const authStore = useAuthStore()

const isRegister = ref(false)
const isResetPassword = ref(false)
const loading = ref(false)
const emailCaptchaEnabled = ref(false)

// 登录表单
const loginFormRef = ref<FormInst | null>(null)
const loginForm = reactive({
  username: '',
  password: ''
})

const loginRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

// 注册表单
const registerFormRef = ref<FormInst | null>(null)
const registerForm = reactive({
  username: '',
  email: '',
  captchaCode: '',
  nickname: '',
  password: '',
  confirmPassword: ''
})

const sendingCode = ref(false)
const countdown = ref(0)
let countdownTimer: number | null = null

const sendingResetCode = ref(false)
const resetCountdown = ref(0)
let resetCountdownTimer: number | null = null

const usernameChecking = ref(false)
const usernameAvailable = ref<boolean | null>(null)

// 找回密码表单
const resetPasswordFormRef = ref<FormInst | null>(null)
const resetPasswordForm = reactive({
  email: '',
  captchaCode: '',
  newPassword: '',
  confirmPassword: ''
})

const resetPasswordRules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ],
  captchaCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 4, message: '验证码为4位数字', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value) => {
        if (value !== resetPasswordForm.newPassword) {
          return new Error('两次输入的密码不一致')
        }
        return true
      },
      trigger: 'blur'
    }
  ]
}

async function handleSendResetCode() {
  const email = resetPasswordForm.email.trim()
  if (!email) {
    message.warning('请先输入邮箱')
    return
  }
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(email)) {
    message.warning('请输入有效的邮箱地址')
    return
  }

  sendingResetCode.value = true
  try {
    await authApi.sendResetPasswordCode(email)
    message.success('验证码已发送到您的邮箱')
    resetCountdown.value = 60
    resetCountdownTimer = window.setInterval(() => {
      resetCountdown.value--
      if (resetCountdown.value <= 0) {
        if (resetCountdownTimer) {
          clearInterval(resetCountdownTimer)
          resetCountdownTimer = null
        }
      }
    }, 1000)
  } catch (error: any) {
    message.error(error.message || '验证码发送失败')
  } finally {
    sendingResetCode.value = false
  }
}

async function handleResetPassword() {
  try {
    await resetPasswordFormRef.value?.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await authApi.resetPassword({
      email: resetPasswordForm.email,
      captchaCode: resetPasswordForm.captchaCode,
      newPassword: resetPasswordForm.newPassword
    })
    message.success('密码重置成功，请使用新密码登录')
    // 切换到登录表单
    isResetPassword.value = false
    loginForm.username = resetPasswordForm.email
    loginForm.password = ''
    // 清空找回密码表单
    resetPasswordForm.email = ''
    resetPasswordForm.captchaCode = ''
    resetPasswordForm.newPassword = ''
    resetPasswordForm.confirmPassword = ''
  } catch (error: any) {
    message.error(error.message || '密码重置失败')
  } finally {
    loading.value = false
  }
}

// 获取邮箱验证码开关状态
async function loadEmailCaptchaEnabled() {
  try {
    const res = await authApi.getEmailCaptchaEnabled()
    emailCaptchaEnabled.value = res.data === true
  } catch (error) {
    console.error('获取邮箱验证码开关失败', error)
    emailCaptchaEnabled.value = false
  }
}

onMounted(() => {
  loadEmailCaptchaEnabled()
})

async function checkUsernameExists() {
  const val = registerForm.username.trim()
  if (!val || val.length < 3) {
    usernameAvailable.value = null
    return
  }
  usernameChecking.value = true
  usernameAvailable.value = null
  try {
    const res = await authApi.checkUsername(val)
    usernameAvailable.value = !res.data  // true=可用 false=已存在
  } catch {
    usernameAvailable.value = null
  } finally {
    usernameChecking.value = false
  }
}

const registerRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 30, message: '用户名长度3-30位', trigger: 'blur' },
    {
      validator: () => {
        if (usernameAvailable.value === false) return new Error('用户名已存在')
        return true
      },
      trigger: 'blur'
    }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ],
  captchaCode: [
    {
      validator: (rule, value) => {
        if (emailCaptchaEnabled.value) {
          if (!value) return new Error('请输入验证码')
          if (value.length !== 4) return new Error('验证码为4位数字')
        }
        return true
      },
      trigger: 'blur'
    }
  ],
  nickname: [],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule, value) => {
        if (value !== registerForm.password) {
          return new Error('两次输入的密码不一致')
        }
        return true
      },
      trigger: 'blur'
    }
  ]
}

async function handleSendCode() {
  const email = registerForm.email.trim()
  if (!email) {
    message.warning('请先输入邮箱')
    return
  }
  // 验证邮箱格式
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(email)) {
    message.warning('请输入有效的邮箱地址')
    return
  }

  sendingCode.value = true
  try {
    await authApi.sendEmailCode(email)
    message.success('验证码已发送到您的邮箱')
    // 开始倒计时
    countdown.value = 60
    countdownTimer = window.setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        if (countdownTimer) {
          clearInterval(countdownTimer)
          countdownTimer = null
        }
      }
    }, 1000)
  } catch (error: any) {
    message.error(error.message || '验证码发送失败')
  } finally {
    sendingCode.value = false
  }
}

async function handleLogin() {
  try {
    await loginFormRef.value?.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await authStore.login(loginForm)
    message.success('登录成功')
    router.push('/')
  } catch (error: any) {
    message.error(error.message || '登录失败')
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  try {
    await registerFormRef.value?.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    const res = await authApi.register({
      username: registerForm.username,
      email: registerForm.email,
      nickname: registerForm.nickname || undefined,
      password: registerForm.password,
      captchaCode: emailCaptchaEnabled.value ? registerForm.captchaCode : ''
    })
    if (res.code === 200 && res.data) {
      message.success('注册成功，请登录')
      // 切换到登录，并填充用户名
      isRegister.value = false
      loginForm.username = registerForm.username
      loginForm.password = ''
      // 清空注册表单
      registerForm.username = ''
      registerForm.email = ''
      registerForm.captchaCode = ''
      registerForm.nickname = ''
      registerForm.password = ''
      registerForm.confirmPassword = ''
      usernameAvailable.value = null
    } else {
      message.error(res.message || '注册失败')
    }
  } catch (error: any) {
    message.error(error.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.login-card {
  width: 100%;
  max-width: 400px;
  padding: 40px;
  border-radius: 12px;
  border: 1px solid;
}

.login-title {
  margin: 0 0 8px 0;
  font-size: 28px;
  font-weight: 600;
  text-align: center;
}

.login-subtitle {
  margin: 0 0 32px 0;
  font-size: 14px;
  text-align: center;
}

.login-form {
  width: 100%;
}

.switch-mode {
  margin-top: 20px;
  text-align: center;
  font-size: 14px;
}
</style>
