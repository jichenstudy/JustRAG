import request from '@/utils/request'
import type { Result, SysUserTeamVo } from '@/types'

export const teamApi = {
  // 获取已加入的团队列表
  listJoinTeam(): Promise<Result<SysUserTeamVo[]>> {
    return request.get('/sys/user/team/listJoinTeam')
  },

  // 获取我的团队成员列表
  listTeamUser(): Promise<Result<SysUserTeamVo[]>> {
    return request.get('/sys/user/team/listTeamUser')
  },

  // 邀请用户加入我的团队
  inviteTeamUser(userName: string): Promise<Result<boolean>> {
    return request.post('/sys/user/team/inviteTeamUser', null, {
      params: { userName }
    })
  },

  // 退出已加入的团队
  deleteJoinTeam(teamId: string): Promise<Result<boolean>> {
    return request.delete('/sys/user/team/deleteJoinTeam', {
      params: { teamId }
    })
  },

  // 删除我的团队成员
  deleteTeamUser(userId: string): Promise<Result<boolean>> {
    return request.delete('/sys/user/team/deleteTeamUser', {
      params: { userId }
    })
  }
}
