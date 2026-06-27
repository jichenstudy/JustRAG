package com.shujichen.rag.common.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 过程步骤 DTO - 用于 SSE 流式传输中的过程追踪
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessStepDTO {

    /**
     * 步骤类型: RETRIEVE_START, RETRIEVE_END, TOOL_CALL_START, TOOL_CALL_END, MODEL_INFO, THINKING, ERROR
     */
    private String type;

    /**
     * 人类可读的标签
     */
    private String label;

    /**
     * 工具名称（仅 TOOL_CALL 类型）
     */
    private String toolName;

    /**
     * 工具输入（仅 TOOL_CALL 类型）
     */
    private String input;

    /**
     * 工具输出（仅 TOOL_CALL 类型）
     */
    private String output;

    /**
     * 检索到的文档数量（仅 RETRIEVE_END 类型）
     */
    private Integer documentsCount;

    /**
     * 耗时毫秒（仅 END 类型）
     */
    private Long elapsedMs;

    /**
     * 内容（仅 THINKING 类型）
     */
    private String content;

    /**
     * 时间戳
     */
    private Long timestamp;
}
