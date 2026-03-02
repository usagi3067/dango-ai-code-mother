package com.dango.aicodegenerate.streaming;

import cn.hutool.json.JSONUtil;
import com.dango.aicodegenerate.extractor.ToolArgumentsExtractor;
import com.dango.aicodegenerate.model.message.AiResponseMessage;
import com.dango.aicodegenerate.model.message.StreamMessage;
import com.dango.aicodegenerate.model.message.ToolExecutedMessage;
import com.dango.aicodegenerate.tool.ToolConfig;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 流式响应处理器
 *
 * <h2>功能说明</h2>
 * 将 LangChain4j 的 TokenStream 转换为统一的 StreamMessage 流，
 * 便于业务模块进行流式处理和前端展示。
 *
 * <h2>使用场景</h2>
 * <ul>
 *   <li>代码生成场景：实时展示 AI 生成的代码和工具调用过程</li>
 *   <li>对话场景：实时展示 AI 的回复内容</li>
 *   <li>任何需要流式 AI 响应的场景</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 1. 创建处理器（需要提供工具配置）
 * @Resource
 * private StreamingResponseProcessor processor;
 *
 * // 2. 处理 TokenStream
 * TokenStream tokenStream = aiService.chat(userMessage);
 * Flux<StreamMessage> messageFlux = processor.process(tokenStream);
 *
 * // 3. 订阅消息流
 * messageFlux.subscribe(message -> {
 *     if (message instanceof AiResponseMessage) {
 *         // 处理 AI 响应
 *     } else if (message instanceof ToolRequestMessage) {
 *         // 处理工具请求
 *     } else if (message instanceof ToolExecutedMessage) {
 *         // 处理工具执行结果
 *     }
 * });
 *
 * // 4. 或者直接转换为 JSON 字符串流
 * Flux<String> jsonFlux = processor.processAsJson(tokenStream);
 * }</pre>
 *
 * <h2>扩展说明</h2>
 * 如果你的业务场景不需要工具调用，可以不提供 ToolConfig（注入时会自动为 null）：
 * <pre>{@code
 * // ToolConfig 是可选的，如果没有提供，工具调用将不会被处理
 * }</pre>
 */
@Component
public class StreamingResponseProcessor {

    private final ToolConfig toolConfig;

    /**
     * 构造函数
     *
     * @param toolConfig 工具配置，如果不需要工具调用可以不提供（自动注入为 null）
     */
    public StreamingResponseProcessor(@Autowired(required = false) ToolConfig toolConfig) {
        this.toolConfig = toolConfig;
    }

    /**
     * 处理 TokenStream，转换为 StreamMessage 的 Flux
     *
     * @param tokenStream LangChain4j 的 TokenStream
     * @return StreamMessage 的响应式流
     */
    public Flux<StreamMessage> process(TokenStream tokenStream) {
        return Flux.create(sink -> {
            Map<String, ToolArgumentsExtractor> extractors = new ConcurrentHashMap<>();

            tokenStream
                .onPartialResponse(partialResponse -> {
                    sink.next(new AiResponseMessage(partialResponse));
                })
                .onPartialToolCall(partialToolCall -> {
                    if (toolConfig == null) {
                        return; // 不处理工具调用
                    }

                    String toolId = partialToolCall.id();
                    String toolName = partialToolCall.name();
                    String delta = partialToolCall.partialArguments();

                    ToolArgumentsExtractor extractor = extractors.computeIfAbsent(
                        toolId,
                        id -> {
                            String triggerParam = toolConfig.getTriggerParam(toolName);
                            String action = toolConfig.getAction(toolName);
                            return new ToolArgumentsExtractor(id, toolName, triggerParam, action);
                        }
                    );

                    List<StreamMessage> messages = extractor.process(delta);
                    messages.forEach(sink::next);
                })
                .onToolExecuted(toolExecution -> {
                    sink.next(new ToolExecutedMessage(toolExecution));
                })
                .onCompleteResponse(response -> {
                    sink.complete();
                })
                .onError(error -> {
                    sink.error(error);
                })
                .start();
        });
    }

    /**
     * 处理并转换为 JSON 字符串流（便捷方法）
     *
     * @param tokenStream LangChain4j 的 TokenStream
     * @return JSON 字符串的响应式流
     */
    public Flux<String> processAsJson(TokenStream tokenStream) {
        return process(tokenStream)
            .map(JSONUtil::toJsonStr);
    }
}
