package com.dango.dangoaicodeapp.domain.codegen.model;

/**
 * 单次代码生成会话标识。
 *
 * 将跨存储（任务状态 + 聊天消息 + 流 key）的关联信息收敛成一个领域值对象，
 * 避免应用层散落多个独立参数导致一致性约束失效。
 */
public record GenerationSession(Long appId, Long userId, Long chatHistoryId, String streamKey) {

    public GenerationSession {
        if (appId == null || appId <= 0) {
            throw new IllegalArgumentException("appId must be positive");
        }
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        if (chatHistoryId == null || chatHistoryId <= 0) {
            throw new IllegalArgumentException("chatHistoryId must be positive");
        }
        if (streamKey == null || streamKey.isBlank()) {
            throw new IllegalArgumentException("streamKey must not be blank");
        }
    }
}
