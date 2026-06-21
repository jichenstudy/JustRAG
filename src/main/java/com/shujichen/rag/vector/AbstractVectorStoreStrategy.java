package com.shujichen.rag.vector;

import com.shujichen.rag.config.properties.VectorStoreProperties;
import com.shujichen.rag.entity.AiModelConfig;
import com.shujichen.rag.entity.KnowledgeBase;
import com.shujichen.rag.factory.EmbeddingModelFactory;
import com.shujichen.rag.service.AiModelConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 向量库策略抽象基类
 * 提供公共方法：EmbeddingModel 获取、VectorStore 缓存管理等
 */
@Slf4j
public abstract class AbstractVectorStoreStrategy implements VectorStoreStrategy {

    protected final VectorStoreProperties properties;
    protected final EmbeddingModelFactory embeddingModelFactory;
    protected final AiModelConfigService aiModelConfigService;

    /**
     * VectorStore 缓存，key 为 collectionName
     */
    protected final Map<String, VectorStore> vectorStoreCache = new ConcurrentHashMap<>();

    protected AbstractVectorStoreStrategy(VectorStoreProperties properties,
                                          EmbeddingModelFactory embeddingModelFactory,
                                          AiModelConfigService aiModelConfigService) {
        this.properties = properties;
        this.embeddingModelFactory = embeddingModelFactory;
        this.aiModelConfigService = aiModelConfigService;
    }

    /**
     * 获取或创建 VectorStore（模板方法，子类可覆写）
     *
     * @param embeddingModelId 嵌入模型ID
     * @param collectionName   集合名称
     * @return VectorStore 实例
     */
    public VectorStore getOrCreateVectorStore(Long embeddingModelId, String collectionName) {
        VectorStore cached = vectorStoreCache.get(collectionName);
        if (cached != null) {
            return cached;
        }
        return createVectorStore(embeddingModelId, collectionName);
    }

    /**
     * 根据知识库获取 VectorStore
     *
     * @param knowledgeBase 知识库
     * @return VectorStore 实例
     */
    public VectorStore getVectorStore(KnowledgeBase knowledgeBase) {
        return getOrCreateVectorStore(knowledgeBase.getModelId(), knowledgeBase.getCollectionsName());
    }

    /**
     * 创建 VectorStore（由子类实现具体的创建逻辑）
     *
     * @param embeddingModelId 嵌入模型ID
     * @param collectionName   集合名称
     * @return VectorStore 实例
     */
    protected abstract VectorStore createVectorStore(Long embeddingModelId, String collectionName);

    /**
     * 获取 EmbeddingModel
     *
     * @param embeddingModelId 嵌入模型ID
     * @return EmbeddingModel 实例
     */
    protected EmbeddingModel getEmbeddingModel(Long embeddingModelId) {
        AiModelConfig modelConfig = aiModelConfigService.getModelConfigById(embeddingModelId);
        if (modelConfig == null) {
            throw new IllegalArgumentException("嵌入模型配置不存在，模型 ID: " + embeddingModelId);
        }
        EmbeddingModel model = embeddingModelFactory.createEmbeddingModel(modelConfig);
        if (model == null) {
            throw new IllegalStateException("创建嵌入模型失败，模型 ID: " + embeddingModelId);
        }
        return model;
    }

    /**
     * 获取向量维度
     *
     * @param embeddingModelId 嵌入模型ID
     * @return 向量维度
     */
    protected int getEmbeddingDimension(Long embeddingModelId) {
        return getEmbeddingModel(embeddingModelId).dimensions();
    }

    @Override
    public void clearCache() {
        vectorStoreCache.clear();
        log.info("[{}] 已清除所有 VectorStore 缓存", getType());
    }
}
