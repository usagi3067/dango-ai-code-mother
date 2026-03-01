package com.dango.dangoaicodeapp.domain.codegen.port;

import com.dango.dangoaicodeapp.domain.codegen.model.GenerationTaskSnapshot;

/**
 * 代码生成任务端口。
 *
 * 该端口抽象 Redis 等任务状态存储细节，
 * 让领域服务只表达“一次任务只能启动一个会话”的业务约束。
 */
public interface GenerationTaskPort {

    /**
     * 预占任务（CAS 防重）。
     * 先占位再写聊天记录，确保“没有任务就不能出现 generating 消息”。
     */
    boolean tryReserveTask(Long appId, Long userId);

    /**
     * 将占位 AI 消息绑定到已预占任务。
     * 采用二阶段是为了把“抢锁成功但消息落库失败”纳入补偿流程。
     */
    void bindChatHistoryId(Long appId, Long userId, Long chatHistoryId);

    /**
     * 获取任务快照。
     * 对上层屏蔽 Redis Hash 字段名，避免基础设施细节扩散到应用层。
     */
    GenerationTaskSnapshot getTaskSnapshot(Long appId, Long userId);

    /**
     * 获取任务对应的流 key。
     */
    String getStreamKey(Long appId, Long userId);

    /**
     * 正常完成收口（任务与流 TTL 在适配层统一处理）。
     */
    void markCompleted(Long appId, Long userId);

    /**
     * 异常完成收口（任务与流 TTL 在适配层统一处理）。
     */
    void markError(Long appId, Long userId);

    /**
     * 启动失败时清理预占任务，防止僵尸锁。
     */
    void cleanupTask(Long appId, Long userId);
}
