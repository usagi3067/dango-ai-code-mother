package com.dango.dangoaicodeapp.application.service;

import com.dango.dangoaicodeapp.domain.app.valueobject.ElementInfo;
import com.dango.dangoaicodeapp.domain.codegen.model.GenerationTaskSnapshot;
import reactor.core.publisher.Flux;

/**
 * 代码生成 应用服务层。
 *
 * @author dango
 */
public interface CodeGenApplicationService {

    /**
     * 启动后台生成任务（独立于 SSE 连接）
     *
     * @param appId       应用 ID
     * @param message     用户消息
     * @param elementInfo 选中的元素信息（可选）
     * @param userId      用户 ID
     * @return 是否成功启动
     */
    boolean startBackgroundGeneration(Long appId, String message, ElementInfo elementInfo, long userId);

    /**
     * 从 Redis Stream 消费并返回 Flux（用于 SSE 推送）
     *
     * @param appId   应用 ID
     * @param userId  用户 ID
     * @param afterId 从哪个 ID 之后开始读取，"0" 表示从头
     * @return 消息流
     */
    Flux<String> consumeGenerationStream(Long appId, long userId, String afterId);

    /**
     * 查询生成任务状态。
     */
    GenerationTaskSnapshot getGenerationStatus(Long appId, long userId);
}
