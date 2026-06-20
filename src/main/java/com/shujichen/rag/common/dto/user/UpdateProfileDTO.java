package com.shujichen.rag.common.dto.user;

import lombok.Data;

/**
 * 个人信息修改DTO
 */
@Data
public class UpdateProfileDTO {

    private String nickname;
    private String avatar;
    private Integer sex;
    private String signature;
    private String email;
    private String mobile;

}
