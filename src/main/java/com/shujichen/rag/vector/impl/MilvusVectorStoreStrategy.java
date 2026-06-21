package com.shujichen.rag.vector.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shujichen.rag.config.properties.VectorStoreProperties;
import com.shujichen.rag.entity.DocumentChunk;
import com.shujichen.rag.entity.KnowledgeBase;
import com.shujichen.rag.factory.EmbeddingModelFactory;
import com.shujichen.rag.mapper.DocumentChunkMapper;
import com.shujichen.rag.service.AiModelConfigService;
import com.shujichen.rag.vector.AbstractVectorStoreStrategy;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import io.milvus.param.collection.DropCollectionParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Milvus 向量库策略实现
 */
@Slf4j
@Component
public class MilvusVectorStoreStrategy extends AbstractVectorStoreStrategy {

    private final DocumentChunkMapper documentChunkMapper;
    private volatile MilvusServiceClient milvusClient;

    public MilvusVectorStoreStrategy(VectorStoreProperties properties,
                                     EmbeddingModelFactory embeddingModelFactory,
                                     AiModelConfigService aiModelConfigService,
                                     DocumentChunkMapper documentChunkMapper) {
        super(properties, embeddingModelFactory, aiModelConfigService);
        this.documentChunkMapper = documentChunkMapper;
    }

    @Override
    public String getType() {
        return "milvus";
    }

    /**
     * 获取 Milvus 客户端（懒加载 + 双重检查锁）
     */
    private MilvusServiceClient getMilvusClient() {
        if (milvusClient == null) {
            synchronized (this) {
                if (milvusClient == null) {
                    VectorStoreProperties.Milvus config = properties.getMilvus();
                    ConnectParam.Builder connectBuilder = ConnectParam.newBuilder()
                            .withHost(config.getHost())
                            .withPort(config.getPort());

                    // 如果配置了用户名密码，则添加认证
                    if (config.getUsername() != null && !config.getUsername().isBlank()) {
                        connectBuilder.withAuthorization(config.getUsername(), config.getPassword());
                    }

                    milvusClient = new MilvusServiceClient(connectBuilder.build());
                    log.info("Milvus 客户端初始化成功: {}:{}", config.getHost(), config.getPort());
                }
            }
        }
        return milvusClient;
    }

    @Override
    protected VectorStore createVectorStore(Long embeddingModelId, String collectionName) {
        EmbeddingModel embeddingModel = getEmbeddingModel(embeddingModelId);
        int dimension = embeddingModel.dimensions();

        log.info("创建 Milvus VectorStore - collection: {}, 维度: {}", collectionName, dimension);

        MilvusVectorStore vectorStore = MilvusVectorStore.builder(getMilvusClient(), embeddingModel)
                .databaseName(properties.getMilvus().getDatabaseName())
                .collectionName(collectionName)
                .embeddingDimension(dimension)
                .initializeSchema(true)
                .build();

        // 调用 afterPropertiesSet() 触发 Milvus collection 的创建
        // 注意：新创建的 collection 没有索引，会有 "index not found" 的错误日志，这是正常的
        // 索引会在第一次插入数据时自动创建
        try {
            vectorStore.afterPropertiesSet();
        } catch (Exception e) {
            // 忽略索引检查相关的错误，这不影响功能
            log.debug("VectorStore 初始化警告（可忽略）: {}", e.getMessage());
        }

        log.info("Milvus collection 创建成功: {}", collectionName);
        vectorStoreCache.put(collectionName, vectorStore);
        return vectorStore;
    }

    @Override
    public void storeChunkVectors(KnowledgeBase knowledgeBase, Long documentId, List<DocumentChunk> chunks) {
        VectorStore vectorStore = getVectorStore(knowledgeBase);
        if (vectorStore == null) {
            log.warn("VectorStore 不可用，跳过向量存储，documentId: {}, collection: {}",
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

        log.info("Milvus 向量存储成功，documentId: {}, collection: {}, 分块数量: {}, 批次数: {}",
                documentId, knowledgeBase.getCollectionsName(), chunks.size(), totalBatches);
    }

    @Override
    public void deleteDocumentVectors(KnowledgeBase knowledgeBase, Long documentId) {
        VectorStore vectorStore = getVectorStore(knowledgeBase);
        if (vectorStore == null) {
            log.warn("VectorStore 不可用，跳过向量删除，documentId: {}, collection: {}",
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
            log.info("Milvus 向量删除成功，documentId: {}, collection: {}, 删除数量: {}",
                    documentId, knowledgeBase.getCollectionsName(), chunkIds.size());
        } catch (Exception e) {
            log.error("删除文档向量失败，documentId: {}, collection: {}",
                    documentId, knowledgeBase.getCollectionsName(), e);
            throw new RuntimeException("删除文档向量失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Document> searchSimilar(KnowledgeBase knowledgeBase, String query, int topK) {
        VectorStore vectorStore = getVectorStore(knowledgeBase);
        if (vectorStore == null) {
            log.warn("VectorStore 不可用，跳过向量搜索，collection: {}, query: {}",
                    knowledgeBase.getCollectionsName(), query);
            return Collections.emptyList();
        }

        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .build();

            List<Document> results = vectorStore.similaritySearch(searchRequest);
            log.info("Milvus 向量搜索成功，collection: {}, query: {}, 结果数量: {}",
                    knowledgeBase.getCollectionsName(), query, results.size());
            return results;

        } catch (Exception e) {
            log.error("向量搜索失败，collection: {}, query: {}",
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

            // 删除 Milvus collection
            DropCollectionParam dropParam = DropCollectionParam.newBuilder()
                    .withDatabaseName(properties.getMilvus().getDatabaseName())
                    .withCollectionName(collectionName)
                    .build();

            var response = getMilvusClient().dropCollection(dropParam);
            if (response.getStatus() == 0) {
                log.info("Milvus collection 删除成功: {}", collectionName);
                return true;
            } else {
                log.error("删除 collection 失败: {}, 错误: {}", collectionName, response.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("删除 collection 异常: {}", collectionName, e);
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
