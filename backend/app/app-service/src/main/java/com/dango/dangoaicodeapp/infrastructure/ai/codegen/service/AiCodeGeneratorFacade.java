package com.dango.dangoaicodeapp.infrastructure.ai.codegen.service;

import cn.hutool.json.JSONUtil;

import com.dango.aicodegenerate.extractor.ToolArgumentsExtractor;
import com.dango.aicodegenerate.model.message.AiResponseMessage;
import com.dango.aicodegenerate.model.message.StreamMessage;
import com.dango.aicodegenerate.model.message.ToolExecutedMessage;
import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;
import com.dango.dangoaicodeapp.domain.codegen.port.CodeGenerationGateway;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AI 代码生成外观类，组合生成和保存功能
 */
@Slf4j
@Service
public class AiCodeGeneratorFacade {

    @Resource
    private CodeGenerationGateway codeGenerationGateway;

    /**
     * 统一入口：生成并保存代码（流式, 使用 appId）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @param appId           应用 id
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        log.info("开始代码生成流式请求: appId={}, type={}, promptLength={}",
                appId,
                codeGenTypeEnum != null ? codeGenTypeEnum.getValue() : "unknown",
                userMessage != null ? userMessage.length() : 0);
        TokenStream tokenStream = codeGenerationGateway.generateCodeStream(appId, codeGenTypeEnum, userMessage);
        return processTokenStream(tokenStream);
    }

    /**
     * 将 TokenStream 转换为 Flux<String>，并传递工具调用信息
     *
     * @param tokenStream TokenStream 对象
     * @return Flux<String> 流式响应
     */
    private Flux<String> processTokenStream(TokenStream tokenStream) {
        return Flux.create(sink -> {
            // 为每个工具调用维护一个 extractor
            Map<String, ToolArgumentsExtractor> extractors = new ConcurrentHashMap<>();
            AtomicInteger partialResponseChunkCount = new AtomicInteger();
            AtomicLong partialResponseCharCount = new AtomicLong();
            AtomicInteger partialToolCallChunkCount = new AtomicInteger();
            AtomicInteger toolExecutedCount = new AtomicInteger();
            AtomicInteger partialThinkingChunkCount = new AtomicInteger();

            tokenStream.onPartialResponse((String partialResponse) -> {
                        partialResponseChunkCount.incrementAndGet();
                        partialResponseCharCount.addAndGet(partialResponse != null ? partialResponse.length() : 0);
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    .onPartialThinking(partialThinking -> {
                        int chunkIndex = partialThinkingChunkCount.incrementAndGet();
                        if (chunkIndex <= 3) {
                            String thinking = partialThinking.text();
                            log.info("代码生成流式思考片段: chunkIndex={}, chunkLength={}",
                                    chunkIndex,
                                    thinking != null ? thinking.length() : 0);
                        }
                    })
                    .onPartialToolCall(partialToolCall -> {
                        partialToolCallChunkCount.incrementAndGet();
                        String toolId = partialToolCall.id();
                        String toolName = partialToolCall.name();
                        String delta = partialToolCall.partialArguments();

                        if (toolId == null) {
                            log.warn("代码生成流式工具调用缺少 toolId，跳过参数提取: toolName={}, deltaPreview={}",
                                    toolName,
                                    preview(delta, 120));
                            return;
                        }

                        // 获取或创建 extractor
                        ToolArgumentsExtractor extractor = extractors.computeIfAbsent(
                            toolId,
                            id -> new ToolArgumentsExtractor(id, toolName)
                        );

                        // 处理 delta，获取需要发送的消息
                        List<StreamMessage> messages = extractor.process(delta);
                        for (StreamMessage msg : messages) {
                            sink.next(JSONUtil.toJsonStr(msg));
                        }
                    })
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        toolExecutedCount.incrementAndGet();
                        log.info("代码生成工具执行完成: toolName={}", toolExecution.request().name());
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })
                    .onCompleteResponse((ChatResponse response) -> {
                        int completeTextLength = 0;
                        int completeToolRequestCount = 0;
                        String modelName = null;
                        String finishReason = null;
                        Integer inputTokens = null;
                        Integer outputTokens = null;
                        Integer totalTokens = null;

                        if (response != null) {
                            modelName = response.modelName();
                            finishReason = response.finishReason() != null
                                    ? response.finishReason().name()
                                    : null;

                            if (response.aiMessage() != null) {
                                String text = response.aiMessage().text();
                                completeTextLength = text != null ? text.length() : 0;
                                if (response.aiMessage().toolExecutionRequests() != null) {
                                    completeToolRequestCount = response.aiMessage().toolExecutionRequests().size();
                                }
                            }

                            TokenUsage tokenUsage = response.tokenUsage();
                            if (tokenUsage != null) {
                                inputTokens = tokenUsage.inputTokenCount();
                                outputTokens = tokenUsage.outputTokenCount();
                                totalTokens = tokenUsage.totalTokenCount();
                            }
                        }

                        if (partialResponseChunkCount.get() == 0
                                && (completeTextLength > 0 || completeToolRequestCount > 0)) {
                            log.warn("代码生成未收到 onPartialResponse，但完成响应包含内容: completeTextLength={}, completeToolRequestCount={}",
                                    completeTextLength,
                                    completeToolRequestCount);
                        }

                        log.info("代码生成流式完成: model={}, finishReason={}, partialChunks={}, partialChars={}, partialToolCallChunks={}, toolExecutedCount={}, partialThinkingChunks={}, completeTextLength={}, completeToolRequestCount={}, inputTokens={}, outputTokens={}, totalTokens={}",
                                modelName,
                                finishReason,
                                partialResponseChunkCount.get(),
                                partialResponseCharCount.get(),
                                partialToolCallChunkCount.get(),
                                toolExecutedCount.get(),
                                partialThinkingChunkCount.get(),
                                completeTextLength,
                                completeToolRequestCount,
                                inputTokens,
                                outputTokens,
                                totalTokens);
                        sink.complete();
                    })
                    .onError((Throwable error) -> {
                        log.error("代码生成流式失败: partialChunks={}, partialChars={}, partialToolCallChunks={}, toolExecutedCount={}, partialThinkingChunks={}",
                                partialResponseChunkCount.get(),
                                partialResponseCharCount.get(),
                                partialToolCallChunkCount.get(),
                                toolExecutedCount.get(),
                                partialThinkingChunkCount.get(),
                                error);
                        sink.error(error);
                    })
                    .start();
        });
    }

    private static String preview(String text, int maxLength) {
        if (text == null) {
            return "null";
        }
        String normalized = text.replace("\n", "\\n");
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }
}
