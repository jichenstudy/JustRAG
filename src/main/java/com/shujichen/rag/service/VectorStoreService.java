package com.shujichen.rag.service;

import com.shujichen.rag.entity.DocumentChunk;
import com.shujichen.rag.entity.KnowledgeBase;
import org.springframework.ai.document.Document;

import java.util.List;

/**
 * 向量存储服务接口
 */
public interface VectorStoreService {

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
}