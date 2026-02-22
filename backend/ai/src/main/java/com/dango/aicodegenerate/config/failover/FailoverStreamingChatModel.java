package com.dango.aicodegenerate.config.failover;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * StreamingChatModel 故障转移代理
 * 主模型流式调用失败时，自动切换到备选模型重试
 */
@Slf4j
public class FailoverStreamingChatModel implements StreamingChatModel {

    private final List<StreamingChatModel> models;
    private final List<String> modelNames;

    public FailoverStreamingChatModel(List<StreamingChatModel> models, List<String> modelNames) {
        if (models == null || models.isEmpty()) {
            throw new IllegalArgumentException("至少需要一个 StreamingChatModel");
        }
        this.models = models;
        this.modelNames = modelNames;
    }

    @Override
    public void doChat(ChatRequest request, StreamingChatResponseHandler handler) {
        doChat(request, handler, 0);
    }

    private void doChat(ChatRequest request, StreamingChatResponseHandler handler, int index) {
        if (index >= models.size()) {
            handler.onError(new RuntimeException("所有模型均调用失败"));
            return;
        }

        StreamingChatModel model = models.get(index);
        String name = index < modelNames.size() ? modelNames.get(index) : model.getClass().getSimpleName();

        if (index == 0) {
            log.info("AI 调用 - 模型: {}", name);
        } else {
            log.info("Failover: 切换到备选模型: {}", name);
        }

        try {
            model.chat(request, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    handler.onPartialResponse(partialResponse);
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    handler.onCompleteResponse(completeResponse);
                }

                @Override
                public void onError(Throwable error) {
                    log.warn("模型 [{}] 流式调用失败: {}", name, error.getMessage());
                    // 切换到下一个模型
                    doChat(request, handler, index + 1);
                }
            });
        } catch (Exception e) {
            log.warn("模型 [{}] 流式调用异常: {}", name, e.getMessage());
            doChat(request, handler, index + 1);
        }
    }
}