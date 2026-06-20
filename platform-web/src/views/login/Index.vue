<template>
  <div class="login-container" :style="{ backgroundColor: themeStore.theme.colors.background }">
    <div class="login-card" :style="{ backgroundColor: themeStore.theme.colors.surface, borderColor: themeStore.theme.colors.border }">
      <h1 class="login-title" :style="{ color: themeStore.theme.colors.text }">Just RAG</h1>
      <p class="login-subtitle" :style="{ color: themeStore.theme.colors.textSecondary }">
        {{ isRegister ? '创建新账户' : '登录到您的账户' }}
      </p>

      <!-- 登录表单 -->
      <n-form v-if="!isRegister" ref="loginFormRef" :model="loginForm" :rules="loginRules" class="login-form">
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
          <span :style="{ color: themeStore.theme.colors.textSecondary }">还没有账户？</span>
          <n-button text type="primary" @click="isRegister = true">立即注册</n-button>
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
import { ref, reactive } from 'vue'
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
const loading = ref(false)

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
  nickname: '',
  password: '',
  confirmPassword: ''
})

const usernameChecking = ref(false)
const usernameAvailable = ref<boolean | null>(null)

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
      password: registerForm.password
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
