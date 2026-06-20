package com.shujichen.rag.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

/**
 * 文档分块策略枚举
 */
@Getter
public enum ChunkingMode {

    /**
     * 结构感知分块
     */
    STRUCTURE_AWARE("FIXED", "结构感知"),

    /**
     * 固定长度分块
     */
    FIXED_SIZE("FIXED", "固定长度");

    /**
     * 策略值
     */
    private final String value;

    /**
     * 策略描述
     */
    private final String desc;

    ChunkingMode(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    /**
     * 从字符串解析策略
     *
     * @param strategy 策略字符串
     * @return 分块策略枚举
     */
    @JsonCreator
    public static ChunkingMode fromValue(String strategy) {
        if (strategy == null || strategy.isBlank()) {
            return STRUCTURE_AWARE;
        }
        String normalized = strategy.trim().toUpperCase();
        for (ChunkingMode mode : values()) {
            if (mode.value.equalsIgnoreCase(strategy) || mode.name().equalsIgnoreCase(normalized)) {
                return mode;
            }
        }
        return STRUCTURE_AWARE;
    }

    /**
     * 是否为固定长度分块
     *
     * @return true：固定长度；false：结构感知
     */
    public boolean isFixed() {
        return this == FIXED_SIZE;
    }
}