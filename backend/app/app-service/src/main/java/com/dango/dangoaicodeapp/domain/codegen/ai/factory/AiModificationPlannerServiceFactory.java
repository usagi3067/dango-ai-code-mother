package com.dango.dangoaicodeapp.domain.codegen.ai.factory;

import com.dango.aicodegenerate.guardrail.PromptSafetyInputGuardrail;
import com.dango.dangoaicodeapp.domain.codegen.ai.service.AiModificationPlannerService;
import com.dango.dangoaicodeapp.domain.codegen.tools.FileDirReadTool;
import com.dango.dangoaicodeapp.domain.codegen.tools.FileReadTool;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * AI 修改规划服务工厂
 * 用于创建修改规划专用的 AI 服务实例
 * 只注册只读工具（文件读取），供 AI 分析代码和规划修改
 *
 * @author dango
 */
@Component
@Slf4j
public class AiModificationPlannerServiceFactory {

    @Resource
    private ChatModel ordinaryChatModel;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private FileDirReadTool fileDirReadTool;

    @Resource
    private FileReadTool fileReadTool;

    /**
     * 创建修改规划服务实例
     * 注意：不使用缓存，每次创建新实例，因为规划是一次性的
     *
     * @param appId 应用 ID
     * @return AI 修改规划服务实例
     */
    public AiModificationPlannerService createPlannerService(long appId) {
        log.info("为 appId: {} 创建修改规划服务实例", appId);

        // 为修改规划创建独立的对话记忆
        // 注意：工具调用会消耗大量消息（每次调用 = AI请求 + 工具结果 = 2条消息）
        // 设置足够大的窗口以支持多次工具调用
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(50)  // 支持约 20+ 次工具调用
                .build();

        return AiServices.builder(AiModificationPlannerService.class)
                .chatModel(ordinaryChatModel)
                .chatMemory(chatMemory)
                .chatMemoryProvider(memoryId -> chatMemory)
                .tools(
                        // 只提供只读工具
                        fileDirReadTool,
                        fileReadTool
                )
                .inputGuardrails(new PromptSafetyInputGuardrail())
                .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                        toolExecutionRequest, "Error: there is no tool called " + toolExecutionRequest.name()
                ))
                .build();
    }
}
