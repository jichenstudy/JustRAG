package com.shujichen.rag.service;

import com.shujichen.rag.common.dto.auth.LoginDTO;
import com.shujichen.rag.common.dto.auth.LoginUserInfo;
import com.shujichen.rag.common.dto.auth.RegisterDTO;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户登录（支持用户名或邮箱）
     *
     * @param loginDTO 登录请求DTO
     * @return 登录用户信息
     */
    LoginUserInfo login(LoginDTO loginDTO);

    /**
     * 用户注册
     *
     * @param dto 注册请求DTO
     * @return 注册结果
     */
    Boolean register(RegisterDTO dto);

    /**
     * 检查用户名是否已存在
     *
     * @param username 用户名
     * @return 是否存在
     */
    boolean checkUsernameExists(String username);

    /**
     * 获取当前登录用户信息
     *
     * @return 登录用户信息
     */
    LoginUserInfo getLoginUserInfo();

    /**
     * 用户退出登录
     */
    void logout();
}