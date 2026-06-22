package com.shujichen.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shujichen.rag.common.constant.OssConstant;
import com.shujichen.rag.common.util.CacheUtils;
import com.shujichen.rag.common.util.JsonUtils;
import com.shujichen.rag.common.util.RedisUtils;
import com.shujichen.rag.entity.OssConfig;
import com.shujichen.rag.mapper.OssConfigMapper;
import com.shujichen.rag.service.OssConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对象存储配置 Service 实现类
 */
@Service
public class OssConfigServiceImpl extends ServiceImpl<OssConfigMapper, OssConfig>
        implements OssConfigService {

    @Override
    public IPage<OssConfig> selectPage(Integer page, Integer pageSize, String configKey) {
        LambdaQueryWrapper<OssConfig> wrapper = new LambdaQueryWrapper<>();
        if (configKey != null && !configKey.isEmpty()) {
            wrapper.like(OssConfig::getConfigKey, configKey);
        }
        wrapper.orderByDesc(OssConfig::getCreatedAt);
        return page(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public List<OssConfig> selectList() {
        return list();
    }

    @Override
    public OssConfig selectById(Long ossConfigId) {
        return getById(ossConfigId);
    }

    @Override
    public OssConfig selectByConfigKey(String configKey) {
        return getOne(new LambdaQueryWrapper<OssConfig>()
                .eq(OssConfig::getConfigKey, configKey));
    }

    @Override
    public OssConfig selectDefault() {
        return getOne(new LambdaQueryWrapper<OssConfig>()
                .eq(OssConfig::getStatus, "0")
                .last("LIMIT 1"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insert(OssConfig ossConfig) {
        // 设置创建时间
        ossConfig.setCreatedAt(LocalDateTime.now());
        // 如果设置为默认，先重置其他配置
        if ("0".equals(ossConfig.getStatus())) {
            baseMapper.resetAllStatus();
        }
        int result = baseMapper.insert(ossConfig);
        // 更新缓存
        if (result > 0) {
            updateCache(ossConfig);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int update(OssConfig ossConfig) {
        // 如果设置为默认，先重置其他配置
        if ("0".equals(ossConfig.getStatus())) {
            baseMapper.resetAllStatus();
        }
        // 设置更新时间
        ossConfig.setUpdatedAt(LocalDateTime.now());
        int result = baseMapper.updateById(ossConfig);
        // 更新缓存
        if (result > 0) {
            OssConfig updatedConfig = getById(ossConfig.getOssConfigId());
            if (updatedConfig != null) {
                updateCache(updatedConfig);
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteById(Long ossConfigId) {
        OssConfig config = getById(ossConfigId);
        int result = baseMapper.deleteById(ossConfigId);
        // 删除缓存
        if (result > 0 && config != null) {
            CacheUtils.evict(OssConstant.OSS_CONFIG, config.getConfigKey());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int setDefault(Long ossConfigId) {
        // 先将所有配置设为非默认
        baseMapper.resetAllStatus();
        // 再将指定配置设为默认
        int result = baseMapper.setDefaultStatus(ossConfigId);
        // 更新默认配置缓存
        if (result > 0) {
            OssConfig config = getById(ossConfigId);
            if (config != null) {
                RedisUtils.setCacheObject(OssConstant.DEFAULT_CONFIG_KEY, config.getConfigKey());
                updateCache(config);
            }
        }
        return result;
    }

    /**
     * 更新缓存
     */
    private void updateCache(OssConfig config) {
        String configKey = config.getConfigKey();
        // 如果是默认配置，更新默认配置缓存
        if ("0".equals(config.getStatus())) {
            RedisUtils.setCacheObject(OssConstant.DEFAULT_CONFIG_KEY, configKey);
        }
        // 更新配置缓存
        CacheUtils.put(OssConstant.OSS_CONFIG, configKey, JsonUtils.toJsonString(config));
    }
}
