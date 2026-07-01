package com.shujichen.rag.common.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 引用片段 DTO - 用于 RAG 引用回溯功能
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitationDTO {

    /**
     * 引用编号（从 1 开始）
     */
    private Integer index;

    /**
     * 文档 ID
     */
    private String docId;

    /**
     * 文档名称
     */
    private String docName;

    /**
     * 简述信息（用于悬停展示）
     */
    private String preview;

    /**
     * 相似度得分
     */
    private Double score;

    /**
     * 片段 ID
     */
    private String chunkId;

    /**
     * 知识库 ID
     */
    private Long knowledgeBaseId;
}
