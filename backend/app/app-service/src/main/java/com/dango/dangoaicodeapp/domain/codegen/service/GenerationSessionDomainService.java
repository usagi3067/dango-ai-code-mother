package com.dango.dangoaicodeapp.domain.codegen.service;

import com.dango.dangoaicodeapp.domain.codegen.model.GenerationSession;
import com.dango.dangoaicodeapp.domain.codegen.model.GenerationStreamChunk;
import com.dango.dangoaicodeapp.domain.codegen.model.GenerationTaskSnapshot;

import java.util.List;

/**
 * 代码生成会话领域服务。
 *
 * 聚合“任务锁 + AI 占位消息 + 流输出”跨存储不变量，
 * 确保会话启动/结束的一致性在一个领域对象内闭环。
 */
public interface GenerationSessionDomainService {

    /**
     * 启动会话并确保一致性：
     * 任务预占（CAS）+ AI 占位消息创建 + 绑定，失败时做补偿。
     */
    GenerationSession startSession(Long appId, Long userId);

    GenerationTaskSnapshot getTaskSnapshot(Long appId, Long userId);

    void appendChunk(GenerationSession session, String content, String msgType);

    /**
     * 正常结束会话，统一收敛任务状态与消息状态更新。
     */
    void completeSession(GenerationSession session, String finalAiContent);

    /**
     * 失败结束会话（显式内容版本）。
     */
    void failSession(GenerationSession session, String errorContent);

    /**
     * 失败结束会话（异常版本），由领域服务统一兜底错误文案策略。
     */
    void failSession(GenerationSession session, Throwable throwable);

    List<GenerationStreamChunk> readStreamChunks(Long appId, Long userId, String afterId, long count);
}
