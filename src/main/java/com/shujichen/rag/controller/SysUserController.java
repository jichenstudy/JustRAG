package com.shujichen.rag.controller;

import com.google.protobuf.ServiceException;
import com.shujichen.rag.common.dto.Result;
import com.shujichen.rag.common.dto.auth.UpdatePwdDTO;
import com.shujichen.rag.common.dto.user.SysUserAddAndUpdateDto;
import com.shujichen.rag.common.dto.user.UpdateProfileDTO;
import com.shujichen.rag.common.vo.user.SysUserProfileVo;
import com.shujichen.rag.entity.SysUser;
import com.shujichen.rag.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/api/sys/user")
@RequiredArgsConstructor
@Tag(name = "用户管理")
public class SysUserController {

    private final SysUserService sysUserService;

    /**
     * 新增用户
     *
     * @param sysUserAddDto 用户信息
     * @return 操作结果
     */
    @PostMapping
    @Operation(summary = "新增用户")
    public Result<Void> addUser(@RequestBody SysUserAddAndUpdateDto sysUserAddDto) {
        sysUserService.add(sysUserAddDto);
        return Result.success();
    }

    /**
     * 修改用户信息
     *
     * @param user 用户信息
     * @return 操作结果
     */
    @PutMapping
    @Operation(summary = "修改用户")
    public Result<Void> update(@RequestBody SysUserAddAndUpdateDto user) {
        sysUserService.update(user);
        return Result.success();
    }

    /**
     * 修改密码
     *
     * @param updatePwdDTO 密码修改信息
     * @return 操作结果
     */
    @PutMapping("/updatePwd")
    @Operation(summary = "修改密码")
    public Result<Void> updatePwd(@RequestBody UpdatePwdDTO updatePwdDTO) throws ServiceException {
        sysUserService.updatePwd(updatePwdDTO);
        return Result.success();
    }

    /**
     * 获取个人信息
     *
     * @return 个人信息
     */
    @GetMapping("/profile")
    @Operation(summary = "获取个人信息")
    public Result<SysUserProfileVo> profile() {
        return Result.success(sysUserService.profile());
    }

    /**
     * 修改个人信息
     *
     * @param dto 用户信息
     * @return 操作结果
     */
    @PutMapping("/updProfile")
    @Operation(summary = "修改个人信息")
    public Result<Void> updateProfile(@RequestBody UpdateProfileDTO dto) {
        sysUserService.updateProfile(dto);
        return Result.success();
    }

    /**
     * 重置密码
     *
     * @param user 用户信息
     * @return 是否成功
     */
    @PutMapping("/reset")
    @Operation(summary = "重置密码")
    public Result<Boolean> resetPassword(@RequestBody SysUser user) {
        return Result.success(sysUserService.resetPassword(user));
    }
}
