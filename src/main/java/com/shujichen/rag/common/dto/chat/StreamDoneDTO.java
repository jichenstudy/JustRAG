package com.shujichen.rag.common.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 流式完成 DTO - 用于 SSE 流式传输结束时的统计信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamDoneDTO {

    /**
     * Token 使用统计
     */
    private Map<String, Integer> totalTokens;

    /**
     * 总耗时毫秒
     */
    private Long totalElapsedMs;

    /**
     * 保存到数据库后的消息ID
     */
    private Long messageId;
}
