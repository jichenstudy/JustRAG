package com.shujichen.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MCP服务配置表
 */
@TableName(value ="ai_mcp_server_config")
@Data
public class AiMcpServerConfig {

    /**
     * MCP服务配置ID
     */
    @TableId(type = IdType.ASSIGN_ID)
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
    private String command;

    /**
     * STDIO模式：命令参数（JSON格式）
     */
    private String args;

    /**
     * STDIO模式：环境变量（JSON格式）
     */
    private String env;

    /**
     * SSE/HTTP模式：服务地址URL
     */
    private String url;

    /**
     * 创建人ID
     */
    private Long userId;

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
