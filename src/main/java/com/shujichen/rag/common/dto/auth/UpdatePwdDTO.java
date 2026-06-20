package com.shujichen.rag.common.dto.auth;

import lombok.Data;

/**
 * 修改密码请求DTO
 */
@Data
public class UpdatePwdDTO {

    /**
     * 原密码
     */
    private String oldPassword;

    /**
     * 新密码
     */
    private String newPassword;
}