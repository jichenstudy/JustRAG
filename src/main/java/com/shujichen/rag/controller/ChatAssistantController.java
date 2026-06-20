package com.shujichen.rag.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shujichen.rag.common.dto.Result;
import com.shujichen.rag.common.dto.assistant.ChatAssistantDTO;
import com.shujichen.rag.common.dto.assistant.CreateChatAssistantDTO;
import com.shujichen.rag.service.ChatAssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 聊天助理控制器
 */
@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
@Validated
public class ChatAssistantController {

    private final ChatAssistantService chatAssistantService;

    /**
     * 创建聊天助理
     *
     * @param dto 创建参数
     * @return 助理ID
     */
    @PostMapping
    public Result<Long> create(@RequestBody @Validated CreateChatAssistantDTO dto) {
        return Result.success(chatAssistantService.createAssistant(dto));
    }

    /**
     * 获取聊天助理详情
     *
     * @param id 助理ID
     * @return 助理信息
     */
    @GetMapping("/{id}")
    public Result<ChatAssistantDTO> get(@PathVariable Long id) {
        ChatAssistantDTO dto = chatAssistantService.getAssistantById(id);
        if (dto == null) {
            return Result.error("聊天助理不存在");
        }
        return Result.success(dto);
    }

    /**
     * 获取聊天助理列表
     *
     * @return 助理列表
     */
    @GetMapping("/list")
    public Result<List<ChatAssistantDTO>> list() {
        return Result.success(chatAssistantService.listAssistants());
    }

    /**
     * 分页搜索聊天助理
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @param keyword  搜索关键词
     * @return 分页结果
     */
    @GetMapping("/page")
    public Result<Page<ChatAssistantDTO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(chatAssistantService.searchPage(page, pageSize, keyword));
    }

    /**
     * 更新聊天助理
     *
     * @param id  助理ID
     * @param dto 更新参数
     * @return 操作结果
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Validated CreateChatAssistantDTO dto) {
        try {
            chatAssistantService.updateAssistant(id, dto);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除聊天助理
     *
     * @param id 助理ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        try {
            chatAssistantService.deleteAssistant(id);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 搜索聊天助理
     *
     * @param keyword 搜索关键词
     * @return 搜索结果
     */
    @GetMapping("/search")
    public Result<List<ChatAssistantDTO>> search(@RequestParam String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.error("搜索关键词不能为空");
        }
        return Result.success(chatAssistantService.search(keyword));
    }
}
