package com.shujichen.rag.common.dto.knowledgebase;

import lombok.Data;

/**
 * 更新知识库请求DTO
 */
@Data
public class UpdateKnowledgeBaseDTO {

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
     * AI模型ID
     */
    private Long modelId;

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
}