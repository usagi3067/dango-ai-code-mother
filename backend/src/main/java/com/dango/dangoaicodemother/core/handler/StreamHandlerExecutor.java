package com.dango.dangoaicodemother.core.handler;

import com.dango.dangoaicodemother.model.entity.User;
import com.dango.dangoaicodemother.model.enums.CodeGenTypeEnum;
import com.dango.dangoaicodemother.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 流处理器执行器
 * 统一使用 JsonMessageStreamHandler 处理所有代码生成类型的流式消息
 */
@Slf4j
@Component
public class StreamHandlerExecutor {

    @Resource
    private JsonMessageStreamHandler jsonMessageStreamHandler;

    /**
     * 统一处理所有类型的流式消息
     * 不再区分代码生成类型，统一使用 JsonMessageStreamHandler
     *
     * @param originFlux         原始流
     * @param chatHistoryService 聊天历史服务
     * @param appId              应用ID
     * @param loginUser          登录用户
     * @param codeGenType        代码生成类型
     * @return 处理后的流
     */
    public Flux<String> doExecute(Flux<String> originFlux,
                                  ChatHistoryService chatHistoryService,
                                  long appId, User loginUser, CodeGenTypeEnum codeGenType) {
        // 统一使用 JsonMessageStreamHandler 处理所有类型
        return jsonMessageStreamHandler.handle(originFlux, chatHistoryService, appId, loginUser, codeGenType);
    }
}
