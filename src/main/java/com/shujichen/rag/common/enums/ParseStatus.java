package com.shujichen.rag.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文档解析状态枚举
 */
@Getter
@AllArgsConstructor
public enum ParseStatus {

    /**
     * 已上传
     */
    UPLOADED("UPLOADED", "已上传"),

    /**
     * 解析中
     */
    PARSING("PARSING", "解析中"),

    /**
     * 已解析
     */
    PARSED("PARSED", "已解析"),

    /**
     * 解析失败
     */
    FAILED("FAILED", "解析失败");

    /**
     * 状态编码
     */
    private final String code;

    /**
     * 状态描述
     */
    private final String desc;

    /**
     * 根据编码获取枚举
     *
     * @param code 状态编码
     * @return 解析状态枚举
     */
    public static ParseStatus fromCode(String code) {
        for (ParseStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的解析状态: " + code);
    }

    /**
     * 是否已解析完成
     *
     * @return true：已解析；false：未解析
     */
    public boolean isParsed() {
        return this == PARSED;
    }

    /**
     * 是否解析失败
     *
     * @return true：失败；false：未失败
     */
    public boolean isFailed() {
        return this == FAILED;
    }
}