package com.shujichen.rag.common.vo.mcp;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MCP服务器配置VO
 */
@Data
public class AiMcpServerConfigVo {

    /**
     * MCP服务配置ID
     */
    private Long id;

    /**
     * MCP服务名称（唯一标识）
     */
    private String name;

    /**
     * 传输类型: stdio/sse/http
     */
    private String type;

    /**
     * STDIO模式：启动命令
     */
    private String fullCommand;

    /**
     * STDIO模式：环境变量（JSON格式）
     */
    private String env;

    /**
     * SSE/HTTP模式：服务地址URL
     */
    private String url;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;

    /**
     * 是否启用
     */
    private Integer isEnabled;
}