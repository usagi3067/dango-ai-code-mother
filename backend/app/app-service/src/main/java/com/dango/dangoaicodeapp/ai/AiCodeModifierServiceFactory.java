package com.dango.dangoaicodeapp.ai;

import com.dango.aicodegenerate.guardrail.PromptSafetyInputGuardrail;
import com.dango.aicodegenerate.service.AiCodeModifierService;
import com.dango.aicodegenerate.tools.*;
import com.dango.dangoaicodeapp.model.enums.CodeGenTypeEnum;
import com.dango.dangoaicodeapp.service.ChatHistoryService;
import com.dango.dangoaicodecommon.exception.BusinessException;
import com.dango.dangoaicodecommon.exception.ErrorCode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * AI 代码修改服务工厂
 * 用于创建修改模式专用的 AI 服务实例
 * 注册文件操作工具和图片工具，供 AI 按需调用
 */
@Component
@Slf4j
public class AiCodeModifierServiceFactory {

    @Resource
    private StreamingChatModel odinaryStreamingChatModel;

    @Resource
    private StreamingChatModel reasoningStreamingChatModel;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    // 文件操作工具
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

    // 图片工具
    @Resource
    private ImageSearchTool imageSearchTool;

    @Resource
    private UndrawIllustrationTool undrawIllustrationTool;

    @Resource
    private MermaidDiagramTool mermaidDiagramTool;

    @Resource
    private LogoGeneratorTool logoGeneratorTool;

    /**
     * AI 修改服务实例缓存
     * 缓存策略：
     * - 最大缓存 500 个实例
     * - 写入后 30 分钟过期
     * - 访问后 10 分钟过期
     */
    private final Cache<String, AiCodeModifierService> serviceCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 修改服务实例被移除，key: {}, 原因: {}", key, cause);
            })
            .build();

    /**
     * 根据 appId 和代码生成类型获取修改服务（带缓存）
     *
     * @param appId       应用 ID
     * @param codeGenType 代码生成类型
     * @return AI 代码修改服务实例
     */
    public AiCodeModifierService getModifierService(long appId, CodeGenTypeEnum codeGenType) {
        String cacheKey = buildCacheKey(appId, codeGenType);
        return serviceCache.get(cacheKey, key -> createModifierService(appId, codeGenType));
    }

    /**
     * 创建新的 AI 修改服务实例
     *
     * @param appId       应用 ID
     * @param codeGenType 代码生成类型
     * @return AI 代码修改服务实例
     */
    private AiCodeModifierService createModifierService(long appId, CodeGenTypeEnum codeGenType) {
        log.info("为 appId: {} 创建新的 AI 修改服务实例，类型: {}", appId, codeGenType.getValue());

        // 根据 appId 构建独立的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(50)
                .build();

        // 从数据库加载历史对话到记忆中
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);

        // 根据代码生成类型选择不同的模型和工具配置
        return switch (codeGenType) {
            case VUE_PROJECT -> AiServices.builder(AiCodeModifierService.class)
                    .streamingChatModel(reasoningStreamingChatModel)
                    .chatMemory(chatMemory)
                    .chatMemoryProvider(memoryId -> chatMemory)
                    .tools(
                            // 文件操作工具
                            fileDirReadTool,
                            fileReadTool,
                            fileModifyTool,
                            fileWriteTool,
                            fileDeleteTool,
                            // 图片工具
                            imageSearchTool,
                            undrawIllustrationTool,
                            mermaidDiagramTool,
                            logoGeneratorTool
                    )
                    .inputGuardrails(new PromptSafetyInputGuardrail())  // 添加输入护轨
                    .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                            toolExecutionRequest, "Error: there is no tool called " + toolExecutionRequest.name()
                    ))
                    .build();

            case HTML, MULTI_FILE -> AiServices.builder(AiCodeModifierService.class)
                    .streamingChatModel(odinaryStreamingChatModel)
                    .chatMemory(chatMemory)
                    .chatMemoryProvider(memoryId -> chatMemory)
                    .tools(
                            // 文件操作工具
                            fileDirReadTool,
                            fileReadTool,
                            fileModifyTool,
                            fileWriteTool,
                            fileDeleteTool,
                            // 图片工具
                            imageSearchTool,
                            undrawIllustrationTool,
                            mermaidDiagramTool,
                            logoGeneratorTool
                    )
                    .inputGuardrails(new PromptSafetyInputGuardrail())  // 添加输入护轨
                    .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                            toolExecutionRequest, "Error: there is no tool called " + toolExecutionRequest.name()
                    ))
                    .build();

            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "不支持的代码生成类型: " + codeGenType.getValue());
        };
    }

    /**
     * 构建缓存键
     *
     * @param appId       应用 ID
     * @param codeGenType 代码生成类型
     * @return 缓存键
     */
    private String buildCacheKey(long appId, CodeGenTypeEnum codeGenType) {
        return "modifier_" + appId + "_" + codeGenType.getValue();
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
        log.info("已清除 appId: {} 的修改服务缓存", appId);
    }
}
