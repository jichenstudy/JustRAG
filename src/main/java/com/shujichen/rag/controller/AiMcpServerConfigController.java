package com.shujichen.rag.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shujichen.rag.common.dto.Result;
import com.shujichen.rag.common.dto.mcp.McpServerDTO;
import com.shujichen.rag.common.vo.mcp.AiMcpServerConfigVo;
import com.shujichen.rag.service.AiMcpServerConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

/**
 * MCP服务器配置Controller
 */
@RestController
@RequestMapping("/api/mcp")
@RequiredArgsConstructor
public class AiMcpServerConfigController {

    private final AiMcpServerConfigService aiMcpServerConfigService;

    /**
     * 分页查询
     *
     * @param page     页码
     * @param pageSize 每页数量
     * @return 分页数据
     */
    @GetMapping("/page")
    public Result<Page<AiMcpServerConfigVo>> searchPage(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        Page<AiMcpServerConfigVo> pageObj = aiMcpServerConfigService.searchPage(page, pageSize);
        return Result.success(pageObj);
    }

    /**
     * 删除MCP服务
     *
     * @param id MCP服务ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        aiMcpServerConfigService.removeById(id);
        return Result.success();
    }

    /**
     * 创建MCP服务
     *
     * @param dto MCP服务参数
     * @return 操作结果
     */
    @PostMapping
    public Result<Void> create(@RequestBody McpServerDTO dto) {
        aiMcpServerConfigService.createMcpServer(dto);
        return Result.success();
    }

    /**
     * 修改MCP服务
     *
     * @param id  MCP服务ID
     * @param dto MCP服务参数
     * @return 操作结果
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody McpServerDTO dto) {
        aiMcpServerConfigService.updateMcpServer(id, dto);
        return Result.success();
    }

    /**
     * 启用/禁用MCP服务
     *
     * @param id MCP服务ID
     * @return 操作结果
     */
    @GetMapping("/enabled")
    public Result<Void> enabled(@RequestParam Long id) {
        Boolean enabled = aiMcpServerConfigService.enabled(id);
        if (enabled) {
            return Result.success();
        }
        return Result.error("操作失败");
    }

    /**
     * 获取所有工具
     *
     * @param dto MCP服务参数
     * @return 工具列表
     */
    @PostMapping("/getAllTools")
    public Result<HashMap<String, String>> getAllTools(@RequestBody McpServerDTO dto) {
        HashMap<String, String> list = aiMcpServerConfigService.getAllTools(dto);
        return Result.success(list);
    }

    /**
     * 获取所有注册的工具数量
     *
     * @return 工具数量列表
     */
    @GetMapping("/getRegisterAllToolsCount")
    public Result<List<String>> getRegisterAllToolsCount() {
        List<String> count = aiMcpServerConfigService.getRegisterAllToolsCount();
        return Result.success(count);
    }
}