package com.shujichen.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shujichen.rag.entity.AiModelConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * AI模型配置Mapper接口
 */
@Mapper
public interface AiModelConfigMapper extends BaseMapper<AiModelConfig> {

    /**
     * 获取所有向量模型
     *
     * @return 向量模型列表
     */
    @Select("SELECT * FROM ai_model_config where model_type = 'EMBEDDING'")
    List<AiModelConfig> selectEnabledModels();

    /**
     * 根据模型类型获取模型配置
     *
     * @param modelType 模型类型
     * @return 模型配置
     */
    @Select("SELECT * FROM ai_model_config WHERE model_type = #{modelType} LIMIT 1")
    AiModelConfig selectEnabledModelByType(String modelType);
}
