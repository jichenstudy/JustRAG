package com.shujichen.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI模型配置表
 */
@Data
@TableName("ai_model_config")
public class AiModelConfig {

    /**
     * AI大模型配置ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 模型名称，如: gpt-4, qwen-plus
     */
    private String modelName;

    /**
     * 模型类型: CHAT-聊天模型, EMBEDDING-嵌入模型
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
     * 创建人ID
     */
    private Long userId;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
