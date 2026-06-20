package com.shujichen.rag.common.dto.chat;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发送消息请求DTO
 */
@Data
public class SendMessageDTO {

    /**
     * 消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    private String content;
}