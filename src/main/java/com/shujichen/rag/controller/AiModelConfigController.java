package com.shujichen.rag.controller;

import com.shujichen.rag.common.dto.Result;
import com.shujichen.rag.common.dto.model.AiModelConfigDTO;
import com.shujichen.rag.common.dto.model.CreateAiModelConfigDTO;
import com.shujichen.rag.service.AiModelConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI模型配置管理Controller
 */
@RestController
@RequestMapping("/api/model")
@RequiredArgsConstructor
public class AiModelConfigController {

    private final AiModelConfigService aiModelConfigService;

    /**
     * 创建模型配置
     */
    @PostMapping
    public Result<Long> createModelConfig(@Validated @RequestBody CreateAiModelConfigDTO dto) {
        Long id = aiModelConfigService.createModelConfig(dto);
        return Result.success(id);
    }

    /**
     * 更新模型配置
     */
    @PutMapping("/{id}")
    public Result<Void> updateModelConfig(
            @PathVariable Long id,
            @Validated @RequestBody CreateAiModelConfigDTO dto) {
        aiModelConfigService.updateModelConfig(id, dto);
        return Result.success(null);
    }

    /**
     * 删除模型配置
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteModelConfig(@PathVariable Long id) {
        aiModelConfigService.deleteModelConfig(id);
        return Result.success(null);
    }

    /**
     * 获取所有模型配置
     */
    @GetMapping
    public Result<List<AiModelConfigDTO>> getAllModelConfigs() {
        List<AiModelConfigDTO> configs = aiModelConfigService.getAllModelConfigs();
        return Result.success(configs);
    }

    /**
     * 获取所有启用的模型配置
     */
    @GetMapping("/enabled")
    public Result<List<AiModelConfigDTO>> getEnabledModelConfigs() {
        List<AiModelConfigDTO> configs = aiModelConfigService.getEnabledModelConfigs();
        return Result.success(configs);
    }

    /**
     * 获取所有可用的对话模型配置
     */
    @GetMapping("/getAvailableChatModelConfigs")
    public Result<List<AiModelConfigDTO>> getAvailableChatModelConfigs() {
        List<AiModelConfigDTO> configs = aiModelConfigService.getAvailableChatModelConfigs();
        return Result.success(configs);
    }

    /**
     * 获取所有可用的向量模型配置
     */
    @GetMapping("/getAvailableEmbeddingModelConfigs")
    public Result<List<AiModelConfigDTO>> getAvailableEmbeddingModelConfigs() {
        List<AiModelConfigDTO> configs = aiModelConfigService.getAvailableEmbeddingModelConfigs();
        return Result.success(configs);
    }

}
