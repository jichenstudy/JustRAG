package com.shujichen.rag.common.dto.chat;

import lombok.Data;

/**
 * 创建聊天会话请求DTO
 */
@Data
public class CreateChatSessionDTO {

    /**
     * 聊天助理ID
     */
    private Long assistantId;

    /**
     * 知识库ID
     */
    private Long knowledgeBaseId;

    /**
     * 会话标题
     */
    private String title;

    /**
     * AI模型ID
     */
    private Long modelId;
}