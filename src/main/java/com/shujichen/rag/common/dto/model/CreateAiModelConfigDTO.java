package com.shujichen.rag.common.dto.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 创建/更新AI模型配置DTO
 */
@Data
public class CreateAiModelConfigDTO {

    /**
     * 模型名称
     */
    @NotBlank(message = "模型名称不能为空")
    private String modelName;

    /**
     * 模型类型: CHAT-聊天模型, EMBEDDING-向量模型, VISION-视觉模型
     */
    @NotBlank(message = "模型类型不能为空")
    private String modelType;

    /**
     * 提供商
     */
    @NotBlank(message = "提供商不能为空")
    private String provider;

    /**
     * API密钥
     */
    private String apiKey;

    /**
     * API端点
     */
    private String apiEndpoint;

    /**
     * 是否启用
     */
    @NotNull(message = "是否启用不能为空")
    private Boolean isEnabled;

    /**
     * 是否为默认模型
     */
    private Boolean isDefault;
}
