package com.shujichen.rag.common.dto.chat;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天会话DTO
 */
@Data
public class ChatSessionDTO {

    /**
     * 会话ID
     */
    private Long id;

    /**
     * 聊天助理ID
     */
    private Long assistantId;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 知识库ID
     */
    private Long knowledgeBaseId;

    /**
     * AI模型ID
     */
    private Long modelId;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}