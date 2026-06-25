package com.shujichen.rag.common.dto.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI模型配置DTO
 */
@Data
public class AiModelConfigDTO {

    private Long id;

    /**
     * 实际的模型ID
     */
    private String modelId;

    /**
     * 模型名称，如: gpt-4, qwen-plus
     */
    private String modelName;

    /**
     * 模型类型: CHAT-聊天模型, EMBEDDING-向量模型, VISION-视觉模型
     */
    private String modelType;

    /**
     * 提供商: DASHSCOPE, OPENAI, OLLAMA, AZURE_OPENAI
     */
    private String provider;

    /**
     * API密钥（加密存储）
     */
    private String apiKey;

    /**
     * API端点URL
     */
    private String apiEndpoint;


    /**
     * 是否启用: 1-启用, 0-禁用
     */
    private Boolean isEnabled;

    /**
     * 是否为默认模型: 1-是, 0-否
     */
    private Boolean isDefault;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
