package com.dango.aicodegenerate.config;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConditionalOnProperty(prefix = "langchain4j.open-ai.streaming-chat-model", name = "api-key")
@ConfigurationProperties(prefix = "langchain4j.open-ai.streaming-chat-model")
@Data
public class ReasoningStreamingChatModelConfig {

    private String baseUrl;

    private String apiKey;

    private String modelName;

    private Integer maxTokens;

    private Boolean logRequests;

    private Boolean logResponses;

    @Autowired(required = false)
    private List<ChatModelListener> chatModelListeners;

    /**
     * 推理流式模型（用于 Vue 项目生成，带工具调用）
     */
    @Bean("openAiReasoningStreamingChatModel")
    public StreamingChatModel openAiReasoningStreamingChatModel() {
        var builder = OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName != null ? modelName : "deepseek-chat")
                .maxTokens(maxTokens != null ? maxTokens : 8192)
                .logRequests(logRequests != null ? logRequests : true)
                .logResponses(logResponses != null ? logResponses : true);
        // 注册监听器
        if (chatModelListeners != null && !chatModelListeners.isEmpty()) {
            builder.listeners(chatModelListeners);
        }
        return builder.build();
    }
}
