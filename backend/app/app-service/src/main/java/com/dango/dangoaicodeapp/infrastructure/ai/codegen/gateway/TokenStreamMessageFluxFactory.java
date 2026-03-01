package com.dango.dangoaicodeapp.infrastructure.ai.codegen.gateway;

import cn.hutool.json.JSONUtil;
import com.dango.aicodegenerate.extractor.ToolArgumentsExtractor;
import com.dango.aicodegenerate.model.message.AiResponseMessage;
import com.dango.aicodegenerate.model.message.StreamMessage;
import com.dango.aicodegenerate.model.message.ToolExecutedMessage;
import dev.langchain4j.service.TokenStream;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TokenStream -> Flux 消息转换器。
 */
@Component
public class TokenStreamMessageFluxFactory {

    /**
     * 转换为仅文本消息流（JSON 字符串）。
     */
    public Flux<String> toTextFlux(TokenStream tokenStream) {
        return Flux.create(sink -> tokenStream
                .onPartialResponse(chunk -> sink.next(JSONUtil.toJsonStr(new AiResponseMessage(chunk))))
                .onCompleteResponse(response -> sink.complete())
                .onError(sink::error)
                .start());
    }

    /**
     * 转换为文本 + 工具调用消息流（JSON 字符串）。
     */
    public Flux<String> toTextAndToolFlux(TokenStream tokenStream) {
        return Flux.create(sink -> {
            Map<String, ToolArgumentsExtractor> extractors = new ConcurrentHashMap<>();

            tokenStream
                    .onPartialResponse(partialResponse ->
                            sink.next(JSONUtil.toJsonStr(new AiResponseMessage(partialResponse))))
                    .onPartialToolCall(partialToolCall -> {
                        String toolId = partialToolCall.id();
                        String toolName = partialToolCall.name();
                        String delta = partialToolCall.partialArguments();

                        ToolArgumentsExtractor extractor = extractors.computeIfAbsent(
                                toolId,
                                id -> new ToolArgumentsExtractor(id, toolName)
                        );

                        List<StreamMessage> messages = extractor.process(delta);
                        for (StreamMessage message : messages) {
                            sink.next(JSONUtil.toJsonStr(message));
                        }
                    })
                    .onToolExecuted(toolExecution ->
                            sink.next(JSONUtil.toJsonStr(new ToolExecutedMessage(toolExecution))))
                    .onCompleteResponse(response -> sink.complete())
                    .onError(sink::error)
                    .start();
        });
    }
}

