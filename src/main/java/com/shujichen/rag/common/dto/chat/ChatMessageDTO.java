package com.shujichen.rag.common.dto.chat;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息DTO
 */
@Data
public class ChatMessageDTO {

    /**
     * 消息ID
     */
    private Long id;

    /**
     * 会话ID
     */
    private Long sessionId;

    /**
     * 消息角色
     */
    private String role;

    /**
     * 消息内容
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
     * 创建时间
     */
    private LocalDateTime createdAt;
}