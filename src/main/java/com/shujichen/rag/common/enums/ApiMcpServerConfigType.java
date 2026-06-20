package com.shujichen.rag.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * MCP服务器配置类型枚举
 */
@Getter
@AllArgsConstructor
public enum ApiMcpServerConfigType {

    /**
     * STDIO类型
     */
    STDIO("stdio", "标准输入输出"),

    /**
     * SSE类型
     */
    SSE("sse", "服务器发送事件"),

    /**
     * HTTP类型
     */
    HTTP("http", "HTTP协议");

    /**
     * 类型编码
     */
    private final String code;

    /**
     * 类型描述
     */
    private final String desc;

    /**
     * 根据编码获取枚举
     *
     * @param code 类型编码
     * @return MCP服务器配置类型枚举
     */
    public static ApiMcpServerConfigType fromCode(String code) {
        for (ApiMcpServerConfigType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的MCP服务器类型: " + code);
    }
}