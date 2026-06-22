package com.shujichen.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对象存储配置表
 */
@Data
@TableName("oss_config")
@Accessors(chain = true)
@Schema(description = "对象存储配置表对象")
public class OssConfig implements Serializable {

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "存储配置主键ID")
    private Long ossConfigId;

    /**
     * 配置key（唯一标识，如 minio、aliyun、qcloud 等）
     */
    @Schema(description = "配置key")
    private String configKey;

    /**
     * accessKey
     */
    @Schema(description = "accessKey")
    private String accessKey;

    /**
     * secretKey
     */
    @Schema(description = "secretKey")
    private String secretKey;

    /**
     * 桶名称
     */
    @Schema(description = "桶名称")
    private String bucketName;

    /**
     * 前缀（路径前缀）
     */
    @Schema(description = "前缀")
    private String prefix;

    /**
     * 访问站点（如 localhost:9000）
     */
    @Schema(description = "访问站点")
    private String endpoint;

    /**
     * 自定义域名（可选）
     */
    @Schema(description = "自定义域名")
    private String domain;

    /**
     * 是否https（Y=是,N=否）
     */
    @Schema(description = "是否https（Y=是,N=否）")
    private String isHttps;

    /**
     * 域（如 us-east-1）
     */
    @Schema(description = "域")
    private String region;

    /**
     * 桶权限类型(0=private 1=public 2=custom)
     */
    @Schema(description = "桶权限类型(0=private 1=public 2=custom)")
    private String accessPolicy;

    /**
     * 是否默认（0=是,1=否）
     */
    @Schema(description = "是否默认（0=是,1=否）")
    private String status;

    /**
     * 扩展字段（JSON格式）
     */
    @Schema(description = "扩展字段")
    private String extJson;

    /**
     * 备注
     */
    @Schema(description = "备注")
    private String remark;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private Long userId;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
}
