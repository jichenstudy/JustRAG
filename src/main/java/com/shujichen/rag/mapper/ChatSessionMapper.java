package com.shujichen.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shujichen.rag.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天会话Mapper接口
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
}