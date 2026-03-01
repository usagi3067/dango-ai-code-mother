package com.dango.dangoaicodeapp.infrastructure.redis;

import com.dango.dangoaicodeapp.domain.codegen.model.GenerationTaskSnapshot;
import com.dango.dangoaicodeapp.domain.codegen.port.GenerationTaskPort;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * Redis 任务状态适配器。
 */
@Component
public class GenerationTaskPortImpl implements GenerationTaskPort {

    @Resource
    private GenTaskService genTaskService;

    @Override
    public boolean tryReserveTask(Long appId, Long userId) {
        // 直接复用既有 Redis CAS 逻辑，领域层只感知“是否预占成功”。
        return genTaskService.tryReserveTask(appId, userId);
    }

    @Override
    public void bindChatHistoryId(Long appId, Long userId, Long chatHistoryId) {
        // 聊天记录绑定放在预占之后，避免先写消息导致孤儿 generating。
        genTaskService.bindChatHistoryId(appId, userId, chatHistoryId);
    }

    @Override
    public GenerationTaskSnapshot getTaskSnapshot(Long appId, Long userId) {
        String status = genTaskService.getStatus(appId, userId);
        Long chatHistoryId = genTaskService.getChatHistoryId(appId, userId);
        // 在适配层完成 Redis -> 领域快照转换，防止字段细节外泄。
        return new GenerationTaskSnapshot(status, chatHistoryId);
    }

    @Override
    public String getStreamKey(Long appId, Long userId) {
        return genTaskService.getStreamKey(appId, userId);
    }

    @Override
    public void markCompleted(Long appId, Long userId) {
        genTaskService.markCompleted(appId, userId);
    }

    @Override
    public void markError(Long appId, Long userId) {
        genTaskService.markError(appId, userId);
    }

    @Override
    public void cleanupTask(Long appId, Long userId) {
        genTaskService.cleanupTask(appId, userId);
    }
}
