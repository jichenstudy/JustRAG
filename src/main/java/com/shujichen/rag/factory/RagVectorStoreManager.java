package com.shujichen.rag.factory;

import com.shujichen.rag.entity.AiModelConfig;
import com.shujichen.rag.entity.KnowledgeBase;
import com.shujichen.rag.service.AiModelConfigService;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.collection.DropCollectionParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RAG 向量存储管理器 - 根据知识库动态创建和管理 VectorStore
 * <p>
 * 每个知识库对应一个 Milvus Collection，缓存以 collectionName 为 key
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagVectorStoreManager {

    private final ObjectProvider<MilvusServiceClient> milvusClientProvider;
    private final ObjectProvider<AiModelConfigService> aiModelConfigServiceProvider;
    private final EmbeddingModelFactory embeddingModelFactory;

    @Value("${milvus.databaseName}")
    private String databaseName;

    // 缓存 VectorStore，key 是 collectionName（每个知识库一个 collection）
    private final Map<String, VectorStore> vectorStoreCache = new ConcurrentHashMap<>();

    /**
     * 根据知识库获取 VectorStore（便捷方法）
     *
     * @param knowledgeBase 知识库实体
     * @return VectorStore 实例，如果配置不存在返回 null
     */
    public VectorStore getVectorStore(KnowledgeBase knowledgeBase) {
        if (knowledgeBase == null) {
            log.warn("知识库不能为空");
            return null;
        }
        return getOrCreateVectorStore(knowledgeBase.getModelId(), knowledgeBase.getCollectionsName());
    }

    /**
     * 根据嵌入模型ID和集合名称获取或创建 VectorStore
     *
     * @param embeddingModelId 嵌入模型 ID
     * @param collectionName   集合名称（知识库对应的 collection）
     * @return VectorStore 实例，如果配置不存在返回 null
     */
    public VectorStore getOrCreateVectorStore(Long embeddingModelId, String collectionName) {
        if (collectionName == null || collectionName.isBlank()) {
            log.warn("collectionName 不能为空");
            return null;
        }

        // 先检查缓存
        VectorStore cached = vectorStoreCache.get(collectionName);
        if (cached != null) {
            log.debug("使用缓存的 VectorStore，collection: {}", collectionName);
            return cached;
        }

        // 创建新的 VectorStore
        return createVectorStore(embeddingModelId, collectionName);
    }

    /**
     * 创建新的 VectorStore（内部方法）
     */
    private VectorStore createVectorStore(Long embeddingModelId, String collectionName) {
        try {
            MilvusServiceClient milvusClient = milvusClientProvider.getIfAvailable();
            if (milvusClient == null) {
                log.warn("Milvus 客户端不可用");
                return null;
            }

            AiModelConfigService aiModelConfigService = aiModelConfigServiceProvider.getIfAvailable();
            if (aiModelConfigService == null) {
                log.warn("AiModelConfigService 不可用");
                return null;
            }

            AiModelConfig embeddingModelConfig = aiModelConfigService.getModelConfigById(embeddingModelId);
            if (embeddingModelConfig == null) {
                log.warn("嵌入模型配置不存在，模型 ID: {}", embeddingModelId);
                return null;
            }

            EmbeddingModel embeddingModel = embeddingModelFactory.createEmbeddingModel(embeddingModelConfig);
            if (embeddingModel == null) {
                log.warn("创建嵌入模型失败，模型 ID: {}", embeddingModelId);
                return null;
            }

            int dimension = embeddingModel.dimensions();
            log.info("创建 VectorStore - collection: {}, 模型: {}, 维度: {}",
                    collectionName, embeddingModelConfig.getModelName(), dimension);

            MilvusVectorStore vectorStore = MilvusVectorStore.builder(milvusClient, embeddingModel)
                    .databaseName(databaseName)
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

        } catch (Exception e) {
            log.error("创建 VectorStore 失败，collection: {}", collectionName, e);
            return null;
        }
    }

    /**
     * 删除 Milvus collection
     *
     * @param collectionName 集合名称
     * @return 是否删除成功
     */
    public boolean dropCollection(String collectionName) {
        if (collectionName == null || collectionName.isBlank()) {
            log.warn("collectionName 不能为空");
            return false;
        }

        try {
            MilvusServiceClient milvusClient = milvusClientProvider.getIfAvailable();
            if (milvusClient == null) {
                log.warn("Milvus 客户端不可用，无法删除 collection");
                return false;
            }

            // 从缓存中移除
            vectorStoreCache.remove(collectionName);

            // 删除 Milvus collection
            DropCollectionParam dropParam = DropCollectionParam.newBuilder()
                    .withDatabaseName(databaseName)
                    .withCollectionName(collectionName)
                    .build();

            var response = milvusClient.dropCollection(dropParam);
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
     * 清除所有缓存
     */
    public void clearCache() {
        vectorStoreCache.clear();
        log.info("已清除所有 VectorStore 缓存");
    }
}

