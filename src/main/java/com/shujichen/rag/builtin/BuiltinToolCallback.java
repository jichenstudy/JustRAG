package com.shujichen.rag.builtin;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * 将 BuiltinTool 包装为 Spring AI 的 ToolCallback
 * <p>
 * 使内置工具与 MCP 工具在同一体系下工作，对 ChatClient 完全透明
 */
public class BuiltinToolCallback implements ToolCallback {

    private final BuiltinTool tool;
    private final ToolDefinition definition;

    public BuiltinToolCallback(BuiltinTool tool) {
        this.tool = tool;
        this.definition = new BuiltinToolDefinition(tool);
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return definition;
    }

    @Override
    public String call(String toolInput) {
        return tool.execute(toolInput);
    }

    /**
     * 内置工具定义
     */
    private record BuiltinToolDefinition(BuiltinTool tool) implements ToolDefinition {
        @Override
        public String name() {
            return tool.getName();
        }

        @Override
        public String description() {
            return tool.getDescription();
        }

        @Override
        public String inputSchema() {
            return tool.getInputSchema();
        }
    }
}
