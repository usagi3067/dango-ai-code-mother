package com.dango.dangoaicodeapp.infrastructure.ai.codegen.factory;

import com.dango.dangoaicodeapp.domain.codegen.tools.FileDirReadTool;
import com.dango.dangoaicodeapp.domain.codegen.tools.FileReadTool;
import com.dango.dangoaicodeapp.infrastructure.ai.codegen.service.QAService;
import com.dango.dangoaicodeapp.application.service.ChatHistoryService;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import com.dango.aicodegenerate.model.AiModelProvider;

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
    private AiModelProvider aiModelProvider;

    @Resource
    private ChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private FileDirReadTool fileDirReadTool;

    @Resource
    private FileReadTool fileReadTool;

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
                .streamingChatModel(aiModelProvider.getStreamingChatModel("qa"))
                .chatMemory(chatMemory)
                .chatMemoryProvider(memoryId -> chatMemory)
                .tools(
                        fileDirReadTool,
                        fileReadTool
                )
                .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                        toolExecutionRequest, "Error: there is no tool called " + toolExecutionRequest.name()
                ))
                .build();
    }
}
