package com.shujichen.rag.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shujichen.rag.entity.OssConfig;

import java.util.List;

/**
 * 对象存储配置 Service
 */
public interface OssConfigService extends IService<OssConfig> {

    /**
     * 分页查询配置
     */
    IPage<OssConfig> selectPage(Integer page, Integer pageSize, String configKey);

    /**
     * 查询所有配置
     */
    List<OssConfig> selectList();

    /**
     * 根据主键查询配置
     */
    OssConfig selectById(Long ossConfigId);

    /**
     * 根据 configKey 查询配置
     */
    OssConfig selectByConfigKey(String configKey);

    /**
     * 查询默认配置
     */
    OssConfig selectDefault();

    /**
     * 新增配置
     */
    int insert(OssConfig ossConfig);

    /**
     * 修改配置
     */
    int update(OssConfig ossConfig);

    /**
     * 删除配置
     */
    int deleteById(Long ossConfigId);

    /**
     * 设置默认配置
     */
    int setDefault(Long ossConfigId);
}
