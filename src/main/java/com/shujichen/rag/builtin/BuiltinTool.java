package com.shujichen.rag.builtin;

/**
 * 内置工具接口
 * <p>
 * 实现此接口即可注册为系统内置工具，与 MCP 工具一起供 AI 调用
 * 只需添加 @Component 注解即可自动注册
 */
public interface BuiltinTool {

    /**
     * 工具名称（AI 调用时的唯一标识）
     */
    String getName();

    /**
     * 工具描述（AI 根据描述决定是否调用此工具）
     */
    String getDescription();

    /**
     * 输入参数 JSON Schema
     */
    String getInputSchema();

    /**
     * 执行工具
     *
     * @param input JSON 格式的输入参数
     * @return 工具执行结果
     */
    String execute(String input);

    /**
     * 是否启用，默认启用
     */
    default boolean isEnabled() {
        return true;
    }
}
