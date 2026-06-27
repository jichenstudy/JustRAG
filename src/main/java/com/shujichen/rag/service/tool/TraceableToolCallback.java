package com.shujichen.rag.service.tool;

import com.shujichen.rag.common.dto.chat.ProcessStepDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.function.Consumer;

/**
 * 可追踪的工具回调包装类
 * 用于捕获工具调用的开始和结束事件,并发送到 SSE 流中
 */
@Slf4j
public class TraceableToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final Consumer<ProcessStepDTO> eventConsumer;

    public TraceableToolCallback(ToolCallback delegate, Consumer<ProcessStepDTO> eventConsumer) {
        this.delegate = delegate;
        this.eventConsumer = eventConsumer;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public String call(String toolInput) {
        long startTime = System.currentTimeMillis();
        String toolName = getToolDefinition().name();

        // 发送工具调用开始事件
        ProcessStepDTO startEvent = ProcessStepDTO.builder()
                .type("TOOL_CALL_START")
                .label("调用工具")
                .toolName(toolName)
                .input(toolInput)
                .timestamp(startTime)
                .build();

        eventConsumer.accept(startEvent);
        log.debug("工具调用开始: {} 输入: {}", toolName, toolInput);

        try {
            // 执行实际的工具调用
            String result = delegate.call(toolInput);
            long elapsedMs = System.currentTimeMillis() - startTime;

            // 发送工具调用结束事件
            ProcessStepDTO endEvent = ProcessStepDTO.builder()
                    .type("TOOL_CALL_END")
                    .label("工具返回")
                    .toolName(toolName)
//                    .output(result)
                    .elapsedMs(elapsedMs)
                    .timestamp(System.currentTimeMillis())
                    .build();

            eventConsumer.accept(endEvent);
            log.debug("工具调用结束: {} 耗时: {}ms 输出: {}", toolName, elapsedMs, result);

            return result;
        } catch (Exception e) {
            long elapsedMs = System.currentTimeMillis() - startTime;
            log.error("工具调用失败: {} 耗时: {}ms", toolName, elapsedMs, e);

            // 发送错误事件
            ProcessStepDTO errorEvent = ProcessStepDTO.builder()
                    .type("ERROR")
                    .label("工具调用失败")
                    .toolName(toolName)
                    .content(e.getMessage())
                    .elapsedMs(elapsedMs)
                    .timestamp(System.currentTimeMillis())
                    .build();

            eventConsumer.accept(errorEvent);
            throw e;
        }
    }
}
