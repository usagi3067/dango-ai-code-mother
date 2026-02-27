package com.dango.dangoaicodeapp.domain.codegen.handler;


import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 流处理器执行器
 * 统一使用 JsonMessageStreamHandler 处理流式消息
 */
@Slf4j
@Component
public class StreamHandlerExecutor {

    @Resource
    private JsonMessageStreamHandler jsonMessageStreamHandler;

    /**
     * 处理流式消息（不保存到 chatHistory，由调用方自行保存）
     */
    public Flux<String> doExecute(Flux<String> originFlux) {
        return jsonMessageStreamHandler.handleWithoutSave(originFlux);
    }
}
