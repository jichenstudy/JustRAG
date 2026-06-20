package com.shujichen.rag.common.vo.user;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户团队关联VO
 */
@Data
public class SysUserTeamVo {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 团队ID
     */
    private Long teamId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createAt;
}