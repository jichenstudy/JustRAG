package com.shujichen.rag.common.dto.document;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档DTO
 */
@Data
public class DocumentDTO {

    /**
     * 文档ID
     */
    private Long id;

    /**
     * 知识库ID
     */
    private Long knowledgeBaseId;

    /**
     * 文件ID
     */
    private String fileId;

    /**
     * 文档名称
     */
    private String name;

    /**
     * 文档类型
     */
    private String docType;

    /**
     * 解析状态
     */
    private String parseStatus;

    /**
     * 分块数量
     */
    private Integer chunkCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}