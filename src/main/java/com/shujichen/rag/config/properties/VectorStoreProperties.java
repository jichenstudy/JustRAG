package com.shujichen.rag.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 向量库配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "vector-store")
public class VectorStoreProperties {

    /**
     * 向量库类型
     */
    private String type = "milvus";

    /**
     * 全局 Embedding API 批量大小
     * 受限于 DashScope 等 Embedding API 的限制，单次最多处理 10 个文本
     * 建议设置为 8，预留一定的安全余量
     */
    private int embeddingBatchSize = 8;

    /**
     * Milvus 配置
     */
    private Milvus milvus = new Milvus();

    /**
     * PostgreSQL 配置
     */
    private PostgreSQL postgresql = new PostgreSQL();

    /**
     * Elasticsearch 配置
     */
    private Elasticsearch elasticsearch = new Elasticsearch();

    @Data
    public static class Milvus {
        private String host = "localhost";
        private int port = 19530;
        private String databaseName = "default";
        private String username;
        private String password;
    }

    @Data
    public static class PostgreSQL {
        private String host = "localhost";
        private int port = 5432;
        private String database = "just_rag";
        private String username = "postgres";
        private String password = "postgres";
        private String schema = "public";
        private String table = "vector_store";
        private int maxDocumentSize = 100;
    }

    @Data
    public static class Elasticsearch {
        private String hosts = "http://localhost:9200";
        private String indexName = "just_rag_vectors";
        private String username;
        private String password;
        private String apiKey;
        private boolean sslEnabled = false;
    }
}
