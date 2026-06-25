package com.shujichen.rag.vector;

import com.shujichen.rag.entity.DocumentChunk;
import com.shujichen.rag.entity.KnowledgeBase;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

/**
 * 向量库策略接口
 * 定义所有向量库实现必须支持的操作
 */
public interface VectorStoreStrategy {

    /**
     * 获取策略类型标识
     *
     * @return 策略类型
     */
    String getType();

    /**
     * 根据知识库获取 VectorStore
     *
     * @param knowledgeBase 知识库
     * @return VectorStore 实例
     */
    VectorStore getVectorStore(KnowledgeBase knowledgeBase);

    /**
     * 根据向量模型ID和集合名称获取或创建 VectorStore
     *
     * @param embeddingModelId 向量模型 ID
     * @param collectionName   集合名称
     * @return VectorStore 实例
     */
    VectorStore getOrCreateVectorStore(Long embeddingModelId, String collectionName);

    /**
     * 存储文档分块向量到指定知识库
     *
     * @param knowledgeBase 知识库
     * @param documentId    文档ID
     * @param chunks        文档分块列表
     */
    void storeChunkVectors(KnowledgeBase knowledgeBase, Long documentId, List<DocumentChunk> chunks);

    /**
     * 删除指定知识库中的文档向量
     *
     * @param knowledgeBase 知识库
     * @param documentId    文档ID
     */
    void deleteDocumentVectors(KnowledgeBase knowledgeBase, Long documentId);

    /**
     * 在指定知识库中进行相似度搜索
     *
     * @param knowledgeBase 知识库
     * @param query         查询文本
     * @param topK          返回结果数量
     * @return 相似文档列表
     */
    List<Document> searchSimilar(KnowledgeBase knowledgeBase, String query, int topK);

    /**
     * 删除知识库对应的向量集合
     *
     * @param collectionName 集合名称
     * @return 是否删除成功
     */
    boolean dropCollection(String collectionName);

    /**
     * 清除缓存
     */
    void clearCache();
}
