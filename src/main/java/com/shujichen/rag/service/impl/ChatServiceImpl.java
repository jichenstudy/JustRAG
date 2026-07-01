package com.shujichen.rag.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shujichen.rag.common.dto.assistant.ChatAssistantDTO;
import com.shujichen.rag.common.dto.chat.CitationDTO;
import com.shujichen.rag.common.dto.chat.ProcessStepDTO;
import com.shujichen.rag.common.dto.chat.StreamDoneDTO;
import com.shujichen.rag.common.dto.chat.StreamMessageDTO;
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
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.*;

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

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Flux<ServerSentEvent<String>> streamChat(Long sessionId, String userMessage, Long assistantId) {
        long startTime = System.currentTimeMillis();
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

        // 获取模型配置信息
        AiModelConfig modelConfig = aiModelConfigService.getModelConfigById(finalModelId);

        // 发送 MODEL_INFO 步骤
        ProcessStepDTO modelInfoStep = ProcessStepDTO.builder()
                .type("MODEL_INFO")
                .label("模型信息")
                .content(modelConfig.getModelName())
                .timestamp(System.currentTimeMillis())
                .build();

        Flux<ServerSentEvent<String>> modelInfoEvent = createStepEvent(modelInfoStep);

        // 调用AI流式响应，并保存完整响应
        StringBuilder fullResponse = new StringBuilder();
        List<ProcessStepDTO> allProcessSteps = new ArrayList<>();
        allProcessSteps.add(modelInfoStep); // 添加模型信息步骤

        // 捕获当前用户ID，传递给工具执行线程
        Long currentUserId = StpUtil.getLoginIdAsLong();

        // 调用流式对话，获取事件流和引用信息
        List<CitationDTO> allCitations = new ArrayList<>();
        Map.Entry<Flux<ServerSentEvent<String>>, List<CitationDTO>> result = doStreamChatWithSteps(sessionId, userMessage, systemPrompt, topN, finalModelId, knowledgeBase, historyMessages, assistant, currentUserId);
        Flux<ServerSentEvent<String>> baseStream = result.getKey();
        allCitations.addAll(result.getValue());

        Flux<ServerSentEvent<String>> contentStream = baseStream
                .doOnNext(sse -> {
                    // 提取 message 事件中的内容用于保存
                    if ("message".equals(sse.event())) {
                        try {
                            StreamMessageDTO msg = objectMapper.readValue(sse.data(), StreamMessageDTO.class);
                            fullResponse.append(msg.getContent());
                        } catch (Exception ignored) {
                        }
                    }
                    // 收集 step 事件中的过程步骤
                    else if ("step".equals(sse.event())) {
                        try {
                            ProcessStepDTO step = objectMapper.readValue(sse.data(), ProcessStepDTO.class);
                            allProcessSteps.add(step);
                        } catch (Exception ignored) {
                        }
                    }
                    // citations 事件已在 doStreamChatWithSteps 返回值中，无需重复收集
                });

        // 完成后发送 done 事件（先保存消息获取ID）
        Flux<ServerSentEvent<String>> doneEvent = Flux.defer(() -> {
            long elapsedMs = System.currentTimeMillis() - startTime;

            // 先保存消息到数据库，获取真实ID
            Long messageId = null;
            try {
                ChatMessage assistantMsg = new ChatMessage();
                assistantMsg.setSessionId(sessionId);
                assistantMsg.setRole(MessageRole.ASSISTANT.getCode());
                assistantMsg.setContent(fullResponse.toString());

                if (!allProcessSteps.isEmpty()) {
                    String processStepsJson = objectMapper.writeValueAsString(allProcessSteps);
                    assistantMsg.setProcessSteps(processStepsJson);
                }

                if (!allCitations.isEmpty()) {
                    String citationsJson = objectMapper.writeValueAsString(allCitations);
                    assistantMsg.setCitations(citationsJson);
                }

                assistantMsg.setCreatedAt(LocalDateTime.now());
                chatMessageMapper.insert(assistantMsg);
                messageId = assistantMsg.getId();
                log.info("助手消息保存成功,会话ID: {}, 消息ID: {}, 过程步骤数: {}",
                        sessionId, messageId, allProcessSteps.size());
            } catch (Exception e) {
                log.error("保存助手消息失败,会话ID: {}", sessionId, e);
            }

            StreamDoneDTO doneDTO = StreamDoneDTO.builder()
                    .totalTokens(new HashMap<>())
                    .totalElapsedMs(elapsedMs)
                    .messageId(messageId)
                    .build();
            return createDoneEvent(doneDTO);
        });

        return modelInfoEvent
                .concatWith(contentStream)
                .concatWith(doneEvent)
                .concatWith(Flux.defer(() -> Flux.just(createCloseEvent())))
                .doOnError(error -> log.error("流式对话失败,会话ID: {}", sessionId, error));
    }

    /**
     * 执行流式对话，返回内容 token 流。
     * 检索步骤通过 retrieveStepEvents 发出，与内容流拼接。
     * 返回 Pair<Flux, List<CitationDTO>>，其中 Flux 是 SSE 事件流，List<CitationDTO> 是引用信息列表
     */
    private Map.Entry<Flux<ServerSentEvent<String>>, List<CitationDTO>> doStreamChatWithSteps(
            Long sessionId,
            String currentMessage,
            String systemPrompt,
            Integer topN,
            Long chatModelId,
            KnowledgeBase knowledgeBase,
            List<Message> historyMessages,
            ChatAssistantDTO assistant,
            Long userId) {
        try {
            if (StrUtil.isBlank(systemPrompt)) {
                log.info("使用默认系统提示词");
                systemPrompt = RAG_SYSTEM_PROMPT;
            }

            // 使用 Sinks.many() 创建一个可以多播的 sink 用于工具调用事件
            reactor.core.publisher.Sinks.Many<ServerSentEvent<String>> toolEventsSink =
                    reactor.core.publisher.Sinks.many().unicast().onBackpressureBuffer();

            // 创建事件消费者，直接发送到 sink
            java.util.function.Consumer<ProcessStepDTO> toolEventConsumer = event -> {
                toolEventsSink.tryEmitNext(createStepSse(event));
            };

            // 根据modelId动态创建ChatClient（传入事件消费者）
            AiModelConfig modelConfig = aiModelConfigService.getModelConfigById(chatModelId);
            ChatClient chatClient = chatClientFactory.createChatClient(modelConfig, assistant, toolEventConsumer);
            log.info("使用AI模型: {}", modelConfig.getModelName());

            String conversationId = String.valueOf(sessionId);

            // 如果没有关联知识库，直接进行普通聊天
            if (knowledgeBase == null) {
                log.info("未关联知识库，使用普通聊天模式（带记忆），conversationId: {}", conversationId);
                return new AbstractMap.SimpleEntry<>(
                    doStreamContent(chatClient, systemPrompt, currentMessage, historyMessages, toolEventsSink, userId),
                    new ArrayList<>()
                );
            }

            // 获取知识库对应的 VectorStore
            VectorStore vectorStore = vectorStoreStrategyFactory.getStrategy().getVectorStore(knowledgeBase);
            if (vectorStore == null) {
                log.warn("VectorStore 不可用，collection: {}，将使用普通聊天模式",
                        knowledgeBase.getCollectionsName());
                return new AbstractMap.SimpleEntry<>(
                    doStreamContent(chatClient, systemPrompt, currentMessage, historyMessages, toolEventsSink, userId),
                    new ArrayList<>()
                );
            }

            // RAG 检索过程
            log.info("使用RAG模式（带记忆），知识库: {}, collection: {}, conversationId: {}",
                    knowledgeBase.getName(), knowledgeBase.getCollectionsName(), conversationId);

            long retrieveStart = System.currentTimeMillis();

            // 发送 RETRIEVE_START
            ProcessStepDTO retrieveStartStep = ProcessStepDTO.builder()
                    .type("RETRIEVE_START")
                    .label("知识库检索")
                    .timestamp(retrieveStart)
                    .build();

            int retrieveTopK = topN != null ? topN : 5;
            // 设置相似度阈值，过滤不相关的文档
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(currentMessage)
                    .topK(retrieveTopK)
                    .similarityThreshold(0.7) // 只保留相似度 >= 0.7 的文档
                    .build();
            List<Document> retrievedDocs = vectorStore.similaritySearch(searchRequest);
            long retrieveElapsed = System.currentTimeMillis() - retrieveStart;
            log.info("【RAG】检索到 {} 条相关文档, 耗时 {}ms", retrievedDocs.size(), retrieveElapsed);

            // 只有检索到相关文档时才发送检索步骤和引用信息
            Flux<ServerSentEvent<String>> retrieveEvents = Flux.just();
            List<CitationDTO> citationsList = new ArrayList<>();

            if (!retrievedDocs.isEmpty()) {
                // 发送 RETRIEVE_END
                ProcessStepDTO retrieveEndStep = ProcessStepDTO.builder()
                        .type("RETRIEVE_END")
                        .label("检索完成")
                        .documentsCount(retrievedDocs.size())
                        .elapsedMs(retrieveElapsed)
                        .timestamp(System.currentTimeMillis())
                        .build();

                // 构造引用信息列表（去重，只存简述信息）
                Set<String> addedChunkIds = new HashSet<>();
                int citationIndex = 1;
                for (int i = 0; i < retrievedDocs.size(); i++) {
                    Document doc = retrievedDocs.get(i);
                    Map<String, Object> metadata = doc.getMetadata() != null ? doc.getMetadata() : new HashMap<>();

                    // 从metadata中提取字段，尝试多种可能的key
                    String docId = extractMetadataField(metadata, "docId", "documentId", "doc_id");
                    String docName = extractMetadataField(metadata, "docName", "documentName", "title", "name");
                    String chunkId = extractMetadataField(metadata, "chunkId", "chunk_id", "id");

                    // 如果chunkId为空，使用doc.getId()
                    if (chunkId == null || chunkId.isEmpty()) {
                        chunkId = doc.getId();
                    }

                    // 去重：同一个chunkId只添加一次
                    if (chunkId != null && !addedChunkIds.contains(chunkId)) {
                        addedChunkIds.add(chunkId);

                        // 提取简述信息：标题 + 前200字符
                        String fullContent = doc.getText();
                        String preview = generatePreview(fullContent, docName);

                        CitationDTO citation = CitationDTO.builder()
                                .index(citationIndex++)
                                .docId(docId)
                                .docName(docName)
                                .preview(preview)
                                .score(doc.getScore() != null ? doc.getScore() : 0.0)
                                .chunkId(chunkId)
                                .knowledgeBaseId(knowledgeBase.getId())
                                .build();
                        citationsList.add(citation);
                    }
                }

                // 构造 citations SSE 事件
                ServerSentEvent<String> citationsEvent = ServerSentEvent.<String>builder()
                        .event("citations")
                        .data(objectMapper.writeValueAsString(citationsList))
                        .build();

                retrieveEvents = Flux.just(
                        createStepSse(retrieveStartStep),
                        createStepSse(retrieveEndStep),
                        citationsEvent
                );
            }

            // 构建带RAG上下文的用户消息
            String userMessageWithContext;
            if (!retrievedDocs.isEmpty()) {
                StringBuilder context = new StringBuilder();
                context.append("以下是知识库中检索到的参考资料（每份资料前有编号 [N]）：\n\n");
                for (int i = 0; i < retrievedDocs.size(); i++) {
                    Document doc = retrievedDocs.get(i);
                    context.append("[参考资料 ").append(i + 1).append("]\n");
                    context.append(doc.getText()).append("\n\n");
                }
                context.append("## 引用要求\n");
                context.append("在回答中，基于参考资料得出的结论需要标注引用编号，格式为 [N]，多来源用 [N,M]。\n");
                context.append("示例：'Python 是一种解释型语言[1]，支持多种编程范式[2]。'\n");
                context.append("规则：\n");
                context.append("1. 在段落或句子末尾标注引用，不要每行都标注\n");
                context.append("2. 表格内容只需在表格开头或结尾标注一次引用，表格内不要标注\n");
                context.append("3. 列表内容只需在列表开头或结尾标注一次引用，列表项内不要标注\n");
                context.append("4. 引用标记放在句号、逗号之前\n");
                context.append("5. 多个来源用 [1,2] 格式\n");
                context.append("6. 不要编造参考资料中不存在的内容\n");
                context.append("7. 如果回答完全不依赖参考资料（如闲聊），则不添加引用标记\n\n");
                context.append("请优先使用以上参考资料回答问题。如果参考资料不足以回答，使用可用的工具来获取信息。\n\n");
                context.append("用户问题：").append(currentMessage);
                userMessageWithContext = context.toString();
            } else {
                userMessageWithContext = currentMessage;
            }

            // 检索事件 + 内容流（包含工具调用事件）
            Flux<ServerSentEvent<String>> contentEvents = doStreamContent(
                    chatClient, systemPrompt, userMessageWithContext, historyMessages, toolEventsSink, userId);

            Flux<ServerSentEvent<String>> resultFlux = retrieveEvents.concatWith(contentEvents);
            return new AbstractMap.SimpleEntry<>(resultFlux, citationsList);

        } catch (Exception e) {
            log.error("流式对话失败", e);
            return new AbstractMap.SimpleEntry<>(Flux.error(e), new ArrayList<>());
        }
    }

    /**
     * 普通流式内容输出（包含工具调用追踪）
     */
    private Flux<ServerSentEvent<String>> doStreamContent(
            ChatClient chatClient, String systemPrompt, String userMessage,
            List<Message> historyMessages, reactor.core.publisher.Sinks.Many<ServerSentEvent<String>> toolEventsSink, Long userId) {

        // 使用 defer 在流订阅时（实际执行线程）设置用户上下文
        Flux<ServerSentEvent<String>> contentFlux = Flux.defer(() -> {
            return chatClient
                    .prompt(systemPrompt)
                    .messages(historyMessages)
                    .user(userMessage)
                    .stream()
                    .content()
                    .map(this::createMessageEvent)
                    .doFinally(signal -> {
                        // 内容流结束后，延迟一小段时间再完成工具事件流
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException ignored) {}
                        toolEventsSink.tryEmitComplete();
                    });
        });

        // 合并内容流和工具调用事件流
        return Flux.merge(
                contentFlux,
                toolEventsSink.asFlux()
        );
    }

    // ==================== SSE 事件构造辅助方法 ====================

    private ServerSentEvent<String> createStepSse(ProcessStepDTO step) {
        try {
            return ServerSentEvent.<String>builder()
                    .event("step")
                    .data(objectMapper.writeValueAsString(step))
                    .build();
        } catch (Exception e) {
            log.error("序列化 ProcessStepDTO 失败", e);
            return ServerSentEvent.<String>builder()
                    .event("step")
                    .data("{}")
                    .build();
        }
    }

    private Flux<ServerSentEvent<String>> createStepEvent(ProcessStepDTO step) {
        return Flux.just(createStepSse(step));
    }

    private ServerSentEvent<String> createMessageEvent(String content) {
        try {
            StreamMessageDTO msg = StreamMessageDTO.builder().content(content).build();
            return ServerSentEvent.<String>builder()
                    .event("message")
                    .data(objectMapper.writeValueAsString(msg))
                    .build();
        } catch (Exception e) {
            log.error("序列化 StreamMessageDTO 失败", e);
            return ServerSentEvent.<String>builder()
                    .event("message")
                    .data("{\"content\":\"\"}")
                    .build();
        }
    }

    private Flux<ServerSentEvent<String>> createDoneEvent(StreamDoneDTO done) {
        try {
            return Flux.just(ServerSentEvent.<String>builder()
                    .event("done")
                    .data(objectMapper.writeValueAsString(done))
                    .build());
        } catch (Exception e) {
            log.error("序列化 StreamDoneDTO 失败", e);
            return Flux.just(ServerSentEvent.<String>builder()
                    .event("done")
                    .data("{}")
                    .build());
        }
    }

    private ServerSentEvent<String> createCloseEvent() {
        return ServerSentEvent.<String>builder()
                .event("close")
                .data("")
                .build();
    }

    /**
     * 从metadata中提取字段，尝试多个可能的key
     */
    private String extractMetadataField(Map<String, Object> metadata, String... keys) {
        for (String key : keys) {
            Object value = metadata.get(key);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }

    /**
     * 生成简述信息：标题 + 前200字符
     */
    private String generatePreview(String fullContent, String docName) {
        if (fullContent == null || fullContent.isEmpty()) {
            return docName != null ? docName : "";
        }

        // 提取标题（第一行）
        String title = "";
        String[] lines = fullContent.split("\n");
        if (lines.length > 0) {
            title = lines[0].trim();
            // 移除Markdown标题标记
            title = title.replaceAll("^#+\\s*", "");
        }

        // 提取前200字符作为摘要
        String content = fullContent.length() > 200 ? fullContent.substring(0, 200) + "..." : fullContent;
        // 移除Markdown标记
        content = content.replaceAll("[#*`\\[\\]]", "");

        if (title.isEmpty()) {
            return content;
        } else {
            return title + "\n" + content;
        }
    }
}
