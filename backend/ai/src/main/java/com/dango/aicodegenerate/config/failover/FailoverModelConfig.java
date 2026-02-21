package com.dango.aicodegenerate.config.failover;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;

/**
 * Failover 模型组装配置
 * 将多个 provider 的模型按优先级组装为 failover 包装器，注册为 @Primary Bean
 * 配置了 api-key 的 provider 会自动加载，主模型失败时自动切换备选
 */
@Configuration
@Slf4j
public class FailoverModelConfig {

    @Autowired(required = false)
    @Qualifier("anthropicChatModel")
    private ChatModel anthropicChatModel;

    @Autowired(required = false)
    @Qualifier("openAiChatModel")
    private ChatModel openAiChatModel;

    @Autowired(required = false)
    @Qualifier("anthropicReasoningStreamingChatModel")
    private StreamingChatModel anthropicReasoningStreamingChatModel;

    @Autowired(required = false)
    @Qualifier("openAiReasoningStreamingChatModel")
    private StreamingChatModel openAiReasoningStreamingChatModel;

    @Autowired(required = false)
    @Qualifier("anthropicStreamingChatModel")
    private StreamingChatModel anthropicStreamingChatModel;

    @Autowired(required = false)
    @Qualifier("openAiStreamingChatModel")
    private StreamingChatModel openAiStreamingChatModel;

    @Bean("chatModel")
    @Primary
    public ChatModel failoverChatModel() {
        List<ChatModel> models = new ArrayList<>();
        addIfNotNull(models, anthropicChatModel, "Anthropic");
        addIfNotNull(models, openAiChatModel, "OpenAI");
        log.info("ChatModel Failover 链: {} 个模型", models.size());
        if (models.size() == 1) {
            return models.getFirst();
        }
        return new FailoverChatModel(models);
    }

    @Bean("reasoningStreamingChatModel")
    public StreamingChatModel failoverReasoningStreamingChatModel() {
        List<StreamingChatModel> models = new ArrayList<>();
        addIfNotNull(models, anthropicReasoningStreamingChatModel, "Anthropic");
        addIfNotNull(models, openAiReasoningStreamingChatModel, "OpenAI");
        log.info("ReasoningStreamingChatModel Failover 链: {} 个模型", models.size());
        if (models.size() == 1) {
            return models.getFirst();
        }
        return new FailoverStreamingChatModel(models);
    }

    @Bean("streamingChatModel")
    public StreamingChatModel failoverStreamingChatModel() {
        List<StreamingChatModel> models = new ArrayList<>();
        addIfNotNull(models, anthropicStreamingChatModel, "Anthropic");
        addIfNotNull(models, openAiStreamingChatModel, "OpenAI");
        log.info("StreamingChatModel Failover 链: {} 个模型", models.size());
        if (models.size() == 1) {
            return models.getFirst();
        }
        return new FailoverStreamingChatModel(models);
    }

    private <T> void addIfNotNull(List<T> list, T item, String name) {
        if (item != null) {
            list.add(item);
            log.info("  - {} 模型已加入 Failover 链", name);
        }
    }
}
