package com.shujichen.rag.common.dto.mcp;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * MCP服务器配置映射DTO
 */
@Data
public class McpServerMapperDTO {

    /**
     * MCP服务名称（唯一标识）
     */
    private String name;

    /**
     * 传输类型：stdio 或 sse
     */
    private String type;

    /**
     * 启动命令（内部使用），如 "npx"
     */
    private String command;

    /**
     * 命令参数（内部使用），如 ["-y", "@baidumap/mcp-server-baidu-map"]
     */
    private List<String> args;

    /**
     * 环境变量，如 {"BAIDU_MAP_API_KEY": "xxx"}
     */
    private Map<String, String> env;

    /**
     * SSE服务地址，如 "http://localhost:8080/sse"
     */
    private String url;
}