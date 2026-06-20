package com.shujichen.rag.controller;

import com.shujichen.rag.common.dto.Result;
import com.shujichen.rag.common.vo.user.SysUserTeamVo;
import com.shujichen.rag.service.SysUserTeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户团队关联Controller
 */
@RestController
@RequestMapping("/api/sys/user/team")
@RequiredArgsConstructor
public class SysUserTeamController {

    private final SysUserTeamService sysUserTeamService;

    /**
     * 查询已关联的团队列表
     *
     * @return 团队列表
     */
    @GetMapping("/listJoinTeam")
    public Result<List<SysUserTeamVo>> listJoinTeam() {
        return Result.success(sysUserTeamService.listJoinTeam());
    }

    /**
     * 查询团队成员列表
     *
     * @return 成员列表
     */
    @GetMapping("/listTeamUser")
    public Result<List<SysUserTeamVo>> listTeamUser() {
        return Result.success(sysUserTeamService.listTeamUser());
    }

    /**
     * 邀请团队成员
     *
     * @param userName 用户名称
     * @return 是否成功
     */
    @PostMapping("/inviteTeamUser")
    public Result<Boolean> inviteTeamUser(@RequestParam("userName") String userName) {
        return Result.success(sysUserTeamService.inviteTeamUser(userName));
    }

    /**
     * 删除已关联的团队
     *
     * @param teamId 团队ID
     * @return 是否成功
     */
    @DeleteMapping("/deleteJoinTeam")
    public Result<Boolean> deleteJoinTeam(@RequestParam("teamId") Long teamId) {
        return Result.success(sysUserTeamService.deleteJoinTeam(teamId));
    }

    /**
     * 删除团队成员
     *
     * @param userId 用户ID
     * @return 是否成功
     */
    @DeleteMapping("/deleteTeamUser")
    public Result<Boolean> deleteTeamUser(@RequestParam("userId") Long userId) {
        return Result.success(sysUserTeamService.deleteTeamUser(userId));
    }
}