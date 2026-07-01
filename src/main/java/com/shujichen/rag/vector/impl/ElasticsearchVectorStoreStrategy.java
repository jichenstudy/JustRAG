package com.shujichen.rag.vector.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shujichen.rag.config.properties.VectorStoreProperties;
import com.shujichen.rag.entity.DocumentChunk;
import com.shujichen.rag.entity.KnowledgeBase;
import com.shujichen.rag.factory.EmbeddingModelFactory;
import com.shujichen.rag.mapper.DocumentChunkMapper;
import com.shujichen.rag.mapper.DocumentMapper;
import com.shujichen.rag.service.AiModelConfigService;
import com.shujichen.rag.vector.AbstractVectorStoreStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStoreOptions;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch 向量库策略实现
 */
@Slf4j
@Component
public class ElasticsearchVectorStoreStrategy extends AbstractVectorStoreStrategy {

    private final DocumentChunkMapper documentChunkMapper;
    private final DocumentMapper documentMapper;
    private volatile ElasticsearchClient elasticsearchClient;
    private volatile ElasticsearchTransport transport;
    private volatile RestClient restClient;

    public ElasticsearchVectorStoreStrategy(VectorStoreProperties properties,
                                            EmbeddingModelFactory embeddingModelFactory,
                                            AiModelConfigService aiModelConfigService,
                                            DocumentChunkMapper documentChunkMapper,
                                            DocumentMapper documentMapper) {
        super(properties, embeddingModelFactory, aiModelConfigService);
        this.documentChunkMapper = documentChunkMapper;
        this.documentMapper = documentMapper;
    }

    @Override
    public String getType() {
        return "elasticsearch";
    }

    /**
     * 获取 Elasticsearch 客户端（懒加载 + 双重检查锁）
     */
    private ElasticsearchClient getElasticsearchClient() {
        if (elasticsearchClient == null) {
            synchronized (this) {
                if (elasticsearchClient == null) {
                    VectorStoreProperties.Elasticsearch config = properties.getElasticsearch();

                    // 解析 hosts（支持多个节点，用逗号分隔）
                    String[] hostArray = config.getHosts().split(",");
                    HttpHost[] httpHosts = new HttpHost[hostArray.length];
                    for (int i = 0; i < hostArray.length; i++) {
                        String host = hostArray[i].trim();
                        if (host.startsWith("http://") || host.startsWith("https://")) {
                            httpHosts[i] = HttpHost.create(host);
                        } else {
                            // 默认使用 http
                            httpHosts[i] = new HttpHost(host, 9200, "http");
                        }
                    }

                    // 构建 RestClient
                    RestClientBuilder builder = RestClient.builder(httpHosts);

                    // 如果配置了认证
                    if (config.getUsername() != null && !config.getUsername().isBlank()) {
                        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                        credentialsProvider.setCredentials(AuthScope.ANY,
                                new UsernamePasswordCredentials(config.getUsername(), config.getPassword()));
                        builder.setHttpClientConfigCallback(httpClientBuilder ->
                                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
                    }

                    // 如果配置了 API Key
                    if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
                        builder.setDefaultHeaders(new org.apache.http.Header[]{
                                new org.apache.http.message.BasicHeader("Authorization", "ApiKey " + config.getApiKey())
                        });
                    }

                    restClient = builder.build();

                    // 创建 Transport 和 Client
                    JacksonJsonpMapper jsonpMapper = new JacksonJsonpMapper();
                    transport = new RestClientTransport(restClient, jsonpMapper);
                    elasticsearchClient = new ElasticsearchClient(transport);

                    log.info("Elasticsearch 客户端初始化成功: {}", config.getHosts());
                }
            }
        }
        return elasticsearchClient;
    }

    @Override
    protected VectorStore createVectorStore(Long embeddingModelId, String collectionName) {
        EmbeddingModel embeddingModel = getEmbeddingModel(embeddingModelId);
        int dimension = embeddingModel.dimensions();

        log.info("创建 Elasticsearch VectorStore - index: {}, 维度: {}", collectionName, dimension);

        // 创建 ElasticsearchVectorStore 配置
        ElasticsearchVectorStoreOptions options = new ElasticsearchVectorStoreOptions();
        options.setIndexName(collectionName);  // 每个知识库一个索引
        options.setDimensions(dimension);

        // 确保客户端已初始化
        getElasticsearchClient();

        ElasticsearchVectorStore vectorStore = ElasticsearchVectorStore.builder(restClient, embeddingModel)
                .options(options)
                .initializeSchema(true)
                .build();

        // 初始化索引
        try {
            vectorStore.afterPropertiesSet();
            log.info("Elasticsearch 索引创建成功: {}", collectionName);
        } catch (Exception e) {
            log.error("Elasticsearch 索引初始化失败: {}", collectionName, e);
            throw new RuntimeException("Elasticsearch 索引初始化失败: " + e.getMessage(), e);
        }

        vectorStoreCache.put(collectionName, vectorStore);
        return vectorStore;
    }

    @Override
    public void storeChunkVectors(KnowledgeBase knowledgeBase, Long documentId, List<DocumentChunk> chunks) {
        VectorStore vectorStore = getVectorStore(knowledgeBase);
        if (vectorStore == null) {
            log.warn("VectorStore 不可用，跳过向量存储，documentId: {}, index: {}",
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

        log.info("Elasticsearch 向量存储成功，documentId: {}, index: {}, 分块数量: {}, 批次数: {}",
                documentId, knowledgeBase.getCollectionsName(), chunks.size(), totalBatches);
    }

    @Override
    public void deleteDocumentVectors(KnowledgeBase knowledgeBase, Long documentId) {
        VectorStore vectorStore = getVectorStore(knowledgeBase);
        if (vectorStore == null) {
            log.warn("VectorStore 不可用，跳过向量删除，documentId: {}, index: {}",
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
            log.info("Elasticsearch 向量删除成功，documentId: {}, index: {}, 删除数量: {}",
                    documentId, knowledgeBase.getCollectionsName(), chunkIds.size());
        } catch (Exception e) {
            log.error("删除文档向量失败，documentId: {}, index: {}",
                    documentId, knowledgeBase.getCollectionsName(), e);
            throw new RuntimeException("删除文档向量失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Document> searchSimilar(KnowledgeBase knowledgeBase, String query, int topK) {
        VectorStore vectorStore = getVectorStore(knowledgeBase);
        if (vectorStore == null) {
            log.warn("VectorStore 不可用，跳过向量搜索，index: {}, query: {}",
                    knowledgeBase.getCollectionsName(), query);
            return Collections.emptyList();
        }

        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .build();

            List<Document> results = vectorStore.similaritySearch(searchRequest);
            log.info("Elasticsearch 向量搜索成功，index: {}, query: {}, 结果数量: {}",
                    knowledgeBase.getCollectionsName(), query, results.size());
            return results;

        } catch (Exception e) {
            log.error("向量搜索失败，index: {}, query: {}",
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

            // 删除 Elasticsearch 索引
            getElasticsearchClient().indices().delete(req -> req.index(collectionName));

            log.info("Elasticsearch 索引删除成功: {}", collectionName);
            return true;
        } catch (Exception e) {
            log.error("删除 Elasticsearch 索引异常: {}", collectionName, e);
            return false;
        }
    }

    /**
     * 构建 Document 列表
     */
    private List<Document> buildDocuments(Long documentId, KnowledgeBase knowledgeBase, List<DocumentChunk> chunks) {
        // 查询文档信息以获取文档名
        String documentName = getDocumentName(documentId);
        
        return chunks.stream().map(chunk -> {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("documentId", documentId);
            metadata.put("documentName", documentName);
            metadata.put("chunkId", chunk.getId());
            metadata.put("chunkIndex", chunk.getChunkIndex());
            metadata.put("knowledgeBaseId", knowledgeBase.getId());
            return new Document(String.valueOf(chunk.getId()), chunk.getContent(), metadata);
        }).toList();
    }
    
    /**
     * 获取文档名称
     */
    private String getDocumentName(Long documentId) {
        try {
            com.shujichen.rag.entity.Document document = documentMapper.selectById(documentId);
            return document != null ? document.getName() : null;
        } catch (Exception e) {
            log.warn("查询文档名称失败，documentId: {}", documentId, e);
            return null;
        }
    }
}
