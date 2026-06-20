package com.shujichen.rag.common.dto.assistant;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建/更新聊天助理DTO
 */
@Data
public class CreateChatAssistantDTO {

    /**
     * 助理头像
     */
    private String assistantAvatar;

    /**
     * 助理名称
     */
    @NotBlank(message = "助理名称不能为空")
    private String assistantName;

    /**
     * 助理描述信息
     */
    private String assistantDescription;

    /**
     * 空回复（无匹配回复时的默认回复）
     */
    private String emptyReply;

    /**
     * 开场白（会话开始时的欢迎语）
     */
    private String openingStatement;

    /**
     * 关联知识库ID
     */
    private String knowledgeBaseId;

    /**
     * 系统提示词（指导AI行为的指令）
     */
    private String systemPrompt;

    /**
     * 相似度阈值（0-1，默认0.7）
     */
    private BigDecimal similarityThreshold;

    /**
     * 向量相似度权重（0-1，默认0.5）
     */
    private BigDecimal vectorWeight;

    /**
     * Top-P 核采样参数（0-1，控制词汇选择范围）
     */
    private BigDecimal topP;

    /**
     * TopN（返回最相似结果数量，默认5）
     */
    private Integer topN;

    /**
     * 是否开启推理模式（0-关闭，1-开启）
     */
    private Integer enableReasoningMode;

    /**
     * 关联模型（AI模型名称）
     */
    private String modelId;

    /**
     * 温度参数（控制随机性，0-2，默认0.8）
     */
    private BigDecimal temperature;

    /**
     * 存在处罚（减少重复内容，-2到2，默认0）
     */
    private BigDecimal presencePenalty;

    /**
     * 频率惩罚（减少高频词，-2到2，默认0）
     */
    private BigDecimal frequencyPenalty;

    /**
     * 最大token数（单次生成最大长度）
     */
    private Integer maxTokens;
}
