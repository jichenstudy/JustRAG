package com.shujichen.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档内容分块表
 */
@Data
@TableName("document_chunk")
public class DocumentChunk {

    /**
     * 文档分块主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 所属文档ID，对应 document.id
     */
    private Long documentId;

    /**
     * 分块后的文本内容
     */
    private String content;

    /**
     * 分块在文档中的顺序编号，从 0 或 1 开始
     */
    private Integer chunkIndex;

    /**
     * 分块文本的 token 数量，用于上下文窗口控制
     */
    private Integer tokenSize;

    /**
     * 章节路径，如：第一章 > 1.1 概述
     */
    private String sectionPath;

    /**
     * 章节标题
     */
    private String sectionTitle;

    /**
     * 分片在原文章节内的位置序号
     */
    private Integer position;

    /**
     * 分片在原文档中的字符起始位置
     */
    private Integer charStartIndex;

    /**
     * 分片在原文档中的字符结束位置
     */
    private Integer charEndIndex;

    /**
     * 分块创建时间
     */
    private LocalDateTime createdAt;
}
