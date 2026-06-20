package com.shujichen.rag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shujichen.rag.common.dto.model.AiModelConfigDTO;
import com.shujichen.rag.common.dto.model.CreateAiModelConfigDTO;
import com.shujichen.rag.entity.AiModelConfig;

import java.util.List;

/**
 * AI模型配置服务接口
 */
public interface AiModelConfigService extends IService<AiModelConfig> {

    /**
     * 创建模型配置
     *
     * @param dto 创建模型配置DTO
     * @return 模型配置ID
     */
    Long createModelConfig(CreateAiModelConfigDTO dto);

    /**
     * 更新模型配置
     *
     * @param id  模型配置ID
     * @param dto 更新模型配置DTO
     */
    void updateModelConfig(Long id, CreateAiModelConfigDTO dto);

    /**
     * 删除模型配置
     *
     * @param id 模型配置ID
     */
    void deleteModelConfig(Long id);

    /**
     * 根据ID获取模型配置
     *
     * @param id 模型配置ID
     * @return 模型配置实体
     */
    AiModelConfig getModelConfigById(Long id);

    /**
     * 获取所有模型配置
     *
     * @return 模型配置列表
     */
    List<AiModelConfigDTO> getAllModelConfigs();

    /**
     * 获取所有启用的模型配置
     *
     * @return 启用的模型配置列表
     */
    List<AiModelConfigDTO> getEnabledModelConfigs();

    /**
     * 获取默认模型配置
     *
     * @return 默认模型配置
     */
    AiModelConfig getDefaultModelConfig();

    /**
     * 获取指定类型的启用模型（聊天模型或嵌入模型）
     *
     * @param modelType CHAT 或 EMBEDDING
     * @return 模型配置
     */
    AiModelConfig getEnabledModelConfigByType(String modelType);

    /**
     * 获取所有可用的对话模型配置
     *
     * @return 可用的对话模型配置
     */
    List<AiModelConfigDTO> getAvailableChatModelConfigs();

    /**
     * 获取所有可用的向量模型配置
     *
     * @return 可用的向量模型配置
     */
    List<AiModelConfigDTO> getAvailableEmbeddingModelConfigs();
}