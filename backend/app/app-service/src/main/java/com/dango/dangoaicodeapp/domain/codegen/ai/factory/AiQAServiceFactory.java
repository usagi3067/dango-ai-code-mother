package com.dango.dangoaicodeapp.domain.codegen.ai.factory;

import com.dango.dangoaicodeapp.domain.codegen.ai.service.QAService;
import com.dango.dangoaicodeapp.application.service.ChatHistoryService;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 问答 AI 服务工厂
 */
@Component
@Slf4j
public class AiQAServiceFactory {

    @Resource
    private StreamingChatModel odinaryStreamingChatModel;

    @Resource
    private ChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    public QAService createService(long appId) {
        log.info("为 appId: {} 创建问答服务实例", appId);

        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id("chat_" + appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(50)
                .build();

        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);

        return AiServices.builder(QAService.class)
                .streamingChatModel(odinaryStreamingChatModel)
                .chatMemory(chatMemory)
                .chatMemoryProvider(memoryId -> chatMemory)
                .build();
    }
}
