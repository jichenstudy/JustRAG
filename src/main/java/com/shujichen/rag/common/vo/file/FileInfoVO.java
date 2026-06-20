package com.shujichen.rag.common.vo.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文件信息VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileInfoVO {

    /**
     * 上传ID
     */
    private String uploadId;

    /**
     * 文件ID
     */
    private Long fileId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 对象名称（在Minio中的实际名称）
     */
    private String objectName;

    /**
     * 原始文件名
     */
    private String originalFileName;

    /**
     * 文件扩展名
     */
    private String fileExtension;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件大小（格式化字符串）
     */
    private String fileSizeFormatted;

    /**
     * 文件类型/MIME类型
     */
    private String contentType;

    /**
     * 文件Hash值
     */
    private String fileHash;

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * 文件访问URL
     */
    private String url;

    /**
     * 上传状态
     */
    private String status;

    /**
     * 分片数量
     */
    private Integer partCount;

    /**
     * 已完成分片数量
     */
    private Integer completedParts;

    /**
     * 上传开始时间
     */
    private LocalDateTime uploadTime;

    /**
     * 上传完成时间
     */
    private LocalDateTime completeTime;

    /**
     * 错误信息
     */
    private String errorMessage;
}
