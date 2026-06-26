import request from '@/utils/request'
import type { Result, LoginDTO, LoginUserInfo, RegisterDTO, ResetPasswordDTO, UpdateProfileDTO } from '@/types'

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

  // 发送邮箱验证码
  sendEmailCode(email: string): Promise<Result<void>> {
    return request.post('/auth/sendEmailCode', null, { params: { email } })
  },

  // 发送找回密码验证码
  sendResetPasswordCode(email: string): Promise<Result<void>> {
    return request.post('/auth/sendResetPasswordCode', null, { params: { email } })
  },

  // 重置密码
  resetPassword(data: ResetPasswordDTO): Promise<Result<void>> {
    return request.post('/auth/resetPassword', data)
  },

  // 获取邮箱验证码功能是否开启
  getEmailCaptchaEnabled(): Promise<Result<boolean>> {
    return request.get('/auth/emailCaptchaEnabled')
  },

  // 检查用户名是否存在
  checkUsername(username: string): Promise<Result<boolean>> {
    return request.get('/auth/check-username', { params: { username } })
  },

  // 修改个人信息
  updateProfile(data: UpdateProfileDTO): Promise<Result<void>> {
    return request.put('/sys/user/updProfile', data)
  },

  // 修改个人密码
  changePassword(oldPassword: string, newPassword: string): Promise<Result<void>> {
    return request.put('/sys/user/updatePwd', { oldPassword, newPassword })
  }
}
