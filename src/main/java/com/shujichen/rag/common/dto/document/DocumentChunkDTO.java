package com.shujichen.rag.common.dto.document;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档分块DTO
 */
@Data
public class DocumentChunkDTO {

    /**
     * 分块ID
     */
    private Long id;

    /**
     * 文档ID
     */
    private Long documentId;

    /**
     * 分块内容
     */
    private String content;

    /**
     * 分块索引
     */
    private Integer chunkIndex;

    /**
     * Token大小
     */
    private Integer tokenSize;

    /**
     * 章节路径
     */
    private String sectionPath;

    /**
     * 章节标题
     */
    private String sectionTitle;

    /**
     * 位置
     */
    private Integer position;

    /**
     * 字符起始索引
     */
    private Integer charStartIndex;

    /**
     * 字符结束索引
     */
    private Integer charEndIndex;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}