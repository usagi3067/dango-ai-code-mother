package com.dango.dangoaicodemother.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 应用名称生成服务工厂类
 * 创建专用的 ChatModel（不使用 JSON 格式），用于名称生成服务
 */
@Configuration
public class AiAppNameGeneratorServiceFactory {

    @Value("${langchain4j.open-ai.chat-model.base-url}")
    private String baseUrl;

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String apiKey;

    @Value("${langchain4j.open-ai.chat-model.model-name}")
    private String modelName;

    /**
     * 创建专用于名称生成的 ChatModel
     * 不使用 JSON 响应格式，返回纯文本
     */
    @Bean("appNameChatModel")
    public ChatModel appNameChatModel() {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .maxTokens(100)  // 名称生成只需要很少的 token
                .build();
    }

    /**
     * 创建 AiAppNameGeneratorService 实例
     * 使用专用的 ChatModel（纯文本格式）进行 AI 调用
     */
    @Bean
    public AiAppNameGeneratorService aiAppNameGeneratorService() {
        return AiServices.builder(AiAppNameGeneratorService.class)
                .chatModel(appNameChatModel())
                .build();
    }
}
