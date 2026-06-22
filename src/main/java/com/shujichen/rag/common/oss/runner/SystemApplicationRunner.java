package com.shujichen.rag.common.oss.runner;

import com.shujichen.rag.common.constant.OssConstant;
import com.shujichen.rag.common.util.CacheUtils;
import com.shujichen.rag.common.util.JsonUtils;
import com.shujichen.rag.common.util.RedisUtils;
import com.shujichen.rag.entity.OssConfig;
import com.shujichen.rag.service.OssConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 初始化 system 模块对应业务数据
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SystemApplicationRunner implements ApplicationRunner {

    private final OssConfigService ossConfigService;

    @Override
    public void run(ApplicationArguments args) {
        List<OssConfig> list = ossConfigService.list();
        // 加载OSS初始化配置
        if (!list.isEmpty()) {
            for (OssConfig config : list) {
                String configKey = config.getConfigKey();
                if ("0".equals(config.getStatus())) {
                    RedisUtils.setCacheObject(OssConstant.DEFAULT_CONFIG_KEY, configKey);
                }
                CacheUtils.put(OssConstant.OSS_CONFIG, config.getConfigKey(), JsonUtils.toJsonString(config));
            }
            log.info("初始化OSS配置成功");
        } else {
            log.warn("未找到OSS配置");
        }
    }

}
