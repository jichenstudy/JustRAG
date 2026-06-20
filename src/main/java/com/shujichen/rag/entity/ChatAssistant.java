package com.shujichen.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 聊天助理配置表
 */
@TableName(value ="chat_assistant")
@Data
public class ChatAssistant {

    /**
     * 聊天助理主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
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
    private Long modelId;

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

    /**
     * 创建人ID
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createdTime;

    /**
     * 更新时间
     */
    private Date updatedTime;
}
