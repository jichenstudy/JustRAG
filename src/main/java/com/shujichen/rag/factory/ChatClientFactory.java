package com.shujichen.rag.factory;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import com.shujichen.rag.common.dto.assistant.ChatAssistantDTO;
import com.shujichen.rag.entity.AiModelConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.azure.openai.AzureOpenAiChatModel;
import org.springframework.ai.azure.openai.AzureOpenAiChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.ai.zhipuai.ZhiPuAiChatOptions;
import org.springframework.ai.zhipuai.api.ZhiPuAiApi;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * ChatClient工厂类 - 根据模型配置动态创建ChatClient
 * <p>
 * 支持的提供商:
 * - DashScope (阿里云通义千问)
 * - OpenAI (GPT系列)
 * - Ollama (本地模型)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatClientFactory {

    private final DynamicMcpManager mcpManager;

    /**
     * 根据模型配置创建ChatClient
     */
    public ChatClient createChatClient(AiModelConfig modelConfig, ChatAssistantDTO assistant) {
        log.info("创建ChatClient, 提供商: {}, 模型: {}", modelConfig.getProvider(), modelConfig.getModelName());

        if (assistant == null) {
            // 使用默认参数
            assistant = new ChatAssistantDTO();
            assistant.setTemperature(new BigDecimal("0.8"));
            assistant.setMaxTokens(2048);
            assistant.setTopP(new BigDecimal("0.9"));
            assistant.setPresencePenalty(new BigDecimal("1.0"));
            assistant.setFrequencyPenalty(new BigDecimal("1.0"));
            assistant.setEnableReasoningMode(0);
        }

        ChatModel chatModel = switch (modelConfig.getProvider()) {
            case "DASHSCOPE" -> createDashScopeChatModel(modelConfig, assistant);
            case "OPENAI" -> createOpenAiChatModel(modelConfig, assistant);
            case "AZURE OPENAI" -> createAzureOpenAIChatModel(modelConfig, assistant);
            case "ANTHROPIC" -> createAnthropicApiChatModel(modelConfig, assistant);
            case "OLLAMA" -> createOllamaChatModel(modelConfig, assistant);
            case "DEEPSEEK" -> createDeepSeekChatModel(modelConfig, assistant);
            case "ZHIPU" -> createZhiPuApiChatModel(modelConfig, assistant);
            default -> throw new IllegalArgumentException("不支持的模型提供商: " + modelConfig.getProvider());
        };

        if (mcpManager.getAllToolCallbacks().isEmpty()) {
            return ChatClient.builder(chatModel).defaultAdvisors(new SimpleLoggerAdvisor()).build();
        }

        // 使用 ChatModel 创建 ChatClient
        return ChatClient.builder(chatModel).defaultToolCallbacks(mcpManager.getAllToolCallbacks()).defaultAdvisors(new SimpleLoggerAdvisor()).build();
    }

    /**
     * 创建DashScope ChatModel (阿里云通义千问)
     */
    private ChatModel createDashScopeChatModel(AiModelConfig config, ChatAssistantDTO assistant) {
        DashScopeApi.Builder apiBuilder = DashScopeApi.builder()
                .apiKey(config.getApiKey());

        if (config.getApiEndpoint() != null && !config.getApiEndpoint().isBlank()) {
            apiBuilder.baseUrl(config.getApiEndpoint());
        }

        return DashScopeChatModel.builder()
                .dashScopeApi(apiBuilder.build())
                .defaultOptions(
                        DashScopeChatOptions.builder()
                                .withModel(config.getModelName())
                                .withTemperature(assistant.getTemperature().doubleValue())
                                .withMaxToken(assistant.getMaxTokens())
                                .withEnableThinking(assistant.getEnableReasoningMode() == 1)
                                .withRepetitionPenalty(assistant.getPresencePenalty().doubleValue())    // 需要大于0.0
                                .withTopP(assistant.getTopP().doubleValue())
                                .withMultiModel(true)   // 是否多模态
                                .build()
                )
                .build();
    }

    private ChatModel createAnthropicApiChatModel(AiModelConfig config, ChatAssistantDTO assistant) {
        AnthropicApi.Builder apiBuilder = AnthropicApi.builder()
                .apiKey(config.getApiKey());

        if (config.getApiEndpoint() != null && !config.getApiEndpoint().isBlank()) {
            apiBuilder.baseUrl(config.getApiEndpoint());
        }

        return AnthropicChatModel.builder()
                .anthropicApi(apiBuilder.build())
                .defaultOptions(
                        AnthropicChatOptions.builder()
                                .model(config.getModelName())
                                .temperature(assistant.getTemperature().doubleValue())
                                .maxTokens(assistant.getMaxTokens())
                                .topP(assistant.getTopP().doubleValue())
                                .build()
                ).build();
    }

    private ChatModel createAzureOpenAIChatModel(AiModelConfig config, ChatAssistantDTO assistant) {
        if (config.getApiEndpoint() == null || config.getApiEndpoint().isBlank()) {
            throw new IllegalArgumentException("Azure OpenAI 必须配置 API Endpoint");
        }

        OpenAIClientBuilder openAIClientBuilder = new OpenAIClientBuilder()
                .credential(new AzureKeyCredential(config.getApiKey()))
                .endpoint(config.getApiEndpoint());

        return AzureOpenAiChatModel.builder()
                .openAIClientBuilder(openAIClientBuilder)
                .defaultOptions(
                        AzureOpenAiChatOptions.builder()
                                .deploymentName(config.getModelName())
                                .temperature(assistant.getTemperature().doubleValue())
                                .maxTokens(assistant.getMaxTokens())
                                .frequencyPenalty(assistant.getFrequencyPenalty().doubleValue())
                                .topP(assistant.getTopP().doubleValue())
                                .build()
                )
                .build();
    }

    /**
     * 创建OpenAI ChatModel
     */
    private ChatModel createOpenAiChatModel(AiModelConfig config, ChatAssistantDTO assistant) {
        OpenAiApi.Builder apiBuilder = OpenAiApi.builder()
                .apiKey(config.getApiKey());

        if (config.getApiEndpoint() != null && !config.getApiEndpoint().isBlank()) {
            apiBuilder.baseUrl(config.getApiEndpoint());
        }

        return OpenAiChatModel.builder()
                .openAiApi(apiBuilder.build())
                .defaultOptions(
                        OpenAiChatOptions.builder()
                                .model(config.getModelName())
                                .temperature(assistant.getTemperature().doubleValue())
                                .maxTokens(assistant.getMaxTokens())
                                .frequencyPenalty(assistant.getFrequencyPenalty().doubleValue())
                                .topP(assistant.getTopP().doubleValue())
                                .build()
                )
                .build();
    }

    /**
     * 创建Ollama ChatModel (本地模型)
     */
    private ChatModel createOllamaChatModel(AiModelConfig config, ChatAssistantDTO assistant) {
        OllamaApi.Builder apiBuilder = OllamaApi.builder();

        if (config.getApiEndpoint() != null && !config.getApiEndpoint().isBlank()) {
            apiBuilder.baseUrl(config.getApiEndpoint());
        }

        return OllamaChatModel.builder()
                .ollamaApi(apiBuilder.build())
                .defaultOptions(
                        OllamaOptions.builder()
                                .model(config.getModelName())
                                .temperature(assistant.getTemperature().doubleValue())
                                .frequencyPenalty(assistant.getFrequencyPenalty().doubleValue())
                                .presencePenalty(assistant.getPresencePenalty().doubleValue())
                                .topP(assistant.getTopP().doubleValue())
                                .build())
                .build();
    }

    /**
     * 创建DeepSeek ChatModel
     */
    private ChatModel createDeepSeekChatModel(AiModelConfig config, ChatAssistantDTO assistant) {
        DeepSeekApi.Builder apiBuilder = DeepSeekApi.builder()
                .apiKey(config.getApiKey());

        if (config.getApiEndpoint() != null && !config.getApiEndpoint().isBlank()) {
            apiBuilder.baseUrl(config.getApiEndpoint());
        }

        return DeepSeekChatModel.builder()
                .deepSeekApi(apiBuilder.build())
                .defaultOptions(
                        DeepSeekChatOptions.builder()
                                .model(config.getModelName())
                                .temperature(assistant.getTemperature().doubleValue())
                                .maxTokens(assistant.getMaxTokens())
                                .frequencyPenalty(assistant.getFrequencyPenalty().doubleValue())
                                .topP(assistant.getTopP().doubleValue())
                                .build())
                .build();
    }

    private ChatModel createZhiPuApiChatModel(AiModelConfig config, ChatAssistantDTO assistant) {
        ZhiPuAiApi zhiPuAiApi = new ZhiPuAiApi(config.getApiKey());
        if (config.getApiEndpoint() != null && !config.getApiEndpoint().isBlank()) {
            zhiPuAiApi = new ZhiPuAiApi(config.getApiEndpoint(), config.getApiKey());
        }

        ZhiPuAiChatOptions options = new ZhiPuAiChatOptions();
        options.setModel(config.getModelName());
        options.setTemperature(assistant.getTemperature().doubleValue());
        options.setMaxTokens(assistant.getMaxTokens());
        options.setTopP(assistant.getTopP().doubleValue());
        return new ZhiPuAiChatModel(zhiPuAiApi, options);
    }

}
