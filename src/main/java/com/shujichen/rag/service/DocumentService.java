package com.shujichen.rag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shujichen.rag.entity.Document;
import com.shujichen.rag.entity.DocumentChunk;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 文档服务接口
 */
public interface DocumentService extends IService<Document> {

    /**
     * 上传文档
     *
     * @param files           文件列表
     * @param knowledgeBaseId 知识库ID
     * @return 上传结果列表
     */
    List<Map<String, Object>> uploadDocuments(List<MultipartFile> files, Long knowledgeBaseId);

    /**
     * 创建文档
     *
     * @param knowledgeBaseId 知识库ID
     * @param fileId          文件ID
     * @param name            文档名称
     * @param docType         文档类型
     * @return 文档ID
     */
    Long createDocument(Long knowledgeBaseId, String fileId, String name, String docType);

    /**
     * 保存文档分块
     *
     * @param documentId 文档ID
     * @param chunks     文档分块列表
     */
    void saveDocumentChunks(Long documentId, List<DocumentChunk> chunks);

    /**
     * 标记文档解析失败
     *
     * @param documentId 文档ID
     */
    void markDocumentAsParseFailed(Long documentId);

    /**
     * 更新文档名称
     *
     * @param documentId 文档ID
     * @param newName    新名称
     */
    void updateDocumentName(Long documentId, String newName);

    /**
     * 删除文档
     *
     * @param documentId 文档ID
     */
    void deleteDocument(Long documentId);

    /**
     * 根据ID获取文档
     *
     * @param documentId 文档ID
     * @return 文档实体
     */
    Document getDocumentById(Long documentId);

    /**
     * 根据知识库ID获取文档列表
     *
     * @param knowledgeBaseId 知识库ID
     * @return 文档列表
     */
    List<Document> getDocumentsByKnowledgeBaseId(Long knowledgeBaseId);

    /**
     * 获取文档分块列表
     *
     * @param documentId 文档ID
     * @return 文档分块列表
     */
    List<DocumentChunk> getDocumentChunks(Long documentId);

    /**
     * 统计知识库文档数量
     *
     * @param knowledgeBaseId 知识库ID
     * @return 文档数量
     */
    long countDocumentsByKnowledgeBaseId(Long knowledgeBaseId);

    /**
     * 连接知识库
     *
     * @param fileId          文件ID
     * @param knowledgeBaseId 知识库ID
     * @return 是否成功
     */
    Boolean connectKnowledgeBase(String fileId, Long knowledgeBaseId);

    /**
     * 解析文档
     *
     * @param fileId 文档ID
     * @return 是否成功
     */
    Boolean parseDocument(String fileId);

    /**
     * 在指定知识库中进行相似度搜索
     *
     * @param knowledgeBaseId 知识库ID
     * @param query           查询
     * @param topK            返回数量
     * @return 相似度搜索结果
     */
    List<org.springframework.ai.document.Document> searchSimilar(Long knowledgeBaseId, String query, int topK);
}
