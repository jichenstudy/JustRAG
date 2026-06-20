package com.shujichen.rag.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Milvus 配置 - 仅负责初始化 Milvus 客户端连接
 * <p>
 * VectorStore 的创建由 RagVectorStoreManager 根据知识库动态管理
 */
@Slf4j
@Configuration
public class MilvusConfig {

    @Value("${milvus.client.host}")
    private String milvusHost;

    @Value("${milvus.client.port}")
    private int milvusPort;

    /**
     * 创建 Milvus 客户端
     */
    @Bean
    public MilvusServiceClient milvusClient() {
        log.info("初始化 Milvus 客户端: {}:{}", milvusHost, milvusPort);

        ConnectParam connectConfig = ConnectParam.newBuilder()
                .withHost(milvusHost)
                .withPort(milvusPort)
                .build();

        return new MilvusServiceClient(connectConfig);
    }
}
