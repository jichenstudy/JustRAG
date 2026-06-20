package com.shujichen.rag.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户信息
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("sys_user")
@Schema(description = "用户信息")
public class SysUser implements Serializable {

    /**
     * 用户主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 账号
     */
    @Schema(description = "用户名")
    private String username;

    /**
     * 登录密码
     */
    @Schema(description = "密码")
    private String password;

    /**
     * 状态 0:禁用 1:正常
     */
    @Schema(description = "状态")
    private Integer status;

    /**
     * ip地址
     */
    @Schema(description = "ip地址")
    private String ip;

    /**
     * ip来源
     */
    @Schema(description = "ip来源")
    private String ipLocation;

    /**
     * 登录系统
     */
    @Schema(description = "操作系统")
    private String os;

    /**
     * 最后登录时间
     */
    @Schema(description = "上次登录时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastLoginTime;

    /**
     * 浏览器
     */
    @Schema(description = "浏览器")
    private String browser;

    /**
     * 昵称
     */
    @Schema(description = "昵称")
    private String nickname;

    /**
     * 头像
     */
    @Schema(description = "头像")
    private String avatar;

    /**
     * 手机号
     */
    @Schema(description = "手机号")
    private String mobile;

    /**
     * 邮箱
     */
    @Schema(description = "邮箱")
    private String email;

    /**
     * 个性签名
     */
    @Schema(description = "个性签名")
    private String signature;

    /**
     * 性别
     */
    @Schema(description = "性别")
    private Integer sex;

    /**
     * 登录方式
     */
    @Schema(description = "登录方式")
    private String loginType;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
