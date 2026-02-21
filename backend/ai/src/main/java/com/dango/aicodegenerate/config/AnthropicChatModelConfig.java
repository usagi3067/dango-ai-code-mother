package com.dango.aicodegenerate.config;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
@ConditionalOnProperty(prefix = "ai.anthropic.chat-model", name = "api-key")
@ConfigurationProperties(prefix = "ai.anthropic.chat-model")
@Data
public class AnthropicChatModelConfig {

    private String baseUrl;
    private String apiKey;
    private String modelName;
    private Integer maxTokens;
    private Boolean logRequests;
    private Boolean logResponses;
    private Duration timeout;

    @Autowired(required = false)
    private List<ChatModelListener> chatModelListeners;

    @Bean("anthropicChatModel")
    public ChatModel anthropicChatModel() {
        var builder = AnthropicChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .maxTokens(maxTokens != null ? maxTokens : 8192)
                .timeout(timeout != null ? timeout : Duration.ofSeconds(300))
                .logRequests(logRequests != null ? logRequests : true)
                .logResponses(logResponses != null ? logResponses : true);
        if (chatModelListeners != null && !chatModelListeners.isEmpty()) {
            builder.listeners(chatModelListeners);
        }
        return builder.build();
    }
}
