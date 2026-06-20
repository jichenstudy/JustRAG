import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { LoginUserInfo, LoginDTO } from '@/types'
import { authApi } from '@/api/auth'
import router from '@/router'

const TOKEN_KEY = 'token'
const USER_KEY = 'user'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  const user = ref<LoginUserInfo | null>(
    JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  )

  const isLoggedIn = computed(() => !!token.value)

  // 登录
  async function login(loginData: LoginDTO) {
    const res = await authApi.login(loginData)
    if (res.code === 200 && res.data) {
      token.value = res.data.token
      user.value = res.data
      localStorage.setItem(TOKEN_KEY, res.data.token)
      localStorage.setItem(USER_KEY, JSON.stringify(res.data))
      return res
    }
    throw new Error(res.message || '登录失败')
  }

  // 登出
  async function logout() {
    try {
      await authApi.logout()
    } finally {
      clearAuth()
      router.push('/login')
    }
  }

  // 获取用户信息
  async function fetchUserInfo() {
    const res = await authApi.getUserInfo()
    if (res.code === 200 && res.data) {
      user.value = res.data
      localStorage.setItem(USER_KEY, JSON.stringify(res.data))
      return res.data
    }
    throw new Error(res.message || '获取用户信息失败')
  }

  // 清除认证信息
  function clearAuth() {
    token.value = null
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return {
    token,
    user,
    isLoggedIn,
    login,
    logout,
    fetchUserInfo,
    clearAuth
  }
})
