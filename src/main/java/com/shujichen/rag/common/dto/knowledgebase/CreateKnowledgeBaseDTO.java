package com.shujichen.rag.common.dto.knowledgebase;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建知识库请求DTO
 */
@Data
public class CreateKnowledgeBaseDTO {

    /**
     * 知识库名称
     */
    @NotBlank(message = "知识库名称不能为空")
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
     * 切分策略：SMART 结构感知 / FIXED 固定长度
     */
    private String chunkStrategy;

    /**
     * 分片大小（字符数），默认 1000
     */
    private Integer chunkSize;

    /**
     * 分片重叠大小（字符数），默认 200
     */
    private Integer chunkOverlap;

    /**
     * 最小分片大小（字符数），默认 100
     */
    private Integer chunkMinSize;
}