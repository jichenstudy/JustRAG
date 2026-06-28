package com.shujichen.rag.builtin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 内置工具注册中心
 * <p>
 * Spring 自动发现所有 BuiltinTool 实现类，注册并管理其生命周期
 */
@Slf4j
@Component
public class BuiltinToolRegistry {

    private final Map<String, BuiltinTool> toolMap = new ConcurrentHashMap<>();

    /**
     * 构造函数，Spring 自动注入所有 BuiltinTool 实现
     */
    public BuiltinToolRegistry(List<BuiltinTool> tools) {
        for (BuiltinTool tool : tools) {
            toolMap.put(tool.getName(), tool);
            log.info("注册内置工具: {}", tool.getName());
        }
        log.info("内置工具注册完成，共 {} 个", toolMap.size());
    }

    /**
     * 获取所有已启用的内置工具，转换为 ToolCallback 列表
     */
    public List<ToolCallback> getAllToolCallbacks() {
        return toolMap.values().stream()
                .filter(BuiltinTool::isEnabled)
                .map(BuiltinToolCallback::new)
                .collect(Collectors.toList());
    }

    /**
     * 按名称获取工具
     */
    public BuiltinTool getTool(String name) {
        return toolMap.get(name);
    }

    /**
     * 获取已注册的工具数量
     */
    public int getToolCount() {
        return toolMap.size();
    }
}
