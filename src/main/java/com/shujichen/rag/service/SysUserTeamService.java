package com.shujichen.rag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shujichen.rag.common.vo.user.SysUserTeamVo;
import com.shujichen.rag.entity.SysUserTeam;

import java.util.List;

/**
 * 用户团队关联服务接口
 */
public interface SysUserTeamService extends IService<SysUserTeam> {

    /**
     * 根据用户ID查询已关联的团队
     *
     * @return 团队列表
     */
    List<SysUserTeamVo> listJoinTeam();

    /**
     * 查询当前用户的团队成员
     *
     * @return 团队成员列表
     */
    List<SysUserTeamVo> listTeamUser();

    /**
     * 邀请团队成员
     *
     * @param userName 用户名称
     * @return 是否成功
     */
    Boolean inviteTeamUser(String userName);

    /**
     * 删除已关联的团队
     *
     * @param teamId 团队ID
     * @return 是否成功
     */
    Boolean deleteJoinTeam(Long teamId);

    /**
     * 删除团队成员
     *
     * @param userId 用户ID
     * @return 是否成功
     */
    Boolean deleteTeamUser(Long userId);
}