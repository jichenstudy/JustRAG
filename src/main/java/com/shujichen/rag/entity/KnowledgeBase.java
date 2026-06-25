package com.shujichen.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库表
 */
@Data
@TableName("knowledge_base")
public class KnowledgeBase {

    /**
     * 知识库主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 向量模型ID
     */
    private Long embeddingModelId;

    /**
     * 视觉模型ID
     */
    private Long visionModelId;

    /**
     * 集合名称
     */
    private String collectionsName;

    /**
     * 创建人ID
     */
    private Long teamId;

    /**
     * 切分策略：SMART 结构感知 / FIXED 固定长度
     */
    private String chunkStrategy;

    /**
     * 分片大小（字符数），超过此大小的章节会递归细分
     */
    private Integer chunkSize;

    /**
     * 分片重叠大小（字符数），避免边界截断
     */
    private Integer chunkOverlap;

    /**
     * 最小分片大小（字符数），小于此大小的分片会被过滤
     */
    private Integer chunkMinSize;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
