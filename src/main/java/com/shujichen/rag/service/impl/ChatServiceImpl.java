package com.shujichen.rag.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shujichen.rag.common.dto.assistant.ChatAssistantDTO;
import com.shujichen.rag.common.enums.MessageRole;
import com.shujichen.rag.entity.AiModelConfig;
import com.shujichen.rag.entity.ChatMessage;
import com.shujichen.rag.entity.ChatSession;
import com.shujichen.rag.entity.KnowledgeBase;
import com.shujichen.rag.factory.ChatClientFactory;
import com.shujichen.rag.factory.VectorStoreStrategyFactory;
import com.shujichen.rag.mapper.ChatMessageMapper;
import com.shujichen.rag.mapper.ChatSessionMapper;
import com.shujichen.rag.mapper.KnowledgeBaseMapper;
import com.shujichen.rag.service.AiModelConfigService;
import com.shujichen.rag.service.ChatAssistantService;
import com.shujichen.rag.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final int DEFAULT_MEMORY_SIZE = 20;

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final AiModelConfigService aiModelConfigService;
    private final ChatAssistantService chatAssistantService;
    private final ChatClientFactory chatClientFactory;
    private final VectorStoreStrategyFactory vectorStoreStrategyFactory;
    private final ChatMemory chatMemory;  // 自定义的 DatabaseChatMemory

    private static final String RAG_SYSTEM_PROMPT = """
            你是一个专业的知识库问答助手。
            请根据提供的参考资料回答用户问题。
            如果参考资料不足，你可以使用可用的工具（如搜索、地图、天气等）来获取外部信息，优先通过工具调用获取实时或外部数据。

            ## 回答要求：
            1. 优先使用参考资料中的信息
            2. 参考资料不足时，使用可用工具获取信息
            3. 回答要准确、简洁、专业、有条理
            4. 可以适当引用资料来源，如"根据[文档名]..."
            5. 如果问题涉及多个方面，请分点说明
            """;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createChatSession(Long assistantId, Long knowledgeBaseId, String title, Long modelId) {
        if (knowledgeBaseId != null) {
            KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);
            if (knowledgeBase == null) {
                throw new IllegalArgumentException("知识库不存在,ID: " + knowledgeBaseId);
            }
        }

        // 如果未指定模型，使用启用的聊天模型
        if (modelId == null) {
            AiModelConfig chatModel = aiModelConfigService.getEnabledModelConfigByType("CHAT");
            if (chatModel == null) {
                throw new IllegalArgumentException("没有可用的聊天模型配置,请在管理页面添加模型");
            }
            modelId = chatModel.getId();
        }

        ChatSession session = new ChatSession();
        session.setAssistantId(assistantId);
        session.setKnowledgeBaseId(knowledgeBaseId);
        session.setTitle(title != null ? title : "新对话");
        session.setModelId(modelId);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());

        chatSessionMapper.insert(session);

        log.info("聊天会话创建成功,会话ID: {}, 助理ID: {}, 知识库ID: {}, 模型ID: {}", session.getId(), assistantId, knowledgeBaseId, modelId);
        return session.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long sendUserMessage(Long sessionId, String content) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("会话不存在,ID: " + sessionId);
        }

        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setRole(MessageRole.USER.getCode());
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());

        chatMessageMapper.insert(message);

        session.setUpdatedAt(LocalDateTime.now());
        if (session.getTitle() == null || "新对话".equals(session.getTitle())) {
            session.setTitle(content.length() > 20 ? content.substring(0, 20) + "..." : content);
        }
        chatSessionMapper.updateById(session);

        log.info("用户消息发送成功,会话ID: {}, 消息ID: {}", sessionId, message.getId());
        return message.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveAssistantMessage(Long sessionId, String content) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("会话不存在,ID: " + sessionId);
        }

        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setRole(MessageRole.ASSISTANT.getCode());
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());

        chatMessageMapper.insert(message);

        session.setUpdatedAt(LocalDateTime.now());
        chatSessionMapper.updateById(session);

        log.info("助手消息保存成功,会话ID: {}, 消息ID: {}", sessionId, message.getId());
        return message.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSessionTitle(Long sessionId, String newTitle) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("会话不存在,ID: " + sessionId);
        }

        session.setTitle(newTitle);
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionMapper.updateById(session);

        log.info("会话标题更新成功,会话ID: {}, 新标题: {}", sessionId, newTitle);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChatSession(Long sessionId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("会话不存在,ID: " + sessionId);
        }

        LambdaQueryWrapper<ChatMessage> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(ChatMessage::getSessionId, sessionId);
        chatMessageMapper.delete(deleteWrapper);

        chatSessionMapper.deleteById(sessionId);

        log.info("会话删除成功,会话ID: {}", sessionId);
    }

    @Override
    public ChatSession getChatSessionById(Long sessionId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("会话不存在,ID: " + sessionId);
        }
        return session;
    }

    @Override
    public List<ChatSession> getAllChatSessions() {
        LambdaQueryWrapper<ChatSession> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(ChatSession::getUpdatedAt);
        return chatSessionMapper.selectList(queryWrapper);
    }

    @Override
    public List<ChatSession> getChatSessionsByAssistantId(Long assistantId) {
        LambdaQueryWrapper<ChatSession> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatSession::getAssistantId, assistantId);
        queryWrapper.orderByDesc(ChatSession::getUpdatedAt);
        return chatSessionMapper.selectList(queryWrapper);
    }

    @Override
    public List<ChatSession> getChatSessionsByKnowledgeBaseId(Long knowledgeBaseId) {
        LambdaQueryWrapper<ChatSession> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatSession::getKnowledgeBaseId, knowledgeBaseId);
        queryWrapper.orderByDesc(ChatSession::getUpdatedAt);
        return chatSessionMapper.selectList(queryWrapper);
    }

    @Override
    public List<ChatSession> getAllFreeChatSessions() {
        LambdaQueryWrapper<ChatSession> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.isNull(ChatSession::getKnowledgeBaseId);
        queryWrapper.orderByDesc(ChatSession::getUpdatedAt);
        return chatSessionMapper.selectList(queryWrapper);
    }

    @Override
    public List<ChatMessage> getChatMessages(Long sessionId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("会话不存在,ID: " + sessionId);
        }

        LambdaQueryWrapper<ChatMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatMessage::getSessionId, sessionId);
        queryWrapper.orderByAsc(ChatMessage::getCreatedAt);
        return chatMessageMapper.selectList(queryWrapper);
    }

    @Override
    public List<ChatMessage> getLatestMessages(Long sessionId, int limit) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("会话不存在,ID: " + sessionId);
        }

        LambdaQueryWrapper<ChatMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatMessage::getSessionId, sessionId);
        queryWrapper.orderByDesc(ChatMessage::getCreatedAt);
        queryWrapper.last("LIMIT " + limit);
        List<ChatMessage> messages = chatMessageMapper.selectList(queryWrapper);

        // 反转顺序
        java.util.Collections.reverse(messages);
        return messages;
    }

    @Override
    public Flux<String> streamChat(Long sessionId, String userMessage, Long assistantId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("会话不存在,ID: " + sessionId);
        }

        // 获取当前聊天的配置信息
        ChatAssistantDTO assistant = chatAssistantService.getAssistantById(assistantId);

        // 确定使用的模型ID
        Long finalModelId = assistant.getModelId();
        if (finalModelId == null) {
            // 优先使用会话关联的模型
            finalModelId = session.getModelId();
        }
        if (finalModelId == null) {
            // 如果会话也没有指定模型，使用启用的聊天模型
            AiModelConfig chatModel = aiModelConfigService.getEnabledModelConfigByType("CHAT");
            if (chatModel == null) {
                return Flux.error(new IllegalArgumentException("没有可用的聊天模型配置,请在管理页面添加模型"));
            }
            finalModelId = chatModel.getId();
        }

        // 先获取历史消息（在保存用户消息之前）
        String conversationId = String.valueOf(sessionId);
        List<Message> historyMessages = chatMemory.get(conversationId);

        // 保存用户消息到数据库
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setRole(MessageRole.USER.getCode());
        userMsg.setContent(userMessage);
        userMsg.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(userMsg);

        // 更新会话标题
        session.setUpdatedAt(LocalDateTime.now());
        if (session.getTitle() == null || "新对话".equals(session.getTitle())) {
            session.setTitle(userMessage.length() > 20 ? userMessage.substring(0, 20) + "..." : userMessage);
        }
        chatSessionMapper.updateById(session);

        // 构建系统提示词
        Integer topN = assistant.getTopN();
        String systemPrompt = assistant.getSystemPrompt();

        // 获取知识库信息（用于RAG检索）
        Long knowledgeBaseId = assistant.getKnowledgeBaseId();
        KnowledgeBase knowledgeBase = null;
        if (knowledgeBaseId != null) {
            knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);
        }

        // 调用AI流式响应，并保存完整响应
        StringBuilder fullResponse = new StringBuilder();
        return doStreamChat(sessionId, userMessage, systemPrompt, topN, finalModelId, knowledgeBase, historyMessages,assistant)
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> {
                    try {
                        // 保存助手消息到数据库
                        ChatMessage assistantMsg = new ChatMessage();
                        assistantMsg.setSessionId(sessionId);
                        assistantMsg.setRole(MessageRole.ASSISTANT.getCode());
                        assistantMsg.setContent(fullResponse.toString());
                        assistantMsg.setCreatedAt(LocalDateTime.now());
                        chatMessageMapper.insert(assistantMsg);
                        log.info("助手消息保存成功,会话ID: {}, 消息ID: {}", sessionId, assistantMsg.getId());
                    } catch (Exception e) {
                        log.error("保存助手消息失败,会话ID: {}", sessionId, e);
                    }
                })
                .doOnError(error -> log.error("流式对话失败,会话ID: {}", sessionId, error));
    }

    private Flux<String> doStreamChat(
            Long sessionId,
            String currentMessage,
            String systemPrompt,
            Integer topN,
            Long chatModelId,
            KnowledgeBase knowledgeBase,
            List<Message> historyMessages,
            ChatAssistantDTO assistant) {
        try {
            if (StrUtil.isBlank(systemPrompt)) {
                log.info("使用默认系统提示词");
                systemPrompt = RAG_SYSTEM_PROMPT;
            }

            // 根据modelId动态创建ChatClient
            AiModelConfig modelConfig = aiModelConfigService.getModelConfigById(chatModelId);
            ChatClient chatClient = chatClientFactory.createChatClient(modelConfig,assistant);
            log.info("使用AI模型: {}", modelConfig.getModelName());

            // 使用sessionId作为conversationId
            String conversationId = String.valueOf(sessionId);

            // 如果没有关联知识库，直接进行普通聊天
            if (knowledgeBase == null) {
                log.info("未关联知识库，使用普通聊天模式（带记忆），conversationId: {}", conversationId);
                return chatClient
                        .prompt(systemPrompt)
                        .user(currentMessage)
                        .messages(historyMessages)
                        .stream()
                        .content();
            }

            // 获取知识库对应的 VectorStore（包含对应的向量模型和collection）
            VectorStore vectorStore = vectorStoreStrategyFactory.getStrategy().getVectorStore(knowledgeBase);
            if (vectorStore == null) {
                log.warn("VectorStore 不可用，collection: {}，将使用普通聊天模式",
                        knowledgeBase.getCollectionsName());
                return chatClient
                        .prompt(systemPrompt)
                        .user(currentMessage)
                        .messages(historyMessages)
                        .stream()
                        .content();
            }

            log.info("使用RAG模式（带记忆），知识库: {}, collection: {}, conversationId: {}",
                    knowledgeBase.getName(), knowledgeBase.getCollectionsName(), conversationId);

            // RAG模式手动检索
            int retrieveTopK = topN != null ? topN : 5;
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(currentMessage)
                    .topK(retrieveTopK)
                    .build();
            List<Document> retrievedDocs = vectorStore.similaritySearch(searchRequest);
            log.info("【RAG】检索到 {} 条相关文档", retrievedDocs.size());

            // 手动构建带RAG上下文的用户消息
            String userMessageWithContext;
            if (!retrievedDocs.isEmpty()) {
                StringBuilder context = new StringBuilder();
                context.append("以下是知识库中检索到的参考资料：\n\n");
                for (int i = 0; i < retrievedDocs.size(); i++) {
                    Document doc = retrievedDocs.get(i);
                    context.append("--- 参考资料 ").append(i + 1).append(" ---\n");
                    context.append(doc.getText()).append("\n\n");
                }
                context.append("请优先使用以上参考资料回答问题。如果参考资料不足以回答，使用可用的工具来获取信息。\n\n");

                context.append("用户问题：").append(currentMessage);
                userMessageWithContext = context.toString();
            } else {
                userMessageWithContext = currentMessage;
            }

            // 流式输出
            return chatClient
                    .prompt(systemPrompt)
                    .messages(historyMessages)
                    .user(userMessageWithContext)
                    .stream()
                    .content();

        } catch (Exception e) {
            log.error("流式对话失败", e);
            return Flux.error(e);
        }
    }
}
