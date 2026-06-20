package com.shujichen.rag.memory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shujichen.rag.common.enums.MessageRole;
import com.shujichen.rag.entity.ChatMessage;
import com.shujichen.rag.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 基于数据库的 ChatMemory 实现
 * 使用现有的 chat_message 表存储对话历史
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseChatMemory implements ChatMemory {

    private final ChatMessageMapper chatMessageMapper;

    @Override
    public void add(String conversationId, Message message) {
        ChatMemory.super.add(conversationId, message);
    }

    /**
     * 添加消息到指定会话
     */
    @Override
    public void add(String conversationId, List<Message> messages) {
        Long sessionId = Long.parseLong(conversationId);

        for (Message message : messages) {
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setSessionId(sessionId);
            chatMessage.setRole(convertMessageTypeToRole(message.getMessageType()));
            chatMessage.setContent(message.getText());
            chatMessage.setCreatedAt(LocalDateTime.now());

            chatMessageMapper.insert(chatMessage);
            log.debug("保存消息到数据库, sessionId: {}, role: {}", sessionId, chatMessage.getRole());
        }
    }

    /**
     * 获取指定会话的最近N条消息
     */
    @Override
    public List<Message> get(String conversationId) {
        Long sessionId = Long.parseLong(conversationId);

        // 查询最近的N条消息（按时间倒序，然后反转）
        LambdaQueryWrapper<ChatMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatMessage::getSessionId, sessionId)
                .orderByDesc(ChatMessage::getCreatedAt)
                .last("LIMIT " + 20);

        List<ChatMessage> dbMessages = chatMessageMapper.selectList(queryWrapper);

        // 反转顺序（从旧到新）
        Collections.reverse(dbMessages);

        // 转换为 Spring AI Message
        List<Message> messages = new ArrayList<>();
        for (ChatMessage dbMsg : dbMessages) {
            Message message = convertToMessage(dbMsg);
            if (message != null) {
                messages.add(message);
            }
        }

        log.debug("从数据库加载 {} 条消息, sessionId: {}", messages.size(), sessionId);
        return messages;
    }

    /**
     * 清除指定会话的所有消息
     */
    @Override
    public void clear(String conversationId) {
        Long sessionId = Long.parseLong(conversationId);

        LambdaQueryWrapper<ChatMessage> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(ChatMessage::getSessionId, sessionId);
        chatMessageMapper.delete(deleteWrapper);

        log.info("清除会话消息, sessionId: {}", sessionId);
    }

    /**
     * 将数据库消息转换为 Spring AI Message
     */
    private Message convertToMessage(ChatMessage dbMessage) {
        String role = dbMessage.getRole();
        String content = dbMessage.getContent();

        if (MessageRole.USER.getCode().equals(role)) {
            return new UserMessage(content);
        } else if (MessageRole.ASSISTANT.getCode().equals(role)) {
            return new AssistantMessage(content);
        }
        return null;
    }

    /**
     * 将 MessageType 转换为角色字符串
     */
    private String convertMessageTypeToRole(MessageType messageType) {
        return switch (messageType) {
            case USER -> MessageRole.USER.getCode();
            case ASSISTANT -> MessageRole.ASSISTANT.getCode();
            default -> MessageRole.ASSISTANT.getCode();
        };
    }
}
