package com.shujichen.rag.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shujichen.rag.common.dto.assistant.ChatAssistantDTO;
import com.shujichen.rag.common.dto.assistant.CreateChatAssistantDTO;
import com.shujichen.rag.common.util.AvatarUtil;
import com.shujichen.rag.entity.ChatAssistant;
import com.shujichen.rag.mapper.ChatAssistantMapper;
import com.shujichen.rag.service.ChatAssistantService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 聊天助理配置服务实现
 */
@Service
public class ChatAssistantServiceImpl extends ServiceImpl<ChatAssistantMapper, ChatAssistant>
        implements ChatAssistantService {

    @Override
    public Long createAssistant(CreateChatAssistantDTO dto) {
        ChatAssistant chatAssistant = BeanUtil.copyProperties(dto, ChatAssistant.class);
        // 根据名称获取头像
        if (StringUtils.isBlank(dto.getAssistantAvatar()) && StringUtils.isNotBlank(dto.getAssistantName())) {
            String avatar = AvatarUtil.getAvatarByUsername(dto.getAssistantName());
            chatAssistant.setAssistantAvatar(avatar);
        }
        chatAssistant.setUserId(StpUtil.getLoginIdAsLong());
        this.save(chatAssistant);
        return chatAssistant.getId();
    }

    @Override
    public ChatAssistantDTO getAssistantById(Long id) {
        ChatAssistant chatAssistant = this.getById(id);
        if (chatAssistant == null) {
            return null;
        }
        return BeanUtil.copyProperties(chatAssistant, ChatAssistantDTO.class);
    }

    @Override
    public List<ChatAssistantDTO> listAssistants() {
        List<ChatAssistant> list = this.list(new LambdaQueryWrapper<ChatAssistant>()
                .eq(ChatAssistant::getUserId, StpUtil.getLoginIdAsLong()));
        return BeanUtil.copyToList(list, ChatAssistantDTO.class);
    }

    @Override
    public Page<ChatAssistantDTO> searchPage(long page, long pageSize, String keyword) {
        Page<ChatAssistant> pageObj = new Page<>(page, pageSize);
        LambdaQueryWrapper<ChatAssistant> wrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(ChatAssistant::getAssistantName, keyword)
                    .or()
                    .like(ChatAssistant::getAssistantDescription, keyword);
        }

        wrapper.orderByDesc(ChatAssistant::getCreatedTime);
        this.page(pageObj, wrapper);

        Page<ChatAssistantDTO> dtoPage = new Page<>();
        dtoPage.setRecords(BeanUtil.copyToList(pageObj.getRecords(), ChatAssistantDTO.class));
        dtoPage.setTotal(pageObj.getTotal());
        dtoPage.setCurrent(pageObj.getCurrent());
        dtoPage.setSize(pageObj.getSize());
        return dtoPage;
    }

    @Override
    public List<ChatAssistantDTO> search(String keyword) {
        LambdaQueryWrapper<ChatAssistant> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(ChatAssistant::getAssistantName, keyword)
                .or()
                .like(ChatAssistant::getAssistantDescription, keyword);
        wrapper.eq(ChatAssistant::getUserId, StpUtil.getLoginIdAsLong());
        wrapper.orderByDesc(ChatAssistant::getCreatedTime);
        List<ChatAssistant> list = this.list(wrapper);
        return BeanUtil.copyToList(list, ChatAssistantDTO.class);
    }

    @Override
    public void updateAssistant(Long id, CreateChatAssistantDTO dto) {
        ChatAssistant chatAssistant = this.getById(id);
        if (chatAssistant == null) {
            throw new RuntimeException("聊天助理不存在");
        }

        ChatAssistant updateData = BeanUtil.copyProperties(dto, ChatAssistant.class);
        updateData.setId(id);
        this.updateById(updateData);
    }

    @Override
    public void deleteAssistant(Long id) {
        ChatAssistant chatAssistant = this.getById(id);
        if (chatAssistant == null) {
            throw new RuntimeException("聊天助理不存在");
        }
        this.removeById(id);
    }

}
