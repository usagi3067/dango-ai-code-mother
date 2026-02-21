package com.dango.aicodegenerate.config;

import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConditionalOnProperty(name = "ai.provider", havingValue = "anthropic")
@ConfigurationProperties(prefix = "ai.anthropic.reasoning-streaming-chat-model")
@Data
public class AnthropicReasoningStreamingChatModelConfig {

    private String baseUrl;
    private String apiKey;
    private String modelName;
    private Integer maxTokens;
    private Boolean logRequests;
    private Boolean logResponses;

    @Autowired(required = false)
    private List<ChatModelListener> chatModelListeners;

    @Bean
    public StreamingChatModel reasoningStreamingChatModel() {
        var builder = AnthropicStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .maxTokens(maxTokens != null ? maxTokens : 65536)
                .logRequests(logRequests != null ? logRequests : true)
                .logResponses(logResponses != null ? logResponses : true);
        if (chatModelListeners != null && !chatModelListeners.isEmpty()) {
            builder.listeners(chatModelListeners);
        }
        return builder.build();
    }
}
