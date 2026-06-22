package com.shujichen.rag.common.dto.assistant;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 聊天助理DTO
 */
@Data
public class ChatAssistantDTO {

    /**
     * 助理ID
     */
    private Long id;

    /**
     * 助理头像
     */
    private String assistantAvatar;

    /**
     * 助理名称
     */
    private String assistantName;

    /**
     * 助理描述
     */
    private String assistantDescription;

    /**
     * 空回复内容
     */
    private String emptyReply;

    /**
     * 开场白
     */
    private String openingStatement;

    /**
     * 关联知识库ID
     */
    private Long knowledgeBaseId;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * Top-P参数
     */
    private BigDecimal topP;

    /**
     * TopN参数
     */
    private Integer topN;

    /**
     * 是否启用推理模式
     */
    private Integer enableReasoningMode;

    /**
     * AI模型ID
     */
    private Long modelId;

    /**
     * 温度参数
     */
    private BigDecimal temperature;

    /**
     * 存在惩罚参数
     */
    private BigDecimal presencePenalty;

    /**
     * 频率惩罚参数
     */
    private BigDecimal frequencyPenalty;

    /**
     * 最大Token数
     */
    private Integer maxTokens;

    /**
     * 创建时间
     */
    private Date createdTime;

    /**
     * 更新时间
     */
    private Date updatedTime;
}