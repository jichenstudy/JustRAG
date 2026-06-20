package com.shujichen.rag.common.dto.auth;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录用户信息DTO
 */
@Data
public class LoginUserInfo implements Serializable {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 性别
     */
    private Integer sex;

    /**
     * 个人签名
     */
    private String signature;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 登录Token
     */
    private String token;
}