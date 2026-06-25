package com.shujichen.rag.common.dto.knowledgebase;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库DTO
 */
@Data
public class KnowledgeBaseDTO {

    /**
     * 知识库ID
     */
    private Long id;

    /**
     * 知识库名称
     */
    private String name;

    /**
     * 知识库描述
     */
    private String description;

    /**
     * 向量模型ID
     */
    private Long embeddingModelId;

    /**
     * 视觉模型ID
     */
    private Long visionModelId;

    /**
     * 切分策略
     */
    private String chunkStrategy;

    /**
     * 切分大小
     */
    private Integer chunkSize;

    /**
     * 切分重叠大小
     */
    private Integer chunkOverlap;

    /**
     * 最小切分大小
     */
    private Integer chunkMinSize;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
