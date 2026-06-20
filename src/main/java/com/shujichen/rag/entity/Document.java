package com.shujichen.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库文档表
 */
@Data
@TableName("document")
public class Document {

    /**
     * 文档主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 所属知识库ID
     */
    private Long knowledgeBaseId;

    /**
     * 关联的文件ID，对应 file_detail.id
     */
    private String fileId;

    /**
     * 文档名称
     */
    private String name;

    /**
     * 文档类型：PDF / DOCX / TXT / HTML / MARKDOWN
     */
    private String docType;

    /**
     * 解析状态：UPLOADED 已上传 / PARSING 解析中 / PARSED 已解析 / FAILED 解析失败
     */
    private String parseStatus;

    /**
     * 文档拆分后的分块数量
     */
    private Integer chunkCount;

    /**
     * 文档创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 文档最后更新时间
     */
    private LocalDateTime updatedAt;
}
