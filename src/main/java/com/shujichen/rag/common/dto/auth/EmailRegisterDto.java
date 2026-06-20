package com.shujichen.rag.common.dto.auth;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 邮箱注册请求DTO
 */
@Data
public class EmailRegisterDto {

    /**
     * 邮箱
     */
    @NotNull(message = "邮箱不能为空")
    private String email;

    /**
     * 密码
     */
    @NotNull(message = "密码不能为空")
    private String password;

    /**
     * 昵称
     */
    @NotNull(message = "昵称不能为空")
    private String nickname;
}