package com.shujichen.rag.controller;

import com.shujichen.rag.common.dto.Result;
import com.shujichen.rag.common.dto.auth.LoginDTO;
import com.shujichen.rag.common.dto.auth.LoginUserInfo;
import com.shujichen.rag.common.dto.auth.RegisterDTO;
import com.shujichen.rag.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@RequestMapping("/api/auth")
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     *
     * @param loginDTO 登录信息
     * @return 用户信息
     */
    @PostMapping("/login")
    public Result<LoginUserInfo> login(@Validated @RequestBody LoginDTO loginDTO) {
        return Result.success(authService.login(loginDTO));
    }

    /**
     * 用户注册（用户名+邮箱+密码，昵称选填）
     */
    @PostMapping("/register")
    public Result<Boolean> register(@Validated @RequestBody RegisterDTO dto) {
        return Result.success(authService.register(dto));
    }

    /**
     * 检查用户名是否已存在
     */
    @GetMapping("/check-username")
    public Result<Boolean> checkUsername(@RequestParam String username) {
        return Result.success(authService.checkUsernameExists(username));
    }

    /**
     * 用户退出登录
     *
     * @return 操作结果
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success(null);
    }

    /**
     * 获取当前登录用户信息
     *
     * @return 用户信息
     */
    @GetMapping("/info")
    public Result<LoginUserInfo> getUserInfo() {
        return Result.success(authService.getLoginUserInfo());
    }
}
