package com.dango.aicodegenerate.config;

import com.dango.aicodegenerate.model.AiModelProvider;
import com.dango.aicodegenerate.model.AiServiceType;
import dev.langchain4j.http.client.spring.restclient.SpringRestClient;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class HigressAiModelProvider implements AiModelProvider {

    private final AiGatewayProperties properties;
    private final List<ChatModelListener> listeners;
    private final AsyncTaskExecutor streamingExecutor;

    private final ConcurrentHashMap<AiServiceType, ChatModel> chatModelCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<AiServiceType, StreamingChatModel> streamingModelCache = new ConcurrentHashMap<>();

    public HigressAiModelProvider(
            AiGatewayProperties properties,
            @Autowired(required = false) List<ChatModelListener> listeners,
            @Autowired(required = false) @Qualifier("streamingContextPropagatingExecutor") AsyncTaskExecutor streamingExecutor
    ) {
        this.properties = properties;
        this.listeners = listeners;
        this.streamingExecutor = streamingExecutor;
    }

    @Override
    public ChatModel getChatModel(AiServiceType serviceType) {
        return chatModelCache.computeIfAbsent(serviceType, this::buildChatModel);
    }

    @Override
    public StreamingChatModel getStreamingChatModel(AiServiceType serviceType) {
        return streamingModelCache.computeIfAbsent(serviceType, this::buildStreamingChatModel);
    }

    private ChatModel buildChatModel(AiServiceType serviceType) {
        var gw = properties.getGateway();
        var svc = getServiceConfig(serviceType);
        String modelName = resolveModelName(svc);
        int maxTokens = svc.getMaxTokens() != null ? svc.getMaxTokens() : gw.getDefaultMaxTokens();
        Duration timeout = svc.getTimeout() != null ? svc.getTimeout() : gw.getDefaultTimeout();

        log.info("构建 ChatModel: service={}, model={}, maxTokens={}, timeout={}s",
                serviceType.getConfigKey(), modelName, maxTokens, timeout.toSeconds());

        var builder = OpenAiChatModel.builder()
                .baseUrl(gw.getBaseUrl())
                .apiKey(gw.getApiKey())
                .modelName(modelName)
                .maxTokens(maxTokens)
                .timeout(timeout)
                .logRequests(gw.getLogRequests())
                .logResponses(gw.getLogResponses());
        if (listeners != null && !listeners.isEmpty()) {
            builder.listeners(listeners);
        }
        return builder.build();
    }

    private StreamingChatModel buildStreamingChatModel(AiServiceType serviceType) {
        var gw = properties.getGateway();
        var svc = getServiceConfig(serviceType);
        String modelName = resolveModelName(svc);
        int maxTokens = svc.getMaxTokens() != null ? svc.getMaxTokens() : gw.getDefaultMaxTokens();
        Duration timeout = svc.getTimeout() != null ? svc.getTimeout() : gw.getDefaultTimeout();

        log.info("构建 StreamingChatModel: service={}, model={}, maxTokens={}, timeout={}s",
                serviceType.getConfigKey(), modelName, maxTokens, timeout.toSeconds());

        var builder = OpenAiStreamingChatModel.builder()
                .baseUrl(gw.getBaseUrl())
                .apiKey(gw.getApiKey())
                .modelName(modelName)
                .maxTokens(maxTokens)
                .timeout(timeout)
                .logRequests(gw.getLogRequests())
                .logResponses(gw.getLogResponses());
        if (streamingExecutor != null) {
            builder.httpClientBuilder(SpringRestClient.builder().streamingRequestExecutor(streamingExecutor));
        }
        if (listeners != null && !listeners.isEmpty()) {
            builder.listeners(listeners);
        }
        return builder.build();
    }

    private AiGatewayProperties.ServiceConfig getServiceConfig(AiServiceType serviceType) {
        return properties.getServices()
                .getOrDefault(serviceType.getConfigKey(), new AiGatewayProperties.ServiceConfig());
    }

    private String resolveModelName(AiGatewayProperties.ServiceConfig svc) {
        return svc.getModel() != null ? svc.getModel() : properties.getDefaultModel();
    }

    @EventListener(EnvironmentChangeEvent.class)
    public void onConfigChange(EnvironmentChangeEvent event) {
        Set<String> changedKeys = event.getKeys();
        boolean aiConfigChanged = changedKeys.stream()
                .anyMatch(key -> key.startsWith("ai."));
        if (aiConfigChanged) {
            log.info("检测到 AI 配置变更，清空模型缓存。变更的 key: {}", changedKeys);
            chatModelCache.clear();
            streamingModelCache.clear();
        }
    }
}
