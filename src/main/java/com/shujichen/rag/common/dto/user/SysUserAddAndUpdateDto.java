package com.shujichen.rag.common.dto.user;

import com.shujichen.rag.entity.SysUser;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 新增/修改用户参数DTO
 */
@Data
@Schema(description = "新增用户参数")
public class SysUserAddAndUpdateDto {

    /**
     * 用户信息
     */
    @Schema(description = "用户信息")
    private SysUser user;
}