package com.shujichen.rag.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shujichen.rag.common.vo.user.SysUserTeamVo;
import com.shujichen.rag.entity.SysUser;
import com.shujichen.rag.entity.SysUserTeam;
import com.shujichen.rag.mapper.SysUserTeamMapper;
import com.shujichen.rag.service.SysUserService;
import com.shujichen.rag.service.SysUserTeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 团队关联服务实现
 */
@Service
@RequiredArgsConstructor
public class SysUserTeamServiceImpl extends ServiceImpl<SysUserTeamMapper, SysUserTeam>
        implements SysUserTeamService {

    private final SysUserService userService;

    @Override
    public List<SysUserTeamVo> listJoinTeam() {
        // 获取当前登录的用户ID
        Long currentUserId = StpUtil.getLoginIdAsLong();

        // 根据用户ID查询userTeam信息，查找已关联的团队
        return this.baseMapper.selectJoinTeam(currentUserId);
    }

    @Override
    public List<SysUserTeamVo> listTeamUser() {
        // 获取当前登录的用户ID
        Long currentUserId = StpUtil.getLoginIdAsLong();

        // 根据用户ID查询当前用户团队的成员
        return this.baseMapper.selectTeamUser(currentUserId);
    }

    @Override
    public Boolean inviteTeamUser(String userName) {
        // 获取当前登录的用户ID
        Long currentUserId = StpUtil.getLoginIdAsLong();

        SysUser user = userService.getUserByName(userName);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (currentUserId.equals(user.getId())) {
            throw new RuntimeException("无法邀请自己");
        }

        SysUserTeam sysUserTeam = new SysUserTeam();
        sysUserTeam.setUserId(user.getId());
        sysUserTeam.setTeamId(currentUserId);
        sysUserTeam.setCreateAt(LocalDateTime.now());
        return this.save(sysUserTeam);
    }

    @Override
    public Boolean deleteJoinTeam(Long teamId) {
        // 获取当前登录的用户ID
        Long currentUserId = StpUtil.getLoginIdAsLong();

        // 不能退出用户默认创建团队
        if (Objects.equals(teamId, currentUserId)) {
            return false;
        }

        return remove(new LambdaQueryWrapper<SysUserTeam>()
                .eq(SysUserTeam::getTeamId, teamId)
                .eq(SysUserTeam::getUserId, currentUserId));
    }

    @Override
    public Boolean deleteTeamUser(Long userId) {
        // 获取当前登录的用户ID
        Long currentUserId = StpUtil.getLoginIdAsLong();
        return remove(new LambdaQueryWrapper<SysUserTeam>()
                .eq(SysUserTeam::getUserId, userId)
                .eq(SysUserTeam::getTeamId, currentUserId));
    }
}




