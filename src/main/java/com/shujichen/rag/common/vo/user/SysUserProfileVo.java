package com.shujichen.rag.common.vo.user;

import com.shujichen.rag.entity.SysUser;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户个人信息VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "个人信息")
public class SysUserProfileVo {

    /**
     * 用户信息
     */
    @Schema(description = "用户信息")
    private SysUser sysUser;
}