package com.shujichen.rag.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO配置类
 */
@Configuration
public class MinIOConfig {

    /**
     * MinIO服务端点
     */
    @Value("${minio.endpoint}")
    private String endpoint;

    /**
     * MinIO访问密钥
     */
    @Value("${minio.access-key}")
    private String accessKey;

    /**
     * MinIO秘密密钥
     */
    @Value("${minio.secret-key}")
    private String secretKey;

    /**
     * 创建MinIO客户端
     *
     * @return MinIO客户端实例
     */
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}