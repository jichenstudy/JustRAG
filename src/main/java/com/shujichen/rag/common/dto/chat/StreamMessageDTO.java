package com.shujichen.rag.common.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流式消息 DTO - 用于 SSE 流式传输中的文本内容
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamMessageDTO {

    /**
     * 文本内容
     */
    private String content;
}
