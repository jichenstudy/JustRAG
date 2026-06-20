package com.shujichen.rag.common.dto.mcp;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * MCP服务器配置请求DTO
 */
@Data
public class McpServerDTO {

    /**
     * 配置ID
     */
    private Long id;

    /**
     * MCP服务名称（唯一标识）
     */
    @NotNull
    private String name;

    /**
     * 传输类型：stdio 或 sse
     */
    @NotNull
    private String type;

    /**
     * 完整命令（前端输入），如 "npx -y @baidumap/mcp-server-baidu-map"
     */
    private String fullCommand;

    /**
     * SSE服务地址，如 "http://localhost:8080/sse"
     */
    private String url;

    /**
     * 环境变量，如 "BAIDU_MAP_API_KEY=xxx"
     */
    private String env;
}