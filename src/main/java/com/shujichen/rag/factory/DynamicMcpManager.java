package com.shujichen.rag.factory;

import com.shujichen.rag.common.dto.mcp.McpServerMapperDTO;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.SyncMcpToolCallback;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态MCP服务管理器
 * 负责MCP服务的注册、注销和工具收集
 */
@Slf4j
@Component
public class DynamicMcpManager {

    /**
     * MCP客户端注册表
     * Key: MCP服务名称
     * Value: MCP同步客户端
     */
    private final Map<String, McpSyncClient> mcpRegistry = new ConcurrentHashMap<>();

    /**
     * 注册新的MCP服务
     *
     * @param config MCP服务配置
     */
    public void register(McpServerMapperDTO config) {
        // 如果已存在同名服务，先注销
        if (mcpRegistry.containsKey(config.getName())) {
            unregister(config.getName());
        }

        McpSyncClient client;

        if ("stdio".equalsIgnoreCase(config.getType())) {
            client = createStdioClient(config);
        } else if ("sse".equalsIgnoreCase(config.getType())) {
            client = createSseClient(config);
        } else if ("http".equalsIgnoreCase(config.getType())) {
            client = createHttpClient(config);
        } else {
            throw new IllegalArgumentException("不支持的MCP类型: " + config.getType());
        }

        // 初始化连接
        try {
            client.initialize();
        } catch (Exception e) {
            log.warn("MCP服务注册失败: {}", config.getName(), e);
        }

        // 注册到Map
        mcpRegistry.put(config.getName(), client);
        log.info("MCP服务注册成功: {}", config.getName());
    }

    /**
     * 注销MCP服务
     *
     * @param name MCP服务名称
     */
    public void unregister(String name) {
        McpSyncClient client = mcpRegistry.remove(name);
        if (client != null) {
            try {
                client.close();
                log.info("MCP服务注销成功: {}", name);
            } catch (Exception e) {
                log.warn("关闭MCP客户端异常: {}", name, e);
            }
        }
    }

    /**
     * 获取所有已注册MCP服务的工具回调
     * 这是核心方法，ChatClient调用时使用
     *
     * @return 所有工具回调列表
     */
    public List<ToolCallback> getAllToolCallbacks() {
        List<ToolCallback> allTools = new ArrayList<>();

        for (Map.Entry<String, McpSyncClient> entry : mcpRegistry.entrySet()) {
            try {
                McpSyncClient client = entry.getValue();

                log.info("正在从 {} 获取工具列表...", entry.getKey());
                long startTime = System.currentTimeMillis();

                // 从MCP服务获取工具列表
                McpSchema.ListToolsResult toolsResult = client.listTools();
                long duration = System.currentTimeMillis() - startTime;

                log.info("从 {} 获取到 {} 个工具，耗时 {} ms",
                        entry.getKey(), toolsResult.tools().size(), duration);

                // 将每个工具转换为ToolCallback
                for (McpSchema.Tool tool : toolsResult.tools()) {
                    allTools.add(new SyncMcpToolCallback(client, tool));
                    log.debug("添加工具: {} 来自 {}", tool.name(), entry.getKey());
                }

            } catch (Exception e) {
                log.error("获取MCP工具失败: {}", entry.getKey(), e);
            }
        }

        log.info("总共收集到 {} 个工具", allTools.size());
        return allTools;
    }

    /**
     * 获取已注册的MCP服务名称列表
     *
     * @return 服务名称列表
     */
    public List<String> listRegistered() {
        return new ArrayList<>(mcpRegistry.keySet());
    }

    /**
     * 检查指定MCP服务是否已注册
     *
     * @param name MCP服务名称
     * @return 是否已注册
     */
    public boolean isRegistered(String name) {
        return mcpRegistry.containsKey(name);
    }

    /**
     * 获取指定MCP配置的工具列表（用于测试配置）
     * 创建临时客户端，获取工具列表后立即关闭，不注册到内存
     *
     * @param config MCP服务配置
     * @return 工具名称列表
     */
    public HashMap<String, String> getToolsForConfig(McpServerMapperDTO config) {
        McpSyncClient client = null;
        try {
            // 根据类型创建临时客户端
            if ("stdio".equalsIgnoreCase(config.getType())) {
                client = createStdioClient(config);
            } else if ("sse".equalsIgnoreCase(config.getType())) {
                client = createSseClient(config);
            } else if ("http".equalsIgnoreCase(config.getType())) {
                client = createHttpClient(config);
            } else {
                throw new IllegalArgumentException("不支持的MCP类型: " + config.getType());
            }

            // 初始化连接
            client.initialize();
            log.info("临时MCP客户端初始化成功: {}", config.getName());

            // 获取工具列表
            long startTime = System.currentTimeMillis();
            McpSchema.ListToolsResult toolsResult = client.listTools();
            long duration = System.currentTimeMillis() - startTime;

            log.info("从 {} 获取到 {} 个工具，耗时 {} ms",
                    config.getName(), toolsResult.tools().size(), duration);

            // 提取工具名称列表
            HashMap<String, String> toolMap = new HashMap<>();
            for (McpSchema.Tool tool : toolsResult.tools()) {
                toolMap.put(tool.name(),tool.description());
                log.debug("工具: {} - 描述: {}", tool.name(), tool.description());
            }

            return toolMap;

        } catch (Exception e) {
            log.error("获取MCP工具列表失败: {}", config.getName(), e);
            throw new RuntimeException("获取工具列表失败: " + e.getMessage(), e);
        } finally {
            // 立即关闭临时客户端
            if (client != null) {
                try {
                    client.close();
                    log.info("临时MCP客户端已关闭: {}", config.getName());
                } catch (Exception e) {
                    log.warn("关闭临时MCP客户端异常: {}", config.getName(), e);
                }
            }
        }
    }

    /**
     * 创建STDIO模式的MCP客户端
     */
    private McpSyncClient createStdioClient(McpServerMapperDTO config) {
        // 合并系统环境变量和用户提供的环境变量
        // 重要：必须保留系统环境变量（如PATH、HOME等），否则子进程无法正常执行
        Map<String, String> mergedEnv = new HashMap<>(System.getenv());
        if (config.getEnv() != null) {
            mergedEnv.putAll(config.getEnv());
        }

        ServerParameters params = ServerParameters.builder(config.getCommand())
                .args(config.getArgs())
                .env(mergedEnv)
                .build();

        StdioClientTransport transport = new StdioClientTransport(params, McpJsonMapper.createDefault());

        return McpClient.sync(transport)
//                .clientInfo(new McpSchema.Implementation(config.getName(), "1.0.0"))
                .build();
    }

    /**
     * 创建SSE模式的MCP客户端
     */
    private McpSyncClient createSseClient(McpServerMapperDTO config) {
        URI uri = URI.create(config.getUrl());
        String baseUrl = uri.getScheme() + "://" + uri.getHost() + (uri.getPort() != -1 ? ":" + uri.getPort() : "");
        String endpoint = uri.getPath();

        HttpClientSseClientTransport.Builder builder =
                HttpClientSseClientTransport.builder(baseUrl)
                        .sseEndpoint(endpoint);

        HttpClientSseClientTransport transport = builder.build();

        return McpClient.sync(transport)
//                .clientInfo(new McpSchema.Implementation(config.getName(), "1.0.0"))
                .build();
    }

    /**
     * 创建SSE模式的MCP客户端
     */
    private McpSyncClient createHttpClient(McpServerMapperDTO config) {
        URI uri = URI.create(config.getUrl());
        String baseUrl = uri.getScheme() + "://" + uri.getHost() + (uri.getPort() != -1 ? ":" + uri.getPort() : "");
        String endpoint = uri.getPath();

        HttpClientStreamableHttpTransport.Builder builder =
                HttpClientStreamableHttpTransport.builder(baseUrl)
                        .endpoint(endpoint);

        HttpClientStreamableHttpTransport transport = builder.build();

        return McpClient.sync(transport)
//                .clientInfo(new McpSchema.Implementation(config.getName(), "1.0.0"))
                .build();
    }


    /**
     * 应用关闭时清理所有MCP连接
     */
    @PreDestroy
    public void cleanup() {
        log.info("正在清理所有MCP连接...");
        new ArrayList<>(mcpRegistry.keySet()).forEach(this::unregister);
    }
}
