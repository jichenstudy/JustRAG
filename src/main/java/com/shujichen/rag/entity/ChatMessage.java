package com.shujichen.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息表
 */
@Data
@TableName("chat_message")
public class ChatMessage {

    /**
     * 聊天消息主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 所属聊天会话ID，对应 chat_session.id
     */
    private Long sessionId;

    /**
     * 消息角色：USER 用户 / ASSISTANT 助手
     */
    private String role;

    /**
     * 消息内容文本
     */
    private String content;

    /**
     * 过程步骤（JSON格式，记录思考链路和工具调用）
     */
    private String processSteps;

    /**
     * 引用信息（JSON格式，记录RAG检索的文档片段）
     */
    private String citations;

    /**
     * 消息发送或生成时间
     */
    private LocalDateTime createdAt;
}
