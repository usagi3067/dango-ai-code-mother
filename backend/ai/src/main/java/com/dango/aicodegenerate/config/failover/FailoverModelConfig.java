package com.dango.aicodegenerate.config.failover;

import com.dango.aicodegenerate.config.AnthropicChatModelConfig;
import com.dango.aicodegenerate.config.AnthropicOrdinaryStreamingChatModelConfig;
import com.dango.aicodegenerate.config.AnthropicReasoningStreamingChatModelConfig;
import com.dango.aicodegenerate.config.OpenAiChatModelConfig;
import com.dango.aicodegenerate.config.OpenAiStreamingChatModelConfig;
import com.dango.aicodegenerate.config.ReasoningStreamingChatModelConfig;
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

    // 注入配置类以获取模型名称
    @Autowired(required = false)
    private AnthropicChatModelConfig anthropicChatModelConfig;
    @Autowired(required = false)
    private OpenAiChatModelConfig openAiChatModelConfig;
    @Autowired(required = false)
    private AnthropicReasoningStreamingChatModelConfig anthropicReasoningConfig;
    @Autowired(required = false)
    private ReasoningStreamingChatModelConfig openAiReasoningConfig;
    @Autowired(required = false)
    private AnthropicOrdinaryStreamingChatModelConfig anthropicOrdinaryConfig;
    @Autowired(required = false)
    private OpenAiStreamingChatModelConfig openAiStreamingConfig;

    @Bean("chatModel")
    @Primary
    public ChatModel failoverChatModel() {
        List<ChatModel> models = new ArrayList<>();
        List<String> names = new ArrayList<>();
        addIfNotNull(models, names, anthropicChatModel,
                anthropicChatModelConfig != null ? anthropicChatModelConfig.getModelName() : null, "Anthropic");
        addIfNotNull(models, names, openAiChatModel,
                openAiChatModelConfig != null ? openAiChatModelConfig.getModelName() : null, "OpenAI");
        log.info("ChatModel Failover 链: {}", names);
        if (models.size() == 1) {
            return new FailoverChatModel(models, names);
        }
        return new FailoverChatModel(models, names);
    }

    @Bean("reasoningStreamingChatModel")
    public StreamingChatModel failoverReasoningStreamingChatModel() {
        List<StreamingChatModel> models = new ArrayList<>();
        List<String> names = new ArrayList<>();
        addIfNotNull(models, names, anthropicReasoningStreamingChatModel,
                anthropicReasoningConfig != null ? anthropicReasoningConfig.getModelName() : null, "Anthropic");
        addIfNotNull(models, names, openAiReasoningStreamingChatModel,
                openAiReasoningConfig != null ? openAiReasoningConfig.getModelName() : null, "OpenAI");
        log.info("ReasoningStreamingChatModel Failover 链: {}", names);
        if (models.size() == 1) {
            return new FailoverStreamingChatModel(models, names);
        }
        return new FailoverStreamingChatModel(models, names);
    }

    @Bean("streamingChatModel")
    public StreamingChatModel failoverStreamingChatModel() {
        List<StreamingChatModel> models = new ArrayList<>();
        List<String> names = new ArrayList<>();
        addIfNotNull(models, names, anthropicStreamingChatModel,
                anthropicOrdinaryConfig != null ? anthropicOrdinaryConfig.getModelName() : null, "Anthropic");
        addIfNotNull(models, names, openAiStreamingChatModel,
                openAiStreamingConfig != null ? openAiStreamingConfig.getModelName() : null, "OpenAI");
        log.info("StreamingChatModel Failover 链: {}", names);
        if (models.size() == 1) {
            return new FailoverStreamingChatModel(models, names);
        }
        return new FailoverStreamingChatModel(models, names);
    }

    private <T> void addIfNotNull(List<T> list, List<String> names, T item, String modelName, String provider) {
        if (item != null) {
            list.add(item);
            String displayName = modelName != null ? modelName : provider;
            names.add(displayName);
        }
    }
}
