package com.dango.dangoaicodeapp.infrastructure.ai.codegen.gateway;

import cn.hutool.json.JSONUtil;
import com.dango.aicodegenerate.model.message.AiResponseMessage;
import com.dango.aicodegenerate.streaming.StreamingResponseProcessor;
import dev.langchain4j.service.TokenStream;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * TokenStream -> Flux 消息转换器。
 */
@Component
public class TokenStreamMessageFluxFactory {

    @Resource
    private StreamingResponseProcessor streamingProcessor;

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
     * 转换为纯文本分片流（不做 JSON 包装）。
     */
    public Flux<String> toChunkFlux(TokenStream tokenStream) {
        return Flux.create(sink -> tokenStream
                .onPartialResponse(sink::next)
                .onCompleteResponse(response -> sink.complete())
                .onError(sink::error)
                .start());
    }

    /**
     * 转换为文本 + 工具调用消息流（JSON 字符串）。
     * 使用 ai 模块的 StreamingResponseProcessor 处理流式响应。
     */
    public Flux<String> toTextAndToolFlux(TokenStream tokenStream) {
        return streamingProcessor.processAsJson(tokenStream);
    }
}
