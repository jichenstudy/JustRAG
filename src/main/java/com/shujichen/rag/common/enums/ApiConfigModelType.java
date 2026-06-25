package com.shujichen.rag.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI模型配置类型枚举
 */
@Getter
@AllArgsConstructor
public enum ApiConfigModelType {

    /**
     * 对话模型
     */
    CHAT("CHAT", "对话模型"),

    /**
     * 向量化模型
     */
    EMBEDDING("EMBEDDING", "向量化模型"),

    /**
     * 视觉模型
     */
    VISION("VISION", "视觉模型");

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
     * @return 模型类型枚举
     */
    public static ApiConfigModelType fromCode(String code) {
        for (ApiConfigModelType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的文档类型: " + code);
    }
}
