package com.shujichen.rag.factory;

import com.shujichen.rag.config.properties.VectorStoreProperties;
import com.shujichen.rag.vector.VectorStoreStrategy;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 向量库策略工厂
 * 根据配置自动选择对应的向量库策略
 */
@Slf4j
@Component
public class VectorStoreStrategyFactory {

    private final VectorStoreProperties properties;
    private final List<VectorStoreStrategy> strategies;
    private Map<String, VectorStoreStrategy> strategyMap;

    public VectorStoreStrategyFactory(VectorStoreProperties properties,
                                      List<VectorStoreStrategy> strategies) {
        this.properties = properties;
        this.strategies = strategies;
    }

    @PostConstruct
    public void init() {
        strategyMap = strategies.stream()
                .collect(Collectors.toMap(VectorStoreStrategy::getType, Function.identity()));
        log.info("已注册向量库策略: {}", strategyMap.keySet());
        log.info("当前使用的向量库类型: [{}]", properties.getType());
    }

    /**
     * 获取当前配置对应的向量库策略
     *
     * @return 向量库策略实例
     * @throws IllegalArgumentException 如果配置的类型不支持
     */
    public VectorStoreStrategy getStrategy() {
        String type = properties.getType();
        VectorStoreStrategy strategy = strategyMap.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("不支持的向量库类型: " + type
                    + "，支持的类型: " + strategyMap.keySet());
        }
        return strategy;
    }

    /**
     * 获取当前向量库类型
     *
     * @return 向量库类型
     */
    public String getCurrentType() {
        return properties.getType();
    }
}
