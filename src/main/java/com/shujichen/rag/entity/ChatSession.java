package com.shujichen.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天会话表
 */
@Data
@TableName("chat_session")
public class ChatSession {

    /**
     * 聊天会话主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 聊天助理ID
     */
    private Long assistantId;

    /**
     * 会话标题，通常取自首条用户问题
     */
    private String title;

    /**
     * 关联的知识库ID，可为空表示自由对话
     */
    private Long knowledgeBaseId;

    /**
     * 使用的AI模型ID
     */
    private Long modelId;

    /**
     * 会话创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 会话最后活跃时间
     */
    private LocalDateTime updatedAt;
}
