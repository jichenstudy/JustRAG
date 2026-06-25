package com.shujichen.rag.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shujichen.rag.common.dto.model.AiModelConfigDTO;
import com.shujichen.rag.common.dto.model.CreateAiModelConfigDTO;
import com.shujichen.rag.common.enums.ApiConfigModelType;
import com.shujichen.rag.entity.AiModelConfig;
import com.shujichen.rag.mapper.AiModelConfigMapper;
import com.shujichen.rag.service.AiModelConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI模型配置服务实现
 */
@Slf4j
@Service
public class AiModelConfigServiceImpl extends ServiceImpl<AiModelConfigMapper, AiModelConfig> implements AiModelConfigService {

    @Autowired
    private AiModelConfigMapper aiModelConfigMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createModelConfig(CreateAiModelConfigDTO dto) {
        // 检查模型名称是否已存在
//        LambdaQueryWrapper<AiModelConfig> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.eq(AiModelConfig::getModelName, dto.getModelName());
//        if (aiModelConfigMapper.selectCount(queryWrapper) > 0) {
//            throw new IllegalArgumentException("模型名称已存在: " + dto.getModelName());
//        }

        AiModelConfig config = new AiModelConfig();
        BeanUtils.copyProperties(dto, config);
        long userId = StpUtil.getLoginIdAsLong();
        config.setUserId(userId);
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());

        aiModelConfigMapper.insert(config);
        log.info("创建模型配置成功, ID: {}, 模型名称: {}", config.getId(), config.getModelName());
        return config.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateModelConfig(Long id, CreateAiModelConfigDTO dto) {
        AiModelConfig existingConfig = aiModelConfigMapper.selectById(id);
        if (existingConfig == null) {
            throw new IllegalArgumentException("模型配置不存在, ID: " + id);
        }

        // 如果修改了模型名称,检查新名称是否已被使用
        if (!existingConfig.getModelName().equals(dto.getModelName())) {
            LambdaQueryWrapper<AiModelConfig> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(AiModelConfig::getModelName, dto.getModelName());
            if (aiModelConfigMapper.selectCount(queryWrapper) > 0) {
                throw new IllegalArgumentException("模型名称已存在: " + dto.getModelName());
            }
        }

        // 保存原来的 API Key
        String originalApiKey = existingConfig.getApiKey();

        BeanUtils.copyProperties(dto, existingConfig);
        existingConfig.setUpdatedAt(LocalDateTime.now());

        // 如果前端传来的 apiKey 为空或是脱敏值，保留原来的 API Key
        if (dto.getApiKey() == null || dto.getApiKey().isEmpty() || dto.getApiKey().contains("****")) {
            existingConfig.setApiKey(originalApiKey);
        }

        aiModelConfigMapper.updateById(existingConfig);

        // 清除该模型的 VectorStore 缓存（因为向量模型可能已改变）
        // TODO: 需要查看当前模型是否已被数据库绑定
        // VectorStoreStrategyFactory factory = vectorStoreStrategyFactoryProvider.getIfAvailable();
        // if (factory != null) {
        //     factory.getStrategy().clearCache();
        // }

        log.info("更新模型配置成功, ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteModelConfig(Long id) {
        AiModelConfig config = aiModelConfigMapper.selectById(id);
        if (config == null) {
            throw new IllegalArgumentException("模型配置不存在, ID: " + id);
        }

        aiModelConfigMapper.deleteById(id);

        // 清除该模型的 VectorStore 缓存
        // TODO: 需要查看当前模型是否已被数据库绑定
        // VectorStoreStrategyFactory factory = vectorStoreStrategyFactoryProvider.getIfAvailable();
        // if (factory != null) {
        //     factory.getStrategy().clearCache();
        // }

        log.info("删除模型配置成功, ID: {}", id);
    }

    @Override
    public AiModelConfig getModelConfigById(Long id) {
        AiModelConfig config = aiModelConfigMapper.selectById(id);
        if (config == null) {
            throw new IllegalArgumentException("模型配置不存在, ID: " + id);
        }
        return config;
    }

    @Override
    public List<AiModelConfigDTO> getAllModelConfigs() {
        List<AiModelConfig> configs = aiModelConfigMapper.selectList(new LambdaQueryWrapper<AiModelConfig>()
                .eq(AiModelConfig::getUserId, StpUtil.getLoginIdAsLong()));
        return configs.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AiModelConfigDTO> getEnabledModelConfigs() {
        List<AiModelConfig> configs = aiModelConfigMapper.selectEnabledModels();
        return configs.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public AiModelConfig getDefaultModelConfig() {
        // 如果没有默认模型,返回第一个启用的模型
        List<AiModelConfig> enabledModels = aiModelConfigMapper.selectEnabledModels();
        if (enabledModels.isEmpty()) {
            log.warn("没有可用的AI模型配置,请在管理页面添加模型");
            return null;
        }
        AiModelConfig config = enabledModels.get(0);
        log.warn("使用的向量模型: {}", config.getModelName());
        return config;
    }

    @Override
    public AiModelConfig getEnabledModelConfigByType(String modelType) {
        AiModelConfig config = aiModelConfigMapper.selectEnabledModelByType(modelType);
        if (config == null) {
            log.warn("没有启用的{}模型配置,请在管理页面添加模型", modelType);
            return null;
        }
        return config;
    }

    @Override
    public List<AiModelConfigDTO> getAvailableChatModelConfigs() {
        return list(new LambdaQueryWrapper<AiModelConfig>()
                .eq(AiModelConfig::getModelType, ApiConfigModelType.CHAT)
                .eq(AiModelConfig::getUserId, StpUtil.getLoginIdAsLong()))
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AiModelConfigDTO> getAvailableEmbeddingModelConfigs() {
        return list(new LambdaQueryWrapper<AiModelConfig>()
                .eq(AiModelConfig::getModelType, ApiConfigModelType.EMBEDDING)
                .eq(AiModelConfig::getUserId, StpUtil.getLoginIdAsLong()))
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AiModelConfigDTO> getAvailableVisionModelConfigs() {
        return list(new LambdaQueryWrapper<AiModelConfig>()
                .eq(AiModelConfig::getModelType, ApiConfigModelType.VISION)
                .eq(AiModelConfig::getUserId, StpUtil.getLoginIdAsLong()))
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 实体转DTO(返回脱敏的API Key)
     */
    private AiModelConfigDTO convertToDTO(AiModelConfig config) {
        AiModelConfigDTO dto = new AiModelConfigDTO();
        BeanUtils.copyProperties(config, dto);
        // 返回脱敏的API Key，显示前后部分，中间用星号代替
        if (config.getApiKey() != null && !config.getApiKey().isEmpty()) {
            dto.setApiKey(maskApiKey(config.getApiKey()));
        }
        return dto;
    }

    /**
     * 脱敏API Key，保留前4位和后4位
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return "****";
        }
        String prefix = apiKey.substring(0, Math.min(4, apiKey.length()));
        String suffix = apiKey.substring(Math.max(0, apiKey.length() - 4));
        return prefix + "****" + suffix;
    }
}
