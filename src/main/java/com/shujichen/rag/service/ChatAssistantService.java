package com.shujichen.rag.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shujichen.rag.common.dto.assistant.ChatAssistantDTO;
import com.shujichen.rag.common.dto.assistant.CreateChatAssistantDTO;
import com.shujichen.rag.entity.ChatAssistant;

import java.util.List;

/**
 * 聊天助理服务接口
 */
public interface ChatAssistantService extends IService<ChatAssistant> {

    /**
     * 创建聊天助理
     *
     * @param dto 创建数据
     * @return 助理ID
     */
    Long createAssistant(CreateChatAssistantDTO dto);

    /**
     * 获取聊天助理详情
     *
     * @param id 助理ID
     * @return 助理DTO
     */
    ChatAssistantDTO getAssistantById(Long id);

    /**
     * 获取所有聊天助理列表
     *
     * @return 助理列表
     */
    List<ChatAssistantDTO> listAssistants();

    /**
     * 分页搜索聊天助理
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @param keyword  搜索关键词
     * @return 分页结果
     */
    Page<ChatAssistantDTO> searchPage(long page, long pageSize, String keyword);

    /**
     * 搜索聊天助理
     *
     * @param keyword 搜索关键词
     * @return 搜索结果列表
     */
    List<ChatAssistantDTO> search(String keyword);

    /**
     * 更新聊天助理
     *
     * @param id  助理ID
     * @param dto 更新数据
     */
    void updateAssistant(Long id, CreateChatAssistantDTO dto);

    /**
     * 删除聊天助理
     *
     * @param id 助理ID
     */
    void deleteAssistant(Long id);
}
