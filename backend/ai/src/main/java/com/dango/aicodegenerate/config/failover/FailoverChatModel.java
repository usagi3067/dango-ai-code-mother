package com.dango.aicodegenerate.config.failover;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * ChatModel 故障转移代理
 * 按顺序尝试多个模型，主模型失败时自动切换到备选模型
 */
@Slf4j
public class FailoverChatModel implements ChatModel {

    private final List<ChatModel> models;
    private final List<String> modelNames;

    public FailoverChatModel(List<ChatModel> models, List<String> modelNames) {
        if (models == null || models.isEmpty()) {
            throw new IllegalArgumentException("至少需要一个 ChatModel");
        }
        this.models = models;
        this.modelNames = modelNames;
    }

    @Override
    public ChatResponse doChat(ChatRequest request) {
        Exception lastException = null;
        for (int i = 0; i < models.size(); i++) {
            ChatModel model = models.get(i);
            String name = i < modelNames.size() ? modelNames.get(i) : model.getClass().getSimpleName();
            try {
                log.info("AI 调用 - 模型: {}", name);
                ChatResponse response = model.chat(request);
                if (i > 0) {
                    log.info("Failover 成功，使用备选模型: {}", name);
                }
                return response;
            } catch (Exception e) {
                lastException = e;
                log.warn("模型 [{}] 调用失败: {}", name, e.getMessage());
            }
        }
        throw new RuntimeException("所有模型均调用失败", lastException);
    }
}
