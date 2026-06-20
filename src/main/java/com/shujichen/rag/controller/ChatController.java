package com.shujichen.rag.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.shujichen.rag.common.dto.Result;
import com.shujichen.rag.common.dto.chat.ChatMessageDTO;
import com.shujichen.rag.common.dto.chat.ChatSessionDTO;
import com.shujichen.rag.common.dto.chat.CreateChatSessionDTO;
import com.shujichen.rag.common.dto.chat.SendMessageDTO;
import com.shujichen.rag.common.util.BeanCopyUtil;
import com.shujichen.rag.entity.ChatMessage;
import com.shujichen.rag.entity.ChatSession;
import com.shujichen.rag.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天控制器
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 创建聊天会话
     *
     * @param dto 创建会话参数
     * @return 会话ID
     */
    @PostMapping("/session")
    public Result<Long> createChatSession(@RequestBody CreateChatSessionDTO dto) {
        Long sessionId = chatService.createChatSession(
                dto.getAssistantId(),
                dto.getKnowledgeBaseId(),
                dto.getTitle(),
                dto.getModelId()
        );
        return Result.success(sessionId);
    }

    /**
     * 发送用户消息
     *
     * @param sessionId 会话ID
     * @param dto       消息内容
     * @return 消息ID
     */
    @PostMapping("/session/{sessionId}/message")
    public Result<Long> sendUserMessage(
            @PathVariable(name = "sessionId") Long sessionId,
            @Validated @RequestBody SendMessageDTO dto) {
        Long messageId = chatService.sendUserMessage(sessionId, dto.getContent());
        return Result.success(messageId);
    }

    /**
     * 更新会话标题
     *
     * @param sessionId 会话ID
     * @param title     新标题
     * @return 操作结果
     */
    @PutMapping("/session/{sessionId}/title")
    public Result<Void> updateSessionTitle(
            @PathVariable Long sessionId,
            @RequestParam String title) {
        chatService.updateSessionTitle(sessionId, title);
        return Result.success(null);
    }

    /**
     * 删除聊天会话
     *
     * @param sessionId 会话ID
     * @return 操作结果
     */
    @DeleteMapping("/session/{sessionId}")
    public Result<Void> deleteChatSession(@PathVariable Long sessionId) {
        chatService.deleteChatSession(sessionId);
        return Result.success(null);
    }

    /**
     * 根据ID获取会话详情
     *
     * @param sessionId 会话ID
     * @return 会话信息
     */
    @GetMapping("/session/{sessionId}")
    public Result<ChatSessionDTO> getChatSessionById(@PathVariable Long sessionId) {
        ChatSession session = chatService.getChatSessionById(sessionId);
        ChatSessionDTO dto = BeanCopyUtil.copyObj(session, ChatSessionDTO.class);
        return Result.success(dto);
    }

    /**
     * 获取所有会话列表
     *
     * @return 会话列表
     */
    @GetMapping("/session")
    public Result<List<ChatSessionDTO>> getAllChatSessions() {
        List<ChatSession> sessions = chatService.getAllChatSessions();
        List<ChatSessionDTO> dtoList = sessions.stream()
                .map(session -> BeanCopyUtil.copyObj(session, ChatSessionDTO.class))
                .collect(Collectors.toList());
        return Result.success(dtoList);
    }

    /**
     * 根据助理ID获取会话列表
     *
     * @param assistantId 助理ID
     * @return 会话列表
     */
    @GetMapping("/session/assistant/{assistantId}")
    public Result<List<ChatSessionDTO>> getChatSessionsByAssistantId(@PathVariable Long assistantId) {
        List<ChatSession> sessions = chatService.getChatSessionsByAssistantId(assistantId);
        List<ChatSessionDTO> dtoList = sessions.stream()
                .map(session -> BeanCopyUtil.copyObj(session, ChatSessionDTO.class))
                .collect(Collectors.toList());
        return Result.success(dtoList);
    }

    /**
     * 根据知识库ID获取会话列表
     *
     * @param knowledgeBaseId 知识库ID
     * @return 会话列表
     */
    @GetMapping("/session/knowledge-base/{knowledgeBaseId}")
    public Result<List<ChatSessionDTO>> getChatSessionsByKnowledgeBaseId(@PathVariable Long knowledgeBaseId) {
        List<ChatSession> sessions = chatService.getChatSessionsByKnowledgeBaseId(knowledgeBaseId);
        List<ChatSessionDTO> dtoList = sessions.stream()
                .map(session -> BeanCopyUtil.copyObj(session, ChatSessionDTO.class))
                .collect(Collectors.toList());
        return Result.success(dtoList);
    }

    /**
     * 获取所有自由对话会话（无知识库关联）
     *
     * @return 会话列表
     */
    @GetMapping("/session/free-chat")
    public Result<List<ChatSessionDTO>> getAllFreeChatSessions() {
        List<ChatSession> sessions = chatService.getAllFreeChatSessions();
        List<ChatSessionDTO> dtoList = sessions.stream()
                .map(session -> BeanCopyUtil.copyObj(session, ChatSessionDTO.class))
                .collect(Collectors.toList());
        return Result.success(dtoList);
    }

    /**
     * 获取会话的所有消息
     *
     * @param sessionId 会话ID
     * @return 消息列表
     */
    @GetMapping("/session/{sessionId}/messages")
    public Result<List<ChatMessageDTO>> getChatMessages(@PathVariable Long sessionId) {
        List<ChatMessage> messages = chatService.getChatMessages(sessionId);
        List<ChatMessageDTO> dtoList = messages.stream()
                .map(msg -> {
                    ChatMessageDTO dto = BeanCopyUtil.copyObj(msg, ChatMessageDTO.class);
                    dto.setRole(msg.getRole());
                    return dto;
                })
                .collect(Collectors.toList());
        return Result.success(dtoList);
    }

    /**
     * 获取会话的最新消息
     *
     * @param sessionId 会话ID
     * @param limit     数量限制
     * @return 消息列表
     */
    @GetMapping("/session/{sessionId}/messages/latest")
    public Result<List<ChatMessageDTO>> getLatestMessages(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "10") int limit) {
        List<ChatMessage> messages = chatService.getLatestMessages(sessionId, limit);
        List<ChatMessageDTO> dtoList = messages.stream()
                .map(msg -> {
                    ChatMessageDTO dto = BeanCopyUtil.copyObj(msg, ChatMessageDTO.class);
                    dto.setRole(msg.getRole());
                    return dto;
                })
                .collect(Collectors.toList());
        return Result.success(dtoList);
    }

    /**
     * 流式对话接口
     *
     * @param sessionId   会话ID
     * @param message     用户消息
     * @param assistantId 助理ID
     * @param token       认证Token
     * @return 流式响应
     */
    @GetMapping(value = "/session/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(
            @PathVariable Long sessionId,
            @RequestParam String message,
            @RequestParam(required = false) Long assistantId,
            @RequestParam(required = false) String token) {
        if (token != null && !token.isEmpty()) {
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId == null) {
                return Flux.just(ServerSentEvent.<String>builder()
                        .event("error")
                        .data("Token无效或已过期")
                        .build());
            }
        } else {
            return Flux.just(ServerSentEvent.<String>builder()
                    .event("error")
                    .data("未提供认证Token")
                    .build());
        }

        return chatService.streamChat(sessionId, message, assistantId)
                .map(content -> ServerSentEvent.<String>builder()
                        .data(content)
                        .build())
                .concatWith(Flux.just(
                        ServerSentEvent.<String>builder()
                                .event("close")
                                .data("")
                                .build()
                ));
    }
}
