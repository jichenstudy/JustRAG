package com.shujichen.rag.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息角色枚举
 */
@Getter
@AllArgsConstructor
public enum MessageRole {

    /**
     * 用户角色
     */
    USER("USER", "用户"),

    /**
     * 助手角色
     */
    ASSISTANT("ASSISTANT", "助手");

    /**
     * 角色编码
     */
    private final String code;

    /**
     * 角色描述
     */
    private final String desc;

    /**
     * 根据编码获取枚举
     *
     * @param code 角色编码
     * @return 消息角色枚举
     */
    public static MessageRole fromCode(String code) {
        for (MessageRole role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        throw new IllegalArgumentException("未知的消息角色: " + code);
    }

    /**
     * 是否为用户角色
     *
     * @return true：用户；false：非用户
     */
    public boolean isUser() {
        return this == USER;
    }

    /**
     * 是否为助手角色
     *
     * @return true：助手；false：非助手
     */
    public boolean isAssistant() {
        return this == ASSISTANT;
    }
}