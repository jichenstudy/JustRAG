package com.shujichen.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shujichen.rag.entity.OssConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 对象存储配置 Mapper
 */
@Mapper
public interface OssConfigMapper extends BaseMapper<OssConfig> {

    /**
     * 查询默认配置
     */
    @Select("SELECT * FROM oss_config WHERE status = '0' LIMIT 1")
    OssConfig selectDefault();

    /**
     * 根据 configKey 查询配置
     */
    @Select("SELECT * FROM oss_config WHERE config_key = #{configKey}")
    OssConfig selectByConfigKey(@Param("configKey") String configKey);

    /**
     * 重置所有配置为非默认
     */
    @Update("UPDATE oss_config SET status = '1'")
    int resetAllStatus();

    /**
     * 设置指定配置为默认
     */
    @Update("UPDATE oss_config SET status = '0' WHERE oss_config_id = #{ossConfigId}")
    int setDefaultStatus(@Param("ossConfigId") Long ossConfigId);
}
