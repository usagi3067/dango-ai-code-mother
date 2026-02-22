package com.dango.dangoaicodeapp.domain.codegen.handler;


import com.dango.dangoaicodeapp.application.service.ChatHistoryService;
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
     * 统一处理流式消息
     *
     * @param originFlux         原始流
     * @param chatHistoryService 聊天历史服务
     * @param appId              应用ID
     * @param userId             用户ID
     * @return 处理后的流
     */
    public Flux<String> doExecute(Flux<String> originFlux,
                                  ChatHistoryService chatHistoryService,
                                  long appId, long userId) {
        return jsonMessageStreamHandler.handle(originFlux, chatHistoryService, appId, userId);
    }

    /**
     * 处理流式消息但不保存到 chatHistory（由后台任务自行保存）
     */
    public Flux<String> doExecuteWithoutSave(Flux<String> originFlux) {
        return jsonMessageStreamHandler.handleWithoutSave(originFlux);
    }
}
