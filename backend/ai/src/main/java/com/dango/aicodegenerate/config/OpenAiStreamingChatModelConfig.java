package com.dango.aicodegenerate.config;

import dev.langchain4j.http.client.spring.restclient.SpringRestClient;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;

import java.util.List;

/**
 * OpenAI 流式聊天模型配置
 * 覆盖 LangChain4j 自动配置的 Bean，添加监控监听器
 *
 * @author dango
 */
@Configuration
@ConditionalOnProperty(prefix = "langchain4j.open-ai.chat-model", name = "api-key")
@ConfigurationProperties(prefix = "langchain4j.open-ai.chat-model")
@Data
public class OpenAiStreamingChatModelConfig {

    private String baseUrl;

    private String apiKey;

    private String modelName;

    private Integer maxTokens;

    private Boolean logRequests;

    private Boolean logResponses;

    @Autowired(required = false)
    private List<ChatModelListener> chatModelListeners;

    @Autowired(required = false)
    @Qualifier("streamingContextPropagatingExecutor")
    private AsyncTaskExecutor streamingExecutor;

    /**
     * 流式聊天模型
     */
    @Bean("openAiStreamingChatModel")
    public StreamingChatModel openAiStreamingChatModel() {
        var builder = OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName != null ? modelName : "deepseek-chat")
                .maxTokens(maxTokens != null ? maxTokens : 8192)
                .logRequests(logRequests != null ? logRequests : true)
                .logResponses(logResponses != null ? logResponses : true);
        if (streamingExecutor != null) {
            builder.httpClientBuilder(SpringRestClient.builder().streamingRequestExecutor(streamingExecutor));
        }
        // 注册监听器
        if (chatModelListeners != null && !chatModelListeners.isEmpty()) {
            builder.listeners(chatModelListeners);
        }
        return builder.build();
    }
}
