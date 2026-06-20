package com.shujichen.rag.service;

import com.shujichen.rag.entity.KnowledgeBase;

import java.util.List;

/**
 * 知识库服务接口
 */
public interface KnowledgeBaseService {

    /**
     * 创建知识库
     *
     * @param name          知识库名称
     * @param description   知识库描述
     * @param modelId       AI模型ID
     * @param chunkStrategy 切分策略
     * @param chunkSize     切分大小
     * @param chunkOverlap  切分重叠大小
     * @param chunkMinSize  最小切分大小
     * @return 知识库ID
     */
    Long createKnowledgeBase(String name, String description, Long modelId,
                             String chunkStrategy, Integer chunkSize,
                             Integer chunkOverlap, Integer chunkMinSize);

    /**
     * 更新知识库
     *
     * @param id            知识库ID
     * @param name          知识库名称
     * @param description   知识库描述
     * @param modelId       AI模型ID
     * @param chunkStrategy 切分策略
     * @param chunkSize     切分大小
     * @param chunkOverlap  切分重叠大小
     * @param chunkMinSize  最小切分大小
     */
    void updateKnowledgeBase(Long id, String name, String description, Long modelId,
                             String chunkStrategy, Integer chunkSize,
                             Integer chunkOverlap, Integer chunkMinSize);

    /**
     * 启用知识库
     *
     * @param id 知识库ID
     */
    void enableKnowledgeBase(Long id);

    /**
     * 停用知识库
     *
     * @param id 知识库ID
     */
    void disableKnowledgeBase(Long id);

    /**
     * 删除知识库
     *
     * @param id 知识库ID
     */
    void deleteKnowledgeBase(Long id);

    /**
     * 根据ID获取知识库详情
     *
     * @param id 知识库ID
     * @return 知识库信息
     */
    KnowledgeBase getKnowledgeBaseById(Long id);

    /**
     * 获取所有知识库列表
     *
     * @return 知识库列表
     */
    List<KnowledgeBase> getAllKnowledgeBases();
}
