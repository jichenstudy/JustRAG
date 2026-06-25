package com.shujichen.rag.factory;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingOptions;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import com.shujichen.rag.entity.AiModelConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.azure.openai.AzureOpenAiEmbeddingModel;
import org.springframework.ai.azure.openai.AzureOpenAiEmbeddingOptions;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.zhipuai.ZhiPuAiEmbeddingModel;
import org.springframework.ai.zhipuai.ZhiPuAiEmbeddingOptions;
import org.springframework.ai.zhipuai.api.ZhiPuAiApi;
import org.springframework.stereotype.Component;

/**
 * EmbeddingModel工厂类 - 根据模型配置动态创建EmbeddingModel
 * <p>
 * 支持的提供商:
 * - DashScope (阿里云通义千问)
 * - OpenAI
 * - Ollama (本地模型)
 */
@Slf4j
@Component
public class EmbeddingModelFactory {

    /**
     * 根据模型配置创建EmbeddingModel
     */
    public EmbeddingModel createEmbeddingModel(AiModelConfig modelConfig) {
        log.info("创建EmbeddingModel, 提供商: {}, 模型: {}", modelConfig.getProvider(), modelConfig.getModelName());

        return switch (modelConfig.getProvider()) {
            case "DASHSCOPE" -> createDashScopeEmbeddingModel(modelConfig);
            case "OPENAI" -> createOpenAiEmbeddingModel(modelConfig);
            case "OLLAMA" -> createOllamaEmbeddingModel(modelConfig);
            case "ZHIPU" -> createZhipuEmbeddingModel(modelConfig);
            case "AZURE OPENAI" -> createAzureOpenAiEmbeddingModel(modelConfig);
            default -> throw new IllegalArgumentException("不支持的向量模型提供商: " + modelConfig.getProvider());
        };
    }

    private EmbeddingModel createAzureOpenAiEmbeddingModel(AiModelConfig config) {
        OpenAIClientBuilder openAIClientBuilder = new OpenAIClientBuilder()
                .credential(new AzureKeyCredential(config.getApiKey()))
                .endpoint(config.getApiEndpoint());

        return new AzureOpenAiEmbeddingModel(openAIClientBuilder.buildClient(), MetadataMode.EMBED,
                AzureOpenAiEmbeddingOptions.builder()
                        .deploymentName(config.getModelName())
                        .build());
    }

    private EmbeddingModel createZhipuEmbeddingModel(AiModelConfig config) {
        ZhiPuAiApi zhiPuAiApi = new ZhiPuAiApi(config.getApiKey());

        return new ZhiPuAiEmbeddingModel(zhiPuAiApi, MetadataMode.EMBED, ZhiPuAiEmbeddingOptions.builder()
                .model(config.getModelName())
                .build());
    }

    /**
     * 创建DashScope EmbeddingModel
     */
    private EmbeddingModel createDashScopeEmbeddingModel(AiModelConfig config) {
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(config.getApiKey())
                .build();
        DashScopeEmbeddingModel dashScopeEmbeddingModel = new DashScopeEmbeddingModel(dashScopeApi, MetadataMode.EMBED,
                DashScopeEmbeddingOptions.builder()
                        .withModel(config.getModelName())
                        .build());
        int dimensions = dashScopeEmbeddingModel.dimensions();
        return dashScopeEmbeddingModel;
    }

    /**
     * 创建OpenAI EmbeddingModel
     */
    private EmbeddingModel createOpenAiEmbeddingModel(AiModelConfig config) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(config.getApiKey())
                .baseUrl(config.getApiEndpoint() != null ? config.getApiEndpoint() : "https://api.openai.com")
                .build();

        return new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED,
                OpenAiEmbeddingOptions.builder()
                        .model(config.getModelName())
                        .build());
    }

    /**
     * 创建Ollama EmbeddingModel (本地模型)
     */
    private EmbeddingModel createOllamaEmbeddingModel(AiModelConfig config) {
        OllamaApi ollamaApi = OllamaApi.builder()
                .baseUrl(config.getApiEndpoint() != null ? config.getApiEndpoint() : "http://localhost:11434")
                .build();

        return OllamaEmbeddingModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(
                        OllamaOptions.builder()
                                .model(config.getModelName()) // nomic-embed-text, mxbai-embed-large 等
                                .build()
                )
                .build();
    }
}
