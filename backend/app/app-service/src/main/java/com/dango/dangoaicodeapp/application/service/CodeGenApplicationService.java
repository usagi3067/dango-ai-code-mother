package com.dango.dangoaicodeapp.application.service;

import com.dango.dangoaicodeapp.domain.app.valueobject.ElementInfo;
import reactor.core.publisher.Flux;

/**
 * 代码生成 应用服务层。
 *
 * @author dango
 */
public interface CodeGenApplicationService {

    /**
     * 根据应用id和用户消息生成代码（Agent 模式）
     * <p>
     * 默认使用 Agent 模式（工作流模式）生成代码
     *
     * @param appId 应用 ID
     * @param message 用户消息
     * @param elementInfo 选中的元素信息（可选，用于修改模式）
     * @param userId 用户 ID
     * @return 生成的代码流
     */
    Flux<String> chatToGenCode(Long appId, String message, ElementInfo elementInfo, long userId);

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
}
