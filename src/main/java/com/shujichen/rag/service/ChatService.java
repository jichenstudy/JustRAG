package com.shujichen.rag.service;

import com.shujichen.rag.entity.ChatMessage;
import com.shujichen.rag.entity.ChatSession;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 聊天服务接口
 */
public interface ChatService {

    /**
     * 创建聊天会话
     *
     * @param assistantId     聊天助理ID
     * @param knowledgeBaseId 知识库ID
     * @param title           会话标题
     * @param modelId         AI模型ID
     * @return 会话ID
     */
    Long createChatSession(Long assistantId, Long knowledgeBaseId, String title, Long modelId);

    /**
     * 发送用户消息
     *
     * @param sessionId 会话ID
     * @param content   消息内容
     * @return 消息ID
     */
    Long sendUserMessage(Long sessionId, String content);

    /**
     * 保存助手消息
     *
     * @param sessionId 会话ID
     * @param content   消息内容
     * @return 消息ID
     */
    Long saveAssistantMessage(Long sessionId, String content);

    /**
     * 更新会话标题
     *
     * @param sessionId 会话ID
     * @param newTitle  新标题
     */
    void updateSessionTitle(Long sessionId, String newTitle);

    /**
     * 删除聊天会话
     *
     * @param sessionId 会话ID
     */
    void deleteChatSession(Long sessionId);

    /**
     * 根据ID获取会话
     *
     * @param sessionId 会话ID
     * @return 会话信息
     */
    ChatSession getChatSessionById(Long sessionId);

    /**
     * 获取所有会话
     *
     * @return 会话列表
     */
    List<ChatSession> getAllChatSessions();

    /**
     * 根据助理ID获取会话列表
     *
     * @param assistantId 助理ID
     * @return 会话列表
     */
    List<ChatSession> getChatSessionsByAssistantId(Long assistantId);

    /**
     * 根据知识库ID获取会话列表
     *
     * @param knowledgeBaseId 知识库ID
     * @return 会话列表
     */
    List<ChatSession> getChatSessionsByKnowledgeBaseId(Long knowledgeBaseId);

    /**
     * 获取所有自由对话会话（无知识库关联）
     *
     * @return 会话列表
     */
    List<ChatSession> getAllFreeChatSessions();

    /**
     * 获取会话的所有消息
     *
     * @param sessionId 会话ID
     * @return 消息列表
     */
    List<ChatMessage> getChatMessages(Long sessionId);

    /**
     * 获取会话的最新消息
     *
     * @param sessionId 会话ID
     * @param limit     数量限制
     * @return 消息列表
     */
    List<ChatMessage> getLatestMessages(Long sessionId, int limit);

    /**
     * 流式对话
     *
     * @param sessionId   会话ID
     * @param userMessage 用户消息
     * @param assistantId 助理ID
     * @return 流式响应（结构化SSE事件）
     */
    Flux<ServerSentEvent<String>> streamChat(Long sessionId, String userMessage, Long assistantId);
}
