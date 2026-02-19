package com.dango.dangoaicodeapp.domain.codegen.ai.factory;

import com.dango.aicodegenerate.guardrail.PromptSafetyInputGuardrail;
import com.dango.dangoaicodeapp.domain.codegen.ai.service.CodeFixerService;
import com.dango.dangoaicodeapp.domain.codegen.ai.service.LeetCodeCodeFixerService;
import com.dango.dangoaicodeapp.domain.codegen.ai.service.VueCodeFixerService;
import com.dango.dangoaicodeapp.domain.codegen.tools.*;
import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;
import com.dango.dangoaicodeapp.application.service.ChatHistoryService;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * AI 代码修复服务工厂
 * 用于创建修复模式专用的 AI 服务实例
 * 根据质检错误信息进行针对性修复
 */
@Component
@Slf4j
public class AiCodeFixerServiceFactory {

    @Resource
    private StreamingChatModel reasoningStreamingChatModel;

    @Resource
    private ChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    // 文件操作工具（修复时可能需要读取和修改文件）
    @Resource
    private FileDirReadTool fileDirReadTool;

    @Resource
    private FileReadTool fileReadTool;

    @Resource
    private FileModifyTool fileModifyTool;

    @Resource
    private FileWriteTool fileWriteTool;

    @Resource
    private FileDeleteTool fileDeleteTool;

    /**
     * AI 修复服务实例缓存
     * 缓存策略：
     * - 最大缓存 500 个实例
     * - 写入后 30 分钟过期
     * - 访问后 10 分钟过期
     */
    private final Cache<String, CodeFixerService> serviceCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 修复服务实例被移除，key: {}, 原因: {}", key, cause);
            })
            .build();

    /**
     * 根据 appId 和代码生成类型获取修复服务（带缓存）
     *
     * @param appId       应用 ID
     * @param codeGenType 代码生成类型
     * @return AI 代码修复服务实例
     */
    public CodeFixerService getFixerService(long appId, CodeGenTypeEnum codeGenType) {
        String cacheKey = buildCacheKey(appId, codeGenType);
        return serviceCache.get(cacheKey, key -> createFixerService(appId, codeGenType));
    }

    /**
     * 创建新的 AI 修复服务实例
     *
     * @param appId       应用 ID
     * @param codeGenType 代码生成类型
     * @return AI 代码修复服务实例
     */
    private CodeFixerService createFixerService(long appId, CodeGenTypeEnum codeGenType) {
        log.info("为 appId: {} 创建新的 AI 修复服务实例，类型: {}", appId, codeGenType.getValue());

        // 根据 appId 构建独立的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(50)
                .build();

        // 从数据库加载历史对话到记忆中
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);

        Class<? extends CodeFixerService> serviceClass = switch (codeGenType) {
            case LEETCODE_PROJECT -> LeetCodeCodeFixerService.class;
            default -> VueCodeFixerService.class;
        };

        return AiServices.builder(serviceClass)
                .streamingChatModel(reasoningStreamingChatModel)
                .chatMemory(chatMemory)
                .chatMemoryProvider(memoryId -> chatMemory)
                .tools(
                        // 文件操作工具（修复时需要读取和修改文件）
                        fileDirReadTool,
                        fileReadTool,
                        fileModifyTool,
                        fileWriteTool,
                        fileDeleteTool
                )
                .inputGuardrails(new PromptSafetyInputGuardrail())
                .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                        toolExecutionRequest, "Error: there is no tool called " + toolExecutionRequest.name()
                ))
                .build();
    }

    /**
     * 构建缓存键
     *
     * @param appId       应用 ID
     * @param codeGenType 代码生成类型
     * @return 缓存键
     */
    private String buildCacheKey(long appId, CodeGenTypeEnum codeGenType) {
        return "fixer_" + appId + "_" + codeGenType.getValue();
    }

    /**
     * 清除指定应用的缓存
     *
     * @param appId 应用 ID
     */
    public void invalidateCache(long appId) {
        for (CodeGenTypeEnum type : CodeGenTypeEnum.values()) {
            String cacheKey = buildCacheKey(appId, type);
            serviceCache.invalidate(cacheKey);
        }
        log.info("已清除 appId: {} 的修复服务缓存", appId);
    }
}
