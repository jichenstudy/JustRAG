package com.shujichen.rag.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shujichen.rag.common.dto.mcp.McpServerDTO;
import com.shujichen.rag.common.vo.mcp.AiMcpServerConfigVo;
import com.shujichen.rag.entity.AiMcpServerConfig;

import java.util.HashMap;
import java.util.List;

/**
 * MCP服务器配置服务接口
 */
public interface AiMcpServerConfigService extends IService<AiMcpServerConfig> {

    /**
     * 分页查询
     *
     * @param page     页码
     * @param pageSize 每页数量
     * @return 分页数据
     */
    Page<AiMcpServerConfigVo> searchPage(long page, long pageSize);

    /**
     * 创建MCP服务
     *
     * @param dto MCP服务参数
     */
    void createMcpServer(McpServerDTO dto);

    /**
     * 修改MCP服务
     *
     * @param id  MCP服务ID
     * @param dto MCP服务参数
     */
    void updateMcpServer(Long id, McpServerDTO dto);

    /**
     * 启用/禁用MCP服务
     *
     * @param id MCP服务ID
     * @return 启用/禁用结果
     */
    Boolean enabled(Long id);

    /**
     * 获取所有工具
     *
     * @param dto MCP服务参数
     * @return 所有工具
     */
    HashMap<String, String> getAllTools(McpServerDTO dto);

    /**
     * 获取所有注册的工具数量
     *
     * @return 工具数量列表
     */
    List<String> getRegisterAllToolsCount();
}