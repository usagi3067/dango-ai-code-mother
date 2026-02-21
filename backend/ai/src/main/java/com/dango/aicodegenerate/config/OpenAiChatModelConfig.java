package com.dango.aicodegenerate.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConditionalOnProperty(name = "ai.provider", havingValue = "openai", matchIfMissing = true)
@ConfigurationProperties(prefix = "langchain4j.open-ai.chat-model")
@Data
public class OpenAiChatModelConfig {

    private String baseUrl;
    private String apiKey;
    private String modelName;
    private Integer maxTokens;
    private Boolean logRequests;
    private Boolean logResponses;

    @Autowired(required = false)
    private List<ChatModelListener> chatModelListeners;

    @Bean
    public ChatModel openAiChatModel() {
        var builder = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName != null ? modelName : "deepseek-chat")
                .maxTokens(maxTokens != null ? maxTokens : 8192)
                .logRequests(logRequests != null ? logRequests : true)
                .logResponses(logResponses != null ? logResponses : true);
        if (chatModelListeners != null && !chatModelListeners.isEmpty()) {
            builder.listeners(chatModelListeners);
        }
        return builder.build();
    }
}
