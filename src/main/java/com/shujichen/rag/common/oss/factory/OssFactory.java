package com.shujichen.rag.common.oss.factory;

import com.shujichen.rag.common.constant.OssConstant;
import com.shujichen.rag.common.oss.core.OssClient;
import com.shujichen.rag.common.oss.exception.OssException;
import com.shujichen.rag.common.oss.properties.OssProperties;
import com.shujichen.rag.common.util.CacheUtils;
import com.shujichen.rag.common.util.JsonUtils;
import com.shujichen.rag.common.util.RedisUtils;
import com.shujichen.rag.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 文件上传Factory
 */
@Slf4j
public class OssFactory {

    private static final Map<String, OssClient> CLIENT_CACHE = new ConcurrentHashMap<>();
    private static final ReentrantLock LOCK = new ReentrantLock();

    /**
     * 获取默认实例
     */
    public static OssClient instance() {
        // 获取redis 默认类型
        String configKey = RedisUtils.getCacheObject(OssConstant.DEFAULT_CONFIG_KEY);
        if (StringUtils.isEmpty(configKey)) {
            throw new OssException("文件存储服务类型无法找到!");
        }
        return instance(configKey);
    }

    /**
     * 根据类型获取实例
     */
    public static OssClient instance(String configKey) {
        String json = CacheUtils.get(OssConstant.OSS_CONFIG, configKey);
        if (json == null) {
            throw new OssException("系统异常, '" + configKey + "'配置信息不存在!");
        }
        OssProperties properties = JsonUtils.parseObject(json, OssProperties.class);
        OssClient client = CLIENT_CACHE.get(configKey);

        // 客户端不存在或配置不相同则重新构建
        if (client == null || !client.checkPropertiesSame(properties)) {
            LOCK.lock();
            try {
                client = CLIENT_CACHE.get(configKey);
                if (client == null || !client.checkPropertiesSame(properties)) {
                    CLIENT_CACHE.put(configKey, new OssClient(configKey, properties));
                    log.info("创建OSS实例 key => {}", configKey);
                    return CLIENT_CACHE.get(configKey);
                }
            } finally {
                LOCK.unlock();
            }
        }
        return client;
    }

}
