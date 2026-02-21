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
}
