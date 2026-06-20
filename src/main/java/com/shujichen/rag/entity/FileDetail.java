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
 * 文件记录表
 */
@Data
@TableName("file_detail")
@Accessors(chain = true)
@Schema(description = "文件记录表对象")
public class FileDetail implements Serializable {

    /**
     * 文件记录主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @Schema(description = "文件访问地址")
    private String url;

    @Schema(description = "文件大小，单位字节")
    private Long size;

    @Schema(description = "文件名称")
    private String filename;

    @Schema(description = "原始文件名")
    private String originalFilename;

    @Schema(description = "minio存储桶名称")
    private String bucketName;

    @Schema(description = "minio对象名称")
    private String objectName;

    @Schema(description = "基础存储路径")
    private String basePath;

    @Schema(description = "存储路径")
    private String path;

    @Schema(description = "文件扩展名")
    private String ext;

    @Schema(description = "MIME类型")
    private String contentType;

    @Schema(description = "存储平台")
    private String platform;

    @Schema(description = "哈希信息")
    private String hashInfo;

    @Schema(description = "上传ID")
    private String uploadId;

    @Schema(description = "上传状态 0-上传失败 1-上传成功")
    private Integer uploadStatus;

    /**
     * 创建人ID
     */
    private Long userId;

    /**
     * 知识库主键ID
     */
    private Long knowledgeBaseId;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
