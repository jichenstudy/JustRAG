package com.shujichen.rag.vector.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shujichen.rag.config.properties.VectorStoreProperties;
import com.shujichen.rag.entity.DocumentChunk;
import com.shujichen.rag.entity.KnowledgeBase;
import com.shujichen.rag.factory.EmbeddingModelFactory;
import com.shujichen.rag.mapper.DocumentChunkMapper;
import com.shujichen.rag.service.AiModelConfigService;
import com.shujichen.rag.vector.AbstractVectorStoreStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PostgreSQL向量库策略实现
 */
@Slf4j
@Component
public class PostgreSQLVectorStoreStrategy extends AbstractVectorStoreStrategy {

    private final DocumentChunkMapper documentChunkMapper;
    private volatile DataSource dataSource;
    private volatile JdbcTemplate jdbcTemplate;

    public PostgreSQLVectorStoreStrategy(VectorStoreProperties properties,
                                         EmbeddingModelFactory embeddingModelFactory,
                                         AiModelConfigService aiModelConfigService,
                                         DocumentChunkMapper documentChunkMapper) {
        super(properties, embeddingModelFactory, aiModelConfigService);
        this.documentChunkMapper = documentChunkMapper;
    }

    @Override
    public String getType() {
        return "postgresql";
    }

    /**
     * 获取数据源（懒加载 + 双重检查锁）
     */
    private DataSource getDataSource() {
        if (dataSource == null) {
            synchronized (this) {
                if (dataSource == null) {
                    VectorStoreProperties.PostgreSQL config = properties.getPostgresql();
                    DriverManagerDataSource ds = new DriverManagerDataSource();
                    ds.setDriverClassName("org.postgresql.Driver");
                    ds.setUrl(String.format("jdbc:postgresql://%s:%d/%s",
                            config.getHost(), config.getPort(), config.getDatabase()));
                    ds.setUsername(config.getUsername());
                    ds.setPassword(config.getPassword());
                    dataSource = ds;
                    log.info("PostgreSQL 数据源初始化成功: {}:{}/{}",
                            config.getHost(), config.getPort(), config.getDatabase());
                }
            }
        }
        return dataSource;
    }

    /**
     * 获取 JdbcTemplate（懒加载 + 双重检查锁）
     */
    private JdbcTemplate getJdbcTemplate() {
        if (jdbcTemplate == null) {
            synchronized (this) {
                if (jdbcTemplate == null) {
                    jdbcTemplate = new JdbcTemplate(getDataSource());
                }
            }
        }
        return jdbcTemplate;
    }

    @Override
    protected VectorStore createVectorStore(Long embeddingModelId, String collectionName) {
        EmbeddingModel embeddingModel = getEmbeddingModel(embeddingModelId);
        int dimension = embeddingModel.dimensions();

        log.info("创建 PostgreSQL VectorStore - table: {}, 维度: {}", collectionName, dimension);

        // 使用 PgVectorStore 构建器
        PgVectorStore vectorStore = PgVectorStore.builder(getJdbcTemplate(), embeddingModel)
                .dimensions(dimension)
                .vectorTableName(collectionName)  // 每个知识库一张表
                .schemaName(properties.getPostgresql().getSchema())
                .maxDocumentBatchSize(properties.getPostgresql().getMaxDocumentSize())
                .idType(PgVectorStore.PgIdType.TEXT)  // 使用 TEXT 类型 ID，兼容雪花算法
                .initializeSchema(true)
                .build();

        // 初始化 schema（创建表和索引）
        try {
            vectorStore.afterPropertiesSet();
            log.info("PostgreSQL 向量表创建成功: {}", collectionName);
        } catch (Exception e) {
            log.error("PostgreSQL 向量表初始化失败: {}", collectionName, e);
            throw new RuntimeException("PostgreSQL 向量表初始化失败: " + e.getMessage(), e);
        }

        vectorStoreCache.put(collectionName, vectorStore);
        return vectorStore;
    }

    @Override
    public void storeChunkVectors(KnowledgeBase knowledgeBase, Long documentId, List<DocumentChunk> chunks) {
        VectorStore vectorStore = getVectorStore(knowledgeBase);
        if (vectorStore == null) {
            log.warn("VectorStore 不可用，跳过向量存储，documentId: {}, table: {}",
                    documentId, knowledgeBase.getCollectionsName());
            return;
        }

        if (chunks == null || chunks.isEmpty()) {
            log.warn("文档分块列表为空，跳过向量存储，documentId: {}", documentId);
            return;
        }

        List<Document> documents = buildDocuments(documentId, knowledgeBase, chunks);

        // DashScope Embedding API 限制一次最多处理 10 个文本，需要分批处理
        int batchSize = properties.getEmbeddingBatchSize();
        int totalBatches = (documents.size() + batchSize - 1) / batchSize;

        for (int i = 0; i < documents.size(); i += batchSize) {
            int end = Math.min(i + batchSize, documents.size());
            List<Document> batch = documents.subList(i, end);
            vectorStore.add(batch);
            int currentBatch = (i / batchSize) + 1;
            log.debug("向量存储批次 {}/{} 完成，documentId: {}, 本批数量: {}",
                    currentBatch, totalBatches, documentId, batch.size());
        }

        log.info("PostgreSQL 向量存储成功，documentId: {}, table: {}, 分块数量: {}, 批次数: {}",
                documentId, knowledgeBase.getCollectionsName(), chunks.size(), totalBatches);
    }

    @Override
    public void deleteDocumentVectors(KnowledgeBase knowledgeBase, Long documentId) {
        VectorStore vectorStore = getVectorStore(knowledgeBase);
        if (vectorStore == null) {
            log.warn("VectorStore 不可用，跳过向量删除，documentId: {}, table: {}",
                    documentId, knowledgeBase.getCollectionsName());
            return;
        }

        try {
            LambdaQueryWrapper<DocumentChunk> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(DocumentChunk::getDocumentId, documentId);
            List<DocumentChunk> chunks = documentChunkMapper.selectList(queryWrapper);

            if (chunks.isEmpty()) {
                log.info("文档没有分块，无需删除向量，documentId: {}", documentId);
                return;
            }

            List<String> chunkIds = chunks.stream()
                    .map(chunk -> String.valueOf(chunk.getId()))
                    .toList();

            vectorStore.delete(chunkIds);
            log.info("PostgreSQL 向量删除成功，documentId: {}, table: {}, 删除数量: {}",
                    documentId, knowledgeBase.getCollectionsName(), chunkIds.size());
        } catch (Exception e) {
            log.error("删除文档向量失败，documentId: {}, table: {}",
                    documentId, knowledgeBase.getCollectionsName(), e);
            throw new RuntimeException("删除文档向量失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Document> searchSimilar(KnowledgeBase knowledgeBase, String query, int topK) {
        VectorStore vectorStore = getVectorStore(knowledgeBase);
        if (vectorStore == null) {
            log.warn("VectorStore 不可用，跳过向量搜索，table: {}, query: {}",
                    knowledgeBase.getCollectionsName(), query);
            return Collections.emptyList();
        }

        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .build();

            List<Document> results = vectorStore.similaritySearch(searchRequest);
            log.info("PostgreSQL 向量搜索成功，table: {}, query: {}, 结果数量: {}",
                    knowledgeBase.getCollectionsName(), query, results.size());
            return results;

        } catch (Exception e) {
            log.error("向量搜索失败，table: {}, query: {}",
                    knowledgeBase.getCollectionsName(), query, e);
            throw new RuntimeException("向量搜索失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean dropCollection(String collectionName) {
        if (collectionName == null || collectionName.isBlank()) {
            log.warn("collectionName 不能为空");
            return false;
        }

        try {
            // 从缓存中移除
            vectorStoreCache.remove(collectionName);

            // 删除 PostgreSQL 表
            String schema = properties.getPostgresql().getSchema();
            String sql = String.format("DROP TABLE IF EXISTS %s.%s", schema, collectionName);
            getJdbcTemplate().execute(sql);

            log.info("PostgreSQL 向量表删除成功: {}", collectionName);
            return true;
        } catch (Exception e) {
            log.error("删除 PostgreSQL 向量表异常: {}", collectionName, e);
            return false;
        }
    }

    /**
     * 构建 Document 列表
     */
    private List<Document> buildDocuments(Long documentId, KnowledgeBase knowledgeBase, List<DocumentChunk> chunks) {
        return chunks.stream().map(chunk -> {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("documentId", documentId);
            metadata.put("chunkId", chunk.getId());
            metadata.put("chunkIndex", chunk.getChunkIndex());
            metadata.put("knowledgeBaseId", knowledgeBase.getId());
            return new Document(String.valueOf(chunk.getId()), chunk.getContent(), metadata);
        }).toList();
    }
}
