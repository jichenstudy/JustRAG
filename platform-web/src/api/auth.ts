import request from '@/utils/request'
import type { Result, LoginDTO, LoginUserInfo, RegisterDTO, UpdateProfileDTO } from '@/types'

export const authApi = {
  // 登录
  login(data: LoginDTO): Promise<Result<LoginUserInfo>> {
    return request.post('/auth/login', data)
  },

  // 登出
  logout(): Promise<Result<void>> {
    return request.post('/auth/logout')
  },

  // 获取当前用户信息
  getUserInfo(): Promise<Result<LoginUserInfo>> {
    return request.get('/auth/info')
  },

  // 注册
  register(data: RegisterDTO): Promise<Result<boolean>> {
    return request.post('/auth/register', data)
  },

  // 检查用户名是否存在
  checkUsername(username: string): Promise<Result<boolean>> {
    return request.get('/auth/check-username', { params: { username } })
  },

  // 修改个人信息
  updateProfile(data: UpdateProfileDTO): Promise<Result<void>> {
    return request.put('/sys/user/updProfile', data)
  }
}
