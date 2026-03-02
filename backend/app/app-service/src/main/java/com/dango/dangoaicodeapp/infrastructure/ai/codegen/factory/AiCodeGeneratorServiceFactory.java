package com.dango.dangoaicodeapp.infrastructure.ai.codegen.factory;

import com.dango.aicodegenerate.guardrail.PromptSafetyInputGuardrail;
import com.dango.dangoaicodeapp.infrastructure.ai.codegen.service.CodeGeneratorService;
import com.dango.dangoaicodeapp.infrastructure.ai.codegen.service.LeetCodeCodeGeneratorService;
import com.dango.dangoaicodeapp.infrastructure.ai.codegen.service.InterviewCodeGeneratorService;
import com.dango.dangoaicodeapp.infrastructure.ai.codegen.service.InterviewSourceCodeGeneratorService;
import com.dango.dangoaicodeapp.infrastructure.ai.codegen.service.VueCodeGeneratorService;

import com.dango.dangoaicodeapp.domain.codegen.tools.ToolManager;
import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;
import com.dango.dangoaicodeapp.application.service.ChatHistoryService;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import com.dango.aicodegenerate.model.AiModelProvider;

import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Slf4j
public class AiCodeGeneratorServiceFactory {

    @Resource
    private AiModelProvider aiModelProvider;

    @Resource
    private ChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private ToolManager toolManager;

    /**
     * AI 服务实例缓存
     * 缓存策略：
     * - 最大缓存 1000 个实例
     * - 写入后 30 分钟过期
     * - 访问后 10 分钟过期
     */
    private final Cache<String, CodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，appId: {}, 原因: {}", key, cause);
            })
            .build();

    /**
     * 根据 appId 和代码生成类型获取服务（带缓存）
     */
    public CodeGeneratorService getService(long appId, CodeGenTypeEnum codeGenType) {
        String cacheKey = buildCacheKey(appId, codeGenType);
        return serviceCache.get(cacheKey, key -> createService(appId, codeGenType));
    }

    /**
     * 创建新的 AI 服务实例
     */
    private CodeGeneratorService createService(long appId, CodeGenTypeEnum codeGenType) {
        log.info("为 appId: {} 创建新的 AI 服务实例，类型: {}", appId, codeGenType.getValue());
        // 根据 appId 构建独立的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id("chat_" + appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(50)
                .build();
        // 从数据库加载历史对话到记忆中
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);

        Class<? extends CodeGeneratorService> serviceClass = switch (codeGenType) {
            case LEETCODE_PROJECT -> LeetCodeCodeGeneratorService.class;
            case INTERVIEW_PROJECT -> InterviewCodeGeneratorService.class;
            case INTERVIEW_SOURCE_CODE_PROJECT -> InterviewSourceCodeGeneratorService.class;
            default -> VueCodeGeneratorService.class;
        };

        return AiServices.builder(serviceClass)
                .streamingChatModel(aiModelProvider.getStreamingChatModel("code-generator"))
                .chatMemory(chatMemory)
                .chatMemoryProvider(memoryId -> chatMemory)
                .tools(toolManager.getAllTools())
                .inputGuardrails(new PromptSafetyInputGuardrail())
                .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                        toolExecutionRequest, "Error: there is no tool called " + toolExecutionRequest.name()
                ))
                .build();
    }

    /**
     * 构建缓存键
     */
    private String buildCacheKey(long appId, CodeGenTypeEnum codeGenType) {
        return appId + "_" + codeGenType.getValue();
    }

    @EventListener(EnvironmentChangeEvent.class)
    public void onConfigChange(EnvironmentChangeEvent event) {
        if (event.getKeys().stream().anyMatch(k -> k.startsWith("ai."))) {
            log.info("AI 配置变更，清空代码生成服务缓存");
            serviceCache.invalidateAll();
        }
    }

}
